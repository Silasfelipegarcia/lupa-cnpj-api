ALTER TABLE import_jobs
    ADD COLUMN nome_lista VARCHAR(200) NULL,
    ADD COLUMN lista_salva TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN trial_utilizado TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN trial_ate TIMESTAMP(6) NULL;

CREATE INDEX idx_import_jobs_lista_salva ON import_jobs (user_id, lista_salva, created_at DESC);
