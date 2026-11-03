# event-add-pane Specification

## Purpose

Defines how the day sheet offers the ways of adding an event, so that the user's reusable shifts are
the first and easiest choice and a one-off event is the exception.

## Requirements

### Requirement: Shifts come first, one-off events last
On the user's own calendar, the add pane SHALL list, in this order:
1. the user's shifts: one option per personal event type that is not deleted, with an action to
   create a new one;
2. each group's event types, as today;
3. a secondary "Other event…" entry that opens the one-off event form.

The one-off entry SHALL NOT be the first option, nor displayed larger than a shift option. Tapping a
shift SHALL still add it to the open day with a single tap.

Tapping a shift SHALL add exactly one event, however many times it is tapped. The add pane closes
after a shift is tapped, so one visit SHALL add at most one event: repeated taps on the same shift,
or taps on different shifts, SHALL have no effect once the first has been accepted — including taps
that land while the pane is still visible but closing, and taps that land while the first event is
still being written. The count of events added, which drives the share and team prompts, SHALL
likewise rise by one.

#### Scenario: A user with shifts
- **WHEN** a user with personal event types opens a day and taps Add
- **THEN** their shifts are the first options shown and "Other event…" comes after every shift and group section

#### Scenario: One tap adds a shift
- **WHEN** the user taps one of their shifts in the add pane
- **THEN** an event of that type is added to that day

#### Scenario: Repeated taps on a shift add one event
- **WHEN** the user taps the same shift three times in quick succession, while the add pane is closing
- **THEN** that day holds exactly one event of that type

#### Scenario: A tap during a slow write adds nothing extra
- **WHEN** the user taps a shift, the write has not yet been confirmed, and the user taps it again
- **THEN** only one event is written

#### Scenario: A second shift tapped while the pane closes is ignored
- **WHEN** the user taps one shift and then taps a different shift before the add pane has closed
- **THEN** only the first shift's event is added to that day

#### Scenario: Repeated taps count as one added event
- **WHEN** the user taps the same shift three times in quick succession
- **THEN** the count of events added that drives the share and team prompts rises by one, not three

#### Scenario: One-off still reachable
- **WHEN** the user taps "Other event…"
- **THEN** the one-off event form opens, as it did before

### Requirement: An empty state explains shifts when the user has none
When the user has no personal event types, the shifts section of the add pane SHALL show an empty
state. It SHALL say that shifts are created once and then added with a tap, and SHALL offer an action
that opens the shift setup (see `shift-setup`). The "Other event…" entry SHALL remain available below
it.

#### Scenario: No shifts yet
- **WHEN** a user with no personal event types opens a day and taps Add
- **THEN** the empty state and its action to create shifts are shown above "Other event…"

#### Scenario: Creating shifts from the empty state
- **WHEN** the user opens the setup from the empty state, confirms the defaults and opens the day again
- **THEN** the add pane lists Morning, Afternoon and Night as shift options

### Requirement: Personal event types are called shifts in the interface
Everywhere the interface names the user's personal event types, it SHALL call them shifts (Spanish
"turnos"), in all five app languages. This covers the add pane's section title, the settings entry
and screen title that list them, their creation action and their empty state. The one-off event keeps
its own wording.

#### Scenario: Settings entry
- **WHEN** the app language is English and the user opens Settings
- **THEN** the entry that lists personal event types reads "My shifts" and its create action reads "New shift"

### Requirement: A one-off event can be saved as a shift
The form of an existing one-off event SHALL offer "Save as shift". It SHALL open the shift creation
screen prefilled with the one-off's name and color, and, when the one-off is not all day, with its
start and end times. The user SHALL provide the acronym before saving. Saving SHALL create a personal
event type and SHALL leave the one-off event unchanged.

#### Scenario: Saving a timed one-off as a shift
- **WHEN** the user opens a one-off "Guardia" from 09:00 to 21:00, taps "Save as shift", enters acronym "G" and saves
- **THEN** a personal event type "Guardia", acronym "G", 09:00–21:00, with the one-off's color exists
  and the one-off is still on the calendar

#### Scenario: An all-day one-off
- **WHEN** the user taps "Save as shift" on an all-day one-off
- **THEN** the creation screen opens with the name and color prefilled and no times
