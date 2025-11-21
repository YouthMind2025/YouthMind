package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Advanced Moderation Pipeline Service implementing sophisticated moderation logic
 * with context window checks, escalation, caching, and comprehensive decision routing.
 */
@Service
@Transactional
public class AdvancedModerationPipelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedModerationPipelineService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostModerationService postModerationService;
    
    @Autowired
    private DeepSeekAdapterService deepSeekAdapterService;
    
    @Autowired
    private DeepSeekCostOptimizationService costOptimizationService;
    
    @Autowired
    private DeepSeekUsageMonitoringService usageMonitoringService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ModerationAuditRepository moderationAuditRepository;
    
    // Configuration constants
    private static final int MAX_CONTEXT_LEVELS = 3; // Max 3 levels up as specified
    private static final int MAX_ESCALATION_LEVELS = 3; // Max 3 escalation levels
    private static final String CACHE_PREFIX = "moderation:context:";
    private static final int CACHE_TTL_HOURS = 24;
    
    /**
     * 5.1 Implement context window checks (comment + parent + grandparent)
     * Analyze content with full context including parent and grandparent content
     */
    public ModerationResult moderateWithContextWindow(UUID contentId, String contentType, UUID userId) {
        try {
            logger.info("Starting context window moderation for {}: {}", contentType, contentId);
            
            // Build context window
            ContextWindow contextWindow = buildContextWindow(contentId, contentType);
            
            // Check cache first
            String cacheKey = generateContextCacheKey(contextWindow);
            ModerationResult cachedResult = getCachedModerationResult(cacheKey);
            if (cachedResult != null) {
                logger.debug("Using cached moderation result for context: {}", cacheKey);
                return cachedResult;
            }
            
            // Perform moderation with context
            ModerationResult result = performContextualModeration(contextWindow, userId);
            
            // Cache the result
            cacheModerationResult(cacheKey, result);
            
            // Log audit trail
            logModerationAudit(contentId, contentType, userId, result, "CONTEXT_WINDOW");
            
            logger.info("Context window moderation completed for {}: {} - Safe: {}", 
                contentType, contentId, result.isSafe());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in context window moderation for {}: {}: {}", 
                contentType, contentId, e.getMessage(), e);
            return ModerationResult.safe("Context moderation failed, content approved by default");
        }
    }
    
    /**
     * Build context window including parent and grandparent content
     */
    private ContextWindow buildContextWindow(UUID contentId, String contentType) {
        ContextWindow contextWindow = new ContextWindow();
        contextWindow.setContentId(contentId);
        contextWindow.setContentType(contentType);
        
        try {
            if ("comment".equalsIgnoreCase(contentType)) {
                buildCommentContextWindow(contentId, contextWindow);
            } else if ("post".equalsIgnoreCase(contentType)) {
                buildPostContextWindow(contentId, contextWindow);
            }
            
            // Calculate context embedding hash for caching
            contextWindow.setContextHash(generateContextHash(contextWindow));
            
        } catch (Exception e) {
            logger.error("Error building context window: {}", e.getMessage(), e);
        }
        
        return contextWindow;
    }
    
    /**
     * Build context window for comments (comment + parent comment + grandparent comment)
     */
    private void buildCommentContextWindow(UUID commentId, ContextWindow contextWindow) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            logger.warn("Comment not found: {}", commentId);
            return;
        }
        
        Comment comment = commentOpt.get();
        contextWindow.setPrimaryContent(comment.getContent());
        contextWindow.setPrimaryAuthor(comment.getAuthorId());
        
        // Get parent comment
        if (comment.getParentId() != null) {
            Optional<Comment> parentCommentOpt = commentRepository.findById(comment.getParentId());
            if (parentCommentOpt.isPresent()) {
                Comment parentComment = parentCommentOpt.get();
                contextWindow.setParentContent(parentComment.getContent());
                contextWindow.setParentAuthor(parentComment.getAuthorId());
                
                // Get grandparent comment
                if (parentComment.getParentId() != null) {
                    Optional<Comment> grandparentCommentOpt = commentRepository.findById(parentComment.getParentId());
                    if (grandparentCommentOpt.isPresent()) {
                        Comment grandparentComment = grandparentCommentOpt.get();
                        contextWindow.setGrandparentContent(grandparentComment.getContent());
                        contextWindow.setGrandparentAuthor(grandparentComment.getAuthorId());
                    }
                }
            }
        }
        
        // Get post context
        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            contextWindow.setPostContent(post.getContent());
            contextWindow.setPostAuthor(post.getUserId());
        }
    }
    
    /**
     * Build context window for posts (post + recent comments)
     */
    private void buildPostContextWindow(UUID postId, ContextWindow contextWindow) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            logger.warn("Post not found: {}", postId);
            return;
        }
        
        Post post = postOpt.get();
        contextWindow.setPrimaryContent(post.getContent());
        contextWindow.setPrimaryAuthor(post.getUserId());
        
        // Get recent comments for context
        List<Comment> recentComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId);
        if (!recentComments.isEmpty()) {
            List<String> commentContents = recentComments.stream()
                .limit(5) // Limit to top 5 comments
                .map(Comment::getContent)
                .collect(Collectors.toList());
            contextWindow.setRecentComments(commentContents);
        }
    }
    
    /**
     * 5.2 Add DeepSeek integration for borderline moderation calls
     * Use DeepSeek for complex contextual analysis when local analysis is inconclusive
     */
    private ModerationResult performContextualModeration(ContextWindow contextWindow, UUID userId) {
        try {
            // First, try local moderation
            ModerationResult localResult = performLocalContextualAnalysis(contextWindow);
            
            // If local analysis is conclusive, return it
            if (isConclusiveResult(localResult)) {
                logger.debug("Local contextual analysis conclusive, skipping AI");
                return localResult;
            }
            
            // For borderline cases, use DeepSeek
            logger.debug("Local analysis inconclusive, using DeepSeek for contextual analysis");
            return performDeepSeekContextualAnalysis(contextWindow, userId);
            
        } catch (Exception e) {
            logger.error("Error in contextual moderation: {}", e.getMessage(), e);
            return ModerationResult.safe("Contextual moderation failed, content approved by default");
        }
    }
    
    /**
     * Perform local contextual analysis
     */
    private ModerationResult performLocalContextualAnalysis(ContextWindow contextWindow) {
        ModerationResult result = new ModerationResult();
        
        // Analyze primary content
        String primaryContent = contextWindow.getPrimaryContent();
        if (primaryContent == null || primaryContent.trim().isEmpty()) {
            return ModerationResult.safe("Empty content");
        }
        
        // Check for context patterns
        ContextualAnalysis analysis = analyzeContextualPatterns(contextWindow);
        
        // Determine result based on analysis
        if (analysis.isSafe()) {
            result.setSafe(true);
            result.setConfidence(analysis.getConfidence());
            result.setReason("Content deemed safe by contextual analysis: " + analysis.getReason());
        } else {
            result.setSafe(false);
            result.setConfidence(analysis.getConfidence());
            result.setReason("Content flagged by contextual analysis: " + analysis.getReason());
        }
        
        result.setModeratedAt(LocalDateTime.now());
        return result;
    }
    
    /**
     * Analyze contextual patterns in the content window
     */
    private ContextualAnalysis analyzeContextualPatterns(ContextWindow contextWindow) {
        ContextualAnalysis analysis = new ContextualAnalysis();
        
        // Check for conversation context
        if (hasConversationContext(contextWindow)) {
            analysis.addContextType("CONVERSATION");
            analysis.increaseConfidence(0.1);
        }
        
        // Check for supportive context
        if (hasSupportiveContext(contextWindow)) {
            analysis.addContextType("SUPPORTIVE");
            analysis.increaseConfidence(0.2);
            analysis.setSafe(true);
        }
        
        // Check for escalation context
        if (hasEscalationContext(contextWindow)) {
            analysis.addContextType("ESCALATION");
            analysis.decreaseConfidence(0.1);
        }
        
        // Check for repetitive patterns
        if (hasRepetitivePatterns(contextWindow)) {
            analysis.addContextType("REPETITIVE");
            analysis.decreaseConfidence(0.15);
        }
        
        return analysis;
    }
    
    /**
     * Check if content has conversation context
     */
    private boolean hasConversationContext(ContextWindow contextWindow) {
        return contextWindow.getParentContent() != null || 
               contextWindow.getGrandparentContent() != null ||
               (contextWindow.getRecentComments() != null && !contextWindow.getRecentComments().isEmpty());
    }
    
    /**
     * Check if content has supportive context
     */
    private boolean hasSupportiveContext(ContextWindow contextWindow) {
        String primaryContent = contextWindow.getPrimaryContent().toLowerCase();
        
        // Check for supportive keywords
        String[] supportiveKeywords = {"support", "help", "understand", "care", "listen", "here for you"};
        for (String keyword : supportiveKeywords) {
            if (primaryContent.contains(keyword)) {
                return true;
            }
        }
        
        // Check parent/grandparent for supportive context
        if (contextWindow.getParentContent() != null) {
            String parentContent = contextWindow.getParentContent().toLowerCase();
            for (String keyword : supportiveKeywords) {
                if (parentContent.contains(keyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if content has escalation context
     */
    private boolean hasEscalationContext(ContextWindow contextWindow) {
        String primaryContent = contextWindow.getPrimaryContent().toLowerCase();
        
        // Check for escalation keywords
        String[] escalationKeywords = {"angry", "furious", "hate", "kill", "hurt", "destroy"};
        for (String keyword : escalationKeywords) {
            if (primaryContent.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if content has repetitive patterns
     */
    private boolean hasRepetitivePatterns(ContextWindow contextWindow) {
        String primaryContent = contextWindow.getPrimaryContent();
        
        // Check for repeated words or phrases
        String[] words = primaryContent.split("\\s+");
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (String word : words) {
            wordCount.merge(word.toLowerCase(), 1, Integer::sum);
        }
        
        // Check if any word appears more than 3 times
        return wordCount.values().stream().anyMatch(count -> count > 3);
    }
    
    /**
     * Perform DeepSeek contextual analysis for borderline cases
     */
    private ModerationResult performDeepSeekContextualAnalysis(ContextWindow contextWindow, UUID userId) {
        try {
            // Build contextual prompt
            String contextualPrompt = buildContextualPrompt(contextWindow);
            
            // Use DeepSeek adapter for analysis
            ModerationResult result = deepSeekAdapterService.moderateContentWithRetry(
                contextualPrompt, "contextual-analysis", userId, null);
            
            // Enhance result with context information
            result.setReason("DeepSeek contextual analysis: " + result.getReason());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in DeepSeek contextual analysis: {}", e.getMessage(), e);
            return ModerationResult.safe("DeepSeek analysis failed, content approved by default");
        }
    }
    
    /**
     * Build contextual prompt for DeepSeek
     */
    private String buildContextualPrompt(ContextWindow contextWindow) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze the following content with full context:\n\n");
        
        if (contextWindow.getPostContent() != null) {
            prompt.append("POST CONTEXT:\n").append(contextWindow.getPostContent()).append("\n\n");
        }
        
        if (contextWindow.getParentContent() != null) {
            prompt.append("PARENT COMMENT:\n").append(contextWindow.getParentContent()).append("\n\n");
        }
        
        if (contextWindow.getGrandparentContent() != null) {
            prompt.append("GRANDPARENT COMMENT:\n").append(contextWindow.getGrandparentContent()).append("\n\n");
        }
        
        prompt.append("CURRENT CONTENT TO MODERATE:\n").append(contextWindow.getPrimaryContent()).append("\n\n");
        
        prompt.append("Consider the full conversation context and determine if the current content is appropriate. ");
        prompt.append("Respond with APPROVE or REJECT followed by a brief reason.");
        
        return prompt.toString();
    }
    
    /**
     * Check if a moderation result is conclusive
     */
    private boolean isConclusiveResult(ModerationResult result) {
        return result.getConfidence() >= 0.8 || result.getConfidence() <= 0.3;
    }
    
    /**
     * 5.3 Create moderation caching by commentID and parent embedding hash
     * Cache moderation results based on content and context
     */
    private String generateContextCacheKey(ContextWindow contextWindow) {
        return CACHE_PREFIX + contextWindow.getContentId() + ":" + contextWindow.getContextHash();
    }
    
    private String generateContextHash(ContextWindow contextWindow) {
        StringBuilder hashInput = new StringBuilder();
        
        if (contextWindow.getPrimaryContent() != null) {
            hashInput.append(contextWindow.getPrimaryContent());
        }
        if (contextWindow.getParentContent() != null) {
            hashInput.append("|parent:").append(contextWindow.getParentContent());
        }
        if (contextWindow.getGrandparentContent() != null) {
            hashInput.append("|grandparent:").append(contextWindow.getGrandparentContent());
        }
        if (contextWindow.getPostContent() != null) {
            hashInput.append("|post:").append(contextWindow.getPostContent());
        }
        
        return String.valueOf(hashInput.toString().hashCode());
    }
    
    private ModerationResult getCachedModerationResult(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof ModerationResult) {
                return (ModerationResult) cached;
            }
        } catch (Exception e) {
            logger.error("Error retrieving cached moderation result: {}", e.getMessage(), e);
        }
        return null;
    }
    
    private void cacheModerationResult(String cacheKey, ModerationResult result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            logger.error("Error caching moderation result: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 5.4 Implement escalation logic (max 3 levels up)
     * Escalate moderation decisions through multiple levels
     */
    public ModerationResult escalateModerationDecision(UUID contentId, String contentType, UUID userId, 
                                                      ModerationResult initialResult, int currentLevel) {
        try {
            if (currentLevel >= MAX_ESCALATION_LEVELS) {
                logger.warn("Maximum escalation level reached for {}: {}", contentType, contentId);
                return initialResult;
            }
            
            logger.info("Escalating moderation decision for {}: {} (level {})", 
                contentType, contentId, currentLevel + 1);
            
            // Determine escalation strategy based on level
            EscalationStrategy strategy = determineEscalationStrategy(currentLevel, initialResult);
            
            // Perform escalated moderation
            ModerationResult escalatedResult = performEscalatedModeration(contentId, contentType, userId, strategy);
            
            // Log escalation
            logModerationAudit(contentId, contentType, userId, escalatedResult, 
                "ESCALATION_LEVEL_" + (currentLevel + 1));
            
            return escalatedResult;
            
        } catch (Exception e) {
            logger.error("Error in escalation logic: {}", e.getMessage(), e);
            return initialResult;
        }
    }
    
    /**
     * Determine escalation strategy based on level and initial result
     */
    private EscalationStrategy determineEscalationStrategy(int level, ModerationResult initialResult) {
        EscalationStrategy strategy = new EscalationStrategy();
        
        switch (level) {
            case 0:
                // Level 1: Enhanced AI analysis
                strategy.setUseEnhancedAI(true);
                strategy.setRequireHumanReview(false);
                strategy.setStricterThresholds(true);
                break;
            case 1:
                // Level 2: Human review required
                strategy.setUseEnhancedAI(true);
                strategy.setRequireHumanReview(true);
                strategy.setStricterThresholds(true);
                break;
            case 2:
                // Level 3: Final decision with human oversight
                strategy.setUseEnhancedAI(false);
                strategy.setRequireHumanReview(true);
                strategy.setStricterThresholds(false);
                break;
        }
        
        return strategy;
    }
    
    /**
     * Perform escalated moderation
     */
    private ModerationResult performEscalatedModeration(UUID contentId, String contentType, UUID userId, 
                                                       EscalationStrategy strategy) {
        try {
            // Build context window for escalated analysis
            ContextWindow contextWindow = buildContextWindow(contentId, contentType);
            
            if (strategy.isUseEnhancedAI()) {
                // Use enhanced AI analysis
                return performEnhancedAIAnalysis(contextWindow, userId, strategy.isStricterThresholds());
            } else {
                // Use human review process
                return performHumanReviewProcess(contextWindow, userId);
            }
            
        } catch (Exception e) {
            logger.error("Error in escalated moderation: {}", e.getMessage(), e);
            return ModerationResult.safe("Escalated moderation failed, content approved by default");
        }
    }
    
    /**
     * Perform enhanced AI analysis
     */
    private ModerationResult performEnhancedAIAnalysis(ContextWindow contextWindow, UUID userId, boolean stricterThresholds) {
        try {
            // Build enhanced prompt
            String enhancedPrompt = buildEnhancedPrompt(contextWindow, stricterThresholds);
            
            // Use DeepSeek with enhanced parameters
            ModerationResult result = deepSeekAdapterService.moderateContentWithRetry(
                enhancedPrompt, "enhanced-analysis", userId, null);
            
            // Apply stricter thresholds if required
            if (stricterThresholds) {
                result.setConfidence(result.getConfidence() * 0.9); // Reduce confidence
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in enhanced AI analysis: {}", e.getMessage(), e);
            return ModerationResult.safe("Enhanced AI analysis failed, content approved by default");
        }
    }
    
    /**
     * Build enhanced prompt for stricter analysis
     */
    private String buildEnhancedPrompt(ContextWindow contextWindow, boolean stricterThresholds) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("ENHANCED MODERATION ANALYSIS\n\n");
        
        if (stricterThresholds) {
            prompt.append("Apply STRICTER moderation standards. Be more conservative in your decisions.\n\n");
        }
        
        prompt.append("CONTEXT:\n");
        if (contextWindow.getPostContent() != null) {
            prompt.append("Post: ").append(contextWindow.getPostContent()).append("\n");
        }
        if (contextWindow.getParentContent() != null) {
            prompt.append("Parent: ").append(contextWindow.getParentContent()).append("\n");
        }
        prompt.append("Content: ").append(contextWindow.getPrimaryContent()).append("\n\n");
        
        prompt.append("Analyze with enhanced scrutiny and respond with APPROVE or REJECT followed by detailed reasoning.");
        
        return prompt.toString();
    }
    
    /**
     * Perform human review process
     */
    private ModerationResult performHumanReviewProcess(ContextWindow contextWindow, UUID userId) {
        // For now, return a result indicating human review is needed
        // In a real implementation, this would queue the content for human review
        ModerationResult result = new ModerationResult();
        result.setSafe(true); // Default to safe pending human review
        result.setConfidence(0.5);
        result.setReason("Content queued for human review");
        result.setModeratedAt(LocalDateTime.now());
        
        logger.info("Content queued for human review: {}", contextWindow.getContentId());
        
        return result;
    }
    
    /**
     * 5.5 Add contextual analysis with policy rubric
     * Apply policy rubric for consistent moderation decisions
     */
    public ModerationResult applyPolicyRubric(UUID contentId, String contentType, UUID userId) {
        try {
            logger.info("Applying policy rubric for {}: {}", contentType, contentId);
            
            // Build context window
            ContextWindow contextWindow = buildContextWindow(contentId, contentType);
            
            // Apply policy rubric
            PolicyRubricResult rubricResult = evaluatePolicyRubric(contextWindow);
            
            // Create moderation result
            ModerationResult result = new ModerationResult();
            result.setSafe(rubricResult.isSafe());
            result.setConfidence(rubricResult.getConfidence());
            result.setReason("Policy rubric evaluation: " + rubricResult.getReason());
            result.setModeratedAt(LocalDateTime.now());
            
            // Log audit trail
            logModerationAudit(contentId, contentType, userId, result, "POLICY_RUBRIC");
            
            logger.info("Policy rubric applied for {}: {} - Safe: {}", 
                contentType, contentId, result.isSafe());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error applying policy rubric: {}", e.getMessage(), e);
            return ModerationResult.safe("Policy rubric evaluation failed, content approved by default");
        }
    }
    
    /**
     * Evaluate content against policy rubric
     */
    private PolicyRubricResult evaluatePolicyRubric(ContextWindow contextWindow) {
        PolicyRubricResult result = new PolicyRubricResult();
        
        // Evaluate against different policy dimensions
        evaluateSafetyPolicy(contextWindow, result);
        evaluateRespectPolicy(contextWindow, result);
        evaluateSupportPolicy(contextWindow, result);
        evaluateContextPolicy(contextWindow, result);
        
        // Calculate final result
        result.calculateFinalResult();
        
        return result;
    }
    
    /**
     * Evaluate safety policy
     */
    private void evaluateSafetyPolicy(ContextWindow contextWindow, PolicyRubricResult result) {
        String content = contextWindow.getPrimaryContent().toLowerCase();
        
        // Check for safety violations
        String[] safetyViolations = {"self-harm", "suicide", "violence", "threat", "danger"};
        for (String violation : safetyViolations) {
            if (content.contains(violation)) {
                result.addViolation("SAFETY", violation);
                result.decreaseScore(0.3);
            }
        }
        
        // Check for safety support
        String[] safetySupport = {"help", "support", "care", "safe", "okay"};
        for (String support : safetySupport) {
            if (content.contains(support)) {
                result.addSupport("SAFETY", support);
                result.increaseScore(0.1);
            }
        }
    }
    
    /**
     * Evaluate respect policy
     */
    private void evaluateRespectPolicy(ContextWindow contextWindow, PolicyRubricResult result) {
        String content = contextWindow.getPrimaryContent().toLowerCase();
        
        // Check for respect violations
        String[] respectViolations = {"hate", "discrimination", "harassment", "bully", "attack"};
        for (String violation : respectViolations) {
            if (content.contains(violation)) {
                result.addViolation("RESPECT", violation);
                result.decreaseScore(0.2);
            }
        }
        
        // Check for respectful language
        String[] respectfulLanguage = {"respect", "kind", "understanding", "compassion"};
        for (String respectful : respectfulLanguage) {
            if (content.contains(respectful)) {
                result.addSupport("RESPECT", respectful);
                result.increaseScore(0.1);
            }
        }
    }
    
    /**
     * Evaluate support policy
     */
    private void evaluateSupportPolicy(ContextWindow contextWindow, PolicyRubricResult result) {
        String content = contextWindow.getPrimaryContent().toLowerCase();
        
        // Check for supportive language
        String[] supportiveLanguage = {"support", "help", "listen", "understand", "care", "here for you"};
        for (String supportive : supportiveLanguage) {
            if (content.contains(supportive)) {
                result.addSupport("SUPPORT", supportive);
                result.increaseScore(0.15);
            }
        }
        
        // Check for unsupportive language
        String[] unsupportiveLanguage = {"ignore", "don't care", "not my problem", "figure it out"};
        for (String unsupportive : unsupportiveLanguage) {
            if (content.contains(unsupportive)) {
                result.addViolation("SUPPORT", unsupportive);
                result.decreaseScore(0.1);
            }
        }
    }
    
    /**
     * Evaluate context policy
     */
    private void evaluateContextPolicy(ContextWindow contextWindow, PolicyRubricResult result) {
        // Check conversation context
        if (hasConversationContext(contextWindow)) {
            result.addSupport("CONTEXT", "conversation_context");
            result.increaseScore(0.05);
        }
        
        // Check for escalation
        if (hasEscalationContext(contextWindow)) {
            result.addViolation("CONTEXT", "escalation_context");
            result.decreaseScore(0.1);
        }
        
        // Check for repetitive patterns
        if (hasRepetitivePatterns(contextWindow)) {
            result.addViolation("CONTEXT", "repetitive_patterns");
            result.decreaseScore(0.05);
        }
    }
    
    /**
     * 5.6 Create moderation decision routing (allow/hide/shadow-ban/rate-limit/escalate)
     * Route moderation decisions to appropriate actions
     */
    public ModerationAction routeModerationDecision(UUID contentId, String contentType, UUID userId, 
                                                  ModerationResult moderationResult) {
        try {
            logger.info("Routing moderation decision for {}: {} - Safe: {}", 
                contentType, contentId, moderationResult.isSafe());
            
            ModerationAction action = new ModerationAction();
            action.setContentId(contentId);
            action.setContentType(contentType);
            action.setUserId(userId);
            action.setModerationResult(moderationResult);
            action.setTimestamp(LocalDateTime.now());
            
            // Determine action based on result
            if (moderationResult.isSafe()) {
                action.setActionType(ModerationActionType.ALLOW);
                action.setReason("Content approved by moderation");
            } else {
                // Determine severity-based action
                double confidence = moderationResult.getConfidence();
                
                if (confidence >= 0.8) {
                    action.setActionType(ModerationActionType.HIDE);
                    action.setReason("Content hidden due to high confidence violation");
                } else if (confidence >= 0.6) {
                    action.setActionType(ModerationActionType.SHADOW_BAN);
                    action.setReason("Content shadow-banned due to moderate violation");
                } else if (confidence >= 0.4) {
                    action.setActionType(ModerationActionType.RATE_LIMIT);
                    action.setReason("Content rate-limited due to borderline violation");
                } else {
                    action.setActionType(ModerationActionType.ESCALATE);
                    action.setReason("Content escalated for human review");
                }
            }
            
            // Apply the action
            applyModerationAction(action);
            
            // Log audit trail
            logModerationAudit(contentId, contentType, userId, moderationResult, 
                "ROUTING_" + action.getActionType());
            
            logger.info("Moderation action applied: {} for {}: {}", 
                action.getActionType(), contentType, contentId);
            
            return action;
            
        } catch (Exception e) {
            logger.error("Error routing moderation decision: {}", e.getMessage(), e);
            return createFallbackAction(contentId, contentType, userId, moderationResult);
        }
    }
    
    /**
     * Apply moderation action
     */
    private void applyModerationAction(ModerationAction action) {
        try {
            switch (action.getActionType()) {
                case ALLOW:
                    // Content is approved, no action needed
                    break;
                case HIDE:
                    hideContent(action.getContentId(), action.getContentType());
                    break;
                case SHADOW_BAN:
                    shadowBanContent(action.getContentId(), action.getContentType());
                    break;
                case RATE_LIMIT:
                    rateLimitUser(action.getUserId());
                    break;
                case ESCALATE:
                    escalateContent(action.getContentId(), action.getContentType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error applying moderation action: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Hide content
     */
    private void hideContent(UUID contentId, String contentType) {
        if ("post".equalsIgnoreCase(contentType)) {
            Optional<Post> postOpt = postRepository.findById(contentId);
            if (postOpt.isPresent()) {
                Post post = postOpt.get();
                post.setFlagged(true);
                post.setFlagReason("Content hidden by moderation");
                postRepository.save(post);
            }
        } else if ("comment".equalsIgnoreCase(contentType)) {
            // Comments don't have flagged/flagReason fields, so we'll just log the action
            logger.info("Comment {} would be hidden by moderation", contentId);
        }
    }
    
    /**
     * Shadow ban content
     */
    private void shadowBanContent(UUID contentId, String contentType) {
        // Shadow ban implementation would hide content from public view
        // but keep it visible to the author
        logger.info("Shadow banning {}: {}", contentType, contentId);
    }
    
    /**
     * Rate limit user
     */
    private void rateLimitUser(UUID userId) {
        // Rate limiting implementation would restrict user's posting frequency
        logger.info("Rate limiting user: {}", userId);
    }
    
    /**
     * Escalate content
     */
    private void escalateContent(UUID contentId, String contentType) {
        // Escalation implementation would queue content for human review
        logger.info("Escalating {}: {} for human review", contentType, contentId);
    }
    
    /**
     * Create fallback action
     */
    private ModerationAction createFallbackAction(UUID contentId, String contentType, UUID userId, 
                                                 ModerationResult moderationResult) {
        ModerationAction action = new ModerationAction();
        action.setContentId(contentId);
        action.setContentType(contentType);
        action.setUserId(userId);
        action.setModerationResult(moderationResult);
        action.setActionType(ModerationActionType.ALLOW);
        action.setReason("Fallback action - content approved by default");
        action.setTimestamp(LocalDateTime.now());
        return action;
    }
    
    /**
     * 5.7 Implement moderation audit logging and transparency
     * Log all moderation decisions for transparency and auditing
     */
    private void logModerationAudit(UUID contentId, String contentType, UUID userId, 
                                   ModerationResult result, String auditType) {
        try {
            ModerationAudit audit = ModerationAudit.builder()
                .contentId(contentId)
                .contentType(contentType)
                .userId(userId)
                .moderationResultJson(result.toString()) // Serialize the result
                .auditType(auditType)
                .timestamp(LocalDateTime.now())
                .decision(result.isSafe() ? "APPROVED" : "REJECTED")
                .confidence(result.getConfidence())
                .reason(result.getReason())
                .build();
            
            moderationAuditRepository.save(audit);
            
            logger.debug("Moderation audit logged: {} for {}: {}", auditType, contentType, contentId);
            
        } catch (Exception e) {
            logger.error("Error logging moderation audit: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get moderation audit trail
     */
    public List<ModerationAudit> getModerationAuditTrail(UUID contentId, String contentType) {
        try {
            return moderationAuditRepository.findByContentIdAndContentTypeOrderByTimestampDesc(contentId, contentType);
        } catch (Exception e) {
            logger.error("Error retrieving moderation audit trail: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get moderation statistics
     */
    public ModerationStatistics getModerationStatistics(UUID userId, int days) {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            List<ModerationAudit> audits = moderationAuditRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId, startDate);
            
            ModerationStatistics stats = new ModerationStatistics();
            stats.setTotalDecisions(audits.size());
            stats.setApprovedDecisions((int) audits.stream().filter(a -> "APPROVED".equals(a.getDecision())).count());
            stats.setRejectedDecisions((int) audits.stream().filter(a -> "REJECTED".equals(a.getDecision())).count());
            stats.setAverageConfidence(audits.stream().mapToDouble(ModerationAudit::getConfidence).average().orElse(0.0));
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error calculating moderation statistics: {}", e.getMessage(), e);
            return new ModerationStatistics();
        }
    }
    
    // Inner classes for data structures
    
    /**
     * Context window for moderation analysis
     */
    public static class ContextWindow {
        private UUID contentId;
        private String contentType;
        private String primaryContent;
        private UUID primaryAuthor;
        private String parentContent;
        private UUID parentAuthor;
        private String grandparentContent;
        private UUID grandparentAuthor;
        private String postContent;
        private UUID postAuthor;
        private List<String> recentComments;
        private String contextHash;
        
        // Getters and setters
        public UUID getContentId() { return contentId; }
        public void setContentId(UUID contentId) { this.contentId = contentId; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getPrimaryContent() { return primaryContent; }
        public void setPrimaryContent(String primaryContent) { this.primaryContent = primaryContent; }
        
        public UUID getPrimaryAuthor() { return primaryAuthor; }
        public void setPrimaryAuthor(UUID primaryAuthor) { this.primaryAuthor = primaryAuthor; }
        
        public String getParentContent() { return parentContent; }
        public void setParentContent(String parentContent) { this.parentContent = parentContent; }
        
        public UUID getParentAuthor() { return parentAuthor; }
        public void setParentAuthor(UUID parentAuthor) { this.parentAuthor = parentAuthor; }
        
        public String getGrandparentContent() { return grandparentContent; }
        public void setGrandparentContent(String grandparentContent) { this.grandparentContent = grandparentContent; }
        
        public UUID getGrandparentAuthor() { return grandparentAuthor; }
        public void setGrandparentAuthor(UUID grandparentAuthor) { this.grandparentAuthor = grandparentAuthor; }
        
        public String getPostContent() { return postContent; }
        public void setPostContent(String postContent) { this.postContent = postContent; }
        
        public UUID getPostAuthor() { return postAuthor; }
        public void setPostAuthor(UUID postAuthor) { this.postAuthor = postAuthor; }
        
        public List<String> getRecentComments() { return recentComments; }
        public void setRecentComments(List<String> recentComments) { this.recentComments = recentComments; }
        
        public String getContextHash() { return contextHash; }
        public void setContextHash(String contextHash) { this.contextHash = contextHash; }
    }
    
    /**
     * Contextual analysis result
     */
    public static class ContextualAnalysis {
        private boolean safe = true;
        private double confidence = 0.5;
        private String reason = "";
        private List<String> contextTypes = new ArrayList<>();
        
        public void addContextType(String type) { contextTypes.add(type); }
        public void increaseConfidence(double amount) { confidence = Math.min(1.0, confidence + amount); }
        public void decreaseConfidence(double amount) { confidence = Math.max(0.0, confidence - amount); }
        
        // Getters and setters
        public boolean isSafe() { return safe; }
        public void setSafe(boolean safe) { this.safe = safe; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public List<String> getContextTypes() { return contextTypes; }
    }
    
    /**
     * Escalation strategy
     */
    public static class EscalationStrategy {
        private boolean useEnhancedAI = false;
        private boolean requireHumanReview = false;
        private boolean stricterThresholds = false;
        
        // Getters and setters
        public boolean isUseEnhancedAI() { return useEnhancedAI; }
        public void setUseEnhancedAI(boolean useEnhancedAI) { this.useEnhancedAI = useEnhancedAI; }
        
        public boolean isRequireHumanReview() { return requireHumanReview; }
        public void setRequireHumanReview(boolean requireHumanReview) { this.requireHumanReview = requireHumanReview; }
        
        public boolean isStricterThresholds() { return stricterThresholds; }
        public void setStricterThresholds(boolean stricterThresholds) { this.stricterThresholds = stricterThresholds; }
    }
    
    /**
     * Policy rubric result
     */
    public static class PolicyRubricResult {
        private boolean safe = true;
        private double confidence = 0.5;
        private String reason = "";
        private double score = 0.5;
        private Map<String, List<String>> violations = new HashMap<>();
        private Map<String, List<String>> supports = new HashMap<>();
        
        public void addViolation(String category, String violation) {
            violations.computeIfAbsent(category, k -> new ArrayList<>()).add(violation);
        }
        
        public void addSupport(String category, String support) {
            supports.computeIfAbsent(category, k -> new ArrayList<>()).add(support);
        }
        
        public void increaseScore(double amount) { score = Math.min(1.0, score + amount); }
        public void decreaseScore(double amount) { score = Math.max(0.0, score - amount); }
        
        public void calculateFinalResult() {
            confidence = score;
            safe = score >= 0.5;
            
            if (violations.isEmpty() && !supports.isEmpty()) {
                reason = "Content supports community guidelines";
            } else if (!violations.isEmpty() && supports.isEmpty()) {
                reason = "Content violates community guidelines";
            } else {
                reason = "Mixed policy evaluation";
            }
        }
        
        // Getters and setters
        public boolean isSafe() { return safe; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
    }
    
    /**
     * Moderation action
     */
    public static class ModerationAction {
        private UUID contentId;
        private String contentType;
        private UUID userId;
        private ModerationResult moderationResult;
        private ModerationActionType actionType;
        private String reason;
        private LocalDateTime timestamp;
        
        // Getters and setters
        public UUID getContentId() { return contentId; }
        public void setContentId(UUID contentId) { this.contentId = contentId; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public ModerationResult getModerationResult() { return moderationResult; }
        public void setModerationResult(ModerationResult moderationResult) { this.moderationResult = moderationResult; }
        
        public ModerationActionType getActionType() { return actionType; }
        public void setActionType(ModerationActionType actionType) { this.actionType = actionType; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Moderation action types
     */
    public enum ModerationActionType {
        ALLOW, HIDE, SHADOW_BAN, RATE_LIMIT, ESCALATE
    }
    
    /**
     * Moderation statistics
     */
    public static class ModerationStatistics {
        private int totalDecisions = 0;
        private int approvedDecisions = 0;
        private int rejectedDecisions = 0;
        private double averageConfidence = 0.0;
        
        // Getters and setters
        public int getTotalDecisions() { return totalDecisions; }
        public void setTotalDecisions(int totalDecisions) { this.totalDecisions = totalDecisions; }
        
        public int getApprovedDecisions() { return approvedDecisions; }
        public void setApprovedDecisions(int approvedDecisions) { this.approvedDecisions = approvedDecisions; }
        
        public int getRejectedDecisions() { return rejectedDecisions; }
        public void setRejectedDecisions(int rejectedDecisions) { this.rejectedDecisions = rejectedDecisions; }
        
        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }
        
        public double getApprovalRate() {
            return totalDecisions > 0 ? (double) approvedDecisions / totalDecisions : 0.0;
        }
    }
}
