package ai.youthmind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Streams configuration for the recommendation engine event system.
 * Configures consumer groups, serialization, and health monitoring.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "youthmind.recommendation.enabled", havingValue = "true", matchIfMissing = false)
@EnableScheduling
public class RedisEventConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    // Queue depth tracking
    private final AtomicLong rvectorQueueDepth = new AtomicLong(0);
    private final AtomicLong blockPrecalcQueueDepth = new AtomicLong(0);

    /**
     * Redis template configured for Redis Streams operations
     */
    @Bean("redisStreamsTemplate")
    public RedisTemplate<String, Object> redisStreamsTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Use StringRedisSerializer for keys and stream IDs
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serialization for stream values
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Stream message listener container for processing events
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .batchSize(10)
                        .executor(java.util.concurrent.Executors.newFixedThreadPool(4))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        return container;
    }

    /**
     * Initialize Redis Streams and consumer groups on startup
     * Only if Redis supports Streams commands
     */
    @Bean
    public StreamInitializer streamInitializer() {
        if (supportsRedisStreams()) {
            StreamInitializer initializer = new StreamInitializer();
            registerQueueDepthGauges();
            return initializer;
        } else {
            log.warn("Redis Streams not supported by current Redis server. Recommendation engine will use fallback mode.");
            return new NoOpStreamInitializer();
        }
    }
    
    /**
     * Check if Redis server supports Streams commands
     */
    private boolean supportsRedisStreams() {
        try {
            RedisTemplate<String, Object> template = redisStreamsTemplate();
            // Try a simple XADD command to test Streams support
            template.opsForStream().add("test-stream", Collections.singletonMap("test", "value"));
            // Clean up test stream
            template.delete("test-stream");
            return true;
        } catch (Exception e) {
            log.debug("Redis Streams not supported: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Register queue depth gauges for monitoring
     */
    private void registerQueueDepthGauges() {
        Gauge.builder("recommendation_event_queue_depth", rvectorQueueDepth, AtomicLong::get)
                .tag("stream_name", "rvector-updates")
                .description("Current depth of recommendation event queue")
                .register(meterRegistry);
                
        Gauge.builder("recommendation_event_queue_depth", blockPrecalcQueueDepth, AtomicLong::get)
                .tag("stream_name", "block-precalc")
                .description("Current depth of recommendation event queue")
                .register(meterRegistry);
                
        log.info("Registered queue depth gauges for monitoring");
    }
    
    /**
     * Update queue depth metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void updateQueueDepthMetrics() {
        try {
            RedisTemplate<String, Object> template = redisStreamsTemplate();
            
            // Update R vector updates queue depth
            Long rvectorSize = template.opsForStream().size("recommendation:rvector-updates");
            if (rvectorSize != null) {
                rvectorQueueDepth.set(rvectorSize);
            }
            
            // Update block precalc queue depth
            Long blockSize = template.opsForStream().size("recommendation:block-precalc");
            if (blockSize != null) {
                blockPrecalcQueueDepth.set(blockSize);
            }
            
            log.debug("Updated queue depths - rvector: {}, block-precalc: {}", rvectorSize, blockSize);
            
        } catch (Exception e) {
            log.error("Failed to update queue depth metrics: {}", e.getMessage());
        }
    }

    /**
     * Custom ObjectMapper for event serialization with JavaTime support
     */
    @Bean("eventObjectMapper")
    public ObjectMapper eventObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * No-op stream initializer for Redis servers that don't support Streams
     */
    public class NoOpStreamInitializer extends StreamInitializer {
        public NoOpStreamInitializer() {
            super(); // Call parent constructor
            log.info("Redis Streams disabled - using fallback mode");
        }
        
        @Override
        protected void initializeStreams() {
            // Override to do nothing - no streams initialization
            log.info("Skipping Redis Streams initialization - using fallback mode");
        }
    }
    
    /**
     * Stream initializer that creates streams and consumer groups
     */
    public class StreamInitializer {
        
        public StreamInitializer() {
            initializeStreams();
        }

        protected void initializeStreams() {
            try {
                RedisTemplate<String, Object> template = redisStreamsTemplate();
                
                // Initialize recommendation:rvector-updates stream
                initializeStream(template, "recommendation:rvector-updates", "recommendation-engine-workers");
                
                // Initialize recommendation:block-precalc stream  
                initializeStream(template, "recommendation:block-precalc", "recommendation-engine-workers");
                
                log.info("✅ Redis Streams initialized successfully");
                
            } catch (Exception e) {
                log.error("❌ Failed to initialize Redis Streams: {}", e.getMessage(), e);
                throw new RuntimeException("Redis Streams initialization failed", e);
            }
        }

        private void initializeStream(RedisTemplate<String, Object> template, String streamName, String consumerGroup) {
            try {
                // Create stream if it doesn't exist
                if (!template.hasKey(streamName)) {
                    template.opsForStream().add(streamName, Collections.singletonMap("init", "stream-created"));
                    log.info("📝 Created stream: {}", streamName);
                }
                
                // Create consumer group if it doesn't exist
                try {
                    template.opsForStream().createGroup(streamName, ReadOffset.from("0"), consumerGroup);
                    log.info("👥 Created consumer group '{}' for stream: {}", consumerGroup, streamName);
                } catch (Exception e) {
                    if (e.getMessage().contains("BUSYGROUP")) {
                        log.debug("Consumer group '{}' already exists for stream: {}", consumerGroup, streamName);
                    } else {
                        throw e;
                    }
                }
                
            } catch (Exception e) {
                log.error("Failed to initialize stream {}: {}", streamName, e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Health check for Redis Streams
     */
    @Bean
    public RedisStreamsHealthIndicator redisStreamsHealthIndicator() {
        return new RedisStreamsHealthIndicator();
    }

    /**
     * Health indicator for Redis Streams monitoring
     */
    public class RedisStreamsHealthIndicator {
        
        public boolean isHealthy() {
            try {
                RedisTemplate<String, Object> template = redisStreamsTemplate();
                
                // Check if streams exist and are accessible
                boolean rvectorStreamExists = template.hasKey("recommendation:rvector-updates");
                boolean blockStreamExists = template.hasKey("recommendation:block-precalc");
                
                // Check consumer groups
                boolean rvectorGroupHealthy = checkConsumerGroup(template, "recommendation:rvector-updates", "recommendation-engine-workers");
                boolean blockGroupHealthy = checkConsumerGroup(template, "recommendation:block-precalc", "recommendation-engine-workers");
                
                return rvectorStreamExists && blockStreamExists && rvectorGroupHealthy && blockGroupHealthy;
                
            } catch (Exception e) {
                log.error("Redis Streams health check failed: {}", e.getMessage());
                return false;
            }
        }
        
        private boolean checkConsumerGroup(RedisTemplate<String, Object> template, String streamName, String groupName) {
            try {
                // Try to read from the stream to verify it exists
                template.opsForStream().range(streamName, org.springframework.data.domain.Range.unbounded(), org.springframework.data.redis.connection.Limit.limit().count(1));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
