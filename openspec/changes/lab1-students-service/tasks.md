# Tasks

## 1. Каркас и миграция

- [ ] 1.1 Maven-проект Spring Boot (web, data-jpa, validation, liquibase, postgresql, springdoc, actuator); `./mvnw -q compile` проходит
- [ ] 1.2 Changeset `001-create-students.sql` и `application.yaml` с `ddl-auto: validate`; `MigrationIT` проверяет, что в БД ровно `students` и служебные таблицы Liquibase

## 2. Учёт студентов

- [ ] 2.1 Сущность, репозиторий, сервис и контроллер для приёма, списка, просмотра, редактирования, перевода, отчисления и удаления; `StudentApiIT` покрывает сценарии `student-records`
- [ ] 2.2 Обработка ошибок ProblemDetail (400 с полями, 404, 409, 405); сценарии «Формат ошибок» проходят в `StudentApiIT`
- [ ] 2.3 Swagger UI и OpenAPI; `StudentApiIT` проверяет наличие путей в `/v3/api-docs`

## 3. Запуск

- [ ] 3.1 `Dockerfile` (многоступенчатая сборка) и `compose.yaml` (db + app в сети `students`); ручная проверка: `docker compose --profile app up -d --build`, затем `GET /api/students` отвечает 200
- [ ] 3.2 Локальный режим; ручная проверка: `docker compose up -d db`, `.\mvnw.cmd spring-boot:run`, `GET /api/students` отвечает 200
- [ ] 3.3 README: требования к окружению, оба режима запуска, Swagger; команды из README выполняются как написано

## 4. Проверка

- [ ] 4.1 `./mvnw verify` проходит
- [ ] 4.2 Ревью в чистом контексте (скилл code-review), отчёт `docs/reviews/feature-lab1-students.md` без blocker и major

## Workflow follow-up

- Архивировать изменение (`/openspec-archive-change`) после ревью.
- Влить `feature/lab1-students` в dev через `git merge --no-ff`.
