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
- [x] 3.2 Локальный режим; проверено на Windows 11: `docker compose up -d db`, `.\mvnw.cmd spring-boot:run`, `GET /api/students` отвечает 200; запись, созданная через сервис, найдена `psql` в контейнере `db`, то есть сервис подключился к контейнерной БД, а не к PostgreSQL на хосте
- [x] 3.3 README: требования к окружению, режимы запуска, Swagger; проверено на Windows 11 командами из README: все три режима отвечают на `GET /api/students` 200, Swagger открывается; в режиме без Docker пользователь и БД в службе PostgreSQL 18 уже были созданы, порт 5432 задан через `DB_PORT`
- [x] 3.4 Режим без Docker: `docs/sql/create-local-db.sql`; проверено на Linux с PostgreSQL 18.6 без контейнера: скрипт создал пользователя и БД, Liquibase создал схему, тесты прошли на этой БД
- [x] 3.5 Переносимость: `.gitattributes` (LF для `mvnw`, CRLF для `mvnw.cmd`), `mvnw` хранится исполняемым; проверено `git ls-files -s` и `git ls-files --eol`
- [ ] 3.6 macOS: оба режима, при наличии машины

## 4. Исправления по ревью, раунд 1 (`docs/reviews/feature-lab1-students.md`)

- [x] 4.1 Тесты проверяют тело ответа, формат ProblemDetail и состояние БД в каждом сценарии (п. 2 отчёта)
- [x] 4.2 Убраны actuator и healthcheck сервиса, не требуемые лабой; строгий JSON закреплён в спецификации (п. 3)
- [x] 4.3 Уникальность номера зачётки проверяет ограничение БД, гонка даёт 409 вместо 500 (п. 4)
- [x] 4.4 Общие поля запросов вынесены в `PersonalData`, правила состояния перенесены в сущность и покрыты `StudentTest` (п. 5, 9)
- [x] 4.5 Исправлены ссылки README, добавлены `docs/demo.md` и шаг проверки после запуска (п. 10, 11)

## 5. Стек и окружение

- [x] 5.1 PostgreSQL 18 (образ, Testcontainers, путь volume `/var/lib/postgresql`); тесты прошли на 18.6
- [x] 5.2 Java 25 LTS (`pom.xml`, образы `eclipse-temurin:25`); тесты прошли на Temurin 25 (Linux) и на Windows 11
- [x] 5.3 Проверка версии JDK в сборке (`maven-enforcer-plugin`): на JDK 21 сборка останавливается с понятным сообщением

## 6. Проверка

- [x] 6.1 `./mvnw verify` проходит на Windows 11 с Docker Desktop (Testcontainers, JDK 25)
- [x] 6.2 Ревью в чистом контексте (скилл `code-review`): раунд 3 без blocker и major, проход «схлопывания» проведён, отчёт `docs/reviews/feature-lab1-students.md`

## 7. Обвязка

- [x] 7.1 Обвязка агента по итогам аудита (коммиты `chore(harness)` после `0ef9326`), поведение сервиса не меняется; описание в `docs/harness.md`

## 8. Исправления по ревью, раунд 2

- [x] 8.1 Правка, перевод, отчисление и удаление блокируют строку (`PESSIMISTIC_WRITE`, в PostgreSQL `FOR NO KEY UPDATE`); ручная проверка на jar с PostgreSQL 18: 150 параллельных пар PUT и expel дали 8 потерянных отчислений до исправления и 0 после, параллельные DELETE и PUT не дают 500 (п. 10 отчёта)
- [x] 8.2 Курс дробным числом или строкой даёт 400 вместо молчаливого приведения; сценарий «Курс не целым числом» в `StudentApiIT` (п. 10)
- [x] 8.3 Тесты на клетки «статус × операция»: просмотр и удаление отчисленного, 404 для PUT, перевода, отчисления и удаления, 400 для PUT и перевода (п. 2)
- [x] 8.4 `docs/demo.md` не ждёт healthcheck сервиса, `design.md` описывает блокировку и `StudentTest` (п. 12)
- [x] 8.5 `CLAUDE.md`, «Ловушки»: строка про порт 5432 описывает службу Windows PostgreSQL 18 вместо удалённого PostgreSQL в WSL; Docker-база на 55432

## 9. Исправления по ревью, раунд 3

- [x] 9.1 Тест на курс строкой при переводе; `malformedJsonReturns400` проверяет, что запись не создана (п. 2 отчёта)
- [x] 9.2 Ошибка 404 для студента строится в одном месте `StudentService` (п. 5)
- [x] 9.3 `FOR NO KEY UPDATE` вместо `FOR UPDATE` в `design.md`, `tasks.md` и комментарии `StudentRepository`, число тестов в 6.1 по прогону (п. 12)

## 10. Исправления по ревью, схлопывание

- [x] 10.1 `saveChecked` принимает только `Student`, номер зачётки для сообщения берётся из него (п. 1)
- [x] 10.2 Обоснование блокировки убрано из `StudentService`, осталось в строке у `StudentRepository` и в `design.md` (п. 2, частично)
- [x] 10.3 Комментарий к `saveAndFlush` в `StudentService` сокращён до фразы про `flush`, довод про гонку остался в `design.md` (п. 3)
- [x] 10.4 Удалён Javadoc сущности `Student`, повторявший `design.md` (п. 4)
- [x] 10.5 Из задач убраны числа тестов (п. 5)
- [x] 10.6 Тесты на курс не целым числом и на лишние поля в `PUT` сведены в `@ParameterizedTest` (п. 6)
- [x] 10.7 Javadoc `StudentApiIT` не обещает проверку БД в каждом тесте (п. 7)
- [x] 10.8 Хелперы `get(id)` и `delete(id)` в `StudentApiIT` (п. 8)
- [x] 10.9 Задача 7.1 сокращена до ссылки на `docs/harness.md` (п. 9, частично)
- [x] 10.10 `.env.example`: подсказка про занятый порт 5432 без упоминания WSL (п. 11)
- [x] 10.11 README ссылается на `design.md` вместо повтора довода о выборе стека (п. 13)
- [x] 10.12 Зачем нужен `.mvn/jvm.config`, записано в `design.md` (п. 12)
- [x] 10.13 После правок `./mvnw verify` проходит

## Workflow follow-up

- Архивировать изменение (`/openspec-archive-change`) после ревью.
- Влить `feature/lab1-students` в dev через `git merge --no-ff`, затем dev в master с тегом `lab1`.
