# Spec Delta

## MODIFIED Requirements

### Requirement: Confirming creates the selected shifts as personal event types
Confirming SHALL create one personal event type per selected shift, all in a single atomic write.
Names and acronyms SHALL be stored in the language shown at that moment, and SHALL NOT change when
the app language changes later. The setup SHALL NOT create a group or a group event type. The confirm
action SHALL be disabled while no shift is selected, while a write is in flight, and once the setup
has already succeeded and is closing. A confirm tap that lands while a write is in flight, or after
the setup has already succeeded, SHALL NOT start another write. After a successful write, the setup
SHALL close onto the calendar with a hint telling the user to tap a day to add a shift. If the write
fails, the setup SHALL stay open with the user's selection intact and show an error, and confirming
SHALL be available again.

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

#### Scenario: A second tap while the write is in flight creates nothing extra
- **WHEN** the user taps confirm a second time before the first write has finished
- **THEN** only one write is sent and only the one set of personal event types is created

#### Scenario: A tap after a successful confirm creates nothing extra
- **WHEN** the write already succeeded and the setup is closing, and a tap still lands on the
  confirm action before the screen has fully left
- **THEN** no second write is sent and no additional personal event type is created
