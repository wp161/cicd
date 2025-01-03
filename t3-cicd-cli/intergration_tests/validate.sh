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

# Run CLI commands to validate pipeline
log "Running CICD CLI commands..."
cicd config set --is-repo-remote True
cicd config set --repo https://github.com/wp161/cicd-localrepo.git
cicd config set --branch example-config-files
cicd config set --server http://localhost

VALIDATION_OUTPUT=$(cicd validate 2>&1)
if echo "$VALIDATION_OUTPUT" | grep -i -q "error"; then
    error "Validation failed with output: $VALIDATION_OUTPUT"
    exit 1
fi

log "Pipeline validation completed. Checking database for the pipeline..."

# Get PostgreSQL pod name
log "Fetching PostgreSQL pod ..."
POSTGRES_POD=$(kubectl get pod -n "$DB_NAMESPACE" -l app=postgres -o jsonpath="{.items[0].metadata.name}")

# Execute SQL query to check the pipeline in the database
log "Executing SQL query to verify pipeline existence..."
QUERY_RESULT=$(kubectl exec -n "$DB_NAMESPACE" "$POSTGRES_POD" -- \
    psql -U root -d mydb -t -c "SELECT COUNT(*) FROM pipelines WHERE name='$EXPECTED_PIPELINE';")
PIPELINE_COUNT=$(echo "$QUERY_RESULT" | tr -d '[:space:]')

if [[ "$PIPELINE_COUNT" -gt 0 ]]; then
    success "Pipeline '$EXPECTED_PIPELINE' exists in the database."
else
    error "Pipeline '$EXPECTED_PIPELINE' not found in the database."
    exit 1
fi

success "Integration test completed successfully."
