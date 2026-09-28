# Spec Delta

## Purpose

Asks a user who has started using Turnia on their own whether they work with a team, once, so that
someone who never creates or joins a group still finds out that swaps and change chains exist.

## ADDED Requirements

### Requirement: The team prompt is due once the user has added enough events
The system SHALL make the team prompt due when all of these hold:
- the team prompt is enabled in Remote Config;
- the number of events added on the device has reached the Remote Config threshold;
- the user belongs to no group, counting only groups where they are a member, not revoked;
- the team prompt has never been shown on this device.

The count SHALL be the same device-wide count of events added that the share prompt uses: every new
event counts, typed or one-off, and an edit does not. The system SHALL decide group membership from
the backend's answer, never from an empty local cache alone. When it cannot get that answer, the
prompt SHALL NOT be due, and the system SHALL decide again on the next event added.

#### Scenario: Reaching the threshold with no group
- **WHEN** the threshold is 5, the user belongs to no group and adds their fifth event on the device
- **THEN** the team prompt is due

#### Scenario: Below the threshold
- **WHEN** the threshold is 5 and the user adds their fourth event
- **THEN** the team prompt is not due

#### Scenario: A group member never sees it
- **WHEN** a user who belongs to a group adds the event that reaches the threshold
- **THEN** the team prompt is not due, and it is not due on any later event either

#### Scenario: A revoked group does not count
- **WHEN** the user's only group is one where they are revoked and they reach the threshold
- **THEN** the team prompt is due

#### Scenario: Offline at the threshold
- **WHEN** the user reaches the threshold with no connection and their membership is unknown
- **THEN** the team prompt is not due on that event, and is decided again on the next event added

#### Scenario: Not on first launch
- **WHEN** a new user signs in for the first time and has added no event
- **THEN** the team prompt is not shown

### Requirement: The team prompt is configured remotely and ships off
The system SHALL read two Remote Config values:
- `teamPromptEnabled`, a boolean, `false` when the console sets nothing;
- `teamPromptThreshold`, a whole number of events added, `5` when the console sets nothing.

A threshold below 1 SHALL be read as disabling the prompt.

#### Scenario: Defaults
- **WHEN** the console sets neither value
- **THEN** the team prompt is never due

#### Scenario: Enabled with the default threshold
- **WHEN** the console sets `teamPromptEnabled` to `true` and nothing else
- **THEN** the team prompt becomes due on the fifth event added

### Requirement: The team prompt shows only on the user's own calendar
The system SHALL show a due team prompt only on the user's own calendar, and only while no other
sheet of that calendar is open. A team prompt that becomes due elsewhere SHALL wait until the user's
own calendar is open.

#### Scenario: Due while on a group calendar
- **WHEN** the team prompt is due and the user is looking at a group's calendar
- **THEN** it is not shown there, and it is shown when the user opens their own calendar

#### Scenario: Not over the day that triggered it
- **WHEN** the event that makes the prompt due is added from a day's sheet
- **THEN** the team prompt appears only once that sheet has closed

### Requirement: The team prompt shows once per device
Once the team prompt has been on screen, the system SHALL NOT show it again on this device, whatever
the answer. It SHALL remember this on the device only, and SHALL write nothing to the backend. A
prompt that was due but never reached the screen SHALL still be shown later.

#### Scenario: Dismissed
- **WHEN** the user dismisses the team prompt and keeps adding events
- **THEN** the team prompt never appears again on that device

#### Scenario: App closed before it showed
- **WHEN** the team prompt becomes due and the app is closed before it reaches the screen
- **THEN** it is shown on the user's own calendar after a later event is added

#### Scenario: Shown across a rotation
- **WHEN** the team prompt is on screen and the device is rotated
- **THEN** the prompt is still on screen and counts as shown once

### Requirement: The team prompt offers three answers
The team prompt SHALL ask whether the user works with a team and offer:
- **Create a group**, which opens the new-group form;
- **I have a code**, which opens the Groups tab with the join sheet open and its code field empty;
- **Not now**, which closes the prompt.

Closing the prompt by any other means, such as swiping it away or pressing back, SHALL count as
Not now. Its text SHALL be in the app's current language, in all five supported languages.

#### Scenario: Create a group
- **WHEN** the user answers Create a group
- **THEN** the prompt closes and the new-group form opens

#### Scenario: I have a code
- **WHEN** the user answers I have a code
- **THEN** the prompt closes, the Groups tab is selected and the join sheet is open with an empty code

#### Scenario: Swiped away
- **WHEN** the user swipes the prompt down
- **THEN** it is answered as Not now

### Requirement: The team prompt takes precedence over the share prompt
When the team prompt and a share prompt are both due on the user's own calendar, the system SHALL
show only the team prompt. The share prompt SHALL stay due, its milestone unspent, and SHALL be shown
the next time a calendar where it can appear is opened, never straight after the team prompt closes.
A prompt already on screen SHALL keep it: when the team prompt becomes due while a share prompt is
showing, the share prompt stays, and the team prompt waits for the next time the user's own calendar
is opened.

#### Scenario: Both due on the same event
- **WHEN** the event added reaches both the team prompt threshold and a share prompt milestone
- **THEN** only the team prompt is shown
- **THEN** after the user answers it, no share prompt appears until a calendar is opened again

#### Scenario: The share prompt waits for the next open
- **WHEN** the team prompt was shown and answered, and the user later switches to another tab and back to their calendar
- **THEN** the share prompt is shown

#### Scenario: The team prompt does not interrupt a share prompt
- **WHEN** a share prompt is on screen and the team prompt only then becomes due, because the server answered late
- **THEN** the share prompt stays on screen and the team prompt is not shown
- **THEN** after the user answers the share prompt, the team prompt appears only once the calendar is opened again
