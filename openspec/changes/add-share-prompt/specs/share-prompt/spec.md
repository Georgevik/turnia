# Spec Delta

## Purpose

When and how the app asks a user to share Turnia with coworkers or friends, what the shared link
carries so an install can be attributed, and what the prompt reports to analytics.

## ADDED Requirements

### Requirement: The prompt is controlled remotely
The system SHALL show the share prompt only while the remote flag `sharePromptEnabled` is true. It
SHALL read its milestones from the remote parameter `sharePromptMilestones`, a JSON array of
positive integers. A missing, empty or malformed value SHALL mean no milestones, and SHALL never
crash the app. Both SHALL default to off and no milestones when the app has never fetched them.

#### Scenario: The flag is off
- **WHEN** `sharePromptEnabled` is false and the user adds more events than any milestone
- **THEN** the prompt is never shown

#### Scenario: The milestones are malformed
- **WHEN** `sharePromptEnabled` is true and `sharePromptMilestones` is `"10, 100"` (not a JSON array)
- **THEN** the prompt is never shown and the app keeps working

### Requirement: Added events are counted on the device
The system SHALL count every new event the user adds: a group event, a personal typed event or a
personal one-off event. Editing or deleting an event SHALL NOT count. The count SHALL be stored on
the device, SHALL survive the app being closed, SHALL keep counting while the flag is off, and SHALL
involve no Firestore read or write.

#### Scenario: Editing does not count
- **WHEN** the milestones are `[2]` and the user adds one one-off event and edits it twice
- **THEN** the prompt is not shown

#### Scenario: Counting while the feature is off
- **WHEN** the user adds 150 events with the flag off, the flag is then turned on with milestones
  `[10, 100, 200]`, and the user adds one more event
- **THEN** the prompt for milestone 100 is shown

### Requirement: Each milestone shows the prompt once
When an event is added and the flag is on, the system SHALL show the prompt if a milestone lies
above the last milestone shown and at or below the number of events added. It SHALL show it for the
highest such milestone only, and SHALL remember that milestone's value as the last one shown. A
milestone SHALL be shown once whether the user shares or dismisses it, and a later milestone SHALL
still be shown after either answer.

#### Scenario: Milestones in order
- **WHEN** the milestones are `[2, 4]` and the user adds four events
- **THEN** the prompt shows after the second and after the fourth event, and not after the first or
  the third

#### Scenario: Dismissing does not stop later milestones
- **WHEN** the milestones are `[2, 4]` and the user dismisses the prompt at the second event
- **THEN** it does not show at the third event and shows again at the fourth

#### Scenario: Several milestones passed at once
- **WHEN** the user has added 150 events, no milestone has been shown, and the milestones are
  `[10, 100, 200]`
- **THEN** one prompt is shown, for milestone 100, and the next one shown is 200

#### Scenario: Milestones edited remotely
- **WHEN** milestone 100 was the last shown, the user has added 150 events, and the milestones are
  changed to `[10, 50, 300]`
- **THEN** no prompt is shown until the user reaches 300 events

### Requirement: The message fits the event that reached the milestone
The system SHALL address **coworkers** when the event that reached the milestone has a type (a group
event or a personal typed event), and **friends** when it is a one-off event. The earlier events
SHALL NOT affect the choice.

#### Scenario: Shifts
- **WHEN** the milestone is reached by adding a group or personal typed event
- **THEN** the prompt invites the user to share Turnia with their coworkers

#### Scenario: Plans
- **WHEN** the milestone is reached by adding a one-off event
- **THEN** the prompt invites the user to share Turnia with their friends

#### Scenario: Mixed use
- **WHEN** the milestones are `[3]` and the user adds two typed events and then one one-off
- **THEN** the prompt addresses friends

### Requirement: Sharing opens the platform's share sheet with a tagged link
The prompt SHALL offer one action to share and one to dismiss. Sharing SHALL open the platform's
share sheet with a short message in the app's language and the link
`https://turnia.club/?utm_source=turnia_share&utm_medium=share_prompt&utm_campaign=<audience>`, where
the audience is `coworkers` or `friends`. The landing page SHALL send the visitor to their platform's
store and SHALL pass the `utm_*` parameters to Google Play's install referrer.

#### Scenario: Sharing with coworkers
- **WHEN** the prompt addresses coworkers and the user taps the share action
- **THEN** the share sheet opens with the coworkers message and a link whose `utm_campaign` is
  `coworkers`

#### Scenario: A recipient on Android
- **WHEN** someone opens the shared link on an Android phone and taps the Google Play badge
- **THEN** Google Play opens Turnia's listing with the `utm_*` parameters in its `referrer`

### Requirement: The prompt reports to analytics
The system SHALL log `share_prompt_shown` when the prompt appears, `share_prompt_shared` when the user
taps the share action, and `share_prompt_dismissed` when the user dismisses it. Each SHALL carry the
parameters `audience` (`coworkers` or `friends`) and `milestone`. The system SHALL also log `sign_up`,
with the sign-in `method`, when a new account is created.

#### Scenario: A prompt shared
- **WHEN** the prompt for milestone 10 addresses friends and the user taps the share action
- **THEN** `share_prompt_shown` and then `share_prompt_shared` are logged, both with
  `audience = friends` and `milestone = 10`

#### Scenario: A new account
- **WHEN** a user signs in with Google for the first time and their account is created
- **THEN** `sign_up` is logged with `method = google`, and a returning user signing in logs no
  `sign_up`
