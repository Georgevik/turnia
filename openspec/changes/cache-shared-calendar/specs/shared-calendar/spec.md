# Spec Delta

## Purpose

How the app shows the calendar of a colleague who shared it with the viewer. It is loaded one
month at a time and kept on the device. While it is open it stays live, and the app reads from the
server only the documents that changed since the cache last caught up.

## ADDED Requirements

### Requirement: A colleague's calendar is requested per month
The system SHALL request a colleague's calendar one calendar month at a time. For a given month it
SHALL always ask for the same window: from 14 days before the month's first day to 14 days after its
last day. The window SHALL never be wider than the function's 92-day cap. The day of the month the
viewer happens to be on SHALL NOT change the window.

#### Scenario: Two dates in the same month make one request
- **WHEN** the calendar is opened on the 26th and the grid then reports the 1st of the same month
- **THEN** exactly one `getSharedCalendar` call is made for that month

#### Scenario: The window covers the neighbouring days on the grid
- **WHEN** the month of October 2026 is requested
- **THEN** the call asks for 2026-09-17 through 2026-11-14

### Requirement: A colleague's calendar survives the app being killed
The system SHALL store each month on the device, in storage that outlives the process. Entries SHALL
be keyed by the viewer, the owner and the month. A cached month SHALL be shown immediately, with no
loading state, before any network call is made.

#### Scenario: Reopening after a force-stop with nothing changed
- **WHEN** the viewer opens a colleague's calendar for October and November, force-stops the app and
  repeats the same walk, and the owner changed nothing in between
- **THEN** both months paint immediately and no `getSharedCalendar` call is made

#### Scenario: Another account on the same device
- **WHEN** viewer A has cached owner O's October and viewer B signs in on the same device and opens
  O's October
- **THEN** B does not see A's cached entry, and a full request is made for B

#### Scenario: Different owners are cached apart
- **WHEN** the viewer has cached owner O1's October and opens owner O2's October
- **THEN** O2's October is fetched rather than served from O1's entry

### Requirement: The owner keeps change markers a viewer can read
The system SHALL keep, on the owner's user sync document, a per-month timestamp of the last change to
any group event the owner holds or has just stopped holding. Only the server SHALL write it. It SHALL
move when the owner:
- is given a shift, whether by creating it, taking it or having it handed back;
- loses a shift, whether it is taken from them or they give it back;
- has one of their shifts edited, offered for swap or soft-deleted.

When a shift changes month, both months SHALL move. A document removed for real, by the retention
cleanup or a group deletion, SHALL NOT move it.

#### Scenario: A shift is taken from the owner
- **WHEN** member T takes owner O's shift dated in October
- **THEN** October's marker moves on both O's and T's sync documents

#### Scenario: A shift moves month
- **WHEN** O's shift is moved from 31 October to 1 November
- **THEN** both October's and November's markers move on O's sync document

#### Scenario: The retention cleanup purges an old shift
- **WHEN** the scheduled cleanup deletes one of O's shifts from last year
- **THEN** none of O's markers move

#### Scenario: A client tries to move the marker
- **WHEN** any client writes the group-events marker on a user sync document, the owner included
- **THEN** the security rules refuse the write

### Requirement: An open calendar stays live
While a colleague's calendar is on screen, the system SHALL follow the owner's change markers. For
each month shown, it SHALL compare those markers with the values the cached month was last caught up
to. It SHALL request data only when a relevant marker is newer. The relevant markers are:
- the owner's group-events, personal-events and one-off markers for the shown month, and for the
  neighbouring months the window covers;
- the owner's personal-event-types marker.

The system SHALL follow the markers only while the calendar is visible to the viewer. It SHALL stop
within 35 seconds of the calendar being closed, the app going to the background, or another tab being
shown. When the calendar is visible again it SHALL show the cached days at once and make at most one
call, only if a relevant marker moved in the meantime.

#### Scenario: Nothing changed
- **WHEN** a cached month is opened and no relevant marker is newer than the cache
- **THEN** the cached days are shown and no call is made

#### Scenario: The owner swaps a shift while the viewer watches
- **WHEN** the calendar is open on October and a shift of the owner's in October is taken by someone
  else
- **THEN** without any action from the viewer, one call is made and the shift disappears from the
  owner's calendar

#### Scenario: A change in a month that is not shown
- **WHEN** the calendar is open on October and the owner's group-events marker for March moves
- **THEN** no call is made

#### Scenario: The calendar was closed
- **WHEN** the viewer leaves a colleague's calendar and does not come back within 35 seconds
- **THEN** the app no longer listens to that owner's markers

#### Scenario: The app goes to the background on the calendar
- **WHEN** the app has been in the background for 35 seconds with a colleague's calendar on screen,
  and the owner then changes a shift in the shown month
- **THEN** no marker read and no call are made while the app stays in the background
- **AND** on returning, the cached days show at once and exactly one call catches the month up

#### Scenario: Another tab is shown
- **WHEN** the viewer switches to another tab for 35 seconds with a colleague's calendar kept in the
  People tab, and the owner then changes a shift in the shown month
- **THEN** no marker read and no call are made until the People tab is shown again, and then exactly
  one call catches the month up

### Requirement: Catching up reads only the gap
When a cached month is behind, the system SHALL ask the server only for documents changed after the
newest change the cache already holds. It SHALL merge the answer into the cached month:
- changed or new events replace or join the cached ones;
- deleted events, and shifts that are no longer the owner's, are removed;
- everything else is kept.

A month with no cache SHALL be requested whole. Event types, colours, group names and holder names
SHALL be refreshed with every answer.

#### Scenario: One shift changed
- **WHEN** a cached October with 20 events is behind because one shift changed
- **THEN** the answer carries that one event, and after merging October shows the other 19 unchanged
  plus the changed one

#### Scenario: A shift is handed away
- **WHEN** the owner's shift in October is taken by another member
- **THEN** the gap lists it as removed and it no longer shows on the owner's calendar

#### Scenario: A purged month keeps its history
- **WHEN** a cached month from last year is caught up after the server has purged its events
- **THEN** the cached events stay on the device and are still shown

### Requirement: A failed request keeps what was on screen
The system SHALL keep the cached days on screen, without showing an error, when a request fails for
any reason except a withdrawn grant. With nothing cached, the failure SHALL be reported as before. A
failed catch-up SHALL be retried the next time a marker moves or the month is shown again.

#### Scenario: Offline with a cache
- **WHEN** a cached month is behind and the call fails with `LoadFailed`
- **THEN** the cached days stay on screen and no error message is shown

#### Scenario: Offline with no cache
- **WHEN** a month that was never cached is shown and the call fails
- **THEN** the failure is reported and shown to the viewer as an error message

### Requirement: A withdrawn grant erases the owner's cache
When the function answers that the calendar is no longer shared with the viewer, the system SHALL
delete every cached month of that owner for that viewer and report the `NotShared` failure.

#### Scenario: The owner revoked access
- **WHEN** the viewer holds cached months of owner O and a request answers `NotShared`
- **THEN** all of O's entries for this viewer are removed and the viewer sees the not-shared error
