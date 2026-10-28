# Imad-note 2 — Design System

**Direction:** Calm Productivity.
**Personality:** premium, minimal, modern, elegant, organized, approachable.

Notion, TickTick and Keep were used only as *functional* references (what users need to do).
Nothing here copies their look. The visual direction follows the **Imad-note 2 brand board**
supplied by the product owner (icon, palette, Inter, screen layouts, Arabic-first copy).

---

## 1. Principles

1. **Quiet by default, clear when it matters.** Neutral surfaces carry content; colour is reserved
   for meaning (priority, overdue, success, selection).
2. **One hand first.** Primary actions live in the bottom third: navigation bar, Quick Capture
   button, editor toolbar, sheet actions.
3. **Flat, outlined surfaces.** Hierarchy comes from spacing, type weight and a 1 dp hairline —
   shadows are only for things that float (FAB, sheets, dialogs, menus).
4. **Every state is designed.** Loading, empty, success (with undo) and error states exist for
   every screen.
5. **Accessible is the baseline.** All text meets WCAG AA, every target is ≥ 48 dp, all icons
   have labels, layouts mirror in Arabic (RTL) and scale with the system font size.

---

## 2. Brand

### Name & wordmark
`Imad-note 2` — set in Inter SemiBold. The "2" is never stylised separately.

### App icon
A white **"i"** (for Imad) beside an **open page** leaning in — notes and ideas, one place.
Background: a calm navy-to-blue gradient (`#3D72AC` → `#1D3550`) with very faint distant hills,
echoing the landscape imagery of the brand board. The page uses a pale-blue gradient
(`#D6E6F7` → `#7FA8D6`) with a lighter curled corner.

Adaptive icon (background + foreground + monochrome for Android 13 themed icons); the artwork stays
inside the 66 dp safe zone. The same mark, on a rounded square, is the in-app brand mark
(Home top bar, More screen).

**Tagline:** "Notes • Tasks • Ideas • Life" / «كل ما تحتاجه في مكان واحد».

### Motif
The **page** — a soft rectangle with a curled corner — recurs in empty-state illustrations and
on tinted note cards (a tiny fold). Used at most once per screen region.

---

## 3. Colour

The palette from the brief is kept. Three values were darkened **only for text use**
because the originals fail WCAG AA on light backgrounds (measured ratios below);
the original hues are still used for fills, icons and the icon artwork.

### Light theme
| Token | Hex | Use | Contrast |
|---|---|---|---|
| `primary` | `#243B53` | FAB, selected nav, primary buttons, links | 11.5 on surface |
| `onPrimary` | `#FFFFFF` | content on primary | |
| `secondary` | `#527DAA` | fills, progress, focus rings (non-text) | 4.31 (UI ≥ 3 ✓) |
| `secondaryText` | `#3F6A96` | secondary *text* | 5.66 ✓ |
| `background` | `#F7F8FA` | app background | |
| `surface` | `#FFFFFF` | cards, sheets, editor | |
| `textPrimary` | `#202A35` | body & titles | 13.7 ✓ |
| `textSecondary` | `#5F6B7A` | meta, captions (brief: `#7B8794` = 3.45 ✗) | 5.11 ✓ |
| `border` | `#E4E8ED` | hairlines, card outlines (decorative) | |
| `outline` | `#8A96A3` | input borders, unchecked boxes | 3.0 (UI ✓) |
| `success` | `#2F7A64` | completed, saved (brief `#3A8D75` kept for fills) | 5.14 ✓ |
| `warning` | `#9A5B12` | medium priority text (brief `#D99545` kept for fills/accent) | 5.41 ✓ |
| `danger` | `#B42318` | overdue, high priority, destructive | 6.57 ✓ |

### Dark theme
Not an inversion: surfaces get *lighter* as they rise, and the primary shifts to the
softer `#82A9D6` so it doesn't glare.

