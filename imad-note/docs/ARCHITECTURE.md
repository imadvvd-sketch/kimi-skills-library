# IMAD NOTE — architecture and roadmap

Native Android app: Kotlin, Jetpack Compose, Room. Offline-first, no backend, no cloud sync.

## Layers

| Layer | Package | Notes |
|---|---|---|
| Domain | `domain/` | Pure Kotlin, no Android imports. Editor model (`EditorDoc`, `RichText`, `Markdown`, `Equation`). Covered by plain JVM tests. |
| Data | `data/db`, `data/repo` | Room entities and DAOs; repositories own transactions and keep the search index in step. |
| UI | `ui/` | Compose screens and view-models, one package per feature. `ui/theme` and `ui/components` are the design system. |
| Wiring | `AppContainer` | Manual dependency container (single process scope; a DI framework is not justified yet). |

## Decisions

- **One schema for all modules from version 1.** Tables for tasks, projects, events, user databases and
  attachments already exist, so later phases add screens rather than migrations. Any change from here
  needs a `Migration`; destructive fallback is deliberately off. Schemas are exported to `app/schemas`.
- **Blocks are a flat ordered list with an `indent`**, not a parent/child tree. Children are the following
  blocks with a larger indent. Reordering, nesting and toggles become list operations.
- **Editor operations are pure functions on an immutable document.** Undo/redo keeps earlier documents;
  typing in one block is coalesced into one step.
- **Inline formatting is stored as spans** (`start`, `end`, `style`, `value`) in JSON beside the text and
  re-anchored on every edit by `RichText.adjustForEdit`.
- **Backspace at the start of a block** is detected with an invisible zero-width sentinel in each field,
  because soft keyboards do not report it. The sentinel never reaches the model.
- **Editor state is Compose snapshot state, not a Flow**, so a keystroke is visible to the field in the same
  frame and the keyboard's composing text is not reset.
- **Autosave**: 600 ms debounce, plus an immediate flush on stop/close on an application-level scope.
- **Search** is an FTS4 table (unicode61 tokenizer, works for Arabic) written inside the same transaction
  as the content.
- **Attachments** are copied into app-private storage and shared out through a `FileProvider`;
  no storage permission is requested.
- **Language** uses AppCompat per-app locales; **theme** is stored in DataStore.
- **Navigation destinations are only listed once their module works**, so there are no dead buttons.

## Modules

| Module | What is implemented |
|---|---|
| Workspace | Multiple workspaces, switcher, favourites, pins, recent pages, nested pages, folders, trash |
| Editor | 20 block types, inline formatting, colours, alignment, nesting, slash commands, markdown shortcuts, undo/redo, autosave, icon and cover |
| Tasks | Quick add with shortcuts (`tomorrow`, `!3`, `#label`, Arabic `غدا`/`اليوم`), subtasks, priority, due date and time, reminders, repeat rules, labels, projects, attachments; Today, Upcoming, All, Priority, Projects and Completed views |
| Projects | Icon, colour, status, start date, deadline, progress, statistics, timeline, linked tasks and notes |
| Databases | 16 property types, Table/Board/Calendar/Gallery/List views, filters, sorts, grouping, hidden properties, relations across databases, formulas (language documented in `domain/db/Formula.kt`), type conversion |
| Calendar | Month, week, day (hour grid) and agenda views showing events, tasks and project deadlines; event editor with reminders |
| Quick capture | Note, task, checklist, idea, link, image and voice note in one step; share text, links and images from other apps into the Inbox |
| Search | Full-text across pages, page content, tasks, projects, records, events and file names, with filters, history and highlighted matches |
| Analytics | Completed, open, overdue, completion rate, streak, last 7 days, 12 weeks, 6 months, open tasks by priority, project progress — all from stored tasks |
| Backup | ZIP export of all content and files; restore replaces content atomically and rebuilds the index |

## Further decisions

- **No schema change since version 1.** Every module fitted the original tables, so installed copies keep their data without a migration.
- **Reminders** use `AlarmManager`; exact timing when the user allows exact alarms, otherwise Android may deliver a few minutes late. They are rescheduled after reboot and app updates.
- **The phone sidebar is a small custom drawer** (`SideDrawer.kt`), composed only while open.
- **Robolectric limitation:** at phone densities Robolectric never reports Compose idle while a text field is shown inside a Material `AlertDialog` (reproduced with a bare dialog and no app code). Flow tests create named items through repositories; the naming dialog is tested at Robolectric's default size (`DialogProbeTest`).

## Known limitations

- Editor: no selection across several blocks, no drag and drop (blocks move with the toolbar), no inline video/audio player, equations render a LaTeX subset as text, bookmarks have no fetched preview, no UI to create inline links.
- Databases: board cards move between columns with a long press menu rather than drag and drop; no per-record page content; Person is free text (single-user app).
- Calendar: events do not repeat; no import from the system calendar.
- Reminders can be delayed by Android battery optimisation if exact alarms are not allowed.
- No cloud sync; data lives on the device and in backup files the user saves.
- Nothing has been run on a physical device or emulator by the developer; please report device-specific issues.

## Testing

`./gradlew :app:testDebugUnitTest` runs JVM tests and Robolectric tests (database, view-models, and
end-to-end Compose flows). Screenshot tests write PNGs to `app/build/outputs/roborazzi`.
120 tests: JVM unit tests, Robolectric database/view-model tests and end-to-end Compose flows in light, dark, Arabic and tablet layouts.
