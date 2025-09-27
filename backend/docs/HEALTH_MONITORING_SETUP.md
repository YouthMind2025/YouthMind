# YouthMind Backend - System Health Monitoring & DevOps Integration

## 🚀 Overview

This document describes the comprehensive system health visibility setup implemented for the YouthMind Backend API, providing DevOps teams with detailed monitoring, alerting, and observability capabilities.

## ✅ Implementation Status: COMPLETED

All dependencies verified and integrated:

- ✅ **Week 2: Configure Redis integration (Mustafo) - COMPLETED**
- ✅ **Week 1: Design core PostgreSQL schema (Ahmed) - COMPLETED**
- ✅ **Redis and PostgreSQL fully operational**

## 🔧 Components Implemented

### 1. Custom Health Indicators

#### Redis Health Indicator (`/actuator/health/redis`)

- **Location**: `backend/src/main/java/ai/youthmind/config/RedisHealthIndicator.java`
- **Features**:
  - Redis connectivity testing (PING/PONG)
  - Operation testing (SET, GET, DELETE, INCREMENT)
  - Pub/Sub capability verification
  - Server version information
  - Connection status monitoring

#### Database Health Indicator (`/actuator/health/database`)

- **Location**: `backend/src/main/java/ai/youthmind/config/DatabaseHealthIndicator.java`
- **Features**:
  - PostgreSQL connectivity testing
  - Schema validation (users, posts, ai_analysis tables)
  - Query execution testing
  - Connection pool status
  - Migration status verification
  - Record count monitoring

### 2. Actuator Endpoints Configuration

#### Health Endpoints

```
/actuator/health              - Overall system health
/actuator/health/redis         - Redis-specific health
/actuator/health/database      - Database-specific health
/actuator/health/liveness      - Kubernetes liveness probe
/actuator/health/readiness     - Kubernetes readiness probe
/actuator/health/custom        - Custom health group
```

#### Metrics Endpoints

```
/actuator/metrics              - Available metrics list
/actuator/metrics/*            - Specific metric data
/actuator/prometheus           - Prometheus format metrics
```

#### Information Endpoints

```
/actuator/info                 - Application information
/actuator/env                  - Environment properties (Admin)
/actuator/beans               - Spring beans (Admin)
/actuator/configprops         - Configuration properties (Admin)
/actuator/mappings            - Request mappings (Admin)
/actuator/conditions          - Auto-configuration conditions (Admin)
```

### 3. Security Configuration

#### Public Endpoints (No Authentication)

- `/actuator/health/**` - Health checks
- `/actuator/info` - Application info
- `/actuator/metrics/**` - Metrics data
- `/actuator/prometheus` - Prometheus metrics

#### Admin-Only Endpoints (Requires ADMIN role)

- `/actuator/env/**` - Environment configuration
- `/actuator/beans` - Spring beans information
- `/actuator/configprops` - Configuration properties
- `/actuator/mappings` - URL mappings
- `/actuator/conditions` - Auto-configuration report

### 4. Prometheus Integration

#### Metrics Configuration

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 30s
    tags:
      application: youthmind-backend
      environment: ${spring.profiles.active:dev}
      version: ${project.version:0.0.1-SNAPSHOT}
```

#### Available Metrics

- HTTP request metrics with percentiles
- JVM memory and CPU usage
- System metrics (disk, memory)
- Custom business metrics
- Database connection pool metrics
- Redis operation metrics

### 5. Kubernetes Integration

#### Health Groups for Probes

```yaml
management:
  endpoint:
    health:
      group:
        liveness:
          include: ping
        readiness:
          include: redis,database,diskSpace
```

#### Probe Configuration

```yaml
# Kubernetes deployment configuration
livenessProbe:
  httpGet:
    path: "/actuator/health/liveness"
    port: 8080
  failureThreshold: 3
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: "/actuator/health/readiness"
    port: 8080
  failureThreshold: 3
  periodSeconds: 10
```

## 🔍 Monitoring & Alerting Setup

### Grafana Dashboard Integration

#### Recommended Panels

1. **System Health Overview**

   - Overall health status
   - Component health matrix
   - Uptime tracking
2. **Performance Metrics**

   - HTTP request rate and latency
   - JVM memory usage
   - CPU utilization
   - Database connection pool status
3. **Business Metrics**

   - User registration rate
   - Post creation rate
   - AI analysis processing time
   - Cache hit rates
4. **Infrastructure Health**

   - Redis connectivity and performance
   - Database query performance
   - Disk space utilization

### Prometheus Queries

#### Health Status Monitoring

```promql
# Overall health status
up{job="youthmind-backend"}

