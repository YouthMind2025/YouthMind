package ai.youthmind.config;

import ai.youthmind.listener.AiProcessedMessageListener;
import ai.youthmind.listener.PostCreatedMessageListener;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use StringRedisSerializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use GenericJackson2JsonRedisSerializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL: 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Feed cache - shorter TTL for real-time data
        cacheConfigurations.put("feed", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // User data cache - longer TTL for stable data
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Post cache - medium TTL
        cacheConfigurations.put("posts", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Search results cache - short TTL for dynamic content
        cacheConfigurations.put("search", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Analytics cache - longer TTL for computed data
        cacheConfigurations.put("analytics", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Trending data cache - very short TTL for real-time trends
        cacheConfigurations.put("trending", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // User preferences cache - longer TTL for settings
        cacheConfigurations.put("preferences", defaultConfig.entryTtl(Duration.ofHours(6)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory,
                                                   PostCreatedMessageListener postCreatedListener,
                                                   AiProcessedMessageListener aiProcessedListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(postCreatedListener, PatternTopic.of(postCreatedChannel()));
        container.addMessageListener(aiProcessedListener, PatternTopic.of(aiProcessedChannel()));
        return container;
    }

    @Bean
    public String postCreatedChannel() {
        return "post_created";
    }

    @Bean
    public String aiProcessedChannel() {
        return "ai_processed";
    }

} 