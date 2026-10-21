# vafic_launcher
# Vafic Launcher

A quiet, controller-first home screen for Android TV. Three full rows, a fixed
header, a dock of up to five apps, a reorderable library with hidden apps, a
PS5-style focus outline and Windows-Aero glass surfaces — built without AndroidX
or Gradle. A small aapt2 resource step localizes the app name to English and
Russian.

[Русская версия](README.ru.md)

## Features

- **Home.** Clock and localized date, a dock of up to five apps, and a fixed
  status cluster with notifications, network and **launcher settings**. Press Up
  from the dock to enter the status cluster; Down returns to the dock. The gear
  opens the launcher settings menu, which has a **System settings** entry for
  Android TV settings.
- **Hidden apps.** Long-press an app and choose **Hide**: it leaves the library
  and the dock. The launcher settings keep a **Hidden apps** list where every
  hidden app can be restored, or all of them at once.
- **Focus-centred library.** Five columns, three full rows, labels always
  visible. Moving past the middle row glides the page by exactly one row.
  During the glide the focus outline rides the selected card, then continues to
  follow it normally after the page settles.
- **Reorderable library.** Long-press OK on an app and choose **Move**, then use
  the D-pad to place it; OK or BACK finishes. The order is remembered and new
  apps stay alphabetical at the end.
- **PS5-style focus.** A thin border hugs the card with a soft pearl-white
  gradient travelling around it. The outline is drawn above the glass layers.
- **Dock editing.** Long-press OK on a dock card to move, remove or hide it.
  Long-press OK in the library to add/remove an app from the dock, move it in
  the library or hide it. The dock holds up to five apps.
- **First run.** The dock starts with a single **+** card. Press it (or Down) to
  open the library, then long-press an app and choose **Add to dock**. If the
  last app is removed, the **+** card returns.
- **Fresh library visits.** HOME forgets the library cursor, so the next
  **All apps** page starts at the first app. BACK keeps the selected app and
  returns to it.
- **Notifications.** Active notifications, a badge, clearable-only cleanup, and
  an optional per-app filter. System/no-clear notifications are kept.
- **Aero-glass surfaces.** Cards, the dock and every panel are built from the
  same Windows-Aero material: cool blue translucent glass, a milky gloss along
  the top edge, a diagonal reflection, a bright blue rim and a soft shadow that
  lifts the card off the wallpaper.
- **Two interface sounds.** A rising glass note opens menus, the library and
  apps; a falling note goes back and closes panels. Focus ticks are played on
  every selectable element: the dock tiles, the top status cluster and the rows
  of the menus. Sounds can be switched off independently of the background
  music.
- **Language selection.** System, Russian or English, chosen from an anchored
  dropdown. All launcher-owned strings switch immediately; changing the language
  recreates the activity cleanly. The system app name is localized too:
  **Vafic Launcher** / **Vafic Лаунчер**.
- **Small and dependency-free.** Plain Java, no AndroidX, no analytics, no
  network access, no diagnostic logging.

## Controls

| Button | Action |
|---|---|
| ← → ↑ ↓ | Move focus; in the library, move through the grid |
| ↑ from dock | Focus the status cluster (notifications / network / launcher settings) |
| OK | Open an app / panel / menu item; finish a move |
| Long OK | App menu: add/remove dock, move in the library, hide the app, or move a dock card |
| BACK | Close a panel/menu, leave the library, or cancel a move |
| HOME | Return to the home screen and focus the first dock card |


