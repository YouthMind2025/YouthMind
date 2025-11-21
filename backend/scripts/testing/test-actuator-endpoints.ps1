# YouthMind Backend - Comprehensive Actuator Health Monitoring Test Script
# This script tests all actuator endpoints for DevOps and monitoring integration

Write-Host "========================================" -ForegroundColor Green
Write-Host "YouthMind Backend Health Monitoring Test" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

$baseUrl = "http://localhost:8080"
$actuatorBase = "$baseUrl/actuator"

function Test-Endpoint {
    param(
        [string]$Url,
        [string]$Name,
        [switch]$ShowDetails
    )
    
    Write-Host "Testing: $Name" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Gray
    
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -ErrorAction Stop
        Write-Host "✅ SUCCESS" -ForegroundColor Green
        
        if ($ShowDetails) {
            Write-Host "Response:" -ForegroundColor Cyan
            $response | ConvertTo-Json -Depth 5 | Write-Host
        }
        
        return $true
    }
    catch {
        Write-Host "❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    finally {
        Write-Host ""
    }
}

# Test if application is running
Write-Host "1. Testing Application Availability" -ForegroundColor Magenta
Write-Host "-----------------------------------" -ForegroundColor Magenta
Test-Endpoint -Url $baseUrl -Name "Main Application"

# Test Health Endpoints
Write-Host "2. Testing Health Endpoints" -ForegroundColor Magenta
Write-Host "---------------------------" -ForegroundColor Magenta
Test-Endpoint -Url "$actuatorBase/health" -Name "Main Health Check" -ShowDetails
Test-Endpoint -Url "$actuatorBase/health/redis" -Name "Redis Health Check" -ShowDetails
Test-Endpoint -Url "$actuatorBase/health/database" -Name "Database Health Check" -ShowDetails
Test-Endpoint -Url "$actuatorBase/health/liveness" -Name "Kubernetes Liveness Probe"
Test-Endpoint -Url "$actuatorBase/health/readiness" -Name "Kubernetes Readiness Probe"
Test-Endpoint -Url "$actuatorBase/health/custom" -Name "Custom Health Group"

# Test Information Endpoints
Write-Host "3. Testing Information Endpoints" -ForegroundColor Magenta
Write-Host "--------------------------------" -ForegroundColor Magenta
Test-Endpoint -Url "$actuatorBase/info" -Name "Application Information" -ShowDetails

# Test Metrics Endpoints
Write-Host "4. Testing Metrics Endpoints" -ForegroundColor Magenta
Write-Host "----------------------------" -ForegroundColor Magenta
Test-Endpoint -Url "$actuatorBase/metrics" -Name "Available Metrics"
Test-Endpoint -Url "$actuatorBase/metrics/system.cpu.usage" -Name "CPU Usage Metric"
Test-Endpoint -Url "$actuatorBase/metrics/jvm.memory.used" -Name "Memory Usage Metric"
Test-Endpoint -Url "$actuatorBase/prometheus" -Name "Prometheus Metrics Endpoint"

# Test Configuration Endpoints (Admin only)
Write-Host "5. Testing Configuration Endpoints (Admin Required)" -ForegroundColor Magenta
Write-Host "---------------------------------------------------" -ForegroundColor Magenta
Test-Endpoint -Url "$actuatorBase/env" -Name "Environment Properties"
Test-Endpoint -Url "$actuatorBase/beans" -Name "Spring Beans"
Test-Endpoint -Url "$actuatorBase/configprops" -Name "Configuration Properties"
Test-Endpoint -Url "$actuatorBase/mappings" -Name "Request Mappings"
Test-Endpoint -Url "$actuatorBase/conditions" -Name "Auto-configuration Conditions"

Write-Host "========================================" -ForegroundColor Green
Write-Host "Health Monitoring Test Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "INTEGRATION NOTES FOR KEVIN (DEVOPS):" -ForegroundColor Yellow
Write-Host "-------------------------------------" -ForegroundColor Yellow
Write-Host "✅ Health Endpoint: $actuatorBase/health"
Write-Host "✅ Prometheus Metrics: $actuatorBase/prometheus"
Write-Host "✅ Kubernetes Probes: /health/liveness, /health/readiness"
Write-Host "✅ Custom Redis Health: /health/redis"
Write-Host "✅ Custom Database Health: /health/database"
Write-Host "✅ Application Info: $actuatorBase/info"
Write-Host ""
Write-Host "GRAFANA DASHBOARD RECOMMENDATIONS:" -ForegroundColor Yellow
Write-Host "- Monitor HTTP request metrics"
Write-Host "- Track JVM memory and CPU usage"
Write-Host "- Alert on health endpoint failures"
Write-Host "- Monitor Redis and Database connectivity"
Write-Host "- Track custom business metrics"
Write-Host "" 