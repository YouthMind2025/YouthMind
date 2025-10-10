#!/bin/bash

# Export OpenAPI specification for YouthMind Backend API
# This script exports the API documentation in both JSON and YAML formats

echo "🚀 Exporting YouthMind Backend API Documentation..."

# Create docs directory if it doesn't exist
mkdir -p docs

# Export OpenAPI specification in JSON format
echo "📄 Exporting OpenAPI JSON specification..."
curl -s http://localhost:8080/api-docs > docs/youthmind-api.json

# Export OpenAPI specification in YAML format
echo "📄 Exporting OpenAPI YAML specification..."
curl -s http://localhost:8080/api-docs | python3 -c "
import sys, json, yaml
try:
    data = json.load(sys.stdin)
    print(yaml.dump(data, default_flow_style=False, sort_keys=False))
except ImportError:
    print('PyYAML not installed. Install with: pip install pyyaml')
    sys.exit(1)
" > docs/youthmind-api.yaml

echo "✅ API documentation exported successfully!"
echo "📁 Files created:"
echo "   - docs/youthmind-api.json"
echo "   - docs/youthmind-api.yaml"
echo ""
echo "🌐 Swagger UI available at: http://localhost:8080/swagger-ui.html"
echo "📚 OpenAPI JSON available at: http://localhost:8080/api-docs" 