# Lupa Insights — Documentação de Produto, Negócio e Arquitetura

Documentação consolidada dos repositórios `lupa-cnpj` (frontend) e `lupa-cnpj-api` (backend).

**Última atualização:** junho de 2026

---

## 1. Visão geral do produto

| Atributo | Detalhe |
|----------|---------|
| **Nome** | **Lupa Insights** (também referenciado como Lupa CNPJs em URLs de produção) |
| **Categoria** | SaaS B2B — enriquecimento de listas de CNPJ em lote |
| **Proposta de valor** | Transformar planilhas brutas de empresas em **leads prontos para ligar** — com dados cadastrais oficiais, telefone, e-mail, endereço e CNAE — em minutos, sem consulta manual empresa por empresa |
| **Tagline** | *"Prospecção B2B em lote"* · *"Sua lista de empresas, pronta para ligar"* |
| **Público** | SDRs, representantes comerciais e agências que fazem prospecção outbound no Brasil |
| **Contato** | `contato@lupainsights.com.br` |
| **Produção** | Frontend: `https://www.lupacnpjs.com.br` · API: `https://lupa-cnpj-api-production.up.railway.app` |

### O problema que resolvemos

| Antes (sem Lupa) | Depois (com Lupa) |
|------------------|-------------------|
| Planilha suja com nomes errados | Dados oficiais em lote |
| Consulta manual empresa por empresa | Processamento assíncrono com progresso em tempo real |
| Lista cheia de empresas inaptas | Filtro só empresas **ATIVAS** (planos pagos) |
| Copy-paste manual para o CRM | Export Excel/CSV em 1 clique |

### O que buscamos (visão de produto)

1. **Velocidade** — do upload à lista enriquecida em minutos, não horas
2. **Confiança** — dados cadastrais derivados da base oficial (via CNPJ.ws / Receita Federal)
3. **Ação imediata** — export pronto para CRM, com telefone e e-mail quando disponíveis
4. **Acesso democrático** — freemium com preview sem cadastro + planos acessíveis (a partir de R$ 19,90/mês)
5. **Escala progressiva** — do rep que valida 1 lead ao time de SDR que processa centenas de empresas/dia
6. **Retenção** — histórico, listas salvas e reprocessamento com dados atualizados

---

## 2. Personas e jornadas

### Personas primárias

| Persona | Dor principal | O que busca no Lupa | Plano típico |
|---------|---------------|---------------------|--------------|
| **SDR / Pré-vendas** | Lista suja, tempo perdido com inaptas | Filtrar só ativas, exportar com telefone/e-mail, começar outreach no mesmo dia | Prospecção |
| **Agência / Consultoria** | Volume alto, múltiplas campanhas | Salvar listas por campanha, reprocessar, deduplicar CNPJs | Growth |
| **Rep comercial** | Validar lead antes de ligar | Consulta avulsa por CNPJ ou import rápido da lista do CRM | Free → Prospecção |

### Jornada do usuário

```
Visitante → 1 CNPJ grátis (landing) → Cadastro Free → Upload planilha
    → Job assíncrono → Download CSV/Excel
    → [mais volume?] → Trial 7 dias Prospecção → Assinatura Mercado Pago
    → [alto volume?] → Contato Business
```

### Funil de aquisição

| Etapa | Oferta | Objetivo |
|-------|--------|----------|
| **Guest** | 1 CNPJ completo sem cadastro (por IP/dispositivo) | Provar valor antes do signup |
| **Free** | 3 CNPJs/dia + 1 planilha de até 5 linhas | Hábito de uso; dados de contato mascarados |
| **Trial** | 7 dias Prospecção com cartão (sem cobrança imediata) | Converter para pago |
| **Pago** | Prospecção ou Growth | Monetização recorrente |

---

## 3. Modelo de negócio

**Freemium SaaS** com assinaturas mensais via **Mercado Pago** (checkout, cartão salvo, renovação automática).

### Planos e limites (fonte de verdade: API — `PlanLimitsService`)

