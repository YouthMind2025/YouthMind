package ai.youthmind.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an AI service connection with lifecycle management and health tracking.
 */
@Data
@Builder
public class AIServiceConnection {
    
    private String connectionId;
    private String serviceType;
    private String operation;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private boolean isHealthy;
    private RestTemplate restTemplate;
    
    // Connection metrics
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double averageResponseTime;
    
    // Health tracking
    private LocalDateTime lastHealthCheck;
    private String lastError;
    private int consecutiveFailures;
    
    // Connection state
    private boolean isActive;
    private LocalDateTime lastActivity;
    
    /**
     * Close the connection and cleanup resources
     */
    public void close() {
        isActive = false;
        lastActivity = LocalDateTime.now();
        // Additional cleanup can be added here
    }
    
    /**
     * Update connection health status
     */
    public void updateHealth(boolean healthy, String error) {
        this.isHealthy = healthy;
        this.lastHealthCheck = LocalDateTime.now();
        this.lastError = error;
        
        if (!healthy) {
            consecutiveFailures++;
        } else {
            consecutiveFailures = 0;
        }
    }
    
    /**
     * Record request metrics
     */
    public void recordRequest(boolean success, long responseTimeMs) {
        totalRequests++;
        if (success) {
            successfulRequests++;
        } else {
            failedRequests++;
        }
        
        // Update average response time
        averageResponseTime = (averageResponseTime * (totalRequests - 1) + responseTimeMs) / totalRequests;
        lastActivity = LocalDateTime.now();
    }
    
    /**
     * Check if connection is stale and should be refreshed
     */
    public boolean isStale(long staleThresholdMs) {
        if (lastActivity == null) return true;
        return System.currentTimeMillis() - lastActivity.toEpochSecond(java.time.ZoneOffset.UTC) * 1000 > staleThresholdMs;
    }
    
    /**
     * Get connection success rate
     */
    public double getSuccessRate() {
        if (totalRequests == 0) return 1.0;
        return (double) successfulRequests / totalRequests;
    }
}
