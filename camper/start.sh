#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_DIR="$ROOT_DIR/databases/camper-db"
WEBAPP_DIR="$ROOT_DIR/webapp"

cleanup() {
  echo ""
  echo "==> Shutting down..."
  [ -n "${SERVICE_PID:-}" ] && kill "$SERVICE_PID" 2>/dev/null
  [ -n "${WEBAPP_PID:-}" ] && kill "$WEBAPP_PID" 2>/dev/null
  wait 2>/dev/null
  echo "    Done."
}
trap cleanup EXIT

echo "==> Starting database..."
docker compose -f "$DB_DIR/docker-compose.yml" up -d

echo "==> Waiting for database to be ready..."
until docker exec camper-db pg_isready -U postgres > /dev/null 2>&1; do
  sleep 1
done
echo "    Database is ready."

echo "==> Running migrations..."
flyway -configFiles="$DB_DIR/flyway.conf" -locations="filesystem:$DB_DIR/migrations" migrate

echo "==> Seeding dev data..."
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db -f "$DB_DIR/seed/dev_seed.sql"

echo "==> Starting service..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :services:camper-service:bootRun &
SERVICE_PID=$!

echo "==> Starting webapp..."
npm --prefix "$WEBAPP_DIR" run dev &
WEBAPP_PID=$!

echo "==> Waiting for webapp to be ready..."
until curl -s http://localhost:3000 > /dev/null 2>&1; do
  sleep 1
done

echo "==> Opening browser..."
open http://localhost:3000

echo "==> Everything is running. Press Ctrl+C to stop."
wait
