// Хук PreToolUse: Claude Code запускает его перед каждым вызовом Bash, PowerShell, Edit, Write, MultiEdit.
// На вход (stdin) приходит JSON вызова: { tool_name, tool_input: { command | file_path, ... } }.
// Ответ кодом выхода: 0 разрешает вызов, 2 запрещает. Текст из stderr агент получает как причину отказа
// и может исправить действие. Правила здесь про необратимые действия: их нельзя доверять тексту в CLAUDE.md.
const { execFileSync } = require('node:child_process');
const { readFileSync } = require('node:fs');
const path = require('node:path');

const call = JSON.parse(readFileSync(0, 'utf8'));
const projectDir = process.env.CLAUDE_PROJECT_DIR || call.cwd;

function deny(reason) {
  process.stderr.write(`Заблокировано хуком .claude/hooks/guard.js: ${reason}\n`);
  process.exit(2);
}

// Шаблоны команд привязаны к началу команды (после ; & | ( { или перевода строки), чтобы не срабатывать
// на тот же текст внутри grep, echo или commit -m. ARGS: аргументы до конца этой команды.
const START = String.raw`(?:^|[;&|({\n]\s*)(?:\w+=\S*\s+)*`;
const GIT = START + String.raw`git(?:\.exe)?(?:\s+-[cC]\s+\S+)*\s+`;
const ARGS = String.raw`[^;&|\n]*`;

// Опасные команды: [шаблон, причина и что делать вместо этого].
const DANGEROUS_COMMANDS = [
  [new RegExp(START + String.raw`docker(?:-compose|\s+compose)\b${ARGS}\sdown\b${ARGS}\s(?:-v|--volumes)\b`),
    'down -v удаляет volume с данными БД. Останови без -v: docker compose --profile app down'],
  [new RegExp(START + String.raw`docker\s+(?:volume\s+(?:rm|prune)|system\s+prune)\b`),
    'удаление volumes стирает данные БД. Попроси пользователя сделать это вручную'],
  [new RegExp(GIT + String.raw`push\b${ARGS}\s(?:--force\S*|-f|\+\S+)(?=\s|$)`),
    'force push переписывает общую историю. Делай новый коммит поверх'],
  [new RegExp(GIT + String.raw`reset\b${ARGS}\s--hard\b`),
    'reset --hard без возврата теряет незакоммиченную работу. Используй git stash'],
  [new RegExp(GIT + String.raw`clean\b${ARGS}\s(?:-\w*f\w*|--force)\b`),
    'git clean удаляет неотслеживаемые файлы без возврата'],
  // При fast-forward merge-коммита нет, и шлюз pre-merge-commit (отчёт ревью) не срабатывает.
  // merge.ff=false это закрывает, но --ff, --ff-only и -c merge.ff=true в команде сильнее настройки.
  [new RegExp(GIT + String.raw`merge(?![\w-])(?!${ARGS}\s--(?:no-ff|abort|continue|quit)\b)`),
    'git merge без --no-ff может пройти fast-forward мимо хука pre-merge-commit. Добавь --no-ff'],
  [/(?<![\w.-])\.env(?![\w.-])/,
    'в .env лежат локальные пароли, агент их не читает. Пример значений в .env.example'],
];

// На Windows у Claude Code есть отдельный инструмент PowerShell, команды в нём те же.
if (call.tool_name === 'Bash' || call.tool_name === 'PowerShell') {
  const command = call.tool_input.command || '';
  for (const [pattern, reason] of DANGEROUS_COMMANDS) {
    if (pattern.test(command)) deny(reason);
  }
  process.exit(0);
}

// Edit / Write / MultiEdit: changeset, который уже есть в ветке dev, менять нельзя.
// Liquibase хранит checksum каждого применённого changeset; после правки приложение не стартует
// с ошибкой ValidationFailedException на любой БД, где миграция уже прошла.
const relative = path.relative(projectDir, call.tool_input.file_path || '').split(path.sep).join('/');
const isChangeset = relative.startsWith('src/main/resources/db/changelog/')
  && !relative.endsWith('db.changelog-master.yaml');

if (isChangeset) {
  let existsInDev = true;
  try {
    execFileSync('git', ['cat-file', '-e', `dev:${relative}`], { cwd: projectDir, stdio: 'ignore' });
  } catch {
    existsInDev = false;
  }
  if (existsInDev) {
    deny(`${relative} уже влит в dev и мог быть применён. Создай новый changeset по образцу 001 и подключи его в db.changelog-master.yaml`);
  }
}
process.exit(0);
