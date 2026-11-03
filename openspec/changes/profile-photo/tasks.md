# Tasks

## 1. Dependencies

- [x] 1.1 Add to `gradle/libs.versions.toml`:
  - `dev.gitlive:firebase-storage` (version ref `firebase`) to `core`, and to the Android test classpath;
  - Coil 3 `coil-compose` 3.5.0 to `app/shared`: 3.6 needs Compose Multiplatform 1.12.

  The picker and the crop are our own code (4.4, 4.5): no picker or crop library. Verify that
  `./gradlew :app:androidApp:assembleDebug` passes and that Compose Multiplatform stays at 1.11.1 and
  coroutines at 1.10.2 (`./gradlew :app:shared:dependencies --configuration androidRuntimeClasspath`).
- [x] 1.2 Link the `FirebaseStorage` package product to the iOS target in
  `app/iosApp/iosApp.xcodeproj`, next to the other Firebase products. Verify with `xcodebuild` for
  the simulator: `BUILD SUCCEEDED`.

## 2. Backend: rules, emulator, functions

- [x] 2.1 Add `firebase/storage.rules` as in design D3:
  - read when signed in;
  - create by the owner only, at most 100 KB, `image/webp`, file name `^[A-Za-z0-9_-]{1,64}\.webp$`;
  - no update and no delete.

  Declare it in `firebase.json` (`"storage": { "rules": "storage.rules" }`), along with the
  `storage` emulator on port 9199. Verify `firebase emulators:start --only storage` loads the rules,
  after asking the director for the emulator turn.
- [x] 2.2 In `firebase/firestore.rules`, `users/{uid}`:
  - `update` keeps `photoId` unchanged, as it does `name` and `username`;
  - `create` refuses any `photoId`.

  Verified by 2.7.
- [x] 2.3 Point the E2E stack at the Storage emulator:
  - `buildSrc/.../FirebaseEmulators.kt`: port 9199 in `ports` and `storage` in `--only`;
  - `Emulator.STORAGE_PORT`;
  - `TurniaTestApplication`: `Firebase.storage.useEmulator`;
  - a `StorageRest` helper that lists, deletes and uploads with the emulator's `Bearer owner` token;
  - `Fixtures`: wipe `profilePhotos/` before each test, and seed a new `storage` key (path → bundled asset);
  - a small WebP asset and `firebase/test/fixtures/profile-photo.json`, in which bruno has a
    `photoId` on his profile and on his Urgencias and Planta member entries;
  - `firebase/test/README.md`: the `storage` key and the new fixture.

  Verify `AuthFlowsTest` and the classes in `GroupFlowsTest.kt` (`JoinGroupFlowsTest`,
  `InvitationLinkFlowTest`, `GroupMembershipFlowsTest`) still pass with the Storage emulator running.
- [x] 2.4 Add the `setProfilePhoto` callable (`firebase/functions/src/users.ts`, exported from
  `index.ts`, `enforceAppCheck: true`), as in design D2:
  - validate `photoId` (pattern or null);
  - check the file's metadata (exists, ≤ 100 KB, `image/webp`);
  - one transaction for the profile, the reservation marker and `sync/updates.profile`;
  - one batch per group for `members.{uid}.photoId`, the group's `updateAt` and the group marker;
  - re-read the profile, then delete every other file under `profilePhotos/{uid}/`;
  - new `TurniaError` codes for a missing or invalid file.

  Verify `npm --prefix firebase/functions run build` passes. Behaviour is verified by 2.7.
- [x] 2.5 Keep `photoId` in every other writer of a member entry:
  - `updateProfile` writes `members.{uid}.name` and `.username` as separate fields;
  - `acceptJoinRequest` and the auto-approve branch of `requestToJoinGroup` copy the profile's
    `photoId` when it has one;
  - `deleteAccount` deletes `profilePhotos/{uid}/` with `bucket.deleteFiles({ prefix })`.

  Verify the functions build. Behaviour is verified by 2.7.
- [ ] 2.6 Document the backend:
  - `firebase/firestore-schema.md`:
    - `photoId` on `users/{uid}` and on `groups/{g}.members`;
    - a *Storage* section for `profilePhotos/{uid}/{photoId}.webp` (access, limits, single-file invariant);
    - account deletion;
    - the invariants on who writes `photoId`;
  - `CLAUDE.md`:
    - rewrite the *Avatar* rule: the animal is copied nowhere; the photo has a keeper;
    - *Sensitive points* 1 and 6;
    - Firebase Storage under *Tech stack*;
    - `storage.rules` under *Project structure*;
    - Storage listed as not enforced under App Check, to be added to `enable-app-check-enforcement`'s
      rollout.

  Verify the text agrees with the specs and design.
