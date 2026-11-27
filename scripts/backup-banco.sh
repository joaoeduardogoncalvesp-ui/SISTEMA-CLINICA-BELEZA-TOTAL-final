#!/usr/bin/env bash
#
# Gera uma cópia de segurança (dump) do banco da aplicação e confere o arquivo.
#
# Uso:
#   ./scripts/backup-banco.sh
#   DB_NAME=clinica_beleza_total ./scripts/backup-banco.sh
#   BACKUP_DIR=/var/backups/clinica ./scripts/backup-banco.sh
#
# O arquivo é criado em formato custom (-Fc), restaurável com pg_restore.
# A senha é lida de PGPASSWORD ou, se ausente, solicitada ao usuário.
#
set -euo pipefail

DB_NAME="${DB_NAME:-clinica_beleza_total}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/backups}"

mkdir -p "$BACKUP_DIR"
DATA="$(date +%Y%m%d-%H%M%S)"
ARQUIVO="$BACKUP_DIR/${DB_NAME}-${DATA}.dump"

echo "[backup] banco: $DB_NAME em $DB_HOST:$DB_PORT"
echo "[backup] destino: $ARQUIVO"

PGPASSWORD="${PGPASSWORD:-}" pg_dump \
    -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" \
    -Fc --no-owner --no-privileges \
    -f "$ARQUIVO" "$DB_NAME"

if [ ! -s "$ARQUIVO" ]; then
    echo "[backup] ERRO: arquivo vazio, backup inválido" >&2
    exit 1
fi

echo "[backup] conteúdo do dump:"
pg_restore -l "$ARQUIVO" | grep -c "TABLE DATA" | awk '{print "  " $1 " tabelas com dados"}'
echo "[backup] tamanho: $(du -h "$ARQUIVO" | cut -f1)"
echo "[backup] ok"
echo
echo "Para restaurar em uma base vazia:"
echo "  createdb $DB_NAME && pg_restore -h $DB_HOST -U $DB_USER -d $DB_NAME $ARQUIVO"
