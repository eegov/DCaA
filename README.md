# DCaA: учёт студентов

Лабораторные работы по курсу «Распределённые вычисления и приложения», вариант 2.
Сейчас реализована **лабораторная 1**: основной сервис учёта студентов.

- Требования преподавателя: [docs/labs-requirements.md](docs/labs-requirements.md)
- Спецификация поведения: [openspec/specs/](openspec/specs/)
- Как устроена агентская обвязка: [docs/harness.md](docs/harness.md)
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

Курс от 1 до 6, статус `ACTIVE` или `EXPELLED`. Отчисленного нельзя редактировать и переводить.
Ошибки возвращаются в формате `application/problem+json`: 400 некорректные данные, 404 нет студента, 409 конфликт.

В БД одна таблица `students`. Её создаёт Liquibase при старте
(`src/main/resources/db/changelog/`), Hibernate только сверяет сущность со схемой.

## Что нужно установить

- Docker Desktop (Compose v2)
- JDK 21, только для локального запуска сервиса и тестов. Maven ставить не нужно, есть Maven Wrapper.

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

Swagger UI: http://localhost:8080/swagger-ui.html

Остановить с сохранением данных: `docker compose --profile app down`.
С флагом `-v` удалится и volume с данными.

## Тесты

```powershell
.\mvnw.cmd verify
```
Интеграционные тесты поднимают свою PostgreSQL через Testcontainers, поэтому нужен запущенный Docker.
Рабочую БД тесты не трогают.

## Почему такой стек

Java 21 и Spring Boot выбраны под всю серию лабораторных: в лабах 3 и 6 нужны `RestTemplate`
и `CompletableFuture`, это API из мира Java/Spring. PostgreSQL даёт ограничения целостности на уровне БД.
Подробнее о решениях: [design.md](openspec/changes/archive/) изменения `lab1-students-service`.
