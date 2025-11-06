package ai.youthmind;

import ai.youthmind.config.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base test class for integration tests using Testcontainers PostgreSQL.
 * 
 * This class provides:
 * - Testcontainers PostgreSQL setup
 * - Flyway migrations execution
 * - Transaction rollback for test isolation
 * - Proper Spring Boot test context configuration
 * 
 * Usage:
 * Extend this class for integration tests that need database access.
 * Each test method will run in a transaction that gets rolled back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {YouthmindBackendApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    /**
     * Setup method that runs before each test.
     * Can be overridden by subclasses for additional setup.
     */
    @BeforeEach
    void setUp() {
        // Base setup - can be overridden by subclasses
        // The TestContainersConfig will handle database setup
    }
}

