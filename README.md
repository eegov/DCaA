# DCaA: учёт студентов

Лабораторные работы по курсу «Распределённые вычисления и приложения», вариант 2.
Сейчас реализована **лабораторная 1**: основной сервис учёта студентов.

- Требования: [docs/labs-requirements.md](docs/labs-requirements.md)
- Спецификация поведения: [openspec/specs/](openspec/specs/); изменение лабы 1 с решениями и задачами:
  [openspec/changes/archive/2026-11-20-lab1-students-service/](openspec/changes/archive/2026-11-20-lab1-students-service/)
- Как устроена агентская обвязка и как её подключить после клонирования: [docs/harness.md](docs/harness.md)

## Что умеет сервис

| Операция | Запрос |
|---|---|
| Принять студента | `POST /api/students` |
| Список / один студент | `GET /api/students`, `GET /api/students/{id}` |
| Изменить зачётку, ФИО, группу | `PUT /api/students/{id}` |
| Перевести на курс | `POST /api/students/{id}/transfer` |
| Отчислить (запись остаётся) | `POST /api/students/{id}/expel` |
| Удалить запись | `DELETE /api/students/{id}` |

Курс от 1 до 6, статус `ACTIVE` или `EXPELLED`. Отчисленного нельзя редактировать и переводить,
перевод возможен только на другой курс. Поля, которых нет в описании операции, отклоняются.
Ошибки возвращаются в формате `application/problem+json`: 400 некорректные данные, 404 нет студента, 409 конфликт.

В БД одна таблица `students`. Её создаёт Liquibase при старте
(`src/main/resources/db/changelog/`), Hibernate только сверяет сущность со схемой.

## Что нужно установить

Работает одинаково на Windows, Linux и macOS. Различается только имя Maven Wrapper:
`.\mvnw.cmd` в PowerShell, `./mvnw` в bash/zsh.

- Docker Desktop или Docker Engine с Compose v2: для БД и интеграционных тестов.
- JDK 25: для запуска сервиса на хосте и тестов. Maven ставить не нужно.

Настройки по умолчанию лежат в `.env.example`. Чтобы их поменять (например, если порт 5432 занят),
скопируйте файл в `.env` и правьте там: `.env` читают и Compose, и сервис, запущенный на хосте.

## Запуск

**Всё в Docker** (сервис и БД в одной сети `students`, сервис ходит в БД по имени `db`):
```powershell
docker compose --profile app up -d --build
```

**Сервис локально, БД в Docker:**
```powershell
docker compose up -d db
.\mvnw.cmd spring-boot:run        # Linux/macOS: ./mvnw spring-boot:run
```

Чтобы убедиться, что сервис работает, выполните `curl.exe http://localhost:8080/api/students`
(в Linux и macOS просто `curl`): ответом будет `[]` или список студентов.
Swagger UI открывается по адресу http://localhost:8080/swagger-ui.html.

Контейнеры останавливает `docker compose --profile app down`, данные при этом остаются в volume.
С флагом `-v` volume удалится вместе с данными.

Если сервис не стартует с ошибкой `password authentication failed`, значит volume создан раньше с другим
паролем: образ PostgreSQL берёт пароль из `.env` только при первой инициализации пустого volume.
Помогает `docker compose --profile app down -v` и снова `up` (данные при этом удалятся).

## Как заглянуть в БД

Таблицу `students` создаёт Liquibase при первом старте сервиса, поэтому до запуска сервиса БД пустая.
psql запускается внутри контейнера `db`, ставить его на компьютер не нужно.
В командах ниже `students` это `DB_USER` и `DB_NAME` по умолчанию; если меняли их в `.env`, подставьте свои значения.
```powershell
docker compose exec db psql -U students -d students
```
Внутри psql: `\dt` показывает таблицы, `\d students` структуру, `SELECT * FROM students;` данные, `\q` выход.

Тестовые данные (100 студентов) лежат в `docs/sql/seed-students.sql`. Файл нужно сначала скопировать в контейнер:
```powershell
docker compose cp docs/sql/seed-students.sql db:/tmp/seed.sql
docker compose exec db psql -U students -d students -f /tmp/seed.sql
```
Удалить их: `DELETE FROM students WHERE record_book_number LIKE 'SEED-%';` внутри psql.

## Тесты

```powershell
.\mvnw.cmd verify
```
`StudentTest` проверяет правила без БД. Интеграционные `*IT` поднимают свою PostgreSQL через Testcontainers,
поэтому нужен запущенный Docker. Рабочую БД тесты не трогают.

## Почему такой стек

Решения с причинами, включая выбор Java 25 и Spring Boot: [design.md](openspec/changes/archive/2026-11-20-lab1-students-service/design.md).
