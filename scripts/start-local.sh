#!/usr/bin/env bash
# Sobe a API local com variáveis do .env (Mercado Pago, etc.)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Arquivo .env não encontrado. Copie: cp .env.example .env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "API local → http://localhost:8080"
echo "Mercado Pago: ${MERCADOPAGO_PUBLIC_KEY:+configurado}${MERCADOPAGO_PUBLIC_KEY:-NÃO configurado}"
exec mvn spring-boot:run
