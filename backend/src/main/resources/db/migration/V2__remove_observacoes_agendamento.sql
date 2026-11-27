-- O campo de observações do AGENDAMENTO foi descontinuado: as anotações da recepção
-- passaram a ser registradas no atendimento, e o campo não é mais coletado na tela.
-- A coluna de observações da tabela CLIENTES permanece intacta.
ALTER TABLE agendamentos
    DROP COLUMN IF EXISTS observacoes;
