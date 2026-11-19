# service-runtime Specification

## Purpose

Как сервис учёта студентов создаёт схему БД, документирует API и запускается локально и в Docker.

## Requirements

### Requirement: Схема через миграции
Система SHALL создавать схему БД миграциями Liquibase при старте. В БД ровно одна предметная таблица `students`,
кроме служебных таблиц Liquibase. ORM схему не создаёт и не меняет.

#### Scenario: Старт на пустой БД
- **WHEN** сервис запускается на пустой БД
- **THEN** в схеме появляются таблицы `students`, `databasechangelog`, `databasechangeloglock` и больше никаких

### Requirement: Документация API
Система SHALL публиковать Swagger UI и описание OpenAPI со всеми операциями над студентами.

#### Scenario: Swagger
- **WHEN** клиент открывает `/swagger-ui.html` и `/v3/api-docs`
- **THEN** страница Swagger доступна, а OpenAPI содержит пути `/api/students` и `/api/students/{id}`

### Requirement: Локальный запуск
Система SHALL запускаться локально (JVM на хосте) с PostgreSQL в Docker-контейнере или установленной на хост.

#### Scenario: Локальный сервис и контейнерная БД
- **WHEN** разработчик выполняет `docker compose up -d db` и запускает сервис через Maven Wrapper
- **THEN** сервис подключается к БД через опубликованный порт и отвечает на `GET /api/students`

#### Scenario: Без Docker
- **WHEN** на хосте установлена PostgreSQL, создан пользователь и БД из `docs/sql/create-local-db.sql`, сервис запущен через Maven Wrapper
- **THEN** Liquibase создаёт схему и сервис отвечает на `GET /api/students`

### Requirement: Кроссплатформенность
Система SHALL собираться и запускаться на Windows, Linux и macOS одними и теми же шагами;
различается только имя Maven Wrapper (`mvnw.cmd` или `mvnw`).

#### Scenario: Свежий клон
- **WHEN** разработчик клонирует репозиторий на любой из трёх ОС
- **THEN** Maven Wrapper запускается без ручной правки прав и переводов строк, и команды из README выполняются как написано

### Requirement: Запуск в Docker-сети
Система SHALL запускаться в контейнере в одной Docker-сети с БД и обращаться к БД по имени сервиса.

#### Scenario: Полный контейнерный запуск
- **WHEN** разработчик выполняет `docker compose --profile app up -d --build`
- **THEN** сервис подключается к `db:5432` внутри сети `students` и отвечает на `GET /api/students`
