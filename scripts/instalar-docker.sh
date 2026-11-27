#!/usr/bin/env bash
#
# Instala Docker + Docker Compose (plugin "docker compose") no Debian/Ubuntu.
# Uso:
#   sudo bash scripts/instalar-docker.sh
#
set -euo pipefail

if [ "${EUID}" -ne 0 ]; then
    echo "Execute com sudo: sudo bash scripts/instalar-docker.sh" >&2
    exit 1
fi

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose

systemctl enable --now docker

USUARIO="${SUDO_USER:-}"
if [ -n "${USUARIO}" ] && [ "${USUARIO}" != "root" ]; then
    usermod -aG docker "${USUARIO}"
    echo
    echo "Usuário '${USUARIO}' adicionado ao grupo docker."
    echo "Para usar docker sem sudo, saia e entre na sessão novamente (ou rode: newgrp docker)."
fi

echo
docker --version
docker compose version
echo
echo "Docker instalado. Teste com: docker run --rm hello-world"
