# Design

## Context

Why this is needed: see `proposal.md`. What it must do: see `specs/share-prompt/spec.md`.

This is how things stand today:

- **Remote Config.** `RemoteConfigService` sets in-app defaults before anything reads them, then
  maps each key into `FeatureFlags` through `getSafe`. That call falls back to the default on any
  error. GitLive's `get` has no `Int`, so numbers are read as `Long`. The ads banner already uses a
  number from here (`minAdsAction`).
- **Device settings.** There is one Preferences DataStore, `turnia.preferences_pb`, which
  `AppConfigRepositoryImpl` reads for `onboarding_seen`. `CLAUDE.md` says device settings that
  exist before any account are read only through a repository.
- **Adding an event.** Both "add" paths live in `DayDetailSheetViewModel`, and both already call
  `adRepository.actionPerformed()`:
  - `addEventOfType(eventTypeUi)` handles a group or personal event with a type;
  - `saveOneOff(form)` handles a one-off, and it is only an add when `form.editing == null`.
- **Where the day sheet lives.** It is hosted in a `ModalBottomSheet` by `CalendarViewer`, which
  every calendar uses: the user's own, a group's and a colleague's.
- **Sharing text.** `TextSharer` is created with `rememberTextSharer()`, an `expect` composable
  rather than something Koin provides. On Android it starts an `ACTION_SEND` chooser.
- **Analytics.** Events are a sealed `AnalyticsEvent(name, parameters)`, logged through `Analytics`.
  There is no `sign_up` event today. `UserProvisioner.create` is where a new account's documents
  are written.
- **E2E.** `TestModules.kt` binds `Analytics` to `NoAnalytics`, which drops every event, and
  `AppConfigRepository` to `FixedAppConfigRepository`.
- **The landing page.** `firebase/hosting/index.html` sends the visitor to their store. Its
  `playUrl(code)` adds `referrer=code=CODE` only for invitation links; the root page's Play link
  carries no referrer.

## Goals / Non-Goals

**Goals:**
- A prompt that is rare, direct and switchable from the console, with no Firestore involved.
- Enough analytics to compare the two messages and follow an Android install to a sign-up.
- Everything the device decides is covered by E2E.

**Non-Goals:**
- **Sharing a group's invitation link.** The generic app link is used for both audiences.
- **A per-account "already shown" flag.** It is per device: a reinstall or a new phone starts again.
- **Per-user referral tracking** (who brought whom).
- **iOS install-to-sign-up attribution.** Apple does not hand a referrer to the installed app. For
  iOS, only downloads per campaign are visible, in App Store Connect.

## Decisions

### 1. Two Remote Config keys, parsed into `FeatureFlags`

| Key | Type in the console | In-app default | Maps to |
|---|---|---|---|
| `sharePromptEnabled` | Boolean | `false` | `FeatureFlags.sharePromptEnabled: Boolean` |
| `sharePromptMilestones` | String (JSON) | `"[]"` | `FeatureFlags.sharePromptMilestones: List<Int>` |

Remote Config has no array type, so the milestones travel as a JSON string.
`SharePromptMilestonesMapper` (data, next to `RemoteConfigService`, per review: how Remote Config
encodes a value is no business of the domain) parses them with `kotlinx.serialization` inside `outcomeCatching`, then keeps positive values,
removes duplicates and sorts them. Anything unparseable becomes an empty list: no milestones, no
prompt, no crash.

The feature is on only when the flag is `true` and the list is non-empty.

- *Rejected:* a single "negative means off" integer, as `minAdsAction` uses. It cannot hold several
  milestones, and an explicit Boolean is clearer to toggle per app with a Remote Config condition.

### 2. The counter and the last milestone live in the existing DataStore

This adds two keys to `turnia.preferences_pb`:

- `share_prompt_events_added: Int`
- `share_prompt_last_milestone: Int`, holding the milestone's **value**, `0` for none

They are read and written through a new `SharePromptRepository`, placed next to
`AppConfigRepository` and backed by the same `DataStore<Preferences>`, so a single Koin binding
serves both.

The repository counts with `eventAdded(kind: EventKind)`, which returns nothing. It works in two
steps:

1. Inside one `dataStore.edit`, it increments the counter and reads the last milestone shown.
2. If the flags allow it, it takes the highest milestone `m` with `last < m <= count` and raises
   `pending` to `SharePrompt(audience, milestone = m)` (see Decision 4). It stores nothing else.

The milestone is stored when the sheet is **shown**, by `shown(prompt)`, not when it is picked. An
app closed while the prompt was still waiting behind the day sheet would otherwise lose that
milestone; now the next event offers it again. `shown` stores it in its own `dataStore.edit` and
logs `share_prompt_shown` only the first time, so a rotation does not log twice.

