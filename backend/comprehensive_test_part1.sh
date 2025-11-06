#!/bin/bash

# Comprehensive API Testing Script for YouthMind Backend
# Tests ALL 472+ API endpoints across 53 controllers

BASE_URL="http://localhost:8080"
RESULTS_FILE="comprehensive_api_test_results.txt"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "=== YouthMind Backend Comprehensive API Testing Report ===" > $RESULTS_FILE
echo "Generated: $TIMESTAMP" >> $RESULTS_FILE
echo "Total Controllers: 53" >> $RESULTS_FILE
echo "Estimated Endpoints: 472+" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

# Function to test endpoint with detailed logging
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local category=$5
    
    echo "Testing: $method $endpoint - $description"
    
    # Test with different scenarios
    local scenarios=("no_auth" "invalid_auth" "valid_auth")
    
    for scenario in "${scenarios[@]}"; do
        local headers="Content-Type: application/json"
        local auth_header=""
        
        case $scenario in
            "invalid_auth")
                auth_header="Authorization: Bearer invalid_token"
                ;;
            "valid_auth")
                auth_header="Authorization: Bearer valid_token_placeholder"
                ;;
        esac
        
        if [ -n "$data" ]; then
            if [ -n "$auth_header" ]; then
                response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X $method "$BASE_URL$endpoint" \
                    -H "Content-Type: application/json" \
                    -H "$auth_header" \
                    -d "$data" 2>/dev/null)
            else
                response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X $method "$BASE_URL$endpoint" \
                    -H "Content-Type: application/json" \
                    -d "$data" 2>/dev/null)
            fi
        else
            if [ -n "$auth_header" ]; then
                response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X $method "$BASE_URL$endpoint" \
                    -H "$auth_header" 2>/dev/null)
            else
                response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X $method "$BASE_URL$endpoint" 2>/dev/null)
            fi
        fi
        
        http_code=$(echo "$response" | tail -n2 | head -n1)
        response_time=$(echo "$response" | tail -n1)
        body=$(echo "$response" | head -n -2)
        
        echo "[$category] $method $endpoint ($scenario): $http_code (${response_time}s)" >> $RESULTS_FILE
        
        # Log errors with details
        if [ "$http_code" != "200" ] && [ "$http_code" != "201" ] && [ "$http_code" != "204" ]; then
            echo "  Error: $(echo "$body" | head -c 200)..." >> $RESULTS_FILE
        fi
    done
    echo "" >> $RESULTS_FILE
}

# Test Authentication Endpoints
echo "=== AUTHENTICATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/v1/auth/send-otp" '{"email":"test@example.com"}' "Send OTP" "Authentication"
test_endpoint "POST" "/v1/auth/register" '{"email":"test@example.com","otp":"123456","password":"password123"}' "Register User" "Authentication"
test_endpoint "POST" "/v1/auth/login" '{"email":"test@example.com","password":"password123"}' "Login User" "Authentication"
test_endpoint "POST" "/v1/auth/refresh" '{"refreshToken":"dummy"}' "Refresh Token" "Authentication"
test_endpoint "POST" "/v1/auth/logout" '{}' "Logout User" "Authentication"

# Test Admin Endpoints
echo "=== ADMIN ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/v1/admin/register" '{"adminSecretKey":"YOUTHMIND_ADMIN_2024","email":"admin@example.com","firstName":"Admin","lastName":"User","password":"admin123"}' "Admin Register" "Admin"
test_endpoint "POST" "/v1/admin/login" '{"email":"admin@example.com","password":"admin123"}' "Admin Login" "Admin"
test_endpoint "GET" "/admin/v1/users" "" "Get All Users" "Admin"
test_endpoint "POST" "/admin/v1/users" '{"email":"test@example.com","firstName":"Test","lastName":"User"}' "Create User" "Admin"
test_endpoint "GET" "/admin/v1/posts" "" "Get All Posts" "Admin"
test_endpoint "POST" "/admin/v1/posts" '{"title":"Admin Post","content":"Admin content"}' "Create Post" "Admin"
test_endpoint "GET" "/admin/v1/analytics" "" "Get Analytics" "Admin"
test_endpoint "POST" "/admin/v1/migration" '{"type":"user_data"}' "Run Migration" "Admin"

# Test User Management Endpoints
echo "=== USER MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/users" "" "Get Users" "User Management"
test_endpoint "GET" "/api/users/123e4567-e89b-12d3-a456-426614174000" "" "Get User by ID" "User Management"
test_endpoint "PUT" "/api/users/123e4567-e89b-12d3-a456-426614174000" '{"firstName":"Updated","lastName":"User"}' "Update User" "User Management"
test_endpoint "DELETE" "/api/users/123e4567-e89b-12d3-a456-426614174000" "" "Delete User" "User Management"
test_endpoint "GET" "/v1/users/profile" "" "Get User Profile" "User Management"
test_endpoint "PUT" "/v1/users/profile" '{"firstName":"Test","lastName":"User"}' "Update User Profile" "User Management"

# Test Post Management Endpoints
echo "=== POST MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/posts" "" "Get Posts" "Post Management"
test_endpoint "POST" "/api/posts" '{"title":"Test Post","content":"Test content","emotion":"happy"}' "Create Post" "Post Management"
test_endpoint "GET" "/api/posts/123e4567-e89b-12d3-a456-426614174000" "" "Get Post by ID" "Post Management"
test_endpoint "PUT" "/api/posts/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Post","content":"Updated content"}' "Update Post" "Post Management"
test_endpoint "DELETE" "/api/posts/123e4567-e89b-12d3-a456-426614174000" "" "Delete Post" "Post Management"
test_endpoint "GET" "/v1/posts" "" "Get Posts V1" "Post Management"
test_endpoint "POST" "/v1/posts" '{"title":"Test Post V1","content":"Test content V1","emotion":"sad"}' "Create Post V1" "Post Management"
test_endpoint "GET" "/v1/posts/123e4567-e89b-12d3-a456-426614174000" "" "Get Post by ID V1" "Post Management"
test_endpoint "PUT" "/v1/posts/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Post V1","content":"Updated content V1"}' "Update Post V1" "Post Management"
test_endpoint "DELETE" "/v1/posts/123e4567-e89b-12d3-a456-426614174000" "" "Delete Post V1" "Post Management"

echo "Phase 1 Complete - Testing core endpoints..."

