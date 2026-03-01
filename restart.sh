#!/usr/bin/env bash
# Full restart: build frontend, copy to static, kill old server, rebuild & start backend.
# Usage: ./restart.sh
set -e

cd "$(dirname "$0")"
source ./configkeys.sh

echo "==> Building frontend..."
(cd frontend && npm run build --silent)

echo "==> Copying frontend dist to static resources..."
rm -rf src/main/resources/static/*
cp -r frontend/dist/* src/main/resources/static/

echo "==> Stopping old server (port 8080)..."
PID=$(lsof -ti:8080 2>/dev/null || true)
if [ -n "$PID" ]; then
  kill "$PID" 2>/dev/null || true
  # Wait up to 5s for graceful shutdown
  for i in $(seq 1 10); do
    if ! lsof -ti:8080 >/dev/null 2>&1; then break; fi
    sleep 0.5
  done
  # Force kill if still alive
  if lsof -ti:8080 >/dev/null 2>&1; then
    kill -9 $(lsof -ti:8080) 2>/dev/null || true
    sleep 1
  fi
fi
echo "    Port 8080 is free."

echo "==> Building and starting Spring Boot..."
nohup ./mvnw spring-boot:run -q > /tmp/tradeintel.log 2>&1 &
BOOT_PID=$!
echo "    PID: $BOOT_PID  (log: /tmp/tradeintel.log)"

echo "==> Waiting for server to be ready..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null -w '' http://localhost:8080/ 2>/dev/null; then
    echo "==> Server is up! (took ~${i}s)"
    exit 0
  fi
  # Check if process died
  if ! kill -0 "$BOOT_PID" 2>/dev/null; then
    echo "==> ERROR: Server process died. Check /tmp/tradeintel.log"
    tail -30 /tmp/tradeintel.log
    exit 1
  fi
  sleep 1
done

echo "==> ERROR: Server did not start within 60s. Check /tmp/tradeintel.log"
tail -30 /tmp/tradeintel.log
exit 1
