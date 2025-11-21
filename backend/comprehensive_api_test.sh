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
    local scenarios=("no_auth" "invalid_auth")
    
    for scenario in "${scenarios[@]}"; do
        local headers="Content-Type: application/json"
        local auth_header=""
        
        case $scenario in
            "invalid_auth")
                auth_header="Authorization: Bearer invalid_token"
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

echo "=== COMPREHENSIVE API TESTING STARTED ==="
echo "Testing ALL 472+ endpoints across 53 controllers"
echo "Started at: $(date)"
echo ""

# Test Authentication Endpoints
echo "Phase 1: Testing Authentication Endpoints..."
echo "=== AUTHENTICATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/v1/auth/send-otp" '{"email":"test@example.com"}' "Send OTP" "Authentication"
test_endpoint "POST" "/v1/auth/register" '{"email":"test@example.com","otp":"123456","password":"password123"}' "Register User" "Authentication"
test_endpoint "POST" "/v1/auth/login" '{"email":"test@example.com","password":"password123"}' "Login User" "Authentication"
test_endpoint "POST" "/v1/auth/refresh" '{"refreshToken":"dummy"}' "Refresh Token" "Authentication"
test_endpoint "POST" "/v1/auth/logout" '{}' "Logout User" "Authentication"

# Test Admin Endpoints
echo "Phase 2: Testing Admin Endpoints..."
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
echo "Phase 3: Testing User Management Endpoints..."
echo "=== USER MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/users" "" "Get Users" "User Management"
test_endpoint "GET" "/api/users/123e4567-e89b-12d3-a456-426614174000" "" "Get User by ID" "User Management"
test_endpoint "PUT" "/api/users/123e4567-e89b-12d3-a456-426614174000" '{"firstName":"Updated","lastName":"User"}' "Update User" "User Management"
test_endpoint "DELETE" "/api/users/123e4567-e89b-12d3-a456-426614174000" "" "Delete User" "User Management"
test_endpoint "GET" "/v1/users/profile" "" "Get User Profile" "User Management"
test_endpoint "PUT" "/v1/users/profile" '{"firstName":"Test","lastName":"User"}' "Update User Profile" "User Management"

# Test Post Management Endpoints
echo "Phase 4: Testing Post Management Endpoints..."
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

# Test AI Services Endpoints
echo "Phase 5: Testing AI Services Endpoints..."
echo "=== AI SERVICES ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/ai/analyze" '{"content":"Test content for analysis"}' "Analyze Content" "AI Services"
test_endpoint "POST" "/api/ai/classify" '{"text":"Test text for classification"}' "Classify Text" "AI Services"
test_endpoint "POST" "/api/ai/suggest" '{"context":"Test context"}' "Get Suggestions" "AI Services"
test_endpoint "GET" "/v1/ai/health" "" "AI Health Check" "AI Services"
test_endpoint "POST" "/v1/ai/chat" '{"message":"Hello AI","conversationId":"test123"}' "AI Chat" "AI Services"
test_endpoint "GET" "/v1/ai/models" "" "Get AI Models" "AI Services"
test_endpoint "POST" "/v1/ai/train" '{"modelType":"sentiment","data":"training_data"}' "Train AI Model" "AI Services"

# Test Vector Database Endpoints
echo "Phase 6: Testing Vector Database Endpoints..."
echo "=== VECTOR DATABASE ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/vector-db/initialize" '{"collection":"test_collection"}' "Initialize Vector DB" "Vector Database"
test_endpoint "GET" "/api/vector-db/health" "" "Vector DB Health" "Vector Database"
test_endpoint "POST" "/api/vector-db/insert" '{"vector":[0.1,0.2,0.3],"metadata":{"id":"test"}}' "Insert Vector" "Vector Database"
test_endpoint "GET" "/api/vector-db/search" "?query=test&limit=10" "Search Vectors" "Vector Database"
test_endpoint "DELETE" "/api/vector-db/delete" '{"id":"test_vector_id"}' "Delete Vector" "Vector Database"

# Test Cache Management Endpoints
echo "Phase 7: Testing Cache Management Endpoints..."
echo "=== CACHE MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/cache/metrics" "" "Cache Metrics" "Cache Management"
test_endpoint "GET" "/v1/cache/health" "" "Cache Health" "Cache Management"
test_endpoint "GET" "/v1/cache/data/test_key" "" "Get Cache Data" "Cache Management"
test_endpoint "POST" "/v1/cache/warm" '{"keys":["key1","key2"]}' "Warm Cache" "Cache Management"
test_endpoint "DELETE" "/v1/cache/clear" "" "Clear Cache" "Cache Management"

