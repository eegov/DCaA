# Tasks

## 1. Каркас и миграция

- [x] 1.1 Maven-проект Spring Boot (webmvc, data-jpa, validation, liquibase, postgresql, springdoc); `./mvnw -q compile` проходит
- [x] 1.2 Changeset `001-create-students.sql` и `application.yaml` с `ddl-auto: validate`; `MigrationIT` проверяет, что в БД ровно `students` и служебные таблицы Liquibase

## 2. Учёт студентов

- [x] 2.1 Сущность, репозиторий, сервис и контроллер для приёма, списка, просмотра, редактирования, перевода, отчисления и удаления; `StudentApiIT` покрывает сценарии `student-records`
- [x] 2.2 Обработка ошибок ProblemDetail (400 с полями, 404, 409, 405); сценарии «Формат ошибок» проходят в `StudentApiIT`
- [x] 2.3 Swagger UI и OpenAPI; `StudentApiIT` проверяет `/swagger-ui.html` и пути в `/v3/api-docs`
- [x] 2.4 Правила состояния в сущности `Student`; unit-тест `StudentTest` проходит в `./mvnw test` без Docker

## 3. Запуск

- [x] 3.1 `Dockerfile` (многоступенчатая сборка) и `compose.yaml` (db + app в сети `students`); ручная проверка: `docker compose --profile app up -d --build`, затем `GET /api/students` отвечает 200
- [ ] 3.2 Локальный режим; ручная проверка: `docker compose up -d db`, `.\mvnw.cmd spring-boot:run`, `GET /api/students` отвечает 200
- [x] 3.4 Режим без Docker: `docs/sql/create-local-db.sql`; проверено на Linux с PostgreSQL 18.6, установленной без контейнера: скрипт создал пользователя и БД, все 28 тестов прошли на этой БД: Liquibase создал схему, все операции API ответили по спецификации
- [x] 3.5 Переносимость: `.gitattributes` (LF для `mvnw`, CRLF для `mvnw.cmd`), `mvnw` хранится в git как исполняемый; проверено `git ls-files -s` и `git ls-files --eol`
- [ ] 3.6 Ручной запуск обоих режимов с Docker на Windows; macOS проверить при наличии машины
- [ ] 3.3 README: требования к окружению, оба режима запуска, Swagger; команды из README выполняются как написано

## 4. Проверка


- [x] 4.1 `./mvnw verify` проходит на машине с Docker (Testcontainers): Windows 11, Docker Desktop, 28 тестов
- [ ] 4.2 Ревью в чистом контексте (скилл code-review), отчёт `docs/reviews/feature-lab1-students.md` без blocker и major

## Workflow follow-up

- Архивировать изменение (`/openspec-archive-change`) после ревью.
- Влить `feature/lab1-students` в dev через `git merge --no-ff`.