| Token | Hex | Use | Contrast |
|---|---|---|---|
| `primary` | `#82A9D6` | | 6.27 on surface |
| `onPrimary` | `#111820` | | 7.31 |
| `background` | `#111820` | | |
| `surface` | `#1B2632` | cards, sheets | |
| `surfaceRaised` | `#223040` | menus, selected rows | |
| `textPrimary` | `#F1F5F9` | | 16.3 ✓ |
| `textSecondary` | `#A8B3C2` | | 7.22 ✓ |
| `border` | `#2D3946` | | |
| `outline` | `#6B7A8C` | | 3.0 |
| `success` | `#6CC3A6` | | 7.30 ✓ |
| `warning` | `#D99545` | | 6.08 ✓ |
| `danger` | `#F2918A` | | 6.71 ✓ |

### Content colours
- **Note tints** (7, index 0 = none): Paper, Sand, Sage, Mist, Lilac, Blush, Slate. Each has a
  light and dark value; secondary text on every tint stays ≥ 4.5 : 1.
- **Project colours** (7): Lake `#527DAA`, Pine `#3A8D75`, Amber `#D99545`, Iris `#8E6CC9`,
  Rose `#C2566B`, Teal `#4A9BB5`, Stone `#7B8794`. Used only as dots, progress fills and
  thin accents — never behind text.

Colour is never the only signal: overdue dates also say "Yesterday"/date, priorities also have a
flag icon with a label for screen readers.

---

## 4. Typography

**Typefaces (bundled, OFL):**
- **Inter** (variable) for the Latin UI — the brand board's typeface.
- **Readex Pro** (variable) when the app runs in Arabic — Inter has no Arabic glyphs, and Readex Pro
  is a modern sans whose Latin matches Inter's proportions, so mixed Arabic/English stays one voice.
  Arabic line heights are 8 % taller for diacritics.

| Role | Size / line height | Weight | Used for |
|---|---|---|---|
| `display` (headlineMedium) | 26 / 34 | 600 | Home greeting |
| `headline` (headlineSmall) | 22 / 30 | 600 | Note title in editor |
| `title` (titleLarge) | 20 / 28 | 600 | Top bar titles |
| `titleMedium` | 16 / 24 | 600 | Card and project titles |
| `titleSmall` | 14 / 20 | 600 | Section headers |
| `bodyLarge` | 16 / 26 | 400 | Editor text, task titles |
| `bodyMedium` | 14 / 22 | 400 | Card previews |
| `bodySmall` | 12 / 18 | 400 | Captions, helper text |
| `labelLarge` | 14 / 20 | 500 | Buttons |
| `labelMedium` | 12 / 16 | 500 | Chips, metadata |
| `labelSmall` | 11 / 16 | 500 | Counters |

Rich text in notes maps onto the same scale: `# Heading` → headline (20 sp in body context),
`## Subheading` → titleMedium, **bold** → 600, *italic*, `> quote` → bodyLarge + secondary text +
leading bar.

**Text size setting:** Standard (1.0×) or Large (1.15×), applied on top of the system font scale.

---

## 5. Spacing & layout

4 dp base grid.

| Token | dp | Use |
|---|---|---|
| `xs` | 4 | icon–label gaps |
| `sm` | 8 | between chips, list item padding |
| `md` | 12 | card inner padding (compact), grid gutters |
| `lg` | 16 | screen side margins, card padding |
| `xl` | 24 | between sections |
| `xxl` | 32 | top of empty states |
| `fabClearance` | 96 | bottom padding so last item clears the FAB |

- **Screen margin:** 16 dp. **Max content width:** 720 dp (centred) on tablets/landscape.
- **Touch targets:** ≥ 48 × 48 dp, even when the visual is smaller (checkbox circle 24 dp).
- **Insets:** edge-to-edge; top bars consume status bar, nav bar consumes navigation bar, editors
  apply `imePadding` so the keyboard never covers the formatting toolbar or the caret.

---

## 6. Shape (corner radii)