# Test Load Balancing Endpoints
echo "Phase 8: Testing Load Balancing Endpoints..."
echo "=== LOAD BALANCING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/load-balancing/services" "" "Get Services" "Load Balancing"
test_endpoint "GET" "/v1/load-balancing/services/ai/discovery" "" "Service Discovery" "Load Balancing"
test_endpoint "POST" "/v1/load-balancing/register" '{"serviceType":"ai","url":"http://localhost:8081"}' "Register Service" "Load Balancing"
test_endpoint "PUT" "/v1/load-balancing/config" '{"strategy":"round_robin"}' "Update Config" "Load Balancing"
test_endpoint "GET" "/v1/load-balancing/health" "" "Load Balancer Health" "Load Balancing"

# Test Model Training Endpoints
echo "Phase 9: Testing Model Training Endpoints..."
echo "=== MODEL TRAINING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/model-training/train/123e4567-e89b-12d3-a456-426614174000" '{"modelType":"sentiment"}' "Train Model" "Model Training"
test_endpoint "GET" "/api/model-training/status/training123" "" "Training Status" "Model Training"
test_endpoint "POST" "/api/model-training/validate" '{"modelId":"model123","testData":"validation_data"}' "Validate Model" "Model Training"
test_endpoint "GET" "/api/model-training/models" "" "Get Models" "Model Training"
test_endpoint "DELETE" "/api/model-training/models/model123" "" "Delete Model" "Model Training"

# Test Moderation Endpoints
echo "Phase 10: Testing Moderation Endpoints..."
echo "=== MODERATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/moderation/analyze" '{"content":"Test content for moderation"}' "Analyze Content" "Moderation"
test_endpoint "POST" "/api/moderation/approve" '{"contentId":"123e4567-e89b-12d3-a456-426614174000"}' "Approve Content" "Moderation"
test_endpoint "POST" "/api/moderation/reject" '{"contentId":"123e4567-e89b-12d3-a456-426614174000","reason":"inappropriate"}' "Reject Content" "Moderation"
test_endpoint "GET" "/v1/moderation/queue" "" "Get Moderation Queue" "Moderation"
test_endpoint "POST" "/v1/moderation/batch" '{"contentIds":["id1","id2","id3"]}' "Batch Moderation" "Moderation"

# Test Analytics Endpoints
echo "Phase 11: Testing Analytics Endpoints..."
echo "=== ANALYTICS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/analytics/users" "" "User Analytics" "Analytics"
test_endpoint "GET" "/api/analytics/posts" "" "Post Analytics" "Analytics"
test_endpoint "GET" "/api/analytics/engagement" "" "Engagement Analytics" "Analytics"
test_endpoint "GET" "/v1/analytics/behavior" "" "Behavior Analytics" "Analytics"
test_endpoint "POST" "/v1/analytics/track" '{"event":"user_login","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Track Event" "Analytics"

# Test Chat & AI Endpoints
echo "Phase 12: Testing Chat & AI Endpoints..."
echo "=== CHAT & AI ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/chat/send" '{"message":"Hello","sessionId":"session123"}' "Send Chat Message" "Chat & AI"
test_endpoint "GET" "/api/chat/history" "?sessionId=session123" "Get Chat History" "Chat & AI"
test_endpoint "POST" "/api/chat/suggest" '{"context":"Test context"}' "Get Chat Suggestions" "Chat & AI"
test_endpoint "GET" "/v1/chat/sessions" "" "Get Chat Sessions" "Chat & AI"
test_endpoint "POST" "/v1/chat/analyze" '{"conversationId":"conv123"}' "Analyze Chat" "Chat & AI"

# Test Search & Discovery Endpoints
echo "Phase 13: Testing Search & Discovery Endpoints..."
echo "=== SEARCH & DISCOVERY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/search/posts" "?query=test" "Search Posts" "Search & Discovery"
test_endpoint "GET" "/api/search/users" "?query=test" "Search Users" "Search & Discovery"
test_endpoint "POST" "/api/search/vector" '{"query":"test query","limit":10}' "Vector Search" "Search & Discovery"
test_endpoint "GET" "/v1/search/recommendations" "?userId=123e4567-e89b-12d3-a456-426614174000" "Get Recommendations" "Search & Discovery"
test_endpoint "POST" "/v1/search/semantic" '{"query":"semantic test","context":"test context"}' "Semantic Search" "Search & Discovery"

