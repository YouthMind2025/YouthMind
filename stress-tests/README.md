# YouthMind Backend Stress Testing Guide

## Overview

This directory contains comprehensive JMeter stress tests for the YouthMind backend API. The tests are designed to simulate real-world load conditions and measure system performance under stress.

### Test Coverage
- **User Registration** (`/v1/auth/register`) - 100-500 concurrent users
- **Posts Feed** (`/v1/posts/feed`) - 200-600 concurrent users  
- **Post Creation** (`/v1/posts/create`) - 100-300 concurrent users

### Performance Goals
- **Response Time**: 95th percentile < 2 seconds
- **Throughput**: > 100 requests/second sustained
- **Error Rate**: < 1% for all endpoints
- **System Stability**: No crashes or timeouts under load

## Prerequisites

### Required Software
1. **Apache JMeter 5.6+**
2. **Java 17+** (for JMeter and YouthMind backend)
3. **Docker & Docker Compose** (for monitoring infrastructure)
4. **Maven** (to build backend)

### Installation Instructions

#### Windows
```cmd
# Download JMeter from https://jmeter.apache.org/download_jmeter.cgi
# Extract to C:\jmeter and add C:\jmeter\bin to PATH

# Or use package managers:
choco install jmeter
# OR
scoop install jmeter

# Verify installation
jmeter -v
```

#### macOS
```bash
# Using Homebrew
brew install jmeter

# Verify installation
jmeter -v
```

#### Linux (Ubuntu/Debian)
```bash
# Using apt
sudo apt update
sudo apt install jmeter

# Or download manually
wget https://downloads.apache.org/jmeter/binaries/apache-jmeter-5.6.2.tgz
tar -xzf apache-jmeter-5.6.2.tgz
sudo mv apache-jmeter-5.6.2 /opt/jmeter
export PATH=$PATH:/opt/jmeter/bin

# Verify installation
jmeter -v
```

## Test Files Structure

```
stress-tests/
├── README.md                     # This file
├── registration-load-test.jmx     # User registration stress test
├── feed-load-test.jmx             # Posts feed performance test
├── post-creation-load-test.jmx    # Post creation load test
├── run-all-stress-tests.sh        # Automated test runner (Unix/Linux/macOS)
├── run-single-test.sh             # Single test runner (Unix/Linux/macOS)
├── data/
│   └── post-content.csv           # Realistic test data for post creation
├── results/                       # Test execution results (generated)
├── reports/                       # HTML test reports (generated)
└── tokens/                        # Authentication tokens (generated)
```

## Setup Instructions

### 1. Start Monitoring Infrastructure

**All Platforms:**
```bash
# Navigate to YouthMind root directory
cd YouthMind

# Start monitoring stack (PostgreSQL, Redis, Prometheus, Grafana)
docker-compose -f docker-compose.monitoring.yml up -d

# Verify services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. Start YouthMind Backend

```bash
# Navigate to backend directory
cd backend

# Build the application
mvn clean package

# Start the backend (leave this running)
java -jar target/youthmind-backend-*.jar
```

### 3. Verify System Health

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check monitoring services
curl http://localhost:9090/-/healthy    # Prometheus
curl http://localhost:3001/api/health   # Grafana
```

Expected responses:
- Backend: `{"status":"UP",...}`
- Prometheus: `Prometheus is Healthy.`
- Grafana: `{"database":"ok","version":"..."}`

### 4. Open Monitoring Dashboards

- **Grafana**: http://localhost:3001 (admin/admin)
  - Navigate to "YouthMind Database & System Monitoring" dashboard
- **Prometheus**: http://localhost:9090

## Running Stress Tests

### Option 1: Automated Test Suite (Recommended)

**Unix/Linux/macOS:**
```bash
cd stress-tests

# Make scripts executable
chmod +x *.sh

# Run all tests with multiple user loads
./run-all-stress-tests.sh

# Run all tests with custom base URL
./run-all-stress-tests.sh your-server:8080
```

**Windows (using Git Bash):**
```bash
cd stress-tests
bash run-all-stress-tests.sh
```

### Option 2: Single Test Execution

