<div align="center">

# SpawnAuthReloaded

**An optimized, ultra-lightweight authentication limbo and location restoration engine for Paper, Purpur, Spigot, and Folia.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-21+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.0-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-2b2b2b.svg?style=flat-square)](LICENSE)

[Overview](#overview) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Supported Auth Providers](#supported-auth-providers) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Installation](#installation) • [Compiling from Source](#compiling-from-source) • [License](#license)

[Перейти к русской версии](#spawnauthreloaded-russian)

</div>

---

## Overview

**SpawnAuthReloaded** is a high-performance, multithread-safe limbo and location restoration plugin engineered specifically for modern Paper, Purpur, and Folia server architectures. It is a modernized, heavily optimized fork of [SpawnAuth](https://github.com/sylveya-com/SpawnAuth).

On standard survival, RPG, or minigame servers, unauthenticated players entering the game are often spawned directly at their logout point. This triggers heavy chunk loading, exposes players to combat hazards or falling damage before they can log in, and enables potential chunk exploitation or collision glitches. Legacy limbo plugins often freeze the server with blocking database operations, cause synchronization bottlenecks, or crash outright when run on Folia's regionalized threading architecture.

SpawnAuthReloaded solves these issues completely. It isolates unauthenticated players in a lightweight void limbo or safe spawn point, neutralizes collisions and weather, and completely bypasses heavy chunk generation. Upon successful authentication, the player is restored to their exact coordinates (world, X, Y, Z, yaw, pitch) instantaneously using a non-blocking asynchronous storage pipeline and an in-memory cache.

---

## Key Features

- **Native Folia & Paper Multi-Threading**
  - Built natively upon `FoliaLib` with full support for Folia's regional multithreading model (`runAtEntity`, `teleportAsync`, and `AsyncScheduler`).
  - Thread-safe location transfers across regional chunk boundaries with zero race conditions.
  - Transparent fallback architecture ensuring identical high-performance operation on Paper, Purpur, and Spigot.

- **High-Throughput Asynchronous Storage Pipeline**
  - **SQLite with WAL Mode**: Configured with Write-Ahead Logging (`PRAGMA journal_mode = WAL`, `synchronous = NORMAL`, `busy_timeout = 5000`, `temp_store = MEMORY`) for rapid local disk access.
  - **MySQL / MariaDB via HikariCP**: Enterprise-grade connection pooling with pre-compiled statement caching (`cachePrepStmts`, `prepStmtCacheSize`, `useServerPrepStmts`, `rewriteBatchedStatements`) designed for high-concurrency networks.
  - **Zero-Latency In-Memory Caching (`cache: true`)**: Real-time `ConcurrentHashMap` caching layer guarantees instantaneous (0ms) location restoration upon authentication.
  - **Stateless In-Memory Storage (`type: none`)**: Option to run entirely in RAM for transient lobby instances without touching persistent disk.
  - Dedicated storage worker thread pool (`SpawnAuth-StoragePool`) ensuring zero database queries or disk writes ever stall the Minecraft server tick loop.

- **Dynamic Limbo & Void World Management**
  - Automated void world generator (`fixed.world: limbo`, `fixed.create: true`) equipped with a customizable barrier platform.
  - Eliminates terrain generation and client chunk rendering loads during authentication.
  - Strict limbo environment isolation: automatically locks daytime (`6000L`), locks weather, disables monster/animal spawning, insomnia, fall damage, and enforces keep-inventory.
  - Client-side weather synchronization (`WeatherType.CLEAR`) and player collision disabling (`setCollidable(false)`) to completely prevent limbo push-griefing.

- **Granular Spawn Routing**
  - Three versatile routing modes: `vanilla` (server spawn with spawn-radius spread support), `fixed` (custom coordinates or isolated void limbo), and `disabled` (in-place authentication).
  - Independent spawn policies for newly registered players (`register.spawn-mode`) versus returning players (`login.spawn-mode`).

- **Fault-Tolerant Safety Protocols**
  - Vehicle dismount safety: players riding boats, minecarts, or horses are safely ejected before teleportation to avoid position desync.
  - Graceful shutdown protection (`onDisable`): all unauthenticated online players are restored to their pre-limbo positions upon server stop or reload, preventing players from being permanently stuck in limbo.
  - Dead player & respawn protection: intercepts respawns and dead-state join events cleanly.

---

## Compatibility

| Platform | Supported Versions | Implementation Details |
| :--- | :--- | :--- |
| **Folia** | 1.20 – 1.21.x+ | Native regional entity schedulers and asynchronous teleportation |
| **Paper** | 1.20 – 1.21.x+ | Asynchronous Paper task schedulers and modern chunk load handling |
| **Purpur** | 1.20 – 1.21.x+ | Fully supported |
| **Spigot** | 1.20 – 1.21.x+ | Supported with standard Bukkit asynchronous scheduler fallback |

---

## Supported Auth Providers

SpawnAuthReloaded automatically detects and hooks into the following authentication systems at startup:

| Auth Plugin | Integration Type | Supported Versions |
| :--- | :--- | :--- |
| **AuthMeReloaded** | Event Listener & Native API Hook | 5.6.0+ |
| **nLogin** | Event Listener & Universal API Hook | 2.0+ |
| **OpeNLogin** | Event Listener & Universal API Hook | 1.3+ |
| **LoginSecurity** | Event Listener & Session Hook | 3.3.1+ |
| **LoginTo** | Event Listener Hook | 4.1.1+ |

---

## Commands and Permissions

### Commands

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/spawnauth` | `/sa` | Displays plugin status and version information | `spawnauth.admin` |
| `/spawnauth reload` | `/sa reload` | Reloads configurations, cache, and limbo world references | `spawnauth.admin` |

### Permissions

| Node | Description | Default Assignment |
| :--- | :--- | :--- |
| `spawnauth.admin` | Allows managing SpawnAuthReloaded and executing the reload command | `op` |

---

## Configuration

The default configuration file (`config.yml`):

```yaml
# ==============================================================================
# SpawnAuth Configuration
# ==============================================================================

# The world player returns to after authentication if their previous world is missing.
worlds:
  overworld: world

# Where to move unauthenticated players (waiting spot before auth):
#   vanilla  - leave/move the player at the server spawn of the overworld
#   fixed    - move the player to the fixed spawn coordinates below
#   disabled - do not move the player at all
#
# After authentication the player is always returned to the
# location they had before being moved to the auth spawn.
register:
  spawn-mode: vanilla
login:
  spawn-mode: vanilla

# Where to send the player in "fixed" mode.
fixed:
  # The world to use. Can be an existing world or one created by this plugin.
  world: limbo

  # If true, the void world is created automatically when it doesn't exist
  # (overworld type, with a small barrier platform under the spawn point).
  # If false and the world doesn't exist, the fixed mode is skipped.
  create: false

  # Exact spawn coordinates. Always used in "fixed" mode.
  spawn:
    x: 7.0
    y: 70.0
    z: 7.0
    yaw: 0.0
    pitch: 0.0

  # Radius of the platform around the spawn in the auto-created void world.
  platform-radius: 1

# Storage settings:
#   sqlite - high-performance server-side SQLite database (WAL mode enabled)
#   mysql  - external MySQL/MariaDB database (HikariCP connection pool)
#   none   - temporary in-memory cache only (requires cache: true)
database:
  type: sqlite

  # Prefix used for table names in the database.
  table-prefix: spawnauth

  # Cache locations in memory on top of the storage backend.
  # Highly recommended: guarantees instantaneous (0ms) location restoration upon auth.
  cache: true

  # MySQL connection details (only used when type: mysql)
  mysql:
    host: localhost
    port: 3306
    database: spawnauth
    user: root
    password: ""

    # Extra MySQL JDBC properties
    url-parameters: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

  pool:
    max-pool-size: 10
    min-idle: 2
    connection-timeout-ms: 30000
```

---

## Installation

1. Download the latest `spawnauth-1.0.jar` from the GitHub Releases.
2. Place the JAR file into your server's `plugins/` folder.
3. Make sure one of the supported authentication plugins (`AuthMe`, `nLogin`, `OpeNLogin`, `LoginSecurity`, or `LoginTo`) is installed.
4. Start the server to generate default configurations and SQLite database tables.
5. Customize `plugins/SpawnAuth/config.yml` according to your infrastructure.
6. Run `/spawnauth reload` to apply settings without restarting the server.

---

## Compiling from Source

### Requirements
- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.8+
- Git

### Build Instructions

```bash
git clone https://github.com/NothingFlare/SpawnAuthReloaded.git
cd SpawnAuthReloaded
mvn clean package
```

The resulting compiled binary will be located at:
```text
target/spawnauth-1.0.jar
```

---

## License

This project is licensed under the [MIT License](LICENSE).

Developed and maintained by **NothingFlare**.  
This project is an optimized fork of [SpawnAuth](https://github.com/sylveya-com/SpawnAuth).

---
---

<div align="center" id="spawnauthreloaded-russian">

# SpawnAuthReloaded (Russian)

**Высокопроизводительный, легковесный плагин для изоляции игроков в Limbo на время авторизации и мгновенного возврата на исходную точку для Paper, Purpur, Spigot и Folia.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-21+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.0-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-2b2b2b.svg?style=flat-square)](LICENSE)

[Обзор](#обзор) • [Ключевые особенности](#ключевые-особенности) • [Совместимость](#совместимость-1) • [Провайдеры авторизации](#провайдеры-авторизации) • [Команды и права](#команды-и-права) • [Конфигурация](#конфигурация-1) • [Установка](#установка-1) • [Сборка из исходников](#сборка-из-исходников) • [Лицензия](#лицензия)

</div>

---

## Обзор

**SpawnAuthReloaded** — это высокопроизводительный многопоточный плагин для изоляции игроков в буферной зоне (Limbo) на время аутентификации с последующим мгновенным возвратом на точное место выхода. Плагин представляет собой глубоко переработанный и оптимизированный форк оригинального проекта [SpawnAuth](https://github.com/sylveya-com/SpawnAuth).

На стандартных серверах неавторизованные игроки при входе появляются непосредственно в игровом мире на точке прошлого выхода. Это вызывает резкие скачки нагрузки из-за генерации и подгрузки тяжелых чанков, подвергает игрока риску получения урона от мобов, падения или удушья в блоках до ввода пароля, а также создает уязвимости для эксплойтов коллизий. Устаревшие аналоги limbo-плагинов нередко блокируют основной поток сервера синхронными запросами к базам данных либо полностью несовместимы с многопоточной архитектурой регионов Folia.

SpawnAuthReloaded полностью устраняет эти проблемы. Он мгновенно переносит неавторизованного игрока в изолированный мир-пустоту (Limbo) или на серверный спавн, отключает коллизии и синхронизирует погоду. После успешной авторизации игрок возвращается на точные координаты (мир, X, Y, Z, yaw, pitch) с нулевой задержкой (0 мс) благодаря многоуровневому асинхронному хранилищу и оперативной памяти.

---

## Ключевые особенности

- **Полная интеграция с Folia и Paper Multi-Threading**
  - Разработан с использованием библиотеки `FoliaLib` и полностью учитывает региональное разделение потоков Folia (`runAtEntity`, `teleportAsync`, `AsyncScheduler`).
  - Гарантирует абсолютную потокобезопасность при телепортациях между региональными границами чанков без Race Conditions.
  - Прозрачная архитектура совместимости с серверами Paper, Purpur и Spigot.

- **Высокопроизводительный асинхронный конвейер БД**
  - **SQLite с поддержкой режима WAL**: Использование журнала Write-Ahead Logging (`PRAGMA journal_mode = WAL`, `synchronous = NORMAL`, `busy_timeout = 5000`, `temp_store = MEMORY`) обеспечивает максимальную скорость дисковых операций.
  - **MySQL / MariaDB на базе пула HikariCP**: Промышленный пул соединений с кэшированием подготовленных выражений (`cachePrepStmts`, `prepStmtCacheSize`, `useServerPrepStmts`, `rewriteBatchedStatements`) для крупных сетей серверов.
  - **Кэш оперативной памяти с нулевой задержкой (`cache: true`)**: Потокобезопасная хэш-таблица `ConcurrentHashMap` обеспечивает моментальное (0 мс) считывание и восстановление координат без ожидания ответа базы данных.
  - **Режим чистой памяти (`type: none`)**: Возможность работы исключительно в RAM для временных лобби-серверов без дисковых операций.
  - Отдельный пул потоков хранилища (`SpawnAuth-StoragePool`), благодаря которому ни одна операция ввода-вывода не затрагивает основной тик-поток Minecraft.

- **Изоляция Limbo и автоматический генератор пустотного мира**
  - Встроенный генератор мира пустоты (`fixed.world: limbo`, `fixed.create: true`) с созданием барьерной платформы заданного радиуса.
  - Исключает нагрузку от рендеринга и генерации тяжелых чанков во время аутентификации.
  - Автоматическая фиксация игровых правил: заморозка дневного цикла (`6000L`), отключение смены погоды, запрет спавна мобов, отключение бессонницы, отключение урона от падения и сохранение инвентаря.
  - Индивидуальная ясная погода (`WeatherType.CLEAR`) и отключение коллизии игроков (`setCollidable(false)`), исключающее выталкивание и грифинг в Limbo.

- **Гибкая маршрутизация спавна**
  - Три режима: `vanilla` (спавн основного мира с поддержкой радиуса разброса), `fixed` (фиксированные координаты или мир Limbo) и `disabled` (авторизация на месте).
  - Раздельные правила спавна для впервые регистрирующихся игроков (`register.spawn-mode`) и для входящих повторно (`login.spawn-mode`).

- **Защита от сбоев и безопасность игроков**
  - Автоматическое высаживание из транспорта (лодки, вагонетки, лошади) перед перемещением во избежание десинхронизации позиции.
  - Корректная обработка отключения плагина (`onDisable`): при перезагрузке или остановке сервера все неавторизованные игроки автоматически возвращаются на исходные позиции, что защищает от застревания в пустоте.
  - Безопасная обработка смерти и возрождения игроков (`PlayerRespawnEvent` и респавн погибших при подключении).

---

## Совместимость

| Платформа | Поддерживаемые версии | Особенности реализации |
| :--- | :--- | :--- |
| **Folia** | 1.20 – 1.21.x+ | Нативные региональные планировщики сущностей и асинхронные телепортации |
| **Paper** | 1.20 – 1.21.x+ | Асинхронные планировщики Paper и современные события загрузки инвентаря/мира |
| **Purpur** | 1.20 – 1.21.x+ | Полная поддержка всех возможностей |
| **Spigot** | 1.20 – 1.21.x+ | Поддержка со стандартным Bukkit fallback планировщиком |

---

## Провайдеры авторизации

SpawnAuthReloaded автоматически распознает установленный плагин авторизации при запуске:

| Плагин авторизации | Тип интеграции | Поддерживаемые версии |
| :--- | :--- | :--- |
| **AuthMeReloaded** | Слушатель событий и прямой API-хук | 5.6.0+ |
| **nLogin** | Слушатель событий и Universal API | 2.0+ |
| **OpeNLogin** | Слушатель событий и Universal API | 1.3+ |
| **LoginSecurity** | Слушатель событий и сессионный хук | 3.3.1+ |
| **LoginTo** | Слушатель событий авторизации | 4.1.1+ |

---

## Команды и права

### Команды

| Команда | Алиасы | Описание | Право по умолчанию |
| :--- | :--- | :--- | :--- |
| `/spawnauth` | `/sa` | Показывает статус плагина и информацию о версии | `spawnauth.admin` |
| `/spawnauth reload` | `/sa reload` | Перезагружает конфигурацию, кэш и ссылки на мир Limbo | `spawnauth.admin` |

### Права

| Право | Описание | Значение по умолчанию |
| :--- | :--- | :--- |
| `spawnauth.admin` | Доступ к управлению SpawnAuthReloaded и команде перезагрузки | `op` |

---

## Конфигурация

Файл конфигурации по умолчанию (`config.yml`):

```yaml
# ==============================================================================
# Конфигурация SpawnAuth
# ==============================================================================

# Мир, в который возвращается игрок после авторизации, если его исходный мир недоступен.
worlds:
  overworld: world

# Куда направлять неавторизованных игроков (место ожидания перед вводом пароля):
#   vanilla  - оставить/перенести игрока на серверный спавн основного мира
#   fixed    - перенести игрока на фиксированные координаты, указанные ниже
#   disabled - не перемещать игрока
#
# После успешной авторизации игрок всегда возвращается на
# позицию, на которой он находился до переноса.
register:
  spawn-mode: vanilla
login:
  spawn-mode: vanilla

# Параметры перемещения в режиме "fixed".
fixed:
  # Используемый мир. Может быть существующим миром или автоматически созданным плагином.
  world: limbo

  # Если true, пустотный мир создается автоматически при его отсутствии
  # (тип overworld с небольшой платформой из барьеров под точкой спавна).
  # Если false и мир отсутствует, режим fixed будет пропущен.
  create: false

  # Точные координаты точки спавна. Всегда используются в режиме "fixed".
  spawn:
    x: 7.0
    y: 70.0
    z: 7.0
    yaw: 0.0
    pitch: 0.0

  # Радиус платформы вокруг спавна в автоматически создаваемом мире пустоты.
  platform-radius: 1

# Настройки базы данных:
#   sqlite - высокопроизводительная база SQLite (с активированным режимом WAL)
#   mysql  - внешняя база данных MySQL/MariaDB (пул подключений HikariCP)
#   none   - только временный кэш в оперативной памяти (требует cache: true)
database:
  type: sqlite

  # Префикс названий таблиц в базе данных.
  table-prefix: spawnauth

  # Кэширование локаций в оперативной памяти поверх хранилища базы данных.
  # Настоятельно рекомендуется: гарантирует мгновенное (0 мс) восстановление позиции.
  cache: true

  # Настройки подключения MySQL (используются только при type: mysql)
  mysql:
    host: localhost
    port: 3306
    database: spawnauth
    user: root
    password: ""

    # Дополнительные параметры JDBC URL для MySQL
    url-parameters: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

  pool:
    max-pool-size: 10
    min-idle: 2
    connection-timeout-ms: 30000
```

---

## Установка

1. Скачайте актуальный файл `spawnauth-1.0.jar` со страницы GitHub Releases.
2. Поместите JAR-файл в папку `plugins/` вашего сервера.
3. Убедитесь, что на сервере установлен один из поддерживаемых плагинов авторизации (`AuthMe`, `nLogin`, `OpeNLogin`, `LoginSecurity` или `LoginTo`).
4. Запустите сервер для генерации стандартного файла конфигурации и таблиц SQLite.
5. Настройте файл `plugins/SpawnAuth/config.yml` под нужды вашего проекта.
6. Выполните команду `/spawnauth reload` для применения изменений без перезагрузки сервера.

---

## Сборка из исходников

### Требования
- Java Development Kit (JDK) 21 или выше
- Apache Maven 3.8+
- Git

### Инструкция по сборке

```bash
git clone https://github.com/NothingFlare/SpawnAuthReloaded.git
cd SpawnAuthReloaded
mvn clean package
```

Скомпилированный файл будет находиться по пути:
```text
target/spawnauth-1.0.jar
```

---

## Лицензия

Проект распространяется под лицензией [MIT License](LICENSE).

Автор и разработчик форка: **NothingFlare**.  
Данный проект является оптимизированным форком [SpawnAuth](https://github.com/sylveya-com/SpawnAuth).
