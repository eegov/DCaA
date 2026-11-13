---
name: liquibase-migration
description: Порядок изменения схемы БД через Liquibase. Использовать при любом изменении структуры БД: новая таблица, столбец, индекс, ограничение, переименование, а также когда нужно показать время запроса до и после миграции. Не использовать для изменения данных через API и для правки Java-кода без изменения схемы.
---

# Изменение схемы через Liquibase

Почему так строго: Liquibase записывает каждый применённый changeset в таблицу `databasechangelog`
вместе с checksum. Если изменить уже применённый файл, приложение не стартует (`ValidationFailedException`).
Поэтому схема только дополняется новыми файлами. Хук `guard.js` блокирует правку changeset, который уже есть в dev.

## Порядок
1. Создай файл `src/main/resources/db/changelog/NNN-<что-делает>.sql`, где NNN следующий номер.
2. Формат файла:
   ```sql
   --liquibase formatted sql
   --changeset dcaa:NNN-<что-делает>
   <DDL>
   --rollback <обратный DDL>
   ```
   Один changeset делает одно логическое изменение. Rollback пишем всегда.
3. Добавь `- include: { file: NNN-<что-делает>.sql, relativeToChangelogFile: true }` в конец `db.changelog-master.yaml`.
4. Если меняется таблица с JPA-сущностью, обнови сущность: `ddl-auto: validate` сверит её со схемой при старте.
5. Добавь или обнови тест в `MigrationIT`: он поднимает чистую PostgreSQL и проверяет итоговую схему.
6. `./mvnw verify`.

## Как показать эффект миграции (индекс, новая таблица)
Сценарий с pgAdmin и `EXPLAIN ANALYZE` описан в `docs/demo.md`, раздел «Допка».
Время выполнения миграций видно в логе приложения: строки `ChangeSet ... ran successfully in N ms`.
