#!/bin/bash

# Quick single test runner for YouthMind stress tests
# Usage: ./run-single-test.sh [test-name] [users] [base-url]

set -e

# Configuration
TEST_NAME=${1:-"registration"}
USERS=${2:-100}
BASE_URL=${3:-"localhost:8080"}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Create directories
mkdir -p results reports tokens

# Map test names to files
case $TEST_NAME in
    "registration" | "reg")
        TEST_FILE="registration-load-test.jmx"
        RAMP_UP=60
        DURATION=300
        ;;
    "feed")
        TEST_FILE="feed-load-test.jmx"
        RAMP_UP=30
        DURATION=300
        ;;
    "post" | "post-creation")
        TEST_FILE="post-creation-load-test.jmx"
        RAMP_UP=60
        DURATION=300
        ;;
    *)
        print_error "Unknown test: $TEST_NAME"
        echo "Available tests: registration, feed, post-creation"
        exit 1
        ;;
esac

print_info "Running $TEST_NAME test with $USERS users"
print_info "Test file: $TEST_FILE"
print_info "Base URL: http://$BASE_URL"

# Check if backend is running
if curl -f -s http://${BASE_URL}/actuator/health > /dev/null; then
    print_success "Backend is responding"
else
    print_error "Backend is not responding at http://${BASE_URL}"
    exit 1
fi

# Run the test
RESULT_FILE="results/${TEST_NAME}_${USERS}users_${TIMESTAMP}.jtl"
REPORT_DIR="reports/${TEST_NAME}_${USERS}users_${TIMESTAMP}"

print_info "Starting JMeter test..."

jmeter -n -t ${TEST_FILE} \
       -JbaseUrl=${BASE_URL} \
       -Jusers=${USERS} \
       -JrampUp=${RAMP_UP} \
       -Jduration=${DURATION} \
       -l ${RESULT_FILE} \
       -e -o ${REPORT_DIR}

if [ $? -eq 0 ]; then
    print_success "Test completed successfully!"
    print_info "Results: ${RESULT_FILE}"
    print_info "HTML Report: ${REPORT_DIR}/index.html"
    print_info "View report: open ${REPORT_DIR}/index.html"
else
    print_error "Test failed!"
    exit 1
fi 