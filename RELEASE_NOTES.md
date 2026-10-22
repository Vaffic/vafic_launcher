# Vafic Launcher 1.2.0 — release notes

Ready-to-paste text for the GitHub release page. The English block goes into the
release body, the Russian block can be used for a Russian-speaking audience.

---

## English

**Vafic Launcher 1.2.0** — a quiet, controller-first home screen for Android TV.

Three full rows, a fixed header, a resizable dock of three to eight apps,
optional weather and a short forecast next to the clock, a reorderable library
with hidden apps, a PS5-style focus outline and Windows-Aero glass surfaces.
Plain Java, no AndroidX, no Gradle, no analytics, no diagnostic logging. The
network is used only by the optional weather section.

### What is new in 1.2.0

- **Weather next to the clock.** The current temperature, condition and a short
  forecast can now sit under the date. The new **Weather** section in the
  launcher settings can hide the whole widget, hide only the forecast, detect
  the city by network, pick a preset or type a city, and refresh on demand.
  Weather is off by default and starts no requests while it is switched off.
  Open-Meteo is the primary source; wttr.in is used when Open-Meteo is not
  reachable from the television's network.
- **Dock size instead of a fixed app count.** Six levels scale the cards from
  the smallest size (up to eight apps) to the largest (three apps). The chosen
  size directly sets how many apps fit. The slider stops at the largest size
  that still holds every current dock app; trying to grow further keeps the
  apps and shows a short reminder to remove an app from the dock first. The
  settings summary shows the selected size and capacity.
- **Separate sound controls.** Background music and interface sounds now have
  independent switches and volume sliders. The music keeps its level while a
  tick or a note plays instead of dipping.
- **No more TV audio cut-outs.** The launcher no longer changes the
  television's system volume stream. On this TV `STREAM_SYSTEM` is aliased to
  `STREAM_MUSIC`, so the old system-stream mute also muted everything else and
  made the audio drop out when entering or leaving apps. The launcher now
  suppresses the framework's focus and click effects inside its own windows
  instead: D-pad focus is moved by the app, so ViewRootImpl never plays the
  system navigation tick, and system click effects are switched off. Only the
  launcher's own tick and notes are heard.
- **Wallpaper dim everywhere.** The **Background dim** slider now darkens the
  built-in Aero wallpaper as well as a user photo.
- **Language focus fixed.** Opening the language picker keeps the focus and the
  highlight on the **Language** row behind the popup instead of jumping to the
  first setting.
- **Sturdier sound preferences.** Volume values saved as whole percentages by
  early 1.2 test builds are read correctly instead of crashing the sound
  settings.
- **Release build remains free of diagnostic logging.**

### Install

```powershell
adb install -r vafic-launcher_1.2.0.apk
adb shell cmd package set-home-activity com.vafic.launcher/.MainActivity
adb shell cmd notification allow_listener com.vafic.launcher/.NotifListener
```

The last command is optional: without notification access the launcher still
works, but the notification badge and shade stay empty. Set Vafic Launcher as
the home app in the TV settings if another launcher is selected.

### Build

- Universal APK, no ABI split.
- Package `com.vafic.launcher`, version `1.2.0` (versionCode `9`).
- Target SDK 34 (Android 14), minimum SDK 23 (Android 6.0).
- Signed with the local debug keystore in `.keystore/` (ignored by git).
- Size: 58294994 bytes.
- SHA-256: `1353CC61F77DD09967930C05ECA4964F4E4D13E9CA15A1C954F6E2E6B2F24851`

The APK is larger than the early builds because the lossless background track
(14:47, 44.1 kHz stereo FLAC) is bundled with it.

### Verify the download

```powershell
Get-FileHash .\vafic-launcher_1.2.0.apk -Algorithm SHA256
```

### Documentation

- [README.md](README.md) — English.
- [README.ru.md](README.ru.md) — Russian.
- [CHANGELOG.md](CHANGELOG.md) — the full history.

Fresh English and Russian screenshots of every screen are attached to this
release.

### Notes

- The launcher does not bypass paid content, remove ads from other apps or
  include content from third-party services.
- App names and notification text come from the installed apps.
- Weather data comes from Open-Meteo and wttr.in.
- The project is not affiliated with Google, Sony or any TV vendor.
- MIT licensed.

---

## Русский

**Vafic Launcher 1.2.0** — тихий домашний экран для Android TV с управлением
с пульта.

