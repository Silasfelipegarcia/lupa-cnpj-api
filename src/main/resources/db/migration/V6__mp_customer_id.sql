ALTER TABLE users
    ADD COLUMN mp_customer_id VARCHAR(100) NULL AFTER trial_ate;

CREATE INDEX idx_users_mp_customer ON users (mp_customer_id);
