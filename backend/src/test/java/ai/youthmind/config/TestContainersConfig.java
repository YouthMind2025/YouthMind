package ai.youthmind.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers configuration for PostgreSQL testing.
 * This configuration provides a PostgreSQL container for integration tests
 * to ensure production parity and accurate API testing.
 */
@TestConfiguration
@Testcontainers
public class TestContainersConfig {

    /**
     * PostgreSQL container for testing.
     * Uses PostgreSQL 12 for full compatibility with Flyway 10.12.0 and all features.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12-alpine")
            .withDatabaseName("youthmind_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true); // Enable container reuse for faster test execution

    /**
     * Bean to provide the PostgreSQL container for manual access if needed.
     * This is useful for tests that need direct container access.
     */
    @Bean
    @Primary
    public PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }
}
