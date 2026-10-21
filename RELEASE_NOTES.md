# Vafic Launcher 1.1.5 — release notes

Ready-to-paste text for the GitHub release page. The English block goes into the
release body, the Russian block can be used for a Russian-speaking audience.

---

## English

**Vafic Launcher 1.1.5** — a quiet, controller-first home screen for Android TV.

Three full rows, a fixed header, a dock of up to five apps, a reorderable
library with hidden apps, a PS5-style focus outline and Windows-Aero glass
surfaces. Plain Java, no AndroidX, no Gradle, no analytics, no network access,
no diagnostic logging.

### What is new in 1.1.5

- **Your own background track.** The full lossless FLAC (14:47) plays exactly
  as it is and loops seamlessly: the track fades out at the end and fades in at
  the beginning, so the loop point cannot be heard. The track is unpacked to
  the cache in the background, the music starts a moment after the launcher
  appears.
- **The sounds stay on top of the music.** The background level was lowered,
  the tick and the forward/back notes were raised, and the music dips for half
  a second while a sound plays.
- **Much lighter on the CPU.** The focus ring is now a small layer that rides
  on the focused card instead of a full screen overlay. It turns exactly as
  before, but the GPU no longer repaints the glass underneath it on every
  frame.
- **Completely idle in the background.** While another app is in front the
  music is paused, the focus ring stops turning and no artwork is decoded. The
  launcher measures 0% CPU in the background, so it never slows down the app
  you are watching.
- **A soft Aero halo.** The ring got a wider, softer glow with room around
  it, so the light is no longer cut off at the edges.
- **The app artwork is prepared in the background**, at the lowest priority,
  and it stops the moment the launcher goes to the background. The library
  opens without a stall and the launcher never takes CPU away from the app you
  are watching.
- **The TV can go to sleep.** The launcher holds the screen awake while you use
  the remote and releases it a minute later, so the television falls asleep on
  its own instead of burning the night.
- **The standard Ethernet icon.** The status bar shows the very symbol the
  Android system bar uses, taken from the platform vector, without the small
  stray mark on its right.
- **The dock cursor stays in the dock.** Scrolling right or left on the dock
  moves card by card and stops at the edges, instead of jumping up to the
  status cluster.
- **A proper cold start.** The launcher hands the focus to the first dock card
  as soon as it appears, so the first key press never sends the cursor to a
  random place.
- **Windows-Aero interface.** Cool blue translucent glass, a milky gloss along
  the top edge, a diagonal reflection, a bright blue rim and a soft shadow
  under every card. The wallpaper is an Aero-blue gradient with light streaks
  and the focus outline got a cool blue tint.
- **Bigger dock cards.** The dock artwork now fills the glass frame, so the
  focus outline hugs the card instead of floating in a wide empty margin.
- **Hidden apps.** Long-press an app and choose **Hide**. The app leaves the
  library and the dock, and the launcher settings keep a **Hidden apps** list
  where every hidden app can be restored, one by one or all at once.
- **No duplicate tile.** The **Launcher** tile was removed from **All apps**;
  the launcher settings now live only in the gear of the top status cluster.
- **Launcher settings menu.** The gear opens the launcher settings with a
  **System settings** entry that opens the Android TV settings.
- **Fixed status icons.** The focus glow of the notification badge and the
  status icons is no longer clipped into a square.
- **Interface tick everywhere.** The cursor ticks in the top status cluster and
  in every menu, including the rows of the notification, network and settings
  panels.
- **Fresh library visits.** HOME forgets the library cursor, so the next
  **All apps** page starts at the first app. BACK still returns to the app that
  was selected.

### Install

```powershell
adb install -r vafic-launcher.apk
adb shell cmd package set-home-activity com.vafic.launcher/.MainActivity
adb shell cmd notification allow_listener com.vafic.launcher/.NotifListener
```

The last command is optional: without notification access the launcher still
works, but the notification badge and shade stay empty. Set Vafic Launcher as
the home app in the TV settings if another launcher is selected.

### Build

- Universal APK, no ABI split.
- Package `com.vafic.launcher`, version `1.1.5` (versionCode `7`).
- Target SDK 34 (Android 14), minimum SDK 23 (Android 6.0).
- Signed with the local debug keystore in `.keystore/` (ignored by git).
- Size: 58237650 bytes.
- SHA-256: `BF37FB8DACB8169C313B7AE3C0FF55D7D1F9C4938BD34915901D97370C0FDC70`

The APK is larger than before because the lossless background track (14:47,
44.1 kHz stereo FLAC) is bundled with it.

### Verify the download

```powershell
Get-FileHash .\vafic-launcher.apk -Algorithm SHA256
```

### Documentation

- [README.md](README.md) — English.
- [README.ru.md](README.ru.md) — Russian.
- [CHANGELOG.md](CHANGELOG.md) — the full history.

### Notes

- The launcher does not bypass paid content, remove ads from other apps or
  include content from third-party services.
- App names and notification text come from the installed apps.
- The project is not affiliated with Google, Sony or any TV vendor.
- MIT licensed.

---

## Русский

**Vafic Launcher 1.1.5** — тихий домашний экран для Android TV с управлением
с пульта.

