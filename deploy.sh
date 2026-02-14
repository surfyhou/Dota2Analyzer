#!/usr/bin/env bash
set -euo pipefail

VM_HOST="10.0.1.140"
VM_USER="root"
REPO_URL="$(git remote get-url origin)"
REPO_DIR="Dota2Analyzer"

echo "=== Dota2Analyzer Deploy ==="

# 1. Push latest code
echo "[1/3] Pushing latest code..."
git push

# 2. SSH to VM: clone or pull, then build and start
echo "[2/3] Building and starting on ${VM_HOST}..."
ssh "${VM_USER}@${VM_HOST}" bash -s -- "${REPO_URL}" "${REPO_DIR}" <<'REMOTE'
set -euo pipefail
REPO_URL="$1"
REPO_DIR="$2"

if [ -d "$REPO_DIR" ]; then
    cd "$REPO_DIR"
    git pull
else
    git clone "$REPO_URL" "$REPO_DIR"
    cd "$REPO_DIR"
fi

docker compose up -d --build
REMOTE

# 3. Done
echo "[3/3] Deploy complete!"
echo ""
echo "Access: http://${VM_HOST}:40071"
