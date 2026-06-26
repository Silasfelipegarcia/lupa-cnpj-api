# Mercado Pago — referência rápida

## Local (desenvolvimento)

Credenciais **TEST-** no `backend/.env`. Guia completo:

→ **[docs/GUIA-TESTES-LOCAL.md](../docs/GUIA-TESTES-LOCAL.md)** (cartões de teste, checklist de pagamentos)

```bash
cd backend
./scripts/start-local.sh
```

## Produção (Railway + Vercel)

Credenciais **APP_USR-**, webhook, variáveis Railway:

→ **[docs/CONFIGURACAO-PRODUCAO.md](../docs/CONFIGURACAO-PRODUCAO.md)**

Template de variáveis: [`railway.env.example`](railway.env.example) → cole no painel Railway → **Redeploy**.

### Webhook (produção)

```
https://lupa-cnpj-api-production.up.railway.app/payments/mercadopago/webhook
```

Evento: **payment**

Configure `MERCADOPAGO_WEBHOOK_SECRET` no Railway (secret do painel MP → Webhooks).

### Validar

```bash
curl https://lupa-cnpj-api-production.up.railway.app/actuator/health
```

Logado: `GET /payments/config` → `configured: true`
