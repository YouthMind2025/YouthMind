package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.PersonalityComputationService;
import ai.youthmind.service.PersonalityScheduler;
import ai.youthmind.util.ResponseBuilder;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Personality Computation", description = "AI-powered personality analysis and computation endpoints")
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
    @Operation(
        summary = "Get comprehensive personality analysis",
        description = "Retrieve comprehensive personality analysis for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality analysis retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComprehensivePersonalityAnalysis(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting comprehensive personality analysis for user {}", userId);
            
            Map<String, Object> analysis = personalityComputationService.getComprehensivePersonalityAnalysis(userId);
            
            return ResponseBuilder.success(analysis, "Personality analysis retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive personality analysis for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get personality analysis");
        }
    }
    
    /**
     * Compute active personality for a user
     * GET /api/personality/active/{userId}
     */
    @GetMapping("/active/{userId}")
    @Operation(
        summary = "Get active personality",
        description = "Retrieve active personality computation for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active personality retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> computeActivePersonality(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Computing active personality for user {}", userId);
            
            Map<String, Object> activePersonality = personalityComputationService.computeActivePersonality(userId);
            
            return ResponseBuilder.success(activePersonality, "Active personality retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error computing active personality for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to compute active personality");
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
    @Operation(
        summary = "Get fused personality",
        description = "Retrieve fused personality computation combining active and subconscious personalities for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fused personality retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> fusePersonalities(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Fusing personalities for user {}", userId);
            
            Map<String, Object> fusedPersonality = personalityComputationService.fusePersonalities(userId);
            
            return ResponseBuilder.success(fusedPersonality, "Fused personality retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error fusing personalities for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to fuse personalities");
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
    @Operation(
        summary = "Personality Computation Health Check",
        description = "Check the health status of personality computation services"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality computation services are healthy"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Personality computation services are unhealthy")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
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
            
            return ResponseBuilder.success(health, "Personality computation services are healthy");
            
        } catch (Exception e) {
            logger.error("Error performing personality computation health check: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Personality computation services are unhealthy");
        }
    }
}
