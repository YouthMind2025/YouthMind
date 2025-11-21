#!/bin/bash

# YouthMind Backend - Admin Registration Test Script
# Tests admin registration with environment variable configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
ADMIN_SECRET_KEY="${ADMIN_SECRET_KEY:-YOUTHMIND_ADMIN_2024}"
TEST_EMAIL="admin-test@youthmind.ai"
TEST_PASSWORD="TestPassword123!"
TEST_FIRST_NAME="Test"
TEST_LAST_NAME="Admin"
TEST_USERNAME="testadmin"

echo -e "${BLUE}🧪 YouthMind Backend - Admin Registration Test${NC}"
echo "=================================================="
echo ""

# Function to print test results
print_result() {
    local test_name="$1"
    local status="$2"
    local message="$3"
    
    if [ "$status" = "PASS" ]; then
        echo -e "✅ ${GREEN}PASS${NC}: $test_name - $message"
    elif [ "$status" = "FAIL" ]; then
        echo -e "❌ ${RED}FAIL${NC}: $test_name - $message"
    else
        echo -e "⚠️  ${YELLOW}WARN${NC}: $test_name - $message"
    fi
}

# Function to check if server is running
check_server() {
    echo -e "${BLUE}🔍 Checking if server is running...${NC}"
    
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_result "Server Health Check" "PASS" "Server is running and healthy"
        return 0
    else
        print_result "Server Health Check" "FAIL" "Server is not running or not accessible at $BASE_URL"
        echo "Please start the server first:"
        echo "  mvn spring-boot:run"
        echo "  or"
        echo "  java -jar target/youthmind-backend-0.0.1-SNAPSHOT.jar"
        return 1
    fi
}

# Function to test admin registration with correct secret
test_admin_registration_success() {
    echo -e "${BLUE}🔐 Testing admin registration with correct secret...${NC}"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/v1/auth/admin/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$TEST_EMAIL\",
            \"password\": \"$TEST_PASSWORD\",
            \"firstName\": \"$TEST_FIRST_NAME\",
            \"lastName\": \"$TEST_LAST_NAME\",
            \"username\": \"$TEST_USERNAME\",
            \"adminSecretKey\": \"$ADMIN_SECRET_KEY\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "201" ]; then
        print_result "Admin Registration (Success)" "PASS" "Admin registered successfully with correct secret"
        echo "Response: $body"
        return 0
    else
        print_result "Admin Registration (Success)" "FAIL" "Expected 201, got $http_code. Response: $body"
        return 1
    fi
}

# Function to test admin registration with incorrect secret
test_admin_registration_failure() {
    echo -e "${BLUE}🔒 Testing admin registration with incorrect secret...${NC}"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/v1/auth/admin/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"admin-fail@youthmind.ai\",
            \"password\": \"$TEST_PASSWORD\",
            \"firstName\": \"$TEST_FIRST_NAME\",
            \"lastName\": \"$TEST_LAST_NAME\",
            \"username\": \"testadminfail\",
            \"adminSecretKey\": \"WRONG_SECRET_KEY\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "400" ]; then
        print_result "Admin Registration (Failure)" "PASS" "Correctly rejected with wrong secret"
        echo "Response: $body"
        return 0
    else
        print_result "Admin Registration (Failure)" "FAIL" "Expected 400, got $http_code. Response: $body"
        return 1
    fi
}

# Function to test admin registration without secret
test_admin_registration_no_secret() {
    echo -e "${BLUE}🔓 Testing admin registration without secret...${NC}"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/v1/auth/admin/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"admin-nosecret@youthmind.ai\",
            \"password\": \"$TEST_PASSWORD\",
            \"firstName\": \"$TEST_FIRST_NAME\",
            \"lastName\": \"$TEST_LAST_NAME\",
            \"username\": \"testadminnosecret\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "400" ]; then
        print_result "Admin Registration (No Secret)" "PASS" "Correctly rejected without secret"
        echo "Response: $body"
        return 0
    else
        print_result "Admin Registration (No Secret)" "FAIL" "Expected 400, got $http_code. Response: $body"
        return 1
    fi
}

