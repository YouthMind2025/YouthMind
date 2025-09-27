#!/bin/bash

# YouthMind Psychological Analysis Services Kubernetes Deployment Script
# Supports Kubernetes deployment with blue-green strategy and automated rollback

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
K8S_DIR="$PROJECT_ROOT/k8s"

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
NAMESPACE="youthmind"
IMAGE_URL=""
REPLICAS=2
STRATEGY="blue-green"
DRY_RUN=false
ROLLBACK=false
TARGET_VERSION=""
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

Deploy YouthMind Psychological Analysis Services using Kubernetes

OPTIONS:
    -s, --service SERVICE_NAME    Name of the service to deploy
    -v, --version VERSION         Version to deploy
    -e, --environment ENV         Environment (dev, staging, prod) [default: dev]
    -n, --namespace NAMESPACE     Kubernetes namespace [default: youthmind]
    -i, --image IMAGE_URL         Docker image URL
    -r, --replicas REPLICAS       Number of replicas [default: 2]
    -t, --strategy STRATEGY       Deployment strategy (blue-green, rolling) [default: blue-green]
    -r, --rollback VERSION        Rollback to specific version
    -d, --dry-run                 Perform dry run without actual deployment
    -h, --help                    Show this help message

EXAMPLES:
    # Deploy with blue-green strategy
    $0 -s psychological_analysis -v 1.2.0 -e prod -i youthmind/psychological-analysis:1.2.0

    # Rollback to previous version
    $0 -s state_update -r 1.1.0 -e prod

    # Dry run deployment
    $0 -s mood_analysis -v 1.3.0 -e staging -d

EOF
}

