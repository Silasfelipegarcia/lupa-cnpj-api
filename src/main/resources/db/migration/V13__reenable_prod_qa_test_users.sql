-- Reabilita contas QA em produção com senha rotacionada (BCrypt 12).
-- Senha em texto: ver docs/USUARIOS-TESTE.md (seção Produção).
-- Não reutiliza o hash público do V8 (Lupa@Test2026).

SET @pwd = '$2a$12$t3lGyqWDh.8f.Ol1MJRB7.8r4xhJ0vx5GWiqJOBPHs93mkZw3Himu';

UPDATE users
SET enabled = 1,
    password_hash = @pwd,
    failed_login_attempts = 0,
    locked_until = NULL,
    password_reset_token_hash = NULL,
    password_reset_expires_at = NULL
WHERE email IN (
    'teste.free@lupainsights.com.br',
    'teste.premium@lupainsights.com.br',
    'teste.proplus@lupainsights.com.br',
    'admin@lupainsights.com.br'
);

UPDATE users
SET plan = 'FREE',
    plan_valid_until = NULL,
    plan_cancelled_at = NULL,
    auto_renew = FALSE,
    trial_ate = NULL,
    trial_utilizado = 0
WHERE email = 'teste.free@lupainsights.com.br';

UPDATE users
SET plan = 'PREMIUM',
    plan_valid_until = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 365 DAY),
    plan_cancelled_at = NULL,
    auto_renew = TRUE,
    trial_ate = NULL
WHERE email = 'teste.premium@lupainsights.com.br';

UPDATE users
SET plan = 'PRO_PLUS',
    plan_valid_until = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 365 DAY),
    plan_cancelled_at = NULL,
    auto_renew = TRUE,
    trial_ate = NULL
WHERE email = 'teste.proplus@lupainsights.com.br';

UPDATE users
SET role = 'ADMIN',
    plan = 'PRO_PLUS',
    plan_valid_until = NULL,
    plan_cancelled_at = NULL,
    auto_renew = FALSE
WHERE email = 'admin@lupainsights.com.br';
