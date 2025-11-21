package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.util.ModerationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for logging AI moderation results and maintaining audit trails.
 * Provides comprehensive logging for moderation decisions and outcomes.
 */
@Service
public class ModerationLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModerationLoggingService.class);
    private static final Logger moderationLogger = LoggerFactory.getLogger("MODERATION_AUDIT");
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Log moderation result for a post with comprehensive audit information.
     * 
     * @param post The post that was moderated
     * @param userId The user who created the post
     * @param result The moderation result
     * @param moderationType The type of moderation (e.g., "pre_publish", "post_creation")
     */
    public void logPostModeration(Post post, UUID userId, ModerationResult result, String moderationType) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            
            // Structured logging for audit trail
            moderationLogger.info("POST_MODERATION|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                moderationType,
                post.getId(),
                userId,
                username,
                result.isSafe() ? "SAFE" : "FLAGGED",
                result.getConfidence(),
                ModerationUtils.getPrimaryCategory(result),
                ModerationUtils.getSeverityAsString(result),
                result.getReason() != null ? result.getReason() : "No reason provided",
                result.getTokensUsed(),
                result.getCost()
            );
            
            // Additional context logging
            logger.info("Moderation logged for post {} by user {} - Type: {}, Safe: {}, Confidence: {}", 
                       post.getId(), username, moderationType, result.isSafe(), result.getConfidence());
                       
        } catch (Exception e) {
            logger.error("Failed to log moderation result for post {} by user {}: {}", 
                        post.getId(), userId, e.getMessage());
        }
    }
    
    /**
     * Log moderation result for draft content.
     * 
     * @param title The draft title
     * @param content The draft content
     * @param userId The user who created the draft
     * @param result The moderation result
     * @param moderationType The type of moderation
     */
    public void logDraftModeration(String title, String content, UUID userId, ModerationResult result, String moderationType) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            
            // Truncate content for logging (first 100 chars)
            String truncatedContent = content != null && content.length() > 100 ? 
                content.substring(0, 100) + "..." : content;
            
            // Structured logging for audit trail
            moderationLogger.info("DRAFT_MODERATION|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                moderationType,
                userId,
                username,
                title != null ? title : "No title",
                truncatedContent,
                result.isSafe() ? "SAFE" : "FLAGGED",
                result.getConfidence(),
                ModerationUtils.getPrimaryCategory(result),
                ModerationUtils.getSeverityAsString(result),
                result.getReason() != null ? result.getReason() : "No reason provided",
                result.getTokensUsed(),
                result.getCost()
            );
            
            logger.info("Draft moderation logged for user {} - Type: {}, Safe: {}, Confidence: {}", 
                       username, moderationType, result.isSafe(), result.getConfidence());
                       
        } catch (Exception e) {
            logger.error("Failed to log draft moderation result for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Log moderation error for troubleshooting.
     * 
     * @param userId The user involved
     * @param postId The post ID (if applicable)
     * @param error The error that occurred
     * @param context Additional context about the error
     */
    public void logModerationError(UUID userId, UUID postId, Exception error, String context) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            
            moderationLogger.error("MODERATION_ERROR|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                userId,
                username,
                postId != null ? postId : "N/A",
                context,
                error.getMessage()
            );
            
            logger.error("Moderation error logged for user {} (post: {}) - Context: {}, Error: {}", 
                        username, postId, context, error.getMessage());
                        
        } catch (Exception e) {
            logger.error("Failed to log moderation error: {}", e.getMessage());
        }
    }
    
    /**
     * Log warning issuance as a result of moderation.
     * 
     * @param userId The user who received the warning
     * @param postId The post that triggered the warning
     * @param warningType The type of warning issued
     * @param severity The severity of the warning
     * @param reason The reason for the warning
     */
    public void logWarningIssuance(UUID userId, UUID postId, String warningType, String severity, String reason) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            
            moderationLogger.warn("WARNING_ISSUED|{}|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                userId,
                username,
                postId,
                warningType,
                severity,
                reason
            );
            
            logger.warn("Warning issuance logged for user {} (post: {}) - Type: {}, Severity: {}", 
                       username, postId, warningType, severity);
                       
        } catch (Exception e) {
            logger.error("Failed to log warning issuance: {}", e.getMessage());
        }
    }
    
    /**
     * Log post flagging/unflagging actions.
     * 
     * @param postId The post being flagged/unflagged
     * @param userId The user who created the post
     * @param flagged Whether the post is being flagged or unflagged
     * @param reason The reason for flagging (if applicable)
     * @param actionBy The user performing the action (null for system actions)
     */
    public void logPostFlagging(UUID postId, UUID userId, boolean flagged, String reason, UUID actionBy) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            String username = user != null ? user.getUsername() : "Unknown";
            
            String actionByUsername = "System";
            if (actionBy != null) {
                User actionByUser = userRepository.findById(actionBy).orElse(null);
                actionByUsername = actionByUser != null ? actionByUser.getUsername() : "Unknown";
            }
            
            moderationLogger.info("POST_FLAGGING|{}|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                postId,
                userId,
                username,
                flagged ? "FLAGGED" : "UNFLAGGED",
                reason != null ? reason : "No reason provided",
                actionByUsername
            );
            
            logger.info("Post flagging logged - Post: {}, User: {}, Action: {}, By: {}", 
                       postId, username, flagged ? "FLAGGED" : "UNFLAGGED", actionByUsername);
                       
        } catch (Exception e) {
            logger.error("Failed to log post flagging: {}", e.getMessage());
        }
    }
    
    /**
     * Log moderation statistics for monitoring and analytics.
     * 
     * @param totalModerated Total number of posts moderated
     * @param flaggedCount Number of posts flagged
     * @param warningsIssued Number of warnings issued
     * @param avgConfidence Average confidence score
     * @param timePeriod Time period for the statistics
     */
    public void logModerationStatistics(int totalModerated, int flaggedCount, int warningsIssued, 
                                      double avgConfidence, String timePeriod) {
        try {
            moderationLogger.info("MODERATION_STATS|{}|{}|{}|{}|{}|{}",
                LocalDateTime.now(),
                timePeriod,
                totalModerated,
                flaggedCount,
                warningsIssued,
                avgConfidence
            );
            
            logger.info("Moderation statistics logged - Period: {}, Total: {}, Flagged: {}, Warnings: {}, Avg Confidence: {}", 
                       timePeriod, totalModerated, flaggedCount, warningsIssued, avgConfidence);
                       
        } catch (Exception e) {
            logger.error("Failed to log moderation statistics: {}", e.getMessage());
        }
    }
    
}
