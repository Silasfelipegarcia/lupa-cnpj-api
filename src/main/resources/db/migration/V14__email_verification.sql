ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verification_token_hash VARCHAR(64) NULL,
    ADD COLUMN email_verification_expires_at TIMESTAMP(6) NULL;

CREATE INDEX idx_users_email_verification_token_hash ON users (email_verification_token_hash);

UPDATE users SET email_verified = TRUE;
