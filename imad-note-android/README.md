# Imad Note (Android)

Offline-first notes, tasks and projects in one calm workspace.

- **Stack:** Kotlin, Jetpack Compose (Material 3), Room, DataStore, Coil, Navigation Compose.
- **Min SDK** 26 (Android 8.0), **target/compile SDK** 34.
- **Languages:** English and Arabic (RTL handled by Compose).

## Build

```bash
./gradlew assembleRelease   # signed with the debug key for side-loading
./gradlew testDebugUnitTest # JVM unit tests
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

## Structure

| Package | Contents |
|---|---|
| `domain` | Pure scheduling rules (due buckets, reminders, recurrence) — unit tested |
| `data` | Room entities/DAOs, repositories, attachment storage, settings |
| `reminders` | AlarmManager scheduling, notification channel, boot/time-change rescheduling |
| `ui/*` | One package per feature: home, tasks, notes, projects, search, settings, capture |

## Data safety

- No destructive migrations: schema changes must add a Room `Migration` (schemas are exported to `app/schemas`).
- Notes use `@Upsert`, never `REPLACE`, so saving never cascades away checklist items or images.
- Deleting a project or note unlinks its tasks (`SET NULL`) rather than deleting them.
- Images are copied into app-private storage; orphaned files are cleaned up on start.
