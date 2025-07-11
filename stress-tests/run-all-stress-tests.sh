#!/bin/bash

# YouthMind Backend Stress Testing Script
# This script runs all stress tests with different user loads and generates reports

set -e

# Configuration
BASE_URL=${1:-"localhost:8080"}
RESULTS_DIR="results"
REPORTS_DIR="reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create directories
mkdir -p ${RESULTS_DIR}
mkdir -p ${REPORTS_DIR}
mkdir -p tokens

# Function to print colored output
print_status() {
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

# Function to check if JMeter is installed
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        print_error "JMeter is not installed or not in PATH"
        print_status "Please install JMeter and add it to your PATH"
        exit 1
    fi
    print_success "JMeter found: $(jmeter -v | head -1)"
}

# Function to check if backend is running
check_backend() {
    print_status "Checking if YouthMind backend is running..."
    if curl -f -s http://${BASE_URL}/actuator/health > /dev/null; then
        print_success "Backend is running at http://${BASE_URL}"
    else
        print_error "Backend is not responding at http://${BASE_URL}"
        print_status "Please start the backend with: java -jar target/youthmind-backend-*.jar"
        exit 1
    fi
}

# Function to run a JMeter test
run_test() {
    local test_name=$1
    local test_file=$2
    local users=$3
    local ramp_up=$4
    local duration=$5
    
    print_status "Running ${test_name} (${users} users, ${ramp_up}s ramp-up, ${duration}s duration)..."
    
    local result_file="${RESULTS_DIR}/${test_name}_${users}users_${TIMESTAMP}.jtl"
    local report_dir="${REPORTS_DIR}/${test_name}_${users}users_${TIMESTAMP}"
    
    jmeter -n -t ${test_file} \
           -JbaseUrl=${BASE_URL} \
           -Jusers=${users} \
           -JrampUp=${ramp_up} \
           -Jduration=${duration} \
           -l ${result_file} \
           -e -o ${report_dir}
    
    if [ $? -eq 0 ]; then
        print_success "${test_name} completed successfully"
        print_status "Results: ${result_file}"
        print_status "Report: ${report_dir}/index.html"
    else
        print_error "${test_name} failed"
        return 1
    fi
}

# Function to cleanup old tokens
cleanup_tokens() {
    print_status "Cleaning up old auth tokens..."
    rm -f tokens/*.txt
}

# Main execution
main() {
    echo "========================================"
    echo "YouthMind Backend Stress Testing Suite"
    echo "========================================"
    echo ""
    
    # Pre-checks
    check_jmeter
    check_backend
    cleanup_tokens
    
    print_status "Starting stress tests at $(date)"
    print_status "Base URL: http://${BASE_URL}"
    echo ""
    
    # Test configurations: [users, ramp_up, duration]
    declare -a test_configs=(
        "100 60 600"   # 100 users, 1min ramp-up, 10min test
        "200 120 600"  # 200 users, 2min ramp-up, 10min test
        "300 180 900"  # 300 users, 3min ramp-up, 15min test
        "500 300 900"  # 500 users, 5min ramp-up, 15min test
    )
    
    # Registration Load Tests
    print_status "========== REGISTRATION LOAD TESTS =========="
    for config in "${test_configs[@]}"; do
        read -r users ramp_up duration <<< "$config"
        run_test "registration" "registration-load-test.jmx" $users $ramp_up $duration
        sleep 30  # Cool down between tests
    done
    
    # Feed Load Tests  
    print_status "========== FEED LOAD TESTS =========="
    for config in "${test_configs[@]}"; do
        read -r users ramp_up duration <<< "$config"
        # Use higher user counts for feed tests (read-heavy)
        feed_users=$((users + 100))
        run_test "feed" "feed-load-test.jmx" $feed_users 30 $duration
        sleep 30
    done
    
    # Post Creation Load Tests
    print_status "========== POST CREATION LOAD TESTS =========="
    for config in "${test_configs[@]}"; do
        read -r users ramp_up duration <<< "$config"
        # Use lower user counts for post creation (write-heavy)
        post_users=$((users < 300 ? users : 300))
        run_test "post-creation" "post-creation-load-test.jmx" $post_users $ramp_up $duration
        sleep 60  # Longer cool down for write tests
    done
    
    # Generate summary report
    generate_summary_report
    
    print_success "All stress tests completed!"
    print_status "Check the reports directory for detailed results: ${REPORTS_DIR}/"
    print_status "Test completed at: $(date)"
}

# Function to generate summary report
generate_summary_report() {
    print_status "Generating summary report..."
    
    local summary_file="${REPORTS_DIR}/stress_test_summary_${TIMESTAMP}.html"
    
    cat > ${summary_file} << EOF
<!DOCTYPE html>
<html>
<head>
    <title>YouthMind Stress Test Summary - ${TIMESTAMP}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .test-section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { padding: 8px; text-align: left; border: 1px solid #ddd; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>YouthMind Backend Stress Test Summary</h1>
        <p><strong>Test Date:</strong> $(date)</p>
        <p><strong>Base URL:</strong> http://${BASE_URL}</p>
        <p><strong>Test ID:</strong> ${TIMESTAMP}</p>
    </div>
    
    <div class="test-section">
        <h2>Test Configuration</h2>
        <table>
            <tr><th>Test Type</th><th>User Loads Tested</th><th>Endpoints</th></tr>
            <tr><td>Registration</td><td>100, 200, 300, 500</td><td>/v1/auth/register</td></tr>
            <tr><td>Feed Access</td><td>200, 300, 400, 600</td><td>/v1/posts/feed</td></tr>
            <tr><td>Post Creation</td><td>100, 200, 300, 300</td><td>/v1/posts/create</td></tr>
        </table>
    </div>
    
    <div class="test-section">
        <h2>Generated Reports</h2>
        <p>Individual test reports are available in the following directories:</p>
        <ul>
EOF

    # List all generated reports
    for report_dir in ${REPORTS_DIR}/*_${TIMESTAMP}; do
        if [ -d "$report_dir" ]; then
            echo "            <li><a href=\"$(basename $report_dir)/index.html\">$(basename $report_dir)</a></li>" >> ${summary_file}
        fi
    done

    cat >> ${summary_file} << EOF
        </ul>
    </div>
    
    <div class="test-section">
        <h2>Key Metrics to Analyze</h2>
        <ul>
            <li><strong>Response Time:</strong> 95th percentile should be &lt; 2 seconds</li>
            <li><strong>Throughput:</strong> Should maintain &gt; 100 requests/second</li>
            <li><strong>Error Rate:</strong> Should be &lt; 1% for all endpoints</li>
            <li><strong>System Stability:</strong> No crashes or memory leaks</li>
        </ul>
    </div>
    
    <div class="test-section">
        <h2>Monitoring</h2>
        <p>Monitor the following during test analysis:</p>
        <ul>
            <li><a href="http://localhost:3001" target="_blank">Grafana Dashboard</a> - Real-time system metrics</li>
            <li><a href="http://localhost:9090" target="_blank">Prometheus</a> - Raw metrics and queries</li>
            <li>Database connection pools and query performance</li>
            <li>Memory usage and garbage collection patterns</li>
        </ul>
    </div>
</body>
</html>
EOF

    print_success "Summary report generated: ${summary_file}"
}

# Handle script interruption
trap 'print_warning "Test interrupted. Cleaning up..."; cleanup_tokens; exit 1' INT TERM

# Run main function
main "$@" 