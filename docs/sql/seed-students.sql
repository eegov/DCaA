-- Демонстрационные данные: 200 000 студентов, 72 группы по ~2800 человек.
-- Запускать в pgAdmin (Query Tool) на БД students после старта сервиса.
-- Удалить: DELETE FROM students WHERE record_book_number LIKE 'SEED-%';
INSERT INTO students (record_book_number, last_name, first_name, middle_name, group_name, course, status)
SELECT 'SEED-' || g,
       (ARRAY['Иванов', 'Петров', 'Сидоров', 'Смирнов', 'Кузнецов', 'Попов', 'Соколов'])[1 + g % 7],
       (ARRAY['Иван', 'Пётр', 'Анна', 'Мария', 'Олег'])[1 + g % 5],
       NULL,
       (ARRAY['ПИбд', 'ИСЭбд', 'УПАС', 'ИВТАСбд'])[1 + g % 4] || '-' || (1 + g / 4 % 6) || (1 + g / 24 % 3),
       1 + g / 4 % 6,
       CASE WHEN g % 10 = 0 THEN 'EXPELLED' ELSE 'ACTIVE' END
FROM generate_series(1, 200000) AS g;

-- Обновить статистику, чтобы планировщик знал реальный размер таблицы.
ANALYZE students;
