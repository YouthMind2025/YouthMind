#!/bin/bash

# Configuration helper for YouthMind Recommendation Engine Validation Suite

# Load environment variables from .env file if it exists
if [[ -f "$SCRIPT_DIR/../.env" ]]; then
    source "$SCRIPT_DIR/../.env"
    log_info "Loaded configuration from .env file"
fi

# Set default values
BASE_URL="${BASE_URL:-http://localhost:8080}"
TOK="${TOK:-}"
ADMIN_TOK="${ADMIN_TOK:-}"
USER_ID="${USER_ID:-test-user-$(date +%s)}"

# Validate required configuration
if [[ -z "$TOK" ]]; then
    log_warning "TOK (user token) not set - some API tests may fail"
fi

if [[ -z "$ADMIN_TOK" ]]; then
    log_warning "ADMIN_TOK (admin token) not set - admin endpoint tests will be skipped"
fi

# Export configuration
export BASE_URL TOK ADMIN_TOK USER_ID

log_info "Configuration loaded:"
log_info "  BASE_URL: $BASE_URL"
log_info "  TOK: ${TOK:+[SET]}${TOK:-[NOT SET]}"
log_info "  ADMIN_TOK: ${ADMIN_TOK:+[SET]}${ADMIN_TOK:-[NOT SET]}"
log_info "  USER_ID: $USER_ID"
