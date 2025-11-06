package ai.youthmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Configuration properties for Qdrant Vector Database integration.
 * Provides comprehensive settings for production-ready vector database operations.
 */
@Configuration
@ConfigurationProperties(prefix = "qdrant")
@Data
public class QdrantConfig {

    // Connection Configuration
    private String host = "localhost";
    private int port = 6333;
    private String apiKey;
    private boolean useHttps = false;
    private int timeoutMs = 10000;
    private int connectionTimeoutMs = 5000;

    // Collection Configuration
    private String defaultCollection = "youthmind_vectors";
    private int vectorSize = 43; // Default for psychological vectors
    private String distanceMetric = "Cosine"; // Cosine, Dot, Euclid
    private int replicationFactor = 1;
    private int writeConsistencyFactor = 1;

    // Performance Configuration
    private int maxConnections = 10;
    private int maxIdleConnections = 5;
    private long connectionMaxIdleTimeMs = 300000; // 5 minutes
    private boolean enableConnectionPooling = true;

    // Indexing Configuration
    private boolean enableHnswIndex = true;
    private int hnswM = 16;
    private int hnswEfConstruct = 100;
    private int hnswEfSearch = 50;
    private boolean enablePayloadIndex = true;

    // Search Configuration
    private int defaultLimit = 10;
    private double defaultThreshold = 0.7;
    private boolean enableExactSearch = false;
    private int maxSearchResults = 1000;

    // Backup and Recovery Configuration
    private boolean enableAutoBackup = false;
    private String backupSchedule = "0 2 * * *"; // Daily at 2 AM
    private String backupLocation = "./backups/qdrant";
    private int backupRetentionDays = 30;
    private boolean enableCompression = true;

    // Monitoring Configuration
    private boolean enableMetrics = true;
    private boolean enableHealthChecks = true;
    private int healthCheckIntervalMs = 30000; // 30 seconds
    private boolean enableSlowQueryLogging = true;
    private long slowQueryThresholdMs = 1000;

    // Security Configuration
    private boolean enableTls = false;
    private String tlsCertPath;
    private String tlsKeyPath;
    private String tlsCaPath;
    private boolean verifyTlsCert = true;

    // Development Configuration
    private boolean enableDebugLogging = false;
    private boolean enableQueryLogging = false;
    private boolean enablePerformanceLogging = false;

    // Collection-specific configurations
    private Map<String, CollectionConfig> collections = Map.of(
        "posts", new CollectionConfig("posts", 43, "Cosine", true),
        "comments", new CollectionConfig("comments", 43, "Cosine", true),
        "moderation", new CollectionConfig("moderation", 43, "Cosine", true),
        "embeddings", new CollectionConfig("embeddings", 43, "Cosine", true)
    );

    @Data
    public static class CollectionConfig {
        private String name;
        private int vectorSize;
        private String distanceMetric;
        private boolean enablePayloadIndex;

        public CollectionConfig() {}

        public CollectionConfig(String name, int vectorSize, String distanceMetric, boolean enablePayloadIndex) {
            this.name = name;
            this.vectorSize = vectorSize;
            this.distanceMetric = distanceMetric;
            this.enablePayloadIndex = enablePayloadIndex;
        }
    }

    // Getters for connection URL
    public String getConnectionUrl() {
        String protocol = useHttps ? "https" : "http";
        return String.format("%s://%s:%d", protocol, host, port);
    }

    public boolean isProductionMode() {
        return !host.equals("localhost") && !host.equals("127.0.0.1");
    }

    public boolean isDevelopmentMode() {
        return host.equals("localhost") || host.equals("127.0.0.1");
    }

    /**
     * RestTemplate bean for HTTP communication with Qdrant REST API.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
