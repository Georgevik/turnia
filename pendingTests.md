# Pending tests

Tests that are agreed but not written. Each entry says what it must prove, what has to exist first,
and the fixture it needs — enough to pick up cold.

> **The project has no tests at all today.** `core/build.gradle.kts:47` and
> `app/shared/build.gradle.kts:75` both declare a `commonTest` source set with `libs.kotlin.test`,
> but neither `src/commonTest/` directory exists. There is no Compose UI-test dependency, no
> Firebase emulator block in `firebase/firebase.json`, and no fixture file: `MockData.kt` (277 lines)
> was deleted on 2026-09-04 in commit `83887e5` "Remove mocks and use groups firestore". Anything
> below therefore starts by creating infrastructure, not just a test.

---

## E2E: swapping a shift, with mock data

**Status:** pending. Blocked on nothing but itself; best written once Phase 2 of the swap feature
(`~/.claude/plans/add-the-feature-to-toasty-wadler.md`) has merged, so there is a full offer→take
round trip to drive.

### What it must prove

The chained change the app exists for — A→B→C — end to end, and the two rules that are easy to
regress because nothing else checks them:

1. **Offer.** The assignee of a group event whose type is `swappable` can set `onSwap = true`, and the
   event's `history` is untouched by that write.
2. **Take.** Another member of the same group takes it: `assigneeId` moves to them, `onSwap` returns to
   `false`, and exactly one `transferred` entry is appended to `history`.
3. **Chain (the important one).** The taker re-offers the same shift and a third member takes it.
   `history` now holds two `transferred` entries and `GroupMapper.chainOf` renders A→B→C. This is the
   case a naive owner-check in `setOnSwap` silently breaks, so it is the regression this test exists
   for.
4. **Mutual exclusion, FIFO.** Two takers race on one offered event. Exactly one succeeds; the other
   fails with `SwapError.TakenBySomeoneElse` (function code 3006), and the event is left consistent —
   one assignee, `onSwap == false`, one new history entry, not two.
5. **The gates.** A non-assignee cannot offer (`NotAssignee`); an event of a non-`swappable` type
   cannot be offered (`NotSwappable`); a user outside the group cannot take (`NotMember`, 3003).
6. **Read cost.** After the initial sync, a single offer/withdraw costs **one write** and **one
   single-document delta read per live collector** — never a whole-month read. This is the property
   the per-month sync markers in `groups/{g}/sync/updates` exist to provide, and the only way to catch
   its loss is to assert on it.

### Fixture (the mock data to rebuild)

A `MockData`-style object under a new `core/src/commonTest/kotlin/.../fixtures/`. Minimum shape:

- **Three users** — `alice`, `bruno`, `carla` — plus `dana`, who belongs to no group, for the
  `NotMember` case.
- **One group** with all four of `alice`/`bruno`/`carla` in `memberUids`, `alice` in `adminUids`, and
  `members` carrying their display names (the calendar reads names from that copy, so leaving it empty
  makes assertions on names silently pass on `""`).
- **Two group event types**: one `swappable = true`, one `swappable = false`. Both need a
  `defaultColor`, or every rendering assertion falls through to the id-derived fallback.
- **Group events** covering each state the swap tab segments on, all dated inside the retention window
  and at least one in a future month:
  | Event | `ownerId` | `assigneeId` | `onSwap` | Segment it must land in |
  |---|---|---|---|---|
  | `e1` | alice | alice | `true`  | Ofrecidos (for alice), Disponibles (for bruno) |
  | `e2` | alice | bruno | `false` | Cubiertos (alice), Cubro yo (bruno) |
  | `e3` | alice | bruno | `true`  | the chain case: bruno re-offering alice's shift |
  | `e4` | alice | alice | `false` | must appear in no segment |
  | `e5` | alice | alice | `true`  | of the non-swappable type — must not be offerable |
- **A revoked member**: `carla` in `revokedUids` and absent from `members`, still holding one event.
  Their query is narrowed to `assigneeId == me` by the security rules, which is what accidentally
  keeps `canTake` false for them — a fragile invariant worth pinning with a test.

### Infrastructure to stand up first

Decisions to make before writing a line; none of this exists:

- **Where it runs.** Two honest options, and they test different things:
  - *Firebase emulator suite* (`firebase emulators:start --only firestore,functions,auth`, plus an
    `emulators` block in `firebase/firebase.json`, which has none). This is the only way to exercise
    `takeEvent`'s transaction, the real security rules, and the FIFO race — i.e. items 2, 4 and 5
    above. It cannot run in a plain unit test; it needs the emulator running and the GitLive client
    pointed at it (`useEmulator`).
  - *Fake datasources in `commonTest`* — fast, no emulator, but it re-implements the transaction and
    the rules in the fake, so it proves the repository's own logic (items 1, 3, 6) and nothing about
    concurrency or permissions.

  Recommended: **both, split by what they can actually prove.** Repository-level tests in
  `commonTest` against fakes for the predicates, the pre-checks and the segment derivation; a separate
  emulator-backed suite for the transaction, the rules and the race. Do not write one test that
  pretends to do both.
- **The rules test.** `@firebase/rules-unit-testing` in `firebase/functions/` (or its own package) is
  the standard way to assert the `firestore.rules` change from Phase 1 — that an admin who is not the
  assignee cannot move `onSwap`, and that the revoked soft-delete branch still works. This is a
  different runtime from the Kotlin tests and needs its own npm setup.
- **Compose UI tests.** Nothing is wired for them. Driving the day-sheet toggle and the swap tab's
  segmented tabs needs `compose.uiTest` plus an Android instrumented or desktop test target. Given the
  cost, prefer asserting the ViewModel's UiState directly and leaving the composables to the 13
  existing `@Preview`s — unless a genuine UI regression justifies the setup.
- **The FIFO race is the hard one.** Two coroutines calling `takeEvent` concurrently against the
  emulator will usually serialise cleanly enough to look like a pass without ever contending. To make
  it a real test, launch both from the same `runTest` with no suspension between them and assert on the
  *pair* of outcomes (exactly one success, one `TakenBySomeoneElse`), then re-read the event and assert
  its history has exactly one new entry. Assert on the invariant, not on which caller won.

### Conventions the tests must follow

- `Outcome<T, E>` is the return type, so assert on the sealed variant — never `getOrNull()`-style
  collapsing. A failure's *reason* is the thing under test.
- No `Throwable` should ever reach the assertions from a repository; if one does, that is the bug.
- Comments in English, per `CLAUDE.md`.
