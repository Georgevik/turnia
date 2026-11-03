# Proposal

## Why

The only picture a user can show of themselves today is an animal on a background colour. In a team
that works shifts together, a real face is what tells colleagues who is who — on the member list, on
a swap offer, on a shared calendar. The profile screen already edits the name, the username and the
animal; what it cannot do is take a photo.

## What Changes

- **An optional profile photo.** A user can upload a photo from their profile screen. When they
  have one, it is shown wherever they appear instead of the animal. The animal and its colour stay
  as they are and remain the fallback: no photo, a photo still loading, no connection, a photo
  removed.
- **Picking and cropping.** The photo comes from the system photo picker, which asks for no
  permission, and is cropped to a circle with pan and pinch. There is no camera. The crop is a
  Compose Multiplatform library, not code of our own.
- **Compressed on the device.** The cropped photo is re-encoded as a 384×384 WebP at quality 80
  (about 15–30 KB), which drops its EXIF metadata, location included. Storage refuses anything over
  100 KB or not `image/webp`.
- **Firebase Storage joins the project.** Photos live at `profilePhotos/{uid}/{photoId}.webp`. Any
  signed-in user can read one, only its owner can upload, and only the server deletes. A photo is
  never reachable through a public link.
- **A keeper for the photo.** A new `setProfilePhoto` callable publishes or removes a photo. It
  writes `photoId` on the profile, copies it into every group the user is a member of, moves the
  sync markers, and deletes every other file in the user's folder. A colleague sees a new photo as
  soon as the group's listener delivers it, with no read of the profile.
- **Profile screen.** The avatar grows to 96 dp and opens a menu: *Upload photo*, *Choose animal and
  colour*, *Remove photo*. Uploading and removing apply at once. The animal, the name and the
  username still wait for *Save*. No new profile field.
- **Account deletion** also deletes the user's photos.
- **Analytics.** `profile_photo_uploaded`, with no parameters.
- **Emulators and E2E.** The Storage emulator joins the local and CI runs, the fixtures can seed a
  photo, and the E2E app replaces the system picker with one that returns a bundled image.
- **Out of scope**, moved to TUR-60: filtering photos with Cloud Vision SafeSearch, and reporting a
  photo. Also out of scope: refreshing a colleague's cached animal, which stays as stale as it is
  today.

## Capabilities

### New Capabilities
- `profile-photo`: the optional photo on a user's profile. Covers uploading, cropping, compression,
  who can see it, removing it, how it reaches colleagues, the fallback to the animal, and what account
  deletion does to it.

### Modified Capabilities
- `analytics-events`: adds `profile_photo_uploaded`.

## Impact

- **Firebase**:
  - Storage enters the project: the default bucket, `storage.rules`, and the `storage` emulator in
    `firebase.json`.
  - Firestore: `photoId` on `users/{uid}` and on each `groups/{g}.members.{uid}`. The rules freeze
    `photoId` for the client, as they already freeze `name` and `username`.
  - Functions:
    - a new `setProfilePhoto`;
    - `updateProfile` writes `members.{uid}.name` and `.username` as fields, no longer the whole entry;
    - `acceptJoinRequest` and the auto-approve branch of `requestToJoinGroup` copy the `photoId`;
    - `deleteAccount` deletes the user's Storage folder.
- **Client**:
  - `dev.gitlive:firebase-storage`, Coil 3, a photo picker and a crop library.
  - `UserAvatar` learns to draw a photo over the animal.
  - The profile screen and its ViewModel.
  - `GroupMemberDocument` and `GroupMember` gain `photoId`, so that an admin's group save keeps it.
  - A Storage block in the usage audit (`FirestoreAudit`).
- **iOS**: the `FirebaseStorage` package product linked in the Xcode project.
- **E2E and CI**:
  - the Storage emulator in `buildSrc/FirebaseEmulators.kt`, in the test app and in the fixtures;
  - new profile photo flows;
  - `--only auth,firestore,functions,storage` in `.github/workflows/android-e2e.yml`. That file
    belongs to TUR-21 (PR #58), so this edit lands **after TUR-21 merges**, once `origin/main` is
    merged into this branch.
- **Docs**:
  - the *Avatar* rule and *Sensitive points* in `CLAUDE.md`;
  - `firebase/firestore-schema.md`, `firebase/firestore-usage.md` and `firebase/test/README.md`.
- **Release**: photos ship with **no moderation** until TUR-60 lands. Apple's guideline 1.2 expects
  user-generated content to be filterable, reportable and blockable. The recommendation is that **no
  store release carries this change before TUR-60**. The alternative is shipping it with the upload
  hidden until then.
- **Console, done by the developer before deploying**: create the default Storage bucket, choosing
  its location, and deploy `storage.rules` along with the functions and Firestore rules.
