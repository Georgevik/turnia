# Spec Delta

## Purpose

Decides which builds and devices report to Google Analytics, so its numbers describe real users
only and never development runs, automated tests or store review robots.

## ADDED Requirements

### Requirement: Debug builds never report to Analytics
A debug build SHALL NOT send any analytics data — automatic events (such as `first_open`,
`session_start`, `screen_view`, `user_engagement`) or the app's own events — on Android or iOS.
Nothing the app does at runtime SHALL be able to turn collection on in a debug build.

#### Scenario: A development run
- **WHEN** a developer installs and uses a debug build on a device or simulator
- **THEN** no event from that device appears in Google Analytics, including DebugView

#### Scenario: The E2E suite
- **WHEN** the E2E suite runs the debug build, clearing the app's data before every test, locally or
  from the AndroidE2E or Release workflow
- **THEN** no `first_open` or other event from those runs appears in Google Analytics

#### Scenario: Something tries to enable collection
- **WHEN** code running in a debug build asks the SDK to enable analytics collection
- **THEN** collection stays off

### Requirement: Release builds on Test Lab devices do not report to Analytics
A release build running on a Firebase Test Lab device SHALL NOT send any analytics data. This covers
the robots of Google Play's pre-launch report. No event SHALL be sent before the app has decided
whether it is on Test Lab, including on its first launch.

#### Scenario: The pre-launch report crawls an upload
- **WHEN** a release build uploaded to a Play testing track is launched by the pre-launch report on
  a Test Lab device
- **THEN** no event from that device, `first_open` included, appears in Google Analytics

### Requirement: Real users of release builds keep reporting
A release build on any device that is not a Test Lab device SHALL collect the same automatic and
custom events it collects today, from its first launch on and on every later launch.

#### Scenario: A first install from the store
- **WHEN** a user installs a release build on their own device and opens it
- **THEN** `first_open` and the following automatic and custom events reach Google Analytics

#### Scenario: A later launch
- **WHEN** that user opens the app again on a later day
- **THEN** its events still reach Google Analytics
