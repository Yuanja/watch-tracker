#!/usr/bin/env bash
# Full restart: build frontend, copy to static, package JAR, restart systemd service.
# Usage: ./restart.sh
set -e

cd "$(dirname "$0")"
source ./configkeys.sh

echo "==> Building frontend..."
(cd frontend && npm run build --silent)

echo "==> Copying frontend dist to static resources..."
rm -rf src/main/resources/static/*
cp -r frontend/dist/* src/main/resources/static/

echo "==> Packaging JAR..."
./mvnw package -DskipTests -q

echo "==> Restarting tradeintel service..."
sudo systemctl restart tradeintel

echo "==> Waiting for server to be ready..."
for i in $(seq 1 180); do
  if curl -s -o /dev/null -w '' http://localhost:8080/ 2>/dev/null; then
    echo "==> Server is up! (took ~${i}s)"
    exit 0
  fi
  sleep 1
done

echo "==> ERROR: Server did not start within 180s."
sudo journalctl -u tradeintel --since "3 min ago" --no-pager | tail -30
exit 1
