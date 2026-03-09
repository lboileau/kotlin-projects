#!/usr/bin/env bash
set -euo pipefail

# Deploy camper to Railway
# Usage: ./deploy.sh

cd "$(dirname "$0")"

echo "==> Checking Railway CLI..."
if ! command -v railway &>/dev/null; then
  echo "Error: railway CLI not found. Install with: brew install railway"
  exit 1
fi

echo "==> Verifying Railway project link..."
railway status || { echo "Error: not linked to a Railway project. Run: railway link"; exit 1; }

echo "==> Running full build locally to catch errors early..."
./gradlew clean build --no-daemon

echo "==> Deploying to Railway..."
railway up

echo "==> Deploy complete!"