Три полных ряда, закреплённая шапка, док на три–восемь приложений с регулируемым
размером карточек, необязательная погода и короткий прогноз рядом с часами,
переставляемая библиотека со скрытыми приложениями, обводка фокуса в духе PS5 и
стеклянные поверхности в стиле Windows Aero. Чистый Java, без AndroidX и Gradle,
без аналитики и диагностического логирования. Сеть используется только
необязательной погодой.

### Что нового в 1.2.0

- **Погода рядом с часами.** Под датой теперь можно показать текущую
  температуру, состояние и короткий прогноз. Новый раздел **Погода** в
  настройках лаунчера умеет скрыть весь виджет, оставить только текущую погоду,
  определить город по сети, выбрать готовый город или ввести свой, а также
  обновить данные по запросу. По умолчанию погода выключена и не делает
  запросов, пока её не включат. Основной источник — Open-Meteo; если он
  недоступен из сети телевизора, используется wttr.in.
- **Размер дока вместо фиксированного числа приложений.** Шесть уровней
  масштабируют карточки от минимального размера (до восьми приложений) до
  максимального (три приложения). Выбранный размер напрямую задаёт ёмкость
  дока. Ползунок останавливается на самом крупном размере, при котором ещё
  помещаются все текущие приложения; попытка увеличить дальше сохраняет
  приложения и показывает короткую подсказку сначала убрать приложение из дока.
  В настройках видно выбранный размер и ёмкость.
- **Раздельные настройки звука.** У фоновой музыки и звуков интерфейса теперь
  свои выключатели и ползунки громкости. Музыка больше не притихает во время
  тика или ноты.
- **Телевизор больше не теряет звук.** Лаунчер больше не меняет системный поток
  громкости телевизора. На этом ТВ `STREAM_SYSTEM` привязан к `STREAM_MUSIC`,
  поэтому старое выключение системного потока глушило весь звук и вызывало
  провалы при входе в приложения и выходе из них. Теперь системные звуки фокуса
  и клика подавляются только внутри окон лаунчера: стрелки двигают фокус силами
  приложения, поэтому ViewRootImpl не проигрывает свой тик, а системные клики
  отключены. Слышны только собственный тик и ноты лаунчера.
- **Затемнение фона везде.** Ползунок **Затемнение фона** теперь затемняет и
  встроенные обои Aero, и выбранную фотографию.
- **Исправлен фокус в выборе языка.** При открытии списка языков фокус и
  подсветка остаются на строке **Язык** за попапом, а не прыгают на первый
  пункт настроек.
- **Надёжные настройки звука.** Значения громкости, сохранённые ранними
  тестовыми сборками 1.2 в целых процентах, читаются правильно и больше не
  роняют раздел звука.
- **Релизная сборка по-прежнему без диагностического логирования.**

### Установка

```powershell
adb install -r vafic-launcher_1.2.0.apk
adb shell cmd package set-home-activity com.vafic.launcher/.MainActivity
adb shell cmd notification allow_listener com.vafic.launcher/.NotifListener
```

Последняя команда необязательна: без доступа к уведомлениям лаунчер работает,
но счётчик и панель уведомлений будут пустыми. Если выбран другой лаунчер,
назначьте Vafic Лаунчер домашним приложением в настройках телевизора.

### Сборка

- Универсальный APK, без разделения по ABI.
- Пакет `com.vafic.launcher`, версия `1.2.0` (versionCode `9`).
- Target SDK 34 (Android 14), минимальная версия — API 23 (Android 6.0).
- Подписан локальным debug-keystore из `.keystore/` (исключён из git).
- Размер: 58294994 байт.
- SHA-256: `1353CC61F77DD09967930C05ECA4964F4E4D13E9CA15A1C954F6E2E6B2F24851`

APK стал больше ранних сборок, потому что в него вложена lossless фоновая
дорожка (14:47, FLAC 44.1 кГц, стерео).

### Проверка загрузки

```powershell
Get-FileHash .\vafic-launcher_1.2.0.apk -Algorithm SHA256
```

### Документация

- [README.ru.md](README.ru.md) — русский.
- [README.md](README.md) — английский.
- [CHANGELOG.md](CHANGELOG.md) — полная история изменений.

К релизу приложены свежие английские и русские скриншоты всех экранов.

### Важно

- Лаунчер не обходит платный доступ, не вырезает рекламу из чужих приложений и
  не включает сторонний контент.
- Названия приложений и текст уведомлений приходят от установленных приложений.
- Данные погоды приходят от Open-Meteo и wttr.in.
- Проект не связан с Google, Sony или производителями телевизоров.
- Лицензия MIT.
