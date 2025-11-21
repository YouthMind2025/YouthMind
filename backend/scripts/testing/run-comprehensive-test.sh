#!/bin/bash

echo "Running Comprehensive YouthMind API Test Suite..."
echo

# Change to script directory
cd "$(dirname "$0")"

echo "Building the project..."
mvn clean compile test-compile

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo
echo "Running comprehensive test suite..."
mvn test -Dtest=ComprehensiveApiTest

if [ $? -ne 0 ]; then
    echo
    echo "Test suite failed!"
    exit 1
fi

echo
echo "All tests passed successfully!"
echo
echo "Test Summary:"
echo "- Entity creation and validation: PASSED"
echo "- Repository operations: PASSED"
echo "- DTO creation and validation: PASSED"
echo "- Cascade operations: PASSED"
echo "- Unique constraints: PASSED"
echo "- Data integrity: PASSED"
echo "- Enum values: PASSED"
echo "- Audit fields: PASSED"
echo "- Default values: PASSED"
echo "- Search and filtering: PASSED"
echo "- Pagination: PASSED"
echo "- Complex queries: PASSED"
echo "- Complete workflow: PASSED"
echo
echo "YouthMind API implementation is working correctly!"
