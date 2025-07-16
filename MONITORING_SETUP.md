# YouthMind Log Aggregation & Database Monitoring Setup

## Overview

This monitoring setup provides comprehensive observability for the YouthMind backend system, including:

- **PostgreSQL Database Monitoring**: Query performance, slow queries, connection counts, table usage
- **Redis Cache Monitoring**: Memory usage, command rates, connection counts
- **Spring Boot Application Metrics**: API request rates, response times, JVM metrics
- **Log Aggregation**: Centralized logging from all services
- **Real-time Dashboards**: Grafana dashboards for visualization

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │   PostgreSQL    │    │     Redis       │
│   Backend       │    │   Database      │    │     Cache       │
│   :8080         │    │   :5432         │    │     :6379       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ /actuator/prometheus  │ postgres-exporter     │ redis-exporter
         │                       │ :9187                 │ :9121
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Prometheus                                  │
│                     :9090                                       │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Grafana                                     │
│                     :3001                                       │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Prerequisites

Ensure you have Docker and Docker Compose installed:
```bash
docker --version
docker-compose --version
```

### 2. Start the Monitoring Stack

```bash
# Navigate to YouthMind directory
cd YouthMind

# Start all monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Check that all services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 3. Start the Spring Boot Backend

```bash
# Build and run the backend
cd backend
mvn clean package
java -jar target/youthmind-backend-*.jar
```

### 4. Access the Monitoring Dashboards

- **Grafana Dashboard**: http://localhost:3001
  - Username: `admin`
  - Password: `admin`
  - Pre-configured dashboard: "YouthMind Database & System Monitoring"

- **Prometheus**: http://localhost:9090
  - Raw metrics and query interface

- **Database Logs**: Available in `monitoring/postgres/logs/`

## Services and Ports

| Service | Port | Purpose |
|---------|------|---------|
| Spring Boot Backend | 8080 | Main application |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache and pub/sub |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Monitoring dashboards |
| PostgreSQL Exporter | 9187 | Database metrics |
| Redis Exporter | 9121 | Cache metrics |

## Monitoring Features

### Database Monitoring
- **Query Performance**: Track slow queries, execution times
- **Connection Monitoring**: Active connections, pool usage
- **Table Usage**: Sequential vs index scans, insert/update/delete rates
- **Transaction Stats**: Commit/rollback rates
- **Buffer Cache**: Hit ratios, disk read patterns

### Application Monitoring
- **API Metrics**: Request rates, response times, error rates
- **JVM Metrics**: Memory usage, garbage collection, thread counts
- **Custom Metrics**: Business logic metrics from Spring Boot

### Cache Monitoring
- **Redis Performance**: Command rates, memory usage
- **Connection Stats**: Client connections, slow commands
- **Cache Hit Ratios**: Effectiveness of caching strategy

## Configuration Files

### Spring Boot Actuator Configuration
```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
```

### PostgreSQL Logging Configuration
- Enhanced logging for all SQL statements
- Slow query detection (>100ms)
- Connection/disconnection logging
- Lock wait monitoring
- pg_stat_statements extension enabled

### Prometheus Scraping Targets
- Spring Boot: `/actuator/prometheus`
- PostgreSQL: via postgres-exporter
- Redis: via redis-exporter

## Grafana Dashboard Features

### Database Performance Panel
- Transaction rates (commits/rollbacks per second)
- Active database connections
- Query execution patterns (sequential vs index scans)

### Slow Query Analysis
- Top queries by execution time
- Query frequency and patterns
- Performance trends over time

### System Overview
- API request rates and response times
- Redis cache performance
- Resource utilization metrics

## Testing the Setup

### 1. Verify Services are Running
```bash
# Check all containers
docker-compose -f docker-compose.monitoring.yml ps

# Check service health
curl http://localhost:8080/actuator/health
curl http://localhost:9090/-/healthy
curl http://localhost:3001/api/health
```

### 2. Generate Test Data
```bash
# Create some database activity
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password"}'

# Check metrics are being collected
curl http://localhost:8080/actuator/prometheus | grep postgres
```

### 3. Verify Metrics Collection
1. Open Prometheus: http://localhost:9090
2. Search for metrics: `pg_stat_database_xact_commit`
3. Verify data is being collected

### 4. Check Grafana Dashboard
1. Open Grafana: http://localhost:3001
2. Login with admin/admin
3. Navigate to "YouthMind Database & System Monitoring" dashboard
4. Verify panels show data

## Log Aggregation

### PostgreSQL Logs
- Location: `monitoring/postgres/logs/postgresql.log`
- Format: Structured logs with timestamp, user, database, query
- Retention: Configurable via Docker volume

### Application Logs
- Spring Boot logs output to console (captured by Docker)
- Access via: `docker logs youthmind-backend`

### Redis Logs
- Available via: `docker logs youthmind-redis`
- Slow log available via Redis CLI: `SLOWLOG GET`

## Performance Optimization Insights

### Identify Slow Queries
1. Check Grafana "Slow Queries" panel
2. Look for queries with high mean execution time
3. Analyze pg_stat_statements data

### Monitor Connection Pool
1. Track active connections in Grafana
2. Alert if connections approach pool limit
3. Optimize connection usage in application

### Cache Effectiveness
1. Monitor Redis hit/miss ratios
2. Track memory usage and eviction patterns
3. Optimize cache keys and TTL values

## Troubleshooting

### Common Issues

**Services not starting:**
```bash
# Check logs for specific service
docker-compose -f docker-compose.monitoring.yml logs postgres
docker-compose -f docker-compose.monitoring.yml logs prometheus
```

**No metrics in Grafana:**
1. Verify Prometheus is scraping: http://localhost:9090/targets
2. Check Spring Boot actuator endpoint: http://localhost:8080/actuator/prometheus
3. Verify network connectivity between containers

**Database connection issues:**
1. Check PostgreSQL logs
2. Verify connection string in postgres-exporter
3. Ensure database is accepting connections

### Useful Commands
```bash
# Restart monitoring stack
docker-compose -f docker-compose.monitoring.yml restart

# View real-time logs
docker-compose -f docker-compose.monitoring.yml logs -f

# Stop monitoring stack
docker-compose -f docker-compose.monitoring.yml down

# Clean up and restart fresh
docker-compose -f docker-compose.monitoring.yml down -v
docker-compose -f docker-compose.monitoring.yml up -d
```

## Integration with JMeter Testing

This monitoring setup is designed to work perfectly with Junayd's JMeter stress testing task:

1. **Baseline Metrics**: Establish normal operation metrics before testing
2. **Real-time Monitoring**: Watch performance during stress tests
3. **Bottleneck Identification**: Identify database/cache/API bottlenecks
4. **Performance Analysis**: Compare before/after metrics
5. **Optimization Guidance**: Use metrics to guide performance improvements

### Recommended Workflow
1. Start monitoring stack
2. Record baseline metrics (5-10 minutes of normal operation)
3. Run JMeter stress tests
4. Monitor real-time performance in Grafana
5. Analyze results and identify bottlenecks
6. Document findings and improvement suggestions

## Next Steps

With this monitoring infrastructure in place, you can:

1. **Run Stress Tests**: Execute JMeter tests with full observability
2. **Set Up Alerts**: Configure Grafana alerts for critical thresholds
3. **Extend Metrics**: Add custom application metrics as needed
4. **Performance Tuning**: Use insights to optimize database queries and caching
5. **Production Deployment**: Adapt configuration for staging/production environments

This monitoring setup provides the foundation for maintaining a high-performance, reliable YouthMind backend system! 🚀 