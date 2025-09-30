@echo off
REM YouthMind Backend - Admin Registration Test Script (Windows)
REM Tests admin registration with environment variable configuration

setlocal enabledelayedexpansion

REM Configuration
set BASE_URL=http://localhost:8080
set ADMIN_SECRET_KEY=%ADMIN_SECRET_KEY%
if "%ADMIN_SECRET_KEY%"=="" set ADMIN_SECRET_KEY=YOUTHMIND_ADMIN_2024
set TEST_EMAIL=admin-test@youthmind.ai
set TEST_PASSWORD=TestPassword123!
set TEST_FIRST_NAME=Test
set TEST_LAST_NAME=Admin
set TEST_USERNAME=testadmin

echo 🧪 YouthMind Backend - Admin Registration Test
echo ==================================================
echo.

REM Function to print test results
:print_result
set test_name=%~1
set status=%~2
set message=%~3

if "%status%"=="PASS" (
    echo ✅ PASS: %test_name% - %message%
) else if "%status%"=="FAIL" (
    echo ❌ FAIL: %test_name% - %message%
) else (
    echo ⚠️  WARN: %test_name% - %message%
)
goto :eof

REM Function to check if server is running
:check_server
echo 🔍 Checking if server is running...

curl -s -f "%BASE_URL%/actuator/health" >nul 2>&1
if %errorlevel% equ 0 (
    call :print_result "Server Health Check" "PASS" "Server is running and healthy"
    set /a server_running=1
) else (
    call :print_result "Server Health Check" "FAIL" "Server is not running or not accessible at %BASE_URL%"
    echo Please start the server first:
    echo   mvn spring-boot:run
    echo   or
    echo   java -jar target/youthmind-backend-0.0.1-SNAPSHOT.jar
    set /a server_running=0
)
goto :eof

REM Function to test admin registration with correct secret
:test_admin_registration_success
echo 🔐 Testing admin registration with correct secret...

curl -s -w "\n%%{http_code}" -X POST "%BASE_URL%/v1/auth/admin/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"email\": \"%TEST_EMAIL%\", \"password\": \"%TEST_PASSWORD%\", \"firstName\": \"%TEST_FIRST_NAME%\", \"lastName\": \"%TEST_LAST_NAME%\", \"username\": \"%TEST_USERNAME%\", \"adminSecretKey\": \"%ADMIN_SECRET_KEY%\"}" > temp_response.txt

for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /r "^[0-9][0-9][0-9]$"') do set http_code=%%i
for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /v /r "^[0-9][0-9][0-9]$"') do set body=%%i

if "%http_code%"=="201" (
    call :print_result "Admin Registration (Success)" "PASS" "Admin registered successfully with correct secret"
    echo Response: !body!
    set /a admin_reg_success=1
) else (
    call :print_result "Admin Registration (Success)" "FAIL" "Expected 201, got %http_code%. Response: !body!"
    set /a admin_reg_success=0
)
del temp_response.txt
goto :eof

REM Function to test admin registration with incorrect secret
:test_admin_registration_failure
echo 🔒 Testing admin registration with incorrect secret...

curl -s -w "\n%%{http_code}" -X POST "%BASE_URL%/v1/auth/admin/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"email\": \"admin-fail@youthmind.ai\", \"password\": \"%TEST_PASSWORD%\", \"firstName\": \"%TEST_FIRST_NAME%\", \"lastName\": \"%TEST_LAST_NAME%\", \"username\": \"testadminfail\", \"adminSecretKey\": \"WRONG_SECRET_KEY\"}" > temp_response.txt

for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /r "^[0-9][0-9][0-9]$"') do set http_code=%%i
for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /v /r "^[0-9][0-9][0-9]$"') do set body=%%i

if "%http_code%"=="400" (
    call :print_result "Admin Registration (Failure)" "PASS" "Correctly rejected with wrong secret"
    echo Response: !body!
    set /a admin_reg_failure=1
) else (
    call :print_result "Admin Registration (Failure)" "FAIL" "Expected 400, got %http_code%. Response: !body!"
    set /a admin_reg_failure=0
)
del temp_response.txt
goto :eof

REM Function to test admin registration without secret
:test_admin_registration_no_secret
echo 🔓 Testing admin registration without secret...

curl -s -w "\n%%{http_code}" -X POST "%BASE_URL%/v1/auth/admin/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"email\": \"admin-nosecret@youthmind.ai\", \"password\": \"%TEST_PASSWORD%\", \"firstName\": \"%TEST_FIRST_NAME%\", \"lastName\": \"%TEST_LAST_NAME%\", \"username\": \"testadminnosecret\"}" > temp_response.txt

