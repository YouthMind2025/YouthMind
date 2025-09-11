package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.WarningHistory;
import ai.youthmind.entity.WarningHistory.WarningType;
import ai.youthmind.entity.WarningHistory.Severity;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
import ai.youthmind.exception.WarningSystemException;
import ai.youthmind.util.ModerationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling AI-powered post moderation during creation and publishing.
 * Integrates DeepSeek API with post creation flow to provide real-time content screening.
 */
@Service
@Transactional
public class PostModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostModerationService.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private WarningService warningService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserModerationStatusRepository moderationStatusRepository;
    
    /**
     * Moderate post content before creation/publishing.
     * 
     * @param post The post to moderate
     * @param userId The user creating the post
     * @return ModerationResult with safety assessment
     */
    public ModerationResult moderatePost(Post post, UUID userId) {
        logger.info("Starting AI moderation for post by user {}", userId);
        
        try {
            // Combine title and content for comprehensive analysis
            String fullContent = buildContentForModeration(post);
            
            // Call AI moderation service
            ModerationResult result = aiServiceClient.moderateContent(fullContent, "post");
            
            // Log moderation result
            logModerationResult(post, userId, result);
            
            // Apply moderation actions based on result
            applyModerationActions(post, userId, result);
            
            logger.info("AI moderation completed for post by user {} - Safe: {}", userId, result.isSafe());
            return result;
            
        } catch (Exception e) {
            logger.error("Error during AI moderation for post by user {}: {}", userId, e.getMessage(), e);
            
            // Return safe result on error to avoid blocking legitimate content
            ModerationResult fallbackResult = ModerationResult.safe("Moderation service unavailable - content approved by default");
            fallbackResult.setConfidence(0.0); // Low confidence due to error
            return fallbackResult;
        }
    }
    
    /**
     * Moderate draft content before publishing.
     * 
     * @param title The draft title
     * @param content The draft content
     * @param userId The user publishing the draft
     * @return ModerationResult with safety assessment
     */
    public ModerationResult moderateDraft(String title, String content, UUID userId) {
        logger.info("Starting AI moderation for draft by user {}", userId);
        
        try {
            // Build content for moderation
            String fullContent = buildDraftContentForModeration(title, content);
            
            // Call AI moderation service
            ModerationResult result = aiServiceClient.moderateContent(fullContent, "draft");
            
            // Log moderation result
            logDraftModerationResult(title, content, userId, result);
            
            logger.info("AI moderation completed for draft by user {} - Safe: {}", userId, result.isSafe());
            return result;
            
        } catch (Exception e) {
            logger.error("Error during AI moderation for draft by user {}: {}", userId, e.getMessage(), e);
            
            // Return safe result on error
            ModerationResult fallbackResult = ModerationResult.safe("Moderation service unavailable - content approved by default");
            fallbackResult.setConfidence(0.0);
            return fallbackResult;
        }
    }
    
    /**
     * Check if user should be exempt from AI moderation.
     * 
     * @param userId The user to check
     * @return true if user should be exempt from moderation
     */
    public boolean isUserExemptFromModeration(UUID userId) {
        try {
            UserModerationStatus status = moderationStatusRepository.findByUserId(userId).orElse(null);
            if (status == null) {
                return false; // New user, not exempt
            }
            
            // Exempt users with clean status and low warning count
            return status.getStatus() == UserModerationStatus.ModerationStatus.CLEAN && 
                   status.getTotalWarnings() == 0 &&
                   !status.getManualReviewRequired();
                   
        } catch (Exception e) {
            logger.error("Error checking moderation exemption for user {}: {}", userId, e.getMessage());
            return false; // Default to not exempt on error
        }
    }
    
    /**
     * Build content string for moderation by combining post elements.
     */
    private String buildContentForModeration(Post post) {
        StringBuilder content = new StringBuilder();
        
        if (post.getTitle() != null && !post.getTitle().trim().isEmpty()) {
            content.append("Title: ").append(post.getTitle()).append("\n\n");
        }
        
        if (post.getContent() != null && !post.getContent().trim().isEmpty()) {
            content.append("Content: ").append(post.getContent()).append("\n\n");
        }
        
        if (post.getEmotion() != null && !post.getEmotion().trim().isEmpty()) {
            content.append("Emotion: ").append(post.getEmotion()).append("\n");
        }
        
        if (post.getTopics() != null && !post.getTopics().trim().isEmpty()) {
            content.append("Topics: ").append(post.getTopics()).append("\n");
        }
        
        return content.toString().trim();
    }
    
    /**
     * Build content string for draft moderation.
     */
    private String buildDraftContentForModeration(String title, String content) {
        StringBuilder fullContent = new StringBuilder();
        
        if (title != null && !title.trim().isEmpty()) {
            fullContent.append("Title: ").append(title).append("\n\n");
        }
        
        if (content != null && !content.trim().isEmpty()) {
            fullContent.append("Content: ").append(content).append("\n");
        }
        
        return fullContent.toString().trim();
    }
    
    /**
     * Apply moderation actions based on AI analysis result.
     */
    private void applyModerationActions(Post post, UUID userId, ModerationResult result) {
        if (result.isSafe()) {
            // Content is safe, no action needed
            post.setFlagged(false);
            post.setFlagReason(null);
            post.setFlaggedAt(null);
            return;
        }
        
        // Content is flagged as potentially harmful
        post.setFlagged(true);
        post.setFlagReason(result.getReason());
        post.setFlaggedAt(LocalDateTime.now());
        
        // Determine if we should issue a warning
        if (ModerationUtils.shouldIssueWarning(result)) {
            try {
                // Issue warning to user
                WarningType warningType = ModerationUtils.mapCategoryToWarningType(ModerationUtils.getPrimaryCategory(result));
                Severity severity = ModerationUtils.mapConfidenceToSeverity(result.getConfidence());
                
                warningService.issueWarning(
                    userId,
                    warningType,
                    severity,
                    "AI-detected harmful content: " + result.getReason(),
                    null, // System-generated warning
                    post.getId(),
                    null
                );
                
                logger.warn("Warning issued to user {} for AI-detected harmful content: {}", 
                           userId, result.getReason());
                           
            } catch (Exception e) {
                logger.error("Failed to issue warning to user {}: {}", userId, e.getMessage());
            }
        }
    }
    
    
    /**
     * Log moderation result for audit purposes.
     */
    private void logModerationResult(Post post, UUID userId, ModerationResult result) {
        logger.info("Moderation Result - User: {}, Post: {}, Safe: {}, Confidence: {}, Category: {}, Reason: {}", 
                   userId, post.getId(), result.isSafe(), result.getConfidence(), 
                   ModerationUtils.getPrimaryCategory(result), result.getReason());
    }
    
    /**
     * Log draft moderation result for audit purposes.
     */
    private void logDraftModerationResult(String title, String content, UUID userId, ModerationResult result) {
        logger.info("Draft Moderation Result - User: {}, Safe: {}, Confidence: {}, Category: {}, Reason: {}", 
                   userId, result.isSafe(), result.getConfidence(), 
                   ModerationUtils.getPrimaryCategory(result), result.getReason());
    }
}
