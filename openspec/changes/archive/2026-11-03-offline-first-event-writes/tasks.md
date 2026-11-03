# Tasks

## 1. Confirm the Firestore semantics the fix rests on

- [x] 1.1 With a temporary `Logger.d` in `UserSyncFirestore.snapshots`, add an event with `disableNetwork()` and record what the unresolved month marker deserializes to (`null`, `Timestamp.ServerTimestamp`, or a client estimate) and what `Synced.confirmed` is on that emission; verify by reading both values in logcat and noting them in `design.md` under *Decisions*
- [x] 1.2 In the same run, confirm the marker listener emits at all on a local pending write and again when it resolves on reconnect; verify two unconfirmed emissions and one confirmed emission appear in logcat, then remove the temporary logging
- [x] 1.3 Widen `GroupSyncFirestore.synced` so a reader can see the `confirmed` flag Firebase already provides — the flow is already `Flow<Synced<GroupSyncDocument>>` and only `private`, and `observe()` drops the flag with `.map { it.value }`. Nothing is built here: expose it as `observeWithPendingWrites`, matching the accessor `UserSyncFirestore` already has and the preferences reader already uses. `UserSyncFirestore` needs no change. Verify `./gradlew :core:compileKotlinMetadata` passes and the existing E2E group-calendar tests still pass unchanged

## 2. The offline lever for the E2E suite

- [x] 2.1 Add an infra helper in `app/androidApp/src/androidTest/.../e2e/infra/` that suspends `Firebase.firestore.disableNetwork()` / `enableNetwork()` around a block, resolving the same Firestore instance the app uses; verify with one throwaway test that a `Source.SERVER` read fails while the network is disabled and succeeds after
- [x] 2.2 Document the helper in `firebase/test/README.md` alongside the existing seeding notes; verify the documented usage compiles as written in a test

## 3. The per-month event readers take the cache path

- [x] 3.1 `PersonalEventFirestore.get`: switch to `userSyncFirestore.observeWithPendingWrites`; on an emission with `confirmed == false`, re-read the open months with `Source.CACHE`, merge into `known` by id, emit, and attempt no server query; verify with an E2E test that adds a shift with the network disabled and sees it on the day sheet and the month grid
- [x] 3.2 `GroupEventFirestore.get`: same change against `GroupSyncFirestore.observeWithPendingWrites`, preserving the `assigneeId` filter on the cache query so a revoked user's query keeps its required filter; verify with an E2E test that adds a group shift with the network disabled, and that the existing revoked-member tests still pass
- [x] 3.3 `PersonalOneOffEventFirestore.get`: same change, and do **not** call `recordChecked` on the cache path — a pending month has not been checked against the server; verify with an E2E test that saves a one-off spanning two months with the network disabled and sees it on days in both months
- [x] 3.4 `GroupEventExtrasFirestore.get`: same change; verify with an E2E test that writes a private note on a group shift with the network disabled and sees the note on the row
- [x] 3.5 Put back, on each reader changed in 3.1–3.4, the noise filter `observe()` used to apply for them: `distinctUntilChanged()` on the reader's **emitted list** rather than on the marker, since `confirmed` can no longer be filtered out ahead of the decision that reads it. Verified at E2E level rather than by unit test — the datasources take `FirebaseFirestore` concretely and the project has no fake for it, so there is no seam to drive a fake marker flow through (see *Verification* in `design.md`). The full suite passing with the extra emissions in place is what shows nothing re-renders spuriously
- [x] 3.6 Add a line to `firebase/firestore-usage.md` recording that the offline path costs cache reads only and shows as `ReadCache` in the audit; verify the stated numbers against the audit output of the 3.1 test run

## 4. A shift type created offline renders its events

- [x] 4.1 `PersonalEventTypesFirestore.snapshots`: take the cache path on an unconfirmed marker, as in group 3; verify with an E2E test that creates a shift with the network disabled, adds it to a day, and sees the event render with that type's name and colour — `PersonalEventRepositoryImpl.getEvents` drops an event whose type is absent from the types map, so this is what makes 3.1 complete for a new type

## 5. A failed server read keeps what is known

- [x] 5.1 `PersonalEventFirestore`: wrap the `Source.SERVER` delta where `known` is in scope; on failure re-read those months from `Source.CACHE` and merge, falling back to `known` unchanged if that fails too, logging the failure as an error. Not unit-testable (no Firebase fake) and not stageable in E2E either: `disableNetwork()` always yields `confirmed == false`, which takes the cache path and never reaches the server query. Kept as defence in depth for a transient or denied read; the user-facing outcome it protects is covered by 5.4
- [x] 5.2 Apply the same inner fallback to `GroupEventFirestore`, `PersonalOneOffEventFirestore`, `PersonalEventTypesFirestore` and `RevokedGroupFirestore`, with `PersonalOneOffEventFirestore` recording a month as checked only when the server actually answered. Same verification limits as 5.1
- [x] 5.3 Apply it to `GroupEventExtrasFirestore` and give it the terminal `.catch` it currently lacks, emitting an empty note list rather than letting the throw reach the calendar flow it is combined into. Same verification limits as 5.1; that the notes path stays alive is covered by 3.4 and the full suite
- [x] 5.4 Verify the never-synced-month case end to end: seed a month through `FirestoreRest` that the device has never read, move the user's sync marker for it, disable the network, open the calendar, and assert every month the device *does* hold still renders — this is the blank-calendar regression and spans all of 5.1–5.3

## 6. Integration across the whole offline path

- [x] 6.1 E2E: add two shifts with the network disabled, re-enable it, and assert both are on the calendar exactly once and present on the server through `FirestoreRest`; verify no duplicate document and no reverted row
- [x] 6.2 E2E: assert nothing marks an unsent event — the row added offline matches the rendering of a seeded one, and no error, snackbar or retry prompt appears for it
- [x] 6.3 E2E: delete an event with the network disabled and assert it stops being shown immediately, then that it is soft-deleted on the server after reconnect
- [x] 6.4 Run the full suite (`./gradlew :app:androidApp:connectedDebugAndroidTest`) and confirm no existing test regressed; verify the run is green without any addition to `.github/known-flaky-e2e-tests.txt`
- [x] 6.5 Resolve `design.md`'s open question: if the instrumented suite cannot restart the process with a mutation still queued (each test runs in a fresh process with app data cleared), record the *closed before reconnecting* scenario as manually verified and note how it was checked
