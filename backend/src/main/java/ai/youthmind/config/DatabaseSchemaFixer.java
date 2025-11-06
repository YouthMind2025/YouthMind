package ai.youthmind.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;

/**
 * DatabaseSchemaFixer - Automatically fixes schema issues on startup
 * 
 * This component runs after application startup and fixes known schema mismatches,
 * such as VARCHAR(255) columns that should be TEXT for AI-generated content.
 * Runs automatically on every startup to ensure schema consistency.
 * 
 * @author YouthMind Development Team
 */
@Component
@Profile("!test") // Skip in test environment
public class DatabaseSchemaFixer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaFixer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Running database schema fixer...");
            fixMessagesTextColumn();
            logger.info("Database schema fixer completed successfully");
        } catch (Exception e) {
            logger.error("Error during database schema fixing: {}", e.getMessage(), e);
            // Don't throw - allow application to continue even if fix fails
            // The migration will handle it or user can fix manually
        }
    }
    
    /**
     * Fix messages.text column if it's VARCHAR(255) instead of TEXT
     */
    private void fixMessagesTextColumn() {
        try {
            // Check current column type
            String currentType = getColumnDataType("messages", "text");
            
            if (currentType == null) {
                logger.warn("messages.text column not found - table may not exist yet");
                return;
            }
            
            logger.debug("Current messages.text column type: {}", currentType);
            
            // Check if it's VARCHAR with length limit (needs fixing)
            if (currentType.startsWith("character varying") || 
                currentType.startsWith("varchar") || 
                (currentType.contains("varying") && !currentType.equals("text"))) {
                
                logger.info("Fixing messages.text column: {} -> TEXT", currentType);
                
                // Drop constraint if it exists (may have max length limit)
                try {
                    jdbcTemplate.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS chk_messages_text_length");
                    logger.debug("Dropped old constraint if it existed");
                } catch (Exception e) {
                    logger.debug("No constraint to drop or constraint doesn't exist: {}", e.getMessage());
                }
                
                // Change column type to TEXT
                jdbcTemplate.execute("ALTER TABLE messages ALTER COLUMN text TYPE TEXT");
                logger.info("Successfully changed messages.text column to TEXT");
                
                // Re-add constraint without max length (only minimum length check)
                try {
                    jdbcTemplate.execute("ALTER TABLE messages ADD CONSTRAINT chk_messages_text_length CHECK (LENGTH(text) >= 1)");
                    logger.debug("Re-added constraint with correct definition");
                } catch (Exception e) {
                    logger.debug("Constraint may already exist or creation failed: {}", e.getMessage());
                }
                
                // Add comment
                try {
                    jdbcTemplate.execute("COMMENT ON COLUMN messages.text IS 'Message content - supports unlimited length for AI-generated responses'");
                    logger.debug("Added column comment");
                } catch (Exception e) {
                    logger.debug("Comment may already exist: {}", e.getMessage());
                }
                
                logger.info("Successfully fixed messages.text column type to TEXT");
                
            } else if (currentType.equalsIgnoreCase("text")) {
                logger.debug("messages.text column is already TEXT - no fix needed");
            } else {
                logger.warn("messages.text column has unexpected type: {} - skipping fix", currentType);
            }
            
        } catch (Exception e) {
            logger.error("Error fixing messages.text column: {}", e.getMessage(), e);
            // Don't throw - allow app to continue
        }
    }
    
    /**
     * Get the data type of a specific column
     */
    private String getColumnDataType(String tableName, String columnName) {
        try {
            String sql = """
                SELECT data_type, character_maximum_length
                FROM information_schema.columns
                WHERE table_name = ? AND column_name = ?
                """;
            
            return jdbcTemplate.query(sql, 
                new ResultSetExtractor<String>() {
                    @Override
                    public String extractData(ResultSet rs) throws java.sql.SQLException {
                        if (!rs.next()) {
                            return null;
                        }
                        
                        String dataType = rs.getString("data_type");
                        Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                        
                        // Format: "character varying" or "varchar(255)" etc.
                        if (maxLength != null && (dataType.equals("character varying") || dataType.equals("varchar"))) {
                            return dataType + "(" + maxLength + ")";
                        }
                        
                        return dataType;
                    }
                },
                tableName, columnName);
            
        } catch (Exception e) {
            logger.error("Error checking column data type: {}", e.getMessage(), e);
            return null;
        }
    }
}

