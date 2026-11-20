# Ревью fix/drop-host-postgres, раунд 1
Прогон: `./mvnw verify` → BUILD SUCCESS; тестов 37 (StudentTest 5, MigrationIT 2, StudentApiIT 30), упало 0.

| № | Вопрос | Ответ | Доказательство | Важность |
|---|---|---|---|---|
| 1 | Выполнено каждое требование лабы 1? | ДА | Ветка код не трогает: `git diff dev...HEAD --stat -- src pom.xml Dockerfile compose.yaml` пуст. «Локальная работа с базой» по новой дельте `openspec/changes/drop-host-postgres/specs/service-runtime/spec.md:13-18` проверена запуском: `docker compose -p dcaa-rev1 up -d db` (DB_PORT=55433), `java -jar target/students.jar` (SERVER_PORT=8091), `GET /api/students` 200 `[]`. Liquibase, одна таблица, Swagger: `MigrationIT`, `StudentApiIT.swaggerAndOpenApiAreAvailable` (запуском) | — |
| 2 | Каждый сценарий покрыт тестом со статусом, телом и БД? | ДА | Таблица «статус × операция» (чтением `StudentApiIT.java`): ACTIVE × GET 200 `getReturnsStudent`, × PUT 200 `updateChangesPersonalDataAndGroup`, × PUT чужой номер 409 `updateWithRecordBookOfAnotherStudentReturns409`, × PUT с `course`/`status` 400 `updateWithCourseOrStatusReturns400AndKeepsRecord`, × transfer 200 `transferChangesCourseAndGroup`, × transfer на тот же курс 409 `transferToCurrentCourseReturns409`, × expel 200 `expelKeepsRecordWithCourseAndGroup`, × DELETE 204 `deleteRemovesRecord`; EXPELLED × GET 200 `getReturnsExpelledStudent`, × PUT 409 `updateExpelledReturns409`, × transfer 409 `transferExpelledReturns409`, × expel 200 `repeatedExpelReturns200AndKeepsRecord`, × DELETE 204 `deleteRemovesExpelledRecord`; нет записи × GET 404 `getUnknownReturns404`, × PUT/transfer/expel/DELETE 404 `modifyingUnknownStudentReturns404`. Новый сценарий «Локальный сервис и контейнерная БД» закрыт ручной проверкой в `tasks.md:8-9`, как разрешает `openspec/config.yaml:17`; повторил запуском (п. 1) | — |
| 3 | Нет лишнего сверх спецификации и лабы? | НЕТ | `git diff` (рабочее дерево): незакоммиченная правка `README.md:6` «Требования преподавателя» → «Требования». Её нет ни в `proposal.md`, ни в `tasks.md`. Закоммитить отдельной задачей в `tasks.md` или откатить | minor |
| 4 | Ошибки не проглатываются и не превращаются в 500 без причины? | ДА | Код не менялся. `StudentService.java:70-80` переводит в 409 только `uq_students_record_book`, остальное пробрасывает (чтением) | — |
| 5 | Нет дублирования логики? | ДА | В diff только документация; README и шапка `seed-students.sql` ссылаются друг на друга, команды не повторяют (чтением) | — |
| 6 | Нет мёртвого кода и конфигурации? | ДА | `create-local-db.sql` удалён; `git grep create-local-db` вне архива, `docs/reviews/` и самого изменения находит только `openspec/specs/service-runtime/spec.md:32`, которую уберёт archive (запуском) | — |
| 7 | Валидация на границе? | ДА | Код не менялся; `@Valid` в `StudentController.java:33,55,61` (чтением) | — |
| 8 | Схему создаёт только Liquibase, `ddl-auto: validate`? | ДА | `application.yaml:14`; `MigrationIT` (запуском) | — |
| 9 | Бизнес-правила тестируются без запуска системы? | ДА | `StudentTest` без БД, 5 тестов (запуском) | — |
| 10 | Нет ошибок логики и пропущенных крайних случаев? | НЕТ | `README.md:69,76` пишут `-U students -d students` жёстко, а `README.md:37-38` предлагает менять `.env`. Сценарий: в `.env` `DB_USER=app`, `DB_NAME=app`, `docker compose exec db psql -U students -d students` → `FATAL: role "students" does not exist`. Прежний README хотя бы оговаривал «если не меняли `.env`»; нужна такая же оговорка (чтением) | minor |
| 11 | По README можно с нуля запустить оба режима за 10 минут? | ДА | Локальный режим и заливка данных запуском: команды `README.md:74-77` в PowerShell 5.1 (добавлен только `-p dcaa-rev1`) дали `INSERT 0 100`, `GET /api/students` вернул 100 записей, фамилии кириллицей без искажений, то есть удаление `SET client_encoding` безопасно. Сервис запускал из `target/students.jar`, а не через `spring-boot:run`. Docker-режим ветка не меняла (`README.md:41-50`, чтением) | — |
| 12 | Документация без воды и устаревших утверждений? | НЕТ | `README.md:90` отправляет за решениями в архивный `design.md`, а там `design.md:22` «подойдёт и установленная в систему версия» и `design.md:44` «БД в официальном образе или установленная на хост» противоречат новой спецификации. Архив править нельзя (`proposal.md:40-41`), но читатель README увидит отменённый режим как действующий. Достаточно одной фразы в README, что режим без Docker убран изменением `drop-host-postgres` (чтением) | minor |
| 13 | Нет секретов, логов, артефактов? | ДА | `git ls-files`: нет `.env`, `target/`, `*.log`; `.gitignore:2,4,12` (запуском) | — |
| 14 | Коммиты в Conventional Commits, каждый делает одно? | НЕТ | Формат соблюдён. `b4d3ad4` делает две вещи («keep a host psql client… and fix the Cyrillic grep check»), а первую из них отменяет следующий `d8ca28d` («seed through the db container»). Три коммита `docs(openspec)` правят один и тот же план; при желании свернуть их в один до merge (`git log --stat dev..HEAD`) | minor |