for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /r "^[0-9][0-9][0-9]$"') do set http_code=%%i
for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /v /r "^[0-9][0-9][0-9]$"') do set body=%%i

if "%http_code%"=="400" (
    call :print_result "Admin Registration (No Secret)" "PASS" "Correctly rejected without secret"
    echo Response: !body!
    set /a admin_reg_no_secret=1
) else (
    call :print_result "Admin Registration (No Secret)" "FAIL" "Expected 400, got %http_code%. Response: !body!"
    set /a admin_reg_no_secret=0
)
del temp_response.txt
goto :eof

REM Function to test admin login
:test_admin_login
echo 🔑 Testing admin login...

curl -s -w "\n%%{http_code}" -X POST "%BASE_URL%/v1/auth/admin/login" ^
    -H "Content-Type: application/json" ^
    -d "{\"email\": \"%TEST_EMAIL%\", \"password\": \"%TEST_PASSWORD%\"}" > temp_response.txt

for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /r "^[0-9][0-9][0-9]$"') do set http_code=%%i
for /f "tokens=*" %%i in ('type temp_response.txt ^| findstr /v /r "^[0-9][0-9][0-9]$"') do set body=%%i

if "%http_code%"=="200" (
    call :print_result "Admin Login" "PASS" "Admin login successful"
    echo Response: !body!
    set /a admin_login=1
) else (
    call :print_result "Admin Login" "FAIL" "Expected 200, got %http_code%. Response: !body!"
    set /a admin_login=0
)
del temp_response.txt
goto :eof

REM Function to test environment variable configuration
:test_environment_config
echo ⚙️  Testing environment variable configuration...

if not "%ADMIN_SECRET_KEY%"=="" (
    call :print_result "Environment Variable" "PASS" "ADMIN_SECRET_KEY is configured: %ADMIN_SECRET_KEY:~0,10%..."
    set /a env_config=1
) else (
    call :print_result "Environment Variable" "FAIL" "ADMIN_SECRET_KEY is not set"
    set /a env_config=0
    goto :eof
)

if "%ADMIN_SECRET_KEY%"=="YOUTHMIND_ADMIN_2024" (
    call :print_result "Default Secret Key" "WARN" "Using default admin secret key (acceptable for development)"
) else (
    call :print_result "Custom Secret Key" "PASS" "Using custom admin secret key"
)
goto :eof

REM Function to test health endpoint
:test_health_endpoint
echo 🏥 Testing health endpoint...

curl -s "%BASE_URL%/actuator/health" > temp_health.txt
findstr /c:"\"status\":\"UP\"" temp_health.txt >nul
if %errorlevel% equ 0 (
    call :print_result "Health Endpoint" "PASS" "Health endpoint shows UP status"
    type temp_health.txt
    set /a health_check=1
) else (
    call :print_result "Health Endpoint" "FAIL" "Health endpoint shows DOWN status"
    type temp_health.txt
    set /a health_check=0
)
del temp_health.txt
goto :eof

REM Main test execution
:main
echo Configuration:
echo   Base URL: %BASE_URL%
echo   Admin Secret Key: %ADMIN_SECRET_KEY:~0,10%...
echo   Test Email: %TEST_EMAIL%
echo.

REM Initialize counters
set /a pass_count=0
set /a fail_count=0

REM Run tests
call :test_environment_config
if !env_config! equ 1 (
    set /a pass_count+=1
) else (
    set /a fail_count+=1
)

call :check_server
if !server_running! equ 1 (
    set /a pass_count+=1
    
    call :test_health_endpoint
    if !health_check! equ 1 (
        set /a pass_count+=1
    ) else (
        set /a fail_count+=1
    )
    
    call :test_admin_registration_success
    if !admin_reg_success! equ 1 (
        set /a pass_count+=1
    ) else (
        set /a fail_count+=1
    )
    
    call :test_admin_registration_failure
    if !admin_reg_failure! equ 1 (
        set /a pass_count+=1
    ) else (
        set /a fail_count+=1
    )
    
    call :test_admin_registration_no_secret
    if !admin_reg_no_secret! equ 1 (
        set /a pass_count+=1
    ) else (
        set /a fail_count+=1
    )
    
    call :test_admin_login
    if !admin_login! equ 1 (
        set /a pass_count+=1
    ) else (
        set /a fail_count+=1
    )
) else (
    set /a fail_count+=1
)

REM Print summary
echo.
echo 📊 Test Summary
echo ===============
echo Results: !pass_count! passed, !fail_count! failed

if !fail_count! equ 0 (
    echo 🎉 All tests passed! Admin registration with environment variables is working correctly.
    exit /b 0
) else (
    echo ❌ Some tests failed. Please check the configuration and try again.
    exit /b 1
)

REM Run main function
call :main
