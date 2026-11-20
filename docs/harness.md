# Обвязка агента

Агент: Claude Code. Обвязка здесь понимается как «упряжь»: всё, что надето на готового агента,
чтобы он работал по правилам этого проекта (гл. 3 «Агенты и вайб-кодинг»). Сам агентный цикл чужой,
правила, процедуры, проверки и границы свои.

## Подключение после клонирования

Один раз на машине разработчика:
```powershell
java -version                                  # нужен JDK 25; сборка проверит это сама
git config core.hooksPath .githooks            # git-хуки проекта
git config merge.ff false                      # merge всегда с merge-коммитом: при fast-forward pre-merge-commit не срабатывает
npm i -g @fission-ai/openspec@1.16.0           # та же версия, что сгенерировала скиллы
openspec config set delivery skills            # только скиллы, без дублирующих slash-команд
```
Обе команды `git config` выполняет человек: `guard.js` не даёт агенту менять `core.hooksPath` и `merge.ff`.
Хуки Claude Code (`.claude/settings.json`) подключаются сами при открытии проекта. На Windows им нужны
Git Bash (его требует сам Claude Code) и Node.js в `PATH`.

## Из чего состоит

| Часть | Где лежит | Что делает | Почему именно так |
|---|---|---|---|
| Файл правил | `CLAUDE.md` (`AGENTS.md` ссылается на него) | Подкладывается в контекст на каждом ходу: что за проект, инварианты, ловушки, как проверить | Только то, что агент не узнает из кода (гл. 10). Каждый запрет с причиной (гл. 11) |
| Спецификации | `openspec/` | Требования и сценарии до кода; изменение проходит propose → apply → ревью → archive | Требование преподавателя. Код сверяется со спекой, а не с перепиской |
| Скиллы OpenSpec | `.claude/skills/openspec-*` | Процедуры propose, apply, explore, update, sync, archive | Сгенерированы `openspec init --tools claude`, мы их не писали. Режим `delivery: skills`, чтобы не было дублей в виде slash-команд |
| Скилл `code-review` | `.claude/skills/code-review/` | Цикл ревью: раунды до чистого результата, затем «схлопывание»; исправления каждого раунда записываются задачами в `tasks.md` | Нельзя быть себе судьёй (гл. 22) |
| Субагент `reviewer` | `.claude/agents/reviewer.md` | Проверяет ветку в чистом контексте по 14 вопросам «да/нет», пишет отчёт | Просьба «оцени качество» даёт вежливую пустоту, работает список вопросов с ответом да или нет (гл. 22) |
| Хук SessionStart | `.claude/hooks/session-start.sh` | В начале сессии показывает агенту ветку, правки, активные OpenSpec-изменения | Инструкция «прочитай состояние» срабатывает редко, хук всегда (гл. 13) |
| Хук PreToolUse | `.claude/hooks/guard.js` | Блокирует `down -v`, удаление volumes, force push, `reset --hard`, `git clean -f`, `git merge` без `--no-ff` или с `--ff`; отключение git-хуков (`--no-verify` и его сокращения, `commit -n`, запись `core.hooksPath`, `merge.ff`, `branch.<имя>.mergeoptions`, `git -c` с ними, переменные `GIT_CONFIG_*`); команду с `.env` (кроме `.env.example` и сообщения `-m` у commit, tag, merge, в том числе heredoc); правку уже влитых миграций: через Edit и Write, а в Bash и PowerShell часть команды с именем влитого changeset, каталогом changelog целиком или глобом в нём, если это не чтение (`cat`, `Get-Content`, `grep`, `ls`, `find`, `git diff/log/show`) и не `git restore`. Новый changeset ветки не трогает | Необратимые действия закрываем кодом, а не текстом (гл. 13, 24). Агент получает причину отказа и что делать вместо этого. Для миграций запрет по умолчанию: способов записать файл больше, чем прочитать. Guard ловит ошибки агента по явному тексту команды, от намеренного обхода (`bash -c`, имя файла по частям, правка `.git/config`) не защищает |
| Права | `.claude/settings.json` → `permissions` | Разрешает безопасные команды в Bash и PowerShell без подтверждения, запрещает чтение `.env` | Встроенный механизм дешевле своего кода |
| Git-хуки | `.githooks/` | `pre-commit`: нет коммитов в master/dev и `.env`, а завершение merge в dev после конфликта проходит тот же шлюз, что `pre-merge-commit` (git его в этом случае не вызывает); `commit-msg`: Conventional Commits; `pre-merge-commit`: в dev не вливается feature/fix без отчёта ревью, в котором последний раунд без blocker и major, пройдено схлопывание, а после коммита с отчётом менялись только `*.md` и `openspec/` (ловит забытые правки, от намеренного обхода не защищает); `chore/*` вливается без отчёта, если не меняет `src/`, `pom.xml`, `Dockerfile` и `compose.yaml` (сравнение с merge-base) | Работают одинаково для человека и агента. Последний хук превращает ревью из пожелания в шлюз (гл. 3, п. 8) |

