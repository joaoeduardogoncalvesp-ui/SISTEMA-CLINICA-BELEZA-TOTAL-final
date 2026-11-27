-- A coluna `consentimento_whats_app` (gerada pelo mapeamento da entidade Cliente no
-- perfil dev, onde o Hibernate cria o schema) não correspondia ao nome criado pela
-- migração V1 (`consentimento_whatsapp`). No perfil prod, em que o Hibernate apenas
-- valida o schema, essa divergência impedia a subida da aplicação.
--
-- Esta migração alinha o banco ao modelo da entidade e mantém a compatibilidade com
-- bases já criadas pelo perfil dev. É idempotente em ambos os casos.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'clientes' AND column_name = 'consentimento_whatsapp')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'clientes' AND column_name = 'consentimento_whats_app') THEN
        ALTER TABLE clientes RENAME COLUMN consentimento_whatsapp TO consentimento_whats_app;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'clientes' AND column_name = 'consentimento_whats_app') THEN
        ALTER TABLE clientes
            ADD COLUMN consentimento_whats_app BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;
