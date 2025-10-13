package ai.youthmind.events.consumer;

import ai.youthmind.events.model.*;
import ai.youthmind.service.RVectorService;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Consumer for R vector update events from Redis Streams.
 * Processes events to update user R vectors based on interactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RVectorUpdateConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final ObjectMapper eventObjectMapper;
    private final RVectorService rVectorService;
    private final MeterRegistry meterRegistry;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

    private static final String STREAM_NAME = "recommendation:rvector-updates";
    private static final String CONSUMER_GROUP = "recommendation-engine-workers";
    private static final String CONSUMER_NAME = "rvector-consumer-" + UUID.randomUUID().toString().substring(0, 8);

    // Metrics
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter postCreatedProcessed;
    private final Counter postViewedProcessed;
    private final Counter postLikedProcessed;
    private final Timer processingLatency;

    // Dead Letter Queue tracking
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService dlqProcessor = Executors.newSingleThreadScheduledExecutor();

    public RVectorUpdateConsumer(RedisTemplate<String, Object> redisStreamsTemplate,
                                ObjectMapper eventObjectMapper,
                                RVectorService rVectorService,
                                MeterRegistry meterRegistry,
                                StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer) {
        this.redisStreamsTemplate = redisStreamsTemplate;
        this.eventObjectMapper = eventObjectMapper;
        this.rVectorService = rVectorService;
        this.meterRegistry = meterRegistry;
        this.streamMessageListenerContainer = streamMessageListenerContainer;

        // Initialize metrics
        this.eventsProcessed = Counter.builder("recommendation.consumer.events_processed")
                .tag("consumer", "rvector_update")
                .register(meterRegistry);
        this.eventsFailed = Counter.builder("recommendation.consumer.events_failed")
                .tag("consumer", "rvector_update")
                .register(meterRegistry);
        this.postCreatedProcessed = Counter.builder("recommendation.consumer.events_by_type")
                .tag("consumer", "rvector_update")
                .tag("event_type", "post_created")
                .register(meterRegistry);
        this.postViewedProcessed = Counter.builder("recommendation.consumer.events_by_type")
                .tag("consumer", "rvector_update")
                .tag("event_type", "post_viewed")
                .register(meterRegistry);
        this.postLikedProcessed = Counter.builder("recommendation.consumer.events_by_type")
                .tag("consumer", "rvector_update")
                .tag("event_type", "post_liked")
                .register(meterRegistry);
        this.processingLatency = Timer.builder("recommendation.consumer.processing_latency")
                .tag("consumer", "rvector_update")
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

            // Start DLQ processor
            dlqProcessor.scheduleAtFixedRate(this::processDeadLetterQueue, 0, 30, TimeUnit.SECONDS);

            log.info("✅ R Vector Update Consumer started - Group: {}, Consumer: {}", 
                    CONSUMER_GROUP, CONSUMER_NAME);

        } catch (Exception e) {
            log.error("❌ Failed to start R Vector Update Consumer: {}", e.getMessage(), e);
            throw new RuntimeException("Consumer startup failed", e);
        }
    }

    @PreDestroy
    public void stopConsumer() {
        try {
            dlqProcessor.shutdown();
            log.info("🛑 R Vector Update Consumer stopped");
        } catch (Exception e) {
            log.error("Error stopping R Vector Update Consumer: {}", e.getMessage());
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

        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Processing R vector update event: {}", messageId);
            
            // Parse event from message
            UserInteractionEvent event = parseEvent(message);
            if (event == null) {
                log.warn("Failed to parse event from message: {}", messageId);
                eventsFailed.increment();
                return;
            }

            // Process event based on type
            processEvent(event);
            
            // Mark as processed
            processedMessageIds.add(messageId);
            eventsProcessed.increment();
            
            log.debug("Successfully processed R vector update event: {}", messageId);

        } catch (Exception e) {
            log.error("Failed to process R vector update event {}: {}", messageId, e.getMessage(), e);
            eventsFailed.increment();
            
            // Move to dead letter queue
            moveToDeadLetterQueue(message, e);
            
        } finally {
            sample.stop(processingLatency);
        }
    }

    /**
     * Parse UserInteractionEvent from Redis Stream message
     */
    private UserInteractionEvent parseEvent(MapRecord<String, String, String> message) {
        try {
            String eventType = message.getValue().get("eventType");
            String eventData = message.getValue().get("eventData");
            
            if (eventType == null || eventData == null) {
                log.warn("Missing eventType or eventData in message");
                return null;
            }

            // Deserialize based on event type
            switch (eventType) {
                case "POST_CREATED":
                    return eventObjectMapper.readValue(eventData, PostCreatedEvent.class);
                case "POST_VIEWED":
                    return eventObjectMapper.readValue(eventData, PostViewedEvent.class);
                case "POST_LIKED":
                    return eventObjectMapper.readValue(eventData, PostLikedEvent.class);
                default:
                    log.warn("Unknown event type: {}", eventType);
                    return null;
            }

        } catch (Exception e) {
            log.error("Failed to parse event from message: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Process the parsed event by calling appropriate R vector service method
     */
    private void processEvent(UserInteractionEvent event) {
        switch (event.getEventType()) {
            case "POST_CREATED":
                PostCreatedEvent postCreated = (PostCreatedEvent) event;
                rVectorService.updateOnPost(postCreated.getUserId(), postCreated.getPostId(), postCreated.getEmotion());
                postCreatedProcessed.increment();
                break;

            case "POST_VIEWED":
                PostViewedEvent postViewed = (PostViewedEvent) event;
                if (postViewed.shouldUpdateRVector()) {
                    rVectorService.updateOnView(postViewed.getUserId(), postViewed.getPostId(), 
                                              postViewed.getEmotion(), postViewed.getDuration());
                    postViewedProcessed.increment();
                } else {
                    log.debug("Skipping R vector update for view duration < 15s: {}", postViewed.getDuration());
                }
                break;

            case "POST_LIKED":
                PostLikedEvent postLiked = (PostLikedEvent) event;
                if (postLiked.getIsLike()) {
                    rVectorService.updateOnLike(postLiked.getUserId(), postLiked.getPostId(), postLiked.getEmotion());
                    postLikedProcessed.increment();
                } else {
                    log.debug("Skipping R vector update for unlike action");
                }
                break;

            default:
                log.warn("Unhandled event type in R vector consumer: {}", event.getEventType());
        }
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
            List<MapRecord<String, Object, Object>> dlqMessages = redisStreamsTemplate.opsForStream()
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
}
