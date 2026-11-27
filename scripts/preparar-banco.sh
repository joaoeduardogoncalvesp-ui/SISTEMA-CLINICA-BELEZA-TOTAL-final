#!/usr/bin/env bash
#
# Prepara o PostgreSQL nativo para o projeto:
#   - define a senha do usuário postgres (padrão: postgres)
#   - cria o banco da aplicação (clinica_beleza_total)
#   - cria o banco da Evolution API (evolution)
#
# Uso:
#   sudo bash scripts/preparar-banco.sh
#   sudo DB_PASSWORD=minhaSenha bash scripts/preparar-banco.sh
#
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
    echo "Execute com sudo: sudo bash scripts/preparar-banco.sh" >&2
    exit 1
fi

DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_APP="${DB_APP:-clinica_beleza_total}"
DB_EVOLUTION="${DB_EVOLUTION:-evolution}"

runuser -u postgres -- psql -v ON_ERROR_STOP=1 \
    -v pw="${DB_PASSWORD}" \
    -v app="${DB_APP}" \
    -v evo="${DB_EVOLUTION}" <<'SQL'
ALTER USER postgres WITH PASSWORD :'pw';

SELECT format('CREATE DATABASE %I', :'app')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'app')\gexec

SELECT format('CREATE DATABASE %I', :'evo')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'evo')\gexec
SQL

echo
echo "Bancos prontos: ${DB_APP} e ${DB_EVOLUTION}"
echo "Usuário: postgres | Senha: ${DB_PASSWORD}"
echo "Teste: PGPASSWORD='${DB_PASSWORD}' psql -h localhost -U postgres -d ${DB_APP} -c '\\dt'"
