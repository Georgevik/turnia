# analytics-events Specification

## Purpose

Defines which user actions Turnia reports to Google Analytics, what each report carries, and the
guarantee that no report identifies a person. Together they show how people actually use the app.

## Requirements

### Requirement: An action is reported only once it has succeeded
The system SHALL log an action's event only after the action has succeeded, and SHALL log it once
per action. An action that fails, or that is refused before it reaches the backend, SHALL log
nothing, except where this spec names a failure event.

#### Scenario: A failed write
- **WHEN** the user deletes a one-off event and the delete fails
- **THEN** no `one_off_event_deleted` is logged

#### Scenario: A refused delete
- **WHEN** a user who took a group shift tries to delete it and the app refuses because they are not its creator
- **THEN** no `group_event_deleted` is logged

### Requirement: Reports never identify a person
Event parameters and user property values SHALL NOT contain a user id, a group id, an event id, a
name, a username, an email, an invitation code, notes or any other free text. Every parameter SHALL
take one of a small, fixed set of values: a boolean, a small count, or one of the enumerated strings
this spec defines. The account is linked to its reports only through the Analytics user id the app
already sets.

#### Scenario: Notes saved
- **WHEN** the user saves notes on a personal event
- **THEN** `event_notes_saved` is logged with no parameter holding the notes

### Requirement: Existing event names are kept
The system SHALL keep logging `screen_view`, `sign_up`, `group_created`, `group_event_created`,
`join_group_requested`, `join_group`, `share_prompt_shown`, `share_prompt_shared` and
`share_prompt_dismissed` under those exact names. New parameters MAY be added to them; their names
SHALL NOT change.

#### Scenario: A group created after this change
- **WHEN** a user creates a group
- **THEN** the event is still named `group_created`

### Requirement: Personal events are reported
The system SHALL log `personal_event_created` when the user adds an event of one of their personal
event types, and `personal_event_deleted` when they delete one. It SHALL log `event_notes_saved`
when the user saves notes on a personal event.

#### Scenario: A personal event added
- **WHEN** the user adds an event of their personal type "Gym" on a day
- **THEN** `personal_event_created` is logged

#### Scenario: A personal event deleted
- **WHEN** the user deletes one of their personal typed events
- **THEN** `personal_event_deleted` is logged

### Requirement: One-off events are reported
The system SHALL log `one_off_event_created` when the user adds a personal one-off event, with the
parameters `all_day` (whether it is all day) and `multi_month` (whether it spans more than one
calendar month). It SHALL log `one_off_event_updated` when the user saves a change to one, and
`one_off_event_deleted` when they delete one.

#### Scenario: A multi-month all-day one-off
- **WHEN** the user adds an all-day one-off event from 28 January to 3 February
- **THEN** `one_off_event_created` is logged with `all_day = true` and `multi_month = true`

#### Scenario: A one-off edited
- **WHEN** the user changes the name of an existing one-off event and saves it
- **THEN** `one_off_event_updated` is logged, and no `one_off_event_created`

### Requirement: Group events are reported
The system SHALL keep logging `group_event_created` when the user adds a group event. It SHALL log
`group_event_deleted` when the user deletes a group event.

#### Scenario: A group event deleted
- **WHEN** the creator of a group shift who still holds it deletes it
- **THEN** `group_event_deleted` is logged

### Requirement: Event types are reported when created
The system SHALL log `personal_event_type_created` when the user creates a personal event type, and
`group_event_type_created` for each group event type that is created. That includes the types a
group is created with. Editing an existing type SHALL log neither.

#### Scenario: A group created with two types
- **WHEN** an admin creates a group with two event types
- **THEN** `group_event_type_created` is logged twice, and `group_created` once

#### Scenario: A personal type edited
- **WHEN** the user changes the colour of an existing personal event type and saves it
- **THEN** no `personal_event_type_created` is logged

### Requirement: The swap cycle is reported
The system SHALL log:
- `swap_offered` when the assignee puts a group event up for swap;
- `swap_withdrawn` when they take it back off swap;
- `swap_taken` when a member takes an event offered for swap;
- `swap_returned` when whoever took a shift gives it back.

When taking an event fails because somebody else took it first, the system SHALL log
`swap_take_lost`. Other take failures SHALL log nothing.

