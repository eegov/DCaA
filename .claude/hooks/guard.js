// Хук PreToolUse: Claude Code запускает его перед каждым вызовом Bash, PowerShell, Edit, Write, MultiEdit.
// На вход (stdin) приходит JSON вызова: { tool_name, tool_input: { command | file_path, ... } }.
// Ответ кодом выхода: 0 разрешает вызов, 2 запрещает. Текст из stderr агент получает как причину отказа
// и может исправить действие. Правила здесь про необратимые действия: их нельзя доверять тексту в CLAUDE.md.
const { execFileSync } = require('node:child_process');
const { readFileSync } = require('node:fs');
const path = require('node:path');

// Ручная проверка из docs/harness.md: если в PowerShell 5.1 $OutputEncoding равен UTF-8, строка приходит с BOM.
const call = JSON.parse(readFileSync(0, 'utf8').replace(/^\uFEFF/, ''));
const projectDir = process.env.CLAUDE_PROJECT_DIR || call.cwd;
const CHANGELOG_DIR = 'src/main/resources/db/changelog/';
const CHANGELOG_MASTER = 'db.changelog-master.yaml';

function deny(reason) {
  process.stderr.write(`Заблокировано хуком .claude/hooks/guard.js: ${reason}\n`);
  process.exit(2);
}

// Changeset-файлы, которые уже есть в dev: Liquibase хранит checksum каждого применённого changeset,
// после правки приложение не стартует с ValidationFailedException на любой БД, где миграция уже прошла.
function changesetsInDev() {
  try {
    return execFileSync('git', ['ls-tree', '-r', '--name-only', 'dev', '--', CHANGELOG_DIR],
      { cwd: projectDir, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] })
      .split('\n').filter((f) => f && !f.endsWith(CHANGELOG_MASTER));
  } catch {
    return [];
  }
}

// Шаблоны команд привязаны к началу команды (после ; & | ( { или перевода строки), чтобы не срабатывать
// на тот же текст внутри grep, echo или commit -m. ARGS: аргументы до конца этой команды.
const START = String.raw`(?:^|[;&|({\n]\s*)(?:\w+=\S*\s+)*`;
const GIT = START + String.raw`git(?:\.exe)?(?:\s+-[cC]\s+\S+)*\s+`;
const ARGS = String.raw`[^;&|\n]*`;
const HOOK_SETTINGS = String.raw`(?:core\.hookspath|merge\.ff|branch\.[^\s=]+?\.mergeoptions)`;

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
  [new RegExp(GIT + String.raw`merge\b${ARGS}\s--ff[\w-]*(?=\s|$)`),
    '--ff и --ff-only сильнее --no-ff, если стоят после него, и merge пройдёт fast-forward мимо pre-merge-commit. Оставь только --no-ff'],
  // Git-хуки (.githooks/) проверяют ветку, формат коммита и отчёт ревью. Их отключение снимает все проверки сразу.
  // git принимает сокращение опции, поэтому ловим и --no-veri, и --no-v.
  [new RegExp(GIT + String.raw`[\w-]+\b${ARGS}\s--no-v(?:e(?:r(?:i(?:f(?:y)?)?)?)?)?(?=\s|$)`),
    '--no-verify пропускает git-хуки проекта. Прочитай сообщение хука и исправь причину отказа'],
  [/\bGIT_CONFIG_(?:COUNT|KEY_\d+|VALUE_\d+|PARAMETERS)\s*=/i,
    'переменные GIT_CONFIG_* задают настройки git в обход git config и могут отключить хуки проекта. Запусти команду без них'],
  [new RegExp(GIT + String.raw`commit\b${ARGS}\s-[a-zA-Z]*n[a-zA-Z]*(?=\s|$)`),
    'git commit -n пропускает git-хуки проекта. Прочитай сообщение хука и исправь причину отказа'],
  [new RegExp(START + String.raw`git(?:\.exe)?\s+(?:-[cC]\s+\S+\s+)*-c\s+${HOOK_SETTINGS}=`, 'i'),
    '-c core.hooksPath, merge.ff или branch.<имя>.mergeoptions в команде отключает git-хуки проекта или включает fast-forward мимо них. Запусти команду без -c'],
  // Чтение (git config core.hooksPath, git config --get merge.ff) проходит, запись и удаление нет.
  // branch.<имя>.mergeoptions с --ff включил бы fast-forward для merge в эту ветку.
  [new RegExp(GIT + String.raw`config\b(?=${ARGS}\b${HOOK_SETTINGS}\b)(?=${ARGS}(?:\s--(?:unset|unset-all|add|replace-all|edit|remove-section|rename-section)\b|\s(?:set|unset|edit)\s|\b${HOOK_SETTINGS}\s+[^\s;&|]))`, 'i'),
    'core.hooksPath, merge.ff и branch.<имя>.mergeoptions определяют, сработают ли git-хуки проекта (docs/harness.md), агент их не меняет. Если настройка неверна, скажи пользователю'],
];

