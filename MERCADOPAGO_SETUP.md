# Mercado Pago — configuração Railway + webhook

## 1. Variáveis no Railway

Painel Railway → serviço da API → **Variables**. Cole de [`railway.env`](railway.env) (arquivo local gitignored):

| Variável | Descrição |
|----------|-----------|
| `MERCADOPAGO_PUBLIC_KEY` | `TEST-...` (primeiro teste) |
| `MERCADOPAGO_ACCESS_TOKEN` | `TEST-...` (primeiro teste) |
| `API_PUBLIC_URL` | `https://lupa-cnpj-api-production.up.railway.app` |
| `FRONTEND_URL` | URL do front na Vercel |
| `ALLOWED_ORIGINS` | `https://lupa-insights.vercel.app,http://localhost:4200,https://*.vercel.app` |

Após salvar, faça **Redeploy** do serviço.

> **URL atual da API:** `lupa-cnpj-api-production.up.railway.app` (domínio ativo no Railway).

## 2. Webhook no painel Mercado Pago

1. [developers.mercadopago.com](https://www.mercadopago.com.br/developers/panel/app) → sua aplicação
2. **Webhooks** → URL de produção:

```
https://lupa-cnpj-api-production.up.railway.app/payments/mercadopago/webhook
```

3. Eventos: **payment**
4. Use o mesmo ambiente das credenciais (TEST ou produção)

## 3. Validar

```bash
# API no ar
curl https://lupa-cnpj-api-production.up.railway.app/actuator/health

# Webhook acessível (retorna 200)
curl -X POST "https://lupa-cnpj-api-production.up.railway.app/payments/mercadopago/webhook"

# Front local → API Railway
cd ../frontend && npm run start:api-prod
```

Logado no app: **Conta → Cobrança** → cartão teste Visa `4235 6477 2802 5682`, CVV `123`, `11/30`.
