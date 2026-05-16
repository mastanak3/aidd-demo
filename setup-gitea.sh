#!/bin/bash
set -euo pipefail

GITEA_URL="http://localhost:3000"
ADMIN_USER="gitea-admin"
ADMIN_PASS="password123"
ADMIN_EMAIL="admin@example.com"
REPO_NAME="aidd-demo"
RUNNER_TOKEN="workshop-runner-token-2024"

GITEA_VERSION="1.23.0"
RUNNER_VERSION="0.2.12"
GITEA_DIR="${HOME}/.gitea"
RUNNER_DIR="${HOME}/.act_runner"
PG_DIR="${HOME}/.postgresql"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
MAX_WAIT=120

echo "=== Gitea CI Setup ==="
echo ""

# --------------------------------------------------
# Step 0: Start PostgreSQL
# --------------------------------------------------
echo "[0/7] Setting up PostgreSQL..."
if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
  echo "  PostgreSQL already running."
else
  mkdir -p "${PG_DIR}/data" "${PG_DIR}/log"
  if [ ! -f "${PG_DIR}/data/PG_VERSION" ]; then
    echo "  Initializing database..."
    initdb -D "${PG_DIR}/data" --auth=trust --no-locale --encoding=UTF8 > /dev/null 2>&1
  fi
  echo "  Starting PostgreSQL..."
  pg_ctl -D "${PG_DIR}/data" -l "${PG_DIR}/log/postgresql.log" -o "-p 5432 -k /tmp" start > /dev/null 2>&1
  sleep 2
  if ! psql -h localhost -p 5432 -d postgres -c "SELECT 1 FROM pg_roles WHERE rolname='library'" -tA 2>/dev/null | grep -q 1; then
    createuser -h localhost -p 5432 library 2>/dev/null || true
    createdb -h localhost -p 5432 -O library library 2>/dev/null || true
    psql -h localhost -p 5432 -d postgres -c "ALTER USER library PASSWORD 'library';" > /dev/null 2>&1
  fi
  echo "  PostgreSQL ready (library/library@localhost:5432/library)."
fi

# --------------------------------------------------
# Step 1: Download and start Gitea
# --------------------------------------------------
echo "[1/7] Setting up Gitea server..."
mkdir -p "${GITEA_DIR}/data" "${GITEA_DIR}/custom/conf" "${GITEA_DIR}/repositories" "${GITEA_DIR}/log"

GITEA_BIN="${GITEA_DIR}/gitea"
if [ ! -f "${GITEA_BIN}" ]; then
  echo "  Downloading Gitea v${GITEA_VERSION}..."
  curl -sL "https://dl.gitea.com/gitea/${GITEA_VERSION}/gitea-${GITEA_VERSION}-linux-amd64" \
    -o "${GITEA_BIN}"
  chmod +x "${GITEA_BIN}"
  echo "  Downloaded."
else
  echo "  Gitea binary already exists."
fi

cat > "${GITEA_DIR}/custom/conf/app.ini" << INIEOF
[server]
HTTP_PORT = 3000
HTTP_ADDR = 0.0.0.0
ROOT_URL = http://localhost:3000/

[database]
DB_TYPE = sqlite3
PATH = ${GITEA_DIR}/data/gitea.db

[repository]
ROOT = ${GITEA_DIR}/repositories

[lfs]
PATH = ${GITEA_DIR}/data/lfs

[log]
ROOT_PATH = ${GITEA_DIR}/log

[security]
INSTALL_LOCK = true

[actions]
ENABLED = true
INIEOF

if pgrep -f "gitea.*web" > /dev/null 2>&1; then
  echo "  Gitea already running."
else
  echo "  Starting Gitea..."
  cd "${GITEA_DIR}"
  GITEA_WORK_DIR="${GITEA_DIR}" nohup "${GITEA_BIN}" web -c "${GITEA_DIR}/custom/conf/app.ini" > "${GITEA_DIR}/log/gitea.log" 2>&1 &
  cd "${PROJECT_DIR}"
fi

echo "  Waiting for Gitea to be ready (timeout: ${MAX_WAIT}s)..."
ELAPSED=0
until curl -sf "${GITEA_URL}/api/v1/version" > /dev/null 2>&1; do
  if [ "$ELAPSED" -ge "$MAX_WAIT" ]; then
    echo "  ERROR: Gitea did not start within ${MAX_WAIT}s."
    echo "  Check logs: ${GITEA_DIR}/log/gitea.log"
    exit 1
  fi
  sleep 3
  ELAPSED=$((ELAPSED + 3))
done
echo "  Gitea is ready."

# --------------------------------------------------
# Step 2: Create admin user
# --------------------------------------------------
echo "[2/7] Creating admin user..."
if curl -sf "${GITEA_URL}/api/v1/user" -u "${ADMIN_USER}:${ADMIN_PASS}" > /dev/null 2>&1; then
  echo "  Admin user already exists, skipping."
else
  cd "${GITEA_DIR}"
  "${GITEA_BIN}" admin user create \
    --username "${ADMIN_USER}" \
    --password "${ADMIN_PASS}" \
    --email "${ADMIN_EMAIL}" \
    --admin \
    --must-change-password=false \
    -c "${GITEA_DIR}/custom/conf/app.ini" 2>/dev/null
  cd "${PROJECT_DIR}"
  echo "  Admin user created."
