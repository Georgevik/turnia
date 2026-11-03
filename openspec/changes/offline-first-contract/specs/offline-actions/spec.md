# Spec Delta

## Purpose

States what every user action does with no connection: which actions work locally at once, which
need a connection and how they say so, and what a screen shows when it cannot reach the server. It
is the contract the code and the tests obey offline.

## ADDED Requirements

### Requirement: Event actions work locally at once

With no connection, these SHALL take effect at once, as `offline-event-visibility` requires:
- adding, editing and deleting an event of any kind;
- writing a note, on a personal event or as one's private note on a group event;
- an admin deleting a group event;
- offering a shift for swap and withdrawing the offer.

Colleagues SHALL see an offer, and get its push, once the device reconnects.

#### Scenario: Offering a shift in airplane mode

- **WHEN** the user with no connection offers one of their group shifts for swap
- **THEN** the shift shows as offered straight away, with no message and no loading state

#### Scenario: The offer reaches colleagues on reconnect

- **WHEN** the user who offered a shift offline turns airplane mode off
- **THEN** the shift appears in the group's swap list on a colleague's device, and the colleague gets the push

#### Scenario: Withdrawing an offer in airplane mode

- **WHEN** the user with no connection withdraws the offer of one of their shifts
- **THEN** the shift shows as no longer offered straight away

### Requirement: Shift types and the shift setup work locally at once

With no connection, creating, editing or deleting a shift type of the user's own, and confirming the
shift setup, SHALL finish at once. The screen SHALL close as it does online, with no spinner, and the
result SHALL be usable straight away. Picking one's own colour for a group's event type SHALL show
the new colour at once.

#### Scenario: Saving a shift type in airplane mode

- **WHEN** the user with no connection fills the form for a new shift of their own and saves
- **THEN** the form closes straight away and the new shift is offered in the add pane

#### Scenario: Confirming the shift setup in airplane mode

- **WHEN** a new user with no connection confirms the shift setup with three shifts selected
- **THEN** the setup closes onto the calendar straight away and the three shifts are offered in the add pane

#### Scenario: One's own colour for a group type in airplane mode

- **WHEN** the user with no connection picks a new colour for one of a group's event types
- **THEN** the picker shows the new colour straight away, and so do that type's shifts on the user's calendar

### Requirement: Profile and preferences work locally at once

With no connection, these SHALL take effect at once, with no spinner and no error:
- saving the avatar's animal and background colour;
- turning notifications on or off;
- hiding a calendar shared with the user, or showing it again;
- choosing the app language.

#### Scenario: Saving the avatar in airplane mode

- **WHEN** the user with no connection picks another animal and colour and saves their profile
- **THEN** the profile closes straight away and their avatar shows the new animal and colour

#### Scenario: Hiding a shared calendar in airplane mode

- **WHEN** the user with no connection hides a calendar shared with them
- **THEN** it moves to the *Hidden* chip straight away

### Requirement: Signing out works at once offline

Signing out SHALL complete at once with no connection, and SHALL show no warning about changes not yet
uploaded. Those changes SHALL stay on the device and SHALL upload the next time the same account signs
in on that device.

#### Scenario: Signing out in airplane mode

- **WHEN** the user with no connection signs out
- **THEN** the app reaches the sign-in screen straight away, with no warning

#### Scenario: Unsent changes wait for the same account

- **WHEN** the user adds a shift with no connection, signs out, regains a connection and signs in again with the same account on that device
- **THEN** the shift is uploaded and appears exactly once

### Requirement: A local change is reported only when the server rejects it

A change made by an action that works locally SHALL look the same as one the server has taken, before
and after upload, with no pending marker and no error. It SHALL be uploaded on reconnect without the
user acting. The user SHALL be told only when the server rejects it.

#### Scenario: Nothing is said while the change is unsent

- **WHEN** the user saves a shift type with no connection and keeps using the app offline
- **THEN** nothing on screen says the type is unsent

#### Scenario: A rejected change is reported

- **WHEN** the user deletes a shift offline that a colleague took meanwhile, and then reconnects
- **THEN** the user is told the change could not be made

### Requirement: Group and membership actions need a connection

These SHALL need a connection:
- creating a group;
- editing an existing group: its name, colour, invitation code or auto-approve, or adding, editing
  or removing one of its event types;
- joining with an invitation code;
- accepting or rejecting a join request;
- leaving a group, removing a member and deleting a group.

An event type for a group not yet created SHALL stay local until the group is created.

#### Scenario: Creating a group in airplane mode

