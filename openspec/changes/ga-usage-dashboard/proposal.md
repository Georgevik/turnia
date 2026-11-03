# Proposal

## Why

The app already sends about 45 analytics events and three user properties, but nobody can see how
Turnia is used: no report in the GA4 property is built around them, and the custom definitions they
need are registered by hand with no written list of how. TUR-49 asks for a usage dashboard that the
developer, a GA beginner, can build from a document in the repo, without an agent touching the
console.

## What Changes

- A new guide, `docs/analytics-dashboard.md`, written in Spanish, that walks click by click through:
  - the property settings the dashboard depends on: event data retention raised to 14 months, and
    reporting identity set to *Blended*, so a user on two devices counts once;
  - registering every custom definition: each event-scoped and user-scoped dimension the app sends,
    plus `event_count` as a **custom metric**, so the group events a move to a group creates can be
    summed;
  - a report collection **"Turnia"** in the Library with five reports: *Daily summary* (active and
    new users per day), *Events created* (personal-type, one-off and group events per day, and
    events per user), *Groups created* (`group_created` per day), *Feature use* and *Users*
    (sign-in method, app language, group count);
  - three saved Explorations: *Activation funnel*, *Onboarding and prompts*, *Retention* (D1, D7,
    D30 cohorts);
  - what each result should look like, and what to do when a dimension shows `(not set)` or is
    missing: definitions only fill in from the moment they are registered, they take 24 to 48 hours,
    and debug builds never send anything.
- The *Analytics* section of `CLAUDE.md` points to the guide, and its list of custom definitions
  separates dimensions from metrics, so a new numeric parameter is registered the right way.
- **No new event, parameter or user property.** Every question the dashboard answers is already
  logged. The explore with the user (the `## Explore` comment on issue #49) rejected the only
  candidate, a `push_enabled` user property.

Out of scope, by the user's decision: running-total curves, Looker Studio, and the exact number of
groups that exist today. GA counts what happens, not what exists, so groups are reported as
`group_created` per day.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. The app's behavior does not change: this change is documentation and console setup that the
developer performs by hand. It sets `skip_specs: true`.

## Impact

- New file: `docs/analytics-dashboard.md`.
- Edited: `CLAUDE.md`, *Analytics* section.
- No Kotlin, Cloud Functions, Firestore rules, fixtures or tests change.
- The GA4 property is changed by the developer following the guide, never from the repo or by an
  agent.
