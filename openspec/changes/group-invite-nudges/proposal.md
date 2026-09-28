# Proposal

## Why

Swaps, the traceable A→B→C chain and notifications only exist inside a group, so a user who never
creates or joins one never sees what sets Turnia apart. Today the only nudge is the empty Groups tab.
Creating a group is also slowed down by having to define its first types by hand, and the group then
sits empty with nothing asking the creator to invite anyone.

## What Changes

- **Team prompt.** Once the user has been using the app, a one-time sheet asks "Do you work with a
  team?" and offers Create a group, I have a code, and Not now.
  - It is not shown on first launch. It appears once the count of events added on the device
    (already kept by `SharePromptRepository`) reaches a threshold from Remote Config, with an
    on/off switch shipped off and a proposed threshold of 5.
  - It shows only if the user belongs to no group, and only on their own calendar.
  - It shows once per device and is remembered in DataStore; nothing is written to Firestore.
  - If it becomes due together with a share prompt milestone, the team prompt wins and the share
    prompt waits for the next time the calendar opens.
- **Default types when creating a group.** The group form comes with M, T, N and MT already
  proposed as its event types, so a group can be created by giving it a name. The proposed types
  can be removed or edited before saving; the rule that a group is created with at least one type
  still holds.
- **Invite right after creating.** Saving a new group leads straight to "Your group is ready. Invite
  your colleagues", with the invitation link share button up front, instead of an empty group.
- **"Only you here" card.** While a group has a single member, its detail screen shows a card
  inviting the user to bring their team, with the share button. It derives from `memberUids`, which
  is already read, so it costs nothing.
- **Analytics**:
  - `onboard_team_shown` when the team prompt is shown;
  - `onboard_team_answered` with `choice` = `create` | `join` | `dismissed`.

  Shares from the new entry points keep logging `group_invite_shared`.

## Capabilities

### New Capabilities
- `team-prompt`: when the "Do you work with a team?" sheet appears, what it offers, its Remote Config
  and its precedence over the share prompt.
- `group-invite-nudges`: default event types in the group form, the invite step after creating a
  group, and the single-member card.

### Modified Capabilities
- `analytics-events`: adds `onboard_team_shown` and `onboard_team_answered` with `choice`.

## Impact

- **UI** (`app/shared`):
  - `MyCalendarScreen` and the share-prompt host, which gains prompt precedence;
  - the group creation form;
  - the post-create navigation;
  - `GroupDetail`;
  - strings in all five languages.
- **Core**:
  - Remote Config keys for the team prompt;
  - a DataStore flag for "team prompt shown";
  - preset type definitions shared with `shift-setup-onboarding`.
- **Analytics**: `choice` must be registered in the GA console.
- **Firestore**: no schema change.
- **E2E**: the group creation flow changes, because of the prefilled types and the invite step.
- **Dependencies**: reuses the presets introduced by `shift-setup-onboarding`.
