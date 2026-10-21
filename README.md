# Vafic Launcher

A quiet, controller-first home screen for Android TV. Three full rows, a fixed
header, a dock of up to five apps, a reorderable library with hidden apps, a
PS5-style focus outline and Windows-Aero glass surfaces — built without AndroidX
or Gradle. A small aapt2 resource step localizes the app name to English and
Russian.

[Русская версия](README.ru.md)

## Screenshots

| Home | All apps |
|---|---|
| ![Home screen](screenshots/01-home.png) | ![All apps](screenshots/02-all-apps.png) |

| Launcher settings | Hidden apps |
|---|---|
| ![Launcher settings](screenshots/07-settings.png) | ![Hidden apps](screenshots/09-hidden-apps.png) |

| Notifications | App menu |
|---|---|
| ![Notifications](screenshots/05-notifications.png) | ![App menu](screenshots/04-app-menu.png) |

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

## Install

```powershell
adb install -r vafic-launcher.apk
adb shell cmd package set-home-activity com.vafic.launcher/.MainActivity
adb shell cmd notification allow_listener com.vafic.launcher/.NotifListener
```

The last command is optional: without notification access the launcher still
works, but the notification badge and shade stay empty. You can also grant access
in **Settings → Apps → Special app access → Notification access**.

Set Vafic Launcher as the home app in the TV settings if another launcher is
currently selected. The APK targets Android 14 (API 34) and supports API 23+.

## Settings

Open the gear in the top status cluster (press Up from the dock and walk to the
right). Launcher settings are not duplicated in the long-press menus.

- **Background music** — the built-in ambient loop.
- **Interface sounds** — focus ticks plus the forward/back notes.
- **Language** — System / Русский / English in an anchored dropdown.
- **App notifications** — per-app notification filter.
- **Hidden apps** — the apps that were hidden from the library; open one to
  bring it back, or restore all of them.
- **System settings** — open the Android TV settings.
- **Close** — leave settings.

The background track is a full lossless FLAC in `assets/ambient.flac`. It is
played as it is and loops seamlessly: the track fades out at the end and fades
in at the beginning, so the loop point is smooth and the sound is untouched.
The interface sounds are original synthesized WAV files in `assets/`:
`tick.wav`, `forward.wav` and `back.wav`.

## Build from source

Requirements:

- JDK 17+
- Android SDK platform 34
- Android SDK build-tools 34+
- Windows PowerShell (the build script is PowerShell; the source is portable)

Set the SDK and JDK locations, then run:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_SDK_ROOT = "C:\path\to\Android\Sdk"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

The script uses `aapt2`, `javac`, `d8`, `zipalign` and `apksigner` directly.
It writes `vafic-launcher.apk` next to the script.

Signing:

- By default a local debug keystore is created at `.keystore/vafic.keystore`
  (ignored by git). This is fine for sideloading and personal builds. Keep this
  file if you want future builds to install as an update over the current APK.
- For a release, provide your own key:

```powershell
$env:VAFIC_KEYSTORE = "C:\keys\release.keystore"
$env:VAFIC_KEYSTORE_PASS = "your-password"
```

Assets and resources are staged in the system temp directory because `aapt2` on
Windows cannot open an asset or resource directory whose path contains Cyrillic
characters. The script compiles `res/` into a temporary resource zip before
linking.

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | List every installed app in the library and notification filter |
| `ACCESS_NETWORK_STATE` | Show Wi-Fi/Ethernet/offline state |
| `ACCESS_WIFI_STATE` | Read Wi-Fi signal level and SSID |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Optional notification shade and badge |

The app has no internet permission and does not modify other packages. Any
system debloating is a separate device-level operation; it is not performed by
the launcher. Uninstalling the launcher and selecting another home app is
enough to revert the launcher itself.

## Project layout

```
vafic-launcher/
  AndroidManifest.xml
  build.ps1
  assets/                 background track and interface sounds
  res/values/             English app name
  res/values-ru/          Russian app name
  src/com/vafic/launcher/ plain-Java sources
  screenshots/             pictures for the README and the release
  README.md
  README.ru.md
  RELEASE_NOTES.md
  LICENSE
  CHANGELOG.md
```

## Notes

- The launcher does not bypass paid content, remove ads from other apps, or
  include content from third-party services.
- App names and notification text come from the installed apps and are shown
  as provided by those apps.
- The project is not affiliated with Google, Sony or any TV vendor.

## License

MIT. See [LICENSE](LICENSE).
