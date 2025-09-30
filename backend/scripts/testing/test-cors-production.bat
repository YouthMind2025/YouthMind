@echo off
echo ========================================
echo YouthMind Backend - CORS Production Test
echo ========================================
echo.

REM Set production environment
set SPRING_PROFILES_ACTIVE=prod
set ALLOWED_ORIGINS=https://youthmind.ai,https://www.youthmind.ai,https://api.youthmind.ai

echo Testing CORS configuration for production environment...
echo Allowed Origins: %ALLOWED_ORIGINS%
echo.

REM Test 1: Preflight request from production domain
echo [TEST 1] Testing preflight request from production domain...
curl -X OPTIONS ^
  -H "Origin: https://youthmind.ai" ^
  -H "Access-Control-Request-Method: POST" ^
  -H "Access-Control-Request-Headers: Content-Type,Authorization,X-API-Key" ^
  -v http://localhost:8080/api/v1/auth/login
echo.
echo.

REM Test 2: Preflight request from www subdomain
echo [TEST 2] Testing preflight request from www subdomain...
curl -X OPTIONS ^
  -H "Origin: https://www.youthmind.ai" ^
  -H "Access-Control-Request-Method: GET" ^
  -H "Access-Control-Request-Headers: Authorization,X-Client-Version" ^
  -v http://localhost:8080/api/v1/posts
echo.
echo.

REM Test 3: Preflight request from API subdomain
echo [TEST 3] Testing preflight request from API subdomain...
curl -X OPTIONS ^
  -H "Origin: https://api.youthmind.ai" ^
  -H "Access-Control-Request-Method: PUT" ^
  -H "Access-Control-Request-Headers: Content-Type,Authorization" ^
  -v http://localhost:8080/api/v1/posts/1
echo.
echo.

REM Test 4: Preflight request from disallowed origin (should fail)
echo [TEST 4] Testing preflight request from disallowed origin (should fail)...
curl -X OPTIONS ^
  -H "Origin: https://malicious-site.com" ^
  -H "Access-Control-Request-Method: POST" ^
  -H "Access-Control-Request-Headers: Content-Type" ^
  -v http://localhost:8080/api/v1/auth/login
echo.
echo.

REM Test 5: Preflight request from HTTP (should fail in production)
echo [TEST 5] Testing preflight request from HTTP (should fail in production)...
curl -X OPTIONS ^
  -H "Origin: http://youthmind.ai" ^
  -H "Access-Control-Request-Method: GET" ^
  -H "Access-Control-Request-Headers: Authorization" ^
  -v http://localhost:8080/api/v1/posts
echo.
echo.

REM Test 6: Check security headers and CSP
echo [TEST 6] Testing security headers and CSP...
curl -I -H "Origin: https://youthmind.ai" http://localhost:8080/api/v1/posts
echo.
echo.

REM Test 7: Test with production headers
echo [TEST 7] Testing with production-specific headers...
curl -X GET ^
  -H "Origin: https://youthmind.ai" ^
  -H "Authorization: Bearer test-token" ^
  -H "X-API-Key: test-api-key" ^
  -H "X-Client-Version: 1.0.0" ^
  -H "Content-Type: application/json" ^
  -v http://localhost:8080/api/v1/posts
echo.
echo.

echo ========================================
echo CORS Production Testing Complete
echo ========================================
echo.
echo Expected Results:
echo - Tests 1-3: Should return CORS headers with production domains
echo - Tests 4-5: Should return CORS error or no CORS headers
echo - Test 6: Should show strict security headers (CSP, HSTS, etc.)
echo - Test 7: Should include proper CORS headers with production headers
echo.
echo Security Headers to Verify:
echo - Content-Security-Policy: Should be present and strict
echo - Strict-Transport-Security: Should be present with max-age
echo - X-Frame-Options: Should be DENY
echo - X-Content-Type-Options: Should be nosniff
echo - X-XSS-Protection: Should be 1; mode=block
echo.
pause
