-- Демонстрационные данные: 100 студентов на всех шести курсах, каждый десятый отчислен.
-- Их немного, потому что GET /api/students отдаёт весь список сразу, без страниц.
-- Запускать после старта сервиса внутри контейнера db (команды в README, раздел «Как заглянуть в БД»).
-- Удалить: DELETE FROM students WHERE record_book_number LIKE 'SEED-%';
INSERT INTO students (record_book_number, last_name, first_name, middle_name, group_name, course, status)
SELECT 'SEED-' || g,
       (ARRAY['Иванов', 'Петров', 'Сидоров', 'Смирнов', 'Кузнецов', 'Попов', 'Соколов'])[1 + g % 7],
       (ARRAY['Иван', 'Пётр', 'Анна', 'Мария', 'Олег'])[1 + g % 5],
       NULL,
       (ARRAY['ПИбд', 'ИСЭбд', 'УПАС', 'ИВТАСбд'])[1 + g % 4] || '-' || (1 + g / 4 % 6) || (1 + g / 24 % 3),
       1 + g / 4 % 6,
       CASE WHEN g % 10 = 0 THEN 'EXPELLED' ELSE 'ACTIVE' END
FROM generate_series(1, 100) AS g;
