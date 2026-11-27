#!/usr/bin/env bash
#
# Verificação de saúde da instalação. Executa os itens técnicos do checklist de
# aceite que podem ser conferidos sem intervenção humana e imprime um relatório
# que serve como evidência da implantação.
#
# Uso:
#   ./scripts/verificar-implantacao.sh
#   BASE_URL=http://192.168.0.10:8080 ./scripts/verificar-implantacao.sh
#   ./scripts/verificar-implantacao.sh admin@belezatotal.com.br 'senha'
#
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${1:-${EMAIL:-admin@belezatotal.com.br}}"
SENHA="${2:-${SENHA:-}}"
DB_NAME="${DB_NAME:-clinica_beleza_total}"
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-postgres}"

VERDE=$'\e[32m'; VERMELHO=$'\e[31m'; AMARELO=$'\e[33m'; RESET=$'\e[0m'
FALHAS=0; AVISOS=0
OK()    { echo "${VERDE}✔${RESET} $*"; }
NOK()   { echo "${VERMELHO}✘${RESET} $*"; FALHAS=$((FALHAS+1)); }
AVISO() { echo "${AMARELO}⚠${RESET} $*"; AVISOS=$((AVISOS+1)); }
INFO()  { echo "ℹ $*"; }

http() { curl -s -o /tmp/verifica_body -w '%{http_code}' "$@"; }

echo "=== Relatório de verificação da implantação ==="
echo "Data: $(date '+%d/%m/%Y %H:%M')"
echo "Servidor: $BASE_URL"
echo "Banco: $DB_NAME em $DB_HOST"
echo

echo "--- 1. Aplicação acessível ---"
STATUS=$(http "$BASE_URL/api/public/homepage")
[ "$STATUS" = "200" ] && OK "Página inicial e API respondem (HTTP 200)" \
    || { NOK "Sem resposta em $BASE_URL (HTTP $STATUS)"; echo; echo "Interrompido."; exit 1; }

echo
echo "--- 2. Banco de dados e migrações ---"
if command -v psql >/dev/null 2>&1; then
    VERSAO=$(PGPASSWORD="${PGPASSWORD:-postgres}" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -Atc \
        "select version, description, success from flyway_schema_history order by installed_rank desc limit 1" 2>/dev/null)
    if [ -n "$VERSAO" ]; then
        OK "Flyway: versão ${VERSAO%%|*} aplicada (${VERSAO#*|} )"
    else
        AVISO "Não foi possível ler flyway_schema_history (perfil dev não usa Flyway)"
    fi
    TABELAS=$(PGPASSWORD="${PGPASSWORD:-postgres}" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -Atc \
        "select count(*) from information_schema.tables where table_schema='public'" 2>/dev/null)
    [ "${TABELAS:-0}" -ge 7 ] && OK "Tabelas no schema public: $TABELAS" \
        || AVISO "Contagem de tabelas inesperada: ${TABELAS:-?}"
    CRED=$(PGPASSWORD="${PGPASSWORD:-postgres}" psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -Atc \
        "select count(*) from usuarios where senha = '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'" 2>/dev/null)
    [ "${CRED:-1}" = "0" ] && OK "Nenhum usuário com a senha pública de exemplo" \
        || NOK "Existem usuários com a senha pública de exemplo"
else
    AVISO "psql não encontrado; verificação do banco ignorada"
fi

echo
echo "--- 3. Segurança ---"
STATUS=$(http "$BASE_URL/api/clientes")
[ "$STATUS" = "401" ] && OK "Endpoint protegido recusa acesso sem token (HTTP 401)" \
    || NOK "Esperado 401 sem token, obtido $STATUS"

if [ -z "$SENHA" ]; then
    read -r -s -p "Senha de $EMAIL (Enter para pular login): " SENHA; echo
fi
if [ -n "$SENHA" ]; then
    STATUS=$(http -X POST "$BASE_URL/api/usuarios/login" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"$EMAIL\",\"senha\":\"$SENHA\"}")
    if [ "$STATUS" = "200" ]; then
        TOKEN=$(python3 -c "import json;print(json.load(open('/tmp/verifica_body'))['token'])" 2>/dev/null)
        OK "Login autenticado (HTTP 200)"

        STATUS=$(http -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/funcionarios")
        [ "$STATUS" = "200" ] && OK "Leitura autenticada de funcionários (HTTP 200)" \
            || NOK "Falha ao ler funcionários (HTTP $STATUS)"

        STATUS=$(http -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL/api/indicadores?inicio=$(date +%Y-%m-01)&fim=$(date +%Y-%m-%d)")
        [ "$STATUS" = "200" ] && OK "Indicadores do mês (HTTP 200)" \
            || NOK "Falha nos indicadores (HTTP $STATUS)"

        STATUS=$(http -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/agendamentos/diagnostico-whatsapp")
        if [ "$STATUS" = "200" ]; then
            HABILITADO=$(python3 -c "
import json
d = json.load(open('/tmp/verifica_body'))
print(d.get('envioRealHabilitado', d.get('habilitado', '?')))
" 2>/dev/null)
            INFO "WhatsApp habilitado: ${HABILITADO:-?}"
            INFO "Detalhe: $(head -c 300 /tmp/verifica_body)"
        else
            AVISO "Diagnóstico de WhatsApp indisponível (HTTP $STATUS)"
        fi
    else
        NOK "Login falhou (HTTP $STATUS): $(head -c 200 /tmp/verifica_body)"
    fi
fi

echo
echo "--- 4. Carga mínima ---"
PROCS=$(curl -s "$BASE_URL/api/public/portfolio" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)))' 2>/dev/null || echo 0)
[ "$PROCS" -ge 1 ] && OK "Procedimentos cadastrados: $PROCS" || NOK "Nenhum procedimento cadastrado"
FUNCS=$(curl -s "$BASE_URL/api/public/funcionarios" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)))' 2>/dev/null || echo 0)
[ "$FUNCS" -ge 1 ] && OK "Profissionais cadastrados: $FUNCS" || NOK "Nenhum profissional cadastrado"

echo
echo "--- 5. Espaço em disco ---"
DISPONIVEL=$(df -h "$(dirname "${BASH_SOURCE[0]}")" | awk 'NR==2 {print $4}')
USO=$(df -h "$(dirname "${BASH_SOURCE[0]}")" | awk 'NR==2 {print $5}')
INFO "Livre na partição: $DISPONIVEL (uso $USO)"
INT=$(df --output=pcent "$(dirname "${BASH_SOURCE[0]}")" | tail -1 | tr -dc '0-9')
[ "${INT:-0}" -lt 90 ] && OK "Uso de disco abaixo de 90%" || NOK "Uso de disco em ${INT}%"

echo
if [ "$FALHAS" -eq 0 ]; then
    echo "${VERDE}=== VERIFICAÇÃO CONCLUÍDA: $AVISOS aviso(s), nenhuma falha ===${RESET}"
    exit 0
else
    echo "${VERMELHO}=== VERIFICAÇÃO CONCLUÍDA: $FALHAS falha(s), $AVISOS aviso(s) ===${RESET}"
    exit 1
fi
