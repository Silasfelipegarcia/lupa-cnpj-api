# Runbook — incidente DDoS / abuso de tráfego

## Sintomas

- Latência alta ou timeouts na API (`503`, `504`)
- Taxa de `429 Too Many Requests` elevada nos logs
- CPU/memória no Railway próximo do limite
- Picos em `audit_logs` (especialmente `PRODUCT_EVENT` via `/analytics/event`)
- Conexões MySQL esgotadas

## Camadas de defesa (ordem)

1. **Edge (Cloudflare/WAF)** — bloqueio por país, ASN, challenge, rate limit global
2. **Railway** — escala horizontal temporária (se disponível)
3. **API** — `RateLimitFilter` por IP, `AuthenticatedRateLimitFilter` por usuário
4. **Tomcat** — max 50 conexões / 20 threads (backpressure)
5. **Redis** (opcional) — rate limit distribuído quando `REDIS_URL` configurado

## Ações imediatas (0–15 min)

1. Confirmar origem: IP único, botnet, ou tráfego legítimo (campanha/viral)?
2. Verificar logs por `requestId`, path mais atingido (`/analytics/event`, `/auth/login`, `/cnpj/import/historico`)
3. Se ataque a endpoint público:
   - Ativar regra WAF/Cloudflare para o path
   - Reduzir temporariamente limites em `app.security.*-per-minute` via variáveis de ambiente
4. Se IP(s) identificados: bloquear no WAF (não confiar só em block na app)
5. Se abuso autenticado: desabilitar conta (`enabled=false`) no admin

## Variáveis úteis (Railway)

| Variável | Efeito |
|----------|--------|
| `REDIS_URL` | Rate limit compartilhado entre instâncias |
| `ALLOWED_ORIGINS` | Restringe CORS em produção |
| `JWT_SECRET` | Obrigatório em prod (startup falha se default) |
| `MERCADOPAGO_WEBHOOK_SECRET` | Obrigatório se MP configurado |

## Endpoints sensíveis

| Path | Limite default | Risco |
|------|----------------|-------|
| `POST /auth/login` | 10/min/IP | Credential stuffing |
| `POST /cnpj/import` | 5/h/IP | CPU + fila |
| `POST /analytics/event` | 60/min/IP | Amplificação de storage |
| `GET /cnpj/import/historico` | 30/min/IP | Read amplification |
| `GET /cnpj/preview` | 10/min/IP | PII + custo externo |

## Pós-incidente

1. Revisar `audit_logs` no período (retenção 90 dias)
2. Ajustar limites permanentes se necessário
3. Considerar Cloudflare Pro/Business se ataques recorrentes
4. Rotacionar `JWT_SECRET` apenas se houver comprometimento (invalida todas as sessões)

## Contatos / escalação

- Railway dashboard: métricas de CPU, memória, rede
- Mercado Pago: desabilitar webhook temporariamente se fraude de pagamento detectada
- Vercel: verificar se preview domains estão bloqueados (`ALLOWED_ORIGINS` sem `*.vercel.app` em prod)
