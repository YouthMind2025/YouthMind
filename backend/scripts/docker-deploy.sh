#!/bin/bash

# YouthMind Psychological Analysis Services Docker Deployment Script
# Supports containerized deployment with Docker Compose

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
DOCKER_DIR="$PROJECT_ROOT/docker"

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
BUILD_IMAGE=false
PUSH_IMAGE=false
REGISTRY_URL=""
COMPOSE_FILE=""
SCALE_INSTANCES=1
HEALTH_CHECK_TIMEOUT=60

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

Deploy YouthMind Psychological Analysis Services using Docker

OPTIONS:
    -s, --service SERVICE_NAME    Name of the service to deploy
    -v, --version VERSION         Version to deploy
    -e, --environment ENV         Environment (dev, staging, prod) [default: dev]
    -b, --build                   Build Docker image
    -p, --push                    Push Docker image to registry
    -r, --registry REGISTRY_URL   Docker registry URL
    -f, --compose-file FILE       Docker Compose file path
    -n, --scale INSTANCES         Number of instances to scale [default: 1]
    -t, --timeout TIMEOUT         Health check timeout in seconds [default: 60]
    -h, --help                    Show this help message

EXAMPLES:
    # Deploy with Docker Compose
    $0 -s psychological_analysis -v 1.2.0 -e prod -f docker-compose.prod.yml

    # Build and deploy
    $0 -s state_update -v 1.3.0 -e staging -b -f docker-compose.staging.yml

    # Build, push and deploy
    $0 -s mood_analysis -v 1.1.0 -e prod -b -p -r registry.youthmind.com

EOF
}

# Function to validate prerequisites
validate_prerequisites() {
    print_info "Validating prerequisites..."
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    # Check if project root exists
    if [ ! -d "$PROJECT_ROOT" ]; then
        print_error "Project root directory not found: $PROJECT_ROOT"
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
    
    if [ -z "$VERSION" ]; then
        print_error "Version is required"
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

# Function to create Docker Compose file
create_docker_compose_file() {
    if [ -n "$COMPOSE_FILE" ] && [ -f "$COMPOSE_FILE" ]; then
        print_info "Using existing Docker Compose file: $COMPOSE_FILE"
        return
    fi
    
    print_info "Creating Docker Compose file for environment: $ENVIRONMENT"
    
    local compose_content=$(cat << EOF
version: '3.8'

services:
  ${SERVICE_NAME}:
    image: youthmind/${SERVICE_NAME}:${VERSION}
    container_name: youthmind-${SERVICE_NAME}-${ENVIRONMENT}
    environment:
      - SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
      - SERVICE_NAME=${SERVICE_NAME}
      - SERVICE_VERSION=${VERSION}
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
    networks:
      - youthmind-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    deploy:
      replicas: ${SCALE_INSTANCES}
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M

  redis:
    image: redis:7-alpine
    container_name: youthmind-redis-${ENVIRONMENT}
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - youthmind-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    container_name: youthmind-postgres-${ENVIRONMENT}
    environment:
      - POSTGRES_DB=youthmind
      - POSTGRES_USER=youthmind
      - POSTGRES_PASSWORD=youthmind123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - youthmind-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U youthmind"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis-data:
  postgres-data:

networks:
  youthmind-network:
    driver: bridge
EOF
)
    
    local compose_file="$PROJECT_ROOT/docker-compose.${ENVIRONMENT}.yml"
    echo "$compose_content" > "$compose_file"
    COMPOSE_FILE="$compose_file"
    
    print_success "Docker Compose file created: $compose_file"
}

# Function to build Docker image
build_docker_image() {
    print_info "Building Docker image for service: $SERVICE_NAME version: $VERSION"
    
    # Create Dockerfile if it doesn't exist
    local dockerfile="$PROJECT_ROOT/Dockerfile"
    if [ ! -f "$dockerfile" ]; then
        create_dockerfile
    fi
    
    # Build image
    local image_name="youthmind/${SERVICE_NAME}:${VERSION}"
    local build_args="--build-arg SERVICE_NAME=${SERVICE_NAME} --build-arg SERVICE_VERSION=${VERSION}"
    
    docker build -t "$image_name" $build_args "$PROJECT_ROOT"
    
    if [ $? -eq 0 ]; then
        print_success "Docker image built successfully: $image_name"
    else
        print_error "Docker image build failed"
        exit 1
    fi
}

# Function to create Dockerfile
create_dockerfile() {
    print_info "Creating Dockerfile..."
    
    local dockerfile_content=$(cat << 'EOF'
FROM openjdk:17-jdk-slim

ARG SERVICE_NAME
ARG SERVICE_VERSION

LABEL maintainer="YouthMind Team"
LABEL service="${SERVICE_NAME}"
LABEL version="${SERVICE_VERSION}"

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY backend/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
)
    
    echo "$dockerfile_content" > "$PROJECT_ROOT/Dockerfile"
    print_success "Dockerfile created"
}

# Function to push Docker image
push_docker_image() {
    if [ -z "$REGISTRY_URL" ]; then
        print_error "Registry URL is required for pushing images"
        exit 1
    fi
    
    print_info "Pushing Docker image to registry: $REGISTRY_URL"
    
    local image_name="youthmind/${SERVICE_NAME}:${VERSION}"
    local registry_image="${REGISTRY_URL}/${image_name}"
    
    # Tag image for registry
    docker tag "$image_name" "$registry_image"
    
    # Push image
    docker push "$registry_image"
    
    if [ $? -eq 0 ]; then
        print_success "Docker image pushed successfully: $registry_image"
    else
        print_error "Docker image push failed"
        exit 1
    fi
}

# Function to deploy with Docker Compose
deploy_with_compose() {
    print_info "Deploying with Docker Compose..."
    
    # Stop existing containers
    print_info "Stopping existing containers..."
    docker-compose -f "$COMPOSE_FILE" down
    
    # Pull latest images
    print_info "Pulling latest images..."
    docker-compose -f "$COMPOSE_FILE" pull
    
    # Start services
    print_info "Starting services..."
    docker-compose -f "$COMPOSE_FILE" up -d
    
    if [ $? -eq 0 ]; then
        print_success "Services started successfully"
    else
        print_error "Failed to start services"
        exit 1
    fi
}

# Function to perform health check
perform_health_check() {
    print_info "Performing health check..."
    
    local max_attempts=20
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        print_info "Health check attempt $attempt/$max_attempts"
        
        # Check container health
        local container_name="youthmind-${SERVICE_NAME}-${ENVIRONMENT}"
        local health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "unhealthy")
        
        if [ "$health_status" = "healthy" ]; then
            print_success "Health check passed"
            return 0
        fi
        
        print_warning "Health check failed (Status: $health_status), retrying in 5 seconds..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    print_error "Health check failed after $max_attempts attempts"
    return 1
}

