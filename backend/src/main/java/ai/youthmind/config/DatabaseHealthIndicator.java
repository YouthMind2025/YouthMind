package ai.youthmind.config;

import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.AIAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;

    @Override
    public Health health() {
        try {
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                
                // Check if connection is valid
                if (!connection.isValid(5)) {
                    return Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .withDetail("connection", "Invalid")
                        .withDetail("service", "YouthMind Database")
                        .build();
                }

                // Get database metadata
                DatabaseMetaData metaData = connection.getMetaData();
                String databaseName = metaData.getDatabaseProductName();
                String databaseVersion = metaData.getDatabaseProductVersion();
                String driverVersion = metaData.getDriverVersion();
                String url = maskSensitiveUrl(metaData.getURL());

                // Test basic query execution
                try (PreparedStatement stmt = connection.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (!rs.next() || rs.getInt(1) != 1) {
                        return Health.down()
                            .withDetail("error", "Basic query test failed")
                            .withDetail("query", "SELECT 1")
                            .withDetail("service", "YouthMind Database")
                            .build();
                    }
                }

                // Test table existence and basic operations
                long userCount = userRepository.count();
                long postCount = postRepository.count();
                long aiAnalysisCount = aiAnalysisRepository.count();

                // Test schema validation by checking if essential tables exist
                boolean usersTableExists = checkTableExists(connection, "users");
                boolean postsTableExists = checkTableExists(connection, "posts");
                boolean aiAnalysisTableExists = checkTableExists(connection, "ai_analysis");

                if (!usersTableExists || !postsTableExists) {
                    return Health.down()
                        .withDetail("error", "Essential database tables missing")
                        .withDetail("users_table", usersTableExists)
                        .withDetail("posts_table", postsTableExists)
                        .withDetail("ai_analysis_table", aiAnalysisTableExists)
                        .withDetail("service", "YouthMind Database")
                        .build();
                }

                // Test connection pool status
                String poolStatus = getConnectionPoolStatus();
                
                // Test schema migrations
                boolean migrationsStatus = checkMigrationsStatus(connection);

                return Health.up()
                    .withDetail("database", databaseName)
                    .withDetail("version", databaseVersion)
                    .withDetail("driver_version", driverVersion)
                    .withDetail("url", url)
                    .withDetail("connection", "Active")
                    .withDetail("query_test", "SELECT 1 - SUCCESS")
                    .withDetail("users_table", usersTableExists)
                    .withDetail("posts_table", postsTableExists)
                    .withDetail("ai_analysis_table", aiAnalysisTableExists)
                    .withDetail("user_count", userCount)
                    .withDetail("post_count", postCount)
                    .withDetail("ai_analysis_count", aiAnalysisCount)
                    .withDetail("connection_pool", poolStatus)
                    .withDetail("migrations", migrationsStatus ? "Up to date" : "Issues detected")
                    .withDetail("service", "YouthMind Database")
                    .build();

            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("exception", e.getClass().getSimpleName())
                .withDetail("connection", "Failed")
                .withDetail("service", "YouthMind Database")
                .build();
        }
    }

    private boolean checkTableExists(Connection connection, String tableName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return tables.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String maskSensitiveUrl(String url) {
        if (url == null) return "N/A";
        
        // Mask password in JDBC URL if present
        return url.replaceAll("password=[^&;]*", "password=***");
    }

    private String getConnectionPoolStatus() {
        try {
            // Try to get connection pool information
            // This is a simplified approach - in production you might want to 
            // integrate with specific connection pool metrics (HikariCP, etc.)
            try (Connection conn = dataSource.getConnection()) {
                return "Available";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private boolean checkMigrationsStatus(Connection connection) {
        try {
            // Check if flyway_schema_history table exists and has recent entries
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int successfulMigrations = rs.getInt(1);
                        return successfulMigrations > 0;
                    }
                }
            }
        } catch (Exception e) {
            // If flyway table doesn't exist, that's also fine for non-flyway setups
            return true;
        }
        return false;
    }
} 