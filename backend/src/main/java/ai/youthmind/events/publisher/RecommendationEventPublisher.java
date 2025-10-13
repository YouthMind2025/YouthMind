package ai.youthmind.events.publisher;

import ai.youthmind.events.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for publishing recommendation engine events to Redis Streams.
 * Handles serialization, error handling, retries, and metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationEventPublisher {

    private final RedisTemplate<String, Object> redisStreamsTemplate;
    private final ObjectMapper eventObjectMapper;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter postCreatedEventsPublished;
    private final Counter postViewedEventsPublished;
    private final Counter postLikedEventsPublished;
    private final Counter userScrolledEventsPublished;
    private final Counter publishErrors;
    private final Timer publishLatency;

    public RecommendationEventPublisher(RedisTemplate<String, Object> redisStreamsTemplate,
                                      ObjectMapper eventObjectMapper,
                                      MeterRegistry meterRegistry) {
        this.redisStreamsTemplate = redisStreamsTemplate;
        this.eventObjectMapper = eventObjectMapper;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.postCreatedEventsPublished = Counter.builder("recommendation.events.published")
                .tag("event_type", "post_created")
                .register(meterRegistry);
        this.postViewedEventsPublished = Counter.builder("recommendation.events.published")
                .tag("event_type", "post_viewed")
                .register(meterRegistry);
        this.postLikedEventsPublished = Counter.builder("recommendation.events.published")
                .tag("event_type", "post_liked")
                .register(meterRegistry);
        this.userScrolledEventsPublished = Counter.builder("recommendation.events.published")
                .tag("event_type", "user_scrolled")
                .register(meterRegistry);
        this.publishErrors = Counter.builder("recommendation.events.publish_errors")
                .register(meterRegistry);
        this.publishLatency = Timer.builder("recommendation.events.publish_latency")
                .register(meterRegistry);
    }

    /**
     * Publish a PostCreatedEvent to the R vector updates stream
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishPostCreated(UUID userId, UUID postId, Emotion emotion) {
        return publishPostCreated(userId, postId, emotion, null, null, null);
    }

    /**
     * Publish a PostCreatedEvent object directly
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishPostCreated(PostCreatedEvent event) {
        return publishToStream("recommendation:rvector-updates", event, postCreatedEventsPublished);
    }

    /**
     * Publish a PostCreatedEvent with additional details
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishPostViewed(UUID userId, UUID postId, Emotion emotion, Integer duration) {
        return publishPostViewed(userId, postId, emotion, duration, null);
    }

    /**
     * Publish a PostViewedEvent object directly
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishPostLiked(UUID userId, UUID postId, Emotion emotion) {
        return publishPostLiked(userId, postId, emotion, true, null, null);
    }

    /**
     * Publish a PostLikedEvent object directly
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishPostLiked(PostLikedEvent event) {
        return publishToStream("recommendation:rvector-updates", event, postLikedEventsPublished);
    }

    /**
     * Publish a PostLikedEvent with additional details
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
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
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishUserScrolled(UUID userId, Integer currentBlock) {
        return publishUserScrolled(userId, currentBlock, "down", null, null);
    }

    /**
     * Publish a UserScrolledEvent object directly
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishUserScrolled(UserScrolledEvent event) {
        return publishToStream("recommendation:block-precalc", event, userScrolledEventsPublished);
    }

    /**
     * Publish a UserScrolledEvent with additional details
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public RecordId publishUserScrolled(UUID userId, Integer currentBlock, 
                                      String scrollDirection, Integer timeOnBlock, String sessionId) {
        UserScrolledEvent event = new UserScrolledEvent(userId, currentBlock, scrollDirection, timeOnBlock);
        if (sessionId != null) {
            event.setSessionId(sessionId);
        }
        
        return publishToStream("recommendation:block-precalc", event, userScrolledEventsPublished);
    }

    /**
     * Generic method to publish events to Redis Streams
     */
    private RecordId publishToStream(String streamName, UserInteractionEvent event, Counter counter) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            try {
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

                        // Publish to stream
                        RecordId recordId = redisStreamsTemplate.opsForStream()
                                .add(streamName, record);

                        // Update metrics
                        counter.increment();
                        
                        log.debug("Published {} event {} to stream {} with ID {}", 
                                 event.getEventType(), event.getEventId(), streamName, recordId);

                        return recordId;

                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize event {}: {}", event.getEventType(), e.getMessage());
                        publishErrors.increment();
                        throw new RuntimeException("Event serialization failed", e);
            } catch (Exception e) {
                log.error("Failed to publish event {} to stream {}: {}", 
                         event.getEventType(), streamName, e.getMessage());
                publishErrors.increment();
                throw new RuntimeException("Event publishing failed", e);
            }
        } finally {
            sample.stop(publishLatency);
        }
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
}
