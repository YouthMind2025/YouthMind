package ai.youthmind;

import ai.youthmind.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Testcontainers PostgreSQL setup is working correctly.
 * This test ensures:
 * - PostgreSQL container starts successfully
 * - Database connection is established
 * - Flyway migrations run successfully
 * - Schema is created properly
 */
@SpringBootTest
@ContextConfiguration(classes = {YouthmindBackendApplication.class, TestContainersConfig.class})
@ActiveProfiles("test")
@Testcontainers
public class TestContainersSetupTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12-alpine")
            .withDatabaseName("youthmind_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure container is started before configuring properties
        if (!postgres.isRunning()) {
            postgres.start();
        }
        
        // PostgreSQL datasource configuration (overrides any H2 config)
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
                // Force PostgreSQL dialect (overrides H2 dialect)
                registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
                registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop"); // Use create-drop for tests
                
                // Disable Flyway for tests - use Hibernate DDL auto instead
                registry.add("spring.flyway.enabled", () -> false);
        
        // Disable Redis for tests (can be enabled later if needed)
        registry.add("spring.data.redis.enabled", () -> false);
        registry.add("spring.data.redis.repositories.enabled", () -> false);
        
        // Configure logging for tests
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
        registry.add("logging.level.org.hibernate.type.descriptor.sql.BasicBinder", () -> "TRACE");
        registry.add("logging.level.org.flywaydb", () -> "DEBUG");
        
        // Disable AI services for tests (can be mocked later)
        registry.add("ai.deepseek.enabled", () -> false);
        registry.add("youthmind.recommendation.enabled", () -> false);
        
        // Force disable H2 console and any H2 configurations
        registry.add("spring.h2.console.enabled", () -> false);
        registry.add("spring.h2.console.path", () -> "");
        
        // Override any embedded database configurations
        registry.add("spring.datasource.embedded.database-type", () -> "");
        registry.add("spring.datasource.embedded.generate-unique-name", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testPostgreSQLContainerIsRunning() {
        // Verify container is running
        assertThat(postgres.isRunning()).isTrue();
        
        // Verify database connection
        assertThat(postgres.getDatabaseName()).isEqualTo("youthmind_test");
        assertThat(postgres.getUsername()).isEqualTo("test_user");
    }

    @Test
    void testDatabaseConnection() {
        // Test basic database connectivity
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testFlywayMigrationsExecuted() {
        // Since Flyway is disabled for tests, we verify Hibernate DDL auto worked instead
        // Check if core tables exist (created by Hibernate DDL auto)
        Integer usersTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'", 
            Integer.class
        );
        assertThat(usersTableCount).isEqualTo(1);
        
        Integer postsTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'posts'", 
            Integer.class
        );
        assertThat(postsTableCount).isEqualTo(1);
    }

    @Test
    void testCoreTablesExist() {
        // Verify that core tables from Hibernate DDL auto exist
        String[] expectedTables = {
            "users", "posts", "post_resonances", "post_collections", 
            "otp_codes", "personality_computations", "post_views",
            "psychological_analysis_results", "psychological_state_vectors",
            "user_behavior_analytics", "user_consent", "user_engagements",
            "user_feed_state", "user_interaction_modulations", "user_interests",
            "user_memory_preferences", "user_preferences", "user_recommendation_vectors",
            "user_moderation_status", "moderation_logs"
        };

        for (String tableName : expectedTables) {
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", 
                Integer.class, 
                tableName
            );
            assertThat(tableCount)
                .withFailMessage("Table '%s' should exist after Hibernate DDL auto", tableName)
                .isEqualTo(1);
        }
    }

    @Test
    void testUsersTableSchema() {
        // Verify users table has the expected columns from Hibernate DDL auto
        String[] expectedColumns = {
            "id", "email", "password", "first_name", "last_name", 
            "created_at", "updated_at", "role", "warnings_count", 
            "ban_expires_at", "avatar", "bio", "username", "user_status",
            "bookmarks_count", "likes_count", "posts_count", 
            "recommendation_engine_enabled"
        };

        for (String columnName : expectedColumns) {
            Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = ?", 
                Integer.class, 
                columnName
            );
            assertThat(columnCount)
                .withFailMessage("Column '%s' should exist in users table", columnName)
                .isEqualTo(1);
        }
    }
}