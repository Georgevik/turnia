# Tasks

## 1. Guide: property setup and custom definitions

- [x] 1.1 Create `docs/analytics-dashboard.md` in Spanish with an introduction (what the dashboard answers, that only production releases send data, that definitions fill in only from registration on, in 24 to 48 hours) and the property settings: event data retention to 14 months and reporting identity to *Blended*, each in numbered steps with the menu path and exact field names (Spanish console name, English in parentheses). Verify the file renders as Markdown and every step names a path, a field and a value.
- [x] 1.2 Add the custom definitions section: one table per scope (event-scoped dimensions, user-scoped dimensions, the `event_count` custom metric) giving *Dimension name* / *Scope* / *Description* / *Event parameter* or *User property* exactly as typed, plus the click path and the note to skip a definition that already exists. Verify the list matches every parameter in `AnalyticsEvent.kt` and every property in `AnalyticsUserProperty.kt`, with nothing extra.

## 2. Guide: the "Turnia" collection

- [x] 2.1 Add the steps to create the four Library reports (*Daily summary*, *Events created*, *Feature use*, *Users*) and the "Turnia" collection that publishes them. Each report gets the template to start from, dimensions, metrics and filters with exact values, how to save it, and what the result should look like. *Events created* states that the total of events added excludes moves. Verify every event name in the filters exists in `AnalyticsEvent.kt`.

## 3. Guide: Explorations

- [x] 3.1 Add the steps for the three saved Explorations: *Activation funnel* (funnel: `first_open` → `sign_up` → `onboard_shift_completed` → `first_event_added`), *Onboarding and prompts* (free-form, broken down by `via`, `interacted`, `choice`, `audience`, `milestone`) and *Retention* (cohort: first touch, any event, daily, D1/D7/D30). Each gets the steps to build and save it and what the result should look like, including the per-device caveat on the funnel. Verify every name used exists in `AnalyticsEvent.kt`.

## 4. Guide: troubleshooting

- [x] 4.1 Add the troubleshooting section: a dimension showing `(not set)` or missing, empty reports in the first 48 hours, debug builds sending nothing, the data-thresholds notice and the switch to *Device-based*, and a menu that has moved. Verify each case says what the developer sees and what to do.

## 5. CLAUDE.md

- [ ] 5.1 Update the *Analytics* section of `CLAUDE.md`: point to `docs/analytics-dashboard.md`, list `event_count` as a custom metric next to the dimensions, and say that a new parameter or property is also added to the guide's definitions table. Verify the section's list of definitions matches the guide's tables.

## 6. Integration check

- [ ] 6.1 Run a script over `docs/analytics-dashboard.md` that extracts every backticked snake_case name and checks it against the strings in `AnalyticsEvent.kt`, `AnalyticsUserProperty.kt` and the GA built-ins the guide uses (`first_open`, `screen_view`), and run `openspec validate ga-usage-dashboard`. Verify both report no mismatch.

## Workflow follow-up

- Open the PR `TUR-49 <summary>` with *What changes?* / *Why?* / *How to test it*, `Closes #49`, and move the issue to `testing`.
- Archive the change as the last commit of the PR, before merge.
