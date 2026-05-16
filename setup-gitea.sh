#!/bin/bash
set -euo pipefail

GITEA_URL="http://localhost:3000"
ADMIN_USER="gitea-admin"
ADMIN_PASS="password123"
ADMIN_EMAIL="admin@example.com"
REPO_NAME="aidd-demo"
RUNNER_TOKEN="workshop-runner-token-2024"
RUNNER_VERSION="0.2.12"
RUNNER_DIR="${HOME}/.act_runner"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Gitea CI Setup ==="
echo ""

# --------------------------------------------------
# Step 1: Wait for Gitea
# --------------------------------------------------
echo "[1/7] Waiting for Gitea to be ready..."
until curl -sf "${GITEA_URL}/api/v1/version" > /dev/null 2>&1; do
  sleep 2
done
echo "  Gitea is ready."

# --------------------------------------------------
# Step 2: Initial setup (create admin user)
# --------------------------------------------------
echo "[2/7] Configuring Gitea..."
if curl -sf "${GITEA_URL}/api/v1/user" -u "${ADMIN_USER}:${ADMIN_PASS}" > /dev/null 2>&1; then
  echo "  Already configured, skipping."
else
  curl -sf "${GITEA_URL}" -X POST \
    -d "db_type=SQLite3" \
    -d "db_host=localhost:3306" \
    -d "db_user=root" \
    -d "db_passwd=" \
    -d "db_name=gitea" \
    -d "ssl_mode=disable" \
    -d "db_path=/data/gitea/gitea.db" \
    -d "app_name=Gitea" \
    -d "repo_root_path=/data/git/repositories" \
    -d "lfs_root_path=/data/git/lfs" \
    -d "run_user=git" \
    -d "domain=localhost" \
    -d "ssh_port=22" \
    -d "http_port=3000" \
    -d "app_url=${GITEA_URL}/" \
    -d "log_root_path=/data/gitea/log" \
    -d "admin_name=${ADMIN_USER}" \
    -d "admin_passwd=${ADMIN_PASS}" \
    -d "admin_confirm_passwd=${ADMIN_PASS}" \
    -d "admin_email=${ADMIN_EMAIL}" > /dev/null
  echo "  Initial setup complete."
  sleep 3
  until curl -sf "${GITEA_URL}/api/v1/version" > /dev/null 2>&1; do
    sleep 2
  done
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
