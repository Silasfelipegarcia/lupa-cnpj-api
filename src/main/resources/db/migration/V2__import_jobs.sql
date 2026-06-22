CREATE TABLE import_jobs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    arquivo VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total INT NOT NULL DEFAULT 0,
    processados INT NOT NULL DEFAULT 0,
    sucesso INT NOT NULL DEFAULT 0,
    erros INT NOT NULL DEFAULT 0,
    mensagem TEXT,
    linhas_json TEXT NOT NULL,
    resultado_csv LONGBLOB,
    cancelamento_solicitado TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_import_jobs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_import_jobs_user_created ON import_jobs (user_id, created_at DESC);
CREATE INDEX idx_import_jobs_status ON import_jobs (status);

CREATE TABLE import_results (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job_id BINARY(16) NOT NULL,
    linha_numero INT NOT NULL,
    cnpj VARCHAR(20),
    razao_social_informada VARCHAR(500),
    razao_social VARCHAR(500),
    nome_fantasia VARCHAR(500),
    situacao_cadastral VARCHAR(100),
    telefone1 VARCHAR(50),
    telefone2 VARCHAR(50),
    email VARCHAR(255),
    logradouro VARCHAR(500),
    numero VARCHAR(50),
    complemento VARCHAR(255),
    bairro VARCHAR(255),
    cidade VARCHAR(255),
    uf VARCHAR(2),
    cep VARCHAR(20),
    cnae_principal VARCHAR(500),
    observacao TEXT,
    status_consulta VARCHAR(20) NOT NULL,
    erro TEXT,
    CONSTRAINT fk_import_results_job FOREIGN KEY (job_id) REFERENCES import_jobs (id) ON DELETE CASCADE
);

CREATE INDEX idx_import_results_job ON import_results (job_id);
