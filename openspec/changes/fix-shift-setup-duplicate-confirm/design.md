# Design

## Context

See proposal.md - Why. The race is confined to `ShiftSetupViewModel` and `ShiftSetupUi`: a
successful `confirm()` flips `saving` back to `false` before the screen leaves composition, and
`canConfirm` only guards on `saving`, not on `closed`. `skip()` already solved the same class of
race with a private `skipping` flag (see its comment: "A second tap lands while the first skip is
still writing, before the screen closes").

## Goals / Non-Goals

**Goals:**
- A tap that lands after `confirm()` has already succeeded (screen closing) or while it is still
  writing must not start a second write.
- Mirror the existing, already-reviewed pattern (`skip()`'s `skipping` flag) rather than invent a
  new mechanism.

**Non-Goals:**
- No change to `PersonalEventTypesFirestore` / `createEventTypes`. The duplicate writes are each
  individually a correct, successful write of what the user asked for at that moment — the bug is
  that the UI let the user ask more than once for the same thing. Adding a server-side "does this
  account already have these types" check would cost an extra read on every legitimate confirm
  (see `firebase/firestore-usage.md`) to guard a case that is fully preventable in the UI layer.
- No cleanup/dedup of event types already duplicated on accounts affected by this bug before the
  fix ships. Out of scope for this change.
- No change to the ~200ms `NavDisplay` exit transition itself. Shortening or skipping it would not
  close the race, only narrow the window; the fix is to stop trusting `saving` alone as "is it safe
  to confirm again".

## Decisions

**Guard `confirm()` the same way `skip()` is guarded**, with a private re-entrancy flag
(`confirming`) checked and set at the very top of `confirm()`, cleared only on failure (so a
genuine retry after an error is still possible — matching the existing "write fails, stay open,
user can retry" behavior). On success the flag is irrelevant: `closed` is now `true` and the screen
is on its way out regardless of any stray tap.

Also change `ShiftSetupUi.canConfirm` to `selectedCount > 0 && !saving && !closed`, so the button
itself stops being enabled once the setup has succeeded, independent of the ViewModel-level guard.
Both changes together close the race at both layers it's visible from (the button's enabled state,
and the function's own re-entrancy), the same belt-and-suspenders shape `skip()` already uses
(`skip()` guards itself with `skipping`, and the UI separately stops rendering once `closed`).

Alternative considered: dedupe by assigning deterministic ids (e.g. derived from `via` + preset)
instead of `createUuid()`, so a duplicate `set()` would overwrite rather than create a new
document. Rejected: `setAll` already intentionally lets `EventTypeId` be freely chosen per type
(including user-named custom shifts), and collapsing retries into the same ids would mask the real
bug (duplicate confirms happening at all) instead of preventing it; it would also silently
overwrite a type the user had already started customizing from the add pane in the rare case ids
collided for unrelated reasons.

## Risks / Trade-offs

- [Risk] The `confirming` flag only protects a single `ShiftSetupViewModel` instance; if two
  instances existed simultaneously the guard would not help. → Mitigation: not applicable here —
  `rememberViewModelStoreNavEntryDecorator()` retains one instance per back-stack entry, confirmed
  by reading `TurniaNavDisplay.kt`; this is the same assumption `skip()`'s existing guard already
  relies on.
- [Risk] Accounts already duplicated by this bug stay duplicated after the fix ships. → Mitigation:
  explicitly out of scope (Non-Goals); flagged here so it isn't mistaken for an oversight.