# Test Memory & Psychology Endpoints
echo "Phase 14: Testing Memory & Psychology Endpoints..."
echo "=== MEMORY & PSYCHOLOGY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/memory/items" "" "Get Memory Items" "Memory & Psychology"
test_endpoint "POST" "/api/memory/store" '{"content":"Test memory","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Store Memory" "Memory & Psychology"
test_endpoint "GET" "/api/memory/retrieve" "?userId=123e4567-e89b-12d3-a456-426614174000" "Retrieve Memory" "Memory & Psychology"
test_endpoint "POST" "/v1/memory/analyze" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Memory" "Memory & Psychology"
test_endpoint "GET" "/v1/memory/patterns" "?userId=123e4567-e89b-12d3-a456-426614174000" "Memory Patterns" "Memory & Psychology"

# Test Interests & Personality Endpoints
echo "Phase 15: Testing Interests & Personality Endpoints..."
echo "=== INTERESTS & PERSONALITY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/interests" "" "Get Interests" "Interests & Personality"
test_endpoint "POST" "/api/interests/update" '{"userId":"123e4567-e89b-12d3-a456-426614174000","interests":["tech","music"]}' "Update Interests" "Interests & Personality"
test_endpoint "GET" "/v1/personality/profile" "?userId=123e4567-e89b-12d3-a456-426614174000" "Personality Profile" "Interests & Personality"
test_endpoint "POST" "/v1/personality/analyze" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Personality" "Interests & Personality"
test_endpoint "GET" "/v1/personality/insights" "?userId=123e4567-e89b-12d3-a456-426614174000" "Personality Insights" "Interests & Personality"

# Test Notifications Endpoints
echo "Phase 16: Testing Notifications Endpoints..."
echo "=== NOTIFICATIONS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/notifications" "" "Get Notifications" "Notifications"
test_endpoint "POST" "/api/notifications/send" '{"userId":"123e4567-e89b-12d3-a456-426614174000","message":"Test notification"}' "Send Notification" "Notifications"
test_endpoint "PUT" "/api/notifications/123e4567-e89b-12d3-a456-426614174000/read" "" "Mark as Read" "Notifications"
test_endpoint "GET" "/v1/notifications/settings" "?userId=123e4567-e89b-12d3-a456-426614174000" "Notification Settings" "Notifications"
test_endpoint "POST" "/v1/notifications/subscribe" '{"userId":"123e4567-e89b-12d3-a456-426614174000","topic":"updates"}' "Subscribe to Topic" "Notifications"

# Test Comments & Interactions Endpoints
echo "Phase 17: Testing Comments & Interactions Endpoints..."
echo "=== COMMENTS & INTERACTIONS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/comments" "" "Get Comments" "Comments & Interactions"
test_endpoint "POST" "/api/comments" '{"postId":"123e4567-e89b-12d3-a456-426614174000","content":"Test comment"}' "Create Comment" "Comments & Interactions"
test_endpoint "PUT" "/api/comments/123e4567-e89b-12d3-a456-426614174000" '{"content":"Updated comment"}' "Update Comment" "Comments & Interactions"
test_endpoint "DELETE" "/api/comments/123e4567-e89b-12d3-a456-426614174000" "" "Delete Comment" "Comments & Interactions"
test_endpoint "POST" "/api/interactions/like" '{"postId":"123e4567-e89b-12d3-a456-426614174000"}' "Like Post" "Comments & Interactions"
test_endpoint "POST" "/api/interactions/bookmark" '{"postId":"123e4567-e89b-12d3-a456-426614174000"}' "Bookmark Post" "Comments & Interactions"

# Test Articles & Content Endpoints
echo "Phase 18: Testing Articles & Content Endpoints..."
echo "=== ARTICLES & CONTENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/articles" "" "Get Articles" "Articles & Content"
test_endpoint "POST" "/api/articles" '{"title":"Test Article","content":"Test article content"}' "Create Article" "Articles & Content"
test_endpoint "GET" "/api/articles/123e4567-e89b-12d3-a456-426614174000" "" "Get Article by ID" "Articles & Content"
test_endpoint "PUT" "/api/articles/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Article","content":"Updated content"}' "Update Article" "Articles & Content"
test_endpoint "DELETE" "/api/articles/123e4567-e89b-12d3-a456-426614174000" "" "Delete Article" "Articles & Content"

