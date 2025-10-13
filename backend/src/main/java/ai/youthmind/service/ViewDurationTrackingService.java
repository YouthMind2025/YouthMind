package ai.youthmind.service;

import ai.youthmind.dto.ViewSession;
import ai.youthmind.entity.Post;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking view duration with start/end session management
 * Uses Redis for active session caching and integrates with existing view tracking
 */
@Service
@Transactional
public class ViewDurationTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ViewDurationTrackingService.class);
    
    // Redis key prefix for view sessions
    private static final String VIEW_SESSION_KEY_PREFIX = "youthmind:view_session:";
    
    // TTL settings
    private static final int VIEW_SESSION_TTL_HOURS = 2;
    
    // Qualification threshold
    private static final int QUALIFIED_VIEW_THRESHOLD_SECONDS = 15;
    
    @Autowired(required = false)
    private RedisService redisService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private RVectorService rVectorService;
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * Start tracking a view session
     * Creates a Redis cache entry with the view start time and post emotion
     * 
     * @param userId The user ID
     * @param postId The post ID
     * @throws ResourceNotFoundException if post is not found
     */
    public void startView(UUID userId, UUID postId) {
        if (userId == null || postId == null) {
            logger.warn("Cannot start view session: userId or postId is null");
            return;
        }
        
        try {
            // Get post emotion for caching
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            // Create view session
            ViewSession session = new ViewSession(userId, postId, LocalDateTime.now(), post.getEmotion());
            String key = getViewSessionKey(userId, postId);
            
            // Cache in Redis if available
            if (redisService != null) {
                redisService.setValue(key, session, VIEW_SESSION_TTL_HOURS, TimeUnit.HOURS);
                logger.info("Started view session for user {} post {} (cached in Redis)", userId, postId);
            } else {
                logger.warn("Redis not available, view session not cached for user {} post {}", userId, postId);
            }
            
        } catch (ResourceNotFoundException e) {
            logger.error("Cannot start view session: post {} not found", postId);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to start view session for user {} post {}: {}", userId, postId, e.getMessage(), e);
            throw new RuntimeException("Failed to start view session", e);
        }
    }
    
    /**
     * End view session and process if qualified
     * Calculates duration and updates R vector if duration >= 15 seconds
     * 
     * @param userId The user ID
     * @param postId The post ID
     */
    public void endView(UUID userId, UUID postId) {
        if (userId == null || postId == null) {
            logger.warn("Cannot end view session: userId or postId is null");
            return;
        }
        
        try {
            String key = getViewSessionKey(userId, postId);
            ViewSession session = null;
            
            // Get session from Redis if available
            if (redisService != null) {
                session = (ViewSession) redisService.getValue(key);
            }
            
            if (session != null) {
                // Calculate duration
                long durationSeconds = Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
                
                // Record permanent view using existing PostService
                postService.trackPostView(postId, userId);
                
                // Process if qualified
                if (isQualifiedView((int) durationSeconds)) {
                    processQualifiedView(userId, postId, session.getPostEmotion());
                }
                
                // Clean up session from Redis
                redisService.deleteValue(key);
                
                logger.info("Ended view session for user {} post {} - duration: {}s", userId, postId, durationSeconds);
            } else {
                // Session not found, still record the view
                postService.trackPostView(postId, userId);
                logger.warn("View session not found for user {} post {}, recorded view without duration tracking", userId, postId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to end view session for user {} post {}: {}", userId, postId, e.getMessage(), e);
            // Don't throw exception to avoid breaking the view flow
        }
    }
    
    /**
     * Check if view duration qualifies for R vector update
     * 
     * @param durationSeconds Duration in seconds
     * @return true if duration >= 15 seconds
     */
    public boolean isQualifiedView(Integer durationSeconds) {
        return durationSeconds != null && durationSeconds >= QUALIFIED_VIEW_THRESHOLD_SECONDS;
    }
    
    /**
     * Process qualified view and update R vector
     * 
     * @param userId The user ID
     * @param postId The post ID
     * @param emotion The post emotion
     */
    public void processQualifiedView(UUID userId, UUID postId, String emotion) {
        if (emotion != null && !emotion.trim().isEmpty()) {
            try {
                // Use the threshold as the duration for R vector update
                rVectorService.updateOnView(userId, postId, Emotion.valueOf(emotion.toUpperCase()), QUALIFIED_VIEW_THRESHOLD_SECONDS);
                logger.info("✓ R vector updated for qualified view - user: {}, emotion: {}", 
                           userId, emotion);
            } catch (Exception e) {
                logger.error("✗ Failed to update R vector for qualified view - user: {}, emotion: {} - {}", 
                           userId, emotion, e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot process qualified view - emotion is null or empty for user {} post {}", userId, postId);
        }
    }
    
    /**
     * Cleanup stale view sessions
     * Redis TTL handles automatic cleanup, but this provides manual cleanup capability
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStaleViewSessions() {
        try {
            if (redisService != null) {
                // Redis TTL handles automatic cleanup
                logger.debug("View session cleanup - handled by Redis TTL");
            } else {
                logger.debug("View session cleanup - Redis not available");
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup stale view sessions: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get Redis key for view session
     * 
     * @param userId The user ID
     * @param postId The post ID
     * @return Redis key string
     */
    private String getViewSessionKey(UUID userId, UUID postId) {
        return VIEW_SESSION_KEY_PREFIX + userId + ":" + postId;
    }
}