// .env: блокируется любое упоминание файла в команде, кроме .env.example (запрет по умолчанию:
// читателей слишком много, от cat до node -e).
const ENV_FILE = /(?<![\w.-])\.env(?![\w.-])/;

// Сообщение -m/--message у git commit, tag и merge ничего не выполняет, поэтому правила его не видят.
// Срезаются только формы без подстановки команд: '...', "..." без $( ` и \ (в "$(cat .env)" оболочка
// выполнила бы чтение) и heredoc с разделителем в кавычках в "$(cat <<'EOF' ... EOF)", так коммитит Claude Code.
const GIT_MESSAGE = new RegExp(String.raw`(${GIT}(?:commit|tag|merge)\b[^;&|\n]*?)\s(?:-m|--message)(?:\s+|=)`
  + String.raw`(?:'[^']*'|"(?:[^"$\x60\\]|\$(?!\())*"|"\$\(cat\s+<<-?\s*'(\w+)'[ \t]*\n[\s\S]*?\n[ \t]*\2[ \t]*\n\s*\)")`);

function withoutGitMessages(command) {
  let previous;
  do {
    previous = command;
    command = command.replace(GIT_MESSAGE, '$1 ');
  } while (command !== previous);
  return command;
}

const HARMLESS_REDIRECT = /\d?>>?\s*(?:&\d|\/dev\/null|\$null|nul\b)/gi;

// Влитые миграции через оболочку. Часть команды (между ; && || & и переводом строки) «касается» их,
// если упоминает имя влитого changeset, каталог changelog целиком или глоб в нём, а после cd в changelog
// ещё и глоб. Такая часть и все следующие за ней звенья конвейера | (в PowerShell по конвейеру идут
// сами файлы: Get-ChildItem | Remove-Item) должны начинаться с команды, которая не меняет файлы,
// и не содержать перенаправления, подстановки $( ) или --output. Запрет по умолчанию: способов записать
// файл (sed -i, tee, Set-Content, Out-File, mv, cp, python -c...) больше, чем способов его прочитать.
// Цена: sed -n, awk и bash -c с таким путём тоже не пройдут. Новый changeset ветки (его нет в dev) не касается.
const KEEPS_FILES = new Set(['cat', 'type', 'get-content', 'gc', 'head', 'tail', 'less', 'more', 'grep', 'egrep',
  'fgrep', 'rg', 'findstr', 'select-string', 'sls', 'wc', 'diff', 'ls', 'dir', 'get-childitem', 'gci',
  'get-item', 'test-path', 'find', 'sort', 'sort-object', 'uniq', 'cut', 'nl', 'select-object', 'where-object',
  'measure-object', 'format-table', 'format-list', 'out-string', 'cd', 'set-location', 'sl', 'pushd', 'popd',
  'echo', 'write-output']);
const GIT_KEEPS_FILES = new Set(['diff', 'log', 'show', 'blame', 'status', 'grep', 'ls-files', 'ls-tree',
  'cat-file', 'add', 'commit']);
