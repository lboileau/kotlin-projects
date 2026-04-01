#!/usr/bin/env bash
set -euo pipefail

# Full dev environment restart: kill stale processes, clean build, fresh DB, start everything.
# Usage: ./run-dev.sh [--skip-build] [--keep-db]
#
# Options:
#   --skip-build   Skip gradlew clean build (use existing build artifacts)
#   --keep-db      Skip flyway clean (preserve existing data, just run pending migrations)

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_DIR="$ROOT_DIR/databases/camper-db"
WEBAPP_DIR="$ROOT_DIR/webapp"

SKIP_BUILD=false
KEEP_DB=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --keep-db)    KEEP_DB=true ;;
  esac
done

# ── 1. Kill stale processes (port-based, not name-based — works across worktrees) ──

echo "==> Killing existing processes on :8080 and :3000..."
for port in 8080 3000; do
  pids=$(lsof -i :"$port" -t 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "$pids" | xargs kill -9 2>/dev/null || true
    echo "    Killed process(es) on :$port"
  fi
done
sleep 1  # Let ports release

# ── 2. Database ──

echo "==> Starting database..."
docker compose -f "$DB_DIR/docker-compose.yml" up -d

echo "==> Waiting for database to be ready..."
until docker exec camper-db pg_isready -U postgres > /dev/null 2>&1; do
  sleep 1
done
echo "    Database is ready."

# ── 3. Migrations ──

if [ "$KEEP_DB" = false ]; then
  echo "==> Cleaning database (fresh schema)..."
  flyway -configFiles="$DB_DIR/flyway.conf" \
         -locations="filesystem:$DB_DIR/migrations" \
         -cleanDisabled=false clean
fi

echo "==> Running migrations..."
flyway -configFiles="$DB_DIR/flyway.conf" \
       -locations="filesystem:$DB_DIR/migrations" \
       migrate

echo "==> Seeding dev data..."
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db \
  -f "$DB_DIR/seed/dev_seed.sql" > /dev/null

# ── 4. Build ──

if [ "$SKIP_BUILD" = false ]; then
  echo "==> Clean build (skipping tests)..."
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" clean build -x test
else
  echo "==> Skipping build (--skip-build)"
fi

# ── 5. Start services ──

cleanup() {
  echo ""
  echo "==> Shutting down..."
  [ -n "${SERVICE_PID:-}" ] && kill "$SERVICE_PID" 2>/dev/null
  [ -n "${WEBAPP_PID:-}" ] && kill "$WEBAPP_PID" 2>/dev/null
  wait 2>/dev/null
  echo "    Done."
}
trap cleanup EXIT

echo "==> Starting service..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :services:camper-service:bootRun &
SERVICE_PID=$!

echo "==> Starting webapp..."
npm --prefix "$WEBAPP_DIR" run dev &
WEBAPP_PID=$!

# ── 6. Health checks ──

echo "==> Waiting for API (port 8080)..."
for i in $(seq 1 60); do
  if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "    API is ready."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "    ERROR: API failed to start within 60s"
    exit 1
  fi
  sleep 1
done

echo "==> Waiting for webapp (port 3000)..."
for i in $(seq 1 30); do
  if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo "    Webapp is ready."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "    ERROR: Webapp failed to start within 30s"
    exit 1
  fi
  sleep 1
done

# ── 7. Done ──

echo ""
echo "==> Everything is running!"
echo "    API:    http://localhost:8080"
echo "    Webapp: http://localhost:3000"
echo ""
echo "    Press Ctrl+C to stop all services."

open http://localhost:3000
wait
