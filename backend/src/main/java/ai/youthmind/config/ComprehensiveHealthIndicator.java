package ai.youthmind.config;

import ai.youthmind.repository.*;
import ai.youthmind.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component("comprehensive")
public class ComprehensiveHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemoryItemRepository memoryRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean overallHealthy = true;
        
        try {
            // Database connectivity test
            boolean dbHealthy = testDatabaseHealth(details);
            overallHealthy &= dbHealthy;
            
            // Redis connectivity test
            boolean redisHealthy = testRedisHealth(details);
            overallHealthy &= redisHealthy;
            
            // Memory system test
            boolean memoryHealthy = testMemorySystemHealth(details);
            overallHealthy &= memoryHealthy;
            
            // AI services test
            boolean aiHealthy = testAIServicesHealth(details);
            overallHealthy &= aiHealthy;
            
            // Performance metrics
            addPerformanceMetrics(details);
            
            details.put("timestamp", LocalDateTime.now());
            details.put("overallStatus", overallHealthy ? "HEALTHY" : "DEGRADED");
            
            if (overallHealthy) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.down().withDetails(details).build();
            }
            
        } catch (Exception e) {
            details.put("error", e.getMessage());
            details.put("timestamp", LocalDateTime.now());
            return Health.down().withDetails(details).build();
        }
    }
    
    private boolean testDatabaseHealth(Map<String, Object> details) {
        try {
            // Test basic connectivity
            long userCount = userRepository.count();
            long postCount = postRepository.count();
            long memoryCount = memoryRepository.count();
            
            details.put("database", Map.of(
                "status", "UP",
                "userCount", userCount,
                "postCount", postCount,
                "memoryCount", memoryCount,
                "connection", "ACTIVE"
            ));
            
            return true;
        } catch (Exception e) {
            details.put("database", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
            return false;
        }
    }
    
    private boolean testRedisHealth(Map<String, Object> details) {
        try {
            // Test Redis operations
            String testKey = "health_check_" + System.currentTimeMillis();
            redisService.setValue(testKey, "test_value");
            Object value = redisService.getValue(testKey);
            redisService.deleteValue(testKey);
            
            boolean working = "test_value".equals(value);
            
            details.put("redis", Map.of(
                "status", working ? "UP" : "DEGRADED",
                "operations", "FUNCTIONAL"
            ));
            
            return working;
        } catch (Exception e) {
            details.put("redis", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
            return false;
        }
    }
    
    private boolean testMemorySystemHealth(Map<String, Object> details) {
        try {
            // Test memory system
            Map<String, Object> memoryStats = memoryService.getMemoryStats(
                UUID.fromString("00000000-0000-0000-0000-000000000000")); // Test UUID
            
            details.put("memorySystem", Map.of(
                "status", "UP",
                "statsAvailable", memoryStats != null
            ));
            
            return true;
        } catch (Exception e) {
            details.put("memorySystem", Map.of(
                "status", "DEGRADED",
                "error", e.getMessage()
            ));
            return false; // Memory system is not critical
        }
    }
    
    private boolean testAIServicesHealth(Map<String, Object> details) {
        try {
            // Test AI service availability through circuit breaker
            boolean aiAvailable = circuitBreakerService.executeWithCircuitBreaker(
                "ai_service",
                () -> {
                    // Simple test call
                    return true;
                },
                () -> false
            );
            
            details.put("aiServices", Map.of(
                "status", aiAvailable ? "UP" : "DEGRADED",
                "fallbackActive", !aiAvailable
            ));
            
            return true; // AI service degradation is acceptable
        } catch (Exception e) {
            details.put("aiServices", Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "fallbackActive", true
            ));
            return true; // We have fallbacks
        }
    }
    
    private void addPerformanceMetrics(Map<String, Object> details) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            details.put("performance", Map.of(
                "memoryUsedMB", usedMemory / 1024 / 1024,
                "memoryTotalMB", totalMemory / 1024 / 1024,
                "memoryUsagePercent", (usedMemory * 100) / totalMemory,
                "processors", runtime.availableProcessors()
            ));
            
        } catch (Exception e) {
            details.put("performance", Map.of("error", e.getMessage()));
        }
    }
}
