#!/bin/bash

# YouthMind Psychological Analysis Services Deployment Script
# Supports blue-green deployment with automated health checks and rollback

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/backend"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
SERVICE_NAME=""
VERSION=""
ENVIRONMENT="dev"
IMAGE_URL=""
CONFIG_FILE=""
DRY_RUN=false
ROLLBACK=false
TARGET_VERSION=""
HEALTH_CHECK_TIMEOUT=30
DEPLOYMENT_TIMEOUT=10

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy YouthMind Psychological Analysis Services

OPTIONS:
    -s, --service SERVICE_NAME    Name of the service to deploy
    -v, --version VERSION         Version to deploy
    -e, --environment ENV         Environment (dev, staging, prod) [default: dev]
    -i, --image IMAGE_URL         Docker image URL
    -c, --config CONFIG_FILE      Configuration file path
    -r, --rollback VERSION        Rollback to specific version
    -d, --dry-run                 Perform dry run without actual deployment
    -t, --timeout TIMEOUT         Health check timeout in seconds [default: 30]
    -h, --help                    Show this help message

EXAMPLES:
    # Deploy psychological analysis service
    $0 -s psychological_analysis -v 1.2.0 -e prod -i youthmind/psychological-analysis:1.2.0

    # Rollback to previous version
    $0 -s psychological_analysis -r 1.1.0 -e prod

    # Dry run deployment
    $0 -s state_update -v 1.3.0 -e staging -d

EOF
}

# Function to validate prerequisites
validate_prerequisites() {
    print_info "Validating prerequisites..."
    
    # Check if backend directory exists
    if [ ! -d "$BACKEND_DIR" ]; then
        print_error "Backend directory not found: $BACKEND_DIR"
        exit 1
    fi
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check if curl is available
    if ! command -v curl &> /dev/null; then
        print_error "curl is not installed or not in PATH"
        exit 1
    fi
    
    print_success "Prerequisites validated"
}

# Function to validate deployment parameters
validate_deployment_params() {
    print_info "Validating deployment parameters..."
    
    if [ -z "$SERVICE_NAME" ]; then
        print_error "Service name is required"
        exit 1
    fi
    
    if [ "$ROLLBACK" = false ] && [ -z "$VERSION" ]; then
        print_error "Version is required for deployment"
        exit 1
    fi
    
    if [ "$ROLLBACK" = true ] && [ -z "$TARGET_VERSION" ]; then
        print_error "Target version is required for rollback"
        exit 1
    fi
    
    # Validate environment
    case "$ENVIRONMENT" in
        dev|staging|prod)
            ;;
        *)
            print_error "Invalid environment: $ENVIRONMENT. Must be dev, staging, or prod"
            exit 1
            ;;
    esac
    
    print_success "Deployment parameters validated"
}

# Function to load configuration
load_configuration() {
    if [ -n "$CONFIG_FILE" ] && [ -f "$CONFIG_FILE" ]; then
        print_info "Loading configuration from: $CONFIG_FILE"
        source "$CONFIG_FILE"
    else
        print_info "Using default configuration"
    fi
}

# Function to build application
build_application() {
    if [ "$ROLLBACK" = true ]; then
        print_info "Skipping build for rollback operation"
        return
    fi
    
    print_info "Building application..."
    
    cd "$BACKEND_DIR"
    
    # Clean and compile
    mvn clean compile -DskipTests -q
    
    if [ $? -eq 0 ]; then
        print_success "Application built successfully"
    else
        print_error "Application build failed"
        exit 1
    fi
    
    cd "$PROJECT_ROOT"
}

# Function to run tests
run_tests() {
    if [ "$ROLLBACK" = true ]; then
        print_info "Skipping tests for rollback operation"
        return
    fi
    
    print_info "Running tests..."
    
    cd "$BACKEND_DIR"
    
    # Run tests
    mvn test -q
    
    if [ $? -eq 0 ]; then
        print_success "Tests passed successfully"
    else
        print_error "Tests failed"
        exit 1
    fi
    
    cd "$PROJECT_ROOT"
}

# Function to start application
start_application() {
    print_info "Starting application..."
    
    cd "$BACKEND_DIR"
    
    # Start application in background
    mvn spring-boot:run -Dspring-boot.run.profiles="$ENVIRONMENT" -DskipTests -q &
    APP_PID=$!
    
    # Wait for application to start
    print_info "Waiting for application to start..."
    sleep 10
    
    # Check if application is running
    if ps -p $APP_PID > /dev/null; then
        print_success "Application started successfully (PID: $APP_PID)"
        echo $APP_PID > "$PROJECT_ROOT/app.pid"
    else
        print_error "Application failed to start"
        exit 1
    fi
    
    cd "$PROJECT_ROOT"
}

# Function to perform health check
perform_health_check() {
    print_info "Performing health check..."
    
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        print_info "Health check attempt $attempt/$max_attempts"
        
        # Check application health
        local health_response=$(curl -s -w "%{http_code}" -o /dev/null "http://localhost:8080/actuator/health" || echo "000")
        
        if [ "$health_response" = "200" ]; then
            print_success "Health check passed"
            return 0
        fi
        
        print_warning "Health check failed (HTTP: $health_response), retrying in 5 seconds..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    print_error "Health check failed after $max_attempts attempts"
    return 1
}

