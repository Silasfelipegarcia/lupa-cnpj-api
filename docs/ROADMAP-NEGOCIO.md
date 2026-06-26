# Lupa Insights — Roadmap de Negócio e Oportunidades

Análise de melhorias, dores futuras e direções estratégicas para o produto.

**Última atualização:** junho de 2026  
**Documentação base:** [DOCUMENTACAO.md](./DOCUMENTACAO.md)

---

## 1. Onde estamos hoje (diagnóstico)

### Forças

- Proposta clara e nicho bem definido (prospecção B2B com CNPJ)
- Funil freemium funcional (guest → free → trial → pago)
- Stack enxuta com baixo custo operacional
- Diferencial de velocidade vs. consulta manual
- Planos com preço acessível para SDRs e pequenos times

### Lacunas atuais

| Lacuna | Impacto no negócio |
|--------|-------------------|
| Plano Business só no catálogo (sem API/CRM) | Perde receita enterprise e retenção de agências grandes |
| Dados de contato dependem de terceiro (CNPJ.ws) | Risco de qualidade, custo e disponibilidade |
| Export manual para CRM | Fricção pós-export; usuário ainda faz trabalho manual |
| Sem scoring ou priorização de leads | Lista enriquecida, mas não priorizada |
| Free mascara contatos | Bom para conversão, mas pode frustrar antes do upgrade |
| Worker in-process (1 job/usuário) | Limita percepção de escala para Growth/Business |
| Sem relatórios de campanha | Agências não veem ROI dentro do produto |

---

## 2. Dores do mercado que ainda não resolvemos

### Dores dos SDRs / pré-vendas

| Dor | Situação hoje | Oportunidade |
|-----|---------------|--------------|
| "Não sei por onde começar na lista" | Export flat, sem priorização | **Lead scoring** (porte, CNAE, situação, presença de contato) |
| "Telefone/e-mail desatualizado" | Dado pontual da consulta | **Revalidação periódica** + alertas de mudança cadastral |
| "Perco tempo com empresas erradas" | Filtro só ATIVA | Filtros por **porte, faturamento estimado, regime tributário, sócios** |
| "CRM não conversa com a ferramenta" | Download CSV/Excel | **Integrações nativas** (HubSpot, Pipedrive, RD Station, Salesforce) |

### Dores das agências / consultorias

| Dor | Situação hoje | Oportunidade |
|-----|---------------|--------------|
| Múltiplos clientes/campanhas | Listas salvas por usuário | **Workspaces multi-cliente** com permissões |
| Cobrança por projeto | Plano por usuário | **Planos por equipe** ou **créditos por linha** |
| Provar valor ao cliente | Sem dashboard de campanha | **Relatórios exportáveis** (% ativas, % com contato, por segmento) |
| Volume imprevisível | Limites fixos por plano | **Pacotes de créditos** ou **pay-as-you-go** |

### Dores dos reps comerciais

| Dor | Situação hoje | Oportunidade |
|-----|---------------|--------------|
| Validar 1 lead no campo | Consulta avulsa ok | **Extensão Chrome / mobile** para consulta rápida |
| Lista do CRM desatualizada | Import manual | **Sync bidirecional** com CRM |
| Não sabe se empresa é ICP | Só dados cadastrais | **Fit score** por CNAE + região + porte configurável |

### Dores operacionais (nossas)

| Dor | Risco | Ação |
|-----|-------|------|
| Dependência CNPJ.ws | Custo variável, rate limit, single vendor | Cache agressivo, segundo provedor, ou base própria |
| Churn pós-campanha | Uso episódico | Reprocessamento + alertas + casos de uso recorrentes |
| Conversão Free → Pago | Mascaramento pode frustrar | Testar **mais preview**, **créditos limitados** em vez de máscara total |
| Plano Business vazio | Promessa sem entrega | Priorizar API pública ou integração CRM como primeiro passo enterprise |

---

## 3. Oportunidades de produto (o que podemos trazer)

### Curto prazo (0–3 meses) — retenção e conversão

| Iniciativa | Dor que resolve | Esforço | Impacto receita |
|------------|-----------------|---------|-----------------|
| **Melhorar onboarding** — wizard pós-cadastro com upload guiado | Abandono no primeiro uso | Baixo | Alto |
| **E-mail transacional** — job concluído, trial expirando, limite atingido | Usuário não volta | Baixo | Médio |
| **Indicadores na lista** — % ativas, % com telefone, % com e-mail | Lista sem contexto | Baixo | Médio |
| **Templates de filtro salvos** — "Só SP + ativas + com telefone" | Retrabalho a cada export | Médio | Médio |
| **Referral / indicação** — créditos extras por convite | CAC alto | Médio | Alto |

### Médio prazo (3–6 meses) — diferenciação

| Iniciativa | Dor que resolve | Esforço | Impacto receita |
|------------|-----------------|---------|-----------------|
| **API pública + API keys** (plano Business v1) | Integração manual | Alto | Alto (enterprise) |
| **Webhook** — job concluído notifica sistema externo | Automação | Médio | Alto |
| **Integração CRM** (começar por 1: RD Station ou HubSpot) | Copy-paste pós-export | Alto | Alto |
| **Lead scoring básico** — pontuação por completude + situação + CNAE | Priorização manual | Médio | Médio |
| **Busca e segmentação** — filtrar base por CNAE/UF antes de importar | Lista errada desde o início | Alto | Alto |
| **Planos anuais** com desconto | Churn mensal | Baixo | Médio |

### Longo prazo (6–12 meses) — expansão

