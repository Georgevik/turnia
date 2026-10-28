# Spec Delta

## MODIFIED Requirements

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
