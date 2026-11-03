# Design

## Context

What the code does today:

- **The profile screen.** `MyProfileScreen` edits the name and username through the `updateProfile`
  callable, and the animal and colour through `UserPathFirestore.updateAvatar`. That second path is
  a client write of `users/{uid}` plus the reservation marker and `sync/updates.profile`, in one batch.
- **Nobody refreshes another user's profile.** Other users' profiles are read cache-first and never
  refreshed: `fetchProfiles(refresh = true)` has no caller. That is why a colleague's new animal never
  reaches the member list or the swap rows (`firestore-usage.md`, *Known limits*).
- **Writers of `groups/{g}.members.{uid}`:**
  - The client creates it for a group's creator.
  - `acceptJoinRequest`, and the auto-approve branch of `requestToJoinGroup`, write it as
    `{ name, username }`.
  - `updateProfile` and `deleteAccount` overwrite the **whole entry**.
  - `GroupRepositoryImpl.updateGroup` re-sends the **whole group document**, `members` included, as
    it read it, with a `set` that has no merge.
- **Storage.** There is no Firebase Storage, no image loading library, and no Storage emulator. iOS
  links each Firebase SDK as a Swift package product in the Xcode project.
- **The audit.** The usage audit is `FirestoreAudit.kt`, which `CLAUDE.md` still calls
  `FirestoreUsageMetrics`. It counts Firestore reads and writes and callables.

## Goals / Non-Goals

**Goals:**
- A photo is published by one server-side keeper that writes every copy, as the name already is.
- No new Firestore read anywhere. A photo costs one Storage download per device per photo version.
- The animal is always there underneath, so no state of the photo leaves an empty circle.

**Non-Goals:**
- Moderation of any kind (TUR-60). The design leaves room for it, as noted in D2.
- Refreshing a colleague's cached animal.
- Server-side image processing: the server checks the photo but never re-encodes it.

## Decisions

### D1. `photoId` on the profile and on each group's member entry

`users/{uid}.photoId` and `groups/{g}.members.{uid}.photoId` hold a random id. The file is at
`profilePhotos/{uid}/{photoId}.webp`; with no `photoId`, the client draws the animal.

**Why an id and not a URL or a path.**
- An id can be validated, which a URL cannot.
- A new photo is a new id, so the file at a path never changes. A cache can then keep it forever,
  keyed by `{uid}/{photoId}`.
- The path is derived on the client, so the bucket is not baked into the data.

**Alternative rejected: a download URL.** It carries a bearer token, so anyone it reaches can open it
in a browser. That breaks *visible to signed-in users only*.

### D2. The client uploads, the `setProfilePhoto` callable publishes

```
client: pick -> crop -> encode WebP 384/q80
        put profilePhotos/{uid}/{photoId}.webp          (rules: owner, <=100 KB, image/webp, create only)
        call setProfilePhoto({ photoId })               (or { photoId: null } to remove)
server: check the file exists and fits (metadata)
        tx:    users/{uid}.photoId, updateAt
               usernames/{username}.updateAt            (searchers' marker)
               users/{uid}/sync/updates.profile
        per group with uid in memberUids, one batch each:
               members.{uid}.photoId (or delete), updateAt, groups/{g}/sync/updates
        re-read users/{uid}.photoId; delete every other object under profilePhotos/{uid}/
```

`setProfilePhoto` is shaped like `updateProfile`. The fan-out needs a server writer, since clients
never write `members`, and `CLAUDE.md` already gives a copied field exactly that kind of keeper.

**Cleanup reads the profile again** instead of trusting its own argument. Two devices publishing at
once could otherwise each delete the other's file, leaving the profile pointing at nothing.

**Alternatives rejected:**
- *The client writes `photoId`, and a Firestore trigger fans it out.* This saves the callable but
  answers asynchronously. The client would also be trusted with a field the trigger then has to
  undo.
- *A Storage `onObjectFinalized` trigger publishes the photo.* It has no synchronous answer for the
  UI. It is also where TUR-60's moderation would end up anyway, and that will need a staging path,
  not the final one.

**Room for TUR-60.** Moderation slots in between the upload and the publish inside this callable.
The upload then moves to a staging prefix that only its owner can read.

### D3. Security rules

- **Storage (`storage.rules`, new):**
  - `profilePhotos/{uid}/{file}`: `read` if signed in.
  - `create` if `auth.uid == uid`, `request.resource.size <= 100 * 1024`, `contentType ==
    'image/webp'`, and the file name matches `^[A-Za-z0-9_-]{1,64}\.webp$`.
  - `update` and `delete`: never. Only the Admin SDK deletes.
  - No `firestore.get()`: cross-service rules bill a Firestore read on every evaluation, and that
    would mean every image load.
