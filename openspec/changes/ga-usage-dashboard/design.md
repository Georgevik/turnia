# Design

## Context

See proposal.md for the motivation. What shapes the approach:

- Every event the dashboard needs is already logged (`AnalyticsEvent`, `AnalyticsUserProperty`), and
  the parameters they carry match the custom definitions listed in `CLAUDE.md`'s *Analytics* section.
- The app calls `setUser` on sign-in, so GA can count a person on two devices once, but only if the
  property's reporting identity uses the user id.
- Debug builds and Test Lab devices send nothing, so the dashboard shows production releases only.
- Agents never touch the GA console. Everything below becomes steps in
  `docs/analytics-dashboard.md`, followed by the developer, who is a GA beginner.

## Goals / Non-Goals

**Goals:**
- Each question agreed in the explore maps to exactly one place in GA, so the guide has a single path
  per question.
- The guide works on a fresh property and on one where some definitions are already registered.

**Non-Goals:**
- Automating the setup through the GA Admin API, or shipping a Looker Studio template.
- Changing what the app logs.

## Decisions

### Library collection for daily numbers, Explorations for funnels and cohorts

Daily counts and breakdowns go in a Library collection "Turnia", because its reports stay in the left
menu and open in one click. Funnels and retention cohorts exist only in Explorations, so those three
are saved there. Looker Studio was considered and dropped by the user: the only thing it added was
running-total curves, which were also dropped.

| Question | Where | Built from |
|---|---|---|
| Unique and new users per day | *Daily summary* (detail report) | *Date* × built-in *Active users*, *New users* |
| Events created per day, and events per user | *Events created* (detail report) | *Event name* filtered to `personal_event_created`, `one_off_event_created`, `group_event_created`; *Event count*, *Total users*, *Event count per active user* |
| Groups created per day, and feature use | *Feature use* (detail report) | *Event name* filtered to `group_created` and the swap, hand-back, shared-calendar, move, notes and membership events; plus the `events_moved` metric |
| Sign-in method, language, groups | *Users* (detail report) | `method` on `sign_up` / `login`; user properties `app_language`, `group_count`, `is_admin` |
| Activation | *Activation funnel* (funnel exploration) | `first_open` → `sign_up` → `onboard_shift_completed` or `onboard_shift_skipped` → `first_event_added` |
| Onboarding and growth prompts | *Onboarding and prompts* (free-form exploration) | `onboard_shift_*` by `via`/`interacted`, `onboard_team_*` by `choice`, `share_prompt_*` by `audience`/`milestone` |
| Retention D1/D7/D30 | *Retention* (cohort exploration) | First touch, return on any event, daily granularity |

### "Events added" is the sum of three creations; moves are reported apart

The total of events added is `personal_event_created` + `one_off_event_created` +
`group_event_created`. A move to a group converts events that were already counted when they were
created, so adding `personal_events_moved` to the total would count them twice. The moves are
reported separately, as the sum of `event_count`.

Every report is a detail report, even the daily summary, because a detail report keyed on *Date*
gives exactly one row per day. *Events created* has to filter on event name, and a filter applies to
the whole report, so its *Event count per active user* averages over the users who added an event.
The average over all users is that report's total divided by the *Daily summary*'s active users.
That is also why `group_created` lives in *Feature use* and not in *Events created*: there it would
add groups to the total of events added.

### `event_count` is registered as a custom metric

A custom dimension groups by value. Summing the events moved needs a **custom metric**, named
`events_moved` in the console. `event_count`
stays registered as a dimension too, as `CLAUDE.md` already lists it: GA allows the same parameter to
be both. The other numeric parameters (`type_count`, `milestone`, `custom_type_count`,
`skipped_count`) stay dimensions only. The dashboard breaks them down by value and never sums them,
and adding metrics nobody reads would only spend the property's quota.

### Screens use GA's built-in dimension

The app's `screen_view` carries `screen_name`, which Firebase maps to GA's built-in *Page title and
screen name* dimension. The guide uses that dimension. If the console refuses `screen_name` as a
custom dimension, the guide says to skip it, since nothing is lost.

### Guide language and menu names

The guide is in Spanish. Menu and field names are written as the Spanish GA console shows them,
followed by the English name in parentheses when the two differ, so it can still be followed with the
console in English.

### Property settings come first

Two settings change what every report shows from that moment on, so they are the guide's first
steps:
- event data retention set to **14 months**, since Explorations only see the retained window (two
  months by default);
- reporting identity set to **Blended**, which counts a signed-in user once across devices.

### The guide is checked against the code

No test can exercise a doc. Instead, every event, parameter and user property name the guide uses is
checked against `AnalyticsEvent.kt` and `AnalyticsUserProperty.kt` before the PR, so a typo cannot
send the developer looking for a series that never arrives.

## Risks / Trade-offs

- [New definitions only fill in from the moment they are registered, and take 24 to 48 hours] → The
  guide registers them first, and explains `(not set)` and empty dimensions in a troubleshooting
  section.
- [Data thresholds: with few users, and Google signals on under *Blended*, GA hides rows] → The guide
  explains the *thresholds applied* notice, and how to switch identity to *Device-based* to see every
  row, at the cost of counting devices instead of people.
- [Retention lost before the 14-month setting is applied can't be recovered] → It is step one of the
  guide.
- [GA's console moves menus around] → Each step names the exact field as well as the path, so a
  moved menu can still be found through search.
- [`first_event_added` is per device and `sign_up` is per new account] → The activation funnel notes
  that a reinstall or a second device can make a step look larger than the one before it.
