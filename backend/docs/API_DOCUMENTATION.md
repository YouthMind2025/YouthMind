# YouthMind Backend API Documentation

## Overview

This document describes the OpenAPI 3.0 documentation setup for the YouthMind Backend API. The API documentation is automatically generated using Springdoc OpenAPI and provides comprehensive documentation for all endpoints.

## 🚀 Quick Start

### Accessing the Documentation

1. **Swagger UI (Interactive Documentation)**
   - URL: `http://localhost:8080/swagger-ui.html`
   - Features: Interactive API testing, request/response examples, authentication

2. **OpenAPI JSON Specification**
   - URL: `http://localhost:8080/api-docs`
   - Format: JSON
   - Use: For frontend integration, code generation, API clients

### Exporting API Specification

#### For Linux/Mac:
```bash
chmod +x export-openapi.sh
./export-openapi.sh
```

#### For Windows:
```cmd
export-openapi.bat
```

This will create:
- `docs/youthmind-api.json` - OpenAPI specification in JSON format
- `docs/youthmind-api.yaml` - OpenAPI specification in YAML format (Linux/Mac only)

## 📋 Implemented Features

### ✅ Completed Tasks

1. **Springdoc OpenAPI 3 Integration**
   - ✅ Dependency added to `pom.xml`
   - ✅ Configuration in `application.yaml`
   - ✅ Custom OpenAPI configuration class

2. **API Annotations**
   - ✅ `@Operation` - Document endpoint operations
   - ✅ `@Schema` - Document data models and properties
   - ✅ `@Parameter` - Document request parameters
   - ✅ `@ApiResponse` - Document response codes
   - ✅ `@Tag` - Organize endpoints by category

3. **Security Documentation**
   - ✅ JWT Bearer token authentication scheme
   - ✅ Security requirements on protected endpoints
   - ✅ Authentication examples and descriptions

4. **Data Transfer Objects (DTOs)**
   - ✅ `UserDto` - Safe user data representation
   - ✅ `LoginRequest` - Login credentials
   - ✅ `RegisterRequest` - Registration data
   - ✅ `AuthResponse` - Authentication response

5. **Controller Documentation**
   - ✅ `TestController` - Health check and test endpoints
   - ✅ `AuthController` - Authentication endpoints
   - ✅ `UserController` - User management endpoints

## 🔧 Configuration

### Application Properties

The OpenAPI configuration is defined in `application.yaml`:

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  info:
    title: YouthMind Backend API
    description: Backend API for YouthMind - A mental health support platform for youth
    version: 1.0.0
    contact:
      name: YouthMind Team
      email: team@youthmind.ai
    license:
      name: MIT License
      url: https://opensource.org/licenses/MIT
  servers:
    - url: http://localhost:8080
      description: Development server
    - url: https://api.youthmind.ai
      description: Production server
```

### Custom Configuration

The `OpenApiConfig.java` class provides:
- Custom API information
- Security scheme definitions
- Server configurations
- Detailed API descriptions

## 📚 API Endpoints

### Authentication Endpoints (`/api/auth`)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user profile

### User Management Endpoints (`/api/users`)
- `GET /api/users` - Get all users (admin)
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `DELETE /api/users/{id}` - Delete user (admin)

### Test Endpoints (`/api/test`)
- `GET /api/test/hello` - Simple greeting
- `GET /api/test/health` - Health check
- `GET /api/test/redis` - Redis connectivity test

## 🔐 Authentication

The API uses JWT (JSON Web Tokens) for authentication:

1. **Token Format**: `Bearer <jwt-token>`
2. **Header**: `Authorization: Bearer <token>`
3. **Token Expiration**: 24 hours (configurable)
4. **Refresh Token**: Available for token renewal

### Example Usage

```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Use token for authenticated requests
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 🛠 Development

### Adding New Endpoints

1. **Add OpenAPI annotations**:
   ```java
   @Operation(summary = "Endpoint summary", description = "Detailed description")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Success"),
       @ApiResponse(responseCode = "400", description = "Bad request")
   })
   ```

2. **Document parameters**:
   ```java
   @Parameter(description = "Parameter description", example = "example_value")
   ```

3. **Document request/response models**:
   ```java
   @Schema(description = "Model description")
   public class MyDto {
       @Schema(description = "Field description", example = "example")
       private String field;
   }
   ```

### Testing Documentation

1. Start the application: `mvn spring-boot:run`
2. Open Swagger UI: `http://localhost:8080/swagger-ui.html`
3. Test endpoints directly from the UI
4. Export specification using the provided scripts

## 📖 Frontend Integration

### For React/TypeScript
```bash
# Generate TypeScript client
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/api-docs \
  -g typescript-fetch \
  -o src/api-client
```

### For Swift/iOS
```bash
# Generate Swift client
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/api-docs \
  -g swift5 \
  -o YouthMind/API
```

## 🚨 Important Notes

1. **Security**: JWT implementation is currently using dummy tokens. Real JWT service needs to be implemented.
2. **Validation**: Request validation is implemented using Bean Validation annotations.
3. **Error Handling**: Standard HTTP status codes are used for error responses.
4. **CORS**: Configure CORS settings for frontend integration.

## 📞 Support

For questions about the API documentation:
- Check the Swagger UI for interactive documentation
- Review the exported OpenAPI specification files
- Contact the YouthMind development team

---

**Task 7 Status**: ✅ **COMPLETED**

All requirements for Task 7 have been implemented:
- ✅ Springdoc OpenAPI 3 integrated
- ✅ Controllers annotated with `@Operation`, `@Schema`, `@Parameter`
- ✅ Swagger UI accessible at `/swagger-ui.html`
- ✅ Export scripts created for sharing with frontend teams
- ✅ Comprehensive API documentation with examples 