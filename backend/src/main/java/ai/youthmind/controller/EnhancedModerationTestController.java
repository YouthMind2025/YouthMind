package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for testing the enhanced AI moderation system.
 * Provides endpoints to test different layers of moderation.
 */
@RestController
@RequestMapping("/v1/test/moderation")
@Tag(name = "Enhanced Moderation Testing", description = "Test endpoints for the enhanced AI moderation system")
public class EnhancedModerationTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedModerationTestController.class);
    
    @Autowired
    private LocalModerationService localModerationService;
    
    @Autowired
    private ContextAwareModerationService contextAwareModerationService;
    
    @Autowired
    private WordMaskingService wordMaskingService;
    
    @Autowired
    private ToxicityOverloadPreventionService toxicityOverloadPreventionService;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    /**
     * Test local dictionary-based moderation
     */
    @PostMapping("/local")
    @Operation(summary = "Test local moderation", description = "Test the local dictionary-based moderation service")
    public ResponseEntity<ApiResponse<LocalModerationService.LocalModerationAnalysis>> testLocalModeration(
            @RequestBody TestContentRequest request) {
        try {
            LocalModerationService.LocalModerationAnalysis analysis = localModerationService.analyzeContent(request.getContent());
            
            return ResponseEntity.ok(ApiResponse.<LocalModerationService.LocalModerationAnalysis>builder()
                .success(true)
                .message("Local moderation analysis completed")
                .data(analysis)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing local moderation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<LocalModerationService.LocalModerationAnalysis>builder()
                .success(false)
                .message("Failed to test local moderation: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Test context-aware moderation
     */
    @PostMapping("/context")
    @Operation(summary = "Test context-aware moderation", description = "Test the context-aware moderation service")
    public ResponseEntity<ApiResponse<ContextAwareModerationService.ContextAwareAnalysis>> testContextAwareModeration(
            @RequestBody TestContentRequest request) {
        try {
            ContextAwareModerationService.ContextAwareAnalysis analysis = contextAwareModerationService.analyzeWithContext(request.getContent());
            
            return ResponseEntity.ok(ApiResponse.<ContextAwareModerationService.ContextAwareAnalysis>builder()
                .success(true)
                .message("Context-aware moderation analysis completed")
                .data(analysis)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing context-aware moderation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<ContextAwareModerationService.ContextAwareAnalysis>builder()
                .success(false)
                .message("Failed to test context-aware moderation: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Test word masking
     */
    @PostMapping("/masking")
    @Operation(summary = "Test word masking", description = "Test the word masking service")
    public ResponseEntity<ApiResponse<WordMaskingService.MaskingResult>> testWordMasking(
            @RequestBody TestMaskingRequest request) {
        try {
            WordMaskingService.MaskingResult result = wordMaskingService.maskContent(
                request.getContent(), request.getStrategy());
            
            return ResponseEntity.ok(ApiResponse.<WordMaskingService.MaskingResult>builder()
                .success(true)
                .message("Word masking completed")
                .data(result)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing word masking: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<WordMaskingService.MaskingResult>builder()
                .success(false)
                .message("Failed to test word masking: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Test toxicity analysis
     */
    @GetMapping("/toxicity/{userId}")
    @Operation(summary = "Test toxicity analysis", description = "Test the toxicity overload prevention service")
    public ResponseEntity<ApiResponse<ToxicityOverloadPreventionService.ToxicityAnalysis>> testToxicityAnalysis(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        try {
            ToxicityOverloadPreventionService.ToxicityAnalysis analysis = toxicityOverloadPreventionService.analyzeUserToxicity(userId);
            
            return ResponseEntity.ok(ApiResponse.<ToxicityOverloadPreventionService.ToxicityAnalysis>builder()
                .success(true)
                .message("Toxicity analysis completed")
                .data(analysis)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing toxicity analysis: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<ToxicityOverloadPreventionService.ToxicityAnalysis>builder()
                .success(false)
                .message("Failed to test toxicity analysis: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Test enhanced AI moderation with all layers
     */
    @PostMapping("/enhanced")
    @Operation(summary = "Test enhanced AI moderation", description = "Test the complete enhanced AI moderation system")
    public ResponseEntity<ApiResponse<EnhancedModerationTestResult>> testEnhancedModeration(
            @RequestBody TestContentRequest request,
            Authentication authentication) {
        try {
            UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : UUID.randomUUID();
            
            // Layer 1: Local moderation
            LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(request.getContent());
            
            // Layer 2: Context-aware analysis
            ContextAwareModerationService.ContextAwareAnalysis contextAnalysis = contextAwareModerationService.analyzeWithContext(request.getContent());
            
            // Layer 3: Toxicity analysis
            ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis = toxicityOverloadPreventionService.analyzeUserToxicity(userId);
            ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck = toxicityOverloadPreventionService.checkToxicityLimits(userId);
            
            // Layer 4: AI moderation (if needed)
            ModerationResult aiResult = null;
            boolean aiModerationUsed = false;
            
            // Determine if AI moderation is needed
            boolean needsAI = shouldUseAIModeration(localAnalysis, contextAnalysis, toxicityAnalysis, limitCheck);
            
            if (needsAI) {
                aiResult = aiServiceClient.moderateContentWithContext(
                    request.getContent(), "test", userId, null, localAnalysis, contextAnalysis);
                aiModerationUsed = true;
            }
            
            // Layer 5: Word masking
            WordMaskingService.MaskingResult maskingResult = wordMaskingService.maskContent(
                request.getContent(), WordMaskingService.MaskingStrategy.ASTERISK_MASKING);
            
            // Create comprehensive test result
            EnhancedModerationTestResult result = new EnhancedModerationTestResult(
                localAnalysis, contextAnalysis, toxicityAnalysis, limitCheck, 
                aiResult, aiModerationUsed, maskingResult
            );
            
            return ResponseEntity.ok(ApiResponse.<EnhancedModerationTestResult>builder()
                .success(true)
                .message("Enhanced moderation test completed")
                .data(result)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing enhanced moderation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<EnhancedModerationTestResult>builder()
                .success(false)
                .message("Failed to test enhanced moderation: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Test Gen Z language tolerance
     */
    @PostMapping("/gen-z")
    @Operation(summary = "Test Gen Z language tolerance", description = "Test Gen Z language patterns and tolerance")
    public ResponseEntity<ApiResponse<GenZLanguageTestResult>> testGenZLanguage(
            @RequestBody TestContentRequest request) {
        try {
            // Test various Gen Z expressions
            List<String> testPhrases = Arrays.asList(
                "no cap that's fire",
                "I did fucking well on my test",
                "that's so fucking awesome",
                "ngl this is lit",
                "periodt queen",
                "I'm stanning this vibe",
                "this gives me energy",
                "mood af"
            );
            
            List<GenZLanguageTestResult.PhraseResult> results = new ArrayList<>();
            
            for (String phrase : testPhrases) {
                LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(phrase);
                ContextAwareModerationService.ContextAwareAnalysis contextAnalysis = contextAwareModerationService.analyzeWithContext(phrase);
                
                results.add(new GenZLanguageTestResult.PhraseResult(
                    phrase, localAnalysis.isSafe(), contextAnalysis.isSafe(), 
                    contextAnalysis.getContextTypes()
                ));
            }
            
            GenZLanguageTestResult result = new GenZLanguageTestResult(results);
            
            return ResponseEntity.ok(ApiResponse.<GenZLanguageTestResult>builder()
                .success(true)
                .message("Gen Z language tolerance test completed")
                .data(result)
                .build());
                
        } catch (Exception e) {
            logger.error("Error testing Gen Z language: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<GenZLanguageTestResult>builder()
                .success(false)
                .message("Failed to test Gen Z language: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Determine if AI moderation should be used (copied from PostModerationService)
     */
    private boolean shouldUseAIModeration(LocalModerationService.LocalModerationAnalysis localAnalysis, 
                                        ContextAwareModerationService.ContextAwareAnalysis contextAnalysis,
                                        ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis,
                                        ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck) {
        // If toxicity limits exceeded, always use AI for strict moderation
        if (limitCheck.isLimitsExceeded()) {
            return true;
        }
        
        // If user has high toxicity, use AI for stricter moderation
        if (toxicityAnalysis.isHighToxicity()) {
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
        
        // For borderline cases, use AI
        return localAnalysis.getMaxSeverity() >= 2 && contextAnalysis.getConfidence() < 0.8;
    }
    
    /**
     * Request DTO for test content
     */
    public static class TestContentRequest {
        private String content;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    /**
     * Request DTO for test masking
     */
    public static class TestMaskingRequest {
        private String content;
        private WordMaskingService.MaskingStrategy strategy;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public WordMaskingService.MaskingStrategy getStrategy() { return strategy; }
        public void setStrategy(WordMaskingService.MaskingStrategy strategy) { this.strategy = strategy; }
    }
    
    /**
     * Result DTO for enhanced moderation test
     */
    public static class EnhancedModerationTestResult {
        private final LocalModerationService.LocalModerationAnalysis localAnalysis;
        private final ContextAwareModerationService.ContextAwareAnalysis contextAnalysis;
        private final ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis;
        private final ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck;
        private final ModerationResult aiResult;
        private final boolean aiModerationUsed;
        private final WordMaskingService.MaskingResult maskingResult;
        
        public EnhancedModerationTestResult(LocalModerationService.LocalModerationAnalysis localAnalysis,
                                         ContextAwareModerationService.ContextAwareAnalysis contextAnalysis,
                                         ToxicityOverloadPreventionService.ToxicityAnalysis toxicityAnalysis,
                                         ToxicityOverloadPreventionService.ToxicityLimitCheck limitCheck,
                                         ModerationResult aiResult, boolean aiModerationUsed,
                                         WordMaskingService.MaskingResult maskingResult) {
            this.localAnalysis = localAnalysis;
            this.contextAnalysis = contextAnalysis;
            this.toxicityAnalysis = toxicityAnalysis;
            this.limitCheck = limitCheck;
            this.aiResult = aiResult;
            this.aiModerationUsed = aiModerationUsed;
            this.maskingResult = maskingResult;
        }
        
        // Getters
        public LocalModerationService.LocalModerationAnalysis getLocalAnalysis() { return localAnalysis; }
        public ContextAwareModerationService.ContextAwareAnalysis getContextAnalysis() { return contextAnalysis; }
        public ToxicityOverloadPreventionService.ToxicityAnalysis getToxicityAnalysis() { return toxicityAnalysis; }
        public ToxicityOverloadPreventionService.ToxicityLimitCheck getLimitCheck() { return limitCheck; }
        public ModerationResult getAiResult() { return aiResult; }
        public boolean isAiModerationUsed() { return aiModerationUsed; }
        public WordMaskingService.MaskingResult getMaskingResult() { return maskingResult; }
    }
    
    /**
     * Result DTO for Gen Z language test
     */
    public static class GenZLanguageTestResult {
        private final List<PhraseResult> phraseResults;
        
        public GenZLanguageTestResult(List<PhraseResult> phraseResults) {
            this.phraseResults = phraseResults;
        }
        
        public List<PhraseResult> getPhraseResults() { return phraseResults; }
        
        public static class PhraseResult {
            private final String phrase;
            private final boolean localSafe;
            private final boolean contextSafe;
            private final List<String> contextTypes;
            
            public PhraseResult(String phrase, boolean localSafe, boolean contextSafe, List<String> contextTypes) {
                this.phrase = phrase;
                this.localSafe = localSafe;
                this.contextSafe = contextSafe;
                this.contextTypes = contextTypes;
            }
            
            // Getters
            public String getPhrase() { return phrase; }
            public boolean isLocalSafe() { return localSafe; }
            public boolean isContextSafe() { return contextSafe; }
            public List<String> getContextTypes() { return contextTypes; }
        }
    }
}
