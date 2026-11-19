# DCaA: учёт студентов

Лабораторные работы по курсу «Распределённые вычисления и приложения», вариант 2.
Сейчас реализована **лабораторная 1**: основной сервис учёта студентов.

- Требования преподавателя: [docs/labs-requirements.md](docs/labs-requirements.md)
- Спецификация поведения: [openspec/](openspec/). Пока изменение не сдано, оно лежит в `openspec/changes/lab1-students-service/`,
  после сдачи (архивации) его требования переносятся в `openspec/specs/`
- Как устроена агентская обвязка и как её подключить после клонирования: [docs/harness.md](docs/harness.md)
- Сценарий демонстрации и pgAdmin: [docs/demo.md](docs/demo.md)

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

Настройки по умолчанию лежат в `.env.example`. Если нужно что-то поменять (например, порт 5432 занят),
скопируй его в `.env`: этот файл читают и Compose, и локально запущенный сервис.

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
psql -U postgres -f docs\sql\create-local-db.sql   # один раз: пользователь и БД students
.\mvnw.cmd spring-boot:run
```
Команды для Linux и macOS указаны в начале `docs/sql/create-local-db.sql`. Если PostgreSQL слушает не 5432,
задай `DB_PORT` в `.env`. Схему, как и в других режимах, создаст Liquibase при старте.

Проверка: `curl.exe http://localhost:8080/api/students` возвращает `[]` (или список студентов).
Swagger UI: http://localhost:8080/swagger-ui.html

Остановить с сохранением данных: `docker compose --profile app down`.
С флагом `-v` удалится и volume с данными.

## Тесты

```powershell
.\mvnw.cmd verify
```
`StudentTest` проверяет правила без БД. Интеграционные `*IT` поднимают свою PostgreSQL через Testcontainers,
поэтому нужен запущенный Docker. Рабочую БД тесты не трогают.

## Почему такой стек

Java 25 (текущий LTS) и Spring Boot выбраны под всю серию: в лабах 3 и 6 нужны `RestTemplate` и `CompletableFuture`.
Остальные решения с причинами: `design.md` изменения `lab1-students-service` в `openspec/changes/`.
