@echo off
REM Export OpenAPI specification for YouthMind Backend API
REM This script exports the API documentation in JSON format

echo 🚀 Exporting YouthMind Backend API Documentation...

REM Create docs directory if it doesn't exist
if not exist "docs" mkdir docs

REM Export OpenAPI specification in JSON format
echo 📄 Exporting OpenAPI JSON specification...
curl -s http://localhost:8080/api-docs > docs\youthmind-api.json

echo ✅ API documentation exported successfully!
echo 📁 Files created:
echo    - docs\youthmind-api.json
echo.
echo 🌐 Swagger UI available at: http://localhost:8080/swagger-ui.html
echo 📚 OpenAPI JSON available at: http://localhost:8080/api-docs
echo.
echo 💡 To convert to YAML, you can use online tools or install Python with PyYAML
pause 