| Plano | Código interno | Preço | Linhas/arquivo | Planilhas/dia | CNPJ avulso/dia | Destaques |
|-------|----------------|-------|----------------|---------------|-----------------|-----------|
| **Free** | `FREE` | R$ 0 | 5 | 1 | 3 únicos | Histórico 7 dias; telefone/e-mail/endereço mascarados |
| **Prospecção** | `PREMIUM` | R$ 19,90/mês | 100 | 10 | Ilimitado | Excel, filtro ativas, busca razão social, histórico 90 dias, trial 7 dias |
| **Growth** | `PRO_PLUS` | R$ 49,90/mês | 500 | 50 | Ilimitado | Filtros UF/CNAE/contato, dedupe, histórico ilimitado |
| **Business** | — | Sob medida | Custom | Custom | Custom | API dedicada, webhooks, CRM, SLA (catálogo; **não implementado ainda**) |

### Dados entregues por empresa

CNPJ · Razão social · Nome fantasia · Situação cadastral · Telefones · E-mail · Endereço completo · CNAE principal

### Mecânicas comerciais

- **Upgrade proporcional** Premium → Growth via cotação (`/payments/quote`)
- **Cancelamento/reativação** self-service em Conta → Plano
- **Contas master/admin** — uso ilimitado para operação interna
- **Contas de teste** — seeds em migrations (desabilitadas em produção via V11)

---

## 4. Funcionalidades do produto

### Público / marketing

- Landing com preview de 1 CNPJ grátis
- Página de planos (`/planos`)
- Páginas legais (Termos, Privacidade LGPD, Cookies)
- Consentimento de cookies antes do Google Analytics
- SEO com JSON-LD (Organization, SoftwareApplication, FAQ, Offers)
- Prerender SSG das rotas públicas

### Autenticação e conta

- Cadastro: nome, e-mail, CPF, senha
- Login JWT (24h)
- Perfil, troca de senha
- Monitoramento de sessão e expiração do token

### Core — enriquecimento

- Upload CSV/Excel (máx. 5 MB; até 900 linhas na API)
- Consulta avulsa por CNPJ
- Busca por razão social (Prospecção+)
- Job assíncrono com polling a cada 3s
- Resultados parciais durante processamento
- Cancelamento de job em andamento
- Retomada automática após login se houver job ativo
- Download CSV ou Excel com filtros (ativas, UF, CNAE, com telefone, com e-mail)
- Template Excel para importação
- Histórico de consultas com busca
- Salvar listas nomeadas e reprocessar com dados frescos
- Notificação do browser ao concluir job
- Onboarding na primeira visita

### Billing

- Catálogo de planos da API
- Checkout Mercado Pago (redirect)
- Cartão salvo + cobrança por CVV
- Trial 7 dias Prospecção
- Histórico de pagamentos
- Webhook Mercado Pago para sincronização

### Admin (role `ADMIN`)

- Overview: usuários, assinaturas, receita, imports, linhas processadas
- Lista e detalhe de usuários

### Analytics

- Google Analytics 4 (consent-gated)
- Eventos de produto no backend (`/analytics/event`)
- Tracking de CTAs e funis

---

## 5. Arquitetura técnica

### Visão de alto nível

```
Browser
   │
   ▼
Vercel (Angular 19 SSR/SSG)
   │  /api/* proxy
   ▼
Railway (Spring Boot 3.3 Monolith)
   ├── Job Queue (in-process, single-thread)
   ├── MySQL 8
   └── Redis (opcional — rate limit)
   │
   ├── CNPJ.ws (dados CNPJ)
   ├── Mercado Pago (pagamentos)
   ├── Google Analytics 4 (frontend)
   └── New Relic APM (opcional)
```

### Padrão arquitetural

| Camada | Decisão | Motivo |
|--------|---------|--------|
| **Frontend** | SPA Angular 19 standalone + SSR/prerender | SEO nas páginas públicas; UX rica no app autenticado |
| **Backend** | Monolito Spring Boot | Simplicidade operacional; volume atual não exige microserviços |
| **Processamento** | Fila in-process (single-thread worker) | Jobs de import no mesmo JAR; sem fila externa |
| **Proxy API** | Vercel rewrite `/api/*` → Railway | Evita CORS; browser chama mesma origem |
| **Auth** | JWT stateless (HS256, 24h) | Sem sessão server-side; escala horizontal simples |
| **Persistência** | MySQL + Flyway migrations | Schema versionado; `ddl-auto: validate` |
| **Rate limiting** | In-memory ou Redis distribuído | Proteção contra abuso; Redis opcional em multi-instância |