| Token | dp | Use |
|---|---|---|
| `xs` | 4 | progress bars, small indicators |
| `sm` | 8 | chips, thumbnails, menus |
| `md` | 12 | cards, inputs, buttons, list highlights |
| `lg` | 16 | FAB, dialogs, large media |
| `xl` | 28 | bottom-sheet top corners |
| `full` | 50 % | avatars, colour swatches, calendar day |

Buttons use 12 dp (not pill) to match cards — a deliberate, calmer silhouette than default Material.

---

## 7. Borders & elevation

| Level | Treatment | Use |
|---|---|---|
| 0 — Flat | surface + 1 dp `border` | cards, inputs, note tiles |
| 1 — Tonal | `surfaceContainer` fill, no shadow | navigation bar, selected rows, toolbars |
| 2 — Float | 6 dp shadow | FAB |
| 3 — Overlay | scrim + tonal surface | bottom sheets, dialogs, menus |

Coloured notes drop the border (their tint already separates them).
Dark theme never uses shadows for hierarchy — only lighter surfaces.

---

## 8. Components & interactive states

| Component | Spec |
|---|---|
| **Primary button** | filled `primary`, 48 dp, radius md, labelLarge |
| **Secondary button** | tonal `secondaryContainer` |
| **Text button** | `primary` text, for low-emphasis actions |
| **Destructive** | text in `danger`; only confirmed via dialog |
| **Chip (filter)** | radius sm, 32 dp visual / 48 dp target; selected = `secondaryContainer` + check |
| **Card** | Level 0, radius md, padding lg (md in grids) |
| **List row** | 56–72 dp, whole row tappable, trailing meta |
| **Task check** | 24 dp ring tinted by priority; done = filled `success` + strikethrough title |
| **Top bar** | flat, background colour, title titleLarge; actions ≤ 2 visible + overflow |
| **Navigation bar** | Home · Notes · **+** · Tasks · More; labels always visible; outlined → filled when selected |
| **Quick Capture button** | 52 dp circle, `primary`, raised (6 dp) in the centre of the navigation bar |
| **Segmented tabs** | pill track `surfaceContainer`, selected segment filled `primary` |
| **Status chip** | pill, tinted background + coloured text: Active (info), Late (danger), Done (success) |
| **Task check** | 22 dp rounded square, border tinted by priority; done = filled success with ✓ |
| **Bottom sheet** | radius xl top, drag handle, content padded lg, respects IME |
| **Snackbar** | inverse surface, one action max ("Undo") |
| **Dialog** | radius lg, title + one sentence + 2 actions; destructive action on the end |

**States (all interactive elements):** pressed = 10 % state layer; focused = 10 % + focus
ring (2 dp `secondary`) for keyboard/D-pad; disabled = 38 % content, no state layer;
selected = tonal container + filled icon.

---

## 9. Icons & illustrations

- **Icons:** Material Symbols *Outlined* at 24 dp for actions; the *Filled* variant marks a selected
  or active state (nav tab, pinned note). Never mix stroke styles on one screen.
- **Illustrations:** drawn in code (Canvas) so they follow the theme. One family:
  2.5 dp rounded strokes in `textSecondary`, soft fills in `secondaryContainer`, the warm accent
  used once (the folded corner or the "sun"). 120 dp, centred above the empty-state title.
  Set: *Notes, Tasks, Calendar, Projects, Search, Archive, All-clear, Error*.

---

## 10. Motion

| Token | Duration | Easing | Use |
|---|---|---|---|
| `fast` | 120 ms | standard | checkbox, chip, colour change |
| `standard` | 220 ms | emphasized-decelerate | expand/collapse, content swap |
| `screen` | 280 ms | emphasized-decelerate in / accelerate out | screen transitions (fade + 6 % slide) |

Rules: animate *state*, not decoration; nothing loops; tab switches only cross-fade.
Compose follows the system "Remove animations" setting (animator duration scale) automatically.

