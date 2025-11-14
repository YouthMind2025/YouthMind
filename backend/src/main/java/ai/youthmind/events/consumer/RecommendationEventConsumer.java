package ai.youthmind.events.consumer;

import ai.youthmind.config.RecommendationEngineProperties;
import ai.youthmind.enums.Emotion;
import ai.youthmind.events.model.*;
import ai.youthmind.service.RVectorUpdateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for consuming recommendation engine events from Redis Streams
 * and updating R vectors via RVectorUpdateService.
 * 
 * Consumes from stream: "recommendation:rvector-updates"
 * Consumer group: "recommendation-engine-workers"
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "youthmind.recommendation.enabled", havingValue = "true", matchIfMissing = false)
public class RecommendationEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {
    
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RVectorUpdateService rVectorUpdateService;
    private final ObjectMapper eventObjectMapper;
    private final RecommendationEngineProperties properties;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter eventsProcessedTotal;
    private final Counter eventsFailedTotal;
    private final Counter eventsDeserializationErrors;
    private final Timer eventProcessingDuration;
    
    // Subscription management
    private Subscription subscription;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    @Autowired
    public RecommendationEventConsumer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            @Qualifier("redisStreamsTemplate") RedisTemplate<String, Object> redisTemplate,
            RVectorUpdateService rVectorUpdateService,
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            RecommendationEngineProperties properties,
            MeterRegistry meterRegistry) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.rVectorUpdateService = rVectorUpdateService;
        this.eventObjectMapper = eventObjectMapper;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.eventsProcessedTotal = Counter.builder("recommendation_events_processed_total")
                .description("Total number of recommendation events successfully processed")
                .register(meterRegistry);
        
        this.eventsFailedTotal = Counter.builder("recommendation_events_failed_total")
                .description("Total number of recommendation events that failed processing")
                .register(meterRegistry);
        
        this.eventsDeserializationErrors = Counter.builder("recommendation_events_deserialization_errors_total")
                .description("Total number of events that failed deserialization")
                .register(meterRegistry);
        
        this.eventProcessingDuration = Timer.builder("recommendation_event_processing_duration_seconds")
                .description("Time taken to process a recommendation event")
                .register(meterRegistry);
    }
    
    /**
     * Start consuming events from Redis Streams
     * Called automatically after bean initialization
     */
    @PostConstruct
    public void start() {
        if (isRunning.get()) {
            log.warn("RecommendationEventConsumer is already running");
            return;
        }
        
        try {
            String streamName = properties.getStreams().getRvectorUpdatesStream();
            String consumerGroup = properties.getStreams().getConsumerGroup();
            String consumerName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
            
            log.info("Starting RecommendationEventConsumer - stream: {}, group: {}, consumer: {}", 
                    streamName, consumerGroup, consumerName);
            
            // Create subscription to the stream
            StreamOffset<String> streamOffset = StreamOffset.create(streamName, ReadOffset.lastConsumed());
            Consumer consumer = Consumer.from(consumerGroup, consumerName);
            
            subscription = container.receive(consumer, streamOffset, this);
            
            isRunning.set(true);
            log.info("✅ RecommendationEventConsumer started successfully - listening to stream: {}", streamName);
            
        } catch (Exception e) {
            log.error("❌ Failed to start RecommendationEventConsumer", e);
            isRunning.set(false);
            throw new RuntimeException("Failed to start RecommendationEventConsumer", e);
        }
    }
    
    /**
     * Stop consuming events from Redis Streams
     * Called automatically before bean destruction
     */
    @PreDestroy
    public void stop() {
        if (!isRunning.get()) {
            log.debug("RecommendationEventConsumer is not running");
            return;
        }
        
        try {
            log.info("Stopping RecommendationEventConsumer...");
            
            if (subscription != null) {
                subscription.cancel();
                subscription = null;
            }
            
            isRunning.set(false);
            log.info("✅ RecommendationEventConsumer stopped successfully");
            
        } catch (Exception e) {
            log.error("Error stopping RecommendationEventConsumer", e);
        }
    }
    
    /**
     * Handle stream messages from Redis Streams
     * This method is called by StreamMessageListenerContainer for each message
     * 
     * @param message Stream message containing event data
     */
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String recordId = message.getId().getValue();
        
        try {
            log.debug("Received stream message - recordId: {}, stream: {}", recordId, message.getStream());
            
            // Extract event data from stream record
            Map<String, String> recordValue = message.getValue();
            String eventDataJson = recordValue.get("eventData");
            String eventType = recordValue.get("eventType");
            
            if (eventDataJson == null || eventDataJson.isEmpty()) {
                log.warn("Received message with empty eventData - recordId: {}, eventType: {}", recordId, eventType);
                eventsDeserializationErrors.increment();
                return;
            }
            
            // Deserialize event
            UserInteractionEvent event = deserializeEvent(eventDataJson, eventType);
            if (event == null) {
                log.warn("Failed to deserialize event - recordId: {}, eventType: {}", recordId, eventType);
                eventsDeserializationErrors.increment();
                return;
            }
            
            // Validate event
            if (!event.isValid()) {
                log.warn("Received invalid event - recordId: {}, eventType: {}, userId: {}", 
                        recordId, eventType, event.getUserId());
                eventsFailedTotal.increment();
                return;
            }
            
            // Route event to appropriate handler
            processEvent(event, recordId);
            
            eventsProcessedTotal.increment();
            log.debug("Successfully processed event - recordId: {}, eventType: {}, userId: {}", 
                     recordId, eventType, event.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing stream message - recordId: {}", recordId, e);
            eventsFailedTotal.increment();
            // Don't rethrow - continue processing other messages
        } finally {
            sample.stop(eventProcessingDuration);
        }
    }
    
    /**
     * Deserialize event JSON to UserInteractionEvent
     * 
     * @param eventDataJson JSON string of the event
     * @param eventType Event type hint (for logging)
     * @return Deserialized UserInteractionEvent or null if deserialization fails
     */
    private UserInteractionEvent deserializeEvent(String eventDataJson, String eventType) {
        try {
            // Use ObjectMapper with type information to deserialize polymorphic types
            UserInteractionEvent event = eventObjectMapper.readValue(eventDataJson, UserInteractionEvent.class);
            log.debug("Deserialized event - type: {}, userId: {}", eventType, event.getUserId());
            return event;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event - type: {}, error: {}", eventType, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error deserializing event - type: {}", eventType, e);
            return null;
        }
    }
    
    /**
     * Route event to appropriate R vector update method
     * 
     * @param event UserInteractionEvent to process
     * @param recordId Stream record ID for logging
     */
    private void processEvent(UserInteractionEvent event, String recordId) {
        try {
            String eventType = event.getEventType();
            UUID userId = event.getUserId();
            
            log.debug("Processing event - type: {}, userId: {}, recordId: {}", eventType, userId, recordId);
            
            switch (eventType) {
                case "POST_CREATED" -> handlePostCreated((PostCreatedEvent) event);
                case "POST_LIKED" -> handlePostLiked((PostLikedEvent) event);
                case "POST_VIEWED" -> handlePostViewed((PostViewedEvent) event);
                default -> {
                    log.warn("Unknown event type: {} - recordId: {}, userId: {}", eventType, recordId, userId);
                    eventsFailedTotal.increment();
                }
            }
            
        } catch (ClassCastException e) {
            log.error("Event type mismatch - expected type doesn't match actual event class - recordId: {}", 
                     recordId, e);
            eventsFailedTotal.increment();
        } catch (Exception e) {
            log.error("Error processing event - recordId: {}, eventType: {}", 
                     recordId, event.getEventType(), e);
            eventsFailedTotal.increment();
            // Don't rethrow - continue processing other events
        }
    }
    
    /**
     * Handle PostCreatedEvent - update R vector with +6 weight
     */
    private void handlePostCreated(PostCreatedEvent event) {
        try {
            UUID userId = event.getUserId();
            UUID postId = event.getPostId();
            ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
            
            if (eventEmotion == null) {
                log.warn("PostCreatedEvent has null emotion - userId: {}, postId: {}", userId, postId);
                return;
            }
            
            // Convert event model Emotion to enum Emotion
            Emotion emotionEnum = convertEmotion(eventEmotion);
            
            log.info("Processing PostCreatedEvent - userId: {}, postId: {}, emotion: {}", 
                    userId, postId, emotionEnum);
            
            rVectorUpdateService.updateOnPost(userId, postId, emotionEnum);
            
            log.debug("Successfully processed PostCreatedEvent - userId: {}, postId: {}", userId, postId);
            
        } catch (Exception e) {
            log.error("Error handling PostCreatedEvent - userId: {}, postId: {}", 
                     event.getUserId(), event.getPostId(), e);
            throw e; // Re-throw to be caught by processEvent
        }
    }
    
    /**
     * Handle PostLikedEvent - update R vector with +4 weight (only if isLike = true)
     */
    private void handlePostLiked(PostLikedEvent event) {
        try {
            UUID userId = event.getUserId();
            UUID postId = event.getPostId();
            Boolean isLike = event.getIsLike();
            ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
            
            // Only process likes, not unlikes
            if (isLike == null || !isLike) {
                log.debug("Skipping PostLikedEvent - isLike is false or null - userId: {}, postId: {}", 
                         userId, postId);
                return;
            }
            
            if (eventEmotion == null) {
                log.warn("PostLikedEvent has null emotion - userId: {}, postId: {}", userId, postId);
                return;
            }
            
            // Convert event model Emotion to enum Emotion
            Emotion emotionEnum = convertEmotion(eventEmotion);
            
            log.info("Processing PostLikedEvent - userId: {}, postId: {}, emotion: {}", 
                    userId, postId, emotionEnum);
            
            rVectorUpdateService.updateOnLike(userId, postId, emotionEnum);
            
            log.debug("Successfully processed PostLikedEvent - userId: {}, postId: {}", userId, postId);
            
        } catch (Exception e) {
            log.error("Error handling PostLikedEvent - userId: {}, postId: {}", 
                     event.getUserId(), event.getPostId(), e);
            throw e; // Re-throw to be caught by processEvent
        }
    }
    
    /**
     * Handle PostViewedEvent - update R vector with +2 weight (only if duration >= 15s)
     */
    private void handlePostViewed(PostViewedEvent event) {
        try {
            UUID userId = event.getUserId();
            UUID postId = event.getPostId();
            Integer duration = event.getDuration();
            ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
            
            // Check if duration is sufficient (should already be filtered by publisher, but double-check)
            if (!event.shouldUpdateRVector()) {
                log.debug("Skipping PostViewedEvent - duration {}s < 15s - userId: {}, postId: {}", 
                         duration, userId, postId);
                return;
            }
            
            if (eventEmotion == null) {
                log.warn("PostViewedEvent has null emotion - userId: {}, postId: {}", userId, postId);
                return;
            }
            
            // Convert event model Emotion to enum Emotion
            Emotion emotionEnum = convertEmotion(eventEmotion);
            
            log.info("Processing PostViewedEvent - userId: {}, postId: {}, emotion: {}, duration: {}s", 
                    userId, postId, emotionEnum, duration);
            
            rVectorUpdateService.updateOnView(userId, postId, emotionEnum, duration);
            
            log.debug("Successfully processed PostViewedEvent - userId: {}, postId: {}", userId, postId);
            
        } catch (Exception e) {
            log.error("Error handling PostViewedEvent - userId: {}, postId: {}", 
                     event.getUserId(), event.getPostId(), e);
            throw e; // Re-throw to be caught by processEvent
        }
    }
    
    /**
     * Convert event model Emotion to enum Emotion
     * Maps the event model emotion values to the enum used by RVectorUpdateService
     * 
     * @param eventEmotion Emotion from event model
     * @return Emotion enum for RVectorUpdateService
     */
    private Emotion convertEmotion(ai.youthmind.events.model.Emotion eventEmotion) {
        if (eventEmotion == null) {
            return null;
        }
        
        // Map event model Emotion to enum Emotion
        // Event model uses: HAPPY, SAD, ANGRY, FEAR, SURPRISED, CONFLICTED
        // Enum uses: HAPPY, SAD, ANGRY, FEAR, SURPRISED, CONFLICTED
        String emotionValue = eventEmotion.getValue(); // Gets lowercase value like "happy", "sad", etc.
        
        return switch (emotionValue.toLowerCase()) {
            case "happy" -> Emotion.HAPPY;
            case "sad" -> Emotion.SAD;
            case "angry" -> Emotion.ANGRY;
            case "fear" -> Emotion.FEAR;
            case "surprised" -> Emotion.SURPRISED;
            case "conflicted" -> Emotion.CONFLICTED;
            default -> {
                log.warn("Unknown emotion value: {}, defaulting to HAPPY", emotionValue);
                yield Emotion.HAPPY;
            }
        };
    }
    
    /**
     * Check if consumer is currently running
     * 
     * @return true if consumer is active
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Get subscription status
     * 
     * @return true if subscription is active
     */
    public boolean isSubscribed() {
        return subscription != null && subscription.isActive();
    }
}

