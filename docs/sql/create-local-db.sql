-- Пользователь и БД для запуска сервиса с PostgreSQL, установленной без Docker.
-- Значения совпадают с .env.example. Выполнять от суперпользователя postgres:
--   Windows:      psql -U postgres -f docs\sql\create-local-db.sql
--   Linux:        sudo -u postgres psql -f docs/sql/create-local-db.sql
--   macOS (brew): psql -d postgres -f docs/sql/create-local-db.sql
CREATE ROLE students LOGIN PASSWORD 'students';
CREATE DATABASE students OWNER students;