**Unix/Linux/macOS:**
```bash
cd stress-tests

# Quick tests (5 minutes each)
./run-single-test.sh registration 100
./run-single-test.sh feed 200
./run-single-test.sh post 150

# Custom configuration
./run-single-test.sh registration 500 localhost:8080
```

**Windows:**
```cmd
cd stress-tests
bash run-single-test.sh registration 100
```

### Option 3: Direct JMeter Commands

#### Registration Stress Test
```bash
# Create directories
mkdir -p results reports tokens

# Run test
jmeter -n -t registration-load-test.jmx \
       -JbaseUrl=localhost:8080 \
       -Jusers=100 \
       -JrampUp=60 \
       -Jduration=600 \
       -l results/registration-100users.jtl \
       -e -o reports/registration-100users
```

#### Feed Performance Test
```bash
jmeter -n -t feed-load-test.jmx \
       -JbaseUrl=localhost:8080 \
       -Jusers=200 \
       -JrampUp=30 \
       -Jduration=900 \
       -l results/feed-200users.jtl \
       -e -o reports/feed-200users
```

#### Post Creation Test
```bash
jmeter -n -t post-creation-load-test.jmx \
       -JbaseUrl=localhost:8080 \
       -Jusers=150 \
       -JrampUp=60 \
       -Jduration=600 \
       -l results/post-creation-150users.jtl \
       -e -o reports/post-creation-150users
```

### Windows-Specific Commands

```cmd
REM Create directories
mkdir results reports tokens

REM Registration test
jmeter -n -t registration-load-test.jmx -JbaseUrl=localhost:8080 -Jusers=100 -JrampUp=60 -Jduration=600 -l results\registration-100users.jtl -e -o reports\registration-100users

REM Feed test
jmeter -n -t feed-load-test.jmx -JbaseUrl=localhost:8080 -Jusers=200 -JrampUp=30 -Jduration=900 -l results\feed-200users.jtl -e -o reports\feed-200users

REM Post creation test
jmeter -n -t post-creation-load-test.jmx -JbaseUrl=localhost:8080 -Jusers=150 -JrampUp=60 -Jduration=600 -l results\post-creation-150users.jtl -e -o reports\post-creation-150users
```

## Test Parameters

### Configurable Parameters
- **baseUrl**: Backend server URL (default: localhost:8080)
- **users**: Number of concurrent users (default: varies by test)
- **rampUp**: Time to ramp up to full load in seconds
- **duration**: Test duration in seconds

### Test Scenarios

#### 1. Registration Stress Test
- **Purpose**: Test user registration under load
- **Endpoint**: `/v1/auth/register`
- **Default Users**: 100-500 concurrent
- **Ramp-up**: 60 seconds
- **Duration**: 10 minutes
- **Features**: 
  - Unique email generation
  - Response validation
  - Performance assertions

#### 2. Feed Performance Test
- **Purpose**: Test feed retrieval with authentication
- **Endpoint**: `/v1/posts/feed`
- **Default Users**: 200-600 concurrent
- **Ramp-up**: 30 seconds
- **Duration**: 15 minutes
- **Features**:
  - Pre-created authenticated users
  - Pagination testing
  - Think time simulation

#### 3. Post Creation Load Test
- **Purpose**: Test post creation under load
- **Endpoint**: `/v1/posts/create`
- **Default Users**: 100-300 concurrent
- **Ramp-up**: 60 seconds
- **Duration**: 10 minutes
- **Features**:
  - Realistic post content from CSV
  - Authentication required
  - Varied emotion tags

## Analyzing Results

### 1. JMeter HTML Reports

After each test, navigate to the generated report:

**Unix/Linux/macOS:**
```bash
open reports/[test-name]/index.html
```

**Windows:**
```cmd
start reports\[test-name]\index.html
```

### Key Metrics to Review:

#### Response Times
- **Average**: Should be < 1 second for good UX
- **90th Percentile**: Should be < 2 seconds
- **95th Percentile**: Should be < 3 seconds
- **99th Percentile**: Should be < 5 seconds

#### Throughput
- **Requests/second**: Target > 100 req/sec sustained
- **Transactions/second**: Measure of successful operations

#### Error Analysis
- **Error Rate**: Should be < 1%
- **Error Types**: Identify patterns in failures
- **Response Codes**: Check for 4xx/5xx errors

