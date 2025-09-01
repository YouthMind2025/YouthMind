package ai.youthmind.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "localhost", matchIfMissing = true)
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(6379);
            redisServer.start();
            System.out.println("✅ Embedded Redis server started on port 6379");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to start embedded Redis server: " + e.getMessage());
            System.err.println("ℹ️ Application will continue without Redis caching");
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            System.out.println("🛑 Embedded Redis server stopped");
        }
    }
} 