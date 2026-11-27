#!/usr/bin/env bash
#
# Sobe ou derruba os serviços de apoio do sistema no Docker:
# Redis e Evolution API (WhatsApp). O PostgreSQL NÃO é gerenciado aqui — ele roda
# nativo na máquina (veja scripts/preparar-banco.sh).
#
# Uso:
#   bash scripts/servicos-docker.sh up      # sobe redis e evolution-api
#   bash scripts/servicos-docker.sh down    # derruba os containers (mantém dados)
#   bash scripts/servicos-docker.sh status  # mostra o estado
#   bash scripts/servicos-docker.sh wait    # aguarda a Evolution responder
#
# Variáveis:
#   WHATSAPP_API_URL   endereço da Evolution (padrão http://localhost:8081)
#   DOCKER_TIMEOUT     segundos de espera (padrão 60)
#
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$RAIZ/docker-compose.yml"
URL="${WHATSAPP_API_URL:-http://localhost:8081}"
TIMEOUT="${DOCKER_TIMEOUT:-60}"
SERVICOS=(redis evolution-api)

log()  { echo "[servicos-docker] $*"; }
erro() { echo "[servicos-docker] ERRO: $*" >&2; }

precisa_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        erro "Docker não encontrado. Instale com: sudo bash scripts/instalar-docker.sh"
        exit 1
    fi
    if ! docker info >/dev/null 2>&1; then
        erro "Sem permissão para falar com o Docker. Entre no grupo docker (newgrp docker) ou use sudo."
        exit 1
    fi
}

usuarios_ativos() {
    # Conta apenas conexões do pool da aplicação (HikariCP) na base do sistema.
    # Conexões de psql, scripts e da própria Evolution API não contam.
    local total
    total="$(
        PGPASSWORD="${DB_PASSWORD:-postgres}" psql -h "${DB_HOST:-localhost}" -U "${DB_USERNAME:-postgres}" \
            -d "${DB_NAME:-clinica_beleza_total}" -tAc \
            "select count(*) from pg_stat_activity
              where datname = current_database()
                and application_name ilike 'HikariPool%'" \
            2>/dev/null || echo "?"
    )"
    echo "$total"
}

subir() {
    precisa_docker
    log "Subindo serviços: ${SERVICOS[*]}"
    docker compose -f "$COMPOSE_FILE" up -d "${SERVICOS[@]}"
    aguardar
    log "Pronto."
}

derrubar() {
    precisa_docker

    local ativos
    ativos="$(usuarios_ativos)"
    if [ "$ativos" != "?" ] && [ "$ativos" -gt 0 ]; then
        log "Atenção: $ativos conexão(ões) aberta(s) no banco."
        if [ "${FORCAR:-0}" != "1" ]; then
            erro "Derrubar a Evolution API com o sistema em uso interrompe as notificações."
            erro "Encerre o backend antes, ou rode novamente com FORCAR=1."
            exit 2
        fi
        log "FORCAR=1: prosseguindo mesmo assim."
    fi

    log "Derrubando serviços: ${SERVICOS[*]}"
    docker compose -f "$COMPOSE_FILE" stop "${SERVICOS[@]}"
    log "Containers parados. Os dados foram preservados (volumes e banco intactos)."
}

estado() {
    precisa_docker
    docker compose -f "$COMPOSE_FILE" ps "${SERVICOS[@]}" || true
    echo
    if curl -fsS --max-time 3 "$URL/" >/dev/null 2>&1; then
        log "Evolution API respondendo em $URL"
    else
        log "Evolution API NÃO responde em $URL"
    fi
}

aguardar() {
    local inicio
    inicio=$(date +%s)
    log "Aguardando a Evolution API em $URL (até ${TIMEOUT}s)..."
    while [ $(( $(date +%s) - inicio )) -lt "$TIMEOUT" ]; do
        if curl -fsS --max-time 3 "$URL/" >/dev/null 2>&1; then
            log "Evolution API no ar após $(( $(date +%s) - inicio ))s."
            return 0
        fi
        sleep 2
    done
    erro "A Evolution API não respondeu em ${TIMEOUT}s. Veja: docker compose logs -f evolution-api"
    return 1
}

case "${1:-}" in
    up)     subir ;;
    down)   derrubar ;;
    status) estado ;;
    wait)   aguardar ;;
    *)
        echo "Uso: bash scripts/servicos-docker.sh {up|down|status|wait}"
        exit 1
        ;;
esac
