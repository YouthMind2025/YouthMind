# JMeter Stress Testing Setup for YouthMind Backend

## Overview

This guide sets up Apache JMeter to perform comprehensive stress testing on the YouthMind backend API, testing the three critical endpoints:
- `/v1/auth/register` - User registration
- `/v1/posts/feed` - Post feed retrieval  
- `/v1/posts/create` - Post creation

**Testing Goals:**
- Simulate 100-500 concurrent users
- Measure latency, throughput, and error rates
- Identify performance bottlenecks
- Monitor system behavior under load with real-time Grafana dashboards

## Installation

### Option 1: Download JMeter (Recommended)

```bash
# Download JMeter (latest version)
cd ~/Downloads
wget https://downloads.apache.org/jmeter/binaries/apache-jmeter-5.6.2.tgz

# Extract
tar -xzf apache-jmeter-5.6.2.tgz

# Move to applications directory
sudo mv apache-jmeter-5.6.2 /opt/jmeter

# Add to PATH (add to ~/.bashrc or ~/.zshrc)
export PATH=$PATH:/opt/jmeter/bin

# Verify installation
jmeter -v
```

### Option 2: Package Manager

**macOS (Homebrew):**
```bash
brew install jmeter
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install jmeter
```

**Windows:**
- Download from https://jmeter.apache.org/download_jmeter.cgi
- Extract and add to PATH

## JMeter Configuration

### Increase Memory Allocation

Edit JMeter configuration for better performance:

```bash
# Edit jmeter startup script
vim /opt/jmeter/bin/jmeter

# Or create custom JVM options
export JVM_ARGS="-Xms1g -Xmx4g -XX:MaxMetaspaceSize=256m"
```

### Install Useful Plugins

```bash
# Download JMeter Plugins Manager
cd /opt/jmeter/lib/ext
wget https://jmeter-plugins.org/get/

# Or install via JMeter GUI:
# Options → Plugins Manager → Available Plugins
# Install: 3 Basic Graphs, PerfMon, Custom Thread Groups
```

## Test Plan Structure

Our JMeter test plan will include:

1. **Test Plan: YouthMind Backend Load Test**
   - Thread Groups for different load patterns
   - HTTP Request Defaults
   - User authentication handling
   - Result collectors and reporting

2. **Thread Groups:**
   - Registration Load Test (100-500 users)
   - Feed Load Test (100-500 users)  
   - Post Creation Load Test (100-500 users)
   - Mixed Workload Test (realistic usage pattern)

3. **Monitoring Integration:**
   - Real-time metrics collection
   - Integration with Grafana dashboards
   - Performance threshold assertions

## Load Testing Scenarios

### Scenario 1: Registration Stress Test
- **Users:** 100-500 concurrent
- **Ramp-up:** 60 seconds
- **Duration:** 10 minutes
- **Target:** `/v1/auth/register`

### Scenario 2: Feed Performance Test  
- **Users:** 200-500 concurrent
- **Ramp-up:** 30 seconds
- **Duration:** 15 minutes
- **Target:** `/v1/posts/feed`

### Scenario 3: Post Creation Load Test
- **Users:** 100-300 concurrent
- **Ramp-up:** 60 seconds  
- **Duration:** 10 minutes
- **Target:** `/v1/posts/create`

### Scenario 4: Mixed Workload (Realistic Usage)
- **Registration:** 10% of traffic
- **Feed browsing:** 70% of traffic
- **Post creation:** 20% of traffic
- **Total users:** 300-500 concurrent

## Performance Targets

### Success Criteria
- **Response Time:** 95th percentile < 2 seconds
- **Throughput:** > 100 requests/second sustained
- **Error Rate:** < 1% for all endpoints
- **System Stability:** No crashes or timeouts under load

### Warning Thresholds  
- **Response Time:** 95th percentile > 3 seconds
- **Error Rate:** > 0.5%
- **Database Connections:** > 80% of pool utilization
- **Memory Usage:** > 85% of available heap

## Integration with Monitoring

### Pre-Test Checklist
```bash
# 1. Start monitoring infrastructure
cd YouthMind
docker-compose -f docker-compose.monitoring.yml up -d

# 2. Start Spring Boot backend
cd backend
java -jar target/youthmind-backend-*.jar

# 3. Open Grafana dashboard
# http://localhost:3001 - YouthMind Database & System Monitoring

# 4. Record baseline metrics (5 minutes normal operation)
```

### During Test Monitoring
- **Grafana Dashboard:** Real-time performance metrics
- **JMeter GUI:** Live test execution stats
- **System Monitoring:** CPU, memory, disk I/O
- **Database Monitoring:** Connection pools, slow queries

### Post-Test Analysis
- **JMeter Reports:** HTML dashboard with detailed metrics
- **Grafana Analysis:** Historical performance data
- **Log Analysis:** Error patterns and bottlenecks
- **Database Analysis:** Query performance under load

## Running Tests

### GUI Mode (Development/Debugging)
```bash
# Start JMeter GUI
jmeter

# Load test plan: stress-tests/youthmind-load-test.jmx
# Configure parameters and run individual tests
```

### Command Line Mode (Production Testing)
```bash
# Run specific test scenario
jmeter -n -t stress-tests/registration-load-test.jmx \
       -l results/registration-results.jtl \
       -e -o results/registration-html-report

# Run all tests with different user loads
./run-all-stress-tests.sh
```

## Expected Deliverables

### 1. Test Execution Results
- **Response time percentiles** (50th, 90th, 95th, 99th)
- **Throughput metrics** (requests/second, transactions/second)
- **Error analysis** (error rates, failure patterns)
- **Resource utilization** (CPU, memory, database connections)

### 2. Performance Analysis Report
- **Bottleneck identification** (database, application, network)
- **Scalability assessment** (linear vs non-linear scaling)
- **Breaking point analysis** (maximum concurrent users)
- **Stability evaluation** (performance degradation over time)

### 3. Improvement Recommendations
- **Database optimizations** (indexing, query tuning, connection pooling)
- **Application tuning** (caching strategies, async processing)
- **Infrastructure scaling** (horizontal vs vertical scaling)
- **Monitoring enhancements** (alert thresholds, SLA definitions)

## Next Steps

1. **Install JMeter** using the guide above
2. **Create test plans** for the three target endpoints
3. **Configure load scenarios** with realistic user patterns
4. **Execute baseline tests** with monitoring enabled
5. **Analyze results** and identify optimization opportunities
6. **Document findings** and create improvement roadmap

This comprehensive stress testing approach will provide valuable insights into the YouthMind backend's performance characteristics and help ensure it can handle production load effectively! 🚀 