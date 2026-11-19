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

- [x] 3.1 `Dockerfile` (многоступенчатая сборка) и `compose.yaml` (db + app в сети `students`); проверено на Windows 11: `docker compose --profile app up -d --build`, Swagger открывается
- [ ] 3.2 Локальный режим; ручная проверка: `docker compose up -d db`, `.\mvnw.cmd spring-boot:run`, `GET /api/students` отвечает 200
- [ ] 3.3 README: требования к окружению, режимы запуска, Swagger; команды из README выполняются как написано
- [x] 3.4 Режим без Docker: `docs/sql/create-local-db.sql`; проверено на Linux с PostgreSQL 18.6 без контейнера: скрипт создал пользователя и БД, Liquibase создал схему, 28 тестов прошли на этой БД
- [x] 3.5 Переносимость: `.gitattributes` (LF для `mvnw`, CRLF для `mvnw.cmd`), `mvnw` хранится исполняемым; проверено `git ls-files -s` и `git ls-files --eol`
- [ ] 3.6 macOS: оба режима, при наличии машины

## 4. Исправления по ревью, раунд 1 (`docs/reviews/feature-lab1-students.md`)

- [x] 4.1 Тесты проверяют тело ответа, формат ProblemDetail и состояние БД в каждом сценарии (п. 2 отчёта)
- [x] 4.2 Убраны actuator и healthcheck сервиса, не требуемые лабой; строгий JSON закреплён в спецификации (п. 3)
- [x] 4.3 Уникальность номера зачётки проверяет ограничение БД, гонка даёт 409 вместо 500 (п. 4)
- [x] 4.4 Общие поля запросов вынесены в `PersonalData`, правила состояния перенесены в сущность и покрыты `StudentTest` (п. 5, 9)
- [x] 4.5 Исправлены ссылки README, добавлены `docs/demo.md` и шаг проверки после запуска (п. 10, 11)

## 5. Стек и окружение

- [x] 5.1 PostgreSQL 18 (образ, Testcontainers, путь volume `/var/lib/postgresql`); 28 тестов прошли на 18.6
- [x] 5.2 Java 25 LTS (`pom.xml`, образы `eclipse-temurin:25`); 28 тестов прошли на Temurin 25 (Linux) и на Windows 11
- [x] 5.3 Проверка версии JDK в сборке (`maven-enforcer-plugin`): на JDK 21 сборка останавливается с понятным сообщением

## 6. Проверка

- [x] 6.1 `./mvnw verify` проходит на Windows 11 с Docker Desktop (Testcontainers, JDK 25): 28 тестов
- [ ] 6.2 Ревью в чистом контексте (скилл `code-review`), раунд 2 без blocker и major, проход «схлопывания»

## Workflow follow-up

- Архивировать изменение (`/openspec-archive-change`) после ревью.
- Влить `feature/lab1-students` в dev через `git merge --no-ff`, затем dev в master с тегом `lab1`.
