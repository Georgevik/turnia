# Spec Delta

## Purpose

Makes a new group quick to create and hard to leave empty: it starts with the usual shifts already
proposed, leads straight to inviting colleagues, and keeps asking for them while its creator is alone.

## ADDED Requirements

### Requirement: A new group starts with four proposed event types
When the user opens the form to create a group, the system SHALL list these event types already
proposed, in this order, with these times:

| Type | Times |
|------|-------|
| Morning | 08:00–15:00 |
| Afternoon | 15:00–22:00 |
| Night | 22:00–08:00 |
| Morning & afternoon | 08:00–22:00 |

Their names and acronyms SHALL be those of the shift setup's presets of the same name, in the app's
language when the form opens; in Spanish the acronyms SHALL be M, T, N and MT. Each SHALL carry as
its default color the color of that preset. Each SHALL allow swapping.

The user SHALL be able to edit any proposed type and remove it before saving, and to add types of
their own. The group SHALL be created with exactly the types listed when it is saved. The rule that a
group is created with at least one event type SHALL still hold: with every type removed, the group
SHALL NOT be saved.

#### Scenario: Creating a group by naming it
- **WHEN** the user opens the new-group form, types a name and saves
- **THEN** the group is created with the four proposed types

#### Scenario: Proposed types follow the app language
- **WHEN** the app language is Spanish and the new-group form opens
- **THEN** the proposed types read Mañana (M), Tarde (T), Noche (N) and Mañana y tarde (MT)

#### Scenario: Removing a proposed type
- **WHEN** the user removes Night and saves
- **THEN** the group is created with Morning, Afternoon and Morning & afternoon only

#### Scenario: Editing a proposed type
- **WHEN** the user changes Morning's times to 07:00–14:00 and saves the group
- **THEN** the group's Morning type starts at 07:00 and ends at 14:00

#### Scenario: Removing every type
- **WHEN** the user removes all four proposed types and adds none
- **THEN** the group cannot be saved

#### Scenario: Existing groups are untouched
- **WHEN** an admin opens an existing group
- **THEN** no type is proposed or added to it

### Requirement: Saving a new group leads to inviting colleagues
When a new group has been created, the system SHALL show a step that says the group is ready and
invites the user to bring their colleagues, with the invitation link's share button as its main
action and a way to finish without sharing. Sharing SHALL send the same invitation link, with the
same text, as the share button on the group's screen. Finishing SHALL leave the user where saving a
group leaves them today. The step SHALL NOT appear after saving changes to an existing group, nor
when creating the group fails.

#### Scenario: Group created
- **WHEN** the user saves a new group and the write succeeds
- **THEN** the "Your group is ready" step is shown with the share button

#### Scenario: Sharing from the step
- **WHEN** the user taps the share button on that step
- **THEN** the system share sheet opens with the group's invitation link

#### Scenario: Finishing without sharing
- **WHEN** the user finishes the step without sharing
- **THEN** they return to where they created the group from

#### Scenario: Editing an existing group
- **WHEN** an admin saves a change to an existing group
- **THEN** the step is not shown

#### Scenario: Creation fails
- **WHEN** the user saves a new group and the write fails
- **THEN** the step is not shown and the form stays on screen with its error

### Requirement: A group with a single member asks for its team
While a group has exactly one member, the group's screen SHALL show a card that invites the user to
bring their team, with the invitation link's share button. The card SHALL disappear as soon as a
second member joins, and SHALL return if the group is back to one member. It SHALL be derived from
the group data already loaded for the screen and SHALL cost no additional read.

#### Scenario: Alone in a group
- **WHEN** the only member of a group opens the group's screen
- **THEN** the card inviting them to bring their team is shown with the share button

#### Scenario: A second member joins
- **WHEN** a join request is accepted while the admin is on the group's screen
- **THEN** the card disappears

#### Scenario: A group with several members
- **WHEN** a member of a group with three members opens the group's screen
- **THEN** no such card is shown