#### Scenario: A shift offered and taken
- **WHEN** A offers their shift and B takes it
- **THEN** `swap_offered` is logged on A's device and `swap_taken` on B's

#### Scenario: Two members race for one shift
- **WHEN** B and C tap to take the same offered shift and B's take commits first
- **THEN** `swap_taken` is logged on B's device and `swap_take_lost` on C's

#### Scenario: A shift given back
- **WHEN** B gives back a shift they took from A
- **THEN** `swap_returned` is logged

### Requirement: Group growth is reported
The system SHALL log `group_created` with the parameters `auto_approve` (whether auto-approve is
on) and `type_count` (how many event types it was created with). It SHALL log:
- `group_invite_shared` when a member opens the system share sheet from a group's share button;
- `invitation_opened` when the app receives a valid invitation code, with `via = link` when an
  invitation link opened the app and `via = install_referrer` when the code arrived through the
  install referrer on first launch;
- `join_request_rejected` when an admin rejects a join request.

`join_group_requested` and `join_group` SHALL keep their current meaning.

#### Scenario: A group created with auto-approve
- **WHEN** an admin creates a group with auto-approve on and one event type
- **THEN** `group_created` is logged with `auto_approve = true` and `type_count = 1`

#### Scenario: An invitation link opened
- **WHEN** a user taps `https://turnia.club/join/CODE` with the app installed
- **THEN** `invitation_opened` is logged with `via = link`

#### Scenario: A link that carries no valid code
- **WHEN** the app is opened with a link whose code is not valid
- **THEN** no `invitation_opened` is logged

### Requirement: Shared calendars are reported
The system SHALL log:
- `calendar_shared` when the user grants another user access to their calendar;
- `calendar_share_revoked` when they withdraw it;
- `shared_calendar_viewed` each time the user opens a colleague's shared calendar;
- `shared_calendar_hidden` when they hide a calendar shared with them.

Opening a group's calendar SHALL NOT log `shared_calendar_viewed`.

#### Scenario: A colleague's calendar opened
- **WHEN** the user opens a calendar someone shared with them
- **THEN** `shared_calendar_viewed` is logged

#### Scenario: A group's calendar opened
- **WHEN** the user opens a group's calendar
- **THEN** no `shared_calendar_viewed` is logged

### Requirement: Churn is reported
The system SHALL log `group_left` when the user leaves a group, `member_removed` when an admin
removes a member, `group_deleted` when a group is deleted, and `account_deleted` when the user's
account is deleted. `account_deleted` SHALL be logged while the session is still bound to the
account.

#### Scenario: An account deleted
- **WHEN** the user deletes their account and the deletion succeeds
- **THEN** `account_deleted` is logged before the user is signed out

#### Scenario: The last admin refused
- **WHEN** the only admin of a group with other members tries to leave it and is refused
- **THEN** no `group_left` is logged

### Requirement: Opened notifications are reported
The system SHALL log `notification_opened` when the user taps a push notification of a type the app
recognises. Its parameter `type` SHALL be one of `join_requested`, `join_accepted`,
`calendar_shared`, `event_on_swap` or `event_taken`. A tap on a notification of an unrecognised
type SHALL log nothing.

#### Scenario: A swap notification tapped
- **WHEN** the user taps the push saying a shift is on swap
- **THEN** `notification_opened` is logged with `type = event_on_swap`

### Requirement: Sign-ins are reported
The system SHALL log `login` with the parameter `method` (`google`, `apple`, `email` or `other`)
when a user signs in to an account that already existed. A session restored at launch SHALL NOT log
`login`. A sign-in that creates the account SHALL log `sign_up`, not `login`.

#### Scenario: A returning user signs in
- **WHEN** a signed-out user signs in with Apple to their existing account
- **THEN** `login` is logged with `method = apple`

#### Scenario: A restored session
- **WHEN** a signed-in user relaunches the app
- **THEN** no `login` is logged

#### Scenario: A brand-new account
- **WHEN** a user signs in with Google for the first time
- **THEN** `sign_up` is logged with `method = google` and no `login` is logged

### Requirement: User properties describe the user
The system SHALL set these user properties on the signed-in user:
- `group_count`: how many groups they are a member of, not counting groups where they are revoked;
- `is_admin`: whether they are an admin of at least one of those groups;
- `app_language`: the language picked in Preferences (`en`, `es`, `fr`, `de`, `it`), or `system`
  when the app follows the device.