# Function to test admin login
test_admin_login() {
    echo -e "${BLUE}🔑 Testing admin login...${NC}"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/v1/auth/admin/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$TEST_EMAIL\",
            \"password\": \"$TEST_PASSWORD\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ]; then
        print_result "Admin Login" "PASS" "Admin login successful"
        echo "Response: $body"
        return 0
    else
        print_result "Admin Login" "FAIL" "Expected 200, got $http_code. Response: $body"
        return 1
    fi
}

# Function to test environment variable configuration
test_environment_config() {
    echo -e "${BLUE}⚙️  Testing environment variable configuration...${NC}"
    
    # Check if ADMIN_SECRET_KEY is set
    if [ -n "$ADMIN_SECRET_KEY" ]; then
        print_result "Environment Variable" "PASS" "ADMIN_SECRET_KEY is configured: ${ADMIN_SECRET_KEY:0:10}..."
    else
        print_result "Environment Variable" "FAIL" "ADMIN_SECRET_KEY is not set"
        return 1
    fi
    
    # Check if using default value
    if [ "$ADMIN_SECRET_KEY" = "YOUTHMIND_ADMIN_2024" ]; then
        print_result "Default Secret Key" "WARN" "Using default admin secret key (acceptable for development)"
    else
        print_result "Custom Secret Key" "PASS" "Using custom admin secret key"
    fi
}

# Function to test health endpoint
test_health_endpoint() {
    echo -e "${BLUE}🏥 Testing health endpoint...${NC}"
    
    local response=$(curl -s "$BASE_URL/actuator/health")
    
    if echo "$response" | grep -q '"status":"UP"'; then
        print_result "Health Endpoint" "PASS" "Health endpoint shows UP status"
        echo "Health Response: $response"
    else
        print_result "Health Endpoint" "FAIL" "Health endpoint shows DOWN status"
        echo "Health Response: $response"
        return 1
    fi
}

# Main test execution
main() {
    echo "Configuration:"
    echo "  Base URL: $BASE_URL"
    echo "  Admin Secret Key: ${ADMIN_SECRET_KEY:0:10}..."
    echo "  Test Email: $TEST_EMAIL"
    echo ""
    
    local test_results=()
    
    # Run tests
    test_environment_config && test_results+=("ENV_CONFIG:PASS") || test_results+=("ENV_CONFIG:FAIL")
    
    if check_server; then
        test_results+=("SERVER_CHECK:PASS")
        
        test_health_endpoint && test_results+=("HEALTH:PASS") || test_results+=("HEALTH:FAIL")
        
        test_admin_registration_success && test_results+=("ADMIN_REG_SUCCESS:PASS") || test_results+=("ADMIN_REG_SUCCESS:FAIL")
        
        test_admin_registration_failure && test_results+=("ADMIN_REG_FAILURE:PASS") || test_results+=("ADMIN_REG_FAILURE:FAIL")
        
        test_admin_registration_no_secret && test_results+=("ADMIN_REG_NO_SECRET:PASS") || test_results+=("ADMIN_REG_NO_SECRET:FAIL")
        
        test_admin_login && test_results+=("ADMIN_LOGIN:PASS") || test_results+=("ADMIN_LOGIN:FAIL")
    else
        test_results+=("SERVER_CHECK:FAIL")
    fi
    
    # Print summary
    echo ""
    echo -e "${BLUE}📊 Test Summary${NC}"
    echo "==============="
    
    local pass_count=0
    local fail_count=0
    
    for result in "${test_results[@]}"; do
        local test_name=$(echo "$result" | cut -d: -f1)
        local status=$(echo "$result" | cut -d: -f2)
        
        if [ "$status" = "PASS" ]; then
            echo -e "✅ ${GREEN}$test_name${NC}"
            ((pass_count++))
        else
            echo -e "❌ ${RED}$test_name${NC}"
            ((fail_count++))
        fi
    done
    
    echo ""
    echo "Results: $pass_count passed, $fail_count failed"
    
    if [ $fail_count -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Admin registration with environment variables is working correctly.${NC}"
        exit 0
    else
        echo -e "${RED}❌ Some tests failed. Please check the configuration and try again.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
