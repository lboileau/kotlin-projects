#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_DIR="$ROOT_DIR/databases/camper-db"

echo "==> Stopping webapp (if running)..."
pkill -f 'vite.*webapp' 2>/dev/null && echo "    Webapp stopped." || echo "    No running webapp found."

echo "==> Stopping service (if running)..."
pkill -f 'camper-service' 2>/dev/null && echo "    Service stopped." || echo "    No running service found."

echo "==> Stopping database..."
docker compose -f "$DB_DIR/docker-compose.yml" down

echo "==> All stopped."
