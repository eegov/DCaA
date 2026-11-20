# Spec Delta

## REMOVED Requirements

### Requirement: Локальный запуск
**Reason**: Требование допускало PostgreSQL, установленную на хост, без Docker. Задание лабы 1 требует БД в контейнере,
поэтому режим без Docker лишний. Через MODIFIED сценарий «Без Docker» не убрать: OpenSpec не даёт MODIFIED
отбрасывать сценарии, поэтому требование заменено новым «Локальный запуск с БД в контейнере».
**Migration**: Запускать сервис на хосте с БД из `docker compose up -d db`, как в сценарии нового требования.

## ADDED Requirements

### Requirement: Локальный запуск с БД в контейнере
Система SHALL запускаться локально (JVM на хосте) с PostgreSQL в Docker-контейнере.

#### Scenario: Локальный сервис и контейнерная БД
- **WHEN** разработчик выполняет `docker compose up -d db` и запускает сервис через Maven Wrapper
- **THEN** сервис подключается к БД через опубликованный порт и отвечает на `GET /api/students`