- **WHEN** the user with no connection names a new group and saves it
- **THEN** the message *No connection* is shown and no group is created, then or on reconnect

#### Scenario: Editing a group's event type in airplane mode

- **WHEN** an admin with no connection edits one of an existing group's event types and saves
- **THEN** the message *No connection* is shown and the group keeps its types unchanged

#### Scenario: A type for a group being created

- **WHEN** the user with no connection adds an event type to a group they are still creating
- **THEN** the type is listed on the group's form straight away

### Requirement: Shift transfer and move actions need a connection

Taking a shift offered for swap, giving a shift back, and moving personal shifts to a group SHALL need
a connection.

#### Scenario: Taking a shift in airplane mode

- **WHEN** the user with no connection takes a shift offered for swap
- **THEN** the message *No connection* is shown and the shift stays with its holder

#### Scenario: Moving a shift to a group in airplane mode

- **WHEN** the user with no connection moves a personal shift to a group
- **THEN** the message *No connection* is shown and the shift stays personal

### Requirement: Account, sharing and people actions need a connection

These SHALL need a connection:
- changing the name or the username, including the name a new user is asked for;
- uploading or removing the profile photo;
- searching people;
- sharing one's calendar and stopping sharing it;
- deleting the account;
- signing in, creating an account and resetting a password.

#### Scenario: Stopping sharing in airplane mode

- **WHEN** the user with no connection stops sharing their calendar with someone
- **THEN** the message *No connection* is shown and that person is still listed as able to see the calendar

#### Scenario: Changing the username in airplane mode

- **WHEN** the user with no connection changes their username and saves
- **THEN** the message *No connection* is shown and the profile keeps the username typed

### Requirement: An action that needs a connection says so

The button of an action that needs a connection SHALL stay enabled. With no network, tapping it SHALL
show *No connection* at once, with no wait for a timeout. With a network that does not answer, it
SHALL show *No connection* after 15 seconds at most, and the profile photo upload after 30 seconds at most. *No connection*
SHALL be the same for every action and distinct from a generic failure.

#### Scenario: No network at all

- **WHEN** the device has no network and the user taps the button of an action that needs a connection
- **THEN** *No connection* is shown within two seconds, and no loading state is left on screen

#### Scenario: A network that does not answer

- **WHEN** the device has a network that answers nothing and the user takes a shift
- **THEN** *No connection* is shown within 15 seconds and the loading state ends

#### Scenario: The button is available offline

- **WHEN** the device has no network and the user opens a screen with an action that needs a connection
- **THEN** that action's button is enabled

### Requirement: An action that needs a connection keeps what the user entered

When an action that needs a connection cannot reach the server, the screen SHALL stay open with
everything the user entered intact, and SHALL accept the same action again.

#### Scenario: Retrying a group creation

- **WHEN** the user's group creation shows *No connection*, and the user reconnects and saves again
- **THEN** the form still holds the name, colour and event types they entered, and the group is created once

### Requirement: An action that needs a connection never runs later on its own

An action that needs a connection and could not reach the server SHALL NOT be queued: it SHALL NOT take
effect when the connection returns unless the user performs it again.

#### Scenario: A take attempted offline

- **WHEN** the user tries to take a shift with no connection, sees *No connection*, and then reconnects without trying again
- **THEN** the shift is still not theirs

### Requirement: A screen shows what the device holds

With no connection, every screen SHALL show what the device already holds, with no marker that it may be
out of date. Opening the app with no connection SHALL reach the calendar for a signed-in user, whatever
the app was waiting on at launch.

#### Scenario: A group opened before

- **WHEN** the user with no connection opens a group whose screen they have opened before
- **THEN** its name, members and event types are shown as last held, with no error

#### Scenario: Launching offline

- **WHEN** a signed-in user opens the app with no connection
- **THEN** the app reaches the calendar

### Requirement: A screen with nothing cached says No connection

When a screen, or part of one, has nothing on the device and cannot reach the server, it SHALL show
*No connection* there instead of an empty state, a "nothing here" message or an endless loader. When
the connection returns, an open screen SHALL fill in without the user asking to retry.

#### Scenario: A colleague's month never opened

- **WHEN** the user with no connection opens a month of a colleague's calendar that this device never loaded
- **THEN** *No connection* is shown for that month, and the months the device holds are shown as usual

#### Scenario: Searching people in airplane mode

- **WHEN** the user with no connection searches for a person
- **THEN** *No connection* is shown instead of "no results"

#### Scenario: Filling in on reconnect

- **WHEN** a screen showing *No connection* is open and the connection returns
- **THEN** the screen loads its content without the user tapping anything
