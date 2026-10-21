# Changelog

## 1.1.6 — 2026-10-22

- Fixed the long lists in the panels. The notification shade, the per-app
  notification filter and the hidden apps list are cut off cleanly at the edge
  of their window now: the rows no longer slide over the title or over the
  **Clear notifications** button, and every notification can be reached with the
  D-pad.
- The list keeps its full height inside the window, so the scroll reaches the
  last entry instead of stopping after a few rows.
- The focus ring is now a small layer that rides on the focused card, so the
  glass underneath it is no longer repainted on every frame of the rotation.
- The screen is released a minute after the last key press, so the television
  can fall asleep on its own.

## 1.1.5 — 2026-10-21

- New background track: the full lossless FLAC (14:47) now plays as it is and
  loops seamlessly — the track fades out at the end and fades in at the
  beginning, so the loop point is inaudible. The track is unpacked to the cache
  in the background, the music starts a moment after the launcher appears.
- The focus ring is now a small layer that rides on the focused card instead
  of a full screen overlay. It turns exactly as before, but the GPU no longer
  repaints the glass underneath it on every frame.
- The ring got a soft Aero halo and a little more room, so the glow is no
  longer cut off at the edges.
- The artwork of the installed apps is decoded in the background at the lowest
  priority and stops as soon as the launcher goes to the background, so the
  library opens without a stall and never takes the CPU away from the app you
  are watching.
- While another app is in front the launcher is completely idle: the music is
  paused, the focus ring stops turning and nothing is decoded. It measures 0%
  CPU in the background.
- The screen is no longer held on forever. The launcher keeps the display awake
  while the remote is in use and releases it a minute later, so the television
  can fall asleep on its own.
- The Ethernet status icon is now the standard Android symbol, the very vector
  the system status bar uses, without the small stray mark on its right.
- The glass surfaces no longer use clip paths, which makes every card a little
  cheaper to draw.
- The background music switched from a hand-made PCM loop to a real track
  player, so the music is no longer limited to 22 kHz mono.
- The music now sits well below the interface sounds: the background level was
  lowered, the tick and the forward/back notes were raised, and the music dips
  for half a second while a sound plays.
- The dock cursor no longer jumps up to the status cluster when it reaches the
  last card: left and right now move inside the dock and stop at its edges.
- A cold start now hands the focus to the first dock card. Before, the first
  key press after the launcher appeared could send the cursor anywhere.
- Restyled the whole interface in Windows-Aero glass: a cool blue translucent
  body, a milky gloss along the top edge, a diagonal reflection, a bright blue
  rim and a soft shadow under every card. The wallpaper became an Aero-blue
  gradient with light streaks, and the focus outline got a cool blue tint.
- The dock cards are larger and their artwork fills the glass frame, so the
  focus outline hugs the card instead of floating in a wide empty margin.
- Removed the duplicate **Launcher** tile from **All apps**. The launcher
  settings now live only in the gear of the top status cluster.
- Added app hiding: long-press an app and choose **Hide**. The app leaves the
  library and the dock, and the launcher settings show it under
  **Hidden apps**, where it can be restored one by one or all at once.
- The status icons no longer clip their focus glow into a square: the circle is
  drawn inside the icon frame at any scale.
- The interface tick now plays in the top status cluster and in every menu,
  including the rows of the notification, network and settings panels.
- Leaving the launcher with HOME forgets the library cursor: the next visit to
  **All apps** starts at the first app again, while BACK still returns to the
  app that was selected.
- The status gear opens the **launcher settings** menu with a **System
  settings** entry for Android TV settings.
- Release build remains free of diagnostic logging.

## 1.1.4 — 2026-10-21

- Removed the duplicate **Launcher** tile from **All apps**. The launcher
  settings now live only in the gear of the top status cluster.
- Added app hiding: long-press an app and choose **Hide**. The app leaves the
  library and the dock, and the launcher settings show it under
  **Hidden apps**, where it can be restored one by one or all at once.
- Restyled the whole interface in Windows-Aero glass: a cool blue translucent
  body, a milky gloss along the top edge, a diagonal reflection, a bright blue
  rim and a soft shadow under every card. The wallpaper became an Aero-blue
  gradient with light streaks, and the focus outline got a cool blue tint.
