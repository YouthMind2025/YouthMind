@echo off
echo ========================================
echo Testing YAML Configuration
echo ========================================
echo.

echo Testing YAML syntax...
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none" -q
if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ YAML CONFIGURATION IS VALID!
    echo The duplicate 'app:' key issue has been fixed.
    echo.
    echo You can now run the application with:
    echo   mvn spring-boot:run
    echo.
) else (
    echo.
    echo ❌ YAML CONFIGURATION STILL HAS ISSUES!
    echo Please check the error output above.
    echo.
)

pause
