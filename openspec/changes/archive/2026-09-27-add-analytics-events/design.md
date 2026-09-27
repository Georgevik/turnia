# Design

## Context

Custom events go through `Analytics.log(AnalyticsEvent)`. `AnalyticsEvent` is a sealed class, and
it holds every event name and parameter key the app sends. `AnalyticsImpl` forwards to GitLive's
`FirebaseAnalytics`. Most events are logged in the repository, straight after the operation
succeeds, as in `GroupRepositoryImpl.createGroup` or `acceptJoinRequest(...).also { if (it.isSuccess) … }`.
`ScreenReporter` is the one exception: it logs from the app layer, because only navigation knows
the screen.

`analytics-collection` is now a spec, so debug builds, the E2E suite and Test Lab devices collect
nothing. The E2E suite replaces `Analytics` with `RecordingAnalytics`, and `DemoAnalytics` swallows
everything in demo mode.

Some observations shape the design:

- **Group event types**: `GroupRepositoryImpl.saveEventType` can tell a new type from an edited
  one, because `existing == null` means the type is new. The types a group is created with never
  pass through it: they go straight into `createGroup(NewGroup)`.
- **Personal event types**: `PersonalEventRepositoryImpl.saveEventType` cannot tell. Only
  `EventTypeDetailViewModel` knows, through `EventTypeDetailData.NewPersonal` vs `EditPersonal`.
- **Silent results**:
  - `PersonalEventRepository.addEvent` and `deleteEvent` return `Unit` and drop their datasource's
    `Outcome`.
  - `GroupRepositoryImpl.addEvent` logs `group_event_created` without checking the write.
- **Sign-in**: `authStateChanged` also emits the session restored at launch. Only the provisioner
  knows that an account is new, and it logs `sign_up`.
- **`getGroups()`** is not shared. Every collector opens its own listeners, and `MyCalendarViewModel`,
  the start screen, is always collecting it.

## Goals / Non-Goals

**Goals:**
- Every event in the spec is logged in one place, after success, with no new Firestore read, write
  or listener.
- Event names, parameter keys and user property names live in `core/domain/analytics` only.
- The E2E suite can assert the new events and user properties through the flows it already runs.

**Non-Goals:**
- `group_left` / `member_removed` do not say whether the member became revoked. The callable does
  not return it, and finding out would cost a read.
- No ad, premium or purchase events. Those features have no client flow yet.
- No server-side events from Cloud Functions. The push a taker triggers is already visible as
  `notification_opened` on the other side.
- Registering custom dimensions in the GA console stays a manual step (task 7).

## Decisions

### 1. Log in the repository, after success; the app layer only for what the repository cannot see
The repositories are the one place every writer passes through, so an event logged there cannot
be missed by a second screen that reaches the same action. Examples: the day sheet and the calendar
both add events, and the group screen and the members list both remove members.

Three actions never reach a repository, so the app layer logs them:
- `group_invite_shared`: the share sheet is `TextSharer`, called from a composable.
  `ShareInvitationAction` gets an `onShared` callback into `GroupDetailViewModel`.
- `shared_calendar_viewed`: `ExternalCalendarViewModel.init` for `ExternalCalendarData.Personal`,
  next to the `adRepository.actionPerformed()` that already counts the same visit.
- `app_language`: the language lives in `app/shared` (`AppLanguage`, `currentAppLanguage()`).

*Alternative*: log everything from ViewModels. That spreads each event over every screen that
triggers it, and a new entry point silently goes unreported.

### 2. Success means the `Outcome` said so
Every logged action checks its `Outcome`. The code that currently drops it changes:
- `PersonalEventRepository.addEvent` / `deleteEvent` keep their `Unit` signature and log only when
  the datasource's `Outcome` succeeds. Changing their return type would touch every caller for no
  user-visible gain.
- `GroupRepositoryImpl.addEvent` logs `group_event_created` on the write's success.
- Swap functions log on `Outcome.Success`. `takeEvent` additionally maps
  `SwapError.TakenBySomeoneElse` to `swap_take_lost`, and no other error is reported.

### 3. `setOnSwap` picks the event by the flag it wrote
`setOnSwap(onSwap = true)` logs `swap_offered` and `onSwap = false` logs `swap_withdrawn`. When
`returnEvent` puts a shift back on offer, that is a server write, not `setOnSwap`, so it is reported
as `swap_returned` only.

