# Tasks

Verification is E2E, as for the rest of the suite. Tasks before group 5 are checked by building.
Their behaviour is proven by group 5.

## 1. Flags and device state (core)

- [x] 1.1 Add `sharePromptEnabled` (Boolean, default `false`) and `sharePromptMilestones` (JSON string,
  default `"[]"`) to `RemoteKey`, `setDefaults` and `getFlags` in `RemoteConfigService`.
  - Parse the milestones in `data/config/mappers/SharePromptMilestonesMapper`, inside
    `outcomeCatching`. Keep positive values, remove duplicates and sort. Anything unparseable
    becomes an empty list.
  - Expose `FeatureFlags.sharePromptEnabled` and `FeatureFlags.sharePromptMilestones: List<Int>`.

  Verify that `./gradlew :core:compileAndroidMain :core:compileKotlinIosSimulatorArm64` passes.
- [x] 1.2 Add `SharePromptRepository` (interface in `domain/repository`, implementation in `data/`)
  over the existing `DataStore<Preferences>`.
  - Keys `share_prompt_events_added` and `share_prompt_last_milestone`.
  - `eventAdded(kind: EventKind)` increments the counter in one `dataStore.edit`, then picks the
    highest milestone above the last one shown and at or below the count, and raises `pending` with
    it. It returns nothing and stores no milestone. Nothing is picked when the flag is off or there
    are no milestones.
  - `shown(prompt)` stores the milestone's value once the sheet is on screen, and logs `shown` the
    first time only.
  - `pending: StateFlow<SharePrompt?>`, and `answered(prompt, answer)`, which clears it only if
    `prompt` is still pending and returns whether it was.
  - `SharePrompt(audience: SharePromptAudience, milestone: Int)`. `EventKind.Typed` maps to
    `Coworkers` and `EventKind.OneOff` to `Friends`.

  Bind it in `DataModule`. Verify that core compiles.
- [x] 1.3 Add the analytics events to `AnalyticsEvent`: `SharePromptShown`, `SharePromptShared` and
  `SharePromptDismissed`, each with `audience` and `milestone`, plus `SignUp(method)` named
  `sign_up`. Verify that core compiles.
- [x] 1.4 Log `sign_up` with the sign-in method once `UserProvisioner.create` succeeds, and never for a
  returning sign-in. The method is the first provider that is not `firebase` (Android lists
  Firebase's own entry first), `other` when it is none of `google`, `apple` or `email`. Verify that
  core compiles, then check it in GA4 DebugView on a first sign-in (8.2).
- [x] 1.5 Build the share link next to `InvitationLinkConfig`:
  `https://turnia.club/?utm_source=turnia_share&utm_medium=share_prompt` (no `utm_campaign`, per review).
  Verify it is used by 3.2.

## 2. Counting (app)

- [x] 2.1 In `DayDetailSheetViewModel`, call `sharePromptRepository.eventAdded(...)` next to
  `adRepository.actionPerformed()`:
  - `EventKind.Typed` in `addEventOfType`;
  - `EventKind.OneOff` in `saveOneOff`'s `previous == null` branch only.
  - Each is counted as it is issued, launched before the write, and only when a user is signed in:
    an add the repositories would drop is not counted.

  Verify that `./gradlew :app:shared:compileAndroidMain` passes.

## 3. The prompt (app)

- [x] 3.1 Add `LocalTextSharer`, provided at the root with `rememberTextSharer()`, and move
  `GroupDetailScreen` and `AboutScreen` to read it. Verify that both still share and copy (E2E group
  tests pass in 5.3).
- [x] 3.2 Add `SharePromptViewModel` and a `SharePromptSheet`, shown by `CalendarViewer` as a
  `ModalBottomSheet` only while no day sheet is open, and only on the user's own calendar and on
  group calendars (`showSharePrompt`), never on a colleague's.
  - The sheet has a title and one line for the audience, a "Share Turnia" button and a "Not now"
    button.
  - Sharing calls `LocalTextSharer.current.share(message + link)`, only if `answered` says this tap
    was the one that answered, so a double tap shares once.
  - Either answer, a swipe or back calls `answered(prompt, answer)`.
  - It calls `shown(prompt)` when it appears, then logs `shared` or `dismissed`.

  Verify that the app compiles for Android and iOS.
- [x] 3.3 Add the strings for both audiences to `values/`, `values-es/`, `values-fr/`, `values-de/` and
  `values-it/`: the title, the line, the share message and the two buttons. Mark the non-English
  copy for the product owner's review. Verify that every key exists in all five files.

## 4. Landing page (hosting)

- [x] 4.1 In `firebase/hosting/index.html`, make `playUrl` forward the page's `utm_*` query parameters
  into the Play `referrer`, next to `code=`. Keep `/join/CODE` producing exactly
  `referrer=code=CODE` as today.

  Verified by running the page's own `playUrl` for four inputs (join code alone, `utm_*` alone,
  neither, both): `/join/ABC123` still yields exactly `referrer=code%3DABC123`.

## 5. E2E

- [x] 5.1 Test infrastructure:
  - `FixedAppConfigRepository` gains `sharePromptEnabled` and `sharePromptMilestones`, set per test;
  - `NoAnalytics` becomes a `RecordingAnalytics` the tests can read;
  - a `RecordingTextSharer` is provided through `LocalTextSharer` in the test application.

  Verify that the existing suite still passes.
- [x] 5.2 Add `SharePromptFlowsTest`, covering design Decision 8's ten cases: coworkers, friends,
  the last event deciding on mixed use, sharing with the exact link, "Not now" followed by the next
  milestone, edits not counting, the flag off, malformed milestones, several milestones passed at
  once, and a shift added in a group calendar. Add a `SharePromptRobot` if the steps repeat.
  - A test asserting no sheet waits for the add to be counted (`CountingSharePromptRepository`)
    before it looks, never for a fixed time.
  - Each add waits for the day sheet to finish closing before the next day is opened.

  Verify that
  `./gradlew :app:androidApp:connectedDebugAndroidTest` passes.
- [x] 5.3 Update the E2E path count in `CLAUDE.md` and `.github/workflows/android-e2e.yml`. Verify the
  number matches the suite.

## 6. Docs and checks

- [x] 6.1 Document the two Remote Config parameters and the `sign_up` / `share_prompt_*` events where
  the project records its flags and events (`CLAUDE.md` *Monetization* sits next to ads, or a new
  short section). Verify it names the console defaults.
- [ ] 6.2 Check by hand, on a device, with the milestones at `[3]`:
  1. Add 2 events.
  2. `adb shell am force-stop com.geoviksoft.turnia.debug`.
  3. Add 1 more, and check that the sheet shows.
  4. Share, and check that the link opens the landing page.
- [ ] 6.3 Check attribution: install through a Play internal-test link carrying the `utm_*`, sign up,
  and see `first_open` attributed to `turnia_share` and `sign_up` in GA4 DebugView.
