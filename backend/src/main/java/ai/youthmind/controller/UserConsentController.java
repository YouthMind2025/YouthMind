package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.entity.UserConsent;
import ai.youthmind.entity.UserConsent.ConsentStatus;
import ai.youthmind.service.UserConsentService;
import ai.youthmind.service.WordMaskingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for handling user consent requests for masked content.
 */
@RestController
@RequestMapping("/v1/consent")
@Tag(name = "User Consent", description = "User consent management for masked content")
public class UserConsentController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserConsentController.class);
    
    @Autowired
    private UserConsentService userConsentService;
    
    @Autowired
    private WordMaskingService wordMaskingService;
    
    /**
     * Get pending consent requests for the authenticated user
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending consent requests", description = "Get all pending consent requests for the authenticated user")
    public ResponseEntity<ApiResponse<List<UserConsent>>> getPendingConsentRequests(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            List<UserConsent> pendingRequests = userConsentService.getPendingConsentRequests(userId);
            
            return ResponseEntity.ok(ApiResponse.<List<UserConsent>>builder()
                .success(true)
                .message("Pending consent requests retrieved successfully")
                .data(pendingRequests)
                .build());
                
        } catch (Exception e) {
            logger.error("Error getting pending consent requests: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<UserConsent>>builder()
                .success(false)
                .message("Failed to retrieve pending consent requests: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Get consent history for the authenticated user
     */
    @GetMapping("/history")
    @Operation(summary = "Get consent history", description = "Get consent history for the authenticated user")
    public ResponseEntity<ApiResponse<List<UserConsent>>> getConsentHistory(
            Authentication authentication,
            @Parameter(description = "Maximum number of records to return") 
            @RequestParam(defaultValue = "50") int limit) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            List<UserConsent> history = userConsentService.getConsentHistory(userId, limit);
            
            return ResponseEntity.ok(ApiResponse.<List<UserConsent>>builder()
                .success(true)
                .message("Consent history retrieved successfully")
                .data(history)
                .build());
                
        } catch (Exception e) {
            logger.error("Error getting consent history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<UserConsent>>builder()
                .success(false)
                .message("Failed to retrieve consent history: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Process user's consent decision
     */
    @PostMapping("/{consentId}/decision")
    @Operation(summary = "Process consent decision", description = "Process user's decision on a consent request")
    public ResponseEntity<ApiResponse<UserConsent>> processConsentDecision(
            @PathVariable @Parameter(description = "Consent request ID", required = true) UUID consentId,
            @RequestBody ConsentDecisionRequest request,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            UserConsent updatedConsent = userConsentService.processConsentDecision(
                consentId, userId, request.getDecision(), 
                request.getModifiedContent(), request.getMaskingStrategy());
            
            return ResponseEntity.ok(ApiResponse.<UserConsent>builder()
                .success(true)
                .message("Consent decision processed successfully")
                .data(updatedConsent)
                .build());
                
        } catch (Exception e) {
            logger.error("Error processing consent decision: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<UserConsent>builder()
                .success(false)
                .message("Failed to process consent decision: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Check if user can proceed with specific content
     */
    @GetMapping("/check/{contentId}")
    @Operation(summary = "Check content consent", description = "Check if user can proceed with specific content")
    public ResponseEntity<ApiResponse<ConsentCheckResponse>> checkContentConsent(
            @PathVariable @Parameter(description = "Content ID", required = true) UUID contentId,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            boolean canProceed = userConsentService.canUserProceedWithContent(userId, contentId);
            String finalContent = userConsentService.getFinalContent(userId, contentId).orElse(null);
            
            ConsentCheckResponse response = new ConsentCheckResponse(canProceed, finalContent);
            
            return ResponseEntity.ok(ApiResponse.<ConsentCheckResponse>builder()
                .success(true)
                .message("Content consent check completed")
                .data(response)
                .build());
                
        } catch (Exception e) {
            logger.error("Error checking content consent: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<ConsentCheckResponse>builder()
                .success(false)
                .message("Failed to check content consent: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Get consent statistics for the authenticated user
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get consent statistics", description = "Get consent statistics for the authenticated user")
    public ResponseEntity<ApiResponse<UserConsentService.ConsentStatistics>> getConsentStatistics(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserConsentService.ConsentStatistics statistics = userConsentService.getConsentStatistics(userId);
            
            return ResponseEntity.ok(ApiResponse.<UserConsentService.ConsentStatistics>builder()
                .success(true)
                .message("Consent statistics retrieved successfully")
                .data(statistics)
                .build());
                
        } catch (Exception e) {
            logger.error("Error getting consent statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<UserConsentService.ConsentStatistics>builder()
                .success(false)
                .message("Failed to retrieve consent statistics: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Get alternative words for a profane word
     */
    @GetMapping("/alternatives/{word}")
    @Operation(summary = "Get alternative words", description = "Get alternative words for a profane word")
    public ResponseEntity<ApiResponse<List<String>>> getAlternativeWords(
            @PathVariable @Parameter(description = "Word to get alternatives for", required = true) String word) {
        try {
            List<String> alternatives = wordMaskingService.getAlternativeWords(word);
            
            return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Alternative words retrieved successfully")
                .data(alternatives)
                .build());
                
        } catch (Exception e) {
            logger.error("Error getting alternative words: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<String>>builder()
                .success(false)
                .message("Failed to retrieve alternative words: " + e.getMessage())
                .build());
        }
    }
    
    /**
     * Request DTO for consent decision
     */
    public static class ConsentDecisionRequest {
        private ConsentStatus decision;
        private String modifiedContent;
        private WordMaskingService.MaskingStrategy maskingStrategy;
        
        // Getters and setters
        public ConsentStatus getDecision() { return decision; }
        public void setDecision(ConsentStatus decision) { this.decision = decision; }
        
        public String getModifiedContent() { return modifiedContent; }
        public void setModifiedContent(String modifiedContent) { this.modifiedContent = modifiedContent; }
        
        public WordMaskingService.MaskingStrategy getMaskingStrategy() { return maskingStrategy; }
        public void setMaskingStrategy(WordMaskingService.MaskingStrategy maskingStrategy) { this.maskingStrategy = maskingStrategy; }
    }
    
    /**
     * Response DTO for consent check
     */
    public static class ConsentCheckResponse {
        private boolean canProceed;
        private String finalContent;
        
        public ConsentCheckResponse(boolean canProceed, String finalContent) {
            this.canProceed = canProceed;
            this.finalContent = finalContent;
        }
        
        // Getters
        public boolean isCanProceed() { return canProceed; }
        public String getFinalContent() { return finalContent; }
    }
}
