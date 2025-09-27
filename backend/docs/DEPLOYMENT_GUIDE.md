# YouthMind Psychological Analysis Services Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying YouthMind Psychological Analysis Services using various deployment strategies and tools.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Deployment Strategies](#deployment-strategies)
3. [Environment Configurations](#environment-configurations)
4. [Service Configurations](#service-configurations)
5. [Deployment Scripts](#deployment-scripts)
6. [Monitoring and Health Checks](#monitoring-and-health-checks)
7. [Rollback Procedures](#rollback-procedures)
8. [Troubleshooting](#troubleshooting)
9. [Best Practices](#best-practices)

## Prerequisites

### System Requirements

- **Java 17+**: Required for running the Spring Boot application
- **Maven 3.6+**: For building the application
- **Docker**: For containerized deployments
- **Kubernetes**: For Kubernetes deployments (optional)
- **PostgreSQL 13+**: Database server
- **Redis 6+**: Caching and session storage

### Environment Setup

1. **Development Environment**
   ```bash
   # Clone the repository
   git clone https://github.com/youthmind/youthmind.git
   cd youthmind
   
   # Install dependencies
   mvn clean install
   
   # Start required services
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Staging Environment**
   ```bash
   # Deploy to staging
   ./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e staging
   ```

3. **Production Environment**
   ```bash
   # Deploy to production
   ./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e prod
   ```

## Deployment Strategies

### 1. Blue-Green Deployment

Blue-green deployment provides zero-downtime deployments by maintaining two identical production environments.

**Benefits:**
- Zero downtime deployments
- Easy rollback
- Reduced risk

**Process:**
1. Deploy new version to green environment
2. Validate green environment
3. Switch traffic to green environment
4. Remove blue environment

**Usage:**
```bash
# Deploy with blue-green strategy
./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e prod -t blue-green
```

### 2. Rolling Update Deployment

Rolling update gradually replaces instances of the old version with new ones.

**Benefits:**
- Continuous availability
- Resource efficient
- Gradual rollout

**Process:**
1. Update deployment configuration
2. Roll out new version gradually
3. Monitor deployment progress
4. Complete rollout

**Usage:**
```bash
# Deploy with rolling update strategy
./backend/scripts/deploy.sh -s state_update -v 1.3.0 -e staging -t rolling
```

## Environment Configurations

### Development Environment

```yaml
environment: dev
baseUrl: http://localhost:8080
databaseUrl: jdbc:postgresql://localhost:5432/youthmind_dev
redisUrl: redis://localhost:6379
logLevel: DEBUG
enableMetrics: true
enableTracing: false
securityEnabled: false
sslEnabled: false
```

### Staging Environment

```yaml
environment: staging
baseUrl: https://staging-api.youthmind.com
databaseUrl: jdbc:postgresql://staging-db.youthmind.com:5432/youthmind_staging
redisUrl: redis://staging-redis.youthmind.com:6379
logLevel: INFO
enableMetrics: true
enableTracing: true
securityEnabled: true
sslEnabled: true
```

### Production Environment

```yaml
environment: prod
baseUrl: https://api.youthmind.com
databaseUrl: jdbc:postgresql://prod-db.youthmind.com:5432/youthmind_prod
redisUrl: redis://prod-redis.youthmind.com:6379
logLevel: WARN
enableMetrics: true
enableTracing: true
securityEnabled: true
sslEnabled: true
```

## Service Configurations

### Psychological Analysis Service

```yaml
serviceName: psychological_analysis
serviceType: AI_SERVICE
port: 8080
replicas: 2
resourceRequirements:
  cpuRequest: 250m
  cpuLimit: 500m
  memoryRequest: 512Mi
  memoryLimit: 1Gi
healthCheckConfig:
  path: /actuator/health
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3
deploymentStrategy: BLUE_GREEN
autoScalingEnabled: true
minReplicas: 1
maxReplicas: 5
```

### State Update Service

```yaml
serviceName: state_update
serviceType: AI_SERVICE
port: 8080
replicas: 3
resourceRequirements:
  cpuRequest: 200m
  cpuLimit: 400m
  memoryRequest: 256Mi
  memoryLimit: 512Mi
healthCheckConfig:
  path: /actuator/health
  initialDelaySeconds: 30
  periodSeconds: 15
  timeoutSeconds: 5
  failureThreshold: 3
deploymentStrategy: ROLLING_UPDATE
autoScalingEnabled: true
minReplicas: 2
maxReplicas: 10
```

## Deployment Scripts

### 1. Standard Deployment Script

The `deploy.sh` script provides comprehensive deployment capabilities.

**Usage:**
```bash
# Basic deployment
./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e prod

# With custom configuration
./backend/scripts/deploy.sh -s state_update -v 1.3.0 -e staging -c config/staging.yml

# Dry run
./backend/scripts/deploy.sh -s mood_analysis -v 1.1.0 -e prod -d
```

**Options:**
- `-s, --service`: Service name to deploy
- `-v, --version`: Version to deploy
- `-e, --environment`: Environment (dev, staging, prod)
- `-c, --config`: Configuration file path
- `-d, --dry-run`: Perform dry run without actual deployment
- `-t, --timeout`: Health check timeout in seconds

### 2. Docker Deployment Script

The `docker-deploy.sh` script handles containerized deployments.

**Usage:**
```bash
# Deploy with Docker Compose
./backend/scripts/docker-deploy.sh -s psychological_analysis -v 1.2.0 -e prod -f docker-compose.prod.yml

# Build and deploy
./backend/scripts/docker-deploy.sh -s state_update -v 1.3.0 -e staging -b -f docker-compose.staging.yml

# Build, push and deploy
./backend/scripts/docker-deploy.sh -s mood_analysis -v 1.1.0 -e prod -b -p -r registry.youthmind.com
```

**Options:**
- `-b, --build`: Build Docker image
- `-p, --push`: Push Docker image to registry
- `-r, --registry`: Docker registry URL
- `-f, --compose-file`: Docker Compose file path
- `-n, --scale`: Number of instances to scale

### 3. Kubernetes Deployment Script

The `k8s-deploy.sh` script handles Kubernetes deployments.

**Usage:**
```bash
# Deploy with blue-green strategy
./backend/scripts/k8s-deploy.sh -s psychological_analysis -v 1.2.0 -e prod -i youthmind/psychological-analysis:1.2.0

# Rollback to previous version
./backend/scripts/k8s-deploy.sh -s state_update -r 1.1.0 -e prod

# Dry run deployment
./backend/scripts/k8s-deploy.sh -s mood_analysis -v 1.3.0 -e staging -d
```

**Options:**
- `-n, --namespace`: Kubernetes namespace
- `-i, --image`: Docker image URL
- `-r, --replicas`: Number of replicas
- `-t, --strategy`: Deployment strategy (blue-green, rolling)
- `-r, --rollback`: Rollback to specific version

## Monitoring and Health Checks

### Health Check Endpoints

All services expose health check endpoints at `/actuator/health`:

```bash
# Check service health
curl http://localhost:8080/actuator/health

# Check specific service health
curl http://localhost:8080/v1/deployment/services/psychological_analysis/validate
```

### Monitoring Endpoints

**Deployment Status:**
```bash
curl http://localhost:8080/v1/deployment/services/psychological_analysis/status
```

**Deployment Metrics:**
```bash
curl http://localhost:8080/v1/deployment/services/psychological_analysis/metrics
```

**Monitoring Report:**
```bash
curl http://localhost:8080/v1/deployment-monitoring/services/psychological_analysis/report
```

### Prometheus Metrics

Services expose Prometheus-compatible metrics at `/actuator/prometheus`:

```bash
# Get Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Rollback Procedures

### 1. Automatic Rollback

The deployment system automatically rolls back if health checks fail:

```bash
# Deploy with automatic rollback
./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e prod --auto-rollback
```

### 2. Manual Rollback

**Using Deployment Script:**
```bash
# Rollback to previous version
./backend/scripts/deploy.sh -s psychological_analysis -r 1.1.0 -e prod
```

**Using API:**
```bash
# Rollback via API
curl -X POST http://localhost:8080/v1/deployment/services/psychological_analysis/rollback?targetVersion=1.1.0
```

**Using Kubernetes:**
```bash
# Rollback Kubernetes deployment
kubectl rollout undo deployment/psychological_analysis -n youthmind
```

### 3. Emergency Rollback

For critical issues, use emergency rollback procedures:

```bash
# Emergency rollback script
./backend/scripts/emergency-rollback.sh -s psychological_analysis -e prod
```

## Troubleshooting

### Common Issues

1. **Deployment Timeout**
   ```bash
   # Check deployment logs
   kubectl logs deployment/psychological_analysis -n youthmind
   
   # Increase timeout
   ./backend/scripts/deploy.sh -s psychological_analysis -v 1.2.0 -e prod -t 300
   ```

2. **Health Check Failures**
   ```bash
   # Check service health
   curl http://localhost:8080/actuator/health
   
   # Check specific health checks
   curl http://localhost:8080/v1/deployment/services/psychological_analysis/validate
   ```

3. **Resource Constraints**
   ```bash
   # Check resource usage
   kubectl top pods -n youthmind
   
   # Scale down if needed
   kubectl scale deployment psychological_analysis --replicas=1 -n youthmind
   ```

### Debugging Commands

**Check Deployment Status:**
```bash
kubectl get deployments -n youthmind
kubectl describe deployment psychological_analysis -n youthmind
```

**Check Pod Status:**
```bash
kubectl get pods -n youthmind
kubectl describe pod <pod-name> -n youthmind
```

**Check Service Status:**
```bash
kubectl get services -n youthmind
kubectl describe service psychological_analysis -n youthmind
```

**Check Logs:**
```bash
kubectl logs deployment/psychological_analysis -n youthmind
kubectl logs -f deployment/psychological_analysis -n youthmind
```

## Best Practices

### 1. Deployment Best Practices

- **Always test in staging first**
- **Use blue-green deployments for production**
- **Monitor deployments closely**
- **Have rollback procedures ready**
- **Use feature flags for gradual rollouts**

### 2. Configuration Management

- **Use environment-specific configurations**
- **Store sensitive data in secrets**
- **Validate configurations before deployment**
- **Version control all configurations**

### 3. Monitoring and Alerting

- **Set up comprehensive monitoring**
- **Configure alerts for critical metrics**
- **Monitor deployment success rates**
- **Track performance metrics**

### 4. Security

- **Use HTTPS in production**
- **Implement proper authentication**
- **Regular security updates**
- **Audit deployment access**

### 5. Backup and Recovery

- **Regular database backups**
- **Configuration backups**
- **Disaster recovery procedures**
- **Test recovery procedures regularly**

## API Reference

### Deployment Management API

**Base URL:** `http://localhost:8080/v1/deployment`

**Endpoints:**
- `POST /services/{serviceName}/deploy` - Deploy service
- `POST /services/{serviceName}/rollback` - Rollback service
- `GET /services/{serviceName}/status` - Get deployment status
- `POST /services/{serviceName}/validate` - Validate service health
- `GET /metrics` - Get deployment metrics

### Monitoring API

**Base URL:** `http://localhost:8080/v1/deployment-monitoring`

**Endpoints:**
- `POST /services/{serviceName}/start` - Start monitoring
- `POST /services/{serviceName}/stop` - Stop monitoring
- `GET /services/{serviceName}/status` - Get monitoring status
- `GET /services/{serviceName}/metrics` - Get monitoring metrics
- `GET /services/{serviceName}/report` - Get monitoring report

### Configuration API

**Base URL:** `http://localhost:8080/v1/deployment-config`

**Endpoints:**
- `GET /environments/{environment}` - Get environment configuration
- `PUT /environments/{environment}` - Update environment configuration
- `GET /services/{serviceName}` - Get service configuration
- `PUT /services/{serviceName}` - Update service configuration
- `POST /validate` - Validate deployment configuration

## Support

For deployment issues and questions:

1. **Check the logs** for error messages
2. **Review the troubleshooting section** above
3. **Contact the development team** for assistance
4. **Create an issue** in the project repository

## Changelog

### Version 1.2.0
- Added blue-green deployment support
- Improved monitoring and alerting
- Enhanced rollback procedures
- Added Kubernetes deployment scripts

### Version 1.1.0
- Added Docker deployment support
- Improved health checks
- Enhanced configuration management
- Added deployment monitoring

### Version 1.0.0
- Initial deployment system
- Basic deployment scripts
- Health check endpoints
- Configuration management
