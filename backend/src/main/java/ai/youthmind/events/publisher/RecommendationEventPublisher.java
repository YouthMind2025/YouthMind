package ai.youthmind.events.publisher;

import ai.youthmind.events.model.*;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import ai.youthmind.util.StructuredLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
// Spring retry removed - dependency not available
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.*;
import static ai.youthmind.util.StructuredLogger.*;

/**
 * Service for publishing recommendation engine events to Redis Streams.
 * Handles serialization, error handling, retries, and metrics.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "youthmind.recommendation.enabled", havingValue = "true", matchIfMissing = false)
public class RecommendationEventPublisher {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final ObjectMapper eventObjectMapper;
    private final MeterRegistry meterRegistry;
    private final UserRecommendationVectorRepository vectorRepository;

    // Metrics
    private final Counter postCreatedEventsPublished;
    private final Counter postViewedEventsPublished;
    private final Counter postLikedEventsPublished;
    private final Counter userScrolledEventsPublished;
    private final Counter publishErrors;
    private final Timer publishLatency;
    
    // Event processing metrics (created dynamically with tags)
    
    // R Vector specific metrics
    private final Counter rvectorUpdatesTotal;
    private final Counter rvectorHalvingEventsTotal;
    private final DistributionSummary rvectorDimensionDistribution;

    public RecommendationEventPublisher(RedisTemplate<String, Object> redisStreamsTemplate,
                                      ObjectMapper eventObjectMapper,
                                      MeterRegistry meterRegistry,
                                      UserRecommendationVectorRepository vectorRepository) {
        this.redisStreamsTemplate = redisStreamsTemplate;
        this.eventObjectMapper = eventObjectMapper;
        this.meterRegistry = meterRegistry;
        this.vectorRepository = vectorRepository;
        
        // Initialize metrics
        this.postCreatedEventsPublished = Counter.builder("recommendation_events_published_total")
                .tag("event_type", "post_created")
                .description("Total number of recommendation events published")
                .register(meterRegistry);
        this.postViewedEventsPublished = Counter.builder("recommendation_events_published_total")
                .tag("event_type", "post_viewed")
                .description("Total number of recommendation events published")
                .register(meterRegistry);
        this.postLikedEventsPublished = Counter.builder("recommendation_events_published_total")
                .tag("event_type", "post_liked")
                .description("Total number of recommendation events published")
                .register(meterRegistry);
        this.userScrolledEventsPublished = Counter.builder("recommendation_events_published_total")
                .tag("event_type", "user_scrolled")
                .description("Total number of recommendation events published")
                .register(meterRegistry);
        this.publishErrors = Counter.builder("recommendation.events.publish_errors")
                .register(meterRegistry);
        this.publishLatency = Timer.builder("recommendation.events.publish_latency")
                .register(meterRegistry);
        
        // Event processing metrics are created dynamically with tags when needed
        
        // Initialize R Vector specific metrics
        this.rvectorUpdatesTotal = Counter.builder("rvector_updates_total")
                .description("Total number of R vector update events published")
                .register(meterRegistry);
        this.rvectorHalvingEventsTotal = Counter.builder("rvector_halving_events_total")
                .description("Total number of R vector halving events")
                .register(meterRegistry);
        this.rvectorDimensionDistribution = DistributionSummary.builder("rvector_dimension_distribution")
                .description("Distribution of R vector dimension values")
                .register(meterRegistry);
    }

    /**
     * Publish a PostCreatedEvent to the R vector updates stream
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostCreated(UUID userId, UUID postId, Emotion emotion) {
        return publishPostCreated(userId, postId, emotion, null, null, null);
    }

    /**
     * Publish a PostCreatedEvent object directly
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostCreated(PostCreatedEvent event) {
        return publishToStream("recommendation:rvector-updates", event, postCreatedEventsPublished);
    }

    /**
     * Publish a PostCreatedEvent with additional details
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostCreated(UUID userId, UUID postId, Emotion emotion, 
                                     String content, String category, String sessionId) {
        PostCreatedEvent event = new PostCreatedEvent(userId, postId, emotion, content, category);
        if (sessionId != null) {
            event.setSessionId(sessionId);
        }
        
        return publishToStream("recommendation:rvector-updates", event, postCreatedEventsPublished);
    }

    /**
     * Publish a PostViewedEvent to the R vector updates stream (only if duration ≥ 15s)
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostViewed(UUID userId, UUID postId, Emotion emotion, Integer duration) {
        return publishPostViewed(userId, postId, emotion, duration, null);
    }

    /**
     * Publish a PostViewedEvent object directly
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostViewed(PostViewedEvent event) {
        // Only publish if duration is sufficient for R vector update
        if (!event.shouldUpdateRVector()) {
            log.debug("Skipping PostViewedEvent for user {} post {} - duration {} < 15s", 
                     event.getUserId(), event.getPostId(), event.getDuration());
            return null;
        }
        return publishToStream("recommendation:rvector-updates", event, postViewedEventsPublished);
    }

    /**
     * Publish a PostViewedEvent with session information
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostViewed(UUID userId, UUID postId, Emotion emotion, 
                                    Integer duration, String sessionId) {
        PostViewedEvent event = new PostViewedEvent(userId, postId, emotion, duration, sessionId);
        
        // Only publish if duration is sufficient for R vector update
        if (!event.shouldUpdateRVector()) {
            log.debug("Skipping PostViewedEvent for user {} post {} - duration {} < 15s", 
                     userId, postId, duration);
            return null;
        }
        
        return publishToStream("recommendation:rvector-updates", event, postViewedEventsPublished);
    }

    /**
     * Publish a PostLikedEvent to the R vector updates stream
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostLiked(UUID userId, UUID postId, Emotion emotion) {
        return publishPostLiked(userId, postId, emotion, true, null, null);
    }

    /**
     * Publish a PostLikedEvent object directly
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostLiked(PostLikedEvent event) {
        return publishToStream("recommendation:rvector-updates", event, postLikedEventsPublished);
    }

    /**
     * Publish a PostLikedEvent with additional details
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishPostLiked(UUID userId, UUID postId, Emotion emotion, 
                                   Boolean isLike, UUID postCreatorId, String sessionId) {
        PostLikedEvent event = new PostLikedEvent(userId, postId, emotion, isLike, postCreatorId);
        if (sessionId != null) {
            event.setSessionId(sessionId);
        }
        
        return publishToStream("recommendation:rvector-updates", event, postLikedEventsPublished);
    }

    /**
     * Publish a UserScrolledEvent to the block pre-calculation stream
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishUserScrolled(UUID userId, Integer currentBlock) {
        return publishUserScrolled(userId, currentBlock, "down", null, null);
    }

    /**
     * Publish a UserScrolledEvent object directly
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishUserScrolled(UserScrolledEvent event) {
        return publishToStream("recommendation:block-precalc", event, userScrolledEventsPublished);
    }

    /**
     * Publish a UserScrolledEvent with additional details
     */
    // Retry logic removed - spring-retry dependency not available
    public RecordId publishUserScrolled(UUID userId, Integer currentBlock, 
                                      String scrollDirection, Integer timeOnBlock, String sessionId) {
        UserScrolledEvent event = new UserScrolledEvent(userId, currentBlock, scrollDirection, timeOnBlock);
        if (sessionId != null) {
            event.setSessionId(sessionId);
        }
        
        return publishToStream("recommendation:block-precalc", event, userScrolledEventsPublished);
    }

    /**
     * Extract emotion weight from event for dimension distribution tracking
     */
    private double extractEmotionWeight(UserInteractionEvent event) {
        if (event instanceof PostCreatedEvent) {
            return getEmotionWeight(((PostCreatedEvent) event).getEmotion());
        } else if (event instanceof PostViewedEvent) {
            return getEmotionWeight(((PostViewedEvent) event).getEmotion());
        } else if (event instanceof PostLikedEvent) {
            return getEmotionWeight(((PostLikedEvent) event).getEmotion());
        }
        return 0.0; // Default weight for unknown event types
    }
    
    /**
     * Convert emotion enum to numeric weight for histogram recording
     */
    private double getEmotionWeight(Emotion emotion) {
        if (emotion == null) return 0.0;
        
        // Map emotions to numeric weights (0.0 to 1.0 scale)
        return switch (emotion) {
            case HAPPY, JOY -> 0.8;
            case SAD, SADNESS -> 0.3;
            case ANGRY, ANGER -> 0.2;
            case FEAR -> 0.1;
            case SURPRISED, SURPRISE -> 0.6;
            case CONFLICTED -> 0.4;
            case DISGUST -> 0.1;
        };
    }
    
    /**
     * Check if user's R vector needs halving and record metric if so
     */
    private void checkAndRecordHalvingEvent(UUID userId) {
        try {
            if (userId != null) {
                // Fixed: Changed from findCurrentByUserId() to findFirstByUserIdOrderByVersionDesc()
                // to prevent NonUniqueResultException when multiple vector versions exist
                // Old code: vectorRepository.findCurrentByUserId(userId)
                vectorRepository.findFirstByUserIdOrderByVersionDesc(userId)
                    .ifPresent(vector -> {
                        if (vector.needsHalving()) {
                            rvectorHalvingEventsTotal.increment();
                            log.debug("R vector halving event recorded for user: {}", userId);
                        }
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to check halving condition for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Record R vector metrics for the event
     */
    private void recordRVectorMetrics(UserInteractionEvent event) {
        // Record dimension distribution (histogram of emotion weights)
        double emotionWeight = extractEmotionWeight(event);
        if (emotionWeight > 0.0) {
            rvectorDimensionDistribution.record(emotionWeight);
        }
        
        // Check for halving events
        checkAndRecordHalvingEvent(event.getUserId());
    }

    /**
     * Generic method to publish events to Redis Streams or fallback to Redis Lists
     */
    private RecordId publishToStream(String streamName, UserInteractionEvent event, Counter counter) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String correlationId = MDC.get("correlationId");
        
        try {
            try {
                // Add correlation ID to event metadata if not already present
                if (correlationId != null && (event.getMetadata() == null || !event.getMetadata().contains("correlationId"))) {
                    String existingMetadata = event.getMetadata();
                    String newMetadata = existingMetadata != null ? 
                        existingMetadata + ",correlationId:" + correlationId : 
                        "correlationId:" + correlationId;
                    event.setMetadata(newMetadata);
                }
                
                // Validate event
                if (!event.isValid()) {
                    throw new IllegalArgumentException("Invalid event: " + event.getEventType());
                }

                // Serialize event to JSON
                String eventJson = eventObjectMapper.writeValueAsString(event);
                
                // Create stream record
                Map<String, Object> record = new HashMap<>();
                record.put("eventType", event.getEventType());
                record.put("eventData", eventJson);
                record.put("timestamp", event.getTimestamp().toString());
                record.put("userId", event.getUserId().toString());
                
                if (event.getSessionId() != null) {
                    record.put("sessionId", event.getSessionId());
                }
                
                if (event.getMetadata() != null) {
                    record.put("metadata", event.getMetadata());
                }

                // Try to publish to Redis Streams first
                RecordId recordId;
                try {
                    recordId = redisStreamsTemplate.opsForStream()
                            .add(streamName, record);
                } catch (Exception streamException) {
                    // Fallback to Redis Lists if Streams not supported
                    log.debug("Redis Streams not available, using fallback to Redis Lists: {}", streamException.getMessage());
                    publishToRedisList(streamName, event);
                    // Return a mock RecordId for compatibility
                    recordId = RecordId.of("0-0");
                }

                // Update metrics
                counter.increment();
                
                // Structured logging for event publish
                logEventPublished(log, event.getEventType(), event.getUserId(), 
                                 streamName, recordId.getValue());
                
                // Record R vector specific metrics for rvector-updates stream
                if ("recommendation:rvector-updates".equals(streamName)) {
                    rvectorUpdatesTotal.increment();
                    logRVectorEventMetrics(event);
                }

                return recordId;

            } catch (JsonProcessingException e) {
                log.error("Event serialization failed",
                    kv("streamName", streamName),
                    kv("eventType", event.getEventType()),
                    kv("userId", event.getUserId()),
                    kv("error", e.getMessage())
                );
                publishErrors.increment();
                throw new RuntimeException("Event serialization failed", e);
            } catch (Exception e) {
                log.error("Event publishing failed",
                    kv("streamName", streamName),
                    kv("eventType", event.getEventType()),
                    kv("userId", event.getUserId()),
                    kv("error", e.getMessage())
                );
                publishErrors.increment();
                throw new RuntimeException("Event publishing failed", e);
            }
        } finally {
            sample.stop(publishLatency);
        }
    }

    /**
     * Log R vector event metrics with before values
     */
    private void logRVectorEventMetrics(UserInteractionEvent event) {
        try {
            // Fetch current R vector before update
            // Fixed: Changed from findCurrentByUserId() to findFirstByUserIdOrderByVersionDesc()
            // to prevent NonUniqueResultException when multiple vector versions exist
            // Old code: UserRecommendationVector beforeVector = vectorRepository.findCurrentByUserId(event.getUserId()).orElse(null);
            UserRecommendationVector beforeVector = vectorRepository
                .findFirstByUserIdOrderByVersionDesc(event.getUserId()).orElse(null);
            
            if (beforeVector != null) {
                Map<String, Double> beforeValues = extractTopEmotions(beforeVector);
                
                log.info("R vector update triggered",
                    kv("userId", event.getUserId()),
                    kv("eventType", event.getEventType()),
                    kv("beforeVector", beforeValues),
                    kv("vectorVersion", beforeVector.getVersion())
                );
            }
        } catch (Exception e) {
            log.warn("Failed to log R vector metrics for user {}: {}", 
                    event.getUserId(), e.getMessage());
        }
    }
    
    /**
     * Fallback method to publish events to Redis Lists when Streams are not available
     */
    private void publishToRedisList(String listName, UserInteractionEvent event) {
        try {
            String eventJson = eventObjectMapper.writeValueAsString(event);
            // Use Redis Lists as fallback - prepend list name with "list:" to distinguish from streams
            String fallbackListName = "list:" + listName;
            redisStreamsTemplate.opsForList().leftPush(fallbackListName, eventJson);
            log.debug("Event published to Redis List fallback: {}", fallbackListName);
        } catch (Exception e) {
            log.error("Failed to publish to Redis List fallback: {}", e.getMessage());
        }
    }
    
    /**
     * Extract top emotions from R vector for logging
     */
    private Map<String, Double> extractTopEmotions(UserRecommendationVector vector) {
        Map<String, Double> topEmotions = new LinkedHashMap<>();
        try {
            if (vector == null) {
                log.warn("Cannot extract emotions from null vector");
                return topEmotions;
            }
            
            // Get all emotion weights from the vector
            Map<String, Integer> allWeights = vector.getAllEmotionWeights();
            
            // Convert Integer to Double and sort by weight (descending)
            allWeights.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(5) // Top 5 emotions for logging
                .forEach(entry -> {
                    String emotion = entry.getKey();
                    Double weight = entry.getValue().doubleValue();
                    topEmotions.put(emotion, weight);
                });
            
            log.debug("Extracted top emotions from R vector: {}", topEmotions);
            
        } catch (Exception e) {
            log.warn("Failed to extract emotions from vector: {}", e.getMessage());
            // Return empty map on error
        }
        return topEmotions;
    }

    /**
     * Check if Redis Streams are available
     */
    public boolean isStreamAvailable(String streamName) {
        try {
            return redisStreamsTemplate.hasKey(streamName);
        } catch (Exception e) {
            log.error("Failed to check stream availability for {}: {}", streamName, e.getMessage());
            return false;
        }
    }

    /**
     * Get stream length for monitoring
     */
    public Long getStreamLength(String streamName) {
        try {
            return redisStreamsTemplate.opsForStream().size(streamName);
        } catch (Exception e) {
            log.error("Failed to get stream length for {}: {}", streamName, e.getMessage());
            return -1L;
        }
    }

    /**
     * Record that an event was successfully processed
     * For use by future event processors
     */
    public void recordEventProcessed(String eventType) {
        Counter.builder("recommendation_events_processed_total")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record that an event processing failed
     * For use by future event processors
     */
    public void recordEventFailed(String eventType) {
        Counter.builder("recommendation_events_failed_total")
            .tag("event_type", eventType)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Start timing an event processing operation
     * For use by future event processors
     */
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timing an event processing operation and record the duration
     * For use by future event processors
     */
    public void stopProcessingTimer(Timer.Sample sample, String eventType) {
        Timer timer = Timer.builder("recommendation_event_processing_duration_seconds")
            .tag("event_type", eventType)
            .register(meterRegistry);
        sample.stop(timer);
    }
}
