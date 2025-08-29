package ai.youthmind.config;

import ai.youthmind.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//@Component("redis")
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisService redisService;

    @Override
    public Health health() {
        try {
            // Test basic Redis connectivity with ping
            String pingResult = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if (!"PONG".equals(pingResult)) {
                return Health.down()
                    .withDetail("error", "Redis ping failed")
                    .withDetail("response", pingResult)
                    .withDetail("connection", "Failed")
                    .build();
            }

            // Test Redis operations
            String testKey = "youthmind:health:test";
            String testValue = "health_check_" + System.currentTimeMillis();
            
            // Test SET operation
            redisService.setValue(testKey, testValue, 30, TimeUnit.SECONDS);
            
            // Test GET operation
            Object retrievedValue = redisService.getValue(testKey);
            
            if (!testValue.equals(retrievedValue)) {
                return Health.down()
                    .withDetail("error", "Redis SET/GET operation failed")
                    .withDetail("expected", testValue)
                    .withDetail("actual", retrievedValue)
                    .withDetail("connection", "Degraded")
                    .build();
            }

            // Test DELETE operation
            Boolean deleted = redisService.deleteValue(testKey);
            if (!Boolean.TRUE.equals(deleted)) {
                return Health.down()
                    .withDetail("error", "Redis DELETE operation failed")
                    .withDetail("connection", "Degraded")
                    .build();
            }

            // Test increment operation
            String incrementKey = "youthmind:health:increment";
            Long incrementResult = redisService.increment(incrementKey);
            redisService.deleteValue(incrementKey); // Clean up

            // Get Redis connection info
            String redisInfo = getRedisInfo();
            
            // Test pub/sub capability
            boolean pubSubWorking = testPubSubCapability();

            return Health.up()
                .withDetail("ping", "PONG")
                .withDetail("operations", "SET, GET, DELETE, INCREMENT all working")
                .withDetail("connection", "Active")
                .withDetail("increment_test", incrementResult)
                .withDetail("server_info", redisInfo)
                .withDetail("pubsub", pubSubWorking ? "Working" : "Limited")
                .withDetail("service", "YouthMind Redis Cache")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("exception", e.getClass().getSimpleName())
                .withDetail("connection", "Failed")
                .withDetail("service", "YouthMind Redis Cache")
                .build();
        }
    }

    private String getRedisInfo() {
        try {
            // Get basic Redis server information
            return redisTemplate.getConnectionFactory()
                .getConnection()
                .info("server")
                .getProperty("redis_version");
        } catch (Exception e) {
            return "Unable to retrieve server info: " + e.getMessage();
        }
    }
    
    private boolean testPubSubCapability() {
        try {
            // Test publishing capability (used for post events)
            redisService.publishPostCreated("health_check_message");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 