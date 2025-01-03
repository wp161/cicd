#!/bin/bash

set -e
trap 'error "An unexpected error occurred. Exiting."' ERR

DB_NAMESPACE="t3cicdbackend-datastore"
EXPECTED_PIPELINE="example-pipeline"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
RESET='\033[0m'

function log() {
    echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${RESET}"
}

function success() {
    echo -e "${GREEN}✔ $1${RESET}"
}

function warning() {
    echo -e "${YELLOW}⚠ $1${RESET}"
}

function error() {
    echo -e "${RED}✘ $1${RESET}"
}

# Run CLI commands to run pipeline
log "Running CICD CLI commands..."
cicd config set --is-repo-remote True
cicd config set --repo https://github.com/wp161/cicd-localrepo.git
cicd config set --branch example-config-files
cicd config set --server http://localhost

log "Executing the pipeline, this may take a while..."

PIPELINE_OUTPUT=$(cicd run --pipeline "$EXPECTED_PIPELINE" 2>&1)
if echo "$PIPELINE_OUTPUT" | grep -i -q -E "error|uncommitted changes"; then
    error "Pipeline execution failed with output: $PIPELINE_OUTPUT"
    exit 1
fi

log "Pipeline run completed. Checking database for the pipeline..."
# Get PostgreSQL pod name
log "Fetching PostgreSQL pod..."
POSTGRES_POD=$(kubectl get pod -n "$DB_NAMESPACE" -l app=postgres -o jsonpath="{.items[0].metadata.name}")

# Check pipeline existence and log status
log "Executing SQL query to verify pipeline existence and status..."
QUERY="WITH latest_pipeline AS (
           SELECT id FROM pipelines WHERE name='$EXPECTED_PIPELINE' ORDER BY id DESC LIMIT 1
       )
       SELECT COUNT(*) FROM argo_logs WHERE pipeline_id=(SELECT id FROM latest_pipeline) AND status!='COMPLETED';"

QUERY_RESULT=$(kubectl exec -n "$DB_NAMESPACE" "$POSTGRES_POD" -- \
    psql -U root -d mydb -t -c "$QUERY" | tr -d '[:space:]')

# Validate the result
if [[ "$QUERY_RESULT" == "0" ]]; then
    success "All Argo log entries for pipeline '$EXPECTED_PIPELINE' have the status: COMPLETED."
else
    error "Some log entries for pipeline '$EXPECTED_PIPELINE' do not have the status: COMPLETED. Non-completed entries count: $QUERY_RESULT"
    exit 1
fi

success "Integration test completed successfully."