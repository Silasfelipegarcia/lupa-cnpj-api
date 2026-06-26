# Usuários de teste (dev/local)

Contas criadas pela migration `V8__test_users_seed.sql`.

| Perfil   | E-mail                              | Plano    |
|----------|-------------------------------------|----------|
| Free     | `teste.free@lupainsights.com.br`    | FREE     |
| Premium  | `teste.premium@lupainsights.com.br` | PREMIUM  |
| Pro+     | `teste.proplus@lupainsights.com.br` | PRO_PLUS |
| Admin    | `admin@lupainsights.com.br`         | PRO_PLUS |

**Senha comum:** `Lupa@Test2026`

## Produção vs dev

- Em **produção**, a migration `V11` desativa essas contas (senha conhecida no repositório).
- Em **dev/local** (sem profile `production`), o `DevTestUsersEnabler` reabilita automaticamente na subida da API.

## Recriar manualmente

```bash
./scripts/seed-test-users.sh --local
```

Reinicie a API após rodar o seed se ela já estiver no ar.
