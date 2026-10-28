# Vafic Launcher 1.4.0 — release notes

Ready-to-paste text for the GitHub release page. The English block goes into the
release body, the Russian block can be used for a Russian-speaking audience.

---

## English

**Vafic Launcher 1.4.0** — a quiet, controller-first home screen for Android TV.

The dock now scrolls, gamepads work, the focus outline no longer breaks at the
dock edge, and the launcher asks for notification access when it is missing.

### What is new in 1.4.0

- **Scrollable dock.** The dock still shows three to eight cards at a time, but
  can now hold up to 24 apps. Moving past the visible window slides the row one
  card at a time, with a soft fade at the plate edges and a short resistance
  nudge at either end. The **Dock** setting changes only the visible window and
  never removes apps.
- **Focus outline fixed.** While the dock moves, the outline follows its card
  and is clipped and faded together with the artwork at the plate edge. It no
  longer leaves a bright vertical line or a detached outline. The initial focus
  animation and the library focus restore also stopped flashing and playing a
  stray tick.
- **Basic gamepad support.** A and the left-stick click work as OK, B works as
  BACK, and the D-pad, left stick and hat move focus with hold-to-repeat,
  including inside the launcher's dialogs.
- **Remote gear and TV keys.** The gear opens Android TV settings; the TV button
  opens Live TV or the channel list when the device provides one.
- **Notification-access prompt.** When access is missing, the launcher asks once
  per start with **Grant access**, **Later** and **Don't remind me**. The
  notification badge and **App notifications** settings open the same prompt or
  the system access screen, and the settings section shows the current state. On
  Android 13+ the prompt also explains how to remove the restricted-settings
  limitation for apps installed from outside a store.
- **Android TV launcher entry.** The app now has its own icon and banner,
  registers the leanback category and marks the touchscreen as optional. Icons
  and banners are read from the active leanback/launcher activity, so packages
  that use activity aliases show the right artwork.
- **New interface cues.** The dock edge and incoming notification banners have
  their own short sounds.

### Documentation

- [README.md](README.md) — English.
- [README.ru.md](README.ru.md) — Russian.
- [CHANGELOG.md](CHANGELOG.md) — the full history.

### Technical

- Package `com.vafic.launcher`, version `1.4.0` (versionCode `11`).
- Target SDK 34 (Android 14), minimum SDK 23 (Android 6.0).
- Plain Java, no AndroidX and no Gradle. Build instructions are in the README.

---

## Русский

**Vafic Launcher 1.4.0** — тихий лаунчер для Android TV с управлением с пульта.

Док теперь прокручивается, появилось управление геймпадом, обводка фокуса больше
не ломается на краю дока, а при отсутствии доступа к уведомлениям лаунчер сам
предлагает его выдать.

### Что нового в 1.4.0

- **Прокручиваемый док.** В доке по-прежнему видно три–восемь карточек
  одновременно, но всего он вмещает до 24 приложений. При выходе за видимое окно
  ряд прокручивается на одну карточку за нажатие: у краёв плашки работает мягкое
  растворение, а в конце — короткий упругий отклик. Настройка **Док** меняет
  только число видимых карточек и ничего не удаляет.
- **Исправлена обводка фокуса.** При движении дока обводка следует за карточкой и
  обрезается вместе с обложкой на краю плашки. Больше нет яркой вертикальной
  линии и оторванной от карточки рамки. Первичная анимация фокуса и
  восстановление фокуса в библиотеке больше не мигают и не проигрывают лишний
  тик.
- **Базовое управление геймпадом.** A и нажатие левого стика работают как OK, B —
  как BACK, а D-pad, левый стик и хат перемещают фокус с автоповтором, в том
  числе в диалогах лаунчера.
- **Кнопки шестерёнки и TV на пульте.** Шестерёнка открывает настройки Android TV,
  кнопка TV — Live TV или список каналов, если устройство это поддерживает.
- **Запрос доступа к уведомлениям.** Если доступа нет, лаунчер спрашивает один раз
  за запуск и предлагает **Разрешить доступ**, **Позже** или **Больше не
  напоминать**. Счётчик уведомлений и раздел **Уведомления приложений** открывают
  тот же запрос или системный экран доступа, а в настройках видно текущее
  состояние. На Android 13+ диалог также подсказывает, как снять ограничение
  настроек для приложений, установленных вне магазина.
- **Запись лаунчера для Android TV.** Появились собственные иконка и баннер,
  добавлена категория leanback, а сенсорный экран отмечен как необязательный.
  Иконки и баннеры читаются из активной leanback/launcher-activity, поэтому
  пакеты с activity-alias показывают правильную графику.
- **Новые звуки интерфейса.** У края дока и у входящих уведомлений есть свои
  короткие сигналы.

### Документация

- [README.md](README.md) — английский.
- [README.ru.md](README.ru.md) — русский.
- [CHANGELOG.md](CHANGELOG.md) — полная история.

### Технические данные

- Пакет `com.vafic.launcher`, версия `1.4.0` (versionCode `11`).
- Target SDK 34 (Android 14), минимальная версия — SDK 23 (Android 6.0).
- Чистый Java, без AndroidX и Gradle. Инструкции по сборке — в README.
