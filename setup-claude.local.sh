#!/bin/bash
# GCP Vertex AI Setup Helper Script
# DevContainer内で実行してください
#
# Usage:
#   export GCP_PROJECT_ID=your-project-id  # 事前に環境変数を設定
#   source ./setup-claude.local.sh
#
# または引数で指定:
#   source ./setup-claude.local.sh your-project-id
#
# NOTE: `source` で実行してください。`./setup-claude.local.sh` だとサブシェルで実行され、
#       環境変数が現在のシェルに反映されません。

set -e

# プロジェクトIDを引数または環境変数から取得
if [ -n "$1" ]; then
    GCP_PROJECT_ID="$1"
elif [ -z "$GCP_PROJECT_ID" ]; then
    echo "ERROR: GCP_PROJECT_ID is not set."
    echo ""
    echo "Please set GCP_PROJECT_ID before running this script:"
    echo "  export GCP_PROJECT_ID=your-project-id"
    echo "  source ./setup-claude.local.sh"
    echo ""
    echo "Or provide it as an argument:"
    echo "  source ./setup-claude.local.sh your-project-id"
    echo ""
    return 1 2>/dev/null || exit 1
fi

echo ""
echo "========================================"
echo "  GCP Vertex AI Setup for Claude Code"
echo "========================================"
echo ""

# Step 1: GCloud Init
echo "Step 1/4: GCloud Initialization"
echo "----------------------------------------"
echo ""
echo "Using project ID: ${GCP_PROJECT_ID}"
echo ""
echo "Setting project configuration..."
gcloud config set project "${GCP_PROJECT_ID}"

echo ""
echo "✓ GCloud initialized"
echo ""

# Step 2: Application Default Credentials
echo "Step 2/4: Application Default Authentication"
echo "----------------------------------------"
echo ""
echo "This will authenticate you via browser."
echo ""

gcloud auth application-default login

echo ""
echo "✓ Application default authentication complete"
echo ""

# Step 3: Set Quota Project
echo "Step 3/4: Setting Quota Project"
echo "----------------------------------------"
echo ""
echo "Setting quota project to: REDACTED-QUOTA-PROJECT"
echo ""

gcloud auth application-default set-quota-project REDACTED-QUOTA-PROJECT

echo ""
echo "✓ Quota project set"
echo ""

# Step 4: Environment Variables
echo "Step 4/4: Configuring Environment Variables"
echo "----------------------------------------"
echo ""
echo "Using GCP Project ID: ${GCP_PROJECT_ID}"
echo ""

# Add to .bashrc if not already present
SHELL_RC="${HOME}/.bashrc"

add_to_rc() {
    local var_name=$1
    local var_value=$2
    local export_line="export ${var_name}=${var_value}"

    if ! grep -q "^export ${var_name}=" "${SHELL_RC}" 2>/dev/null; then
        echo "${export_line}" >> "${SHELL_RC}"
        echo "  Added ${var_name} to ${SHELL_RC}"
    else
        echo "  ${var_name} already in ${SHELL_RC}"
    fi
}

add_to_rc "GCP_PROJECT_ID" "${GCP_PROJECT_ID}"
add_to_rc "CLAUDE_CODE_USE_VERTEX" "1"
add_to_rc "CLOUD_ML_REGION" "us-east5"
add_to_rc "ANTHROPIC_VERTEX_PROJECT_ID" "\${GCP_PROJECT_ID}"

echo ""
echo "✓ Environment variables configured"
echo ""

# Apply configuration to current shell
# (`. ~/.bashrc` is skipped by non-interactive shell guard, so export directly)
export GCP_PROJECT_ID="${GCP_PROJECT_ID}"
export CLAUDE_CODE_USE_VERTEX=1
export CLOUD_ML_REGION=us-east5
export ANTHROPIC_VERTEX_PROJECT_ID="${GCP_PROJECT_ID}"

# Summary
echo ""
echo "========================================"
echo "  Setup Complete!"
echo "========================================"
echo ""
echo "Configuration:"
echo "  GCP_PROJECT_ID: ${GCP_PROJECT_ID}"
echo "  CLAUDE_CODE_USE_VERTEX: ${CLAUDE_CODE_USE_VERTEX}"
echo "  CLOUD_ML_REGION: ${CLOUD_ML_REGION}"
echo "  ANTHROPIC_VERTEX_PROJECT_ID: ${ANTHROPIC_VERTEX_PROJECT_ID}"
echo ""
echo "NOTE: Run 'source ~/.bashrc' or open a new terminal"
echo "      to use these variables in other shells."
echo ""
echo "You're ready to use Claude Code with Vertex AI!"
echo ""