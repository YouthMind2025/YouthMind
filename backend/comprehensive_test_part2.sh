#!/bin/bash

# Comprehensive API Testing Script - Part 2
# Tests AI Services, Vector Database, Cache Management, and more

# Continue from Part 1
RESULTS_FILE="comprehensive_api_test_results.txt"

# Test AI Services Endpoints
echo "=== AI SERVICES ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/ai/analyze" '{"content":"Test content for analysis"}' "Analyze Content" "AI Services"
test_endpoint "POST" "/api/ai/classify" '{"text":"Test text for classification"}' "Classify Text" "AI Services"
test_endpoint "POST" "/api/ai/suggest" '{"context":"Test context"}' "Get Suggestions" "AI Services"
test_endpoint "GET" "/v1/ai/health" "" "AI Health Check" "AI Services"
test_endpoint "POST" "/v1/ai/chat" '{"message":"Hello AI","conversationId":"test123"}' "AI Chat" "AI Services"
test_endpoint "GET" "/v1/ai/models" "" "Get AI Models" "AI Services"
test_endpoint "POST" "/v1/ai/train" '{"modelType":"sentiment","data":"training_data"}' "Train AI Model" "AI Services"

# Test Vector Database Endpoints
echo "=== VECTOR DATABASE ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/vector-db/initialize" '{"collection":"test_collection"}' "Initialize Vector DB" "Vector Database"
test_endpoint "GET" "/api/vector-db/health" "" "Vector DB Health" "Vector Database"
test_endpoint "POST" "/api/vector-db/insert" '{"vector":[0.1,0.2,0.3],"metadata":{"id":"test"}}' "Insert Vector" "Vector Database"
test_endpoint "GET" "/api/vector-db/search" "?query=test&limit=10" "Search Vectors" "Vector Database"
test_endpoint "DELETE" "/api/vector-db/delete" '{"id":"test_vector_id"}' "Delete Vector" "Vector Database"

# Test Cache Management Endpoints
echo "=== CACHE MANAGEMENT ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/cache/metrics" "" "Cache Metrics" "Cache Management"
test_endpoint "GET" "/v1/cache/health" "" "Cache Health" "Cache Management"
test_endpoint "GET" "/v1/cache/data/test_key" "" "Get Cache Data" "Cache Management"
test_endpoint "POST" "/v1/cache/warm" '{"keys":["key1","key2"]}' "Warm Cache" "Cache Management"
test_endpoint "DELETE" "/v1/cache/clear" "" "Clear Cache" "Cache Management"

# Test Load Balancing Endpoints
echo "=== LOAD BALANCING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/v1/load-balancing/services" "" "Get Services" "Load Balancing"
test_endpoint "GET" "/v1/load-balancing/services/ai/discovery" "" "Service Discovery" "Load Balancing"
test_endpoint "POST" "/v1/load-balancing/register" '{"serviceType":"ai","url":"http://localhost:8081"}' "Register Service" "Load Balancing"
test_endpoint "PUT" "/v1/load-balancing/config" '{"strategy":"round_robin"}' "Update Config" "Load Balancing"
test_endpoint "GET" "/v1/load-balancing/health" "" "Load Balancer Health" "Load Balancing"

# Test Model Training Endpoints
echo "=== MODEL TRAINING ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/model-training/train/123e4567-e89b-12d3-a456-426614174000" '{"modelType":"sentiment"}' "Train Model" "Model Training"
test_endpoint "GET" "/api/model-training/status/training123" "" "Training Status" "Model Training"
test_endpoint "POST" "/api/model-training/validate" '{"modelId":"model123","testData":"validation_data"}' "Validate Model" "Model Training"
test_endpoint "GET" "/api/model-training/models" "" "Get Models" "Model Training"
test_endpoint "DELETE" "/api/model-training/models/model123" "" "Delete Model" "Model Training"

# Test Moderation Endpoints
echo "=== MODERATION ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/moderation/analyze" '{"content":"Test content for moderation"}' "Analyze Content" "Moderation"
test_endpoint "POST" "/api/moderation/approve" '{"contentId":"123e4567-e89b-12d3-a456-426614174000"}' "Approve Content" "Moderation"
test_endpoint "POST" "/api/moderation/reject" '{"contentId":"123e4567-e89b-12d3-a456-426614174000","reason":"inappropriate"}' "Reject Content" "Moderation"
test_endpoint "GET" "/v1/moderation/queue" "" "Get Moderation Queue" "Moderation"
test_endpoint "POST" "/v1/moderation/batch" '{"contentIds":["id1","id2","id3"]}' "Batch Moderation" "Moderation"

# Test Analytics Endpoints
echo "=== ANALYTICS ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/analytics/users" "" "User Analytics" "Analytics"
test_endpoint "GET" "/api/analytics/posts" "" "Post Analytics" "Analytics"
test_endpoint "GET" "/api/analytics/engagement" "" "Engagement Analytics" "Analytics"
test_endpoint "GET" "/v1/analytics/behavior" "" "Behavior Analytics" "Analytics"
test_endpoint "POST" "/v1/analytics/track" '{"event":"user_login","userId":"123e4567-e89b-12d3-a456-426614174000"}' "Track Event" "Analytics"

# Test Chat & AI Endpoints
echo "=== CHAT & AI ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "POST" "/api/chat/send" '{"message":"Hello","sessionId":"session123"}' "Send Chat Message" "Chat & AI"
test_endpoint "GET" "/api/chat/history" "?sessionId=session123" "Get Chat History" "Chat & AI"
test_endpoint "POST" "/api/chat/suggest" '{"context":"Test context"}' "Get Chat Suggestions" "Chat & AI"
test_endpoint "GET" "/v1/chat/sessions" "" "Get Chat Sessions" "Chat & AI"
test_endpoint "POST" "/v1/chat/analyze" '{"conversationId":"conv123"}' "Analyze Chat" "Chat & AI"

# Test Search & Discovery Endpoints
echo "=== SEARCH & DISCOVERY ENDPOINTS ===" >> $RESULTS_FILE
test_endpoint "GET" "/api/search/posts" "?query=test" "Search Posts" "Search & Discovery"
test_endpoint "GET" "/api/search/users" "?query=test" "Search Users" "Search & Discovery"
test_endpoint "POST" "/api/search/vector" '{"query":"test query","limit":10}' "Vector Search" "Search & Discovery"
test_endpoint "GET" "/v1/search/recommendations" "?userId=123e4567-e89b-12d3-a456-426614174000" "Get Recommendations" "Search & Discovery"
test_endpoint "POST" "/v1/search/semantic" '{"query":"semantic test","context":"test context"}' "Semantic Search" "Search & Discovery"

echo "Phase 2 Complete - Testing AI, Vector DB, Cache, and more endpoints..."

