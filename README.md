# LupaCNPJ API

API Spring Boot para enriquecer listas de CNPJs consultando a [API pública CNPJ.ws](https://publica.cnpj.ws).

Parte do projeto **LupaCNPJ** — frontend em [lupa-cnpj](https://github.com/Silasfelipegarcia/lupa-cnpj).

## Stack

- Java 21
- Spring Boot 3.3
- Maven

## Executar localmente

```bash
mvn spring-boot:run
```

API em `http://localhost:8080`.

## Variáveis de ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `PORT` | Porta do servidor (Railway define automaticamente) | `8080` |
| `ALLOWED_ORIGINS` | Origens CORS separadas por vírgula | `http://localhost:4200` |
| `CNPJ_WS_TOKEN` | Token para busca por razão social (opcional) | — |

## Deploy no Railway

1. Crie um projeto em [railway.app](https://railway.app)
2. **New → GitHub Repo** → selecione `lupa-cnpj-api`
3. Configure as variáveis:
   - `ALLOWED_ORIGINS` = URL do frontend Vercel (ex: `https://lupa-cnpj.vercel.app`)
   - `CNPJ_WS_TOKEN` = (opcional) token CNPJ.ws comercial
4. Railway detecta o `Dockerfile` e faz o deploy automaticamente
5. Copie a URL pública gerada (ex: `https://lupa-cnpj-api.up.railway.app`) e configure no frontend

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/cnpj/import` | Upload CSV → retorna `jobId` |
| GET | `/cnpj/import/{jobId}/status` | Progresso + resultados parciais |
| GET | `/cnpj/import/{jobId}/download` | Download do CSV final |
| GET | `/actuator/health` | Health check |

## CSV de entrada

Colunas `cnpj` e/ou `razao_social` (pelo menos uma por linha).

Exemplos em `exemplos/`.
