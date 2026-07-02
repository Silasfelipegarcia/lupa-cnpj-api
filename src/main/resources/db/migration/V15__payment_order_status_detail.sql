ALTER TABLE payment_orders
    ADD COLUMN status_detail VARCHAR(120) NULL AFTER status;
