package ai.youthmind.config;

import ai.youthmind.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component("enhancedDatabase")
public class EnhancedDatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserEngagementRepository userEngagementRepository;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                
                // Check connection validity
                if (!connection.isValid(5)) {
                    return Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .withDetail("connection", "Invalid")
                        .build();
                }

                // Get database metadata
                DatabaseMetaData metaData = connection.getMetaData();
                details.put("database", metaData.getDatabaseProductName());
                details.put("version", metaData.getDatabaseProductVersion());
                details.put("driver", metaData.getDriverVersion());
                details.put("url", maskSensitiveUrl(metaData.getURL()));

                // Test basic query execution
                long queryStartTime = System.currentTimeMillis();
                try (PreparedStatement stmt = connection.prepareStatement("SELECT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (!rs.next() || rs.getInt(1) != 1) {
                        return Health.down()
                            .withDetail("error", "Basic query test failed")
                            .withDetail("query", "SELECT 1")
                            .build();
                    }
                }
                long queryTime = System.currentTimeMillis() - queryStartTime;
                details.put("basicQueryTime", queryTime + "ms");

                // Test table counts and performance
                long countStartTime = System.currentTimeMillis();
                long userCount = userRepository.count();
                long postCount = postRepository.count();
                long commentCount = commentRepository.count();
                long engagementCount = userEngagementRepository.count();
                long countTime = System.currentTimeMillis() - countStartTime;

                details.put("userCount", userCount);
                details.put("postCount", postCount);
                details.put("commentCount", commentCount);
                details.put("engagementCount", engagementCount);
                details.put("countQueryTime", countTime + "ms");

                // Test complex query performance
                long complexQueryStartTime = System.currentTimeMillis();
                try {
                    postRepository.countPostsSince(LocalDateTime.now().minusDays(1));
                    long complexQueryTime = System.currentTimeMillis() - complexQueryStartTime;
                    details.put("complexQueryTime", complexQueryTime + "ms");
                    
                    // Warn if queries are slow
                    if (complexQueryTime > 1000) {
                        details.put("warning", "Complex queries are slow (>" + complexQueryTime + "ms)");
                    }
                } catch (Exception e) {
                    details.put("complexQueryError", e.getMessage());
                }

                // Check connection pool status
                try {
                    details.put("activeConnections", getActiveConnections(connection));
                } catch (Exception e) {
                    details.put("connectionPoolError", e.getMessage());
                }

                // Check for schema issues
                boolean schemaHealthy = checkSchemaHealth(connection);
                details.put("schemaHealthy", schemaHealthy);
                
                if (!schemaHealthy) {
                    return Health.down()
                        .withDetails(details)
                        .withDetail("error", "Schema validation failed")
                        .build();
                }

                // Performance thresholds
                boolean performanceHealthy = queryTime < 100 && countTime < 500;
                details.put("performanceHealthy", performanceHealthy);
                
                if (!performanceHealthy) {
                    return Health.degraded()
                        .withDetails(details)
                        .withDetail("warning", "Database performance is degraded")
                        .build();
                }

                details.put("status", "Database is healthy");
                details.put("lastChecked", LocalDateTime.now());
                
                return Health.up()
                    .withDetails(details)
                    .build();

            } catch (Exception e) {
                return Health.down()
                    .withDetail("error", "Database connection failed")
                    .withDetail("exception", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
            }

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Health check failed")
                .withDetail("exception", e.getMessage())
                .withDetail("timestamp", LocalDateTime.now())
                .build();
        }
    }

    private String maskSensitiveUrl(String url) {
        if (url == null) return null;
        
        // Mask password in connection URL
        return url.replaceAll("password=[^&;]*", "password=***");
    }

    private boolean checkSchemaHealth(Connection connection) {
        try {
            // Check if essential tables exist
            String[] essentialTables = {"users", "posts", "comments", "user_engagements", "conversations", "messages"};
            
            for (String tableName : essentialTables) {
                if (!checkTableExists(connection, tableName)) {
                    System.err.println("Essential table missing: " + tableName);
                    return false;
                }
            }
            
            // Check if essential indexes exist
            String[] essentialIndexes = {
                "idx_posts_user_id", "idx_posts_emotion", "idx_posts_created_at",
                "idx_users_email", "idx_comments_post_id", "idx_user_engagements_user_id"
            };
            
            for (String indexName : essentialIndexes) {
                if (!checkIndexExists(connection, indexName)) {
                    System.err.println("Essential index missing: " + indexName);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Schema health check failed: " + e.getMessage());
            return false;
        }
    }

    private boolean checkTableExists(Connection connection, String tableName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = CURRENT_SCHEMA()")) {
            
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkIndexExists(Connection connection, String indexName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?")) {
            
            stmt.setString(1, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private int getActiveConnections(Connection connection) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