# Component health status
health_component_status{component="redis"}
health_component_status{component="database"}
```

#### Performance Monitoring

```promql
# HTTP request rate
rate(http_requests_total[5m])

# Response time percentiles
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes
```

### Alert Rules

#### Critical Alerts

```yaml
- alert: ApplicationDown
  expr: up{job="youthmind-backend"} == 0
  for: 1m
  labels:
    severity: critical
  annotations:
    summary: "YouthMind Backend is down"

- alert: DatabaseConnectionFailure
  expr: health_component_status{component="database"} == 0
  for: 30s
  labels:
    severity: critical
  annotations:
    summary: "Database connection failed"

- alert: RedisConnectionFailure
  expr: health_component_status{component="redis"} == 0
  for: 30s
  labels:
    severity: warning
  annotations:
    summary: "Redis connection failed"
```

#### Performance Alerts

```yaml
- alert: HighResponseTime
  expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High response time detected"

- alert: HighMemoryUsage
  expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "High JVM memory usage"
```

## 🛠 Testing & Validation

### Test Script

**Location**: `backend/test-actuator-endpoints.ps1`

This PowerShell script comprehensively tests all actuator endpoints and provides detailed output for validation.

**Usage**:

```powershell
cd backend
.\test-actuator-endpoints.ps1
```

### Manual Testing Commands

```bash
# Test main health endpoint
curl http://localhost:8080/actuator/health

# Test specific component health
curl http://localhost:8080/actuator/health/redis
curl http://localhost:8080/actuator/health/database

# Test Kubernetes probes
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# Test Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Test application info
curl http://localhost:8080/actuator/info
```

## 📊 Application Information Exposed

The `/actuator/info` endpoint provides comprehensive application metadata:

```json
{
  "app": {
    "name": "YouthMind Backend API",
    "description": "Backend API for YouthMind - A mental health support platform for youth",
    "version": "1.0.0"
  },
  "team": {
    "name": "YouthMind Development Team",
    "lead-developer": "Ahmed",
    "backend-architect": "Mustafo",
    "devops-engineer": "Kevin"
  },
  "features": {
    "authentication": "JWT-based authentication",
    "caching": "Redis caching with pub/sub",
    "database": "PostgreSQL with Flyway migrations",
    "ai-integration": "AI analysis for posts",
    "monitoring": "Spring Actuator with Prometheus metrics"
  }
}
```

## 🔐 Security Considerations

### Endpoint Access Control

- Health and metrics endpoints are public for monitoring
- Configuration endpoints require ADMIN role
- Sensitive information is masked in environment endpoints
- CORS is properly configured for monitoring tools

### Production Recommendations

1. Use HTTPS for all actuator endpoints
2. Implement proper authentication for admin endpoints
3. Use network-level security (VPNs, firewalls)
4. Regularly rotate admin credentials
5. Monitor actuator endpoint access logs

## 🚀 Deployment Guidelines

### Staging Environment

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Production Environment

```yaml
management:
  endpoint:
    health:
      show-details: never
    env:
      show-values: never
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

## 📞 Support & Coordination

### Team Coordination

- **Ahmed (Lead Developer)**: Database schema and migrations ✅ COMPLETED
- **Mustafo (Backend Architect)**: Redis integration and caching ✅ COMPLETED
- **Kevin (DevOps Engineer)**: Monitoring and infrastructure integration

### Next Steps for Kevin

1. Configure Prometheus to scrape `/actuator/prometheus`
2. Set up Grafana dashboards using provided queries
3. Implement alert rules for critical system health
4. Configure Kubernetes health checks
5. Set up log aggregation for actuator endpoints

### Troubleshooting

- Check application logs for health indicator failures
- Verify Redis and PostgreSQL connectivity
- Ensure proper network access to monitoring endpoints
- Review security configurations for admin endpoints

---

**Implementation Complete** ✅
**Ready for Production Monitoring** 🚀
**All Dependencies Verified** ✅
