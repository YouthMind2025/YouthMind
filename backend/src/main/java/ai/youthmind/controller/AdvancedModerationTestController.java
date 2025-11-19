package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.entity.ModerationAudit;
import ai.youthmind.service.AdvancedModerationPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Test controller for Advanced Moderation Pipeline
 */
@RestController
@RequestMapping("/api/test/advanced-moderation")
public class AdvancedModerationTestController {
    
    @Autowired
    private AdvancedModerationPipelineService advancedModerationPipelineService;
    
    /**
     * Test 5.1: Context window checks (comment + parent + grandparent)
     */
    @PostMapping("/context-window")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testContextWindow(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            var result = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("moderationResult", result);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Context window test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.2: DeepSeek integration for borderline moderation calls
     */
    @PostMapping("/deepseek-borderline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testDeepSeekBorderline(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            // Test borderline content that should trigger DeepSeek
            var result = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("moderationResult", result);
            response.put("deepseekUsed", result.getReason().contains("DeepSeek"));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "DeepSeek borderline test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.3: Moderation caching by commentID and parent embedding hash
     */
    @PostMapping("/caching")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testModerationCaching(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // First request - should cache the result
            var result1 = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            long firstRequestTime = System.currentTimeMillis() - startTime;
            
            // Second request - should use cache
            startTime = System.currentTimeMillis();
            var result2 = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            long secondRequestTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("firstRequest", Map.of(
                "result", result1,
                "processingTimeMs", firstRequestTime
            ));
            response.put("secondRequest", Map.of(
                "result", result2,
                "processingTimeMs", secondRequestTime
            ));
            response.put("cacheEffectiveness", secondRequestTime < firstRequestTime ? "Cache working" : "Cache not working");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Moderation caching test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.4: Escalation logic (max 3 levels up)
     */
    @PostMapping("/escalation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testEscalationLogic(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int escalationLevel) {
        
        try {
            // Create a mock initial result that needs escalation
            var initialResult = new ai.youthmind.dto.ModerationResult();
            initialResult.setSafe(false);
            initialResult.setConfidence(0.4); // Low confidence triggers escalation
            initialResult.setReason("Initial moderation result requiring escalation");
            
            var escalatedResult = advancedModerationPipelineService.escalateModerationDecision(
                contentId, contentType, userId, initialResult, escalationLevel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("initialResult", initialResult);
            response.put("escalatedResult", escalatedResult);
            response.put("escalationLevel", escalationLevel);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Escalation logic test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            error.put("escalationLevel", escalationLevel);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.5: Contextual analysis with policy rubric
     */
    @PostMapping("/policy-rubric")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testPolicyRubric(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            var result = advancedModerationPipelineService.applyPolicyRubric(contentId, contentType, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("policyRubricResult", result);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Policy rubric test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.6: Moderation decision routing (allow/hide/shadow-ban/rate-limit/escalate)
     */
    @PostMapping("/decision-routing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testDecisionRouting(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            // Create a mock moderation result
            var moderationResult = new ai.youthmind.dto.ModerationResult();
            moderationResult.setSafe(false);
            moderationResult.setConfidence(0.7); // Should trigger HIDE action
            moderationResult.setReason("Test content for decision routing");
            
            var action = advancedModerationPipelineService.routeModerationDecision(
                contentId, contentType, userId, moderationResult);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("moderationResult", moderationResult);
            response.put("moderationAction", action);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Decision routing test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Test 5.7: Moderation audit logging and transparency
     */
    @PostMapping("/audit-logging")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testAuditLogging(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            // Perform moderation to generate audit logs
            var result = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            
            // Get audit trail
            var auditTrail = advancedModerationPipelineService.getModerationAuditTrail(contentId, contentType);
            
            // Get moderation statistics
            var statistics = advancedModerationPipelineService.getModerationStatistics(userId, 7);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("userId", userId);
            response.put("moderationResult", result);
            response.put("auditTrail", auditTrail);
            response.put("statistics", statistics);
            response.put("auditCount", auditTrail.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Audit logging test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Comprehensive integration test for all Advanced Moderation Pipeline features
     */
    @PostMapping("/integration-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testIntegration(
            @RequestParam UUID contentId,
            @RequestParam String contentType,
            @RequestParam UUID userId) {
        
        try {
            Map<String, Object> results = new HashMap<>();
            results.put("contentId", contentId);
            results.put("contentType", contentType);
            results.put("userId", userId);
            results.put("timestamp", LocalDateTime.now());
            
            // Test 1: Context window moderation
            var contextResult = advancedModerationPipelineService.moderateWithContextWindow(contentId, contentType, userId);
            results.put("contextWindowResult", contextResult);
            
            // Test 2: Policy rubric application
            var policyResult = advancedModerationPipelineService.applyPolicyRubric(contentId, contentType, userId);
            results.put("policyRubricResult", policyResult);
            
            // Test 3: Decision routing
            var action = advancedModerationPipelineService.routeModerationDecision(contentId, contentType, userId, contextResult);
            results.put("decisionRouting", action);
            
            // Test 4: Escalation (if needed)
            if (!contextResult.isSafe() && contextResult.getConfidence() < 0.5) {
                var escalatedResult = advancedModerationPipelineService.escalateModerationDecision(
                    contentId, contentType, userId, contextResult, 0);
                results.put("escalationResult", escalatedResult);
            }
            
            // Test 5: Audit trail
            var auditTrail = advancedModerationPipelineService.getModerationAuditTrail(contentId, contentType);
            results.put("auditTrail", auditTrail);
            
            // Test 6: Statistics
            var statistics = advancedModerationPipelineService.getModerationStatistics(userId, 7);
            results.put("statistics", statistics);
            
            results.put("integrationTestStatus", "SUCCESS");
            results.put("featuresTested", Arrays.asList(
                "Context Window Analysis",
                "Policy Rubric Evaluation", 
                "Decision Routing",
                "Escalation Logic",
                "Audit Logging",
                "Statistics Generation"
            ));
            
            return ResponseEntity.ok(ApiResponse.success(results));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Integration test failed");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            error.put("userId", userId);
            error.put("integrationTestStatus", "FAILED");
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Get moderation audit trail for a specific content item
     */
    @GetMapping("/audit-trail/{contentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditTrail(
            @PathVariable UUID contentId,
            @RequestParam String contentType) {
        
        try {
            var auditTrail = advancedModerationPipelineService.getModerationAuditTrail(contentId, contentType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("contentType", contentType);
            response.put("auditTrail", auditTrail);
            response.put("auditCount", auditTrail.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve audit trail");
            error.put("message", e.getMessage());
            error.put("contentId", contentId);
            error.put("contentType", contentType);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
    
    /**
     * Get moderation statistics for a user
     */
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "7") int days) {
        
        try {
            var statistics = advancedModerationPipelineService.getModerationStatistics(userId, days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("days", days);
            response.put("statistics", statistics);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve statistics");
            error.put("message", e.getMessage());
            error.put("userId", userId);
            error.put("days", days);
            
            return ResponseEntity.ok(ApiResponse.error(error, 500));
        }
    }
}
