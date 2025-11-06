#!/bin/bash

# Comprehensive API Testing Script - Part 3
# Tests Memory, Psychology, Interests, Notifications, and more

# Continue from Part 2
RESULTS_FILE="comprehensive_api_test_results.txt"

# Test Memory & Psychology Endpoints
echo "=== MEMORY & PSYCHOLOGY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/memory/items" "" "Get Memory Items" "Memory & Psychology"
test_endpoint "POST" "/api/memory/store" '{"content":"Test memory","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Store Memory" "Memory & Psychology"
test_endpoint "GET" "/api/memory/retrieve" "?userId=123e4567-e89b-12d3-a456-426614174000" "Retrieve Memory" "Memory & Psychology"
test_endpoint "POST" "/v1/memory/analyze" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Memory" "Memory & Psychology"
test_endpoint "GET" "/v1/memory/patterns" "?userId=123e4567-e89b-12d3-a456-426614174000" "Memory Patterns" "Memory & Psychology"

# Test Interests & Personality Endpoints
echo "=== INTERESTS & PERSONALITY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/interests" "" "Get Interests" "Interests & Personality"
test_endpoint "POST" "/api/interests/update" '{"userId":"123e4567-e89b-12d3-a456-426614174000","interests":["tech","music"]}' "Update Interests" "Interests & Personality"
test_endpoint "GET" "/v1/personality/profile" "?userId=123e4567-e89b-12d3-a456-426614174000" "Personality Profile" "Interests & Personality"
test_endpoint "POST" "/v1/personality/analyze" '{"userId":"123e4567-e89b-12d3-a456-426614174000"}' "Analyze Personality" "Interests & Personality"
test_endpoint "GET" "/v1/personality/insights" "?userId=123e4567-e89b-12d3-a456-426614174000" "Personality Insights" "Interests & Personality"

# Test Notifications Endpoints
echo "=== NOTIFICATIONS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/notifications" "" "Get Notifications" "Notifications"
test_endpoint "POST" "/api/notifications/send" '{"userId":"123e4567-e89b-12d3-a456-426614174000","message":"Test notification"}' "Send Notification" "Notifications"
test_endpoint "PUT" "/api/notifications/123e4567-e89b-12d3-a456-426614174000/read" "" "Mark as Read" "Notifications"
test_endpoint "GET" "/v1/notifications/settings" "?userId=123e4567-e89b-12d3-a456-426614174000" "Notification Settings" "Notifications"
test_endpoint "POST" "/v1/notifications/subscribe" '{"userId":"123e4567-e89b-12d3-a456-426614174000","topic":"updates"}' "Subscribe to Topic" "Notifications"

# Test Comments & Interactions Endpoints
echo "=== COMMENTS & INTERACTIONS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/comments" "" "Get Comments" "Comments & Interactions"
test_endpoint "POST" "/api/comments" '{"postId":"123e4567-e89b-12d3-a456-426614174000","content":"Test comment"}' "Create Comment" "Comments & Interactions"
test_endpoint "PUT" "/api/comments/123e4567-e89b-12d3-a456-426614174000" '{"content":"Updated comment"}' "Update Comment" "Comments & Interactions"
test_endpoint "DELETE" "/api/comments/123e4567-e89b-12d3-a456-426614174000" "" "Delete Comment" "Comments & Interactions"
test_endpoint "POST" "/api/interactions/like" '{"postId":"123e4567-e89b-12d3-a456-426614174000"}' "Like Post" "Comments & Interactions"
test_endpoint "POST" "/api/interactions/bookmark" '{"postId":"123e4567-e89b-12d3-a456-426614174000"}' "Bookmark Post" "Comments & Interactions"

# Test Articles & Content Endpoints
echo "=== ARTICLES & CONTENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/articles" "" "Get Articles" "Articles & Content"
test_endpoint "POST" "/api/articles" '{"title":"Test Article","content":"Test article content"}' "Create Article" "Articles & Content"
test_endpoint "GET" "/api/articles/123e4567-e89b-12d3-a456-426614174000" "" "Get Article by ID" "Articles & Content"
test_endpoint "PUT" "/api/articles/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Article","content":"Updated content"}' "Update Article" "Articles & Content"
test_endpoint "DELETE" "/api/articles/123e4567-e89b-12d3-a456-426614174000" "" "Delete Article" "Articles & Content"

# Test Drafts & Queue Endpoints
echo "=== DRAFTS & QUEUE ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/drafts" "" "Get Drafts" "Drafts & Queue"
test_endpoint "POST" "/api/drafts" '{"title":"Draft Title","content":"Draft content"}' "Create Draft" "Drafts & Queue"
test_endpoint "PUT" "/api/drafts/123e4567-e89b-12d3-a456-426614174000" '{"title":"Updated Draft","content":"Updated draft content"}' "Update Draft" "Drafts & Queue"
test_endpoint "DELETE" "/api/drafts/123e4567-e89b-12d3-a456-426614174000" "" "Delete Draft" "Drafts & Queue"
test_endpoint "GET" "/api/queue/status" "" "Queue Status" "Drafts & Queue"
test_endpoint "POST" "/api/queue/process" '{"queueId":"queue123"}' "Process Queue" "Drafts & Queue"

# Test Feature Flags Endpoints
echo "=== FEATURE FLAGS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/features/flags" "" "Get Feature Flags" "Feature Flags"
test_endpoint "POST" "/api/features/flags" '{"name":"test_flag","enabled":true}' "Create Feature Flag" "Feature Flags"
test_endpoint "PUT" "/api/features/flags/test_flag" '{"enabled":false}' "Update Feature Flag" "Feature Flags"
test_endpoint "GET" "/v1/features/analytics" "" "Feature Analytics" "Feature Flags"
test_endpoint "POST" "/v1/features/toggle" '{"flagName":"test_flag","enabled":true}' "Toggle Feature Flag" "Feature Flags"

# Test Deployment & Monitoring Endpoints
echo "=== DEPLOYMENT & MONITORING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/deployment/status" "" "Deployment Status" "Deployment & Monitoring"
test_endpoint "POST" "/api/deployment/deploy" '{"service":"test_service","version":"v1.0"}' "Deploy Service" "Deployment & Monitoring"
test_endpoint "GET" "/api/deployment/logs" "" "Deployment Logs" "Deployment & Monitoring"
test_endpoint "GET" "/v1/deployment/health" "" "Deployment Health" "Deployment & Monitoring"
test_endpoint "POST" "/v1/deployment/scale" '{"service":"test_service","replicas":3}' "Scale Service" "Deployment & Monitoring"

# Test Auto Scaling Endpoints
echo "=== AUTO SCALING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/scaling/metrics" "" "Scaling Metrics" "Auto Scaling"
test_endpoint "POST" "/api/scaling/scale-up" '{"service":"test_service"}' "Scale Up" "Auto Scaling"
test_endpoint "POST" "/api/scaling/scale-down" '{"service":"test_service"}' "Scale Down" "Auto Scaling"
test_endpoint "GET" "/v1/scaling/config" "" "Scaling Config" "Auto Scaling"
test_endpoint "PUT" "/v1/scaling/config" '{"minReplicas":1,"maxReplicas":10}' "Update Scaling Config" "Auto Scaling"

echo "Phase 3 Complete - Testing Memory, Psychology, Notifications, and more endpoints..."

