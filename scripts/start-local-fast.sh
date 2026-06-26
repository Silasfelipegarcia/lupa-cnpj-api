#!/usr/bin/env bash
# Start rápido: empacota o JAR (se necessário) e sobe sem mvn spring-boot:run.
# Use quando o código já está compilado e você só quer reiniciar a API.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
JAR="target/lupa-insights-api-1.0.0.jar"

if [[ ! -f .env ]]; then
  echo "Arquivo .env não encontrado. Copie: cp .env.example .env" >&2
  exit 1
fi

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
fi

if lsof -ti :8080 >/dev/null 2>&1; then
  lsof -ti :8080 | xargs kill -9 2>/dev/null || true
  sleep 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ ! -f "$JAR" ]] || [[ -n "$(find src -type f -newer "$JAR" 2>/dev/null | head -1)" ]]; then
  echo "Empacotando (só quando o código mudou)..."
  mvn -Dmaven.test.skip=true -q -DskipTests package
fi

echo "API local → http://localhost:8080 (modo rápido)"
exec java -XX:TieredStopAtLevel=1 -jar "$JAR" --spring.profiles.active=local
