#!/usr/bin/env bash
# Aplica V8__test_users_seed.sql no MySQL (produção ou local).
# Produção: export MYSQL_URL='mysql://...' && ./scripts/seed-test-users.sh
# Local:    ./scripts/seed-test-users.sh --local

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/../src/main/resources/db/migration/V8__test_users_seed.sql"
MODE="${1:-}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "Arquivo não encontrado: $SQL_FILE" >&2
  exit 1
fi

if [[ "$MODE" == "--local" ]]; then
  DB_NAME="${DB_NAME:-lupainsights}"
  DB_USER="${DB_USERNAME:-root}"
  DB_PASS="${DB_PASSWORD:-}"
  echo "Aplicando seed em MySQL local ($DB_NAME)..."
  mysql -u "$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME" < "$SQL_FILE"
else
  if [[ -z "${MYSQL_URL:-}" ]]; then
    echo "Defina MYSQL_URL ou use --local." >&2
    exit 1
  fi
  REST="${MYSQL_URL#mysql://}"
  USER_PASS="${REST%%@*}"
  HOST_DB="${REST#*@}"
  DB_USER="${USER_PASS%%:*}"
  DB_PASS="${USER_PASS#*:}"
  HOST_PORT="${HOST_DB%%/*}"
  DB_NAME="${HOST_DB#*/}"
  DB_HOST="${HOST_PORT%%:*}"
  DB_PORT="${HOST_PORT##*:}"
  [[ "$DB_PORT" == "$DB_HOST" ]] && DB_PORT=3306
  echo "Aplicando seed em Railway MySQL..."
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" --ssl-mode=REQUIRED < "$SQL_FILE"
fi

echo ""
echo "Usuários criados/atualizados. Ver docs/USUARIOS-TESTE.md"
if [[ "$MODE" != "--local" ]]; then
  echo ""
  echo "AVISO: em produção use a migration V13 (deploy Flyway), não este script." >&2
  echo "       Este script reaplica a senha antiga do V8 (Lupa@Test2026)." >&2
fi
mysql_query() {
  if [[ "$MODE" == "--local" ]]; then
    mysql -u "${DB_USERNAME:-root}" ${DB_PASSWORD:+-p"$DB_PASSWORD"} "${DB_NAME:-lupainsights}" -e "$1"
  else
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" --ssl-mode=REQUIRED -e "$1"
  fi
}

mysql_query "SELECT nome, email, role, plan, cpf FROM users WHERE email LIKE '%@lupainsights.com.br' OR cpf = '40173586830' ORDER BY role DESC, plan;"
