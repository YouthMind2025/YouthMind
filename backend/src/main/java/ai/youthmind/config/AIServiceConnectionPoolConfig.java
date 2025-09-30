package ai.youthmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI Service Connection Pooling.
 * Provides centralized configuration for connection management, load balancing, and monitoring.
 */
@Configuration
@ConfigurationProperties(prefix = "ai.service.connection-pool")
@Data
public class AIServiceConnectionPoolConfig {

    // Connection Pool Configuration
    private int maxConnections = 50;
    private int minConnections = 5;
    private int connectionTimeout = 5000;
    private int idleTimeout = 300000;
    private int healthCheckInterval = 30000;
    private boolean enableAutoScaling = true;
    private boolean enableHealthMonitoring = true;

    // Load Balancing Configuration
    private String defaultAlgorithm = "ROUND_ROBIN";
    private boolean enableStickySessions = false;
    private int stickySessionTimeout = 300000;
    private boolean enableHealthAwareRouting = true;

    // Service-Specific Configurations
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    private ModerationConfig moderation = new ModerationConfig();
    private VectorSearchConfig vectorSearch = new VectorSearchConfig();

    // Monitoring Configuration
    private boolean enableMetrics = true;
    private int metricsCollectionInterval = 60000;
    private boolean enablePerformanceTrends = true;
    private int historicalMetricsRetention = 100;

    @Data
    public static class DeepSeekConfig {
        private int maxConnections = 20;
        private int minConnections = 2;
        private int connectionTimeout = 5000;
        private int idleTimeout = 300000;
        private String loadBalancingAlgorithm = "ROUND_ROBIN";
        private boolean enableStickySessions = true;
        private int stickySessionTimeout = 300000;
        private boolean enableHealthMonitoring = true;
        private int healthCheckInterval = 30000;
    }

    @Data
    public static class ModerationConfig {
        private int maxConnections = 15;
        private int minConnections = 2;
        private int connectionTimeout = 3000;
        private int idleTimeout = 180000;
        private String loadBalancingAlgorithm = "LEAST_CONNECTIONS";
        private boolean enableStickySessions = false;
        private boolean enableHealthMonitoring = true;
        private int healthCheckInterval = 20000;
    }

    @Data
    public static class VectorSearchConfig {
        private int maxConnections = 30;
        private int minConnections = 3;
        private int connectionTimeout = 2000;
        private int idleTimeout = 120000;
        private String loadBalancingAlgorithm = "HEALTH_AWARE";
        private boolean enableStickySessions = false;
        private boolean enableHealthMonitoring = true;
        private int healthCheckInterval = 15000;
    }
}
