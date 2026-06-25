#!/usr/bin/env bash
# Consulta e-mail e redefine senha de um usuário pelo CPF.
# Uso (produção Railway):
#   export MYSQL_URL='mysql://user:pass@host:port/railway'
#   ./scripts/manage-user-by-cpf.sh 40173586830 'Felipe@231125'
#
# Uso (MySQL local):
#   ./scripts/manage-user-by-cpf.sh 40173586830 'Felipe@231125' --local

set -euo pipefail

CPF="${1:?Informe o CPF (11 dígitos)}"
PASSWORD="${2:?Informe a nova senha}"
MODE="${3:-}"

CPF="$(echo "$CPF" | tr -cd '0-9')"
if [[ ${#CPF} -ne 11 ]]; then
  echo "CPF inválido: precisa ter 11 dígitos." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

HASH="$(
  cd "$BACKEND_DIR" && mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp-manage-user.txt >/dev/null 2>&1
  javac -cp "$(cat /tmp/cp-manage-user.txt)" -d /tmp "$SCRIPT_DIR/BcryptHashOnce.java" >/dev/null 2>&1
  java -cp "/tmp:$(cat /tmp/cp-manage-user.txt)" BcryptHashOnce "$PASSWORD"
)"

if [[ "$MODE" == "--local" ]]; then
  DB_HOST="${DB_HOST:-127.0.0.1}"
  DB_PORT="${DB_PORT:-3306}"
  DB_NAME="${DB_NAME:-lupainsights}"
  DB_USER="${DB_USERNAME:-root}"
  DB_PASS="${DB_PASSWORD:-}"

  echo "Consultando usuário no MySQL local ($DB_NAME)..."
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME" -e \
    "SELECT id, nome, email, cpf FROM users WHERE cpf = '$CPF';"

  AFFECTED="$(mysql -N -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME" -e \
    "UPDATE users SET password_hash = '$HASH' WHERE cpf = '$CPF'; SELECT ROW_COUNT();")"

  if [[ "$AFFECTED" == "0" ]]; then
    echo "Nenhum usuário encontrado com CPF $CPF no banco local." >&2
    exit 1
  fi

  echo "Senha atualizada no banco local."
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME" -e \
    "SELECT nome, email, cpf FROM users WHERE cpf = '$CPF';"
  exit 0
fi

if [[ -z "${MYSQL_URL:-}" ]]; then
  echo "Defina MYSQL_URL (Railway → MySQL → Variables) ou use --local." >&2
  exit 1
fi

# mysql://user:pass@host:port/db
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

echo "Consultando usuário no Railway MySQL..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" --ssl-mode=REQUIRED -e \
  "SELECT id, nome, email, cpf FROM users WHERE cpf = '$CPF';"

AFFECTED="$(mysql -N -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" --ssl-mode=REQUIRED -e \
  "UPDATE users SET password_hash = '$HASH' WHERE cpf = '$CPF'; SELECT ROW_COUNT();")"

if [[ "$AFFECTED" == "0" ]]; then
  echo "Nenhum usuário encontrado com CPF $CPF." >&2
  exit 1
fi

echo "Senha atualizada."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" --ssl-mode=REQUIRED -e \
  "SELECT nome, email, cpf FROM users WHERE cpf = '$CPF';"
