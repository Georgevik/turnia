# Spec Delta

## Purpose

Covers creating and saving an event type — a shift of the user's own, or a type of a group — from the
form reached from *My shifts*, from the add pane, from "Save as shift" and from a group's type list.

## ADDED Requirements

### Requirement: Saving a new event type creates exactly one
The save action on the event type form SHALL create at most one event type per form, however many
times it is invoked. A save that lands while the first is still being written, or after the first has
already succeeded and the form is closing, SHALL have no effect. This SHALL hold for a shift of the
user's own and for a type of a group alike.

Where a type is saved for a group that does not exist yet, and so waits to be written with the group
itself, a repeated save SHALL leave one type waiting, so the group is not created with the same type
listed twice.

Editing an existing type SHALL remain unaffected: a repeated save of an edit overwrites the same
type and SHALL NOT produce a second one.

#### Scenario: Repeated saves create one shift
- **WHEN** the user fills in a new shift and invokes save three times in quick succession
- **THEN** exactly one personal event type is created

#### Scenario: A save during a slow write creates nothing extra
- **WHEN** the user invokes save, the write has not yet been confirmed, and the user invokes save again
- **THEN** exactly one event type is created

#### Scenario: A save after success creates nothing extra
- **WHEN** the save has already succeeded and the form is closing, and a further save lands before it leaves
- **THEN** no second event type is created

#### Scenario: A group's first type is not duplicated
- **WHEN** the user adds a type to a group that does not exist yet, invokes save twice, and then creates the group
- **THEN** the group is created with that type listed once

#### Scenario: Editing stays idempotent
- **WHEN** the user edits an existing type and invokes save twice
- **THEN** that type is updated and no second type exists

### Requirement: A failed save stays retryable
When a save fails, the form SHALL stay open with the user's input intact, SHALL show an error, and
SHALL accept a further save. A failure SHALL NOT leave the save action permanently unavailable.

#### Scenario: Retrying after a rejected save
- **WHEN** the user's save is rejected and the user invokes save again
- **THEN** the save is attempted again

#### Scenario: A retry after a failure creates one type
- **WHEN** the user's first save fails, the user invokes save again, and the second succeeds
- **THEN** exactly one event type exists
