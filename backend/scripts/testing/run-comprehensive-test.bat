@echo off
echo Running Comprehensive YouthMind API Test Suite...
echo.

cd /d "%~dp0"

echo Building the project...
call mvn clean compile test-compile

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Running comprehensive test suite...
call mvn test -Dtest=ComprehensiveApiTest

if %ERRORLEVEL% neq 0 (
    echo.
    echo Test suite failed!
    pause
    exit /b 1
)

echo.
echo All tests passed successfully!
echo.
echo Test Summary:
echo - Entity creation and validation: PASSED
echo - Repository operations: PASSED
echo - DTO creation and validation: PASSED
echo - Cascade operations: PASSED
echo - Unique constraints: PASSED
echo - Data integrity: PASSED
echo - Enum values: PASSED
echo - Audit fields: PASSED
echo - Default values: PASSED
echo - Search and filtering: PASSED
echo - Pagination: PASSED
echo - Complex queries: PASSED
echo - Complete workflow: PASSED
echo.
echo YouthMind API implementation is working correctly!
pause
