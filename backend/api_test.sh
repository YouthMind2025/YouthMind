#!/bin/bash

# Comprehensive API Testing Script for YouthMind Backend
# Tests all API endpoints systematically

BASE_URL="http://localhost:8080"
RESULTS_FILE="api_test_results.txt"

echo "=== YouthMind Backend API Testing Report ===" > $RESULTS_FILE
echo "Generated: $(date)" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "Testing: $method $endpoint - $description"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    echo "$method $endpoint: $http_code" >> $RESULTS_FILE
    if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
        echo "  Error: $body" >> $RESULTS_FILE
    fi
    echo "" >> $RESULTS_FILE
}

# Test Authentication Endpoints
echo "=== AUTHENTICATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/v1/auth/send-otp" '{"email":"test@example.com"}' "Send OTP"
test_endpoint "POST" "/v1/auth/register" '{"email":"test@example.com","otp":"123456","password":"password123"}' "Register User"
test_endpoint "POST" "/v1/auth/login" '{"email":"test@example.com","password":"password123"}' "Login User"
test_endpoint "POST" "/v1/auth/refresh" '{"refreshToken":"dummy"}' "Refresh Token"
test_endpoint "POST" "/v1/auth/logout" '{}' "Logout User"

# Test Admin Endpoints
echo "=== ADMIN ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/v1/admin/register" '{"adminSecretKey":"YOUTHMIND_ADMIN_2024","email":"admin@example.com","firstName":"Admin","lastName":"User","password":"admin123"}' "Admin Register"
test_endpoint "POST" "/v1/admin/login" '{"email":"admin@example.com","password":"admin123"}' "Admin Login"

# Test Public Endpoints
echo "=== PUBLIC ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/" "Welcome Page"
test_endpoint "GET" "/actuator/health" "Health Check"
test_endpoint "GET" "/actuator/info" "Application Info"
test_endpoint "GET" "/actuator/metrics" "Metrics"
test_endpoint "GET" "/api-docs" "API Documentation"

# Test User Endpoints (will fail without auth)
echo "=== USER ENDPOINTS (Requires Auth) ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/users/profile" "Get User Profile"
test_endpoint "PUT" "/v1/users/profile" '{"firstName":"Test","lastName":"User"}' "Update User Profile"

# Test Post Endpoints (will fail without auth)
echo "=== POST ENDPOINTS (Requires Auth) ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/posts" "Get Posts"
test_endpoint "POST" "/v1/posts" '{"title":"Test Post","content":"Test content","emotion":"happy"}' "Create Post"

echo "API Testing Complete. Results saved to $RESULTS_FILE"