Итог: блокирующих 0, важных 0, мелких 4.
Главное, что нужно исправить: закоммитить с задачей или откатить незакоммиченную правку `README.md:6`; добавить в README оговорку про `-U`/`-d` при изменённом `.env`.

- П. 3 не исправлено, потому что правку `README.md:6` сделали вне этой ветки (не автор изменения); она не закоммичена, решение за пользователем.
- П. 12 не исправлено, потому что архив фиксирует решения на момент лабы 1, а действующее поведение README указывает отдельной ссылкой на `openspec/specs/`; фраза об изменении в README устарела бы после следующего же изменения.
- П. 14 не исправлено, потому что три коммита показывают, как план менялся по замечаниям пользователя, а ветка вливается через `--no-ff` и историю не засоряет.

# Схлопывание fix/drop-host-postgres
Прогон: `./mvnw verify` → BUILD SUCCESS; тестов 37 (StudentTest 5, MigrationIT 2, StudentApiIT 30), упало 0.

| № | Что лишнее | Где (файл:строка) | Что удалить или упростить |
|---|---|---|---|
| 1 | Одно и то же «БД всегда в контейнере» сказано дважды, а про Docker для тестов ещё и в разделе «Тесты» (`README.md:86-87`) | `README.md:34`, `README.md:67` | В `:34` оставить «Docker Desktop или Docker Engine с Compose v2: для БД и интеграционных тестов.»; в `:67` убрать «БД в обоих режимах работает в контейнере `db`,», оставить «psql запускается внутри контейнера `db`, ставить его на компьютер не нужно.» |
| 2 | Обоснование «MODIFIED не даёт отбросить сценарий, поэтому REMOVED + ADDED» записано дважды. Сам обход оправдан: `specs-apply.js:394` в OpenSpec 1.16.0 отклоняет MODIFIED без сценария из основной спеки (проверено чтением исходника) | `openspec/changes/drop-host-postgres/proposal.md:18-20`, `openspec/changes/drop-host-postgres/specs/service-runtime/spec.md:7-8` | Оставить только в `**Reason**` дельты; в `proposal.md` пункт свести к «Требование „Локальный запуск“ заменяется требованием „Локальный запуск с БД в контейнере“» |
| 3 | Раздел Impact пересказывает список What Changes теми же файлами | `openspec/changes/drop-host-postgres/proposal.md:43-46` | Свести к одной строке: «Поведение сервиса и сборка не меняются.» |

Итог: блокирующих 0, важных 0, мелких 3.

Убрано всё: п. 1 (README говорит о БД в контейнере один раз, в списке установки Docker «для БД и интеграционных тестов»),
п. 2 (причина замены через REMOVED + ADDED осталась только в `**Reason**` дельты), п. 3 (Impact в proposal сведён к одной строке).
