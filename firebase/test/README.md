# E2E fixtures

The worlds the Android E2E suite (`app/androidApp/src/androidTest/.../e2e`) seeds into the Firebase
emulators before every test. The build packs this folder into the test APK as assets; the test
wipes Auth and Firestore, creates the accounts under `auth` and writes every document under
`firestore`, bypassing the security rules.

## Running the suite

With an Android emulator running (API 33+ recommended), from the repo root:

```bash
./gradlew :app:androidApp:connectedDebugAndroidTest
```

Running a test from Android Studio's gutter works the same way. Every `connected…AndroidTest` task
starts the Firebase emulators itself — building the Functions first — unless something already
serves ports 9099, 8080 and 5001, and stops the ones it started when the build ends. Their output
goes to `firebase/build/emulators.log`. The Firebase CLI is looked up on `PATH` and under `~/.nvm`;
elsewhere, set `turnia.firebaseCli=/path/to/firebase` in `~/.gradle/gradle.properties`.

CI starts them on its own, and the task then leaves them alone:

```bash
cd firebase && firebase emulators:exec --only auth,firestore,functions --project turnia-23ebc "cd .. && ./gradlew :app:androidApp:connectedDebugAndroidTest"
```

The run installs the debug app on the emulator, clears its data before every test and uninstalls
it at the end. A copy of the app signed with another key (a Play build) has to be uninstalled first.
The Release workflow runs the same thing in the `android-e2e` job, and a red run blocks both uploads.

## `base.json`

| Who | What they have |
|-----|----------------|
| `alice` | Admin of **Urgencias** (manual approval, code `URG001`); personal type `Curso` and one event on it |
| `bruno` | Member of Urgencias, admin of **Planta** (auto-approve, code `PLA001`); shares his calendar with alice |
| `carla` | Member of Urgencias, holds `e5` |
| `dana` | In no group; shares her calendar with alice |
| `irene` | A pending join request to Urgencias |
| `nameless` | A profile with a blank name |
| `nuevo` | Nothing at all: no personal type, no group — the account the shift setup is for |

Urgencias' shifts: `e1` alice's, offered; `e2` alice's, taken by bruno; `e3` alice's, taken by bruno
and offered again; `e4` alice's, not offered; `e5` carla's, of the non-swappable type `NC`.

Every account's password is `Turnia-e2e-1`.

## `hidden-calendar.json`

Layered over `base` by `HiddenSharedCalendarFlowsTest`: alice's `private/preferences` already lists
dana under `hiddenSharedCalendars`, as if she had hidden it on another device. `base` gives nobody
a `private/preferences`, so this fixture is the only one that creates it — and with it the
`preferences` marker.

## `solo-group.json`

Layered over `base` by `AloneGroupFlowsTest`: dana is the only member, and admin, of **Consultas**
(code `CON001`, no auto-approve), and nuevo has a pending request to join it. Accepting that request
is how a group of one becomes a group of two.

## Placeholders

Dates are relative to the day the test runs, so the data never leaves the month on screen or the
retention window:

| Placeholder | Becomes |
|-------------|---------|
| `"$now"` | A timestamp, the same instant everywhere in one seed |
| `"$date(+N)"` | The day N days from today, `YYYY-MM-DD` |
| `"$yearMonth(+N)"` | The month of that same day, `YYYY-MM` |

## Sync markers are derived

A fresh install fetches only what a sync marker says has moved, so a document with no marker never
reaches the screen. The seeder (`Fixtures.kt`) writes `groups/{g}/sync/updates` and
`users/{u}/sync/updates` for every group and user in the fixture, the way the real writers would —
event months, pending join requests, group membership, and each user's `groupEvents` for the months of
the shifts they hold. The Functions emulator's trigger stamps `groupEvents` again a moment after the
seed; a test that counts calls waits for it to settle first. `preferences` is only derived for a user
whose `private/preferences` exists: in production it is written with that document, and a user who
has never picked a colour or hidden a calendar has neither. A marker a fixture writes itself wins.

## Changing the data model

The fixtures are written in the shape the app and the functions store today. When a field is
added or renamed, change it here too: the suite then fails on exactly the paths that read it.
