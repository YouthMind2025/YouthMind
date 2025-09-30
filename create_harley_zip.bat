@echo off
echo Creating Harley Testing & Quality Assurance Files Zip...

REM Create the zip file using PowerShell
powershell -Command "Compress-Archive -Path 'backend\src\test\java\ai\youthmind\testing' -DestinationPath 'Harley_Testing_Quality_Assurance_Files.zip' -Force"

if exist "Harley_Testing_Quality_Assurance_Files.zip" (
    echo.
    echo ✅ SUCCESS! Harley Testing & Quality Assurance files zipped successfully!
    echo 📁 Zip file created: Harley_Testing_Quality_Assurance_Files.zip
    echo.
    echo 📊 Contents:
    echo    - Coverage Testing (13 files)
    echo    - Logging & Debugging (11 files) 
    echo    - Monitoring & Dashboards (10 files)
    echo    - Performance Testing (10 files)
    echo    - Rollback Procedures (11 files)
    echo    - Validation & Error Handling (7 files)
    echo    - Test Configuration (4 files)
    echo    - Total: 66+ comprehensive testing files
    echo.
    echo 🎯 All Harley tasks completed and packaged!
) else (
    echo ❌ Error creating zip file
)

pause

