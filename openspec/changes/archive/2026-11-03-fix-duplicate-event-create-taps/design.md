# Design

## Context

See `proposal.md` — Why, for the two unguarded sites and the production bug that established the
pattern. What shapes the approach is the shape of the window itself.

The window a repeated tap exploits is two windows glued together, and they have different natures:

```
  tap 1
   |
   v
  [--------- write in flight ---------][---- exit animation ----]  gone
   0ms                          ? ... Ns          ~200-300ms
   ^                                  ^           ^             ^
   |                                  |           |             |
   +-- UNBOUNDED: network-dependent --+           +-- bounded, owned by
   |                                                  Material3 / Nav3
   |
   +=============== what must be covered ======================+
```

The first window has no upper bound: it is however long Firestore takes, and the production report
was precisely a user reacting to a *slow* write ("the button appeared unresponsive"). The second is
bounded but its duration belongs to Material3's `ModalBottomSheet.hide()` and Nav3's pop transition,
neither of which is contractual.

Two shipped fixes already cover this window — `ShiftSetupViewModel.confirm()` / `skip()` and
`GroupDetailViewModel.onSave()` — with the same idiom, so the project has a house style to match
rather than a new pattern to invent.

One constraint is specific to the add pane and is the reason its fix is not a copy of the others:
`DayDetailSheet` obtains its view model with `koinViewModel(key = date.toString())`, scoped to the
enclosing `ViewModelStoreOwner`, not to one opening of the sheet. Reopening the same day returns the
same instance. A latch that simply stayed set would therefore stop the user adding a second event to
that day for the rest of the session — a regression, not a fix. The shift setup's latch can be
permanent only because its screen is one-shot and its view model dies with it.

## Goals / Non-Goals

**Goals:**

- Cover both windows with one mechanism, with no tuned duration to own.
- Match the idiom the two shipped fixes already use, so there is one house style for this bug.
- Keep the new tests clock-free, in the shape of the existing `confirmingTwiceCreatesOnlyOnce`.
- Reset correctly: a failed write stays retryable, and reopening a day allows another add.

**Non-Goals:**

- No shared primitive, no change to any clickable outside the two sites. Protecting sites nobody has
  thought of yet is a separate decision, recorded under Decision 1 as deliberately deferred.
- No change to the button's visual feedback. The bug is a duplicate write, not a missing spinner.
  Making a slow write *look* slower is a UX question this change does not touch.

## Decisions

### Decision 1: A latched flag, not a time-based debounce or throttle

Guard each action with a boolean set **synchronously before** the write is launched, latched so
success never clears it, and cleared only on failure.

This is chosen over the obvious alternative of debouncing the click, for four reasons:

**It covers the unbounded window.** A time window of any length expires while a slow write is still
in flight, which is exactly the scenario that shipped to a user on iOS 1.11.0. A 300ms throttle would
not have caught the bug it is being proposed to fix. The latch has no duration: it is set before the
write and held until the write answers.

**"Debounce" in this codebase means the wrong thing.** The only `debounce` in the project is
`kotlinx.coroutines.flow.debounce`, used for the people search and the usage-audit summary. It is
*trailing-edge*: it waits for quiet and then emits the last value. Applied to a button that delays
every add by the window before anything happens — making the unresponsiveness that caused this bug
worse. The right time-based primitive would be a leading-edge throttle, which the project does not
have; naming the decision "debounce" invites someone to reach for the operator already imported and
build the opposite.

**No magic number.** A tuned window has two failure modes and the bad one is intermittent: too short
and the gap reopens on a dropped frame or a cold launch, too long and legitimate fast interaction
feels broken. The latch has no number to get wrong.

**It stays testable without a clock.** The existing coverage is three lines and no time source:

```kotlin
viewModel.confirm(spanish)
viewModel.confirm(spanish)
assertEquals(1, repository.completed.size)
```

A time-based guard needs a `TimeSource` injected into every call site to assert anything
deterministically.

**Alternatives considered:**

| Alternative | Why not |
|---|---|
| Trailing-edge debounce on the click | Delays every add; wrong semantics entirely (see above) |
| Leading-edge throttle on the click | Misses the unbounded write window; needs a tuned duration and an injected clock |
| A shared latching `OneShotButton` in `ui/system` | A UI-layer latch cannot see that the write failed, so a failed save would leave a permanently dead button. Safe only with an escape hatch it has no way to receive |
| Disabling the button harder in the UI | This is what already exists and what failed: `enabled` is driven by state that success itself resets |
| Deduping in the repository by content | Addresses a different problem (see Decision 5); does not stop two taps from being two intents |

A blanket leading-edge throttle as *defense in depth* remains a reasonable follow-up — it cannot
brick a button, so unlike the latch it is safe to apply app-wide, and it would cover sites nobody
remembers. It is deliberately out of this change: it is a new primitive touching every button, and
it fixes none of the two bugs on its own.

### Decision 2: The latch is a private field, not a UiState field

A `private var` on the view model, as `ShiftSetupViewModel` does with `confirming` and `skipping`,
not a new field on the UiState.

The project's rule is that UI state is a faithful representation of what is displayed on screen. The
latch is not displayed — it is a record of an intent already accepted. What *is* displayed is the
button's enabled/loading state, which stays where it is. Putting the latch on the UiState would
invite the same confusion that caused the bug: treating "the write finished" and "the action is still
spent" as one flag when they have different lifetimes.

### Decision 3: The add pane's latch is reset when the sheet leaves composition

Because the view model is keyed by date and outlives one opening of the sheet (see Context), the
latch must be cleared when the sheet goes away, or a day accepts only one event ever.