The milestone's value is stored, not its index, so a console edit can never make a passed milestone
come back or skip one wrongly (see the spec's "Milestones edited remotely" scenario).

- *Rejected:* keeping the counter in memory, as the ads counter does. A user who adds two or three
  shifts per session would reach 10 only by luck.
- *Rejected:* Firestore. The user asked for none.

### 3. Counting hooks sit next to `adRepository.actionPerformed()`

In `DayDetailSheetViewModel`:

- `addEventOfType` calls `eventAdded(EventKind.Typed)`, which maps to the `coworkers` audience;
- `saveOneOff`, in its `previous == null` branch only, calls `eventAdded(EventKind.OneOff)`, which
  maps to the `friends` audience. Edits never count.

An add is counted as it is issued: the count is launched just before the write, because the write
only returns once the server confirms it, which the prompt has no reason to wait for. The outcome
does not matter: the day sheet already treats an add as done once written, since Firestore queues
it offline, and an add later refused by the rules still counts, which is harmless for a prompt. An
add that is never issued, because nobody is signed in, is not counted.

### 4. The prompt is state, shown by `CalendarViewer` once the day sheet closes

A prompt must never be delivered as a one-off event. Following `CLAUDE.md`, the prompt is state:

- **Who holds it.** `SharePromptRepository` holds the pending `SharePrompt?` in a `StateFlow`, so
  it survives the day sheet's ViewModel going away, much as `NotificationRepository` holds a tapped
  notification.
- **Who shows it.** `CalendarViewer` observes it through a small `SharePromptViewModel`. It shows a
  second `ModalBottomSheet` only while no day sheet is open, so the prompt never stacks on the sheet
  that triggered it: it appears as the user closes the day, or immediately if the add closed it.
  Once on screen it calls `shown(prompt)`, which spends the milestone.
- **Where it shows.** Only on the user's own calendar and on group calendars, the ones events are
  added from: `CalendarViewer` hosts it only when its caller passes `showSharePrompt`. A colleague's
  calendar, which the user only reads, never shows it; a prompt still waiting there shows on the
  next own or group calendar opened.
- **When it clears.** Sharing or dismissing calls `answered(prompt, answer)`, which clears the state
  and logs the answer. Dismissing includes swiping the sheet away or pressing back. It acts only if
  `prompt` is still the one pending and returns whether it did, and the sheet shares only when it
  did: a second tap on "Share Turnia" before the sheet closes neither shares nor logs again.

The sheet holds three things:
- a title and one line of text for the audience;
- a filled "Share Turnia" button;
- a text button, "Not now".

- *Rejected:* a dialog. It interrupts more than a sheet, and the day detail is already a sheet, so
  a sheet reads as the natural next step.
- *Rejected:* a Snackbar with an action. It is too easy to miss, and it disappears on its own
  without an answer.

### 5. `TextSharer` becomes injectable

A `LocalTextSharer` `CompositionLocal` is added, provided at the root with `rememberTextSharer()`,
or with a `TextSharer` found in Koin when one is bound — which only the E2E module does.
The prompt reads `LocalTextSharer.current`, and the existing callers can move to it at the same
time. E2E provides a recording fake through the test application's root, so the test can assert
the exact text and link without driving the system's share sheet.

- *Rejected:* Espresso-Intents. It adds a dependency, and it only sees an opaque chooser `Intent`.

### 6. The link and the landing page

The shared text is `"<message> https://turnia.club/?utm_source=turnia_share&utm_medium=share_prompt"`.
It carries no `utm_campaign` (per review). Which audience was shared is in the prompt's own events,
so the link does not split installs by message:

- the message comes from `composeResources`, in all five languages;
- the URL is built in core, next to `InvitationLinkConfig`, so the host stays in one place.

In `index.html`, `playUrl` also forwards any `utm_*` parameters of `location.search` into
`referrer`, next to the existing `code=`:

```
referrer=utm_source%3Dturnia_share%26utm_medium%3Dshare_prompt
```

Firebase Analytics on Android reads the install referrer by itself and attributes the install's
`first_open` to that campaign. `InstallReferrer.codeOf` must keep reading only `code=`, and must
ignore the new keys.

### 7. Analytics events

These are added to `AnalyticsEvent`, whose names are fixed once shipped:

| Event | Parameters | When |
|---|---|---|
| `share_prompt_shown` | `audience`, `milestone` | The sheet appears. |
| `share_prompt_shared` | `audience`, `milestone` | "Share Turnia" is tapped. This is a tap rate, not a send rate: Android's chooser does not say whether anything was sent. |
| `share_prompt_dismissed` | `audience`, `milestone` | "Not now", a swipe or back. |
| `sign_up` | `method` (`google`, `apple`, `email`, `other`) | `UserProvisioner.create` succeeds, meaning a first account and not a returning sign-in. This is Google's recommended event name, so GA4 reports it natively. The method comes from the first provider that is not Firebase's own `firebase` entry, which Android lists first; any provider not named here is `other`. |

In GA4, "shared links that became accounts" is the `sign_up` count filtered by *first user source
= turnia_share*.

### 8. E2E

The test infra changes:
- `FixedAppConfigRepository` gains the two flags, set per test;
- `NoAnalytics` is replaced by a `RecordingAnalytics` that tests can read;
- a `RecordingTextSharer` is provided through `LocalTextSharer`.

The flows go in a new `SharePromptFlowsTest`, reusing `CalendarRobot` for adding events:

| # | Setup | Steps | Expected |
|---|---|---|---|
| 1 | milestones `[2]` | add 2 typed events | coworkers sheet; `share_prompt_shown{coworkers, 2}` |
| 2 | milestones `[2]` | add 2 one-offs | friends sheet |
| 3 | milestones `[2]` | 1 typed, then 1 one-off | friends sheet (the last event decides) |
| 4 | milestones `[2]` | reach 2, tap "Share Turnia" | the sharer received the coworkers text and the tagged link; `share_prompt_shared` |
| 5 | milestones `[2, 4]` | "Not now" at 2, then 2 more | nothing at 3, sheet at 4; `share_prompt_dismissed` then `shown{…, 4}` |
| 6 | milestones `[2]` | add 1 one-off, edit it twice | no sheet |
| 7 | flag off, milestones `[2]` | add 3 | no sheet, no `shown` |
| 8 | flag on, milestones `"2, 4"` | add 3 | no sheet, app keeps working |
| 9 | flag off, milestones `[1, 2, 3]` | add 2, turn the flag on, add 1 more, "Not now", add 1 more | one sheet, for 3; nothing after it; a single `shown` |
| 10 | milestones `[1]` | add 1 shift in the group's calendar | coworkers sheet there; `shown{coworkers, 1}` |

Each test runs in its own process with cleared data, so every counter starts at 0.

A test that asserts no sheet cannot just wait a while and look: a prompt that arrived late would
let it pass. The E2E module wraps the app's repository in a `CountingSharePromptRepository` that
counts each `eventAdded` once it has returned, by which time the prompt is decided. The robot waits
for the expected count, lets the UI go idle, checks the count was not exceeded, and only then checks
that no sheet is on screen. Adds wait for the day sheet to finish closing before the next day is
opened, since its veil would otherwise take the tap.

## Risks / Trade-offs

- **[The counter cannot be proven to survive a restart in E2E]** → Each test runs in a fresh process.
  Accepted: DataStore persists the count, and no manual check is planned.
- **[Attribution is Android-only and can't be end-to-end tested]** → The Play referrer only exists
  for a real install from Google Play, and no manual check is planned. On iOS,
  App Store campaign links could add downloads per campaign later; that needs the provider token
  from App Store Connect (see Open Questions).
- **[The landing page is shared with invitations]** → `playUrl` changes. It must keep producing
  `referrer=code=CODE` for `/join/CODE` exactly as today. `InstallReferrer` parses only `code=`.
- **[A reinstall shows the milestones again]** → Accepted, since the state is per device by
  decision. The milestones are far apart.
- **[Remote defaults]** → The in-app default is off, so a release that ships before the console
  parameters exist shows nothing. The console parameter must also default to `false`, with a
  per-app condition turning it on.
- **[Stale `FeatureFlags` at startup]** → Remote Config activates the last fetched values on
  launch, so a milestone change takes effect after the next fetch, at most a day later in release.
  That's acceptable for a growth prompt.

## Migration Plan

1. Create both Remote Config parameters in the console, disabled.
2. Ship the app and deploy the landing page: `firebase deploy --only hosting`. The order doesn't
   matter, since the page change only adds referrer parameters.
3. Enable the flag for the iOS app with a condition. Enable it for Android once the app is on
   Google Play.
4. To roll back, set `sharePromptEnabled` to `false`.

## Open Questions

- **The final copy in each of the five languages.** Placeholders go in the tasks. The product owner
  should word them before release.
- **iOS campaign links.** Should the shared link carry App Store `pt`/`ct` campaign tokens for iOS
  visitors? That needs the provider token from App Store Connect, and it only affects the landing
  page.