Глобальные инструменты автора, которых нет в репозитории:
- `antislop-chat`: скилл, убирает из документации признаки ИИ-текста.
- [Agent Reach](https://github.com/Panniantong/Agent-Reach) 1.6: CLI и скилл для поиска и чтения в интернете
  на этапе explore (свежая документация Liquibase, Spring, OpenSpec). Проверка: `agent-reach check --text`.
  Веб-поиск идёт через сервис Exa, чтение страниц через Jina Reader: запросы уходят третьим сторонам,
  поэтому ничего приватного в них не передаём. Каналы с логином копируют сессию из Chrome, их не подключаем.

## Рабочий цикл

```
задача
  └─ git switch -c feature/<имя> dev
       └─ /openspec-propose     агент пишет proposal, спеку, design, tasks
            └─ человек читает план и правит (самая дешёвая точка контроля, гл. 11)
                 └─ /openspec-apply-change   код + тесты по tasks.md
                      └─ ./mvnw verify
                           └─ code-review    субагент в чистом контексте, раунды до 0 blocker/major, схлопывание
                                └─ /openspec-archive-change   спека переезжает в openspec/specs/
                                     └─ git merge --no-ff в dev   (хук проверит отчёт ревью)
сдача лабы: тег labN на dev, преподавателю ссылка на тег
приёмка:     git merge --no-ff labN в master (в master только принятое)
```

## Зачем ревьюер в чистом контексте
Автор кода видит то, что хотел написать, а не то, что написал. Ревьюер получает только репозиторий
и отвечает на одни и те же вопросы: требования лабы, лишний код, тесты, README, история коммитов.
Отчёты лежат в `docs/reviews/`, по ним видно, что нашли и что исправили.

## Как ставить задачу агенту коротко
Контекст проекта агент берёт из `CLAUDE.md`, состояние из хука SessionStart, требования из OpenSpec.
Поэтому запрос содержит только то, чего в репозитории нет:

```
/openspec-propose лаба 2: сервис отчётов на той же БД, пока просто список студентов; доступ через шлюз.
Вне рамок: RestTemplate (лаба 3).
```

Длинный пересказ требований в запросе вреден: он дублирует `docs/labs-requirements.md` и может ему противоречить.

## Что сознательно не делали
- Stop-хук и автоматический прогон тестов после каждой правки: сборка Spring Boot идёт десятки секунд,
  а необратимых последствий у пропуска нет. Тесты гоняются по процедуре (гл. 13: критерий хука это обратимость).
- Свои скрипты-обёртки над Maven и Docker: агент вызывает `mvnw` и `docker compose` напрямую.
- Журнал сессий в репозитории: состояние восстанавливается из git, OpenSpec и хука SessionStart.
- Скилл для миграций Liquibase: формат виден по `001-create-students.sql`, правку применённых changeset
  блокирует `guard.js`, итоговую схему проверяет `MigrationIT` (гл. 10: агент узнаёт это из кода).
- Повторное ревью после схлопывания: правки по его итогам коммитятся до отчёта с решениями автора,
  и ревьюер их уже не перепроверяет. Хук видит только, что после коммита с отчётом код не менялся.

## Как убедиться, что обвязка работает
```powershell
git config core.hooksPath .githooks        # один раз после клонирования
git switch dev; git commit --allow-empty -m "test: x"            # отказ pre-commit
'{"tool_name":"Bash","tool_input":{"command":"docker compose down -v"}}' | node .claude/hooks/guard.js   # exit 2
'{"tool_name":"Bash","tool_input":{"command":"git commit --no-verify -m x"}}' | node .claude/hooks/guard.js   # exit 2
'{"tool_name":"PowerShell","tool_input":{"command":"Add-Content src/main/resources/db/changelog/001-create-students.sql x"}}' | node .claude/hooks/guard.js   # exit 2
'{"tool_name":"PowerShell","tool_input":{"command":"Get-Content src/main/resources/db/changelog/001-create-students.sql"}}' | node .claude/hooks/guard.js   # exit 0
```
Код выхода в PowerShell: `$LASTEXITCODE`.
