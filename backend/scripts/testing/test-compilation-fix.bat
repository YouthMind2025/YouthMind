@echo off
echo ========================================
echo Testing SecurityConfig Compilation Fix
echo ========================================
echo.

echo Compiling the project to check for errors...
mvn compile -q

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ COMPILATION SUCCESSFUL!
    echo The SecurityConfig compilation errors have been fixed.
    echo.
    echo You can now run the application with:
    echo   mvn spring-boot:run
    echo.
) else (
    echo.
    echo ❌ COMPILATION FAILED!
    echo There are still compilation errors. Please check the output above.
    echo.
)

pause
