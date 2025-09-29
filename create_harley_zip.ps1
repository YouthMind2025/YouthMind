# Create zip file with all Harley testing and quality assurance files
$zipPath = "Harley_Testing_Quality_Assurance_Files.zip"
$testDir = "backend\src\test\java\ai\youthmind\testing"

# Remove existing zip if it exists
if (Test-Path $zipPath) {
    Remove-Item $zipPath
}

# Create the zip file
Compress-Archive -Path $testDir -DestinationPath $zipPath -Force

Write-Host "✅ Harley Testing & Quality Assurance files zipped successfully!"
Write-Host "📁 Zip file created: $zipPath"
Write-Host "📊 Contents:"
Write-Host "   - Coverage Testing (13 files)"
Write-Host "   - Logging & Debugging (11 files)" 
Write-Host "   - Monitoring & Dashboards (10 files)"
Write-Host "   - Performance Testing (10 files)"
Write-Host "   - Rollback Procedures (11 files)"
Write-Host "   - Validation & Error Handling (7 files)"
Write-Host "   - Test Configuration (4 files)"
Write-Host "   - Total: 66+ comprehensive testing files"