# Function to deploy service
deploy_service() {
    print_info "Deploying service: $SERVICE_NAME version: $VERSION"
    
    # Prepare deployment request
    local deployment_request=$(cat << EOF
{
    "serviceName": "$SERVICE_NAME",
    "version": "$VERSION",
    "imageUrl": "$IMAGE_URL",
    "environment": "$ENVIRONMENT",
    "strategy": "BLUE_GREEN"
}
EOF
)
    
    # Deploy service via API
    local deploy_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$deployment_request" \
        "http://localhost:8080/v1/deployment/services/$SERVICE_NAME/deploy")
    
    if echo "$deploy_response" | grep -q '"status":"SUCCESS"'; then
        print_success "Service deployed successfully"
        return 0
    else
        print_error "Service deployment failed: $deploy_response"
        return 1
    fi
}

# Function to rollback service
rollback_service() {
    print_info "Rolling back service: $SERVICE_NAME to version: $TARGET_VERSION"
    
    # Rollback service via API
    local rollback_response=$(curl -s -X POST \
        "http://localhost:8080/v1/deployment/services/$SERVICE_NAME/rollback?targetVersion=$TARGET_VERSION")
    
    if echo "$rollback_response" | grep -q '"success":true'; then
        print_success "Service rolled back successfully"
        return 0
    else
        print_error "Service rollback failed: $rollback_response"
        return 1
    fi
}

# Function to validate deployment
validate_deployment() {
    print_info "Validating deployment..."
    
    # Validate service health
    local validation_response=$(curl -s -X POST \
        "http://localhost:8080/v1/deployment/services/$SERVICE_NAME/validate")
    
    if echo "$validation_response" | grep -q '"isHealthy":true'; then
        print_success "Deployment validation passed"
        return 0
    else
        print_error "Deployment validation failed: $validation_response"
        return 1
    fi
}

# Function to get deployment status
get_deployment_status() {
    print_info "Getting deployment status..."
    
    local status_response=$(curl -s \
        "http://localhost:8080/v1/deployment/services/$SERVICE_NAME/status")
    
    echo "Deployment Status: $status_response"
}

# Function to cleanup
cleanup() {
    print_info "Cleaning up..."
    
    # Stop application if running
    if [ -f "$PROJECT_ROOT/app.pid" ]; then
        local app_pid=$(cat "$PROJECT_ROOT/app.pid")
        if ps -p $app_pid > /dev/null; then
            print_info "Stopping application (PID: $app_pid)"
            kill $app_pid
        fi
        rm -f "$PROJECT_ROOT/app.pid"
    fi
}

# Function to handle errors
handle_error() {
    print_error "Deployment failed at line $1"
    cleanup
    exit 1
}

# Set error trap
trap 'handle_error $LINENO' ERR

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--service)
            SERVICE_NAME="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -i|--image)
            IMAGE_URL="$2"
            shift 2
            ;;
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -r|--rollback)
            ROLLBACK=true
            TARGET_VERSION="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -t|--timeout)
            HEALTH_CHECK_TIMEOUT="$2"
            shift 2
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Main execution
main() {
    print_info "Starting YouthMind Psychological Analysis Services Deployment"
    print_info "Service: $SERVICE_NAME"
    print_info "Environment: $ENVIRONMENT"
    
    if [ "$ROLLBACK" = true ]; then
        print_info "Target Version: $TARGET_VERSION"
    else
        print_info "Version: $VERSION"
    fi
    
    if [ "$DRY_RUN" = true ]; then
        print_warning "DRY RUN MODE - No actual deployment will be performed"
    fi
    
    # Load configuration
    load_configuration
    
    # Validate prerequisites
    validate_prerequisites
    
    # Validate deployment parameters
    validate_deployment_params
    
    if [ "$DRY_RUN" = true ]; then
        print_success "Dry run completed successfully"
        exit 0
    fi
    
    # Build application
    build_application
    
    # Run tests
    run_tests
    
    # Start application
    start_application
    
    # Perform health check
    if ! perform_health_check; then
        print_error "Application health check failed"
        cleanup
        exit 1
    fi
    
    # Deploy or rollback service
    if [ "$ROLLBACK" = true ]; then
        if ! rollback_service; then
            print_error "Service rollback failed"
            cleanup
            exit 1
        fi
    else
        if ! deploy_service; then
            print_error "Service deployment failed"
            cleanup
            exit 1
        fi
    fi
    
    # Validate deployment
    if ! validate_deployment; then
        print_error "Deployment validation failed"
        cleanup
        exit 1
    fi
    
    # Get deployment status
    get_deployment_status
    
    print_success "Deployment completed successfully!"
    
    # Keep application running
    print_info "Application is running. Press Ctrl+C to stop."
    wait
}

# Run main function
main "$@"