const CHANGELOG_WHOLE = /db[\\/]+changelog(?:[\\/]+(?:[^\s'"]*[*?[][^\s'"]*)?)?(?=$|[\s'")])/i;
const CHANGELOG_ANY = /db[\\/]+changelog/i;
const GLOB = /(?:^|\s)(?!-)\S*[*?[]/;

function words(part) {
  return part.trim().replace(/^[({!\s]+/, '').replace(/^(?:\w+=\S*\s+)+/, '').split(/\s+/);
}

function keepsFiles(part) {
  if (/>|\$\(|\x60|--output\b/.test(part)) return false;
  const w = words(part);
  const program = w[0].toLowerCase().replace(/\.exe$/, '');
  if (program === '') return true;
  if (program === 'find') return !/\s-(?:delete|exec\w*|ok\w*|fprint\w*|fls)\b/.test(part);
  if (program !== 'git') return KEEPS_FILES.has(program);
  let i = 1;
  while (/^-[cC]$/.test(w[i])) i += 2;
  // Возврат файла к закоммиченному виду ничего не ломает, взять его из другой ветки нельзя.
  if (w[i] === 'restore') return !/\s(?:-s|--source)\b/.test(part);
  if (w[i] === 'checkout') return w[i + 1] === '--' || (w[i + 1] === 'HEAD' && w[i + 2] === '--');
  return GIT_KEEPS_FILES.has(w[i]);
}

function changesNamedChangesets(command, mergedNames) {
  let inChangelog = false;
  for (const chain of command.split(/&&|\|\||[;&\n]/)) {
    const parts = chain.split('|');
    const first = parts.findIndex((part) => mergedNames.some((n) => part.includes(n))
      || CHANGELOG_WHOLE.test(part) || (inChangelog && GLOB.test(part)));
    if (first !== -1 && !parts.slice(first).every(keepsFiles)) return true;
    const w = words(parts[0]);
    if (/^(?:cd|set-location|sl|pushd)$/i.test(w[0]) && CHANGELOG_ANY.test(parts[0])) inChangelog = true;
  }
  return false;
}

// На Windows у Claude Code есть отдельный инструмент PowerShell, команды в нём те же.
if (call.tool_name === 'Bash' || call.tool_name === 'PowerShell') {
  // Сообщение коммита и перенаправления в никуда (2>&1, >/dev/null, 2>$null) ничего не выполняют и не пишут.
  const command = withoutGitMessages(call.tool_input.command || '').replace(HARMLESS_REDIRECT, ' ');
  for (const [pattern, reason] of DANGEROUS_COMMANDS) {
    if (pattern.test(command)) deny(reason);
  }
  if (ENV_FILE.test(command)) {
    deny('в .env лежат локальные пароли, агент их не читает. Пример значений в .env.example');
  }
  // Имя без каталога ловит запись по короткому имени после cd. git вызывается, только если команда
  // похожа на работу с changelog.
  if (CHANGELOG_ANY.test(command) || /\.(?:sql|ya?ml|xml|json)\b/i.test(command)) {
    const mergedNames = changesetsInDev().map((f) => path.posix.basename(f));
    if (changesNamedChangesets(command, mergedNames)) {
      deny('команда меняет файлы и упоминает влитый changeset, каталог changelog целиком или глоб в нём. '
        + 'Checksum влитого changeset уже записан в БД, после правки приложение не стартует. '
        + 'Читать можно: cat, Get-Content, grep, ls, find, git diff/log/show; вернуть файл к закоммиченному: git restore. '
        + 'Новый changeset создай инструментом Write по образцу 001 и подключи его в db.changelog-master.yaml инструментом Edit');
    }
  }
  process.exit(0);
}

// Edit / Write / MultiEdit: changeset, который уже есть в ветке dev, менять нельзя.
const relative = path.relative(projectDir, call.tool_input.file_path || '').split(path.sep).join('/');
if (changesetsInDev().includes(relative)) {
  deny(`${relative} уже влит в dev и мог быть применён. Создай новый changeset по образцу 001 и подключи его в db.changelog-master.yaml`);
}
process.exit(0);
