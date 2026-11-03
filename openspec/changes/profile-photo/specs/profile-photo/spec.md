# Spec Delta

## Purpose

Lets a user show a real photo of themselves instead of their animal avatar, the same photo to
everyone who can see them, without giving up the animal as the fallback.

## ADDED Requirements

### Requirement: A user can upload a profile photo
From their profile screen, the user SHALL be able to pick an image with the system photo picker,
crop it to a circle with pan and pinch, and confirm it. The photo SHALL take effect as soon as it is
confirmed, without *Save*, and SHALL NOT discard unsaved edits to the name, username or animal. The
picker SHALL NOT ask for any permission, and no camera SHALL be offered.

#### Scenario: Uploading a photo
- **WHEN** the user taps their avatar, chooses *Upload photo*, picks an image, frames their face and confirms
- **THEN** the profile shows that photo in a circle and the animal is no longer shown

#### Scenario: Cancelling the crop
- **WHEN** the user picks an image and cancels the crop
- **THEN** nothing is uploaded and the avatar stays as it was

#### Scenario: Unsaved edits survive an upload
- **WHEN** the user edits their name, then uploads a photo before tapping *Save*
- **THEN** the edited name is still in the field and is saved by *Save*

### Requirement: A photo is compressed on the device
Before upload, the photo SHALL be re-encoded as a square WebP of 384×384 pixels at quality 80, with
no metadata from the original image, including its location. The backend SHALL refuse a photo that
is larger than 100 KB or is not `image/webp`, whoever sends it.

#### Scenario: A large photo from the gallery
- **WHEN** the user uploads a 4000×3000 photo of several megabytes
- **THEN** the stored photo is a 384×384 WebP of at most 100 KB

#### Scenario: Location metadata
- **WHEN** the original image carries GPS coordinates in its metadata
- **THEN** the stored photo carries no location

#### Scenario: An oversized upload that bypasses the app
- **WHEN** a client tries to store a 2 MB file, or a JPEG, as a profile photo
- **THEN** the backend refuses it

### Requirement: The photo is shown in place of the animal
Wherever a person's avatar is shown, the system SHALL show their photo when they have one, and their
animal on its colour otherwise. The animal SHALL also show while the photo is loading and when it
cannot be loaded, offline included. Both SHALL be drawn as a circle of the same size.

#### Scenario: A colleague with a photo
- **WHEN** the user opens a group whose member Bruno has a photo
- **THEN** Bruno's row shows his photo

#### Scenario: Offline with no cached photo
- **WHEN** the device is offline and has never downloaded Bruno's photo
- **THEN** Bruno's row shows his animal

### Requirement: A user can remove their photo
When the user has a photo, the avatar menu SHALL offer *Remove photo*. Removing it SHALL take effect
at once, without *Save*, and SHALL bring the animal back everywhere the photo was shown. The removed
photo SHALL be deleted from the backend.

#### Scenario: Removing the photo
- **WHEN** the user chooses *Remove photo*
- **THEN** their avatar shows their animal again, and the menu no longer offers *Remove photo*

#### Scenario: No photo to remove
- **WHEN** the user has no photo and taps their avatar
- **THEN** the menu offers *Upload photo* and *Choose animal and colour*, not *Remove photo*

### Requirement: Only the current photo is kept
The backend SHALL keep at most one photo per user. Uploading a new one SHALL delete the previous
one, and a file left behind by an upload that never finished SHALL be deleted the next time the
user sets or removes a photo.

#### Scenario: Replacing a photo
- **WHEN** a user with a photo uploads another
- **THEN** only the new photo remains stored for that user

### Requirement: A photo is visible to signed-in users only
Any signed-in user SHALL be able to see any user's photo, as they can see the name and the animal.
A photo SHALL NOT be reachable without signing in, and no link SHALL open it in a browser. Only the
owner SHALL be able to upload a photo of theirs; nobody SHALL be able to overwrite another's.

#### Scenario: A stranger found by username
- **WHEN** the user searches a username and the result has a photo
- **THEN** the result shows that photo

#### Scenario: No session
- **WHEN** a request for a photo carries no signed-in user
- **THEN** it is refused

#### Scenario: Uploading as somebody else
- **WHEN** a signed-in user tries to store a photo under another user's account
- **THEN** it is refused

### Requirement: A new photo reaches colleagues without reloading the profile
When a user uploads or removes a photo, every group they are a member of SHALL carry the change, so
that members viewing that group see the new photo, or the animal, without re-reading the profile.
The photo SHALL NOT be changed by the client directly; it changes only through the action that keeps
those copies true.

#### Scenario: A colleague has the group open
- **WHEN** Alice uploads a photo while Bruno has their shared group's member list on screen
- **THEN** Bruno's list shows Alice's new photo without leaving the screen

#### Scenario: A member joins later
- **WHEN** a user with a photo is accepted into a group
- **THEN** the members of that group see their photo

#### Scenario: A rename keeps the photo
- **WHEN** a user with a photo changes their name
- **THEN** their colleagues see the new name and still see the photo

#### Scenario: An admin saves the group
- **WHEN** an admin changes the group's name while members have photos
- **THEN** every member still shows their photo

### Requirement: A failed upload changes nothing
If the upload or its publication fails, the offline case included, the system SHALL keep the
avatar the user had before and SHALL tell them the photo could not be uploaded.

#### Scenario: Uploading offline
- **WHEN** the user confirms a cropped photo with no connection
- **THEN** a message says the photo could not be uploaded, and the avatar is unchanged

### Requirement: Deleting the account deletes the photo
Deleting an account SHALL delete every photo stored for it, and SHALL remove the photo from its
profile and from every group's copy. Its avatar then renders as any former member's does.

#### Scenario: A deleted account with a photo
- **WHEN** a user with a photo deletes their account
- **THEN** no photo of theirs remains stored, and their former colleagues no longer see it
