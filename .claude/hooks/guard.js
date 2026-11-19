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

// Опасные команды: [шаблон, причина и что делать вместо этого].
const DANGEROUS_COMMANDS = [
  [/\bdocker(-compose|\s+compose)\b.*\bdown\b.*\s(-v|--volumes)\b/,
    'down -v удаляет volume с данными БД. Останови без -v: docker compose --profile app down'],
  [/\bdocker\s+(volume\s+(rm|prune)|system\s+prune)\b/,
    'удаление volumes стирает данные БД. Попроси пользователя сделать это вручную'],
  [/\bgit\s+push\b.*\s(--force|-f)\b/,
    'force push переписывает общую историю. Делай новый коммит поверх'],
  [/\bgit\s+reset\s+--hard\b/,
    'reset --hard без возврата теряет незакоммиченную работу. Используй git stash'],
  [/\bgit\s+clean\s+-\w*f/,
    'git clean удаляет неотслеживаемые файлы без возврата'],
  // При fast-forward merge-коммита нет, и шлюз pre-merge-commit (отчёт ревью) не срабатывает.
  // merge.ff=false это закрывает, но --ff и --ff-only в команде сильнее настройки.
  [/\bgit\s+merge(?![\w-])(?![^;&|\n]*\s--(no-ff|abort|continue|quit)\b)/,
    'git merge без --no-ff может пройти fast-forward мимо хука pre-merge-commit. Добавь --no-ff'],
  [/(?<![\w.\/-])\.env(?![\w.-])/,
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