### Estrutura do backend (monolito)

```
br.com.lupainsights/
├── controller/      REST endpoints
├── service/         Lógica de negócio, fila de jobs
├── client/          HTTP externo (CNPJ.ws)
├── payment/         Mercado Pago
├── subscription/    Planos, renovação
├── plan/            Limites, mascaramento Free
├── security/        JWT, rate limits
├── audit/           Logs de auditoria
├── entity/          JPA entities
├── repository/      Spring Data
├── csv/             I/O CSV e Excel (POI)
└── config/          DB, security, properties
```

### Estrutura do frontend

```
src/app/
├── components/     landing, import, historico, planos, conta, admin, legal, payment
├── services/       API clients, auth, analytics, cache
├── guards/         auth, guest, admin, planosPublic
├── interceptors/   JWT + X-Request-Id
├── models/         TypeScript interfaces
├── seo/            meta, JSON-LD
└── app.routes.ts   rotas + metadata SEO/analytics
```

### Padrões frontend

- Componentes **standalone** (sem NgModules)
- Estado com **Signals** (`signal()`, `computed()`)
- Interceptors funcionais para JWT e trace ID
- Cache client-side com TTL + ETag (histórico, config, listas)
- Monitor singleton de jobs com polling compartilhado

### Ciclo de vida de um import

```
NA_FILA → PROCESSANDO → CONCLUIDO
                    ↘ ERRO
                    ↘ CANCELADO
```

1. Upload valida limites do plano
2. Dedupe de CNPJs (Growth)
3. Enfileira job (máx. 1 job ativo por usuário)
4. Worker consulta CNPJ.ws linha a linha
5. Resultados persistidos em `import_results`
6. CSV/Excel gerado sob demanda no download

---

## 6. Infraestrutura e deploy

| Componente | Provedor | Detalhes |
|------------|----------|----------|
| **Frontend** | Vercel | Build Angular + sitemap; output `dist/frontend/browser` |
| **API** | Railway | Dockerfile multi-stage; healthcheck `/health` |
| **Banco** | Railway MySQL plugin | `MYSQL_URL` convertido para JDBC automaticamente |
| **Redis** | Railway (opcional) | Rate limiting distribuído |
| **APM** | New Relic | Agente Java no container (ativa com `NEW_RELIC_LICENSE_KEY`) |
| **CDN/Edge** | Vercel + Cloudflare (runbook) | Headers de segurança no `vercel.json` |
| **CI/CD** | Deploy automático | Push em `main` → Vercel/Railway; Dependabot semanal |
| **DNS** | `www.lupacnpjs.com.br` | Domínio canônico de produção |

### Container da API

- **Build:** Maven + Java 21 JDK Alpine
- **Runtime:** JRE 21 Alpine + New Relic agent
- **Entrypoint:** ativa profile `production` automaticamente
- **Porta:** 8080 (Railway injeta `PORT`)

### Segurança operacional