# Function to validate prerequisites
validate_prerequisites() {
    print_info "Validating prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Kubernetes cluster is accessible
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Kubernetes cluster is not accessible"
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        print_warning "Namespace $NAMESPACE does not exist, creating it..."
        kubectl create namespace "$NAMESPACE"
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

# Function to create Kubernetes manifests
create_k8s_manifests() {
    print_info "Creating Kubernetes manifests..."
    
    # Create manifests directory
    mkdir -p "$K8s_DIR"
    
    # Create deployment manifest
    create_deployment_manifest
    
    # Create service manifest
    create_service_manifest
    
    # Create configmap manifest
    create_configmap_manifest
    
    # Create ingress manifest
    create_ingress_manifest
    
    print_success "Kubernetes manifests created"
}

# Function to create deployment manifest
create_deployment_manifest() {
    local deployment_file="$K8s_DIR/${SERVICE_NAME}-deployment.yaml"
    
    cat > "$deployment_file" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${SERVICE_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${SERVICE_NAME}
    version: ${VERSION}
    environment: ${ENVIRONMENT}
spec:
  replicas: ${REPLICAS}
  selector:
    matchLabels:
      app: ${SERVICE_NAME}
  template:
    metadata:
      labels:
        app: ${SERVICE_NAME}
        version: ${VERSION}
        environment: ${ENVIRONMENT}
    spec:
      containers:
      - name: ${SERVICE_NAME}
        image: ${IMAGE_URL}
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "${ENVIRONMENT}"
        - name: SERVICE_NAME
          value: "${SERVICE_NAME}"
        - name: SERVICE_VERSION
          value: "${VERSION}"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: config-volume
        configMap:
          name: ${SERVICE_NAME}-config
      - name: logs-volume
        emptyDir: {}
      restartPolicy: Always
EOF
    
    print_info "Deployment manifest created: $deployment_file"
}

# Function to create service manifest
create_service_manifest() {
    local service_file="$K8s_DIR/${SERVICE_NAME}-service.yaml"
    
    cat > "$service_file" << EOF
apiVersion: v1
kind: Service
metadata:
  name: ${SERVICE_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${SERVICE_NAME}
spec:
  selector:
    app: ${SERVICE_NAME}
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
EOF
    
    print_info "Service manifest created: $service_file"
}

# Function to create configmap manifest
create_configmap_manifest() {
    local configmap_file="$K8s_DIR/${SERVICE_NAME}-configmap.yaml"
    
    cat > "$configmap_file" << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${SERVICE_NAME}-config
  namespace: ${NAMESPACE}
  labels:
    app: ${SERVICE_NAME}
data:
  application.yml: |
    spring:
      profiles:
        active: ${ENVIRONMENT}
    server:
      port: 8080
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
    logging:
      level:
        ai.youthmind: INFO
        org.springframework: WARN
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
EOF
    
    print_info "ConfigMap manifest created: $configmap_file"
}

# Function to create ingress manifest
create_ingress_manifest() {
    local ingress_file="$K8s_DIR/${SERVICE_NAME}-ingress.yaml"
    
    cat > "$ingress_file" << EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ${SERVICE_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${SERVICE_NAME}
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  rules:
  - host: ${SERVICE_NAME}.${ENVIRONMENT}.youthmind.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ${SERVICE_NAME}
            port:
              number: 8080
EOF
    
    print_info "Ingress manifest created: $ingress_file"
}

# Function to deploy with blue-green strategy
deploy_blue_green() {
    print_info "Deploying with blue-green strategy..."
    
    # Create green deployment
    local green_deployment="${SERVICE_NAME}-green"
    local blue_deployment="${SERVICE_NAME}-blue"
    
    # Check if blue deployment exists
    if kubectl get deployment "$blue_deployment" -n "$NAMESPACE" &> /dev/null; then
        print_info "Blue deployment exists, creating green deployment..."
        
        # Create green deployment
        kubectl apply -f "$K8s_DIR/${SERVICE_NAME}-deployment.yaml" -n "$NAMESPACE"
        
        # Wait for green deployment to be ready
        kubectl rollout status deployment/"${SERVICE_NAME}" -n "$NAMESPACE" --timeout=300s
        
        # Switch traffic to green
        switch_traffic_to_green
        
        # Remove blue deployment
        kubectl delete deployment "$blue_deployment" -n "$NAMESPACE"
        
    else
        print_info "No existing deployment, creating initial deployment..."
        kubectl apply -f "$K8s_DIR/${SERVICE_NAME}-deployment.yaml" -n "$NAMESPACE"
        kubectl rollout status deployment/"${SERVICE_NAME}" -n "$NAMESPACE" --timeout=300s
    fi
}

# Function to deploy with rolling update strategy
deploy_rolling_update() {
    print_info "Deploying with rolling update strategy..."
    
    # Apply deployment
    kubectl apply -f "$K8s_DIR/${SERVICE_NAME}-deployment.yaml" -n "$NAMESPACE"
    
    # Wait for rollout to complete
    kubectl rollout status deployment/"${SERVICE_NAME}" -n "$NAMESPACE" --timeout=300s
}

# Function to switch traffic to green
switch_traffic_to_green() {
    print_info "Switching traffic to green deployment..."
    
    # Update service selector to point to green deployment
    kubectl patch service "${SERVICE_NAME}" -n "$NAMESPACE" -p '{"spec":{"selector":{"version":"'${VERSION}'"}}}'
    
    print_success "Traffic switched to green deployment"
}

# Function to rollback deployment
rollback_deployment() {
    print_info "Rolling back to version: $TARGET_VERSION"
    
    # Rollback deployment
    kubectl rollout undo deployment/"${SERVICE_NAME}" -n "$NAMESPACE"
    
    # Wait for rollback to complete
    kubectl rollout status deployment/"${SERVICE_NAME}" -n "$NAMESPACE" --timeout=300s
    
    print_success "Rollback completed"
}

# Function to perform health check
perform_health_check() {
    print_info "Performing health check..."
    
    local max_attempts=20
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        print_info "Health check attempt $attempt/$max_attempts"
        
        # Check pod health
        local pod_status=$(kubectl get pods -l app="${SERVICE_NAME}" -n "$NAMESPACE" -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo "Unknown")
        
        if [ "$pod_status" = "Running" ]; then
            # Check service health
            local service_ip=$(kubectl get service "${SERVICE_NAME}" -n "$NAMESPACE" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || echo "")
            
            if [ -n "$service_ip" ]; then
                print_success "Health check passed"
                return 0
            fi
        fi
        
        print_warning "Health check failed (Pod Status: $pod_status), retrying in 5 seconds..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    print_error "Health check failed after $max_attempts attempts"
    return 1
}

# Function to show deployment status
show_deployment_status() {
    print_info "Deployment Status:"
    
    # Show deployments
    kubectl get deployments -l app="${SERVICE_NAME}" -n "$NAMESPACE"
    
    # Show pods
    kubectl get pods -l app="${SERVICE_NAME}" -n "$NAMESPACE"
    
    # Show services
    kubectl get services -l app="${SERVICE_NAME}" -n "$NAMESPACE"
    
    # Show ingress
    kubectl get ingress -l app="${SERVICE_NAME}" -n "$NAMESPACE"
}

# Function to cleanup
cleanup() {
    print_info "Cleaning up..."
    
    # Delete resources
    kubectl delete -f "$K8s_DIR" -n "$NAMESPACE" --ignore-not-found=true
    
    # Remove manifests directory
    rm -rf "$K8s_DIR"
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
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -i|--image)
            IMAGE_URL="$2"
            shift 2
            ;;
        -r|--replicas)
            REPLICAS="$2"
            shift 2
            ;;
        -t|--strategy)
            STRATEGY="$2"
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
    print_info "Starting YouthMind Psychological Analysis Services Kubernetes Deployment"
    print_info "Service: $SERVICE_NAME"
    print_info "Environment: $ENVIRONMENT"
    print_info "Namespace: $NAMESPACE"
    
    if [ "$ROLLBACK" = true ]; then
        print_info "Target Version: $TARGET_VERSION"
    else
        print_info "Version: $VERSION"
    fi
    
    if [ "$DRY_RUN" = true ]; then
        print_warning "DRY RUN MODE - No actual deployment will be performed"
    fi
    
    # Validate prerequisites
    validate_prerequisites
    
    # Validate deployment parameters
    validate_deployment_params
    
    if [ "$DRY_RUN" = true ]; then
        print_success "Dry run completed successfully"
        exit 0
    fi
    
    # Create Kubernetes manifests
    create_k8s_manifests
    
    # Deploy based on strategy
    case "$STRATEGY" in
        blue-green)
            deploy_blue_green
            ;;
        rolling)
            deploy_rolling_update
            ;;
        *)
            print_error "Invalid deployment strategy: $STRATEGY"
            exit 1
            ;;
    esac
    
    # Perform health check
    if ! perform_health_check; then
        print_error "Health check failed"
        cleanup
        exit 1
    fi
    
    # Show deployment status
    show_deployment_status
    
    print_success "Kubernetes deployment completed successfully!"
}

# Run main function
main "$@"
