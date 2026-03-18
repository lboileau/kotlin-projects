#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "Stopping interview-service..."
pkill -f "interview-service" 2>/dev/null || true

echo "Stopping database..."
cd "$PROJECT_ROOT/databases/interview-manager-db" && docker compose down

echo "Done."
