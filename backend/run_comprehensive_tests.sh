#!/bin/bash

# Master Comprehensive API Testing Script
# Executes all 4 parts and generates final comprehensive report

echo "=== YouthMind Backend Comprehensive API Testing ==="
echo "Testing ALL 472+ endpoints across 53 controllers"
echo "Started at: $(date)"
echo ""

# Make all scripts executable
chmod +x comprehensive_test_part1.sh
chmod +x comprehensive_test_part2.sh
chmod +x comprehensive_test_part3.sh
chmod +x comprehensive_test_part4.sh

# Initialize results file
RESULTS_FILE="comprehensive_api_test_results.txt"
echo "=== YouthMind Backend Comprehensive API Testing Report ===" > $RESULTS_FILE
echo "Generated: $(date '+%Y-%m-%d %H:%M:%S')" >> $RESULTS_FILE
echo "Total Controllers: 53" >> $RESULTS_FILE
echo "Estimated Endpoints: 472+" >> $RESULTS_FILE
echo "" >> $RESULTS_FILE

# Execute all test phases
echo "Phase 1: Testing Core Endpoints (Authentication, Admin, User, Post Management)..."
./comprehensive_test_part1.sh

echo "Phase 2: Testing AI Services, Vector DB, Cache, Load Balancing..."
./comprehensive_test_part2.sh

echo "Phase 3: Testing Memory, Psychology, Notifications, Comments..."
./comprehensive_test_part3.sh

echo "Phase 4: Testing Health, Documentation, Specialized Services..."
./comprehensive_test_part4.sh

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

# Generate category breakdown
echo "CATEGORY BREAKDOWN:" >> $RESULTS_FILE
echo "- Authentication Endpoints: $(grep -c "Authentication" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Admin Endpoints: $(grep -c "Admin" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- User Management Endpoints: $(grep -c "User Management" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Post Management Endpoints: $(grep -c "Post Management" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- AI Services Endpoints: $(grep -c "AI Services" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Vector Database Endpoints: $(grep -c "Vector Database" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Cache Management Endpoints: $(grep -c "Cache Management" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Load Balancing Endpoints: $(grep -c "Load Balancing" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Model Training Endpoints: $(grep -c "Model Training" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Moderation Endpoints: $(grep -c "Moderation" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Analytics Endpoints: $(grep -c "Analytics" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Chat & AI Endpoints: $(grep -c "Chat & AI" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Search & Discovery Endpoints: $(grep -c "Search & Discovery" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Memory & Psychology Endpoints: $(grep -c "Memory & Psychology" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Interests & Personality Endpoints: $(grep -c "Interests & Personality" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Notifications Endpoints: $(grep -c "Notifications" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Comments & Interactions Endpoints: $(grep -c "Comments & Interactions" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Articles & Content Endpoints: $(grep -c "Articles & Content" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Drafts & Queue Endpoints: $(grep -c "Drafts & Queue" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Feature Flags Endpoints: $(grep -c "Feature Flags" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Deployment & Monitoring Endpoints: $(grep -c "Deployment & Monitoring" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Auto Scaling Endpoints: $(grep -c "Auto Scaling" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Health & Monitoring Endpoints: $(grep -c "Health & Monitoring" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Test & Development Endpoints: $(grep -c "Test & Development" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- API Documentation Endpoints: $(grep -c "API Documentation" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Psychological Analysis Endpoints: $(grep -c "Psychological Analysis" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Conversation & Chat Endpoints: $(grep -c "Conversation & Chat" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Batch Processing Endpoints: $(grep -c "Batch Processing" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Warning & Safety Endpoints: $(grep -c "Warning & Safety" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Insights & Analytics Endpoints: $(grep -c "Insights & Analytics" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- User Consent Endpoints: $(grep -c "User Consent" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Queue Management Endpoints: $(grep -c "Queue Management" $RESULTS_FILE)" >> $RESULTS_FILE
echo "- Welcome & Documentation Endpoints: $(grep -c "Welcome & Documentation" $RESULTS_FILE)" >> $RESULTS_FILE

echo ""
echo "=== COMPREHENSIVE API TESTING COMPLETED ==="
echo "Results saved to: $RESULTS_FILE"
echo "Total endpoints tested: $total_tests"
echo "Overall success rate: $success_rate%"
echo "Completed at: $(date)"

