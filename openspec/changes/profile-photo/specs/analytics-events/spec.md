# Spec Delta

## ADDED Requirements

### Requirement: Profile photo uploads are reported
The system SHALL log `profile_photo_uploaded`, with no parameters, once a photo the user uploaded has
been published. An upload that fails SHALL log nothing. Removing a photo, and choosing an animal or
a colour, SHALL log nothing.

#### Scenario: A successful upload
- **WHEN** the user confirms a cropped photo and it is published
- **THEN** `profile_photo_uploaded` is logged once

#### Scenario: A failed upload
- **WHEN** the upload fails because the device is offline
- **THEN** `profile_photo_uploaded` is not logged

#### Scenario: Removing the photo
- **WHEN** the user removes their photo
- **THEN** nothing is logged