- **Firestore:**
  - The `users/{uid}` update rule freezes `photoId`, as it does `name` and `username`.
  - `create` refuses a `photoId`, so a new account cannot skip the keeper. That matters once TUR-60
    moderates.

### D4. Every writer of `members.{uid}` must keep `photoId`

| Writer | Change |
|---|---|
| `updateProfile` | Writes `members.{uid}.name` and `members.{uid}.username` as separate fields, no longer the whole entry. |
| `acceptJoinRequest`, auto-approve `requestToJoinGroup` | Add `photoId` from the profile they already read, when it has one. |
| Client group creation | The creator's entry carries their `photoId`. |
| `GroupRepositoryImpl.updateGroup` | `GroupMemberDocument` gains `photoId: String? = null`, so the full `set` round-trips it. |
| `deleteAccount` | Unchanged: it overwrites the entry with `{ name: "", username: "" }`, which is exactly the drop it needs. It also deletes `profilePhotos/{uid}/` with `bucket.deleteFiles({ prefix })`. |

Making `updateGroup` stop sending `members` would be cleaner, but it changes a write path that
already works for names. The round-trip is the smallest correct step.

### D5. Picking, cropping and encoding on the device

- **Picker: our own `expect`/`actual`.** No library: the only candidate, FileKit, needs Compose
  Multiplatform 1.12 from 0.16, and its 0.15 pulls coroutines up to 1.11.
  - Android: the system Photo Picker (`PickVisualMedia`, images only) through an activity result
    launcher, read with the `ContentResolver`.
  - iOS: `PHPickerViewController` with one image, loaded as `public.jpeg`. A HEIC photo then reaches
    us as JPEG, which matters because Skia cannot decode HEIC.
  - Neither asks for a permission.
- **Crop: our own, in common Compose.** No library either. Crayfish needs Compose Multiplatform 1.12.
  Native crops exist on iOS only (`UIImagePickerController.allowsEditing`, square, legacy API), so
  Android would need its own crop anyway.
  - The picked image sits under a fixed circle and is moved and zoomed with `transformable`. The
    circle never leaves the image.
  - The square behind the circle is drawn into a 384×384 `ImageBitmap`. The circle is only a mask,
    both when cropping and when drawing, so the file needs no alpha.
  - The geometry, meaning the initial fit, the clamping and the source rect, is a pure function so
    that unit tests can cover it.
- **Encode:**
  - Encode to WebP at quality 80 through an `expect` function. On Android that is
    `Bitmap.compress`: `WEBP_LOSSY` from API 30, `WEBP` below, since minSdk is 24. On iOS it is
    Skia's `encodeToData(EncodedImageFormat.WEBP, 80)`.
  - Re-encoding is what drops EXIF.
  - If the result is still over 100 KB, which is unlikely at 384 px, encode once more at quality 60
    before failing.
- **Testing seam:** the picker sits behind a `PhotoPicker` interface bound in Koin. The E2E module
  swaps it for one that returns a bundled image, as it already swaps `TextSharer`.

### D6. Uploading and downloading through the Storage SDK

- **`dev.gitlive:firebase-storage`**, at the same version as the other GitLive modules.
  - **Upload:** `putData` with metadata `contentType = image/webp` and
    `cacheControl = public, max-age=31536000, immutable`.
  - **Download:** the bytes come through the SDK, so the rules and App Check apply, with a 100 KB
    cap. If GitLive has no common `getBytes`, an `expect`/`actual` calls the native SDK through the
    wrapper's `android` / `ios` handle: `getBytes(max)` on Android, `dataWithMaxSize` on iOS.
- **Upload timeout:** 30 s, then the task is cancelled. The native SDKs otherwise retry for up to
  10 minutes, and the user would watch a spinner offline.
- **Images:** Coil 3 with a custom `Fetcher` for a `ProfilePhoto(uid, photoId)` model.
  - The bytes live in `ProfilePhotoCache`, a disk cache keyed `{uid}/{photoId}` that downloads only
    on a miss. Paths never change, so nothing ever invalidates. Coil keeps the decoded images in
    memory and its own disk cache is off.
  - **Priming:** after a successful upload, the profile screen writes the encoded bytes into that
    cache under the new key. The user's own new photo then appears at once, with no download and no
    flash of the animal.

### D7. Drawing the avatar

- **`UserAvatar`** takes the `AnimalAvatar` and an optional `ProfilePhoto`. It always draws the
  animal, and draws the photo over it, clipped to the circle, only once the photo has loaded. So
  loading, failure and offline all show the animal with no extra state.
