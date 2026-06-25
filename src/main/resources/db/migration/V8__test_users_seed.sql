-- Usuários de teste (senha comum: Lupa@Test2026)
-- BCrypt strength 12 — ver docs/USUARIOS-TESTE.md

SET @pwd = '$2a$12$gHHQP12b99Sb5znpzD2Kwu.w/RkshT.UT1U6oEIG.9lFw8HFcdG7a';

-- Admin geral (acesso ilimitado)
INSERT INTO users (id, nome, email, cpf, password_hash, role, plan, enabled, created_at, trial_utilizado)
VALUES (
    UNHEX('00000000000000000000000000000001'),
    'Admin Lupa Insights',
    'admin@lupainsights.com.br',
    '11144477735',
    @pwd,
    'ADMIN',
    'PRO_PLUS',
    1,
    CURRENT_TIMESTAMP(6),
    0
)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    cpf = VALUES(cpf),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    plan = VALUES(plan),
    enabled = VALUES(enabled);

-- Free
INSERT INTO users (id, nome, email, cpf, password_hash, role, plan, enabled, created_at, trial_utilizado)
VALUES (
    UNHEX('00000000000000000000000000000002'),
    'Teste Free',
    'teste.free@lupainsights.com.br',
    '52998224725',
    @pwd,
    'USER',
    'FREE',
    1,
    CURRENT_TIMESTAMP(6),
    0
)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    cpf = VALUES(cpf),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    plan = VALUES(plan),
    enabled = VALUES(enabled);

-- Premium (Prospecção)
INSERT INTO users (id, nome, email, cpf, password_hash, role, plan, enabled, created_at, trial_utilizado)
VALUES (
    UNHEX('00000000000000000000000000000003'),
    'Teste Premium',
    'teste.premium@lupainsights.com.br',
    '39053344705',
    @pwd,
    'USER',
    'PREMIUM',
    1,
    CURRENT_TIMESTAMP(6),
    0
)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    cpf = VALUES(cpf),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    plan = VALUES(plan),
    enabled = VALUES(enabled);

-- Pro+ (Growth)
INSERT INTO users (id, nome, email, cpf, password_hash, role, plan, enabled, created_at, trial_utilizado)
VALUES (
    UNHEX('00000000000000000000000000000004'),
    'Teste Pro Plus',
    'teste.proplus@lupainsights.com.br',
    '71428793860',
    @pwd,
    'USER',
    'PRO_PLUS',
    1,
    CURRENT_TIMESTAMP(6),
    0
)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    cpf = VALUES(cpf),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    plan = VALUES(plan),
    enabled = VALUES(enabled);

-- Conta Silas/Felipe (CPF já cadastrado em produção)
UPDATE users
SET password_hash = @pwd
WHERE cpf = '40173586830';
