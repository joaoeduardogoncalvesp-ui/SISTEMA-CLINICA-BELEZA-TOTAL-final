-- =============================================================================
-- V5: remoção de credenciais fixas criadas pela carga inicial da V1
-- =============================================================================
-- A V1 inseria cinco usuários com o mesmo hash BCrypt público (senha "password").
-- Isso contradiz o funcionamento documentado do DataInitializer, que gera senhas
-- aleatórias, e representa risco de acesso indevido em ambiente de produção.
--
-- Esta migração remove apenas os usuários que ainda usam exatamente aquele hash.
-- Usuários cuja senha já foi alterada pela administração são preservados.
-- Na próxima inicialização, o DataInitializer recria os logins ausentes com
-- senhas aleatórias e as exibe uma única vez no log.

DELETE FROM usuarios
 WHERE senha = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
