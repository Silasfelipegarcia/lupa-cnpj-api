ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'FREE';

CREATE TABLE user_daily_usage (
    user_id BINARY(16) NOT NULL,
    usage_date DATE NOT NULL,
    batch_searches INT NOT NULL DEFAULT 0,
    direct_cnpj_lookups INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date),
    CONSTRAINT fk_daily_usage_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE payment_orders (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    plan VARCHAR(20) NOT NULL,
    mp_preference_id VARCHAR(100) NULL,
    mp_payment_id VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL,
    amount_cents INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_payment_orders_user ON payment_orders (user_id, created_at DESC);
CREATE INDEX idx_payment_orders_mp_payment ON payment_orders (mp_payment_id);

-- Admin master: lupa@admin.com.br / feste@123
INSERT INTO users (id, nome, email, cpf, password_hash, role, plan, enabled, created_at)
VALUES (
    UNHEX('00000000000000000000000000000001'),
    'Admin Lupa',
    'lupa@admin.com.br',
    '11144477735',
    '$2a$12$5HU9U5LlIS8bQEOim35s6uOtfLU6lkKPh.nJB.83TzBD3z.UmHUpS',
    'ADMIN',
    'PRO_PLUS',
    1,
    CURRENT_TIMESTAMP(6)
);
