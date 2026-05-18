#!/usr/bin/env bash
set -euo pipefail

SONAR_URL="http://sonarqube:9000"
SONAR_DEFAULT_PASSWORD="admin"
SONAR_NEW_PASSWORD="${SONAR_ADMIN_PASSWORD:-sonarpass}"
GATE_NAME="CI Gate"

log() { echo "[setup-sonarqube] $*" >&2; }

wait_for_sonarqube() {
  log "Waiting for SonarQube to start..."
  for i in $(seq 1 60); do
    status=$(curl -s -f "$SONAR_URL/api/system/status" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null || true)
    if [ "$status" = "UP" ]; then
      log "SonarQube is up."
      return 0
    fi
    sleep 5
  done
  log "ERROR: SonarQube did not start within 5 minutes."
  exit 1
}

api() {
  local method=$1 endpoint=$2
  shift 2
  curl -s -f -u "admin:${SONAR_NEW_PASSWORD}" -X "$method" "${SONAR_URL}${endpoint}" "$@"
}

change_default_password() {
  if curl -s -f -u "admin:${SONAR_NEW_PASSWORD}" "$SONAR_URL/api/qualitygates/list" >/dev/null 2>&1; then
    log "Password already changed."
    return 0
  fi

  log "Changing default admin password..."
  curl -s -f -u "admin:${SONAR_DEFAULT_PASSWORD}" -X POST \
    "$SONAR_URL/api/users/change_password" \
    -d "login=admin&previousPassword=${SONAR_DEFAULT_PASSWORD}&password=${SONAR_NEW_PASSWORD}"
  log "Password changed."
}

create_quality_gate() {
  existing=$(api GET "/api/qualitygates/list" | python3 -c "
import sys, json
gates = json.load(sys.stdin).get('qualitygates', [])
print(next((str(g['id']) for g in gates if g['name'] == '${GATE_NAME}'), ''))
" 2>/dev/null || true)

  if [ -n "$existing" ]; then
    log "Quality Gate '${GATE_NAME}' already exists (id=$existing)."
    echo "$existing"
    return 0
  fi

  log "Creating Quality Gate '${GATE_NAME}'..."
  gate_id=$(api POST "/api/qualitygates/create" -d "name=${GATE_NAME}" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  log "Created Quality Gate (id=$gate_id)."
  echo "$gate_id"
}

add_condition() {
  local gate_id=$1 metric=$2 op=$3 error=$4
  api POST "/api/qualitygates/create_condition" \
    -d "gateId=${gate_id}&metric=${metric}&op=${op}&error=${error}" >/dev/null 2>&1 || true
}

setup_conditions() {
  local gate_id=$1

  existing_count=$(api GET "/api/qualitygates/show?id=${gate_id}" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(len(data.get('conditions', [])))
" 2>/dev/null || echo "0")

  if [ "$existing_count" -gt "0" ]; then
    log "Quality Gate already has $existing_count condition(s), skipping."
    return 0
  fi

  log "Adding conditions to Quality Gate..."
  # SonarQube automatically adds CAYC conditions (reliability/security/maintainability ratings, security hotspots)
  add_condition "$gate_id" "new_coverage"              "LT" "80"
  add_condition "$gate_id" "new_duplicated_lines_density" "GT" "3"
  log "Conditions added."
}

set_default_gate() {
  local gate_id=$1
  log "Setting Quality Gate as default..."
  api POST "/api/qualitygates/set_as_default" -d "id=${gate_id}" >/dev/null
  log "Quality Gate set as default."
}

main() {
  wait_for_sonarqube
  change_default_password
  gate_id=$(create_quality_gate)
  setup_conditions "$gate_id"
  set_default_gate "$gate_id"
  log "SonarQube setup complete."
}

main
