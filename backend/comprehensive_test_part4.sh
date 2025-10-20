#!/bin/bash

# Comprehensive API Testing Script - Part 4 (Final)
# Tests remaining endpoints including Health, Test, Documentation, and specialized services

# Continue from Part 3
RESULTS_FILE="comprehensive_api_test_results.txt"

# Test Health & Monitoring Endpoints
echo "=== HEALTH & MONITORING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/health" "" "API Health" "Health & Monitoring"
test_endpoint "GET" "/api/health/detailed" "" "Detailed Health" "Health & Monitoring"
test_endpoint "GET" "/actuator/health" "" "Actuator Health" "Health & Monitoring"
test_endpoint "GET" "/actuator/info" "" "Actuator Info" "Health & Monitoring"
test_endpoint "GET" "/actuator/metrics" "" "Actuator Metrics" "Health & Monitoring"
test_endpoint "GET" "/actuator/mappings" "" "Actuator Mappings" "Health & Monitoring"

# Test Test & Development Endpoints
echo "=== TEST & DEVELOPMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/test/hello" "" "Test Hello" "Test & Development"
test_endpoint "GET" "/api/test/health" "" "Test Health" "Test & Development"
test_endpoint "GET" "/api/test/redis" "" "Test Redis" "Test & Development"
test_endpoint "POST" "/api/test/data" '{"testData":"sample"}' "Test Data" "Test & Development"
test_endpoint "GET" "/api/test/performance" "" "Test Performance" "Test & Development"

# Test API Documentation Endpoints
echo "=== API DOCUMENTATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api-docs" "" "API Docs" "API Documentation"
test_endpoint "GET" "/swagger-ui.html" "" "Swagger UI" "API Documentation"
test_endpoint "GET" "/swagger-ui/index.html" "" "Swagger UI Index" "API Documentation"
test_endpoint "GET" "/v3/api-docs" "" "V3 API Docs" "API Documentation"
test_endpoint "GET" "/v3/api-docs/swagger-config" "" "Swagger Config" "API Documentation"

# Test Psychological Analysis Endpoints
echo "=== PSYCHOLOGICAL ANALYSIS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/psychology/analyze" '{"content":"Test psychological content","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Psychology" "Psychological Analysis"
test_endpoint "GET" "/api/psychology/state" "?userId=123e4567-e89b-12d3-a456-426614174000" "Psychology State" "Psychological Analysis"
test_endpoint "POST" "/api/psychology/insights" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Psychology Insights" "Psychological Analysis"
test_endpoint "GET" "/v1/psychology/profile" "?userId=123e4567-e89b-12d3-a456-426614174000" "Psychology Profile" "Psychological Analysis"
test_endpoint "POST" "/v1/psychology/assessment" '{"userId":"123e4567-e89b-12d3-a456-426614174000","assessmentType":"personality"}' "Psychology Assessment" "Psychological Analysis"

# Test Conversation & Chat Endpoints
echo "=== CONVERSATION & CHAT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/conversations" "" "Get Conversations" "Conversation & Chat"
test_endpoint "POST" "/api/conversations" '{"title":"Test Conversation"}' "Create Conversation" "Conversation & Chat"
test_endpoint "GET" "/api/conversations/123e4567-e89b-12d3-a456-426614174000" "" "Get Conversation by ID" "Conversation & Chat"
test_endpoint "POST" "/api/conversations/123e4567-e89b-12d3-a456-426614174000/messages" '{"text":"Test message"}' "Send Message" "Conversation & Chat"
test_endpoint "GET" "/v1/conversations/history" "?userId=123e4567-e89b-12d3-a456-426614174000" "Conversation History" "Conversation & Chat"

# Test Batch Processing Endpoints
echo "=== BATCH PROCESSING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/batch/process" '{"operations":[{"type":"analyze","data":"test"}]}' "Process Batch" "Batch Processing"
test_endpoint "GET" "/api/batch/status" "?batchId=batch123" "Batch Status" "Batch Processing"
test_endpoint "POST" "/api/batch/cancel" '{"batchId":"batch123"}' "Cancel Batch" "Batch Processing"
test_endpoint "GET" "/v1/batch/queue" "" "Batch Queue" "Batch Processing"
test_endpoint "POST" "/v1/batch/retry" '{"batchId":"batch123"}' "Retry Batch" "Batch Processing"

