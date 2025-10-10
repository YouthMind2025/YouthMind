package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking view duration for posts.
 * Used to determine when to publish PostViewedEvent (duration >= 15s).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewDurationTrackingService {

    // Track active views: userId -> postId -> startTime
    private final Map<String, ViewSession> activeViews = new ConcurrentHashMap<>();

    /**
     * Start tracking a view session
     */
    public void startView(UUID userId, UUID postId) {
        String sessionKey = createSessionKey(userId, postId);
        ViewSession session = new ViewSession(userId, postId, LocalDateTime.now());
        activeViews.put(sessionKey, session);
        
        log.debug("Started view tracking for user {} post {} at {}", userId, postId, session.getStartTime());
    }

    /**
     * End tracking a view session and return duration
     */
    public Integer endView(UUID userId, UUID postId) {
        String sessionKey = createSessionKey(userId, postId);
        ViewSession session = activeViews.remove(sessionKey);
        
        if (session == null) {
            log.warn("No active view session found for user {} post {}", userId, postId);
            return 0;
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();
        
        log.debug("Ended view tracking for user {} post {} - duration: {}s", userId, postId, durationSeconds);
        
        return (int) durationSeconds;
    }

    /**
     * Check if a view session exists
     */
    public boolean hasActiveView(UUID userId, UUID postId) {
        String sessionKey = createSessionKey(userId, postId);
        return activeViews.containsKey(sessionKey);
    }

    /**
     * Get current duration for an active view
     */
    public Integer getCurrentDuration(UUID userId, UUID postId) {
        String sessionKey = createSessionKey(userId, postId);
        ViewSession session = activeViews.get(sessionKey);
        
        if (session == null) {
            return 0;
        }
        
        long durationSeconds = java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
        return (int) durationSeconds;
    }

    /**
     * Clean up expired view sessions (older than 1 hour)
     */
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        activeViews.entrySet().removeIf(entry -> {
            ViewSession session = entry.getValue();
            boolean expired = session.getStartTime().isBefore(cutoffTime);
            if (expired) {
                log.debug("Cleaned up expired view session for user {} post {}", 
                         session.getUserId(), session.getPostId());
            }
            return expired;
        });
    }

    /**
     * Create session key for tracking
     */
    private String createSessionKey(UUID userId, UUID postId) {
        return userId.toString() + ":" + postId.toString();
    }

    /**
     * Internal class to track view session
     */
    private static class ViewSession {
        private final UUID userId;
        private final UUID postId;
        private final LocalDateTime startTime;

        public ViewSession(UUID userId, UUID postId, LocalDateTime startTime) {
            this.userId = userId;
            this.postId = postId;
            this.startTime = startTime;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getPostId() {
            return postId;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }
    }
}