- [x] 2.7 Add `ProfilePhotoBackendTest` under `e2e/flows`: instrumented, no UI, signed in through
  the SDK. It covers:
  - upload through `StorageRest`, then `setProfilePhoto` writes `photoId` on the profile, the
    reservation marker and both group copies, and deletes an orphan file;
  - `setProfilePhoto(null)` clears them all and deletes the file;
  - Storage refuses 150 KB, `image/jpeg`, another uid's folder and a read with no session;
  - a client write of `photoId` to `users/{uid}` is denied;
  - `updateProfile` keeps the member entry's `photoId`;
  - accepting a join request copies `photoId`;
  - `deleteAccount` leaves nothing under `profilePhotos/{uid}/`.

  Verify that it passes on the emulators.

## 3. Core data layer

- [x] 3.1 Carry `photoId` through the model:
  - `photoId: String? = null` on `UserDocument`, `GroupMemberDocument`, `UserProfile`, `User` and
    `GroupMember`;
  - map it in `UserDocumentMapper` and `GroupMapper`;
  - `GroupRepositoryImpl.createGroup` writes the creator's `photoId`;
  - `updateGroup` round-trips `members` with it.

  Verify with unit tests: mapping both ways, and a test asserting that the `members` a group save
  writes back keep every `photoId` (`ProfilePhotoMappingTest`; end to end in 5.3).
- [x] 3.2 Add `trackStorage(tag, operation, bytes)` to `FirestoreAudit`, with a Storage block in
  the summary (uploads, downloads, KB per class) printed apart from Firestore and Functions. Add its
  row to the *Firestore usage tracking* table in `CLAUDE.md`. Verify with a unit test on the summary
  text.
- [ ] 3.3 Add `ProfilePhotoStorage`, registered in Koin's `DataModule`:
  - `upload(uid, photoId, bytes)` through `putData`, with `image/webp` and an immutable
    `cacheControl`, under a 30 s timeout;
  - `download(uid, photoId)` capped at 100 KB, through `expect`/`actual` over the native handles if
    GitLive has no `getBytes`;
  - both call `trackStorage` on success.

  Verify with an instrumented `ProfilePhotoStorageTest`: an upload then a download returns the same
  bytes, and a 150 KB upload fails.
- [ ] 3.4 Publish and remove photos:
  - `UserProfileFunction.setProfilePhoto(photoId | null)` with `trackFunction`;
  - a `ProfilePhotoError` (`UploadFailed`, `RemoveFailed`);
  - `UserRepository.uploadProfilePhoto(bytes)`, which uploads under a fresh id, publishes and then
    logs `AnalyticsEvent.ProfilePhotoUploaded` (`profile_photo_uploaded`);
  - `UserRepository.removeProfilePhoto()`, which logs nothing;
  - no-op failures in `DemoRepositories`.

  Verify with unit tests over fakes: a success logs the event once, and a failed upload, a failed
  publish and a removal log nothing.
- [ ] 3.5 Add rows to `firebase/firestore-usage.md`: upload a photo (1 upload, 1 call; server writes
  for the profile, the marker and the reservation, plus 2 per group; each member pays 1 group read
  per shared group), remove a photo, and see photos (0 Firestore reads, 1 Storage download per
  photo version per device). Verify the numbers against the audit output of a manual upload on the
  emulator.

## 4. Showing photos

- [ ] 4.1 Add the photo encoder (`expect`/`actual`):
  - scale to 384×384;
  - WebP at quality 80 (`WEBP_LOSSY` from API 30, `WEBP` below; Skia on iOS);
  - one retry at quality 60 if over 100 KB.

  Verify with an Android instrumented test (a 4000×3000 JPEG with GPS EXIF comes out as a 384×384
  WebP of at most 100 KB with no EXIF) and an `iosSimulatorArm64Test` for the size and dimensions.
- [ ] 4.2 Set up Coil 3:
  - one `ImageLoader` with a disk cache;
  - a `Fetcher` for `ProfilePhoto(uid, photoId)`, keyed `{uid}/{photoId}`, that downloads through
    `ProfilePhotoStorage` on a miss;
  - a `prime(photo, bytes)` that the repository calls after a successful upload.

  Verify with a unit test over a fake storage: a second load does not download, and a primed photo
  never downloads.