# Test Warning & Safety Endpoints
echo "=== WARNING & SAFETY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/warnings" "" "Get Warnings" "Warning & Safety"
test_endpoint "POST" "/api/warnings/create" '{"userId":"123e4567-e89b-12d3-a456-426614174000","reason":"inappropriate_content","severity":"HIGH"}' "Create Warning" "Warning & Safety"
test_endpoint "PUT" "/api/warnings/123e4567-e89b-12d3-a456-426614174000/resolve" '{"resolution":"resolved"}' "Resolve Warning" "Warning & Safety"
test_endpoint "GET" "/v1/warnings/active" "" "Active Warnings" "Warning & Safety"
test_endpoint "POST" "/v1/warnings/escalate" '{"warningId":"123e4567-e89b-12d3-a456-426614174000"}' "Escalate Warning" "Warning & Safety"

# Test Insights & Analytics Endpoints
echo "=== INSIGHTS & ANALYTICS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/insights/user" "?userId=123e4567-e89b-12d3-a456-426614174000" "User Insights" "Insights & Analytics"
test_endpoint "GET" "/api/insights/posts" "" "Post Insights" "Insights & Analytics"
test_endpoint "POST" "/api/insights/generate" '{"userId":"123e4567-e89b-12d3-a456-426614174000","type":"behavioral"}' "Generate Insights" "Insights & Analytics"
test_endpoint "GET" "/v1/insights/trends" "" "Insight Trends" "Insights & Analytics"
test_endpoint "POST" "/v1/insights/predict" '{"userId":"123e4567-e89b-12d3-a456-426614174000","predictionType":"engagement"}' "Predict Insights" "Insights & Analytics"

# Test User Consent Endpoints
echo "=== USER CONSENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/consent/status" "?userId=123e4567-e89b-12d3-a456-426614174000" "Consent Status" "User Consent"
test_endpoint "POST" "/api/consent/grant" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentType":"data_processing"}' "Grant Consent" "User Consent"
test_endpoint "POST" "/api/consent/revoke" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentType":"data_processing"}' "Revoke Consent" "User Consent"
test_endpoint "GET" "/v1/consent/history" "?userId=123e4567-e89b-12d3-a456-426614174000" "Consent History" "User Consent"
test_endpoint "POST" "/v1/consent/update" '{"userId":"123e4567-e89b-12d3-a456-426614174000","consentData":{"data_processing":true}}' "Update Consent" "User Consent"

# Test Queue Management Endpoints
echo "=== QUEUE MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/queue/status" "" "Queue Status" "Queue Management"
test_endpoint "POST" "/api/queue/add" '{"taskType":"analysis","data":"test_data"}' "Add to Queue" "Queue Management"
test_endpoint "GET" "/api/queue/process" "" "Process Queue" "Queue Management"
test_endpoint "GET" "/v1/queue/metrics" "" "Queue Metrics" "Queue Management"
test_endpoint "POST" "/v1/queue/clear" "" "Clear Queue" "Queue Management"

# Test Welcome & Documentation Endpoints
echo "=== WELCOME & DOCUMENTATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/" "" "Welcome Page" "Welcome & Documentation"
test_endpoint "GET" "/docs" "" "Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/api" "" "API Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/swagger" "" "Swagger Documentation" "Welcome & Documentation"
test_endpoint "GET" "/docs/health" "" "Health Documentation" "Welcome & Documentation"

# Generate Summary Report
echo "" >> $RESULTS_FILE
echo "=== COMPREHENSIVE TESTING SUMMARY ===" >> $RESULTS_FILE
echo "Testing completed at: $(date)" >> $RESULTS_FILE
echo "Total endpoints tested: 200+" >> $RESULTS_FILE
echo "Categories tested: 25+" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

echo "Phase 4 Complete - Testing all remaining endpoints..."
echo "Comprehensive API testing completed!"
echo "Results saved to: $RESULTS_FILE"

