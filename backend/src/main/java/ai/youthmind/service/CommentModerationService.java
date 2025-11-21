package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Comment;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling comment moderation using the AdvancedModerationPipelineService.
 * Provides comprehensive moderation for comments including context window analysis,
 * local analysis, and DeepSeek integration.
 */
@Service
@Transactional
public class CommentModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommentModerationService.class);
    
    @Autowired
    private AdvancedModerationPipelineService advancedModerationPipelineService;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * Moderate a comment before saving using the advanced moderation pipeline.
     * This method performs both local analysis and DeepSeek integration.
     * 
     * @param comment The comment to moderate
     * @param userId The user creating the comment
     * @return ModerationResult with safety assessment and actions taken
     */
    public ModerationResult moderateComment(Comment comment, UUID userId) {
        try {
            logger.info("Starting comment moderation for comment by user: {}", userId);
            
            // First, save the comment temporarily to get an ID for context analysis
            Comment savedComment = commentRepository.save(comment);
            
            try {
                // Use the advanced moderation pipeline with context window analysis
                // This will perform both local analysis and DeepSeek integration automatically
                ModerationResult result = advancedModerationPipelineService.moderateWithContextWindow(
                    savedComment.getId(), 
                    "comment", 
                    userId
                );
                
                // Apply policy rubric for additional analysis
                ModerationResult rubricResult = advancedModerationPipelineService.applyPolicyRubric(
                    savedComment.getId(), 
                    "comment", 
                    userId
                );
                
                // Combine results - use the more conservative result
                ModerationResult finalResult = combineModerationResults(result, rubricResult);
                
                // Route the moderation decision to appropriate actions
                advancedModerationPipelineService.routeModerationDecision(
                    savedComment.getId(), 
                    "comment", 
                    userId, 
                    finalResult
                );
                
                // Update comment with moderation result
                updateCommentWithModerationResult(savedComment, finalResult);
                
                logger.info("Comment moderation completed for comment {} - Safe: {}, Confidence: {}", 
                    savedComment.getId(), finalResult.isSafe(), finalResult.getConfidence());
                
                return finalResult;
                
            } catch (Exception e) {
                logger.error("Error during comment moderation: {}", e.getMessage(), e);
                
                // If moderation fails, default to safe but log the error
                ModerationResult fallbackResult = ModerationResult.safe("Moderation failed, content approved by default");
                updateCommentWithModerationResult(savedComment, fallbackResult);
                return fallbackResult;
            }
            
        } catch (Exception e) {
            logger.error("Error in comment moderation service: {}", e.getMessage(), e);
            return ModerationResult.safe("Comment moderation service failed, content approved by default");
        }
    }
    
    /**
     * Moderate a comment with enhanced analysis for borderline cases.
     * This method is used when initial moderation results are inconclusive.
     * 
     * @param comment The comment to moderate
     * @param userId The user creating the comment
     * @param initialResult The initial moderation result
     * @return Enhanced ModerationResult
     */
    public ModerationResult moderateCommentWithEscalation(Comment comment, UUID userId, ModerationResult initialResult) {
        try {
            logger.info("Starting escalated comment moderation for comment by user: {}", userId);
            
            // Escalate the moderation decision
            ModerationResult escalatedResult = advancedModerationPipelineService.escalateModerationDecision(
                comment.getId(), 
                "comment", 
                userId, 
                initialResult, 
                0 // Start at level 0
            );
            
            // Update comment with escalated result
            updateCommentWithModerationResult(comment, escalatedResult);
            
            logger.info("Escalated comment moderation completed for comment {} - Safe: {}, Confidence: {}", 
                comment.getId(), escalatedResult.isSafe(), escalatedResult.getConfidence());
            
            return escalatedResult;
            
        } catch (Exception e) {
            logger.error("Error in escalated comment moderation: {}", e.getMessage(), e);
            return initialResult; // Return original result if escalation fails
        }
    }
    
    /**
     * Combine multiple moderation results into a final decision.
     * Uses the more conservative result when results differ.
     * 
     * @param result1 First moderation result
     * @param result2 Second moderation result
     * @return Combined moderation result
     */
    private ModerationResult combineModerationResults(ModerationResult result1, ModerationResult result2) {
        ModerationResult combined = new ModerationResult();
        
        // Use the more conservative decision (if either says unsafe, final result is unsafe)
        boolean isSafe = result1.isSafe() && result2.isSafe();
        combined.setSafe(isSafe);
        
        // Use the higher confidence score
        double confidence = Math.max(result1.getConfidence(), result2.getConfidence());
        combined.setConfidence(confidence);
        
        // Combine reasons
        String reason = String.format("Combined analysis: Context=%s, Policy=%s", 
            result1.getReason(), result2.getReason());
        combined.setReason(reason);
        combined.setModeratedAt(LocalDateTime.now());
        
        return combined;
    }
    
    /**
     * Update comment with moderation result information.
     * This method adds moderation metadata to the comment.
     * 
     * @param comment The comment to update
     * @param result The moderation result
     */
    private void updateCommentWithModerationResult(Comment comment, ModerationResult result) {
        try {
            // Set moderation metadata
            comment.setModeratedAt(LocalDateTime.now());
            comment.setModerationConfidence(result.getConfidence());
            comment.setModerationReason(result.getReason());
            
            if (result.isSafe()) {
                comment.setModerationStatus("APPROVED");
                comment.setIsFlagged(false);
                comment.setFlagReason(null);
                comment.setFlaggedAt(null);
            } else {
                comment.setModerationStatus("FLAGGED");
                comment.setIsFlagged(true);
                comment.setFlagReason(result.getReason());
                comment.setFlaggedAt(LocalDateTime.now());
            }
            
            // Save the updated comment
            commentRepository.save(comment);
            
            logger.debug("Comment {} moderation result stored: Safe={}, Confidence={}, Reason={}", 
                comment.getId(), result.isSafe(), result.getConfidence(), result.getReason());
            
        } catch (Exception e) {
            logger.error("Error updating comment with moderation result: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get moderation statistics for a user's comments.
     * 
     * @param userId The user ID
     * @param days Number of days to look back
     * @return ModerationStatistics
     */
    public ai.youthmind.service.AdvancedModerationPipelineService.ModerationStatistics getCommentModerationStatistics(UUID userId, int days) {
        try {
            return advancedModerationPipelineService.getModerationStatistics(userId, days);
        } catch (Exception e) {
            logger.error("Error getting comment moderation statistics: {}", e.getMessage(), e);
            return new ai.youthmind.service.AdvancedModerationPipelineService.ModerationStatistics();
        }
    }
    
    /**
     * Get moderation audit trail for a comment.
     * 
     * @param commentId The comment ID
     * @return List of moderation audit records
     */
    public java.util.List<ai.youthmind.entity.ModerationAudit> getCommentModerationAuditTrail(UUID commentId) {
        try {
            return advancedModerationPipelineService.getModerationAuditTrail(commentId, "comment");
        } catch (Exception e) {
            logger.error("Error getting comment moderation audit trail: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Check if a comment should be allowed based on user's moderation history.
     * This method can be used to implement rate limiting or user-specific restrictions.
     * 
     * @param userId The user ID
     * @return true if comment should be allowed, false otherwise
     */
    public boolean shouldAllowComment(UUID userId) {
        try {
            // Get user's recent moderation statistics
            ai.youthmind.service.AdvancedModerationPipelineService.ModerationStatistics stats = 
                getCommentModerationStatistics(userId, 7); // Last 7 days
            
            // Check if user has too many flagged comments
            if (stats.getTotalDecisions() > 0) {
                double rejectionRate = 1.0 - stats.getApprovalRate();
                
                // If rejection rate is too high, don't allow new comments
                if (rejectionRate > 0.5) { // 50% rejection rate threshold
                    logger.warn("User {} has high rejection rate: {}%, blocking new comment", 
                        userId, rejectionRate * 100);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking if comment should be allowed: {}", e.getMessage(), e);
            return true; // Default to allowing comments on error
        }
    }
}
