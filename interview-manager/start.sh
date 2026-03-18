#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
DB_DIR="$PROJECT_ROOT/databases/interview-manager-db"

cleanup() {
    echo ""
    echo "Shutting down..."
    if [ -n "${SERVICE_PID:-}" ]; then
        kill "$SERVICE_PID" 2>/dev/null || true
        wait "$SERVICE_PID" 2>/dev/null || true
    fi
    echo "Done. Run ./stop.sh to also stop the database."
}
trap cleanup EXIT

# 1. Start database
echo "Starting database..."
cd "$DB_DIR" && docker compose up -d && cd "$PROJECT_ROOT"

# 2. Wait for database readiness
echo "Waiting for database..."
until PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d interview_manager_db -c "SELECT 1" > /dev/null 2>&1; do
    sleep 1
done
echo "Database ready."

# 3. Run Flyway migrations
echo "Running migrations..."
flyway -configFiles="$DB_DIR/flyway.conf" migrate

# 4. Seed dev data
echo "Seeding dev data..."
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d interview_manager_db -f "$DB_DIR/seed/dev_seed.sql"

# 5. Start API service in background
echo "Starting interview-service on port 8080..."
cd "$PROJECT_ROOT"
./gradlew :services:interview-service:bootRun &
SERVICE_PID=$!

# 6. Wait for service readiness
echo "Waiting for service..."
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 2
done
echo "Service ready at http://localhost:8080"

# 7. Open browser
if command -v open &> /dev/null; then
    open http://localhost:8080/api/worlds
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:8080/api/worlds
fi

echo "Press Ctrl+C to stop."
wait