- [ ] 4.3 Change `UserAvatar` to take `AnimalAvatar` and an optional `ProfilePhoto`. It always draws
  the animal and draws the loaded photo over it, clipped to the circle. Update every caller:
  - profile and settings: `User.photoId`;
  - group members and swap rows: `GroupMember.photoId`;
  - `PersonCard` and `ShareCalendarSheet`: `UserProfile.photoId`;
  - join requests: the cached profile.

  Add a test tag on the photo layer. Verify with previews for photo, loading and fallback, and with
  `assembleDebug`.
- [ ] 4.4 Add a `PhotoPicker` interface with our own `expect`/`actual`, bound in Koin. It returns the
  picked image's bytes, or nothing when the user cancels:
  - Android: the system Photo Picker (`PickVisualMedia`, images only) through an activity result
    launcher, read with the `ContentResolver`;
  - iOS: `PHPickerViewController` (images, one item), loading the item as `public.jpeg` so a HEIC
    photo arrives as JPEG.

  In `e2eModule`, bind a fake that returns a bundled test image. Verify that the app builds on both
  platforms and that the fake is used in 5.3.
- [ ] 4.5 Add our own circular crop in common Compose:
  - the picked image under a fixed circle, moved and zoomed with `transformable`, never letting the
    circle leave the image;
  - *Cancel* and *Use*;
  - on *Use*, the square behind the circle drawn into a 384×384 `ImageBitmap`, handed to the
    encoder (4.1).

  Keep the visible-square computation a pure function. Verify with unit tests: initial fit,
  clamping at the edges, maximum zoom, and the source rect for a known pan and zoom.

## 5. Profile screen

- [ ] 5.1 Extend `MyProfileUi` and `MyProfileViewModel` as in design D8:
  - `photoId`, `avatarMenuOpen` and `PhotoStep` (`Idle | Cropping | Uploading`);
  - upload, remove and cancel;
  - `ProfileMessage.PhotoUploadFailed` and `PhotoRemoveFailed`;
  - unsaved name, username and animal edits survive a photo change.

  Verify with ViewModel tests: upload success, upload failure, cancelled crop, remove, remove
  failure, and an unsaved name kept across an upload.
- [ ] 5.2 Update `MyProfileScreen`:
  - a 96 dp avatar that opens a menu: *Upload photo*, *Choose animal and colour*, and *Remove photo*
    only when there is a photo;
  - the crop from 4.5 between the picker and the upload;
  - a spinner on the avatar while uploading;
  - snackbars for the failures;
  - test tags for the robot;
  - every new string in `values`, `values-es`, `values-fr`, `values-de` and `values-it`.

  Verify with previews of each state, and manually on the Android emulator.
- [ ] 5.3 Add `ProfileRobot` and `ProfilePhotoFlowsTest`:
  - upload: the avatar shows the photo, the file is in Storage, `photoId` is on the profile and on
    Urgencias' member entry, and `profile_photo_uploaded` is logged (`awaitLogged`);
  - remove: the animal is back and the file is deleted;
  - with `profile-photo.json`, alice sees bruno's photo on Urgencias' member list and on his swap row;
  - an admin renaming the group keeps bruno's photo;
  - an upload with the network cut shows the failure message, and the avatar and the stored data are
    unchanged.

  Verify all pass on the emulators, with the emulator turn.

## 6. Integration

- [ ] 6.1 Once TUR-21 (PR #58) has merged, merge `origin/main` into this branch and change
  `.github/workflows/android-e2e.yml` to
  `firebase emulators:exec --only auth,firestore,functions,storage`. Verify with
  `gh workflow run android-e2e.yml --ref features/tur-56-profile-image` that the whole suite is green.
- [ ] 6.2 Run:
  - `./gradlew :core:allTests`, the shared unit tests and `./gradlew :app:androidApp:assembleDebug`;
  - the iOS simulator build;
  - the full E2E suite.

  Update the E2E count in `CLAUDE.md`. Verify all are green.
- [ ] 6.3 On the iOS simulator, upload, crop and remove a photo and see a colleague's. Verify
  manually and note the result here.
- [ ] 6.4 Take before and after screenshots of the profile screen, the avatar menu, the crop dialog,
  a profile with a photo, and a group member list with photos. Verify they are saved under the
  change's `screenshots/`.

## Workflow follow-up

- Ask the director for the emulator turn before any `firebase emulators:*` or `connected…AndroidTest`
  run, and give it back afterwards.
- Before deploying, the developer creates the default Storage bucket in the console, picking its
  location, and deploys `firestore,functions,storage`. Rules and functions go before any client.
- No store release carries this change before TUR-60 lands. The developer confirms this in the
  `## Proposal`.
- Archive the change as the PR's last commit, once the developer accepts it.
