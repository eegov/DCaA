-- Пользователь и БД для запуска сервиса с PostgreSQL, установленной без Docker.
-- Значения совпадают с .env.example. Выполнять от суперпользователя postgres:
--   Windows:      & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f docs\sql\create-local-db.sql
--                 (установщик PostgreSQL для Windows не добавляет psql в PATH; 18 замените на свою версию)
--   Linux:        sudo -u postgres psql -f docs/sql/create-local-db.sql
--   macOS (brew): psql -d postgres -f docs/sql/create-local-db.sql
CREATE ROLE students LOGIN PASSWORD 'students';
CREATE DATABASE students OWNER students;
