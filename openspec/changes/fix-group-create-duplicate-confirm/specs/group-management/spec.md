# Spec Delta

## Purpose

Lets an admin create a group and later save changes to it, through a single form that creates on
first save and updates on every one after, without ever creating or saving a group twice from one
intended confirm.

## ADDED Requirements

### Requirement: Saving a group cannot be started twice at once
The group detail screen's save action SHALL be disabled, and a request to save SHALL be a no-op,
while a save for that same screen is already in flight. The busy state SHALL be recorded before the
write is started, so a save requested while one is already running SHALL NOT create or update the
group a second time.

#### Scenario: A second save while the first is in flight does nothing extra
- **WHEN** the user triggers save on the group form a second time before the first save has
  finished
- **THEN** only one group is created (or, when editing, only one update is sent) and the second
  request has no effect

#### Scenario: A save can be retried after it fails
- **WHEN** a save fails and the user triggers save again
- **THEN** the save is attempted again, since the busy state was cleared by the failure
