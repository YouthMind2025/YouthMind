@echo off
echo Testing YouthMind Backend Compilation...
echo ========================================

echo.
echo Step 1: Cleaning previous build...
call mvn clean

echo.
echo Step 2: Compiling the project...
call mvn compile

if %errorlevel% equ 0 (
    echo.
    echo ✅ COMPILATION SUCCESSFUL!
    echo The missing DTO imports have been fixed.
    echo.
    echo Step 3: Running tests...
    call mvn test -Dtest=AdminSecretConfigurationTest
    if %errorlevel% equ 0 (
        echo.
        echo ✅ ALL TESTS PASSED!
        echo The admin secret environment variable implementation is working correctly.
    ) else (
        echo.
        echo ⚠️  Some tests failed, but compilation is working.
    )
) else (
    echo.
    echo ❌ COMPILATION FAILED!
    echo Please check the error messages above.
)

echo.
echo ========================================
echo Test completed.
pause