### 4. Creation vs. edit for event types
- **Group types**: `saveEventType` logs `group_event_type_created` when `existing == null`.
  `createGroup` logs one per `NewGroup.types` entry, then `group_created(auto_approve, type_count)`.
- **Personal types**: `PersonalEventRepository.saveEventType(type, isNew: Boolean)`. The ViewModel
  already knows, and asking the repository to find out would cost a cache read on every save.

*Alternative*: split personal types into `createEventType` / `updateEventType`. That is cleaner in
the abstract but doubles an API used by one screen, and the group side does not split either.

### 5. `login`: a sign-in is any user after a signed-out emission in this process
In `UserRepositoryImpl.init`, the auth collector remembers whether it has seen `null` since the
process started:
- The first emission is a user: the session was restored, so nothing is logged.
- A user arrives after a `null`: the user signed in from the sign-in screen, whichever provider
  they used.

`gatherUserInfo` receives that flag. When the profile is found it logs `login(method)`. When it is
`NotFound` the provisioner creates the account and logs `sign_up`, as today. The provider mapping
(`google` / `apple` / `email` / `other`, skipping Android's `firebase` entry) moves out of
`UserProvisioner` into a shared `signInMethod(FirebaseUser)`, so both events use the same words.

*Alternative*: log from `SignInViewModel`. It only sees the KMPAuth result, before the provisioner
decides whether the account is new, so it could not keep `login` and `sign_up` apart.

### 6. User properties are typed like events
`Analytics.setUserProperty(property: AnalyticsUserProperty)` takes a sealed class whose members
carry the name and the value as a string (GA's only type), for the same reason `AnalyticsEvent`
exists: a renamed property starts a new series.
- **`group_count` / `is_admin`**: computed in an `onEach` inside `GroupRepositoryImpl.getGroups()`,
  over non-revoked groups. A `MutableStateFlow` holds the last value reported, so identical
  emissions from the several collectors set nothing. This adds no listener, and the Calendar tab
  keeps the value fresh from launch.
- **`app_language`**: `AppLanguage.tag ?: "system"`, set in `RootViewModel.init` and again in
  `PreferencesViewModel.onLanguageSelected`. The ViewModel survives the activity recreation a
  language change causes, so the new value is set before the recreation.

*Alternative*: a dedicated collector over `getGroups()`. It would open three more listeners per
session, and the audit would count each attach.

### 7. `invitation_opened` and `notification_opened` are logged where the payload is parsed
- `InvitationLinkRepositoryImpl.opened` logs `via = link`, and `referred` logs `via = install_referrer`.
  Both log only when the code is valid, which is the existing early return.
- `NotificationRepositoryImpl.opened` logs the `type` only for the five constants the `when`
  already recognises. The recognised type string is reused as the parameter, so the parameter is
  bounded by that list.

### 8. Churn events
`group_left`, `member_removed`, `group_deleted` and `join_request_rejected` wrap their callables'
`Outcome`s, like `acceptJoinRequest`. `account_deleted` is logged in `deleteAccount`'s `onSuccess`
before `auth.signOut()`. The Analytics user id is then still the account's, so the event lands on
the right user and not on an anonymous one.

## Risks / Trade-offs

- [**`authStateChanged` emits `null` before the restored user on some launch**] → a restored
  session would log a spurious `login`. `AuthFlowsTest` asserts that a relaunch signed in logs no
  `login`. If iOS shows the problem, gate the flag on Firebase Auth's first emission after
  `currentUser` is known.
- [**A write that never completes offline**] → GitLive's `set` resumes on the server's commit, so an
  event created offline is logged only once the device reconnects, or never if the process dies
  first. That is accepted: the count is "events that reached the backend", which is the number
  that matters.
- [**Parameters are invisible in standard reports until registered**] → task 7 lists every
  custom dimension to create in the console. Until then they are still in DebugView and BigQuery.
- [**GA limits**] → 500 distinct event names, 25 parameters per event, 25 user properties. This
  change reaches about 40 events, at most 2 parameters each, and 3 user properties.

## Migration Plan

Nothing to migrate: new events start their own series, and existing ones keep their names. Rolling
back is reverting the release. Past events stay in GA.