The system SHALL update them when they change.

#### Scenario: Joining a first group
- **WHEN** a user with no groups is accepted into a group
- **THEN** `group_count` becomes `1` and `is_admin` stays `false`

#### Scenario: A language picked
- **WHEN** the user picks Italian in Preferences
- **THEN** `app_language` becomes `it`

### Requirement: The shift setup reports how it is used
The system SHALL log:
- `onboard_shift_shown` each time the shift setup is displayed. A rotation or recomposition of the
  same display SHALL NOT log it again.
- `onboard_shift_skipped` when the user skips it.
- `onboard_shift_completed` once the types it creates have been written.

All three SHALL carry `via`:
- `onboarding` when the setup was opened after sign-in;
- `add_pane` when it was opened from the add pane's empty state.

`onboard_shift_skipped` and `onboard_shift_completed` SHALL carry `interacted`. It is `true` when,
before skipping or confirming, the user did any of the following, and `false` otherwise:
- selected or deselected a shift;
- edited a time;
- added a shift of their own.

`onboard_shift_completed` SHALL also carry:
- `type_count`, the number of types created;
- `custom_type_count`, how many of them the user added inline.

Each type created SHALL also log `personal_event_type_created`. A failed write SHALL log neither
`onboard_shift_completed` nor `personal_event_type_created`.

#### Scenario: Confirming the defaults without touching anything
- **WHEN** the setup opens after sign-in and the user confirms it untouched
- **THEN** `onboard_shift_shown` is logged with `via = onboarding`
- **THEN** `onboard_shift_completed` is logged with `via = onboarding`, `interacted = false`,
  `type_count = 3` and `custom_type_count = 0`
- **THEN** `personal_event_type_created` is logged three times

#### Scenario: Toggling then skipping
- **WHEN** the user deselects Night and then skips
- **THEN** `onboard_shift_skipped` is logged with `interacted = true` and no type is created

#### Scenario: Adding a custom shift
- **WHEN** the user deselects Afternoon and Night, adds one shift of their own and confirms
- **THEN** `onboard_shift_completed` is logged with `interacted = true`, `type_count = 2` and
  `custom_type_count = 1`

#### Scenario: Shown once per display
- **WHEN** the setup is displayed and the device is rotated
- **THEN** `onboard_shift_shown` has been logged once

### Requirement: The first event added on a device reports its kind
The system SHALL log `first_event_added` when the first event is added on the device, with `kind`:
- `typed` for an event of a personal or group event type;
- `one_off` for a personal one-off event.

It SHALL be logged at most once per device, when the add is issued by a signed-in user, the same
moment the share prompt counts it. The count SHALL be the same device-wide one the share prompt uses. A device that had already added events
before this change SHALL NOT log it.

#### Scenario: First event is a shift
- **WHEN** a new user adds their first event and it is a shift
- **THEN** `first_event_added` is logged with `kind = typed`

#### Scenario: Only the first
- **WHEN** the user adds a second event
- **THEN** `first_event_added` is not logged again

### Requirement: The team prompt reports how it is answered
The system SHALL log `onboard_team_shown` the first time the team prompt is displayed on the device.
A rotation or recomposition of the same display SHALL NOT log it again.

It SHALL log `onboard_team_answered` once when the user answers it, with `choice`:
- `create` when they chose Create a group;
- `join` when they chose I have a code;
- `dismissed` when they chose Not now or closed the prompt any other way.

A share of the invitation link from the step shown after creating a group, or from the
single-member card, SHALL log `group_invite_shared`, as a share from the group's screen does.

#### Scenario: Prompt shown and dismissed
- **WHEN** the team prompt appears and the user swipes it away
- **THEN** `onboard_team_shown` is logged once and `onboard_team_answered` is logged with `choice = dismissed`

#### Scenario: Choosing to join
- **WHEN** the user answers I have a code
- **THEN** `onboard_team_answered` is logged with `choice = join`

#### Scenario: A double tap
- **WHEN** the user taps Create a group twice before the prompt closes
- **THEN** `onboard_team_answered` is logged once

#### Scenario: Inviting after creating
- **WHEN** the user shares the invitation link from the "Your group is ready" step
- **THEN** `group_invite_shared` is logged