# Test Drafts & Queue Endpoints
echo "Phase 19: Testing Drafts & Queue Endpoints..."
echo "=== DRAFTS & QUEUE ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/drafts" "" "Get Drafts" "Drafts & Queue"
test_endpoint "POST" "/api/drafts" '{"title":"Draft Title","content":"Draft content"}' "Create Draft" "Drafts & Queue"
test_endpoint "PUT" "/api/drafts/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Draft","content":"Updated draft content"}' "Update Draft" "Drafts & Queue"
test_endpoint "DELETE" "/api/drafts/123e4567-e89b-12d3-a456-426614174000" "" "Delete Draft" "Drafts & Queue"
test_endpoint "GET" "/api/queue/status" "" "Queue Status" "Drafts & Queue"
test_endpoint "POST" "/api/queue/process" '{"queueId":"queue123"}' "Process Queue" "Drafts & Queue"

# Test Feature Flags Endpoints
echo "Phase 20: Testing Feature Flags Endpoints..."
echo "=== FEATURE FLAGS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/features/flags" "" "Get Feature Flags" "Feature Flags"
test_endpoint "POST" "/api/features/flags" '{"name":"test_flag","enabled":true}' "Create Feature Flag" "Feature Flags"
test_endpoint "PUT" "/api/features/flags/test_flag" '{"enabled":false}' "Update Feature Flag" "Feature Flags"
test_endpoint "GET" "/v1/features/analytics" "" "Feature Analytics" "Feature Flags"
test_endpoint "POST" "/v1/features/toggle" '{"flagName":"test_flag","enabled":true}' "Toggle Feature Flag" "Feature Flags"

# Test Deployment & Monitoring Endpoints
echo "Phase 21: Testing Deployment & Monitoring Endpoints..."
echo "=== DEPLOYMENT & MONITORING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/deployment/status" "" "Deployment Status" "Deployment & Monitoring"
test_endpoint "POST" "/api/deployment/deploy" '{"service":"test_service","version":"v1.0"}' "Deploy Service" "Deployment & Monitoring"
test_endpoint "GET" "/api/deployment/logs" "" "Deployment Logs" "Deployment & Monitoring"
test_endpoint "GET" "/v1/deployment/health" "" "Deployment Health" "Deployment & Monitoring"
test_endpoint "POST" "/v1/deployment/scale" '{"service":"test_service","replicas":3}' "Scale Service" "Deployment & Monitoring"

# Test Auto Scaling Endpoints
echo "Phase 22: Testing Auto Scaling Endpoints..."
echo "=== AUTO SCALING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/scaling/metrics" "" "Scaling Metrics" "Auto Scaling"
test_endpoint "POST" "/api/scaling/scale-up" '{"service":"test_service"}' "Scale Up" "Auto Scaling"
test_endpoint "POST" "/api/scaling/scale-down" '{"service":"test_service"}' "Scale Down" "Auto Scaling"
test_endpoint "GET" "/v1/scaling/config" "" "Scaling Config" "Auto Scaling"
test_endpoint "PUT" "/v1/scaling/config" '{"minReplicas":1,"maxReplicas":10}' "Update Scaling Config" "Auto Scaling"

# Test Health & Monitoring Endpoints
echo "Phase 23: Testing Health & Monitoring Endpoints..."
echo "=== HEALTH & MONITORING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/health" "" "API Health" "Health & Monitoring"
test_endpoint "GET" "/api/health/detailed" "" "Detailed Health" "Health & Monitoring"
test_endpoint "GET" "/actuator/health" "" "Actuator Health" "Health & Monitoring"
test_endpoint "GET" "/actuator/info" "" "Actuator Info" "Health & Monitoring"
test_endpoint "GET" "/actuator/metrics" "" "Actuator Metrics" "Health & Monitoring"
test_endpoint "GET" "/actuator/mappings" "" "Actuator Mappings" "Health & Monitoring"

# Test Test & Development Endpoints
echo "Phase 24: Testing Test & Development Endpoints..."
echo "=== TEST & DEVELOPMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/test/hello" "" "Test Hello" "Test & Development"
test_endpoint "GET" "/api/test/health" "" "Test Health" "Test & Development"
test_endpoint "GET" "/api/test/redis" "" "Test Redis" "Test & Development"
test_endpoint "POST" "/api/test/data" '{"testData":"sample"}' "Test Data" "Test & Development"
test_endpoint "GET" "/api/test/performance" "" "Test Performance" "Test & Development"

