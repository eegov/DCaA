#!/usr/bin/env bash
# Хук SessionStart. Всё, что скрипт печатает, Claude Code добавляет в контекст в начале сессии.
# Зачем: агент сразу видит ветку, незакоммиченные правки и активные OpenSpec-изменения
# и не работает по устаревшей картине. Раньше для этого была строка в правилах, которую агент пропускал.
cd "$CLAUDE_PROJECT_DIR" || exit 0

branch=$(git branch --show-current)
echo "Текущая ветка: $branch"
if [ "$branch" = "master" ] || [ "$branch" = "dev" ]; then
  echo "ВНИМАНИЕ: перед правками создай ветку от dev: git switch -c feature/<имя> dev"
fi

echo "Незакоммиченные изменения:"
git status --short | head -n 20

echo "Активные OpenSpec-изменения:"
openspec list 2>/dev/null || echo "(openspec CLI не найден: npm i -g @fission-ai/openspec@1.16.0)"
exit 0