- Headers: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`
- Login lock após 5 falhas (15 min)
- Rate limits por IP e por usuário autenticado
- Auditoria com retenção 90 dias
- Runbook DDoS: `lupa-cnpj-api/docs/DDOS-RUNBOOK.md`
- Validação em produção: JWT secret forte, webhook secret MP obrigatório

### O que não temos (ainda)

- Terraform / Kubernetes
- docker-compose no repo
- GitHub Actions (só Dependabot)
- Multi-região / HA explícita

---

## 7. Integrações e serviços externos

| Serviço | Papel | Detalhes técnicos |
|---------|-------|-------------------|
| **CNPJ.ws** | Fonte de dados CNPJ | API pública (3 req/min) ou comercial com token (60 req/min) |
| **Mercado Pago** | Pagamentos e assinaturas | Checkout, cartões salvos, webhooks, renovação automática diária (06:00) |
| **Google Analytics 4** | Analytics de produto | Consent-gated via cookies |
| **New Relic** | APM | Traces e métricas da JVM em produção |
| **Google Fonts** | Tipografia | Inter |
| **Vercel** | Hosting frontend + proxy | Rewrite `/api/:path*` → Railway |
| **Railway** | Hosting API + MySQL + Redis | Deploy via Dockerfile |

### CNPJ.ws — detalhe da integração

| Endpoint | URL | Auth | Uso |
|----------|-----|------|-----|
| Pública | `https://publica.cnpj.ws/cnpj` | Nenhuma | Default; limite 3 req/min |
| Comercial consulta | `https://comercial.cnpj.ws/cnpj` | Header `x_api_token` | Com `CNPJ_WS_TOKEN` |
| Comercial pesquisa | `https://comercial.cnpj.ws/v2/pesquisa` | `x_api_token` | Razão social (Premium+); flag `PESQUISA_RAZAO_SOCIAL=true` |

> Os dados são derivados do cadastro da Receita Federal, mas **não há integração direta** com APIs governamentais (Serpro, etc.).

### Mercado Pago — fluxos

- Checkout redirect para nova assinatura
- Cobrança em cartão salvo (CVV)
- Webhook: `{API_PUBLIC_URL}/payments/mercadopago/webhook`
- Schedulers: renovação, expiração de trial, cleanup de idempotency keys

---

## 8. Base de dados (MySQL 8)

### Schema (Flyway V1–V11)

| Tabela | Propósito | Campos principais |
|--------|-----------|-------------------|
| **`users`** | Identidade e assinatura | UUID, nome, email, CPF, password_hash, role, plan, trial_*, mp_customer_id, default_card_id, plan_valid_until, auto_renew, lock fields |
| **`import_jobs`** | Jobs de importação | status, progresso, linhas_json, resultado_csv (LONGBLOB), nome_lista, lista_salva, cancelamento |
| **`import_results`** | Resultado por linha | CNPJ, dados da empresa, status da consulta, erro |
| **`user_daily_usage`** | Cotas diárias | batch_searches, direct_cnpj_lookups por usuário/dia |
| **`payment_orders`** | Pedidos MP | plan, IDs Mercado Pago, status, valor, flag de renovação |
| **`audit_logs`** | Auditoria | request_id, eventos de segurança/produto (retenção 90 dias) |
| **`idempotency_keys`** | Idempotência de pagamentos | Cache para evitar cobranças duplicadas |

### Schedulers (background)

| Scheduler | Horário | Função |
|-----------|---------|--------|
| `SubscriptionRenewalScheduler` | 06:00 diário | Renovação automática |
| `TrialExpirationScheduler` | — | Expiração de trial + cobrança |
| `ImportJobCleanupScheduler` | — | Limpeza de jobs antigos |
| `AuditLogRetentionScheduler` | — | Purga logs > 90 dias |
| `IdempotencyCleanupScheduler` | — | Limpeza de chaves expiradas |

---

## 9. API — mapa de endpoints

