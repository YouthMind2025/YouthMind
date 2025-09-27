@echo off
echo ========================================
echo YouthMind Backend - CORS Staging Test
echo ========================================
echo.

REM Set staging environment
set SPRING_PROFILES_ACTIVE=staging
set ALLOWED_ORIGINS=https://staging.youthmind.ai,https://staging-api.youthmind.ai,http://localhost:3000,http://localhost:3001

echo Testing CORS configuration for staging environment...
echo Allowed Origins: %ALLOWED_ORIGINS%
echo.

REM Test 1: Preflight request from allowed origin
echo [TEST 1] Testing preflight request from allowed origin...
curl -X OPTIONS ^
  -H "Origin: https://staging.youthmind.ai" ^
  -H "Access-Control-Request-Method: POST" ^
  -H "Access-Control-Request-Headers: Content-Type,Authorization" ^
  -v http://localhost:8080/v1/auth/login
echo.
echo.

REM Test 2: Preflight request from localhost (allowed)
echo [TEST 2] Testing preflight request from localhost...
curl -X OPTIONS ^
  -H "Origin: http://localhost:3000" ^
  -H "Access-Control-Request-Method: GET" ^
  -H "Access-Control-Request-Headers: Authorization" ^
  -v http://localhost:8080/v1/posts
echo.
echo.

REM Test 3: Preflight request from disallowed origin (should fail)
echo [TEST 3] Testing preflight request from disallowed origin (should fail)...
curl -X OPTIONS ^
  -H "Origin: https://malicious-site.com" ^
  -H "Access-Control-Request-Method: POST" ^
  -H "Access-Control-Request-Headers: Content-Type" ^
  -v http://localhost:8080/v1/auth/login
echo.
echo.

REM Test 4: Actual request with credentials
echo [TEST 4] Testing actual request with credentials...
curl -X GET ^
  -H "Origin: https://staging.youthmind.ai" ^
  -H "Authorization: Bearer test-token" ^
  -H "Content-Type: application/json" ^
  -v http://localhost:8080/v1/posts
echo.
echo.

REM Test 5: Check security headers
echo [TEST 5] Testing security headers...
curl -I -H "Origin: https://staging.youthmind.ai" http://localhost:8080/v1/posts
echo.
echo.

echo ========================================
echo CORS Testing Complete
echo ========================================
echo.
echo Expected Results:
echo - Tests 1-2: Should return CORS headers with allowed origins
echo - Test 3: Should return CORS error or no CORS headers
echo - Test 4: Should include proper CORS headers
echo - Test 5: Should show security headers (CSP, HSTS, etc.)
echo.
pause
