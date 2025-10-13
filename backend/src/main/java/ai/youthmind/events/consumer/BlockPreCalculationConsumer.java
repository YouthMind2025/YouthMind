package ai.youthmind.events.consumer;

import ai.youthmind.events.model.UserScrolledEvent;
import ai.youthmind.service.FeedBlockPreCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer for block pre-calculation events from Redis Streams.
 * Processes UserScrolledEvent to pre-calculate next feed blocks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockPreCalculationConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final ObjectMapper eventObjectMapper;
    private final FeedBlockPreCalculationService feedBlockPreCalculationService;
    private final MeterRegistry meterRegistry;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

    private static final String STREAM_NAME = "recommendation:block-precalc";
    private static final String CONSUMER_GROUP = "recommendation-engine-workers";
    private static final String CONSUMER_NAME = "block-precalc-consumer-" + UUID.randomUUID().toString().substring(0, 8);

    // Rate limiting
    private final AtomicInteger requestsInLastMinute = new AtomicInteger(0);
    private final int MAX_REQUESTS_PER_MINUTE = 100;
    
    // Priority queue tracking
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService dlqProcessor = Executors.newSingleThreadScheduledExecutor();

    // Metrics
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter userScrolledProcessed;
    private final Counter rateLimitHits;
    private final Timer processingLatency;

    public BlockPreCalculationConsumer(RedisTemplate<String, Object> redisStreamsTemplate,
                                     ObjectMapper eventObjectMapper,
                                     FeedBlockPreCalculationService feedBlockPreCalculationService,
                                     MeterRegistry meterRegistry,
                                     StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer) {
        this.redisStreamsTemplate = redisStreamsTemplate;
        this.eventObjectMapper = eventObjectMapper;
        this.feedBlockPreCalculationService = feedBlockPreCalculationService;
        this.meterRegistry = meterRegistry;
        this.streamMessageListenerContainer = streamMessageListenerContainer;

        // Initialize metrics
        this.eventsProcessed = Counter.builder("recommendation.consumer.events_processed")
                .tag("consumer", "block_precalc")
                .register(meterRegistry);
        this.eventsFailed = Counter.builder("recommendation.consumer.events_failed")
                .tag("consumer", "block_precalc")
                .register(meterRegistry);
        this.userScrolledProcessed = Counter.builder("recommendation.consumer.events_by_type")
                .tag("consumer", "block_precalc")
                .tag("event_type", "user_scrolled")
                .register(meterRegistry);
        this.rateLimitHits = Counter.builder("recommendation.consumer.rate_limit_hits")
                .tag("consumer", "block_precalc")
                .register(meterRegistry);
        this.processingLatency = Timer.builder("recommendation.consumer.processing_latency")
                .tag("consumer", "block_precalc")
                .register(meterRegistry);
    }

    @PostConstruct
    public void startConsumer() {
        try {
            // Create consumer group if it doesn't exist
            createConsumerGroupIfNeeded();

            // Start listening to the stream
            streamMessageListenerContainer.receive(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamOffset.create(STREAM_NAME, ReadOffset.lastConsumed()),
                this
            );

            // Start rate limiter reset scheduler
            scheduler.scheduleAtFixedRate(this::resetRateLimiter, 0, 1, TimeUnit.MINUTES);
            
            // Start DLQ processor
            dlqProcessor.scheduleAtFixedRate(this::processDeadLetterQueue, 0, 30, TimeUnit.SECONDS);

            log.info("✅ Block Pre-calculation Consumer started - Group: {}, Consumer: {}", 
                    CONSUMER_GROUP, CONSUMER_NAME);

        } catch (Exception e) {
            log.error("❌ Failed to start Block Pre-calculation Consumer: {}", e.getMessage(), e);
            throw new RuntimeException("Consumer startup failed", e);
        }
    }

    @PreDestroy
    public void stopConsumer() {
        try {
            scheduler.shutdown();
            dlqProcessor.shutdown();
            log.info("🛑 Block Pre-calculation Consumer stopped");
        } catch (Exception e) {
            log.error("Error stopping Block Pre-calculation Consumer: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String messageId = message.getId().getValue();
        
        // Check for duplicate processing (idempotency)
        if (processedMessageIds.contains(messageId)) {
            log.debug("Skipping duplicate message: {}", messageId);
            return;
        }

        // Rate limiting check
        if (!checkRateLimit()) {
            log.warn("Rate limit exceeded, skipping message: {}", messageId);
            rateLimitHits.increment();
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Processing block pre-calculation event: {}", messageId);
            
            // Parse event from message
            UserScrolledEvent event = parseEvent(message);
            if (event == null) {
                log.warn("Failed to parse UserScrolledEvent from message: {}", messageId);
                eventsFailed.increment();
                return;
            }

            // Process event with prioritization
            processEventWithPriority(event);
            
            // Mark as processed
            processedMessageIds.add(messageId);
            eventsProcessed.increment();
            userScrolledProcessed.increment();
            
            log.debug("Successfully processed block pre-calculation event: {}", messageId);

        } catch (Exception e) {
            log.error("Failed to process block pre-calculation event {}: {}", messageId, e.getMessage(), e);
            eventsFailed.increment();
            
            // Move to dead letter queue
            moveToDeadLetterQueue(message, e);
            
        } finally {
            sample.stop(processingLatency);
        }
    }

    /**
     * Parse UserScrolledEvent from Redis Stream message
     */
    private UserScrolledEvent parseEvent(MapRecord<String, String, String> message) {
        try {
            String eventType = message.getValue().get("eventType");
            String eventData = message.getValue().get("eventData");
            
            if (eventType == null || eventData == null) {
                log.warn("Missing eventType or eventData in message");
                return null;
            }

            if (!"USER_SCROLLED".equals(eventType)) {
                log.warn("Unexpected event type in block precalc consumer: {}", eventType);
                return null;
            }

            return eventObjectMapper.readValue(eventData, UserScrolledEvent.class);

        } catch (Exception e) {
            log.error("Failed to parse UserScrolledEvent from message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Process the UserScrolledEvent with prioritization logic
     */
    private void processEventWithPriority(UserScrolledEvent event) {
        UUID userId = event.getUserId();
        Integer currentBlock = event.getCurrentBlock();
        
        // Check if user is active (prioritize active users)
        boolean isUserActive = feedBlockPreCalculationService.isUserActive(userId);
        
        if (isUserActive) {
            log.debug("Processing high-priority block pre-calculation for active user: {}", userId);
            
            // For active users, pre-calculate next 2 blocks
            feedBlockPreCalculationService.preCalculateBlocks(userId, currentBlock, 2);
            
        } else {
            log.debug("Processing normal-priority block pre-calculation for user: {}", userId);
            
            // For inactive users, only pre-calculate next block
            feedBlockPreCalculationService.preCalculateNextBlock(userId, currentBlock);
        }
    }

    /**
     * Check rate limiting
     */
    private boolean checkRateLimit() {
        int currentCount = requestsInLastMinute.incrementAndGet();
        return currentCount <= MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Reset rate limiter counter
     */
    private void resetRateLimiter() {
        requestsInLastMinute.set(0);
    }

    /**
     * Create consumer group if it doesn't exist
     */
    private void createConsumerGroupIfNeeded() {
        try {
            redisStreamsTemplate.opsForStream().createGroup(STREAM_NAME, ReadOffset.from("0"), CONSUMER_GROUP);
            log.info("Created consumer group '{}' for stream '{}'", CONSUMER_GROUP, STREAM_NAME);
        } catch (Exception e) {
            if (e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group '{}' already exists for stream '{}'", CONSUMER_GROUP, STREAM_NAME);
            } else {
                throw e;
            }
        }
    }

    /**
     * Move failed message to dead letter queue
     */
    private void moveToDeadLetterQueue(MapRecord<String, String, String> message, Exception error) {
        try {
            String dlqStream = STREAM_NAME + ":dlq";
            
            // Add error information to the message
            message.getValue().put("error", error.getMessage());
            message.getValue().put("failedAt", System.currentTimeMillis() + "");
            
            // Move to DLQ
            redisStreamsTemplate.opsForStream().add(dlqStream, message.getValue());
            
            log.warn("Moved failed message {} to DLQ: {}", message.getId(), dlqStream);
            
        } catch (Exception e) {
            log.error("Failed to move message to DLQ: {}", e.getMessage());
        }
    }

    /**
     * Process messages from dead letter queue
     */
    private void processDeadLetterQueue() {
        try {
            String dlqStream = STREAM_NAME + ":dlq";
            
            // Check if DLQ exists
            if (!redisStreamsTemplate.hasKey(dlqStream)) {
                return;
            }

            // Read from DLQ (simple implementation - in production, use proper consumer group)
            var dlqMessages = redisStreamsTemplate.opsForStream()
                    .read(StreamOffset.create(dlqStream, ReadOffset.from("0")));

            for (MapRecord<String, Object, Object> dlqMessage : dlqMessages) {
                log.info("Processing DLQ message: {}", dlqMessage.getId());
                
                // Remove error info and retry
                dlqMessage.getValue().remove("error");
                dlqMessage.getValue().remove("failedAt");
                
                // Retry processing - cast to expected type
                @SuppressWarnings("unchecked")
                MapRecord<String, String, String> typedMessage = (MapRecord<String, String, String>) (MapRecord<?, ?, ?>) dlqMessage;
                onMessage(typedMessage);
                
                // Remove from DLQ after successful processing
                redisStreamsTemplate.opsForStream().delete(dlqStream, dlqMessage.getId());
            }

        } catch (Exception e) {
            log.error("Error processing DLQ: {}", e.getMessage());
        }
    }

    /**
     * Get consumer health status
     */
    public boolean isHealthy() {
        try {
            return streamMessageListenerContainer.isRunning();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get processing statistics
     */
    public long getProcessedCount() {
        return (long) eventsProcessed.count();
    }

    /**
     * Get failure count
     */
    public long getFailureCount() {
        return (long) eventsFailed.count();
    }

    /**
     * Get current rate limit status
     */
    public int getCurrentRateLimitUsage() {
        return requestsInLastMinute.get();
    }

    /**
     * Get rate limit threshold
     */
    public int getRateLimitThreshold() {
        return MAX_REQUESTS_PER_MINUTE;
    }
}
