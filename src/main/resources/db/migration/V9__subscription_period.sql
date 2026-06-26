ALTER TABLE users
    ADD COLUMN plan_valid_until TIMESTAMP(6) NULL,
    ADD COLUMN plan_cancelled_at TIMESTAMP(6) NULL,
    ADD COLUMN auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN default_card_id VARCHAR(50) NULL;

ALTER TABLE payment_orders
    ADD COLUMN paid_at TIMESTAMP(6) NULL,
    ADD COLUMN renewal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET plan_valid_until = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 30 DAY),
    auto_renew = TRUE
WHERE plan IN ('PREMIUM', 'PRO_PLUS')
  AND plan_valid_until IS NULL;