### Públicos (sem JWT)

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/`, `/health`, `/actuator/health` | Health checks |
| POST | `/auth/register`, `/auth/login` | Autenticação |
| GET | `/plans` | Catálogo de planos |
| GET | `/cnpj/preview`, `/cnpj/preview/quota` | Preview guest |
| POST | `/analytics/event` | Eventos de produto |
| POST/GET | `/payments/mercadopago/webhook` | Webhook MP |

### Autenticados

| Área | Rotas principais |
|------|------------------|
| **Auth** | `GET /auth/me`, `PUT /auth/password` |
| **CNPJ** | `/cnpj/import`, `/cnpj/consulta`, `/cnpj/template`, `/cnpj/config`, download, salvar-lista, reprocessar |
| **Payments** | checkout, charge, quote, cards, subscription, trial, history |
| **Admin** | `/admin/overview`, `/admin/users`, `/admin/users/{id}` |

---

## 10. Stack tecnológica completa

### Frontend (`lupa-cnpj`)

| Tecnologia | Versão/Uso |
|------------|------------|
| Angular | 19.2 |
| TypeScript | 5.7 |
| RxJS | 7.8 |
| SCSS | Estilos |
| SSR + Prerender | `@angular/ssr` |
| Express | Servidor SSR |
| Mercado Pago SDK JS | Tokenização de cartão |
| Node | >= 22 |
| Testes | Jasmine + Karma |

### Backend (`lupa-cnpj-api`)

| Tecnologia | Versão/Uso |
|------------|------------|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Spring Security + JWT | jjwt 0.12.6, BCrypt strength 12 |
| Spring Data JPA | Hibernate, `ddl-auto: validate` |
| MySQL | 8 |
| Flyway | Migrations |
| HikariCP | Pool (max 5 no Railway) |
| OpenCSV | 5.9 |
| Apache POI | 5.2.5 (Excel) |
| Lettuce | Redis client (opcional) |
| Logback + logstash encoder | JSON logs em produção |
| Maven | Build |

---

## 11. Variáveis de ambiente críticas

### Frontend (Vercel build)

| Variável | Padrão | Função |
|----------|--------|--------|
| `API_URL` | `/api` | Base da API (proxy) |
| `SITE_URL` | `https://www.lupacnpjs.com.br` | Canonical + sitemap |
| `GA_MEASUREMENT_ID` | `G-D0DYGXTE04` | Google Analytics |

### Backend (Railway produção)

| Variável | Obrigatória | Função |
|----------|-------------|--------|
| `JWT_SECRET` | Sim | Assinatura JWT (32+ chars) |
| `MYSQL_URL` + credenciais | Sim | Banco (plugin Railway) |
| `ALLOWED_ORIGINS` | Recomendado | CORS |
| `MERCADOPAGO_*` | Para pagamentos | Keys + webhook secret |
| `FRONTEND_URL` | Para pagamentos | Redirects checkout |
| `API_PUBLIC_URL` | Para pagamentos | URL do webhook |
| `CNPJ_WS_TOKEN` | Opcional | API comercial CNPJ.ws |
| `REDIS_URL` | Opcional | Rate limit distribuído |
| `NEW_RELIC_LICENSE_KEY` | Opcional | APM |

---

## 12. Mapa de rotas do frontend

| Rota | Acesso | Função |
|------|--------|--------|
| `/` | Público | Landing + preview CNPJ |
| `/login`, `/cadastro` | Guest | Autenticação |
| `/app` | Auth | Upload e painel principal |
| `/consulta/:jobId` | Auth | Detalhe com progresso |
| `/historico`, `/historico/:jobId` | Auth | Histórico e listas |
| `/planos` | Guest | Pricing público |
| `/conta/perfil`, `/plano`, `/cobranca` | Auth | Gestão de conta |
| `/admin/*` | Admin | Dashboard operacional |
| `/termos`, `/privacidade`, `/cookies` | Público | Legal LGPD |

---

## 13. Documentação relacionada

| Documento | Local |
|-----------|-------|
| Roadmap de negócio e oportunidades | [ROADMAP-NEGOCIO.md](./ROADMAP-NEGOCIO.md) |
| Setup Mercado Pago | `lupa-cnpj-api/MERCADOPAGO_SETUP.md` |
| Runbook DDoS | `lupa-cnpj-api/docs/DDOS-RUNBOOK.md` |
| Usuários de teste | `lupa-cnpj-api/docs/USUARIOS-TESTE.md` |
| README frontend | `lupa-cnpj/README.md` |
| README API | `lupa-cnpj-api/README.md` |

---

## Resumo executivo

**Lupa Insights** é um SaaS freemium de prospecção B2B que enriquece planilhas de CNPJ com dados cadastrais oficiais, entregando listas filtradas e exportáveis para equipes comerciais. A arquitetura é propositalmente simples: **Angular 19 na Vercel** + **Spring Boot monolito no Railway** + **MySQL**, com integrações em **CNPJ.ws** (dados) e **Mercado Pago** (receita). O produto escala por planos (Free → Prospecção → Growth → Business), com funil que começa em 1 CNPJ grátis sem cadastro e converte via trial de 7 dias.
