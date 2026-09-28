# Proposal

## Why

Users mostly create personal one-off events, yet Turnia's value is in shifts defined once and reused
with a tap. The UI steers them the wrong way: the add pane puts the one-off field first and largest,
the concept of a shift type never appears (a bare "Add" chip under "PERSONAL EVENTS"), there is no
empty state when a user has no types, and the only onboarding is a feature tour before sign-in that
sets nothing up.

## What Changes

- **Shift setup after sign-in.** A skippable screen, "What shifts do you work?", appears after
  sign-in when the account has no personal event types, belongs to no group and has no pending
  invitation code. It offers presets that never mention healthcare:
  - selected by default: Morning (08:00–15:00), Afternoon (15:00–22:00), Night (22:00–08:00);
  - not selected: Morning & afternoon (08:00–22:00), 24h duty (08:00–08:00 next day).

  In Spanish their acronyms are M, T, N, MT and 24H; every language has its own.

  Names and acronyms are localized in the language active when the types are created; after that
  they are the user's data and do not change with the app language. Preset times can be edited inline.
  Each type gets a distinct palette color.
- **Inline custom type** on the same screen: "Add another shift" takes a name, an acronym and an
  optional start/end time; its color is assigned automatically.
- Confirming creates every selected type as a **personal event type**, never a group or group type,
  and lands on the calendar with a hint to tap a day. Skipping is remembered in the device's DataStore
  (the file the share prompt uses); nothing is written to Firestore. The screen stops appearing on
  its own once the account has a type or a group. A skip on one device does not hide it on another.
- **Add pane reordered.** The user's shift types come first; the one-off event becomes a secondary
  "Other event…" entry. With no types, an empty state explains that shifts are created once and
  added with a tap, and opens the same setup screen.
- **User-facing wording.** "Personal events" becomes "My shifts" (and equivalents) in the five
  languages. Only strings change; the code vocabulary (`PersonalEventType`) stays.
- **Save as shift.** The one-off event form offers "Save as shift", which creates a personal event
  type from the one-off's name, color and times.
- **Analytics.**
  - `onboard_shift_shown` when the setup screen is shown.
  - `onboard_shift_skipped` with `interacted`.
  - `onboard_shift_completed` with `interacted`, `type_count` and `custom_type_count`.
  - `interacted` is `true` if the user touched the panel in any way before Create or Skip: toggling
    a preset, editing a time or adding a custom type.
  - `first_event_added` with `kind` = `typed` | `one_off`: the activation signal, telling whether
    the first event added on the device came from a shift or was a one-off.
  - The shift setup opened from the add pane's empty state logs the same events with `via = add_pane`;
    the one after sign-in carries `via = onboarding`.

## Capabilities

### New Capabilities
- `shift-setup`: the post-sign-in shift setup screen: when it shows, the presets, the inline custom
  type, skip, and the personal types it creates.
- `event-add-pane`: how the day sheet offers ways to add an event: shift types first, the one-off as a
  secondary entry, the empty state, and "Save as shift" from a one-off.

### Modified Capabilities
- `analytics-events`: adds the `onboard_shift_*` events, their parameters and the activation signal.

## Impact

- **UI** (`app/shared`): a new setup screen and its route after sign-in (`SplashViewModel` / root
  navigation), `DayDetailAddEvent` and `DayDetailSheet`, `OneOffEventForm`, and strings in all five
  `values*/` folders.
- **Core**: preset definitions, a bulk personal type write in `PersonalEventRepository` (one commit
  of one document per type plus the sync marker), a repository deciding when the setup shows, and
  the skip flag in DataStore.
- **Analytics**:
  - new `AnalyticsEvent` entries;
  - `interacted`, `custom_type_count` and `kind` must be registered in the GA console.
- **Firestore**: no schema change. `firestore-usage.md` gains the setup's commit (N + 1 writes).
- **E2E**: robots that match "My events", "New event", the add pane order or the post-sign-in landing
  need updating, and a new flow test class covers the setup, the add pane and "Save as shift".
