#!/usr/bin/env bash
# ============================================================================
# Teste E2E real contra a API rodando (HTTP + JWT + PostgreSQL de verdade)
#
# Uso:
#   ./scripts/teste-e2e.sh                          # usa admin@... e pede a senha
#   SENHA=xxxx ./scripts/teste-e2e.sh               # senha via variável
#   ./scripts/teste-e2e.sh admin@belezatotal.com.br senha
#   BASE_URL=http://localhost:8080 ./scripts/teste-e2e.sh ...
#
# O script cria um cliente, um agendamento e um funcionário "de teste",
# e no final remove o que criou (quando a API permite).
# ============================================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${1:-${EMAIL:-admin@belezatotal.com.br}}"
SENHA="${2:-${SENHA:-}}"

VERDE=$'\e[32m'; VERMELHO=$'\e[31m'; AMARELO=$'\e[33m'; RESET=$'\e[0m'
FALHAS=0
OK()   { echo "${VERDE}✔${RESET} $*"; }
NOK()  { echo "${VERMELHO}✘${RESET} $*"; FALHAS=$((FALHAS+1)); }
INFO() { echo "${AMARELO}ℹ${RESET} $*"; }

exigir() { # exigir "descrição" esperado obtido
  if [ "$2" = "$3" ]; then OK "$1 (HTTP $3)"; else NOK "$1 — esperado HTTP $2, obtido $3"; fi
}

api() { # api MÉTODO CAMINHO [json] -> imprime corpo; define STATUS
  local metodo="$1" caminho="$2" corpo="${3:-}"
  local args=(-s -o /tmp/e2e_body -w '%{http_code}' -X "$metodo" "$BASE_URL$caminho")
  [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$corpo" ] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  STATUS=$(curl "${args[@]}")
  cat /tmp/e2e_body
}

json() { # extrai campo simples de JSON (sem jq)
  python3 -c "import sys,json;d=json.load(sys.stdin);print(d$1)" 2>/dev/null
}

echo "=== 1. Serviço no ar ==="
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/public/homepage")
exigir "GET /api/public/homepage" 200 "$STATUS"
[ "$STATUS" = "200" ] || { echo "Backend não está respondendo em $BASE_URL. Suba com: mvn spring-boot:run"; exit 1; }

echo
echo "=== 2. Login ($EMAIL) ==="
[ -z "$SENHA" ] && read -r -s -p "Senha: " SENHA && echo
TOKEN=""
api POST /api/usuarios/login "{\"email\":\"$EMAIL\",\"senha\":\"$SENHA\"}" >/dev/null
if [ "$STATUS" != "200" ]; then
  NOK "Login falhou (HTTP $STATUS): $(cat /tmp/e2e_body)"
  echo "As senhas do DataInitializer são aleatórias e aparecem no console do backend."
  exit 1
fi
LOGIN=$(cat /tmp/e2e_body)
TOKEN=$(echo "$LOGIN" | json "['token']")
PERFIL=$(echo "$LOGIN" | json "['perfil']")
[ -n "$TOKEN" ] && OK "Login ok, perfil $PERFIL, token recebido" || { NOK "Resposta sem token"; exit 1; }

echo
echo "=== 3. Segurança ==="
TOKEN_ANTIGO="$TOKEN"; TOKEN=""
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/clientes")
exigir "GET /api/clientes sem token deve dar 401" 401 "$STATUS"
TOKEN="$TOKEN_ANTIGO"
api GET /api/indicadores?inicio=2026-01-01\&fim=2026-12-31 >/dev/null
exigir "GET /api/indicadores autenticado" 200 "$STATUS"

echo
echo "=== 4. Catálogo ==="
api GET /api/public/portfolio >/dev/null
exigir "GET /api/public/portfolio" 200 "$STATUS"
PROCS=$(cat /tmp/e2e_body)
NPROC=$(echo "$PROCS" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)))' 2>/dev/null || echo 0)
INFO "Procedimentos disponíveis: $NPROC"

