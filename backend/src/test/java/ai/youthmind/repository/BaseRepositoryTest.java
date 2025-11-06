package ai.youthmind.repository;

import ai.youthmind.config.TestContainersConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for repository tests that provides common configuration
 * and ensures consistent database setup across all repository tests.
 * Uses Testcontainers PostgreSQL for production parity.
 */
@DataJpaTest
@ContextConfiguration(classes = {TestContainersConfig.class})
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {
    // Base class for repository tests using Testcontainers PostgreSQL
}
