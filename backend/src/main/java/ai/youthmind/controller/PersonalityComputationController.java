package ai.youthmind.controller;

import ai.youthmind.service.PersonalityComputationService;
import ai.youthmind.service.PersonalityScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for personality computation services
 * Provides REST API endpoints for personality analysis and computation
 */
@RestController
@RequestMapping("/api/personality")
@CrossOrigin(origins = "*")
public class PersonalityComputationController {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityComputationController.class);
    
    @Autowired
    private PersonalityComputationService personalityComputationService;
    
    @Autowired
    private PersonalityScheduler personalityScheduler;
    
    /**
     * Get comprehensive personality analysis for a user
     * GET /api/personality/analysis/{userId}
     */
    @GetMapping("/analysis/{userId}")
    public ResponseEntity<Map<String, Object>> getComprehensivePersonalityAnalysis(@PathVariable UUID userId) {
        try {
            logger.info("Getting comprehensive personality analysis for user {}", userId);
            
            Map<String, Object> analysis = personalityComputationService.getComprehensivePersonalityAnalysis(userId);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive personality analysis for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get personality analysis",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Compute active personality for a user
     * GET /api/personality/active/{userId}
     */
    @GetMapping("/active/{userId}")
    public ResponseEntity<Map<String, Object>> computeActivePersonality(@PathVariable UUID userId) {
        try {
            logger.info("Computing active personality for user {}", userId);
            
            Map<String, Object> activePersonality = personalityComputationService.computeActivePersonality(userId);
            
            return ResponseEntity.ok(activePersonality);
            
        } catch (Exception e) {
            logger.error("Error computing active personality for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to compute active personality",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Compute subconscious personality for a user
     * GET /api/personality/subconscious/{userId}
     */
    @GetMapping("/subconscious/{userId}")
    public ResponseEntity<Map<String, Object>> computeSubconsciousPersonality(@PathVariable UUID userId) {
        try {
            logger.info("Computing subconscious personality for user {}", userId);
            
            Map<String, Object> subconsciousPersonality = personalityComputationService.computeSubconsciousPersonality(userId);
            
            return ResponseEntity.ok(subconsciousPersonality);
            
        } catch (Exception e) {
            logger.error("Error computing subconscious personality for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to compute subconscious personality",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Fuse personalities for a user
     * GET /api/personality/fused/{userId}
     */
    @GetMapping("/fused/{userId}")
    public ResponseEntity<Map<String, Object>> fusePersonalities(@PathVariable UUID userId) {
        try {
            logger.info("Fusing personalities for user {}", userId);
            
            Map<String, Object> fusedPersonality = personalityComputationService.fusePersonalities(userId);
            
            return ResponseEntity.ok(fusedPersonality);
            
        } catch (Exception e) {
            logger.error("Error fusing personalities for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to fuse personalities",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Trigger personality computation for a user
     * POST /api/personality/compute/{userId}
     */
    @PostMapping("/compute/{userId}")
    public ResponseEntity<Map<String, Object>> triggerPersonalityComputation(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "false") boolean forceRecompute) {
        try {
            logger.info("Triggering personality computation for user {} (force: {})", userId, forceRecompute);
            
            Map<String, Object> result = personalityComputationService.triggerPersonalityComputation(userId, forceRecompute);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error triggering personality computation for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to trigger personality computation",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Schedule personality computation for a user
     * POST /api/personality/schedule/{userId}
     */
    @PostMapping("/schedule/{userId}")
    public ResponseEntity<Map<String, Object>> schedulePersonalityComputation(
            @PathVariable UUID userId,
            @RequestParam String triggerType) {
        try {
            logger.info("Scheduling personality computation for user {} with trigger {}", userId, triggerType);
            
            Map<String, Object> result = personalityScheduler.scheduleComputation(userId, triggerType);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error scheduling personality computation for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to schedule personality computation",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get computation history for a user
     * GET /api/personality/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<Map<String, Object>> getComputationHistory(@PathVariable UUID userId) {
        try {
            logger.info("Getting computation history for user {}", userId);
            
            Map<String, Object> history = personalityScheduler.getComputationHistory(userId);
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error getting computation history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get computation history",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get personality computation status
     * GET /api/personality/status/{userId}
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalityComputationStatus(@PathVariable UUID userId) {
        try {
            logger.info("Getting personality computation status for user {}", userId);
            
            // Get comprehensive analysis to check status
            Map<String, Object> analysis = personalityComputationService.getComprehensivePersonalityAnalysis(userId);
            
            Map<String, Object> status = Map.of(
                "userId", userId.toString(),
                "hasActivePersonality", analysis.containsKey("activePersonality"),
                "hasSubconsciousPersonality", analysis.containsKey("subconsciousPersonality"),
                "hasFusedPersonality", analysis.containsKey("fusedPersonality"),
                "lastAnalysisTime", analysis.getOrDefault("analysisTimestamp", "unknown"),
                "status", "AVAILABLE"
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error getting personality computation status for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get personality computation status",
                "message", e.getMessage(),
                "userId", userId.toString(),
                "status", "ERROR"
            ));
        }
    }
    
    /**
     * Health check endpoint for personality computation services
     * GET /api/personality/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            logger.debug("Performing personality computation health check");
            
            Map<String, Object> health = Map.of(
                "status", "HEALTHY",
                "services", Map.of(
                    "personalityComputationService", "AVAILABLE",
                    "personalityScheduler", "AVAILABLE"
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error performing personality computation health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "UNHEALTHY",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
