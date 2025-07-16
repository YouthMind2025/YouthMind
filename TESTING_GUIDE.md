# Testing Guide: YouthMind Monitoring Infrastructure

## Step-by-Step Testing Process

### Step 1: Start the Monitoring Stack

```bash
# Navigate to the YouthMind directory
cd YouthMind

# Start all monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Wait a few seconds for services to initialize
sleep 10

# Check that all services are running
docker-compose -f docker-compose.monitoring.yml ps
```

Expected output - all services should show "Up":
```
Name                          Command               State          Ports
youthmind-grafana             /run.sh                           Up      0.0.0.0:3001->3000/tcp
youthmind-postgres            docker-entrypoint.sh postgres    Up      0.0.0.0:5432->5432/tcp
youthmind-postgres-exporter   /bin/postgres_exporter           Up      0.0.0.0:9187->9187/tcp
youthmind-prometheus          /bin/prometheus --config.f...    Up      0.0.0.0:9090->9090/tcp
youthmind-redis               docker-entrypoint.sh redis...    Up      0.0.0.0:6379->6379/tcp
youthmind-redis-exporter      /redis_exporter                  Up      0.0.0.0:9121->9121/tcp
```

### Step 2: Build and Start the Spring Boot Backend

```bash
# Navigate to backend directory
cd backend

# Build the application
mvn clean package

# Start the backend (this will run in foreground)
java -jar target/youthmind-backend-*.jar
```

Expected output - should see Spring Boot startup logs ending with:
```
Started YouthmindBackendApplication in X.XXX seconds
```

### Step 3: Verify Service Health

Open a new terminal window and run these health checks:

```bash
# Check Spring Boot health
curl http://localhost:8080/actuator/health

# Check Prometheus health  
curl http://localhost:9090/-/healthy

# Check Grafana health
curl http://localhost:3001/api/health

# Check PostgreSQL connection
docker exec youthmind-postgres pg_isready -U youthmind_user -d youthmind_db

# Check Redis connection
docker exec youthmind-redis redis-cli ping
```

Expected responses:
- Spring Boot: `{"status":"UP",...}`
- Prometheus: `Prometheus is Healthy.`
- Grafana: `{"commit":"...","database":"ok","version":"..."}`
- PostgreSQL: `...accepting connections`
- Redis: `PONG`

### Step 4: Verify Metrics Collection

```bash
# Check Spring Boot metrics endpoint
curl http://localhost:8080/actuator/prometheus | head -20

# Check PostgreSQL exporter metrics
curl http://localhost:9187/metrics | grep postgres

# Check Redis exporter metrics  
curl http://localhost:9121/metrics | grep redis
```

Expected: Should see metrics output with timestamps and values.

### Step 5: Test Prometheus Targets

1. Open Prometheus: http://localhost:9090
2. Go to Status → Targets
3. Verify all targets are "UP":
   - prometheus (localhost:9090)
   - youthmind-backend (host.docker.internal:8080)
   - postgres (postgres-exporter:9187)
   - redis (redis-exporter:9121)

### Step 6: Generate Test Data

```bash
# Test the registration endpoint to generate database activity
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpassword123",
    "firstName": "Test",
    "lastName": "User"
  }'

# Make a few more requests to generate metrics
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com", 
    "password": "testpassword123",
    "firstName": "Test2",
    "lastName": "User2"
  }'

# Test some other endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
```

### Step 7: Verify Metrics Are Being Collected

```bash
# Check that database metrics are being collected
curl http://localhost:8080/actuator/prometheus | grep hikaricp

# Check for HTTP request metrics
curl http://localhost:8080/actuator/prometheus | grep http_server_requests

# Query Prometheus for specific metrics
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=pg_stat_database_xact_commit'
```

### Step 8: Test Grafana Dashboard

1. Open Grafana: http://localhost:3001
2. Login with:
   - Username: `admin`
   - Password: `admin`
3. Skip the password change for now
4. Navigate to "Dashboards" → "YouthMind Database & System Monitoring"
5. Verify panels show data:
   - Database Transaction Rate
   - Database Connections  
   - Table Scan Rates
   - Redis Performance
   - API Request Rate

### Step 9: Verify Log Aggregation

```bash
# Check PostgreSQL logs
cat monitoring/postgres/logs/postgresql.log

# Check application logs
docker logs youthmind-backend

# Check Redis logs
docker logs youthmind-redis
```

## Troubleshooting Common Issues

### Issue: Services not starting

```bash
# Check individual service logs
docker-compose -f docker-compose.monitoring.yml logs postgres
docker-compose -f docker-compose.monitoring.yml logs prometheus
docker-compose -f docker-compose.monitoring.yml logs grafana
```

### Issue: Spring Boot can't connect to database

```bash
# Check if PostgreSQL is accepting connections
docker exec youthmind-postgres pg_isready -U youthmind_user

# Check Spring Boot logs for connection errors
tail -f logs/spring.log
```

### Issue: No metrics in Grafana

1. Check Prometheus targets: http://localhost:9090/targets
2. Verify Spring Boot actuator: http://localhost:8080/actuator/prometheus
3. Check Grafana data source configuration

### Issue: Permission denied or port conflicts

```bash
# Stop all services
docker-compose -f docker-compose.monitoring.yml down

# Check what's using the ports
netstat -tulpn | grep :3001
netstat -tulpn | grep :9090
netstat -tulpn | grep :5432

# Restart with clean slate
docker-compose -f docker-compose.monitoring.yml up -d
```

## Success Criteria ✅

Your monitoring setup is working correctly if:

- [ ] All Docker containers are running (`docker ps`)
- [ ] Spring Boot starts without errors
- [ ] All health checks return positive responses
- [ ] Prometheus shows all targets as "UP"
- [ ] Grafana dashboard displays metrics and charts
- [ ] Database queries generate visible metrics
- [ ] Logs are being written to monitoring/postgres/logs/
- [ ] API requests show up in request rate charts

## Performance Test Preparation

Once all tests pass, your monitoring infrastructure is ready for JMeter stress testing:

1. **Baseline Recording**: Let the system run for 5-10 minutes to establish baseline metrics
2. **Monitoring Dashboard**: Keep Grafana open during stress tests
3. **Resource Monitoring**: Watch for CPU, memory, and database connection spikes
4. **Alert Thresholds**: Note when performance degrades during load testing

## Clean Up (Optional)

To stop and remove all monitoring services:

```bash
# Stop services
docker-compose -f docker-compose.monitoring.yml down

# Remove volumes (WARNING: This deletes all monitoring data)
docker-compose -f docker-compose.monitoring.yml down -v

# Remove unused Docker images
docker system prune -f
``` 