# Function to scale service
scale_service() {
    if [ "$SCALE_INSTANCES" -gt 1 ]; then
        print_info "Scaling service to $SCALE_INSTANCES instances..."
        
        docker-compose -f "$COMPOSE_FILE" up -d --scale "${SERVICE_NAME}=${SCALE_INSTANCES}"
        
        if [ $? -eq 0 ]; then
            print_success "Service scaled successfully"
        else
            print_error "Service scaling failed"
            exit 1
        fi
    fi
}

# Function to show deployment status
show_deployment_status() {
    print_info "Deployment Status:"
    
    # Show running containers
    docker-compose -f "$COMPOSE_FILE" ps
    
    # Show logs
    print_info "Recent logs:"
    docker-compose -f "$COMPOSE_FILE" logs --tail=20 "$SERVICE_NAME"
}

# Function to cleanup
cleanup() {
    print_info "Cleaning up..."
    
    # Stop containers
    if [ -f "$COMPOSE_FILE" ]; then
        docker-compose -f "$COMPOSE_FILE" down
    fi
    
    # Remove unused images
    docker image prune -f
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
        -b|--build)
            BUILD_IMAGE=true
            shift
            ;;
        -p|--push)
            PUSH_IMAGE=true
            shift
            ;;
        -r|--registry)
            REGISTRY_URL="$2"
            shift 2
            ;;
        -f|--compose-file)
            COMPOSE_FILE="$2"
            shift 2
            ;;
        -n|--scale)
            SCALE_INSTANCES="$2"
            shift 2
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
    print_info "Starting YouthMind Psychological Analysis Services Docker Deployment"
    print_info "Service: $SERVICE_NAME"
    print_info "Version: $VERSION"
    print_info "Environment: $ENVIRONMENT"
    print_info "Scale Instances: $SCALE_INSTANCES"
    
    # Validate prerequisites
    validate_prerequisites
    
    # Validate deployment parameters
    validate_deployment_params
    
    # Create Docker Compose file
    create_docker_compose_file
    
    # Build Docker image if requested
    if [ "$BUILD_IMAGE" = true ]; then
        build_docker_image
    fi
    
    # Push Docker image if requested
    if [ "$PUSH_IMAGE" = true ]; then
        push_docker_image
    fi
    
    # Deploy with Docker Compose
    deploy_with_compose
    
    # Scale service if needed
    scale_service
    
    # Perform health check
    if ! perform_health_check; then
        print_error "Health check failed"
        cleanup
        exit 1
    fi
    
    # Show deployment status
    show_deployment_status
    
    print_success "Docker deployment completed successfully!"
}

# Run main function
main "$@"