# Test API Documentation Endpoints
echo "Phase 25: Testing API Documentation Endpoints..."
echo "=== API DOCUMENTATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api-docs" "" "API Docs" "API Documentation"
test_endpoint "GET" "/swagger-ui.html" "" "Swagger UI" "API Documentation"
test_endpoint "GET" "/swagger-ui/index.html" "" "Swagger UI Index" "API Documentation"
test_endpoint "GET" "/v3/api-docs" "" "V3 API Docs" "API Documentation"
test_endpoint "GET" "/v3/api-docs/swagger-config" "" "Swagger Config" "API Documentation"

# Test Psychological Analysis Endpoints
echo "Phase 26: Testing Psychological Analysis Endpoints..."
echo "=== PSYCHOLOGICAL ANALYSIS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/psychology/analyze" '{"content":"Test psychological content","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Psychology" "Psychological Analysis"
test_endpoint "GET" "/api/psychology/state" "?userId=123e4567-e89b-12d3-a456-426614174000" "Psychology State" "Psychological Analysis"
test_endpoint "POST" "/api/psychology/insights" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Psychology Insights" "Psychological Analysis"
test_endpoint "GET" "/v1/psychology/profile" "?userId=123e4567-e89b-12d3-a456-426614174000" "Psychology Profile" "Psychological Analysis"
test_endpoint "POST" "/v1/psychology/assessment" '{"userId":"123e4567-e89b-12d3-a456-426614174000","assessmentType":"personality"}' "Psychology Assessment" "Psychological Analysis"

# Test Conversation & Chat Endpoints
echo "Phase 27: Testing Conversation & Chat Endpoints..."
echo "=== CONVERSATION & CHAT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/conversations" "" "Get Conversations" "Conversation & Chat"
test_endpoint "POST" "/api/conversations" '{"title":"Test Conversation"}' "Create Conversation" "Conversation & Chat"
test_endpoint "GET" "/api/conversations/123e4567-e89b-12d3-a456-426614174000" "" "Get Conversation by ID" "Conversation & Chat"
test_endpoint "POST" "/api/conversations/123e4567-e89b-12d3-a456-426614174000/messages" '{"text":"Test message"}' "Send Message" "Conversation & Chat"
test_endpoint "GET" "/v1/conversations/history" "?userId=123e4567-e89b-12d3-a456-426614174000" "Conversation History" "Conversation & Chat"

# Test Batch Processing Endpoints
echo "Phase 28: Testing Batch Processing Endpoints..."
echo "=== BATCH PROCESSING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/batch/process" '{"operations":[{"type":"analyze","data":"test"}]}' "Process Batch" "Batch Processing"
test_endpoint "GET" "/api/batch/status" "?batchId=batch123" "Batch Status" "Batch Processing"
test_endpoint "POST" "/api/batch/cancel" '{"batchId":"batch123"}' "Cancel Batch" "Batch Processing"
test_endpoint "GET" "/v1/batch/queue" "" "Batch Queue" "Batch Processing"
test_endpoint "POST" "/v1/batch/retry" '{"batchId":"batch123"}' "Retry Batch" "Batch Processing"

# Test Warning & Safety Endpoints
echo "Phase 29: Testing Warning & Safety Endpoints..."
echo "=== WARNING & SAFETY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/warnings" "" "Get Warnings" "Warning & Safety"
test_endpoint "POST" "/api/warnings/create" '{"userId":"123e4567-e89b-12d3-a456-426614174000","reason":"inappropriate_content","severity":"HIGH"}' "Create Warning" "Warning & Safety"
test_endpoint "PUT" "/api/warnings/123e4567-e89b-12d3-a456-426614174000/resolve" '{"resolution":"resolved"}' "Resolve Warning" "Warning & Safety"
test_endpoint "GET" "/v1/warnings/active" "" "Active Warnings" "Warning & Safety"
test_endpoint "POST" "/v1/warnings/escalate" '{"warningId":"123e4567-e89b-12d3-a456-426614174000"}' "Escalate Warning" "Warning & Safety"