### 2. Real-time Monitoring

During tests, monitor via Grafana dashboard:

#### Database Metrics
- Query execution times
- Connection pool utilization
- Slow query identification
- Transaction rates

#### Application Metrics
- JVM memory usage
- Garbage collection patterns
- Thread pool utilization
- API response times

#### System Resources
- CPU utilization
- Memory usage
- Network I/O
- Disk I/O

### 3. Performance Analysis

#### Identify Bottlenecks
1. **Database**: Slow queries, connection limits
2. **Application**: Memory leaks, thread starvation
3. **Network**: Bandwidth limitations
4. **Infrastructure**: CPU/memory constraints

#### Scalability Assessment
- **Linear scaling**: Performance improves with resources
- **Breaking point**: Maximum sustainable load
- **Degradation patterns**: How performance degrades under stress

## Test Data

### Post Content Data
The `data/post-content.csv` file contains 50 realistic post examples with:
- Mental health-related content appropriate for youth
- Various emotion tags (happy, sad, anxious, proud, etc.)
- Authentic language and scenarios

### Authentication
- Tests automatically create and manage user accounts
- Authentication tokens are cached in `tokens/` directory
- Cleanup happens between test runs

## Troubleshooting

### Common Issues

#### 1. Backend Not Responding
```bash
# Check if backend is running
curl http://localhost:8080/actuator/health

# If not running, start it:
cd backend
java -jar target/youthmind-backend-*.jar
```

#### 2. Database Connection Errors
```bash
# Check if monitoring stack is running
docker-compose -f docker-compose.monitoring.yml ps

# Restart if needed
docker-compose -f docker-compose.monitoring.yml restart postgres
```

#### 3. JMeter Out of Memory
```bash
# Increase JMeter heap size
export JVM_ARGS="-Xms1g -Xmx4g -XX:MaxMetaspaceSize=256m"
jmeter [your-test-command]
```

#### 4. High Error Rates
- Check Grafana for database connection pool exhaustion
- Monitor CPU/memory usage during tests
- Review application logs for exceptions
- Verify test data is valid

#### 5. Permission Denied (Unix/Linux)
```bash
chmod +x *.sh
```

### Performance Tuning Tips

#### For Higher Loads
1. **Increase connection pools**: Backend database configuration
2. **Tune JVM settings**: Garbage collection and heap size
3. **Optimize queries**: Add indexes, optimize slow queries
4. **Cache frequently accessed data**: Use Redis effectively

#### For Stability
1. **Monitor memory leaks**: Check for increasing memory usage
2. **Watch connection leaks**: Ensure proper connection cleanup
3. **Set timeouts**: Prevent hanging requests
4. **Implement circuit breakers**: Fail fast on errors

## Expected Deliverables

After running the stress tests, you should have:

### 1. Performance Test Results
- Response time percentiles for each endpoint
- Throughput metrics under various loads
- Error rates and failure analysis
- Resource utilization patterns

### 2. Bottleneck Identification
- Database performance under load
- Application server limitations
- Network and infrastructure constraints
- Scalability patterns and breaking points

### 3. Improvement Recommendations
- Database optimization suggestions
- Application tuning recommendations
- Infrastructure scaling guidance
- Monitoring and alerting setup

### 4. Test Reports
- HTML reports with detailed metrics
- Summary reports across all tests
- Grafana dashboard screenshots
- Performance analysis documentation

## Integration with CI/CD

### Running Tests in Automation

```bash
# Example Jenkins/GitHub Actions script
./run-all-stress-tests.sh production-server:8080

# Check exit codes
if [ $? -eq 0 ]; then
    echo "All stress tests passed"
else
    echo "Stress tests failed"
    exit 1
fi
```

### Performance Thresholds

Set up automated checks for:
- Response time SLA violations
- Error rate thresholds
- Throughput degradation
- Resource usage limits

## Contact and Support

For questions about these stress tests:
- **Documentation**: This README and `../JMETER_SETUP.md`
- **Monitoring Setup**: `../MONITORING_SETUP.md`
- **Backend Configuration**: `../backend/README.md`

---

**Happy Load Testing! 🚀**

*Remember: The goal is not just to break the system, but to understand its behavior under realistic load conditions and identify opportunities for optimization.* 