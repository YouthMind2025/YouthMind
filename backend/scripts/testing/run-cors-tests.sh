#!/bin/bash

echo "========================================"
echo "YouthMind Backend - CORS Tests Runner"
echo "========================================"
echo

echo "Running CORS configuration tests..."
echo

# Run CORS unit tests
echo "[1/5] Running CORS unit tests..."
mvn test -Dtest=CorsConfigTest -Dspring.profiles.active=test
if [ $? -ne 0 ]; then
    echo "ERROR: CORS unit tests failed!"
    exit 1
fi
echo

# Run CORS integration tests
echo "[2/5] Running CORS integration tests..."
mvn test -Dtest=CorsIntegrationTest -Dspring.profiles.active=test
if [ $? -ne 0 ]; then
    echo "ERROR: CORS integration tests failed!"
    exit 1
fi
echo

# Run security headers tests
echo "[3/5] Running security headers tests..."
mvn test -Dtest=SecurityHeadersTest -Dspring.profiles.active=test
if [ $? -ne 0 ]; then
    echo "ERROR: Security headers tests failed!"
    exit 1
fi
echo

# Run comprehensive CORS tests
echo "[4/5] Running comprehensive CORS tests..."
mvn test -Dtest=CorsComprehensiveTest -Dspring.profiles.active=test
if [ $? -ne 0 ]; then
    echo "ERROR: Comprehensive CORS tests failed!"
    exit 1
fi
echo

# Run SecurityConfig CORS tests
echo "[5/5] Running SecurityConfig CORS tests..."
mvn test -Dtest=SecurityConfigCorsTest -Dspring.profiles.active=test
if [ $? -ne 0 ]; then
    echo "ERROR: SecurityConfig CORS tests failed!"
    exit 1
fi
echo

echo "========================================"
echo "All CORS tests completed successfully!"
echo "========================================"
echo
echo "Test Summary:"
echo "- CORS unit tests: PASSED"
echo "- CORS integration tests: PASSED"
echo "- Security headers tests: PASSED"
echo "- Comprehensive CORS tests: PASSED"
echo "- SecurityConfig CORS tests: PASSED"
echo
echo "CORS configuration is working correctly across all environments."
echo
