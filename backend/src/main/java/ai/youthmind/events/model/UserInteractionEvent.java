package ai.youthmind.events.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all user interaction events in the recommendation engine.
 * All events extend this class to ensure consistent structure and serialization.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PostCreatedEvent.class, name = "POST_CREATED"),
    @JsonSubTypes.Type(value = PostViewedEvent.class, name = "POST_VIEWED"),
    @JsonSubTypes.Type(value = PostLikedEvent.class, name = "POST_LIKED"),
    @JsonSubTypes.Type(value = UserScrolledEvent.class, name = "USER_SCROLLED")
})
public abstract class UserInteractionEvent {
    
    /**
     * Unique identifier for this event
     */
    private UUID eventId;
    
    /**
     * ID of the user who performed the interaction
     */
    private UUID userId;
    
    /**
     * Timestamp when the event occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Type of the event (set by subclasses)
     */
    private String eventType;
    
    /**
     * Session ID for grouping related events
     */
    private String sessionId;
    
    /**
     * Additional metadata for the event
     */
    private String metadata;

    protected UserInteractionEvent(UUID userId, String eventType) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.sessionId = null;
        this.metadata = null;
    }

    protected UserInteractionEvent(UUID userId, String eventType, String sessionId) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.metadata = null;
    }

    protected UserInteractionEvent(UUID userId, String eventType, String sessionId, String metadata) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.metadata = metadata;
    }

    /**
     * Get the event type as a string for Redis Streams
     */
    public abstract String getEventType();

    /**
     * Validate that the event has required fields
     */
    public boolean isValid() {
        return eventId != null && userId != null && timestamp != null && eventType != null;
    }
}
