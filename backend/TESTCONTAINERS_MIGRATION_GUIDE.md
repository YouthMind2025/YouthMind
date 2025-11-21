# Testcontainers PostgreSQL Migration Guide

## Overview
This guide explains how to migrate existing tests from H2 to PostgreSQL using Testcontainers for accurate API testing.

## What Changed

### 1. Dependencies Added
- `org.testcontainers:junit-jupiter` - Testcontainers JUnit 5 integration
- `org.testcontainers:postgresql` - PostgreSQL container support
- `org.springframework.boot:spring-boot-testcontainers` - Spring Boot Testcontainers integration

### 2. Configuration Files
- **TestContainersConfig.java** - Main Testcontainers configuration
- **BaseIntegrationTest.java** - Base class for integration tests
- **application.properties** - Updated test properties for PostgreSQL

### 3. Key Changes
- **Database**: H2 → PostgreSQL 15 (production parity)
- **Schema**: Manual H2 schema → Flyway migrations (46 migration files)
- **Dialect**: H2Dialect → PostgreSQLDialect
- **DDL Mode**: create-drop → validate (Flyway handles schema)

## Migration Steps for Existing Tests

### Step 1: Update Test Classes
Replace H2-specific test classes with Testcontainers:

```java
// OLD: H2-based test
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.flyway.enabled=false"
})
class MyTest {
    // test methods
}

// NEW: Testcontainers-based test
@SpringBootTest
@ContextConfiguration(classes = {YouthmindBackendApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
class MyTest extends BaseIntegrationTest {
    // test methods
}
```

### Step 2: Remove H2-Specific Code
- Remove manual schema creation
- Remove H2-specific SQL syntax
- Remove `@Sql` annotations with H2 schema files

### Step 3: Update Test Data
- Use PostgreSQL-compatible SQL syntax
- Leverage Flyway migrations for schema
- Use proper PostgreSQL data types

## Benefits

### Production Parity
- Same database engine (PostgreSQL)
- Same schema (Flyway migrations)
- Same dialect and SQL features

### Accurate Testing
- Real database constraints
- Proper foreign key relationships
- Accurate data type handling

### Schema Consistency
- 46 migration files vs 6 manual fields
- Complete entity mapping
- Production-ready schema

## Running Tests

### Prerequisites
- Docker installed and running
- Maven dependencies downloaded

### Test Execution
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TestContainersSetupTest

# Run with verbose output
mvn test -X
```

### Container Management
- Containers start automatically
- Reuse enabled for faster execution
- Cleanup handled automatically

## Troubleshooting

### Common Issues

1. **Docker not running**
   ```
   Error: Could not find a valid Docker environment
   ```
   Solution: Start Docker daemon

2. **Port conflicts**
   ```
   Error: Port 5432 already in use
   ```
   Solution: Stop local PostgreSQL or change port

3. **Migration failures**
   ```
   Error: Flyway migration failed
   ```
   Solution: Check migration files for syntax errors

### Debug Configuration
Enable debug logging in `application.properties`:
```properties
logging.level.org.flywaydb=DEBUG
logging.level.org.testcontainers=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

## Verification

Run the verification test to ensure setup is working:
```bash
mvn test -Dtest=TestContainersSetupTest
```

This test verifies:
- PostgreSQL container starts
- Database connection works
- Flyway migrations execute
- Core tables exist
- Schema matches expectations

## Next Steps

1. **Run verification test** to confirm setup
2. **Migrate existing tests** one by one
3. **Update CI/CD** to support Docker
4. **Monitor test performance** and optimize if needed

