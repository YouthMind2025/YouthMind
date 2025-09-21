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
    
    @Autowired
    private LocalModerationService localModerationService;
    
    @Autowired
    private ContextAwareModerationService contextAwareModerationService;
    
    @Autowired
    private WordMaskingService wordMaskingService;
    
    @Autowired
    private ToxicityOverloadPreventionService toxicityOverloadPreventionService;
    
    /**
     * Moderate post content before creation/publishing using layered moderation approach.
     * 
     * @param post The post to moderate
     * @param userId The user creating the post
     * @return ModerationResult with safety assessment
     */
    public ModerationResult moderatePost(Post post, UUID userId) {
        logger.info("Starting layered moderation for post by user {}", userId);
        
        try {
            // Combine title and content for comprehensive analysis
            String fullContent = buildContentForModeration(post);
            
            // FIRST LAYER: Local dictionary-based moderation
            LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(fullContent);
            
            // SECOND LAYER: Context-aware analysis
            ContextAwareModerationService.ContextAwareAnalysis contextAnalysis = contextAwareModerationService.analyzeWithContext(fullContent);
            
            // Check toxicity overload prevention
            ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis = toxicityOverloadPreventionService.analyzeUserToxicity(userId);
            ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck = toxicityOverloadPreventionService.checkToxicityLimits(userId);
            
            // Determine if we need AI moderation (third layer) with toxicity considerations
            boolean needsAIModeration = shouldUseAIModeration(localAnalysis, contextAnalysis, toxicityAnalysis, limitCheck);
            
            ModerationResult result;
            
            if (needsAIModeration) {
                logger.debug("Content requires enhanced AI moderation, calling DeepSeek API with context");
                // THIRD LAYER: Enhanced AI moderation with contextual analysis
                result = aiServiceClient.moderateContentWithContext(fullContent, "post", userId, null, localAnalysis, contextAnalysis);
                
                // Further enhance AI result with local analysis insights
                result = enhanceAIResultWithLocalAnalysis(result, localAnalysis, contextAnalysis);
            } else {
                logger.debug("Content passed local and context analysis, skipping AI moderation");
                // Create result based on local analysis
                result = createResultFromLocalAnalysis(localAnalysis, contextAnalysis);
            }
            
            // Apply word masking if needed
            result = applyWordMasking(post, userId, result, localAnalysis, contextAnalysis);
            
            // Log moderation result
            logModerationResult(post, userId, result);
            
            // Apply moderation actions based on result (warnings instead of blocking)
            applyModerationActions(post, userId, result);
            
            logger.info("Layered moderation completed for post by user {} - Safe: {}", userId, result.isSafe());
            return result;
            
        } catch (Exception e) {
            logger.error("Error during layered moderation for post by user {}: {}", userId, e.getMessage(), e);
            
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
            
            // Call AI moderation service with user tracking
            ModerationResult result = aiServiceClient.moderateContent(fullContent, "draft", userId, null);
            
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
     * Apply moderation actions based on AI analysis result with toxicity tracking.
     */
    private void applyModerationActions(Post post, UUID userId, ModerationResult result) {
        if (result.isSafe()) {
            // Content is safe, no action needed
            post.setFlagged(false);
            post.setFlagReason(null);
            post.setFlaggedAt(null);
            
            // Update toxicity status for positive behavior
            toxicityOverloadPreventionService.updateToxicityStatus(userId, false, 0.0);
            return;
        }
        
        // Content is flagged as potentially harmful
        post.setFlagged(true);
        post.setFlagReason(result.getReason());
        post.setFlaggedAt(LocalDateTime.now());
        
        // Update toxicity status for flagged content
        double severity = result.getConfidence();
        toxicityOverloadPreventionService.updateToxicityStatus(userId, true, severity);
        
        // Determine if we should issue a warning
        if (ModerationUtils.shouldIssueWarning(result)) {
            try {
                // Issue warning to user
                WarningType warningType = ModerationUtils.mapCategoryToWarningType(ModerationUtils.getPrimaryCategory(result));
                Severity warningSeverity = ModerationUtils.mapConfidenceToSeverity(result.getConfidence());
                
                warningService.issueWarning(
                    userId,
                    warningType,
                    warningSeverity,
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
        
        // Log toxicity analysis for flagged content
        ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis = 
            toxicityOverloadPreventionService.analyzeUserToxicity(userId);
        
        logger.info("Toxicity analysis for user {} after flagging: Level={}, Score={}, Recommendations={}", 
                   userId, toxicityAnalysis.getLevel(), toxicityAnalysis.getScore(), toxicityAnalysis.getRecommendations());
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
    
    /**
     * Determine if content needs AI moderation based on local, context, and toxicity analysis
     */
    private boolean shouldUseAIModeration(LocalModerationService.LocalModerationAnalysis localAnalysis, 
                                        ContextAwareModerationService.ContextAwareAnalysis contextAnalysis,
                                        ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis,
                                        ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck) {
        // If toxicity limits exceeded, always use AI for strict moderation
        if (limitCheck.isLimitsExceeded()) {
            logger.debug("Toxicity limits exceeded, using AI moderation for strict enforcement");
            return true;
        }
        
        // If user has high toxicity, use AI for stricter moderation
        if (toxicityAnalysis.isHighToxicity()) {
            logger.debug("High toxicity user detected, using AI moderation");
            return true;
        }
        
        // If local analysis shows clean content, no AI needed
        if (localAnalysis.isSafe()) {
            return false;
        }
        
        // If context analysis shows content is safe, no AI needed
        if (contextAnalysis.isSafe()) {
            return false;
        }
        
        // If severe content detected, use AI for final decision
        if (localAnalysis.getMaxSeverity() >= 4) {
            return true;
        }
        
        // Adjust thresholds based on toxicity level
        double contextThreshold = getContextThresholdForToxicity(toxicityAnalysis.getLevel());
        double severityThreshold = getSeverityThresholdForToxicity(toxicityAnalysis.getLevel());
        
        // If moderate severity with low context confidence, use AI
        if (localAnalysis.getMaxSeverity() >= severityThreshold && contextAnalysis.getConfidence() < contextThreshold) {
            return true;
        }
        
        // For borderline cases, use AI based on toxicity-adjusted thresholds
        return localAnalysis.getMaxSeverity() >= 2 && contextAnalysis.getConfidence() < contextThreshold;
    }
    
    /**
     * Get context threshold based on toxicity level
     */
    private double getContextThresholdForToxicity(ToxicityOverloadPreventionService.ToxicityLevel level) {
        return switch (level) {
            case LOW -> 0.8; // Standard threshold
            case MODERATE -> 0.7; // Slightly stricter
            case HIGH -> 0.6; // Stricter
            case CRITICAL -> 0.5; // Very strict
        };
    }
    
    /**
     * Get severity threshold based on toxicity level
     */
    private int getSeverityThresholdForToxicity(ToxicityOverloadPreventionService.ToxicityLevel level) {
        return switch (level) {
            case LOW -> 3; // Standard threshold
            case MODERATE -> 2; // Slightly stricter
            case HIGH -> 2; // Stricter
            case CRITICAL -> 1; // Very strict
        };
    }
    
    /**
     * Enhance AI result with local analysis insights
     */
    private ModerationResult enhanceAIResultWithLocalAnalysis(ModerationResult aiResult, 
                                                            LocalModerationService.LocalModerationAnalysis localAnalysis,
                                                            ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        // If local analysis shows content is safe but AI flagged it, trust local analysis more
        if (localAnalysis.isSafe() && !aiResult.isSafe()) {
            logger.debug("Local analysis shows safe content, overriding AI result");
            aiResult.setSafe(true);
            aiResult.setConfidence(Math.max(aiResult.getConfidence(), 0.8));
            aiResult.setReason("Content deemed safe by local analysis despite AI concerns");
        }
        
        // If context analysis shows high confidence in safety, adjust AI result
        if (contextAnalysis.isSafe() && contextAnalysis.getConfidence() > 0.8) {
            logger.debug("Context analysis shows high confidence in safety, adjusting AI result");
            aiResult.setSafe(true);
            aiResult.setConfidence(Math.max(aiResult.getConfidence(), contextAnalysis.getConfidence()));
            aiResult.setReason("Content deemed safe by context analysis: " + String.join(", ", contextAnalysis.getContextTypes()));
        }
        
        return aiResult;
    }
    
    /**
     * Create moderation result from local analysis
     */
    private ModerationResult createResultFromLocalAnalysis(LocalModerationService.LocalModerationAnalysis localAnalysis,
                                                         ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        ModerationResult result = new ModerationResult();
        
        // Determine safety based on both analyses
        boolean isSafe = localAnalysis.isSafe() || contextAnalysis.isSafe();
        result.setSafe(isSafe);
        
        // Calculate confidence
        double confidence = Math.max(
            localAnalysis.isSafe() ? 0.9 : 0.5,
            contextAnalysis.getConfidence()
        );
        result.setConfidence(confidence);
        
        // Set reason
        if (isSafe) {
            if (contextAnalysis.getContextTypes().isEmpty()) {
                result.setReason("Content passed local moderation checks");
            } else {
                result.setReason("Content deemed safe due to context: " + String.join(", ", contextAnalysis.getContextTypes()));
            }
        } else {
            result.setReason("Content flagged by local moderation: " + String.join(", ", localAnalysis.getCategories()));
        }
        
        // Set categories
        result.setDetectedCategories(localAnalysis.getCategories().toArray(new String[0]));
        
        // Set severity
        if (localAnalysis.getMaxSeverity() >= 4) {
            result.setSeverity(ModerationResult.SeverityLevel.CRITICAL);
        } else if (localAnalysis.getMaxSeverity() >= 3) {
            result.setSeverity(ModerationResult.SeverityLevel.HIGH);
        } else if (localAnalysis.getMaxSeverity() >= 2) {
            result.setSeverity(ModerationResult.SeverityLevel.MEDIUM);
        } else {
            result.setSeverity(ModerationResult.SeverityLevel.LOW);
        }
        
        result.setModeratedAt(LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Apply word masking to post content if needed
     */
    private ModerationResult applyWordMasking(Post post, UUID userId, ModerationResult result,
                                            LocalModerationService.LocalModerationAnalysis localAnalysis,
                                            ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        // Check if content needs masking
        if (localAnalysis.needsMasking() || contextAnalysis.needsMasking()) {
            logger.debug("Content needs masking, applying word masking");
            
            // Apply masking to post content
            String originalContent = post.getContent();
            WordMaskingService.MaskingResult maskingResult = wordMaskingService.maskContent(originalContent, WordMaskingService.MaskingStrategy.ASTERISK_MASKING);
            
            if (maskingResult.isContentModified()) {
                post.setContent(maskingResult.getMaskedContent());
                result.setReason(result.getReason() + " (Content masked for inappropriate language)");
                
                logger.info("Applied word masking to post content for user {}", userId);
            }
        }
        
        return result;
    }
}
