# Tasks

## 1. Убрать режим без Docker

- [ ] 1.1 Удалить `docs/sql/create-local-db.sql`; `git grep create-local-db` вне архива и `docs/reviews/` ничего не находит
- [ ] 1.2 README: убрать режим «Без Docker», абзац о запуске с установленной PostgreSQL через `DB_PORT=5432`
  и PostgreSQL из списка «Что нужно установить»; Docker в этом списке нужен для БД во всех режимах и для тестов.
  Проверка: оба оставшихся режима из README поднимаются командами как написано и отвечают на `GET /api/students` 200
- [ ] 1.3 README, «Как заглянуть в БД»: подключение только `docker compose exec db psql -U students -d students`;
  таблица режимов, команды psql с хоста, путь к `psql.exe`, подсказки про `-h` и `-p` убираются.
  Тестовые данные: `docker compose cp docs/sql/seed-students.sql db:/tmp/seed.sql`, затем
  `docker compose exec db psql -U students -d students -f /tmp/seed.sql`.
  Проверка: в PowerShell 5.1 команды выполняются как написаны, psql печатает `INSERT 0 100`,
  `GET /api/students` отдаёт фамилии кириллицей без искажений
- [ ] 1.4 `docs/sql/seed-students.sql`: шапка описывает запуск через контейнер. Строка `SET client_encoding` и комментарий
  про консоль Windows убираются, если без них сид в контейнере проходит с кириллицей (проверка как в 1.3)
- [ ] 1.5 `CLAUDE.md`: в ловушке про порт 5432 нет пометки «(режим без Docker)»; `git grep "[Бб]ез Docker"` вне архива
  и `docs/reviews/` ничего не находит (регистр перечислен явно: `-i` с кириллицей ненадёжен)

## 2. Проверка

- [ ] 2.1 `openspec validate drop-host-postgres --strict` проходит
- [ ] 2.2 Ревью в чистом контексте (скилл `code-review`)
- [ ] 2.3 `./mvnw verify` проходит

## Workflow follow-up

- Архивировать изменение, влить `fix/drop-host-postgres` в dev через `git merge --no-ff`.
- Тег только по просьбе пользователя.
