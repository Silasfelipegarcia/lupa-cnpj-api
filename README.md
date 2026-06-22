# LupaCNPJ API

API Spring Boot para enriquecer listas de CNPJs consultando a [API pública CNPJ.ws](https://publica.cnpj.ws).

Parte do projeto **LupaCNPJ** — frontend em [lupa-cnpj](https://github.com/Silasfelipegarcia/lupa-cnpj).

## Stack

- Java 21
- Spring Boot 3.3
- PostgreSQL + Flyway
- JWT (Spring Security)

## Executar localmente

Suba o Postgres e rode a API:

```bash
docker compose up -d
mvn spring-boot:run
```

API em `http://localhost:8080`.

## Variáveis de ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `PORT` | Porta do servidor (Railway define automaticamente) | `8080` |
| `DATABASE_URL` | URL Postgres (`postgresql://...`) — Railway injeta ao adicionar Postgres | H2 local |
| `JWT_SECRET` | Segredo HS256 (mín. 32 caracteres aleatórios) | dev local |
| `ALLOWED_ORIGINS` | Origens CORS separadas por vírgula | `http://localhost:4200,https://lupa-cnpj.vercel.app` |
| `CNPJ_WS_TOKEN` | Token para busca por razão social (opcional) | — |

## Deploy no Railway

1. Crie um projeto em [railway.app](https://railway.app)
2. **New → GitHub Repo** → selecione `lupa-cnpj-api`
3. **Add PostgreSQL** no mesmo projeto
4. No serviço da API, configure as variáveis:
   - `DATABASE_URL` — referência ao Postgres (`${{Postgres.DATABASE_URL}}`)
   - `JWT_SECRET` — gere um valor aleatório forte (32+ bytes)
   - `ALLOWED_ORIGINS` = `https://lupa-cnpj.vercel.app,http://localhost:4200`
   - `CNPJ_WS_TOKEN` = (opcional) token CNPJ.ws comercial
5. Railway detecta o `Dockerfile` e faz o deploy automaticamente
6. Copie a URL pública (ex: `https://lupa-cnpj-api-production.up.railway.app`) e configure no frontend Vercel

## Endpoints

### Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Cadastro (`nome`, `email`, `cpf`, `password`) |
| POST | `/auth/login` | Login → `{ token, user }` |
| GET | `/auth/me` | Perfil do usuário logado (Bearer JWT) |

### CNPJ (requer JWT)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/cnpj/import` | Upload CSV/Excel → retorna `jobId` |
| GET | `/cnpj/import/{jobId}/status` | Progresso + resultados parciais |
| GET | `/cnpj/import/{jobId}/download` | Download do CSV final |
| DELETE | `/cnpj/import/{jobId}` | Cancelar consulta |
| GET | `/cnpj/import/historico` | Últimas consultas do usuário |
| GET | `/cnpj/import/historico/{jobId}` | Detalhe de consulta anterior |
| GET | `/cnpj/config` | Configuração da API |
| GET | `/cnpj/template` | Modelo Excel |
| GET | `/health` | Health check |

## CSV de entrada

Colunas `cnpj` e/ou `razao_social` (pelo menos uma por linha).

Exemplos em `exemplos/`.
