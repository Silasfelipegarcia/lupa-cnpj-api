# LupaCNPJ API

API Spring Boot para enriquecer listas de CNPJs consultando a [API pública CNPJ.ws](https://publica.cnpj.ws).

Parte do projeto **LupaCNPJ** — frontend em [lupa-cnpj](https://github.com/Silasfelipegarcia/lupa-cnpj).

## Stack

- Java 21
- Spring Boot 3.3
- MySQL + Flyway
- JWT (Spring Security)

## Executar localmente

Com **MySQL** rodando na máquina (porta 3306):

```bash
# opcional — mesmas variáveis usadas no Hosty
export DB_USERNAME=root
export DB_PASSWORD=sua_senha

mvn spring-boot:run
```

O profile `local` é ativado por padrão. O Flyway cria o banco `lupacnpj` e as tabelas automaticamente.

API em `http://localhost:8080`.

## Variáveis de ambiente

### Local (`profile local`)

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_URL` | URL JDBC MySQL | `jdbc:mysql://127.0.0.1:3306/lupacnpj?...` |
| `DB_USERNAME` | Usuário do banco | `root` |
| `DB_PASSWORD` | Senha do banco | vazio |
| `FLYWAY_ENABLED` | Habilita migrações | `true` |
| `JWT_SECRET` | Segredo HS256 (dev) | valor de desenvolvimento |

### Produção (`profile production`)

| Variável | Descrição |
|----------|-----------|
| `PORT` | Porta (Railway define automaticamente) |
| `JWT_SECRET` | Segredo HS256 forte (obrigatório) |
| `ALLOWED_ORIGINS` | CORS (ex.: `https://lupa-cnpj.vercel.app,http://localhost:4200`) |
| `CNPJ_WS_TOKEN` | Token CNPJ.ws comercial (opcional) |

**Banco de dados** — duas opções (igual ao Hosty):

1. Padrão interno:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`

2. Plugin **MySQL** do Railway:
   - `MYSQL_URL`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

## Deploy no Railway

1. Crie um projeto em [railway.app](https://railway.app)
2. **New → GitHub Repo** → selecione `lupa-cnpj-api`
3. **Add MySQL** no mesmo projeto
4. No serviço da API, configure:
   - `SPRING_PROFILES_ACTIVE=production` (já definido no Dockerfile)
   - `JWT_SECRET` — valor aleatório forte (32+ caracteres)
   - `ALLOWED_ORIGINS` = `https://lupa-cnpj.vercel.app,http://localhost:4200`
   - Variáveis do MySQL (`MYSQL_URL`, `MYSQLUSER`, `MYSQLPASSWORD`) ou `DB_*`
5. Railway detecta o `Dockerfile` e faz o deploy automaticamente

## Endpoints

### Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Cadastro |
| POST | `/auth/login` | Login → `{ token, user }` |
| GET | `/auth/me` | Perfil do usuário logado |

### CNPJ (requer JWT)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/cnpj/import` | Upload CSV/Excel |
| GET | `/cnpj/import/ativo` | Consulta em andamento do usuário |
| GET | `/cnpj/import/historico` | Histórico de consultas |
| GET | `/cnpj/import/{jobId}/status` | Progresso |
| GET | `/cnpj/import/{jobId}/download` | Download CSV |
| DELETE | `/cnpj/import/{jobId}` | Cancelar |
| GET | `/health` | Health check |
