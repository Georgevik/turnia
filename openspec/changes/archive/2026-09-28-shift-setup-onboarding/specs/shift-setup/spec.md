# Spec Delta

## Purpose

Lets a new user define the shifts they work, once, right after signing in, so that adding an event
to their calendar starts from a reusable shift instead of a one-off.

## ADDED Requirements

### Requirement: The shift setup shows after sign-in to an account with nothing to reuse
After sign-in, the system SHALL show the shift setup screen over the calendar when all of these hold:
- the account has no personal event types;
- the account belongs to no group;
- no invitation code is waiting to be used;
- the setup has not been skipped on this device.

The system SHALL decide from the backend's answer, never from an empty local cache alone. When it
cannot get that answer, it SHALL NOT show the setup, and SHALL decide again on the next launch. When
the account still needs a name, the name SHALL be asked first and the setup SHALL follow.

#### Scenario: A new account lands on the setup
- **WHEN** a user creates an account and reaches the calendar
- **THEN** the shift setup screen is shown

#### Scenario: An account with personal types never sees it
- **WHEN** a user whose account already has a personal event type signs in on a fresh install
- **THEN** the shift setup screen is not shown, even though the device's cache starts empty

#### Scenario: A group member never sees it
- **WHEN** a user who belongs to a group, and has no personal event types, signs in
- **THEN** the shift setup screen is not shown

#### Scenario: An invitation takes precedence
- **WHEN** the app was opened by an invitation link and the user signs in with a new account
- **THEN** the shift setup screen is not shown and the join sheet opens as it does today

#### Scenario: Offline the setup waits
- **WHEN** a signed-in user launches the app with no connection and the account's state is unknown
- **THEN** the shift setup screen is not shown on that launch

### Requirement: The setup offers five presets, three of them selected
The setup SHALL list these presets, in this order and with these default times:

| Preset | Times | Selected by default |
|--------|-------|---------------------|
| Morning | 08:00–15:00 | yes |
| Afternoon | 15:00–22:00 | yes |
| Night | 22:00–08:00 | yes |
| Morning & afternoon | 08:00–22:00 | no |
| 24h duty | 08:00–08:00 | no |

Each preset SHALL show its name, its acronym and its times, all in the app's current language. In
Spanish the acronyms SHALL be M, T, N, MT and 24H. The setup SHALL NOT name any profession or sector.
Each preset SHALL carry a distinct color. The user SHALL be able to select or deselect any preset and
edit its start and end time. A time that ends at or before its start SHALL be read as ending the
following day.

#### Scenario: Default selection
- **WHEN** the setup opens
- **THEN** Morning, Afternoon and Night are selected, and Morning & afternoon and 24h duty are not

#### Scenario: Presets follow the app language
- **WHEN** the app language is Spanish and the setup opens
- **THEN** the presets read Mañana (M), Tarde (T), Noche (N), Mañana y tarde (MT) and Guardia 24h (24H)

#### Scenario: Editing a preset's time
- **WHEN** the user changes Morning's times to 07:00–14:00 and confirms
- **THEN** the created Morning type starts at 07:00 and ends at 14:00

### Requirement: The user can add their own shift inline
The setup SHALL let the user add a shift of their own without leaving the screen. It SHALL take a
name and an acronym, both required, and an optional start and end time, and SHALL assign a color not
already used by the setup's other shifts. A shift added this way SHALL be listed with the presets,
selected, and SHALL be deselectable like them.

#### Scenario: A custom shift is created with the rest
- **WHEN** the user adds "On call" with acronym "OC" and no times, then confirms
- **THEN** a personal event type "On call", acronym "OC", with no start or end time, is created
  along with the selected presets

#### Scenario: Name and acronym are required
- **WHEN** the user tries to add a custom shift with an empty name or an empty acronym
- **THEN** it is not added and the empty field is marked

### Requirement: Confirming creates the selected shifts as personal event types
Confirming SHALL create one personal event type per selected shift, all in a single atomic write.
Names and acronyms SHALL be stored in the language shown at that moment, and SHALL NOT change when
the app language changes later. The setup SHALL NOT create a group or a group event type. The confirm
action SHALL be disabled while no shift is selected. After a successful write, the setup SHALL close
onto the calendar with a hint telling the user to tap a day to add a shift. If the write fails, the
setup SHALL stay open with the user's selection intact and show an error.

#### Scenario: Confirming with the defaults
- **WHEN** the user confirms without changing anything
- **THEN** exactly three personal event types, Morning, Afternoon and Night, exist on the account and
  the calendar is shown with the hint

#### Scenario: Nothing selected
- **WHEN** the user deselects every shift
- **THEN** the confirm action is disabled

#### Scenario: The write fails
- **WHEN** the user confirms and the write is rejected
- **THEN** the setup stays open with the same selection and an error message is shown

### Requirement: The setup can be skipped and is not shown again on that device
The setup SHALL offer a skip action. Skipping SHALL close it onto the calendar without creating
anything, and SHALL be remembered on the device only, with nothing written to the backend. Once
skipped, the setup SHALL NOT be shown again after sign-in on that device. A skip on one device SHALL
NOT affect another.

#### Scenario: Skip is remembered across launches
- **WHEN** the user skips the setup and relaunches the app
- **THEN** the setup is not shown and no personal event type was created

### Requirement: The setup can be opened again from the add pane
When opened from the add pane's empty state (see `event-add-pane`), the setup SHALL behave the same,
except that it SHALL NOT be subject to the sign-in conditions. Opening it SHALL close the day sheet,
and confirming SHALL return to the calendar with the same hint to tap a day.

#### Scenario: Opened after skipping
- **WHEN** a user who skipped the setup opens it from the add pane's empty state and confirms
- **THEN** the selected types are created, the calendar shows the hint, and the day's add pane lists
  them as options when the day is opened again
