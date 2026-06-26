ALTER TABLE audit_logs
    ADD COLUMN request_id VARCHAR(36) NULL AFTER duration_ms;

CREATE INDEX idx_audit_logs_request_id ON audit_logs (request_id);

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 AFTER default_card_id,
    ADD COLUMN locked_until TIMESTAMP(6) NULL AFTER failed_login_attempts;

CREATE TABLE idempotency_keys (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    response_body TEXT NOT NULL,
    status_code INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
CREATE INDEX idx_idempotency_user ON idempotency_keys (user_id);
