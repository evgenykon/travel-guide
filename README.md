# Solo Traveler’s Guide

An Android audio guide: a MapLibre map, points with descriptions and automatic
voice narration (Yandex SpeechKit) when you enter a point’s 10-metre zone.
Descriptions can be generated with OpenRouter. Routes, history and settings are
stored locally on the device.

**English** · [Русский](#russian)

[Website](https://evgenykon.github.io/travel-guide/) ·
[Route editor](https://evgenykon.github.io/travel-guide/editor.html) ·
[Download APK](https://github.com/evgenykon/travel-guide/releases) ·
[Report an issue](https://github.com/evgenykon/travel-guide/issues)

- **Website:** <https://evgenykon.github.io/travel-guide/>
- **Browser route editor:** <https://evgenykon.github.io/travel-guide/editor.html>
- **Releases and APK:** <https://github.com/evgenykon/travel-guide/releases>
- **Latest dev APK:** <https://github.com/evgenykon/travel-guide/releases/download/dev-latest/travel-guide-dev-latest.apk>

<a id="english"></a>
## English

### What it does

- **Map:** MapLibre with OpenFreeMap vector tiles. Tap a point for a card with
  its name, OSM object, coordinates, elevation (Open-Meteo) and Play/Pause/Stop
  controls. Long-press the map to create a point, or use the “Point” button to
  add one at your current location. The route filter shows only the selected
  route’s points; tracking also follows that route only.
- **Trigger radius:** active points show a translucent circle with their trigger
  radius. The selected point is highlighted and the camera zooms in. The radius
  is adjustable from 5 to 100 metres.
- **Descriptions:** write them by hand or generate them through OpenRouter.
  The app automatically resolves the OSM object (name and category), address
  (street and number), city and country, then inserts them into the prompt so
  the model does not confuse places with the same name. Coordinates are never
  spoken. The prompt template is editable in Settings.
- **Voice:** Yandex SpeechKit voices and speed are selectable, audio is cached
  and the cache can be cleared. Playback can be paused, resumed or stopped from
  the map.
- **10-metre geofences:** a foreground service with a persistent notification
  tracks your position and narrates the description when you enter a zone,
  including with the screen off. The notification shows the active route (or
  “all points”). Each point is narrated once; delete its History entry to hear
  it again. Before tracking starts, a checklist verifies permissions,
  background location, notifications and unrestricted battery, plus autostart
  and battery settings on Xiaomi/MIUI (with deep links to MIUI settings).
- **Routes:** create, rename, delete, add and remove points.
- **History:** zone entries and playback events, with re-play.
- **Import/export:** routes and points with descriptions are saved to a JSON
  file and can be restored on another device or after reinstalling. Settings
  (prompt, OpenRouter model, voice, speed, radius and map style) are exported
  separately.
- **Local storage:** Room (points, routes, history), DataStore (settings) and
  EncryptedSharedPreferences (keys and tokens). After reinstalling, points,
  routes and history are restored from Android auto-backup; keys are
  intentionally excluded and must be connected again.

### Languages

The interface is available in Russian and English. It follows the system
language; on Android 13+ you can choose a language just for the app
(Settings → Apps → Solo Traveler’s Guide → Language, via `android:localeConfig`).
Map labels, Nominatim addresses, OSM object categories, the AI system prompt and
the default prompt template are localized too. SpeechKit voices are filtered by
the app language (English uses `en-US` and English voices). Strings live in
`res/values/strings.xml` (Russian) and `res/values-en/strings.xml` (English).

### Install

1. Open [Releases](https://github.com/evgenykon/travel-guide/releases) and
   download the latest `travel-guide-vX.Y.Z.apk`. For the newest commit, use the
   [dev build](https://github.com/evgenykon/travel-guide/releases/download/dev-latest/travel-guide-dev-latest.apk).
2. Allow installation from your browser or file manager when Android asks.
3. Open the app and connect the services below. Android 8.0 or newer is
   required.

The app is distributed as an APK through GitHub Releases, not through Google
Play. The app itself is free; Yandex Cloud and OpenRouter bill their services
separately according to your usage.

### Connect Yandex SpeechKit (required for voice)

The app uses an **authorised key of a service account** (`key.json`):

1. Sign in to the [Yandex Cloud console](https://console.yandex.cloud/).
2. Open **Service accounts** and create a service account.
3. Assign the `ai.speechkit-tts.user` role to it.
4. Create an **authorised key** for the service account and download
   `key.json`.
5. In the app open **Settings → Yandex SpeechKit → Connect key.json** and pick
   the file. You can also share `key.json` into the app from a file manager.

The app exchanges the key for an IAM token (valid for 12 hours) and refreshes it
automatically. You do not need to paste the token anywhere.

> Yandex ID OAuth tokens are no longer accepted by Yandex Cloud from 1 June
> 2026, so the app uses a service account authorised key — the supported method.

Useful links:
[SpeechKit documentation](https://yandex.cloud/en/docs/speechkit/) ·
[Authorised keys](https://yandex.cloud/en/docs/iam/concepts/authorization/key)

### Connect OpenRouter (required for generated descriptions)

1. In the app open **Settings → OpenRouter → Sign in with OpenRouter**.
2. A browser window opens on the OpenRouter website. Sign in and approve access.
3. The app receives the key automatically through a local callback, stores it
   encrypted and uses it for description generation.

You can review or revoke the key at <https://openrouter.ai/keys>; models and
pricing are listed at <https://openrouter.ai/models>. Usage is billed by
OpenRouter to your account.

### Browser route editor

The [route editor](https://evgenykon.github.io/travel-guide/editor.html) runs
entirely in the browser, without a backend:

- MapLibre GL JS + OpenFreeMap map, the same style as the app;
- add points by clicking the map or dragging them, create routes, set trigger
  radius, resolve addresses (Nominatim) and OSM objects;
- generate descriptions through OpenRouter. The API key, model and prompt are
  set in the API drawer; the key stays in page memory and can optionally be
  saved in the tab’s `sessionStorage` (survives reloads, disappears when the tab
  closes);
- import and export JSON archives in the app’s `BackupFile` format (`version:`
  `1`, routes + points). Import merges by point name and coordinates like the
  app does.

Only an OpenRouter key is needed there; SpeechKit works in the Android app.

### Build from source

Requirements: JDK 17 and Android SDK (compileSdk 36).

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

### CI/CD (GitHub Actions)

- **Push to `main`** → unit tests, lint, debug APK artifact and an updated
  `dev-latest` pre-release with the debug APK; changes under `site/` deploy the
  website.
- **Tag `vX.Y.Z`** → signed release APK and GitHub Release.
- The site is deployed from `site/` through the `Deploy promo site` workflow.
  Enable it once in **Settings → Pages → Build and deployment → Source: GitHub
  Actions**. Site URL: <https://evgenykon.github.io/travel-guide/>.

#### Signed releases

Generate a keystore:

```bash
keytool -genkeypair -v -keystore release.jks -alias travel-guide \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -i release.jks -o release.jks.base64   # macOS
# base64 -w0 release.jks > release.jks.base64 # Linux
```

Add these secrets in GitHub → Settings → Secrets and variables → Actions:

| Secret               | Value                            |
|----------------------|----------------------------------|
| `KEYSTORE_BASE64`    | contents of `release.jks.base64` |
| `KEYSTORE_PASSWORD`  | keystore password                |
| `KEY_ALIAS`          | `travel-guide`                   |
| `KEY_PASSWORD`       | key password                     |

Without these secrets the release APK is signed with the debug key: it
installs, but it will not update an already installed signed build.

Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Preview the website locally

```bash
python3 -m http.server 8099 --directory site
```

### Tech stack

Kotlin · Jetpack Compose · MapLibre Native · MapLibre GL JS · Room · DataStore ·
Retrofit/OkHttp · kotlinx.serialization · Play Services Location ·
Yandex SpeechKit v1 TTS + v3 voices · OpenRouter API · OpenFreeMap ·
Nominatim · Open-Meteo Elevation · GitHub Actions

---

<a id="russian"></a>
## Русский

Android-приложение-аудиогид: карта MapLibre, точки с описаниями и автоматическая
озвучка (Yandex SpeechKit) при входе в 10-метровую зону точки. Описания можно
генерировать через OpenRouter. Маршруты, история и настройки хранятся локально
на устройстве.

[English](#english) · **Русский**

[Сайт](https://evgenykon.github.io/travel-guide/) ·
[Конструктор маршрутов](https://evgenykon.github.io/travel-guide/editor.html) ·
[Скачать APK](https://github.com/evgenykon/travel-guide/releases) ·
[Сообщить о проблеме](https://github.com/evgenykon/travel-guide/issues)

- **Сайт:** <https://evgenykon.github.io/travel-guide/>
- **Конструктор маршрутов в браузере:** <https://evgenykon.github.io/travel-guide/editor.html>
- **Релизы и APK:** <https://github.com/evgenykon/travel-guide/releases>
- **Свежая dev-сборка:** <https://github.com/evgenykon/travel-guide/releases/download/dev-latest/travel-guide-dev-latest.apk>

### Возможности

- **Карта:** MapLibre с векторными тайлами OpenFreeMap. Тап по точке —
  мини-карточка: название, OSM-объект, координаты, высота (Open-Meteo) и кнопки
  Play/Pause/Stop. Долгое нажатие на карте создаёт точку, кнопка «Точка» —
  добавляет рядом с текущим местоположением. Фильтр по маршруту показывает
  только точки выбранного маршрута; слежение тоже работает только по ним.
- **Радиус срабатывания:** вокруг активных точек отображается прозрачный круг
  радиуса срабатывания, выбранная точка подсвечивается, камера приближается.
  Радиус настраивается от 5 до 100 метров.
- **Описания:** вручную или через OpenRouter. Под точкой автоматически
  определяются объект OSM (имя и категория), адрес (улица и дом), город и
  страна — всё подставляется в промпт, чтобы модель не путала одноимённые
  места. Координаты в озвучиваемый текст не попадают. Шаблон промпта
  редактируется в настройках.
- **Озвучка:** голос и скорость Yandex SpeechKit выбираются, аудио кэшируется,
  кэш можно очистить. С карты можно поставить озвучку на паузу, продолжить или
  остановить.
- **Геозоны 10 м:** foreground-сервис с постоянным уведомлением отслеживает
  позицию и озвучивает описание при входе в зону, в том числе при выключенном
  экране. В уведомлении показан активный маршрут (или «все точки»). Каждая
  точка озвучивается один раз; чтобы услышать снова, удалите запись в
  «Истории». Перед включением слежения приложение проверяет разрешения,
  фоновую геолокацию, уведомления и ограничения батареи, а на Xiaomi/MIUI —
  автозапуск и экономию батареи (с deep-link в настройки MIUI).
- **Маршруты:** создание, переименование, удаление, добавление и удаление
  точек.
- **История:** входы в зоны и прослушивания с возможностью воспроизвести
  заново.
- **Импорт/экспорт:** маршруты и точки с описаниями сохраняются в JSON-файл и
  восстанавливаются на другом устройстве или после переустановки. Настройки
  (промпт, модель OpenRouter, голос, скорость, радиус и стиль карты)
  экспортируются отдельно.
- **Локальное хранение:** Room (точки, маршруты, история), DataStore
  (настройки), EncryptedSharedPreferences (ключи и токены). После переустановки
  точки, маршруты и история восстанавливаются из системного автобэкапа; ключи
  намеренно исключены — их нужно подключить заново.

### Языки

Интерфейс доступен на русском и английском. Язык берётся из системных
настроек; на Android 13+ его можно выбрать отдельно для приложения
(Настройки → Приложения → Solo Traveler’s Guide → Язык, через
`android:localeConfig`). Вместе с интерфейсом локализуются подписи карты,
адреса Nominatim, категории OSM-объектов, системный промпт и шаблон промпта по
умолчанию. Голоса SpeechKit фильтруются по языку приложения (для английского
используется `en-US` и английские голоса). Строки лежат в
`res/values/strings.xml` (русский) и `res/values-en/strings.xml` (английский).

### Установка

1. Откройте [Releases](https://github.com/evgenykon/travel-guide/releases) и
   скачайте последний `travel-guide-vX.Y.Z.apk`. Для самой свежей сборки из
   последнего коммита используйте
   [dev-сборку](https://github.com/evgenykon/travel-guide/releases/download/dev-latest/travel-guide-dev-latest.apk).
2. Разрешите установку из браузера или файлового менеджера, когда Android
   спросит об этом.
3. Откройте приложение и подключите сервисы, описанные ниже. Требуется
   Android 8.0 или новее.

Приложение распространяется как APK через GitHub Releases, а не через Google
Play. Само приложение бесплатное; Yandex Cloud и OpenRouter тарифицируют свои
сервисы отдельно по факту использования.

### Подключение Yandex SpeechKit (обязательно для озвучки)

Приложение использует **авторизованный ключ сервисного аккаунта** (`key.json`):

1. Войдите в [консоль Yandex Cloud](https://console.yandex.cloud/).
2. Откройте **Сервисные аккаунты** и создайте сервисный аккаунт.
3. Назначьте ему роль `ai.speechkit-tts.user`.
4. Создайте **авторизованный ключ** сервисного аккаунта и скачайте `key.json`.
5. В приложении откройте **Настройки → Yandex SpeechKit → «Подключить
   key.json»** и выберите файл. Также можно «поделиться» файлом `key.json` в
   приложение из файлового менеджера.

Приложение само обменяет ключ на IAM-токен (живёт 12 часов) и будет
автоматически его обновлять. Вставлять токен вручную не нужно.

> OAuth-токены Яндекс ID больше не принимаются Yandex Cloud с 01.06.2026,
> поэтому используется авторизованный ключ сервисного аккаунта — поддерживаемый
> способ.

Полезные ссылки:
[документация SpeechKit](https://yandex.cloud/ru/docs/speechkit/) ·
[авторизованные ключи](https://yandex.cloud/ru/docs/iam/concepts/authorization/key)

### Подключение OpenRouter (обязательно для генерации описаний)

1. В приложении откройте **Настройки → OpenRouter → «Войти через OpenRouter»**.
2. Откроется браузер со страницей входа OpenRouter. Войдите и подтвердите
   доступ.
3. Приложение автоматически получит ключ через локальный callback, сохранит его
   в зашифрованном виде и будет использовать для генерации описаний.

Посмотреть или отозвать ключ можно на <https://openrouter.ai/keys>; модели и
цены — на <https://openrouter.ai/models>. Использование оплачивается OpenRouter
с вашего аккаунта.

### Конструктор маршрутов в браузере

[Конструктор](https://evgenykon.github.io/travel-guide/editor.html) полностью
работает в браузере, без бэкенда:

- карта MapLibre GL JS + OpenFreeMap, тот же стиль, что в приложении;
- добавление точек кликом по карте и перетаскиванием, маршруты, радиус
  срабатывания, автоопределение адреса (Nominatim) и OSM-объекта;
- генерация описаний через OpenRouter. Ключ, модель и промпт задаются в шторке
  «API»; ключ живёт в памяти страницы, а по кнопке его можно сохранить в
  `sessionStorage` вкладки (переживает перезагрузку, исчезает при закрытии
  вкладки);
- импорт и экспорт JSON-архивов в формате приложения `BackupFile`
  (`version: 1`, маршруты + точки). Импорт сливается по имени точки и
  координатам, как это делает приложение.

Там нужен только ключ OpenRouter; SpeechKit работает в Android-приложении.

### Сборка из исходников

Требуются JDK 17 и Android SDK (compileSdk 36).

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

### CI/CD (GitHub Actions)

- **Push в `main`** → юнит-тесты, lint, debug APK (артефакт) и обновляемый
  pre-release `dev-latest` с debug APK; изменения в `site/` деплоят сайт.
- **Тег `vX.Y.Z`** → подписанный release APK и GitHub Release.
- Сайт публикуется из `site/` workflow `Deploy promo site`. Включите его один
  раз в **Settings → Pages → Build and deployment → Source: GitHub Actions**.
  Адрес сайта: <https://evgenykon.github.io/travel-guide/>.

#### Подписанные релизы

Сгенерируйте keystore:

```bash
keytool -genkeypair -v -keystore release.jks -alias travel-guide \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -i release.jks -o release.jks.base64   # macOS
# base64 -w0 release.jks > release.jks.base64 # Linux
```

Добавьте секреты в GitHub → Settings → Secrets and variables → Actions:

| Секрет               | Значение                           |
|----------------------|------------------------------------|
| `KEYSTORE_BASE64`    | содержимое `release.jks.base64`    |
| `KEYSTORE_PASSWORD`  | пароль хранилища                   |
| `KEY_ALIAS`          | `travel-guide`                     |
| `KEY_PASSWORD`       | пароль ключа                       |

Если секреты не заданы, release APK соберётся с debug-подписью: он
устанавливается, но не обновит уже установленную подписанную версию.

Выпуск релиза:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Локальный просмотр сайта

```bash
python3 -m http.server 8099 --directory site
```

### Технологии

Kotlin · Jetpack Compose · MapLibre Native · MapLibre GL JS · Room · DataStore ·
Retrofit/OkHttp · kotlinx.serialization · Play Services Location ·
Yandex SpeechKit v1 TTS + v3 voices · OpenRouter API · OpenFreeMap ·
Nominatim · Open-Meteo Elevation · GitHub Actions
