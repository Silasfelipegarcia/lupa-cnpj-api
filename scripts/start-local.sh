#!/usr/bin/env bash
# Sobe a API local com variáveis do .env (Mercado Pago, etc.)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Arquivo .env não encontrado. Copie: cp .env.example .env" >&2
  exit 1
fi

# Java 21 — evita travar com Java 24/26
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  echo "Java: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
else
  echo "Aviso: use Java 21 (export JAVA_HOME=\$(/usr/libexec/java_home -v 21))" >&2
fi

# MySQL
if ! mysqladmin -u "${DB_USERNAME:-root}" ${DB_PASSWORD:+-p"$DB_PASSWORD"} ping >/dev/null 2>&1; then
  echo "MySQL não está rodando. Inicie o MySQL antes da API." >&2
  exit 1
fi

# Libera porta 8080 se ficou presa de um start anterior
if lsof -ti :8080 >/dev/null 2>&1; then
  echo "Porta 8080 em uso — encerrando processo anterior..."
  lsof -ti :8080 | xargs kill -9 2>/dev/null || true
  sleep 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "API local → http://localhost:8080"
echo "Mercado Pago: ${MERCADOPAGO_PUBLIC_KEY:+configurado}${MERCADOPAGO_PUBLIC_KEY:-NÃO configurado}"
echo "Subindo (1ª vez ~30–60s; sem mudança no código fica mais rápido)..."

START=$(date +%s)

# -Dmaven.test.skip=true: não recompila testes a cada start (~5–10s a menos)
# -q: menos log do Maven
exec mvn -Dmaven.test.skip=true -q spring-boot:run \
  -Dspring-boot.run.jvmArguments="-XX:TieredStopAtLevel=1"
