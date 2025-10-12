@echo off
REM YouthMind Database Setup Script
REM This will create the PostgreSQL database for YouthMind

echo ================================================
echo YouthMind Database Setup
echo ================================================
echo.

REM Set PostgreSQL path
set PGPATH="C:\Program Files\PostgreSQL\17\bin"
set PGPASSWORD=postgres

echo Checking if PostgreSQL is running...
%PGPATH%\pg_isready -U postgres >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PostgreSQL is not running!
    echo.
    echo Please start PostgreSQL service:
    echo 1. Press Windows + R
    echo 2. Type: services.msc
    echo 3. Find "postgresql-x64-17" service
    echo 4. Right-click and select "Start"
    echo.
    pause
    exit /b 1
)

echo [OK] PostgreSQL is running!
echo.

echo Creating database 'youthmind_db'...
%PGPATH%\psql -U postgres -c "CREATE DATABASE youthmind_db;" 2>nul
if errorlevel 1 (
    echo [INFO] Database might already exist - checking...
    %PGPATH%\psql -U postgres -c "\l" | findstr youthmind_db >nul
    if errorlevel 1 (
        echo [ERROR] Failed to create database
        pause
        exit /b 1
    ) else (
        echo [OK] Database 'youthmind_db' already exists
    )
) else (
    echo [OK] Database created successfully!
)

echo.
echo Creating user 'youthmind_user'...
%PGPATH%\psql -U postgres -c "CREATE USER youthmind_user WITH PASSWORD 'password';" 2>nul
if errorlevel 1 (
    echo [INFO] User might already exist - continuing...
) else (
    echo [OK] User created successfully!
)

echo.
echo Granting privileges...
%PGPATH%\psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE youthmind_db TO youthmind_user;" >nul 2>&1
%PGPATH%\psql -U postgres -d youthmind_db -c "GRANT ALL ON SCHEMA public TO youthmind_user;" >nul 2>&1
echo [OK] Privileges granted!

echo.
echo ================================================
echo Setup Complete!
echo ================================================
echo.
echo Database: youthmind_db
echo User: youthmind_user
echo Password: password
echo Host: localhost:5432
echo.
echo You can now run YouthMind backend:
echo   java -jar target\youthmind-backend-0.0.1-SNAPSHOT.jar
echo.
pause

