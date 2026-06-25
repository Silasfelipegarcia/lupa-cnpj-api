# Lupa Insights — API

API Spring Boot para enriquecer listas de CNPJs consultando a [API pública CNPJ.ws](https://publica.cnpj.ws).

Frontend em [lupa-insights](https://github.com/Silasfelipegarcia/lupa-insights) · Documentação completa em [docs/DOCUMENTACAO.md](../docs/DOCUMENTACAO.md).

**Produção:** https://lupa-cnpj-api-production.up.railway.app

## Stack

- Java 21
- Spring Boot 3.3
- MySQL 8 + Flyway
- JWT (Spring Security + BCrypt)

## Executar localmente

Com **MySQL** na porta 3306:

```bash
export DB_USERNAME=root
export DB_PASSWORD=sua_senha   # se necessário

mvn spring-boot:run
```

Profile `local` (padrão). Flyway cria o banco `lupainsights` e as tabelas.

API em `http://localhost:8080`.

## Variáveis de ambiente

### Local (`profile local`)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/lupainsights?...` | URL JDBC |
| `DB_USERNAME` | `root` | Usuário |
| `DB_PASSWORD` | vazio | Senha |
| `FLYWAY_ENABLED` | `true` | Migrações |
| `JWT_SECRET` | dev | Segredo HS256 |
| `ALLOWED_ORIGINS` | localhost + vercel | CORS |
| `MERCADOPAGO_PUBLIC_KEY` | — | Public Key (TEST local / APP_USR produção) |
| `MERCADOPAGO_ACCESS_TOKEN` | — | Access Token (TEST local / APP_USR produção) |
| `FRONTEND_URL` | `http://localhost:4200` | Redirects do checkout |
| `API_PUBLIC_URL` | `http://localhost:8080` | Webhook MP |

Veja `.env.example` — copie para `.env` e use credenciais de **teste** do painel Mercado Pago.

Para o **Railway**, use `railway.env.example` → `railway.env` (gitignored) como referência ao colar variáveis no painel.

### Produção (`profile production`)

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `JWT_SECRET` | **Sim** | Segredo forte (32+ caracteres) |
| `ALLOWED_ORIGINS` | Recomendado | `https://lupa-insights.vercel.app,http://localhost:4200` |
| `PORT` | Auto | Railway define automaticamente |
| `CNPJ_WS_TOKEN` | Não | Token API comercial |
| `MERCADOPAGO_PUBLIC_KEY` | Sim (pagamentos) | `TEST-...` (teste) ou `APP_USR-...` (produção) |
| `MERCADOPAGO_ACCESS_TOKEN` | Sim (pagamentos) | `TEST-...` (teste) ou `APP_USR-...` (produção) |
| `FRONTEND_URL` | Sim (pagamentos) | URL do frontend |
| `API_PUBLIC_URL` | Sim (pagamentos) | URL pública da API (webhook) |

Webhook no painel MP: `{API_PUBLIC_URL}/payments/mercadopago/webhook` — ver [MERCADOPAGO_SETUP.md](MERCADOPAGO_SETUP.md).

**Banco — opção 1 (JDBC):**

```
DB_URL=jdbc:mysql://host:porta/railway?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=...
DB_PASSWORD=...
```

**Banco — opção 2 (plugin MySQL Railway — recomendado):**

Vincule o MySQL ao serviço da API. Variáveis injetadas automaticamente:

- `MYSQL_URL` — formato `mysql://...` (convertido para JDBC por `DatabaseConfig`)
- `MYSQLUSER`
- `MYSQLPASSWORD`

> **Importante:** não use `MYSQL_URL` direto em `DB_URL`. A classe `DatabaseConfig` faz a conversão `mysql://` → `jdbc:mysql://`.

## Deploy no Railway

1. Projeto em [railway.app](https://railway.app) → repo `lupa-insights-api`
2. **Add MySQL** e vincule à API
3. Configure `JWT_SECRET` e `ALLOWED_ORIGINS`
4. Dockerfile ativa `production` automaticamente
5. Valide: `GET /actuator/health` → `{"status":"UP"}`

## Endpoints

### Autenticação

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/auth/register` | Não | Cadastro → 201 + token |
| POST | `/auth/login` | Não | Login |
| GET | `/auth/me` | Sim | Perfil |

### CNPJ (requer JWT)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/cnpj/config` | Configuração |
| GET | `/cnpj/template` | Modelo Excel |
| POST | `/cnpj/import` | Upload planilha → job |
| GET | `/cnpj/import/ativo` | Job ativo (204 se nenhum) |
| GET | `/cnpj/import/historico` | Histórico (50 últimos) |
| GET | `/cnpj/import/historico/{jobId}` | Detalhe histórico |
| GET | `/cnpj/import/{jobId}/status` | Progresso |
| DELETE | `/cnpj/import/{jobId}` | Cancelar |
| GET | `/cnpj/import/{jobId}/download` | Download CSV |

### Saúde

| Método | Rota |
|--------|------|
| GET | `/actuator/health` |
| GET | `/health` |

## Status do job

`NA_FILA` → `PROCESSANDO` → `CONCLUIDO` | `ERRO` | `CANCELADO`

## Limites padrão

| Limite | Valor |
|--------|-------|
| Arquivo | 5 MB |
| Linhas/arquivo | 900 |
| Jobs ativos/usuário | 1 |
| Auth | 10 req/min por IP |
| Import | 5 req/h por IP |
| CNPJ.ws (pública) | 3 req/min |

## Banco de dados (Flyway)

| Migração | Tabelas |
|----------|---------|
| `V1__users.sql` | `users` |
| `V2__import_jobs.sql` | `import_jobs`, `import_results` |

## Exemplos

- `exemplos/entrada.csv`
- `exemplos/saida_exemplo.csv`
