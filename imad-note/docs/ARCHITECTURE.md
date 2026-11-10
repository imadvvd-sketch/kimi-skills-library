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

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Architecture, build, schema | Done |
| 2 | Design system, light/dark, RTL | Done |
| 3 | Workspaces, sidebar, navigation, Home, Settings, Trash | Done |
| 4 | Block editor | Done, see limitations |
| 5 | Notes and pages (nesting, folders, favourites, pins, inbox capture) | Done |
| 6 | Tasks and projects | Schema only |
| 7 | Database engine (properties, views, relations, formulas) | Schema only |
| 8 | Calendar and reminders | Schema only |
| 9 | Global search UI, attachment library | Index and storage done, no search screen yet |
| 10 | Analytics, backup and restore | Not started |
| 11 | Performance work, migration tests | Not started |
| 12 | Release build, signing, store assets | Not started |

## Known limitations of the editor

- A text selection cannot span several blocks.
- Blocks move with the toolbar's up/down buttons; drag and drop is not implemented.
- Video, audio and file blocks open in another app; there is no inline player.
- Equations render a documented LaTeX subset as Unicode text (`domain/editor/Equation.kt`), not typeset math.
- Bookmarks store the link and an optional title; no preview is fetched (the app is offline-first).
- Links inside text (`InlineStyle.LINK`) are modelled and rendered but there is no UI to create one yet.

## Testing

`./gradlew :app:testDebugUnitTest` runs JVM tests and Robolectric tests (database, view-models, and
end-to-end Compose flows). Screenshot tests write PNGs to `app/build/outputs/roborazzi`.
Nothing has been run on a physical device or emulator yet.
