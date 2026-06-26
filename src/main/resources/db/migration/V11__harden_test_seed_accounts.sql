-- Desativa contas de teste com senha conhecida do seed V8 (não remove dados).
UPDATE users
SET enabled = 0
WHERE email IN (
    'teste.free@lupainsights.com.br',
    'teste.premium@lupainsights.com.br',
    'teste.proplus@lupainsights.com.br'
);

-- Admin seed só é desativado se ainda usa o hash conhecido do V8.
UPDATE users
SET enabled = 0
WHERE email = 'admin@lupainsights.com.br'
  AND password_hash = '$2a$12$gHHQP12b99Sb5znpzD2Kwu.w/RkshT.UT1U6oEIG.9lFw8HFcdG7a';
