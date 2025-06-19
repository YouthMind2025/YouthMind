# YouthMind Backend

A Spring Boot 3.x REST API backend for the YouthMind application.

## Features

- **Spring Boot 3.2.5** with Java 17
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **Redis** for caching and session management
- **OpenAPI/Swagger** documentation
- **Lombok** for reduced boilerplate code

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production)
- Redis 6+ (for production)

## Quick Start

1. **Clone and navigate to the project:**
   ```bash
   cd Youthmind
   ```

2. **Run the application (Development Mode):**
   ```bash
   mvn spring-boot:run
   ```
   
   This will use H2 in-memory database by default (dev profile).

3. **Access the application:**
   - **Welcome Page:** http://localhost:8080/
   - **API Test:** http://localhost:8080/api/test/hello
   - **Health Check:** http://localhost:8080/api/test/health
   - **API Documentation:** http://localhost:8080/swagger-ui.html
   - **H2 Console:** http://localhost:8080/h2-console (dev mode)

## Configuration

### Development Mode (Default)
Uses H2 in-memory database. No additional setup required.

### Production Mode
1. **Set up PostgreSQL database:**
   ```sql
   CREATE DATABASE youthmind_db;
   CREATE USER youthmind_user WITH PASSWORD 'password';
   GRANT ALL PRIVILEGES ON DATABASE youthmind_db TO youthmind_user;
   ```

2. **Start Redis server:**
   ```bash
   redis-server
   ```

3. **Run with production profile:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
   ```

## Environment Variables

You can customize the application using these environment variables:

- `DB_USERNAME` - Database username (default: youthmind_user)
- `DB_PASSWORD` - Database password (default: password)
- `REDIS_HOST` - Redis host (default: localhost)
- `REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PASSWORD` - Redis password (default: empty)
- `JWT_SECRET` - JWT secret key (default: auto-generated)
- `JWT_EXPIRATION` - JWT expiration time in milliseconds (default: 24 hours)

## API Endpoints

### Test Endpoints
- `GET /api/test/hello` - Returns "Hello, YouthMind!"
- `GET /api/test/health` - Health check endpoint

### Documentation
- `GET /swagger-ui.html` - Interactive API documentation
- `GET /api-docs` - OpenAPI JSON specification

### Authorization
- `POST /v1/auth/register` - Register new user with email and otp, returns userID, JWT and expiration timestamp

## Project Structure

```
src/main/java/ai/youthmind/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── entity/           # JPA entities
├── repository/       # Data repositories
├── service/          # Business logic services
└── YouthmindBackendApplication.java
```

## Building

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package

# Run the packaged JAR
java -jar target/youthmind-backend-0.0.1-SNAPSHOT.jar
```

## Development

The application is configured with:
- **Hot reload** enabled for development
- **Debug logging** for Spring Security and application classes
- **H2 console** accessible at `/h2-console`
- **Swagger UI** for API testing

## Security

- JWT-based authentication (configured but not implemented)
- BCrypt password encoding
- CORS enabled
- Session management disabled (stateless)

## Next Steps

This is a foundation project. You can now add:
- Authentication endpoints
- Business logic for your specific use case
- Additional entities and relationships
- Custom security configurations
- Integration with external services

---

**Status:** ✅ User registration Complete