`DayDetailSheet` already has the hook: a `DisposableEffect(Unit)` whose `onDispose` cancels the
one-off form and reports the form closed. `onDispose` runs after the hide animation completes — after
the window the latch exists to cover — so clearing the latch there gives exactly the right lifetime:

```
  sheet opens            tap            animation            onDispose
       |                  |                 |                    |
       v                  v                 v                    v
  latch = false ---> latch = true ---> [taps ignored] ---> latch = false
                                                           (ready for next visit)
```

This keeps the guard in the view model, where it is unit-testable, while tying its lifetime to the
sheet rather than to the view model that outlives it.

Rejected: a `remember`-ed flag in the composable. Its lifetime would be correct for free, but it puts
the guard in the UI layer where no view model test can reach it, and it diverges from the two shipped
fixes.

Also rejected: changing the view model's scoping key so a reopen yields a fresh instance. That would
fix the lifetime but silently change caching and reload behavior for every other thing the sheet
does — a much larger blast radius than the bug warrants.

### Decision 4: One add per sheet visit, not one add per type

The latch covers the whole sheet, so once any shift has been accepted, tapping a *different* shift
before the sheet closes is also ignored.

This matches what the sheet already does rather than adding a restriction: `onPickEventType` closes
the sheet after the first pick, so a visit was always meant to add exactly one event. A per-type
latch would make a second, different chip tapped during the closing animation succeed — adding an
event the user cannot see being added, on a sheet they have already dismissed. One latch is both
simpler and the better behavior.

Two consequences fall out of guarding at the entry point rather than per write path:

- `countAdded` sits inside the guarded path, so the share- and team-prompt counter stops being
  inflated by duplicate taps without any separate fix.
- `adRepository.actionPerformed()` currently sits *outside* the `when` in `addEventOfType`, so it
  fires per tap. It moves inside the guard with everything else; three taps are one action.

### Decision 5: Only the create paths change; nothing is deduped by content

`EventTypeDetailViewModel`'s edit paths reuse `key.typeId`, so a repeated save overwrites one
document and is already idempotent. Only the create paths mint a fresh id per call. The fix therefore
guards the action, and deliberately does not add any "does an equivalent one already exist?" check.

A content check would be a different feature with a product decision behind it, and it would not fix
this bug: two taps are two accepted intents, and the question of whether a day may hold two Morning
shifts is a domain rule, not a race. See Open Questions.

The second half of the `EventTypeDetail` fix is to stop the success branch re-enabling the button:
`saveButtonLoading = false` and `isSaved = true` are currently emitted together, so the state change
that triggers `navigator.goBack()` is the same one that makes the button live again. The screen is
leaving; the button has no reason to come back.

## Risks / Trade-offs

- **The add pane's latch reset depends on `onDispose` running.** If the sheet were ever rendered
  without that `DisposableEffect`, or disposal were skipped, a day would accept one event per
  session. → The reset goes in the existing `onDispose` that already resets form state, so it shares
  that path's fate rather than adding a second thing to keep true. A test covers add → dispose → add.

- **A failed write that never answers leaves the action latched.** The latch clears `onFailure`, so a
  call that neither succeeds nor fails holds it. → This is the behavior of both shipped fixes, and the
  repositories answer with an `Outcome` rather than hanging. For the add pane the sheet is closing
  anyway, and reopening resets it.

- **Guarding the whole sheet is a visible behavior change.** A user who taps two different shifts
  very fast now gets one event where they previously got two. → Intended, and specified. The sheet
  closes on the first pick, so the second event was never something they could see arriving.

- **This fixes the third and fourth instance of one bug, by hand, again.** The pattern is not holding
  on its own and nothing prevents a fifth. → Accepted for this change; the blanket throttle in
  Decision 1 is the candidate answer and is recorded as a follow-up rather than silently dropped.

- **No cleanup for accounts already affected.** A user who already has two "Morning" types or three
  identical events keeps them. → Matches both sibling changes. Duplicates are editable and deletable
  by hand, and a migration would need the content-equivalence rule this change deliberately avoids.

## Migration Plan

Client-only, no backend, rules, index or fixture changes, so no coordinated deploy and no data
migration. Rollback is reverting the commit. Older clients keep the bug; nothing a fixed client
writes is unreadable to them, since the change only stops writes that should never have happened.

## Open Questions

- **Is "a day holds at most one event of a given type" a real domain rule?** Out of scope here and
  answerable later without changing these specs, the approach or the tasks, because the latch is
  about accepted intents and a uniqueness rule is about stored state. If it is real, it belongs in
  the repository, not in button state — and it is the only thing that would also cover the cases no
  client-side guard reaches: two devices, and an offline queue flushing. One hint that it may already
  exist implicitly: `move-personal-to-group` skips a day that already holds a shift of the target
  group, and reports it.

  **Confirmed on a device, 2026-10-18.** Driving the app in airplane mode showed the latch holding
  end to end — five taps on a shift chip produced one queued write and one event, five taps on the
  form's save produced one type — and then showed the case it cannot reach: with the write still
  queued, leaving the form and filling it in again created a **second** type, and both appeared when
  the network returned. A new screen is a new view model and a new latch, while the first write is
  still in Firestore's queue. Nothing in process can see that, which is the evidence for deciding
  this question at the repository rather than in button state.

- **What should an awaited write do while offline?** Out of scope here and recorded as the follow-up
  the proposal names. Firestore resolves a write only on server acknowledgement, so offline the
  form's save spins with no error and no retry until connectivity returns, when it completes and
  closes the screen by itself; the add pane, which does not await, closes immediately but does not
  render the queued event until the same moment. Neither loses data and neither is a regression from
  this change, but the task 3.3 expectation that a failed save "shows its error and accepts a retry"
  does not hold offline, because offline is not a failure.