Три полных ряда, закреплённая шапка, док до пяти приложений, переставляемая
библиотека со скрытыми приложениями, обводка фокуса в духе PS5 и стеклянные
поверхности в стиле Windows Aero. Чистый Java, без AndroidX и Gradle, без
аналитики, без доступа в интернет и без диагностического логирования.

### Что нового в 1.1.5

- **Ваша фоновая музыка.** Полный lossless FLAC (14:47) играется как есть и
  бесшовно зацикливается: трек затихает в конце и нарастает в начале, поэтому
  стык цикла не слышен. Дорожка распаковывается в кэш в фоне, музыка
  начинается через мгновение после появления лаунчера.
- **Звуки слышны поверх музыки.** Фон приглушён, громкость тика и звуков
  вперёд/назад поднята, а во время звука музыка ещё и притихает на полсекунды.
- **Намного легче по CPU.** Кольцо фокуса теперь небольшой собственный слой,
  который едет по карточке, а не полноэкранная накладка. Крутится оно так же,
  но GPU больше не перерисовывает стекло под ним каждый кадр.
- **В фоне лаунчер полностью спит.** Пока впереди другое приложение, музыка
  на паузе, кольцо не крутится и ничего не декодируется. В фоне лаунчер
  занимает 0% CPU и не тормозит то, что вы смотрите.
- **Мягкое свечение в стиле Aero.** У кольца появился более широкий и мягкий
  ореол с запасом по краям, поэтому свет больше не обрезается.
- **Обложки приложений готовятся в фоне**, с самым низким приоритетом, и
  работа останавливается, как только лаунчер уходит в фон. Библиотека
  открывается без рывка, и лаунчер не отбирает CPU у приложения, которое вы
  смотрите.
- **Телевизор может уснуть.** Лаунчер держит экран включённым, пока вы пользуетесь
  пультом, и отпускает его через минуту — телевизор засыпает сам, а не горит
  всю ночь.
- **Стандартный значок Ethernet.** В статусной строке теперь тот самый символ,
  который использует системная панель Android — вектор взят из платформы, без
  лишней засечки справа.
- **Курсор не убегает вверх.** Пролистывание влево/вправо по доку ходит по
  карточкам и останавливается на краях, а не перескакивает на верхнюю панель.
- **Правильный холодный старт.** Сразу после появления лаунчера фокус встаёт на
  первую карточку дока, поэтому первое нажатие пульта не уводит курсор в
  случайное место.
- **Интерфейс в стиле Windows Aero.** Холодное синее полупрозрачное стекло,
  молочный глянец по верхнему краю, диагональный блик, яркий сине-белый кант и
  мягкая тень под каждой карточкой. Обои — синий градиент Aero со световыми
  полосами, обводка фокуса получила холодный синий оттенок.
- **Крупнее иконки дока.** Иконки заполняют стеклянную рамку целиком, поэтому
  обводка фокуса прилегает к карточке, а не отделена широким полем.
- **Скрытые приложения.** Удерживайте OK на приложении и выберите **Скрыть**:
  приложение исчезнет из библиотеки и из дока. В настройках лаунчера появился
  список **Скрытые приложения**, откуда их можно вернуть по одному или все
  сразу.
- **Без дубликатов.** Плитка **Лаунчер** убрана из **Всех приложений**:
  настройки лаунчера открываются только через шестерёнку в статусной строке.
- **Меню настроек лаунчера.** Шестерёнка открывает настройки лаунчера, где
  есть пункт **Системные настройки** с переходом в настройки Android TV.
- **Исправлены значки вверху.** Подсветка счётчика уведомлений и значков
  больше не обрезается в квадрат.
- **Тик везде.** Звук фокуса играет в верхней статусной строке и в любом меню,
  включая списки уведомлений, сети и настроек.
- **Свежий запуск библиотеки.** HOME забывает позицию курсора, поэтому в
  следующий раз **Все приложения** открываются с первого приложения. BACK
  возвращает выбранное приложение.

### Установка

```powershell
adb install -r vafic-launcher.apk
adb shell cmd package set-home-activity com.vafic.launcher/.MainActivity
adb shell cmd notification allow_listener com.vafic.launcher/.NotifListener
```

Последняя команда необязательна: без доступа к уведомлениям лаунчер работает,
но счётчик и панель уведомлений будут пустыми.

### Сборка

- Универсальный APK, без разделения по ABI.
- Пакет `com.vafic.launcher`, версия `1.1.5` (versionCode `7`).
- Target SDK 34 (Android 14), минимальная версия — API 23 (Android 6.0).
- Подписан локальным debug-keystore из `.keystore/` (исключён из git).
- Размер: 58237650 байт.
- SHA-256: `BF37FB8DACB8169C313B7AE3C0FF55D7D1F9C4938BD34915901D97370C0FDC70`

APK стал больше, потому что в него вложена lossless фоновая дорожка
(14:47, FLAC 44.1 кГц, стерео).

### Документация

- [README.ru.md](README.ru.md) — русский.
- [README.md](README.md) — английский.
- [CHANGELOG.md](CHANGELOG.md) — полная история изменений.

### Важно

- Лаунчер не обходит платный доступ, не вырезает рекламу из чужих приложений и
  не включает сторонний контент.
- Названия приложений и текст уведомлений приходят от установленных приложений.
- Проект не связан с Google, Sony или производителями телевизоров.
- Лицензия MIT.