- **Where the photo comes from:**
  - the user's own avatar (profile, settings): `User.photoId`;
  - the group member list and the swap rows: `GroupMember.photoId`, from the group listener that is
    already open. A swap row's requester is a member of that group;
  - People cards and search results: `UserProfile.photoId`;
  - join requests: the requester's cached profile. They are not a member yet, so it may be stale,
    which is acceptable on an admin screen.

### D8. Profile screen

- **`MyProfileUi` gains:**
  - `photoId`;
  - `avatarMenuOpen`;
  - a `PhotoStep`: `Idle | Cropping(bytes) | Uploading`.
- **New messages:** `ProfileMessage.PhotoUploadFailed` and `PhotoRemoveFailed`.
- **No reset of the form.** Uploading and removing call the repository directly and touch neither
  `initProfileUi` nor the unsaved fields. The new `photoId` arrives through `loggedUserFlow`, whose
  collector copies only the photo fields while the form is dirty.
- **Repository:** `UserRepository` gains `uploadProfilePhoto(bytes)` and `removeProfilePhoto()`. Both
  return `Outcome<Unit, ProfilePhotoError>`, and the first logs `profile_photo_uploaded` on success.

### D9. Audit

- **Storage counter:** `FirestoreAudit` gains `trackStorage(TAG, operation, bytes)` and prints a
  third block, `Storage  Uploads: N  Downloads: N  KB: N`, broken down per class. It is called
  after a successful upload or download, never for a Coil cache hit.
- **Docs:** `firestore-usage.md` gains rows for uploading, removing and seeing photos.

### D10. Emulators and E2E

- **Emulator:**
  - `firebase.json` declares `storage: { rules: "storage.rules" }` and the `storage` emulator on 9199.
  - `FirebaseEmulators.kt` adds 9199 to its ports and to `--only`.
  - `TurniaTestApplication` points Storage at the emulator.
- **Fixtures:**
  - The loader wipes the bucket before each test: it lists `profilePhotos/` with the emulator's
    `Bearer owner` token and deletes what it finds.
  - It uploads the files a fixture names under a new `storage` key, which maps a path to a bundled
    asset.
  - A new `profile-photo.json` gives bruno a photo.
- **CI:** `.github/workflows/android-e2e.yml` needs `storage` in its `--only` list. That file is
  TUR-21's, so the edit comes after #58 merges. Until then the photo flows pass locally and fail on
  CI's emulators.

## Risks / Trade-offs

- **[No moderation until TUR-60]** An offensive photo is visible to every signed-in user until its
  owner removes it, and App Store review may cite guideline 1.2. → No store release carries this
  change before TUR-60. The developer confirms this in the `## Proposal`.
- **[Coil is held at 3.5.0]** 3.6 needs Compose Multiplatform 1.12. → The director is opening a
  ticket for the Compose Multiplatform 1.12 / Kotlin 2.4.20 upgrade. Coil moves up with it.
- **[Our own picker and crop to maintain]** About 150–200 lines of crop and two small platform
  pickers. → The crop geometry is a pure function under unit tests, and the E2E flows drive the crop
  on Android.
- **[GitLive Storage may not expose `getBytes`, or `putData` from a `ByteArray`]** → `expect`/`actual`
  over the native handles. The decision stays, only some platform code is added.
- **[`updateGroup` sends a stale `members` map]** An admin saving from a stale cache could put back an
  old `photoId`, as it can already put back an old name. → The same exposure as today. Making
  `updateGroup` stop writing `members` is a follow-up, not this change.
- **[A colleague's cached profile still freezes their photo outside group screens]** People cards and
  search results come from listeners and markers, so they stay current. Join requests use the cached
  profile. → Acceptable. Group screens are where colleagues are seen.
- **[An upload with no publish leaves an orphan file]** For example, the app is killed between the
  `put` and the call. → The next set or remove deletes it. The cost is one file of at most 100 KB per
  abandoned attempt.
- **[Storage App Check]** Storage is not enforced, and enforcing it before a release that attests
  would lock older builds out of photos only. → Add Storage to the staged rollout in
  `enable-app-check-enforcement`. Nothing is enforced in this change.

## Migration Plan

1. **Developer, in the console:** create the default bucket (`turnia-23ebc.firebasestorage.app`, as
   both apps' configs already name it) and pick its location.
2. **Developer:** `firebase deploy --only firestore,functions,storage`. The rules must deploy before
   the client: without `storage.rules` uploads are denied, and the `photoId` freeze must exist before
   anything writes the field.
3. **Release:** no store release with this change before TUR-60 lands.

**Rollback:** deploy the previous functions and rules. Clients that read `photoId` keep drawing the
animal when the file is gone, so nothing breaks.

## Open Questions

- **The bucket's location.** Storage's free tier applies only to US regions, and every photo is
  downloaded once per device, so latency hardly matters. The developer picks it in the console, and
  nothing in the code depends on it.
