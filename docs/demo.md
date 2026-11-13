# Демонстрация лабораторной 1

## За 10 минут до сдачи

1. Запущен Docker Desktop.
2. `git switch master` (на master лежит сданная версия).
3. Если порт 5432 занят, в `.env` стоит `DB_PORT=55432` (скопировать из `.env.example`).
4. `docker compose --profile app up -d --build`, затем `docker compose ps`: оба сервиса `healthy`.
5. Открыт http://localhost:8080/swagger-ui.html.
6. В pgAdmin сохранено подключение (см. ниже).

## 1. Swagger: CRUD и правила

Порядок, который показывает все требования за пару минут:

| Шаг | Запрос | Что увидеть |
|---|---|---|
| 1 | `POST /api/students` с примером из Swagger | 201, `status: ACTIVE`, заголовок `Location` |
| 2 | ещё один `POST` с другим `recordBookNumber` | 201 |
| 3 | `POST` с тем же `recordBookNumber` | 409 «Номер зачётки уже занят» |
| 4 | `POST` с `"course": 9` и пустой фамилией | 400, в `errors` оба поля |
| 5 | `GET /api/students` | оба студента |
| 6 | `PUT /api/students/1` (новая группа) | 200 |
| 7 | `POST /api/students/1/transfer` `{"course": 3, "groupName": "ПИбд-31"}` | 200, курс 3 |
| 8 | `POST /api/students/1/expel` | 200, `EXPELLED`, курс и группа на месте |
| 9 | снова `transfer` для студента 1 | 409: отчисленного не переводим |
| 9а | `transfer` студента 2 на его же курс | 409: перевод только на другой курс |
| 10 | `DELETE /api/students/2`, затем `GET /api/students/2` | 204, потом 404 |

После каждого шага можно обновить таблицу в pgAdmin: данные меняются в той же БД.

## 2. Два режима запуска

**Docker-сеть.** Сервис и БД в сети `dcaa_students`:
```powershell
docker network inspect dcaa_students --format "{{range .Containers}}{{.Name}} {{end}}"
```
Внутри сети сервис обращается к БД по имени `db` (`DB_HOST: db` в `compose.yaml`). Docker сам резолвит имя сервиса в IP контейнера.

**Локально.** Остановить контейнер сервиса, БД оставить:
```powershell
docker compose stop app
.\mvnw.cmd spring-boot:run
```
Тот же Swagger, те же данные: сервис теперь на хосте и ходит в БД через опубликованный порт `localhost:DB_PORT`.
Значения берутся из `.env` (Spring Boot читает его через `spring.config.import`).

## 3. pgAdmin 4

Register → Server:
- General → Name: `dcaa`
- Connection → Host: `localhost`, Port: значение `DB_PORT` (5432 или 55432), Database: `students`, Username: `students`, Password: `students`

Что показать в Schemas → public → Tables:
- `students`: единственная предметная таблица.
- `databasechangelog`: журнал Liquibase. Строка на каждый применённый changeset: `id`, `author`, `filename`,
  `dateexecuted`, `md5sum`. По `md5sum` Liquibase замечает, что кто-то изменил уже применённый файл.
- `databasechangeloglock`: блокировка, чтобы два экземпляра сервиса не накатывали миграции одновременно.

## 4. Допка: миграция и время запроса

Ожидаемое задание: добавить таблицу или индекс новой миграцией и показать, что время запроса изменилось.
Цифры ниже получены на PostgreSQL 17 при подготовке; на твоей машине будут другие, но порядок тот же.

**Шаг 1. Данные.** pgAdmin → Query Tool → открыть `docs/sql/seed-students.sql` → Execute (F5).
200 000 студентов, 72 группы примерно по 2800 человек.

**Шаг 2. Замер до миграции:**
```sql
EXPLAIN ANALYZE SELECT * FROM students WHERE group_name = 'ПИбд-41';
```
Ожидаемо `Seq Scan`, `Rows Removed by Filter: 197222`, `Execution Time` около 18 ms:
PostgreSQL читает всю таблицу и проверяет каждую строку.

**Шаг 3. Миграция через агента.** В Claude Code:
```
/openspec-propose допка: индекс по group_name в students, показать разницу EXPLAIN ANALYZE до и после
```
Агент по скиллу `liquibase-migration` создаёт новый файл (не правит `001`), например:
```sql
--liquibase formatted sql

--changeset dcaa:002-index-students-group-name
CREATE INDEX idx_students_group_name ON students (group_name);
--rollback DROP INDEX idx_students_group_name;
```
и добавляет его в `db.changelog-master.yaml`. Пересобрать и перезапустить сервис:
`docker compose --profile app up -d --build`. В логе (`docker compose logs app`) строка
`ChangeSet db/changelog/002-... ran successfully in N ms`, в `databasechangelog` появилась вторая строка.

**Шаг 4. Замер после:** тот же `EXPLAIN ANALYZE`. Ожидаемо `Bitmap Index Scan on idx_students_group_name`
и около 3 ms. PostgreSQL находит нужные строки по индексу (B-дерево) и читает только их.

Если просят новую **таблицу**, порядок тот же: новый changeset с `CREATE TABLE` и `--rollback DROP TABLE`,
при необходимости JPA-сущность (без неё `ddl-auto: validate` таблицу просто не проверяет), замер запроса
с `JOIN` до и после индекса по внешнему ключу.

Убрать демо-данные: `DELETE FROM students WHERE record_book_number LIKE 'SEED-%';`

## 5. Что рассказывать

**Как стартует сервис.** Spring Boot поднимает пул соединений к PostgreSQL, затем Liquibase: берёт блокировку
в `databasechangeloglock`, читает `db.changelog-master.yaml`, сравнивает список changeset с `databasechangelog`,
применяет новые по порядку, для каждого пишет строку с checksum. После этого Hibernate в режиме `validate`
сверяет сущность `Student` с реальной таблицей и падает при расхождении. Схему создаёт только Liquibase.

**Почему одна таблица.** Требование лабы 1. Служебные таблицы Liquibase не предметные.

**Отчисление и удаление.** Отчисление меняет статус, запись остаётся (понадобится отчёту в лабе 2).
Удаление стирает запись, нужно для полного CRUD.

**Обвязка.** Кратко по `docs/harness.md`: правила в `CLAUDE.md`, спеки в OpenSpec, хуки блокируют необратимое,
ревью в чистом контексте с отчётом в `docs/reviews/`, без отчёта ветка не вливается в dev.
