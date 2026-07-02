# Usuários de teste

Contas criadas pela migration `V8__test_users_seed.sql` e mantidas pela `V13__reenable_prod_qa_test_users.sql`.

| Perfil   | E-mail                              | Plano    | Role  |
|----------|-------------------------------------|----------|-------|
| Free     | `teste.free@lupainsights.com.br`    | FREE     | USER  |
| Premium  | `teste.premium@lupainsights.com.br` | PREMIUM  | USER  |
| Pro+     | `teste.proplus@lupainsights.com.br` | PRO_PLUS | USER  |
| Admin    | `admin@lupainsights.com.br`         | PRO_PLUS | ADMIN |

## Senhas

| Ambiente   | Senha              | Observação                                      |
|------------|--------------------|-------------------------------------------------|
| Dev/local  | `Lupa@Test2026`    | Hash do V8; reabilitado pelo `DevTestUsersEnabler` |
| Produção   | `LupaProd@T7xK9mQ2` | Hash do V13; contas reativadas na migration V13 |

## Por que a senha antiga parou em produção?

1. A migration **V11** desativa as contas com senha conhecida do V8 (`enabled = 0`).
2. O login falha mesmo com senha correta — a API trata como credencial inválida.
3. A **V13** reabilita as contas e troca para a senha de produção acima.

## Dev/local

```bash
./scripts/seed-test-users.sh --local
```

Reinicie a API após rodar o seed se ela já estiver no ar.

## Produção

**Não** rode `seed-test-users.sh` em produção — ele reaplica o V8 com a senha antiga.

As contas são atualizadas automaticamente no deploy via Flyway (V13).

Para conferir no Railway MySQL:

```sql
SELECT nome, email, role, plan, enabled
FROM users
WHERE email LIKE '%@lupainsights.com.br'
ORDER BY role DESC, plan;
```