| Iniciativa | Dor que resolve | Esforço | Impacto receita |
|------------|-----------------|---------|-----------------|
| **Enriquecimento além do CNPJ** — LinkedIn, site, tech stack | Contato único insuficiente | Alto | Alto |
| **Monitoramento de carteira** — alertas quando empresa muda situação | Dado fica obsoleto | Alto | Alto (receita recorrente) |
| **Workspaces / multi-tenant** para agências | Um login por campanha | Alto | Alto |
| **Marketplace de listas** (compliance) ou **ICP builder** | Prospector não sabe quem buscar | Muito alto | Muito alto |
| **Dados complementares** — Simples Nacional, MEI, quadro societário expandido | Qualificação superficial | Médio | Médio |

---

## 4. Melhorias de modelo de negócio

### Pricing e packaging

| Ideia | Racional |
|-------|----------|
| **Créditos por linha** além de planos fixos | Agências com picos; reduz barreira vs. upgrade de plano inteiro |
| **Plano anual** (-15/20%) | Melhora LTV e reduz churn |
| **Add-on "revalidação"** | Cobrar por reprocessamento automático mensal da carteira |
| **Seats para equipe** | Growth virar plano de time, não só de indivíduo |
| **Business com mínimo de créditos** | Garantir ticket mínimo enterprise |

### Aquisição

| Canal | Ação |
|-------|------|
| **SEO de cauda longa** | Páginas por CNAE, UF, "como prospectar [segmento]" |
| **Conteúdo** | Guias de prospecção B2B com CNPJ (topo de funil) |
| **Parcerias** | Agências de outbound, consultorias de vendas, CRMs brasileiros |
| **Comunidades** | Grupos de SDR, RevOps, founders B2B |

### Retenção

| Mecânica | Por quê |
|----------|---------|
| **Alertas de mudança cadastral** | Motivo para voltar todo mês |
| **Histórico + reprocessar** (já existe) | Reforçar na comunicação como diferencial |
| **Listas por campanha com métricas** | Agência vê valor acumulado |
| **Health score do cliente** | Intervir antes do churn (admin interno) |

---

## 5. Personas futuras a considerar

| Persona nova | Dor | Produto para ela |
|--------------|-----|------------------|
| **RevOps / Sales Ops** | Dados inconsistentes entre ferramentas | API + sync CRM + governança de dados |
| **Founder / MEI B2B** | Validar fornecedor/parceiro | Consulta avulsa + relatório simples |
| **Analista de crédito / compliance** | Due diligence em lote | Situação cadastral + histórico + export auditável |
| **Recrutador B2B** | Lista de empresas por segmento | Busca por CNAE + porte (futuro) |

---

## 6. Métricas de negócio a acompanhar

| Métrica | Por quê importa |
|---------|-----------------|
| **Guest → Cadastro** | Eficácia do preview grátis |
| **Cadastro → 1º import** | Ativação |
| **Free → Trial** | Interesse em pagar |
| **Trial → Pago** | Conversão real |
| **Churn mensal por plano** | Saúde da receita |
| **Linhas processadas / usuário ativo** | Engajamento e fit de plano |
| **% exports com filtro ativas** | Uso de features pagas |
| **Reprocessamentos / usuário** | Sinal de retenção |
| **CAC por canal** | Onde investir aquisição |
| **LTV / CAC** | Sustentabilidade |

---

## 7. Priorização sugerida (framework impacto × esforço)

### Fazer primeiro (quick wins + fundação)

1. E-mails transacionais e notificações de limite/trial
2. Dashboard de qualidade da lista (% ativas, % contato)
3. Onboarding guiado no primeiro login
4. Plano anual
5. API pública mínima (Business v1) — honrar promessa do catálogo

### Fazer em seguida (diferenciação)

6. Integração com 1 CRM brasileiro
7. Webhooks de job concluído
8. Lead scoring básico
9. Pacotes de créditos para picos de volume

### Explorar depois (moat)

10. Monitoramento de carteira com alertas
11. Segundo provedor de dados / cache proprietário
12. Workspaces para agências
13. Enriquecimento além do cadastro CNPJ

---

## 8. Riscos estratégicos

| Risco | Mitigação |
|-------|-----------|
| Concorrente com dados + CRM integrado | Focar velocidade, preço e UX brasileira |
| CNPJ.ws aumenta preço ou limita API | Cache, contrato comercial, plano B de provedor |
| Uso sazonal (campanhas pontuais) | Receita recorrente via monitoramento e créditos |
| LGPD / uso indevido de dados | Termos claros, audit trail, opt-out, DPO |
| Plano Business prometido e não entregue | Entregar API v1 ou remover do catálogo até estar pronto |

---

## 9. Visão de futuro (12–18 meses)

> **De "enriquecedor de planilha" para "plataforma de inteligência comercial B2B no Brasil".**

O usuário não só importa CNPJs — ele **define o ICP**, **enriquece**, **prioriza**, **sincroniza com o CRM** e **monitora mudanças** na carteira. A receita vem de assinaturas por time, créditos de volume e contratos enterprise com API e SLA.

### North Star Metric sugerida

**Linhas enriquecidas que viraram ação** — exports com filtro de ativas + contato, ou sync para CRM (quando existir).

---

## 10. Próximos passos recomendados

1. Validar com 5–10 usuários pagos: qual dor pós-export mais incomoda?
2. Decidir: **API Business** ou **integração CRM** como próximo bet enterprise?
3. Implementar métricas de funil no admin (guest → paid)
4. Testar pricing: créditos avulsos vs. só planos mensais
5. Revisar mascaramento Free — A/B test de conversão