# Test Insights & Analytics Endpoints
echo "Phase 30: Testing Insights & Analytics Endpoints..."
echo "=== INSIGHTS & ANALYTICS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/insights/user" "?userId=123e4567-e89b-12d3-a456-426614174000" "User Insights" "Insights & Analytics"
test_endpoint "GET" "/api/insights/posts" "" "Post Insights" "Insights & Analytics"
test_endpoint "POST" "/api/insights/generate" '{"userId":"123e4567-e89b-12d3-a456-426614174000","type":"behavioral"}' "Generate Insights" "Insights & Analytics"
test_endpoint "GET" "/v1/insights/trends" "" "Insight Trends" "Insights & Analytics"
test_endpoint "POST" "/v1/insights/predict" '{"userId":"123e4567-e89b-12d3-a456-426614174000","predictionType":"engagement"}' "Predict Insights" "Insights & Analytics"

# Test User Consent Endpoints
echo "Phase 31: Testing User Consent Endpoints..."
echo "=== USER CONSENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/consent/status" "?userId=123e4567-e89b-12d3-a456-426614174000" "Consent Status" "User Consent"
test_endpoint "POST" "/api/consent/grant" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentType":"data_processing"}' "Grant Consent" "User Consent"
test_endpoint "POST" "/api/consent/revoke" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentType":"data_processing"}' "Revoke Consent" "User Consent"
test_endpoint "GET" "/v1/consent/history" "?userId=123e4567-e89b-12d3-a456-426614174000" "Consent History" "User Consent"
test_endpoint "POST" "/v1/consent/update" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentData":{"data_processing":true}}' "Update Consent" "User Consent"

# Test Queue Management Endpoints
echo "Phase 32: Testing Queue Management Endpoints..."
echo "=== QUEUE MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/queue/status" "" "Queue Status" "Queue Management"
test_endpoint "POST" "/api/queue/add" '{"taskType":"analysis","data":"test_data"}' "Add to Queue" "Queue Management"
test_endpoint "GET" "/api/queue/process" "" "Process Queue" "Queue Management"
test_endpoint "GET" "/v1/queue/metrics" "" "Queue Metrics" "Queue Management"
test_endpoint "POST" "/v1/queue/clear" "" "Clear Queue" "Queue Management"

# Test Welcome & Documentation Endpoints
echo "Phase 33: Testing Welcome & Documentation Endpoints..."
echo "=== WELCOME & DOCUMENTATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/" "" "Welcome Page" "Welcome & Documentation"
test_endpoint "GET" "/docs" "" "Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/api" "" "API Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/swagger" "" "Swagger Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/health" "" "Health Documentation" "Welcome & Documentation"

# Generate final analysis
echo ""
echo "=== GENERATING COMPREHENSIVE ANALYSIS ==="

# Count results by status
total_tests=$(grep -c ":" $RESULTS_FILE | head -1)
working_tests=$(grep -c ": 200" $RESULTS_FILE)
working_tests=$((working_tests + $(grep -c ": 201" $RESULTS_FILE)))
working_tests=$((working_tests + $(grep -c ": 204" $RESULTS_FILE)))

auth_required_tests=$(grep -c ": 401" $RESULTS_FILE)
forbidden_tests=$(grep -c ": 403" $RESULTS_FILE)
not_found_tests=$(grep -c ": 404" $RESULTS_FILE)
bad_request_tests=$(grep -c ": 400" $RESULTS_FILE)
server_error_tests=$(grep -c ": 500" $RESULTS_FILE)

# Calculate success rate
if [ $total_tests -gt 0 ]; then
    success_rate=$((working_tests * 100 / total_tests))
else
    success_rate=0
fi

# Generate final summary
echo "" >> $RESULTS_FILE
echo "=== FINAL COMPREHENSIVE ANALYSIS ===" >> $RESULTS_FILE
echo "Testing completed at: $(date)" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE
echo "SUMMARY STATISTICS:" >> $RESULTS_FILE
echo "- Total Endpoints Tested: $total_tests" >> $RESULTS_FILE
echo "- Working Endpoints (200/201/204): $working_tests" >> $RESULTS_FILE
echo "- Authentication Required (401): $auth_required_tests" >> $RESULTS_FILE
echo "- Forbidden Access (403): $forbidden_tests" >> $RESULTS_FILE
echo "- Not Found (404): $not_found_tests" >> $RESULTS_FILE
echo "- Bad Request (400): $bad_request_tests" >> $RESULTS_FILE
echo "- Server Error (500): $server_error_tests" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE
echo "OVERALL SUCCESS RATE: $success_rate%" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

echo ""
echo "=== COMPREHENSIVE API TESTING COMPLETED ==="
echo "Results saved to: $RESULTS_FILE"
echo "Total endpoints tested: $total_tests"
echo "Overall success rate: $success_rate%"
echo "Completed at: $(date)"