---

## 11. Screen specifications

**Navigation:** Home · Notes · (+) Quick Capture · Tasks · More. Calendar, Projects, Search,
Archive and Settings live under More (Calendar and Projects are also one tap from Home).

### 1. Home dashboard
Top bar: brand mark + "Imad-note 2", search. Greeting (`display`): «صباح الخير، عماد» (name from
Settings, optional) and «يوم جديد... فرص جديدة». Search field. **2 × 2 stat cards**: Today's tasks
(done/total, amber), Notes (count, green), Projects (count, blue), Calendar (today's date) — each opens
its screen. Then Overdue (danger header), Today's open tasks, Favourites (pinned notes) and
**Recent activity** (note edited / task added / task completed, with "x min ago").

### 2. Notes list
Title + archive action; search field; chips: All · Favourites · Checklists · Images.
**List** (default): icon tile tinted by the note colour (note / checklist / image icon), title,
one-line preview (rich text, markers hidden), relative date. **Grid** optional in Settings.
Empty: plant illustration, «لا توجد ملاحظات بعد», button «إضافة ملاحظة».

### 3. Rich-text note editor
Top bar: back, centred title («مذكرة جديدة» / edited time), pin, archive, overflow, round filled
**✓ Done**. Title (`headline`), body with live formatting. Bottom toolbar above the keyboard:
Heading · Bold · Italic · Bulleted · Numbered · Quote │ Checklist · Image · Camera · Colour.
Lists continue on Enter; Enter on an empty item ends the list. Autosave 0.6 s after typing.

### 4. Task list
Segmented tabs **Today · Week · All**. Today shows Overdue (danger) then today; Week groups the next
7 days by day; All shows dated, undated and completed groups. Rows: square check tinted by priority,
title, meta (time, reminder, repeat, subtasks, project). Full-width **+ Add task** button at the bottom.
Completing shows a snackbar with **Undo**.

### 5. Calendar
Top bar: month + year, week/month toggle. Week strip (default) or month grid; day cells 44 × 52 dp,
selected = filled primary rounded square, today = tonal, dot = open tasks. Below: «السبت، 26 أبريل» and
agenda cards with a coloured start bar (priority), title and time («طوال اليوم» when untimed), plus
**Add** for that day.

### 6. Projects dashboard
Active / Archived chips. Cards: colour dot, name, status chip, deadline, description,
progress bar + "4/9 tasks". Empty: projects illustration + button.

### 7. Project details
Description, deadline, progress; Tasks (add), Notes (add), Images from the project's notes.
Overflow: Archive, Delete (content is kept and unlinked).

### 8. Search results
Search field in the top bar (autofocus), scope chips with counts, results grouped Tasks → Notes →
Projects; illustrated start and no-results states.

### 9. Quick Capture
Opened by the centre **+** (and launcher shortcuts). Inline field first — **Save note** / **Save task**
without leaving the screen (snackbar with *Open*). Tiles: Checklist · Image note · Idea · Detailed task.

### 10. More & Settings
More: brand card (icon, name, tagline) and rows with tinted icon tiles — Projects, Calendar, Search,
Archive, Settings. Settings: Profile (name), **Language (Device / العربية / English)**, Appearance
(Theme, Text size, Notes layout), Home sections, Reminders status, Data, About.

## 12. Accessibility checklist

- [x] Text contrast ≥ 4.5 : 1, UI components ≥ 3 : 1 (both themes).
- [x] Touch targets ≥ 48 dp.
- [x] Content descriptions for all icon-only buttons; decorative images are silent.
- [x] Task rows announce state ("done" / "not done") and merge into one focus stop.
- [x] Section headers marked as headings for TalkBack navigation.
- [x] Works at 200 % font scale (no fixed-height text containers).
- [x] RTL layout, full Arabic translation and an in-app language switch (Arabic is used when the device language is neither Arabic nor English).
- [x] Motion respects the system animation setting.