echo
echo "=== 5. Criar cliente de teste ==="
SUFIXO=$(date +%s)
api POST /api/clientes "{\"nome\":\"Teste E2E $SUFIXO\",\"telefone\":\"(62) 90000-0001\",\"email\":\"e2e$SUFIXO@teste.com\",\"consentimentoDados\":true,\"consentimentoWhatsApp\":false}" >/dev/null
if [ "$STATUS" = "200" ]; then
  CLIENTE_ID=$(cat /tmp/e2e_body | json "['id']")
  OK "Cliente criado id=$CLIENTE_ID"
else
  NOK "Falha ao criar cliente (HTTP $STATUS): $(cat /tmp/e2e_body)"
fi

echo
echo "=== 6. Agendamento real ==="
# /api/funcionarios exige token; escolhe o primeiro habilitado para o procedimento
PROC_ID=$(curl -s "$BASE_URL/api/public/portfolio" | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["id"])' 2>/dev/null)
FUNC_ID=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/funcionarios" | python3 -c "
import sys, json
funcs = json.load(sys.stdin)
alvo = next((f for f in funcs if $PROC_ID in (f.get('procedimentoIds') or [])), funcs[0] if funcs else None)
print(alvo['id'] if alvo else '')
" 2>/dev/null)
DATA=$(date -d "+45 days" +%Y-%m-%d)
INFO "funcionario=$FUNC_ID procedimento=$PROC_ID data=$DATA"
if [ -n "${CLIENTE_ID:-}" ]; then
  api POST /api/agendamentos "{\"clienteId\":$CLIENTE_ID,\"funcionarioId\":$FUNC_ID,\"procedimentoId\":$PROC_ID,\"data\":\"$DATA\",\"horaInicio\":\"16:00:00\"}" >/dev/null
  if [ "$STATUS" = "200" ]; then
    AG_ID=$(cat /tmp/e2e_body | json "['id']")
    OK "Agendamento criado id=$AG_ID (horaFim=$(cat /tmp/e2e_body | json "['horaFim']"))"
  else
    NOK "Falha ao criar agendamento (HTTP $STATUS): $(cat /tmp/e2e_body)"
  fi

  echo
  echo "=== 7. Regras de negócio (pela API) ==="
  api POST /api/agendamentos "{\"clienteId\":$CLIENTE_ID,\"funcionarioId\":$FUNC_ID,\"procedimentoId\":$PROC_ID,\"data\":\"$DATA\",\"horaInicio\":\"16:15:00\"}" >/dev/null
  if [ "$STATUS" = "200" ]; then NOK "Conflito de horário NÃO foi bloqueado"; else OK "Conflito de horário bloqueado (HTTP $STATUS)"; fi

  api POST /api/agendamentos "{\"funcionarioId\":$FUNC_ID,\"procedimentoId\":$PROC_ID,\"data\":\"$DATA\"}" >/dev/null
  if [ "$STATUS" = "400" ]; then OK "Validação de campos obrigatórios (HTTP 400)"; else NOK "Validação esperada 400, obtido $STATUS"; fi

  echo
  echo "=== 8. Status e cancelamento ==="
  if [ -n "${AG_ID:-}" ]; then
    api PATCH "/api/agendamentos/$AG_ID/status?status=CONFIRMADO" >/dev/null
    exigir "PATCH status -> CONFIRMADO" 200 "$STATUS"
    api PATCH "/api/agendamentos/$AG_ID/cancelar" >/dev/null
    exigir "Cancelar agendamento" 200 "$STATUS"

    echo
    echo "=== 9. Limpeza ==="
    api DELETE "/api/agendamentos/$AG_ID" >/dev/null
    exigir "Excluir agendamento de teste" 204 "$STATUS"
  fi
  api DELETE "/api/clientes/$CLIENTE_ID/dados" >/dev/null
  INFO "Anonimização do cliente de teste: HTTP $STATUS"
fi

echo
if [ "$FALHAS" -eq 0 ]; then
  echo "${VERDE}=== TODOS OS TESTES PASSARAM ===${RESET}"
  exit 0
else
  echo "${VERMELHO}=== $FALHAS TESTE(S) FALHARAM ===${RESET}"
  exit 1
fi
