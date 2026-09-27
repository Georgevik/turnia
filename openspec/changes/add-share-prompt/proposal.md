# Proposal

## Why

Turnia grows when a user brings in the people they share shifts or plans with, but nothing in the app
asks them to. The goal is more downloads from a prompt that is direct, rare and timed to a moment
when the user has just got value from the app. The prompt should also measure whether it works,
down to whether the recipient created an account.

## What Changes

- **A share prompt at usage milestones.** The app counts the events the user adds. When the count
  reaches a milestone, it shows once a short sheet with one action, "Share Turnia", which opens the
  platform's share sheet with a link to the app. Each milestone shows once, whether the user shares
  or dismisses it.
- **Two audiences, chosen by the event that reached the milestone:**
  - an event with a type (a group shift or a personal typed event) targets **coworkers**;
  - a one-off event targets **friends**.
- **Remote Config** controls it without a release:
  - `sharePromptEnabled` (Boolean, default `false`) turns the feature on or off, per app, so it can
    be enabled on iOS before the app is on Google Play;
  - `sharePromptMilestones` (JSON string, e.g. `"[10, 100, 200]"`) lists the milestones.
- **On-device state only**, in the existing Preferences DataStore: the number of events added and
  the value of the last milestone shown. No Firestore reads or writes.
- **The link** is the landing page `https://turnia.club`, which already sends each visitor to their
  store, tagged with `utm_source`, `utm_medium` and `utm_campaign`. The landing page forwards those
  tags to Google Play's install referrer.
- **Analytics:**
  - three new events, `share_prompt_shown`, `share_prompt_shared` and `share_prompt_dismissed`,
    each with the audience and the milestone;
  - a new `sign_up` event when an account is created, so an install that came from a shared link
    can be followed to a sign-up.
- **E2E tests** for the prompt's behaviour.

## Capabilities

### New Capabilities
- `share-prompt`: when and how the app asks the user to share Turnia, what the shared link carries,
  and what it reports to analytics.

### Modified Capabilities
- None. There are no existing specs.

## Impact

- **core**:
  - `FeatureFlags` and `RemoteConfigService`, for the two new keys and their defaults;
  - `AppConfigRepository`, or a sibling repository over the same DataStore, for the counter and the
    last milestone;
  - `AnalyticsEvent`, for the four new events;
  - `UserProvisioner`, or wherever an account is first created, to log `sign_up`.
- **app/shared**:
  - `DayDetailSheetViewModel`, where both "add event" paths already exist (`addEventOfType` and
    `saveOneOff` for a new one-off);
  - a new share-prompt sheet;
  - `TextSharer`, made injectable so tests can replace it;
  - the strings, in all five languages.
- **firebase/hosting**: `index.html` forwards `utm_*` query parameters into the Play Store
  `referrer`, alongside the existing `code=`.
- **E2E**:
  - a recording `Analytics` and a fake `TextSharer` in the test modules;
  - new flow tests;
  - `FixedAppConfigRepository` gains the two flags.
- **Firestore**: none. **Cost**: none beyond Analytics events.
- **Console**: two new Remote Config parameters. They must be created with `sharePromptEnabled =
  false` before any release that reads them, since the in-app default is also `false`.