fi

# --------------------------------------------------
# Step 3: Create API token
# --------------------------------------------------
echo "[3/7] Creating API token..."
TOKEN_RESPONSE=$(curl -sf "${GITEA_URL}/api/v1/users/${ADMIN_USER}/tokens" \
  -u "${ADMIN_USER}:${ADMIN_PASS}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"setup-token-$(date +%s)\",\"scopes\":[\"all\"]}")
API_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('sha1', d.get('token', '')))" 2>/dev/null)
if [ -z "$API_TOKEN" ]; then
  echo "  ERROR: Failed to create API token."
  exit 1
fi
echo "  API token created."

# --------------------------------------------------
# Step 4: Create repository
# --------------------------------------------------
echo "[4/7] Creating repository..."
REPO_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
  "${GITEA_URL}/api/v1/repos/${ADMIN_USER}/${REPO_NAME}" \
  -H "Authorization: token ${API_TOKEN}" 2>/dev/null || echo "000")
if [ "$REPO_STATUS" = "200" ]; then
  echo "  Repository already exists, skipping."
else
  curl -sf "${GITEA_URL}/api/v1/user/repos" \
    -H "Authorization: token ${API_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${REPO_NAME}\",\"auto_init\":false,\"default_branch\":\"main\"}" > /dev/null
  echo "  Repository created: ${ADMIN_USER}/${REPO_NAME}"
fi

# --------------------------------------------------
# Step 5: Enable Actions on the repository
# --------------------------------------------------
echo "[5/7] Enabling Actions on repository..."
curl -sf "${GITEA_URL}/api/v1/repos/${ADMIN_USER}/${REPO_NAME}" \
  -X PATCH \
  -H "Authorization: token ${API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"has_actions":true}' > /dev/null
echo "  Actions enabled."

# --------------------------------------------------
# Step 6: Setup act_runner
# --------------------------------------------------
echo "[6/7] Setting up act_runner..."
mkdir -p "${RUNNER_DIR}"

RUNNER_BIN="${RUNNER_DIR}/act_runner"
if [ ! -f "${RUNNER_BIN}" ]; then
  echo "  Downloading act_runner v${RUNNER_VERSION}..."
  curl -sL "https://dl.gitea.com/act_runner/${RUNNER_VERSION}/act_runner-${RUNNER_VERSION}-linux-amd64" \
    -o "${RUNNER_BIN}"
  chmod +x "${RUNNER_BIN}"
  echo "  Downloaded."
else
  echo "  act_runner already downloaded."
fi

if [ ! -f "${RUNNER_DIR}/.runner" ]; then
  echo "  Registering runner..."
  cd "${RUNNER_DIR}"
  "${RUNNER_BIN}" register --no-interactive \
    --instance "${GITEA_URL}" \
    --token "${RUNNER_TOKEN}" \
    --name "devspaces-runner" \
    --labels "ubuntu-latest:host"
  cd "${PROJECT_DIR}"
  echo "  Runner registered."
else
  echo "  Runner already registered."
fi

if pgrep -f "act_runner.*daemon" > /dev/null 2>&1; then
  echo "  Runner daemon already running."
else
  echo "  Starting runner daemon..."
  cd "${RUNNER_DIR}"
  nohup "${RUNNER_BIN}" daemon > "${RUNNER_DIR}/runner.log" 2>&1 &
  cd "${PROJECT_DIR}"
  echo "  Runner daemon started (PID: $!)."
fi

# --------------------------------------------------
# Step 7: Configure git remote and push
# --------------------------------------------------
echo "[7/7] Pushing code to Gitea..."
cd "${PROJECT_DIR}"

git config credential.helper store
echo "http://${ADMIN_USER}:${ADMIN_PASS}@localhost:3000" > ~/.git-credentials

GITEA_REPO_URL="${GITEA_URL}/${ADMIN_USER}/${REPO_NAME}.git"

CURRENT_ORIGIN=$(git remote get-url origin 2>/dev/null || echo "")
if [ -n "$CURRENT_ORIGIN" ] && [ "$CURRENT_ORIGIN" != "$GITEA_REPO_URL" ]; then
  git remote set-url origin "$GITEA_REPO_URL"
  echo "  origin updated to Gitea."
elif [ -z "$CURRENT_ORIGIN" ]; then
  git remote add origin "$GITEA_REPO_URL"
  echo "  origin added."
fi

git push -u origin main 2>&1 || git push -u origin HEAD:main 2>&1
echo "  Code pushed."

echo ""
echo "========================================"
echo "  Gitea CI Setup Complete!"
echo "========================================"
echo ""
echo "  Gitea:      ${GITEA_URL}"
echo "  Repository: ${GITEA_URL}/${ADMIN_USER}/${REPO_NAME}"
echo "  Actions:    ${GITEA_URL}/${ADMIN_USER}/${REPO_NAME}/actions"
echo "  User:       ${ADMIN_USER} / ${ADMIN_PASS}"
echo ""
echo "  Push changes to trigger CI:"
echo "    git push origin main"
echo ""
