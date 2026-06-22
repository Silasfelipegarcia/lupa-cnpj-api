CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_id BINARY(16) NULL,
    action VARCHAR(50) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    status_code INT NOT NULL,
    details VARCHAR(1000) NULL,
    duration_ms INT NOT NULL
);

CREATE INDEX idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