- The dock cards are larger and their artwork fills the glass frame, so the
  focus outline hugs the card instead of floating in a wide empty margin.
- The dock cursor no longer jumps up to the status cluster when it reaches the
  last card: left and right now move inside the dock and stop at its edges.
- A cold start now hands the focus to the first dock card. Before, the first
  key press after the launcher appeared could send the cursor anywhere.
- The status icons no longer clip their focus glow into a square: the circle is
  drawn inside the icon frame at any scale.
- The interface tick now plays in the top status cluster and in every menu,
  including the rows of the notification, network and settings panels.
- Leaving the launcher with HOME forgets the library cursor: the next visit to
  **All apps** starts at the first app again, while BACK still returns to the
  app that was selected.
- The status gear opens the **launcher settings** menu with a **System
  settings** entry for Android TV settings.
- Release build remains free of diagnostic logging.

## 1.1.3 — 2026-10-21

- Fixed the notification badge focus circle: the status cluster no longer clips
  the enlarged badge, so the glow is a full round circle.
- The launcher interface sounds now play on the top status cluster as well:
  moving the cursor over the notification badge, the network icon and the gear
  ticks like the dock tiles, and menu rows tick inside the panels.
- The status gear now opens the **launcher settings** menu instead of Android TV
  settings. The menu gained a **System settings** entry that opens the Android TV
  settings.
- Leaving the launcher with HOME now forgets the library cursor: the next visit
  to **All apps** starts at the first app again, while BACK still returns to the
  app that was selected.
- Release build remains free of diagnostic logging.

## 1.1.2 — 2026-10-21

- Localized the system app name: **Vafic Launcher** in English and
  **Vafic Лаунчер** in Russian.
- Pressing Up from the dock now focuses the status cluster even when the
  notification badge is hidden.
- Added a **system settings** gear to the status cluster; it opens Android TV
  settings. Down returns to the dock.
- Added library reordering: long-press an app, choose **Move**, place it with the
  D-pad, then press OK or BACK. The order is persisted.
- Fixed library focus restoration so the bottom **Launcher** tile is remembered;
  reopening the library no longer teleports to AirReceiver.
- Removed **Launcher settings** from dock and app long-press menus; settings
  remain available from the Launcher tile at the end of the library.
- `build.ps1` now compiles `res/` to a temporary resource zip and links it with
  `aapt2`, so the localized app label is included.
- Release build remains free of diagnostic logging.

## 1.1.1 — 2026-10-21

- First run starts with a single **Add apps (+)** card. It opens the library and
  explains the long-press → **Add to dock** flow.
- Removed automatic first-five dock seeding and the **Reset dock** action.
- Replaced the language cycling row with an anchored dropdown.
- The focus outline now rides the selected card during library page glides
  instead of staying fixed at one screen position.
- Softened the forward/back interface sounds and lowered their playback volume.
- Replaced launcher-flow toasts with an in-app glass notice; the remaining
  notification-clear toast now uses the platform app icon.
- Removed all diagnostic logging from the release build.

## 1.1.0 — 2026-10-21

- Added Russian / English / system language selection; all launcher-owned UI
  strings are localized.
- Added distinct forward and back interface sounds, plus an independent
  interface-sound switch.
- Made the first-run dock universal: it is derived from launchable apps on the
  device, and Reset dock rebuilds it.
- Added a two-pixel spring snap, eliminating the stalled page glide and the
  held-Down cursor bounce.
- Kept the focus ring frozen at its screen position during library page glides.
- Kept an intentionally empty dock usable: Down still opens the library.
- Removed all diagnostic logging from the release build.
- Reworked `build.ps1` to be portable (no hard-coded project path; SDK/JDK from
  the environment; local debug keystore; temp asset staging for Cyrillic paths).
- Added README, Russian README, MIT license, changelog and `.gitignore`.

## 1.0.0

- Initial Vafic Launcher: home clock, five-app dock, three-row library, PS5 focus
  ring, notification shade and filter, network panel, ambient music, nav ticks.
