-- As entidades JPA mapeiam os enums com @Enumerated(EnumType.STRING), o que o Hibernate
-- persiste como VARCHAR. A migração V1, porém, criou tipos ENUM nativos do PostgreSQL.
-- No perfil dev isso não aparece, porque o Hibernate é quem cria o schema (como varchar).
-- No perfil prod, o schema vem do Flyway e o INSERT envia parâmetro varchar para uma
-- coluna enum, gerando SQLState 42804 e impedindo a carga inicial.
--
-- Esta migração alinha o banco ao modelo: converte as colunas enum para VARCHAR e remove
-- os tipos nativos que deixam de ser usados.

ALTER TABLE procedimentos
    ALTER COLUMN categoria TYPE VARCHAR(255) USING categoria::text;

ALTER TABLE agendamentos
    ALTER COLUMN status TYPE VARCHAR(255) USING status::text;

ALTER TABLE usuarios
    ALTER COLUMN perfil TYPE VARCHAR(255) USING perfil::text;

ALTER TABLE horarios_atendimento
    ALTER COLUMN dia_semana TYPE VARCHAR(255) USING dia_semana::text;

DROP TYPE IF EXISTS categoria_procedimento;
DROP TYPE IF EXISTS status_agendamento;
DROP TYPE IF EXISTS perfil_usuario;
DROP TYPE IF EXISTS dia_semana;
