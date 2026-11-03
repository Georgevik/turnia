# Spec Delta

## Purpose

Defines what the calendar shows while the device cannot reach the server: a change made offline is on
the calendar at once and looks no different from one the server has taken, and nothing the device
already holds is lost because a read could not be answered.

## ADDED Requirements

### Requirement: A change made offline is on the calendar at once

An event added, edited or deleted while the device has no connection SHALL appear, change or
disappear on the calendar immediately, without the user leaving the screen, reopening the app or
regaining a connection. This SHALL hold for every kind of event the calendar renders: a shift of a
personal type, a personal one-off event, a group event, and the user's private note on a group
event. A shift type created offline SHALL be available to add events with, and events of it SHALL
render with its name and colour.

#### Scenario: Adding a shift in airplane mode

- **WHEN** the user turns airplane mode on, opens a day and taps one of their shifts
- **THEN** the shift appears on that day straight away, and on the month's calendar, with its name and colour

#### Scenario: Adding a group shift in airplane mode

- **WHEN** the user with no connection opens a day on a group's calendar and taps one of the group's event types
- **THEN** the shift appears on that day straight away

#### Scenario: Adding a one-off event in airplane mode

- **WHEN** the user with no connection fills the one-off event form and saves
- **THEN** the event appears on every day it spans straight away

#### Scenario: Deleting offline

- **WHEN** the user with no connection deletes an event they hold
- **THEN** the event stops being shown straight away

#### Scenario: A note written offline

- **WHEN** the user with no connection writes a private note on a group shift and saves
- **THEN** the shift's row shows that note straight away

#### Scenario: A shift type created offline

- **WHEN** the user with no connection creates a new shift and adds it to a day
- **THEN** the shift is offered in the add pane and the event renders with its name and colour

### Requirement: An event awaiting upload is indistinguishable from a synced one

An event the server has not yet acknowledged SHALL be shown exactly as one it has: no badge, no
distinct colour or opacity, no "pending", "not synced" or "will upload" wording, and no error. The
user SHALL NOT be asked to retry or confirm anything, and SHALL NOT be told the save failed.

#### Scenario: No pending marker on the row

- **WHEN** the user adds an event with no connection and looks at its day and its month
- **THEN** the event is rendered identically to the events already on that calendar, with nothing indicating it is unsent

#### Scenario: No failure is reported

- **WHEN** an add, edit or delete is made with no connection
- **THEN** no error message, snackbar or retry prompt is shown for it

### Requirement: A change made offline is uploaded on reconnect, exactly once

When the connection returns, every change made offline SHALL be uploaded without the user acting,
and SHALL survive the app being closed and reopened before the connection returns. After the upload
the calendar SHALL show each change exactly once — never twice, and never reverted.

#### Scenario: Reconnecting

- **WHEN** the user adds two shifts with no connection and then turns airplane mode off
- **THEN** both shifts are uploaded, each still appears exactly once, and the user's other devices show them

#### Scenario: Closed before reconnecting

- **WHEN** the user adds a shift with no connection, closes the app, reopens it still with no connection
- **THEN** the shift is still on the calendar, and it is uploaded once the connection returns

### Requirement: A calendar is never emptied because the server cannot be reached

When a read of the server fails — no connection, a timeout, or a refusal — the calendar SHALL keep
showing the events the device already holds. It SHALL NOT show an empty month, SHALL NOT drop the
events of months the read did not concern, and SHALL NOT hide an event the user added offline.

#### Scenario: A month the device has never synced

- **WHEN** a user whose other device added events to a month this one never opened goes offline and opens that month
- **THEN** the events this device does hold, for that month and every other month in view, are still shown

#### Scenario: Offline from a cold start

- **WHEN** the user opens the app with no connection
- **THEN** the calendar shows the events the device holds, for every month it holds them for

#### Scenario: The connection drops while the calendar is open

- **WHEN** the connection drops while a month is on screen
- **THEN** the events on screen stay on screen

### Requirement: The server is read only when it can answer and the device is behind

The calendar SHALL read the server only when a connection exists and the sync markers show the
device's cached copy is behind. While the device is offline, or while it holds writes the server has
not acknowledged, the calendar SHALL be served from the device's cache and SHALL attempt no server
read. No change made offline SHALL cause a billable read, and no additional document listener SHALL
be held on an event collection.

#### Scenario: An offline add costs nothing

- **WHEN** the user adds events with no connection and the calendar updates
- **THEN** the usage audit records no server read for them, only cache reads

#### Scenario: Up to date and online

- **WHEN** the device is online and no marker has moved since it last read
- **THEN** the calendar is served from the cache and no server read is made
