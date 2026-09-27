# Proposal

## Why

Google Analytics sees almost nothing of how Turnia is used. Today it gets `screen_view`, `sign_up`,
`group_created`, `group_event_created`, the two join events and the share prompt's three. The
shift swap is what Turnia exists for, and nothing about it is reported. Neither are personal
events, one-offs, event types, shared calendars, people leaving, or notification taps. So we cannot
tell whether new users get past their first event, whether offered shifts get covered, or where
people give up.

## What Changes

- **Events** report what users create, edit and delete:
  - `personal_event_created` / `personal_event_deleted`
  - `one_off_event_created` (`all_day`, `multi_month`) / `one_off_event_updated` / `one_off_event_deleted`
  - `group_event_deleted`, next to the existing `group_event_created`
  - `event_notes_saved`
  - `personal_event_type_created` / `group_event_type_created`
- **The swap cycle** is reported end to end: `swap_offered`, `swap_withdrawn`, `swap_taken`,
  `swap_returned`, and `swap_take_lost` when somebody else took the shift first.
- **Group growth**:
  - `group_invite_shared` (the share button's sheet opened)
  - `invitation_opened` (`via`: `link` or `install_referrer`)
  - `join_request_rejected`
  - `group_created` gains `auto_approve` and `type_count`
- **Shared calendars**: `calendar_shared`, `calendar_share_revoked`, `shared_calendar_viewed`,
  `shared_calendar_hidden`.
- **Churn**: `group_left`, `member_removed`, `group_deleted` and `account_deleted`.
- **Notifications**: `notification_opened` (`type`).
- **Sign-in**: Google's recommended `login` (`method`) when a user signs in to an existing account,
  never for a session restored at launch.
- **User properties**: `group_count`, `is_admin` and `app_language`, so any event can be broken
  down by them.
- Parameters never carry an id, a name, a username, an invitation code or free text.
- Existing event names do not change. The console groups by the literal name, so renaming one
  would orphan its history.

## Capabilities

### New Capabilities
- `analytics-events`: which user actions the app reports to Google Analytics, when each one counts
  (only after it succeeds), the parameters and user properties each carries, and the guarantee
  that none of them identifies a person.

### Modified Capabilities

## Impact

- **core**:
  - `AnalyticsEvent` gains the new events.
  - `Analytics` gains `setUserProperty`, implemented in `AnalyticsImpl` over GitLive's
    `FirebaseAnalytics.setUserProperty`.
  - `GroupRepositoryImpl`, `PersonalEventRepositoryImpl`, `UserRepositoryImpl`,
    `NotificationRepositoryImpl` and `InvitationLinkRepositoryImpl` log after their operations
    succeed.
  - `PersonalEventRepository.saveEventType` learns whether the type is new.
  - `UserRepositoryImpl` logs `login` and `GroupRepositoryImpl.getGroups` keeps `group_count` and
    `is_admin` up to date, with no new Firestore listener.
- **app/shared**:
  - The group screen logs `group_invite_shared`.
  - `ExternalCalendarViewModel` logs `shared_calendar_viewed`.
  - The language code sets `app_language`.
  - `DemoAnalytics` implements the new method.
- **E2E**: `RecordingAnalytics` records user properties, and the existing flows (`CalendarFlowsTest`,
  `SwapFlowsTest`, `GroupFlowsTest`, `SharedCalendarFlowsTest`, `AuthFlowsTest`) assert the events
  they already trigger.
- **Console (manual)**: register the parameters and user properties as custom dimensions, or they
  only show in DebugView and BigQuery.
- **Not affected**: Firestore reads and writes, the security rules, Cloud Functions, push payloads.
- **Builds on** `analytics-collection`: debug builds, the E2E suite and Test Lab devices already
  report nothing, so the new events count real users only.
- **Docs**: `CLAUDE.md` gains a short *Analytics* section: where events are logged and the no-PII
  rule.
