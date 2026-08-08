# Turnia

Multiplatform (KMP) application for managing and swapping shifts in the healthcare sector. It lets nurses and doctors propose, accept and trace shift changes in an orderly way, replacing informal coordination over WhatsApp.

## The problem

Shift changes are currently handled over WhatsApp, which causes confusion and errors, especially when **chained changes** happen:

> A swaps their shift with B. Later, B swaps that same shift (originally A's) with C.

Without a single source of truth, it is easy to lose track of who actually covers each shift. Turnia solves this by keeping **full traceability** of every change and a state that is always consistent.

## Goals

- Single source of truth about who covers each shift at any given moment.
- Full traceability of the chain of changes (auditable history).
- Explicit acceptance flow: a change is only effective when both parties confirm it.
- Conflict prevention (overlaps, double assignments, changes over already-reassigned shifts).
- Notifications to the parties involved on every proposal or confirmation.

## Domain concepts

| Concept | Description |
|---------|-------------|
| **User** | Healthcare professional (nurse/doctor) with assigned shifts. |
| **Shift** | Work slot assigned to a user on a date (morning / afternoon / night / on-call). |
| **Change proposal** | A user's request to swap or hand over a shift. |
| **Confirmed change** | Proposal accepted by the counterpart; updates the shift's ownership. |
| **Change chain** | Ordered history of reassignments over the same shift. |

## Tech stack

- **Kotlin Multiplatform (KMP)** — shared business logic.
- **Compose Multiplatform** — shared UI (Android / iOS / Desktop).
- **Coroutines + Flow** — asynchrony and reactive state.
- **Koin** — dependency injection (DI).
- **Navigation 3 (Nav3)** — shared, back-stack-based navigation.
- **kotlinx.serialization** — serialization (navigation keys, DTOs).
- **SQLDelight** — local multiplatform persistence *(to be confirmed)*.
- **Firebase** — backend (authentication, database and notifications).

## Project structure

```
turnia/
├── composeApp/         # Shared UI (Compose Multiplatform)
│   ├── commonMain/
│   ├── androidMain/
│   └── iosMain/
├── shared/             # Business logic and shared domain
│   ├── commonMain/
│   ├── androidMain/
│   └── iosMain/
└── iosApp/             # iOS entry point (Xcode)
```

## Getting started

### Requirements

- JDK 17+
- Android Studio (with the KMP plugin) / IntelliJ IDEA
- Xcode (to build the iOS app, macOS only)

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

## License

To be defined.
