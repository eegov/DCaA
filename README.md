# DCaA: учёт студентов

Лабораторные работы по курсу «Распределённые вычисления и приложения», вариант 2.
Сейчас реализована **лабораторная 1**: основной сервис учёта студентов.

- Требования преподавателя: [docs/labs-requirements.md](docs/labs-requirements.md)
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

- Docker Desktop или Docker Engine с Compose v2: для режимов с контейнерами и для интеграционных тестов.
- JDK 25: для запуска сервиса на хосте и тестов. Maven ставить не нужно.
- PostgreSQL 10+ (проверено на 18): только для режима без Docker.

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

**Без Docker** (PostgreSQL установлена на компьютер):
```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f docs\sql\create-local-db.sql   # один раз
.\mvnw.cmd spring-boot:run
```
Команды для Linux и macOS указаны в начале `docs/sql/create-local-db.sql`. Если PostgreSQL слушает не 5432,
задайте `DB_PORT` в `.env`. Схему, как и в других режимах, создаст Liquibase при старте.

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
Подключаться нужно к той БД, с которой работает сервис, а она зависит от режима запуска:

| Режим запуска | Где БД | Порт на хосте |
|---|---|---|
| Всё в Docker; сервис локально, БД в Docker | контейнер `db` | `DB_PORT` из `.env`, по умолчанию 5432 |
| Без Docker | PostgreSQL, установленная на компьютер | 5432, если при установке не меняли |

Если на компьютере есть и установленная PostgreSQL, и Docker-база, у них разные порты (например, Docker
на 55432), и `psql` без `-p` попадёт в установленную PostgreSQL, а не в контейнер с данными сервиса.

Подключение (пароль `students`, если не меняли `.env`), порт из таблицы:
```powershell
psql -h 127.0.0.1 -p 5432 -U students -d students
```
В Windows `psql` может не быть в `PATH`, тогда полный путь: `& "C:\Program Files\PostgreSQL\18\bin\psql.exe"`.
В Linux ключ `-h` обязателен: без него psql идёт через Unix-сокет и проверяет, совпадает ли имя пользователя ОС
с ролью (peer), и вход под `students` не пройдёт. К Docker-базе можно подключиться и без psql на хосте:
`docker compose exec db psql -U students -d students`.

Внутри psql: `\dt` показывает таблицы, `\d students` структуру, `SELECT * FROM students;` данные, `\q` выход.

Тестовые данные (100 студентов) добавляет `docs/sql/seed-students.sql`: команда подключения с хоста
плюс `-f docs/sql/seed-students.sql`. Через `docker compose exec` так не выйдет: файл лежит на хосте,
а не в контейнере. Удалить данные: `DELETE FROM students WHERE record_book_number LIKE 'SEED-%';`

## Тесты

```powershell
.\mvnw.cmd verify
```
`StudentTest` проверяет правила без БД. Интеграционные `*IT` поднимают свою PostgreSQL через Testcontainers,
поэтому нужен запущенный Docker. Рабочую БД тесты не трогают.

## Почему такой стек

Решения с причинами, включая выбор Java 25 и Spring Boot: [design.md](openspec/changes/archive/2026-11-20-lab1-students-service/design.md).
