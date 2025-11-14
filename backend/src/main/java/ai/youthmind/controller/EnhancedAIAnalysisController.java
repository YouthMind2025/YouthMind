package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.AIPostAnalysisService;
import ai.youthmind.service.PsychologicalVectorGenerator;
import ai.youthmind.service.BuddhistFactorAnalyzer;
import ai.youthmind.service.IntensityPredictor;
import ai.youthmind.service.StateVectorProcessor;
import ai.youthmind.service.PsychologicalStateCacheService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for enhanced AI analysis services
 * Provides REST API endpoints for psychological analysis and vector processing
 */
@RestController
@RequestMapping("/api/ai-analysis")
@CrossOrigin(origins = "*")
@Tag(name = "Enhanced AI Analysis", description = "AI-powered psychological analysis and vector processing endpoints")
public class EnhancedAIAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAIAnalysisController.class);
    
    @Autowired
    private AIPostAnalysisService aiPostAnalysisService;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityPredictor intensityPredictor;
    
    @Autowired
    private StateVectorProcessor stateVectorProcessor;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    /**
     * Generate psychological vector for content
     * POST /api/ai-analysis/psychological-vector
     */
    @PostMapping("/psychological-vector")
    @Operation(
        summary = "Generate psychological vector",
        description = "Generate a 43-dimensional psychological vector from content analysis"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological vector generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> generatePsychologicalVector(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String userIdStr = (String) request.get("userId");
            
            if (content == null || userIdStr == null) {
                return ResponseBuilder.badRequest("Missing required fields: content, userId");
            }
            
            UUID userId = UUID.fromString(userIdStr);
            logger.info("Generating psychological vector for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.generatePsychologicalVector(content, userId);
            
            return ResponseBuilder.success(result, "Psychological vector generated successfully");
            
        } catch (Exception e) {
            logger.error("Error generating psychological vector: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to generate psychological vector");
        }
    }
    
    /**
     * Analyze Buddhist factors in content
     * POST /api/ai-analysis/buddhist-factors
     */
    @PostMapping("/buddhist-factors")
    @Operation(
        summary = "Analyze Buddhist factors",
        description = "Analyze Buddhist mental factors in content using sophisticated Buddhist psychology algorithms"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Buddhist factors analyzed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeBuddhistFactors(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String userIdStr = (String) request.get("userId");
            
            if (content == null || userIdStr == null) {
                return ResponseBuilder.badRequest("Missing required fields: content, userId");
            }
            
            UUID userId = UUID.fromString(userIdStr);
            logger.info("Analyzing Buddhist factors for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.analyzeBuddhistFactors(content, userId);
            
            return ResponseBuilder.success(result, "Buddhist factors analyzed successfully");
            
        } catch (Exception e) {
            logger.error("Error analyzing Buddhist factors: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to analyze Buddhist factors");
        }
    }
    
    /**
     * Calculate intensity factor for content
     * POST /api/ai-analysis/intensity-factor
     */
    @PostMapping("/intensity-factor")
    public ResponseEntity<Map<String, Object>> calculateIntensityFactor(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String userIdStr = (String) request.get("userId");
            @SuppressWarnings("unchecked")
            java.util.List<Double> psychologicalVector = (java.util.List<Double>) request.get("psychologicalVector");
            @SuppressWarnings("unchecked")
            Map<String, Object> buddhistFactors = (Map<String, Object>) request.get("buddhistFactors");
            
            if (content == null || userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: content, userId"
                ));
            }
            
            UUID userId = UUID.fromString(userIdStr);
            logger.info("Calculating intensity factor for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.calculateIntensityFactor(content, userId, psychologicalVector, buddhistFactors);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating intensity factor: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to calculate intensity factor",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Process psychological content comprehensively
     * POST /api/ai-analysis/process-content
     */
    @PostMapping("/process-content")
    public ResponseEntity<Map<String, Object>> processPsychologicalContent(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String userIdStr = (String) request.get("userId");
            
            if (content == null || userIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: content, userId"
                ));
            }
            
            UUID userId = UUID.fromString(userIdStr);
            logger.info("Processing psychological content for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.processPsychologicalContent(content, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error processing psychological content: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to process psychological content",
                "message", e.getMessage()
            ));
        }
    }
    
    // ===== GET ENDPOINTS FOR USER-SPECIFIC DATA =====
    
    /**
     * Get Buddhist factors analysis for a user
     * GET /api/ai-analysis/buddhist-factors/{userId}
     */
    @GetMapping("/buddhist-factors/{userId}")
    @Operation(
        summary = "Get Buddhist factors analysis",
        description = "Retrieve cached Buddhist factors analysis for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Buddhist factors analysis retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBuddhistFactors(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting Buddhist factors analysis for user {}", userId);
            
            // Try cache first
            Map<String, Double> cachedResult = psychologicalStateCacheService.getBuddhistFactors(userId);
            if (cachedResult != null) {
                Map<String, Object> result = new HashMap<>(cachedResult);
                return ResponseBuilder.success(result, "Buddhist factors analysis retrieved from cache");
            }
            
            // Fallback to service
            Map<String, Object> result = buddhistFactorAnalyzer.getFactorAnalysis(userId);
            
            return ResponseBuilder.success(result, "Buddhist factors analysis retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting Buddhist factors analysis for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get Buddhist factors analysis");
        }
    }
    
    /**
     * Get psychological vector for a user
     * GET /api/ai-analysis/psychological-vector/{userId}
     */
    @GetMapping("/psychological-vector/{userId}")
    @Operation(
        summary = "Get psychological vector",
        description = "Retrieve cached psychological vector for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological vector retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPsychologicalVector(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting psychological vector for user {}", userId);
            
            // Try cache first
            Map<String, Object> cachedResult = psychologicalStateCacheService.getPsychologicalState(userId);
            if (cachedResult != null) {
                return ResponseBuilder.success(cachedResult, "Psychological vector retrieved from cache");
            }
            
            // Fallback to service (this would need to be implemented in PsychologicalVectorGenerator)
            Map<String, Object> result = Map.of(
                "userId", userId.toString(),
                "vector", java.util.List.of(),
                "message", "No cached psychological vector found",
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseBuilder.success(result, "Psychological vector retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting psychological vector for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get psychological vector");
        }
    }
    
    /**
     * Get intensity prediction for a user
     * GET /api/ai-analysis/intensity-prediction/{userId}
     */
    @GetMapping("/intensity-prediction/{userId}")
    @Operation(
        summary = "Get intensity prediction",
        description = "Retrieve cached intensity prediction for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intensity prediction retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntensityPrediction(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting intensity prediction for user {}", userId);
            
            Map<String, Object> result = intensityPredictor.getIntensityPrediction(userId);
            
            return ResponseBuilder.success(result, "Intensity prediction retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting intensity prediction for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get intensity prediction");
        }
    }
    
    /**
     * Get prediction accuracy for a user
     * GET /api/ai-analysis/prediction-accuracy/{userId}
     */
    @GetMapping("/prediction-accuracy/{userId}")
    @Operation(
        summary = "Get prediction accuracy",
        description = "Retrieve prediction accuracy statistics for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Prediction accuracy retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictionAccuracy(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting prediction accuracy for user {}", userId);
            
            Map<String, Object> result = intensityPredictor.getPredictionAccuracy(userId);
            
            return ResponseBuilder.success(result, "Prediction accuracy retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting prediction accuracy for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get prediction accuracy");
        }
    }
    
    /**
     * Get processed vector for a user
     * GET /api/ai-analysis/processed-vector/{userId}
     */
    @GetMapping("/processed-vector/{userId}")
    @Operation(
        summary = "Get processed vector",
        description = "Retrieve cached processed psychological vector for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Processed vector retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessedVector(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting processed vector for user {}", userId);
            
            Map<String, Object> result = stateVectorProcessor.getProcessedVector(userId);
            
            return ResponseBuilder.success(result, "Processed vector retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting processed vector for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get processed vector");
        }
    }
    
    /**
     * Track prediction accuracy
     * POST /api/ai-analysis/track-accuracy/{userId}
     */
    @PostMapping("/track-accuracy/{userId}")
    public ResponseEntity<Map<String, Object>> trackPredictionAccuracy(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> prediction = (Map<String, Object>) request.get("prediction");
            Double actualIntensity = (Double) request.get("actualIntensity");
            
            if (prediction == null || actualIntensity == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: prediction, actualIntensity"
                ));
            }
            
            logger.info("Tracking prediction accuracy for user {}", userId);
            
            intensityPredictor.trackPredictionAccuracy(userId, prediction, actualIntensity);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Prediction accuracy tracked",
                "userId", userId.toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error tracking prediction accuracy for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to track prediction accuracy",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    // ===== PSYCHOLOGY-SPECIFIC ENDPOINTS =====
    
    /**
     * Get current psychological state for a user
     * GET /api/ai/psychology/state/{userId}
     */
    @GetMapping("/psychology/state/{userId}")
    @Operation(
        summary = "Get psychological state",
        description = "Retrieve current psychological state information for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological state retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPsychologicalState(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        try {
            // Verify user authorization
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            if (!currentUserId.equals(userId)) {
                return ResponseBuilder.forbidden("Access denied: Cannot access other user's data");
            }
            
            logger.info("Getting psychological state for user {}", userId);
            
            // Get cached psychological state
            Map<String, Object> cachedState = psychologicalStateCacheService.getPsychologicalState(userId);
            if (cachedState != null) {
                return ResponseBuilder.success(cachedState, "Psychological state retrieved from cache");
            }
            
            // Return default state if no cached state found
            Map<String, Object> defaultState = Map.of(
                "userId", userId.toString(),
                "state", "No psychological state data available",
                "timestamp", java.time.LocalDateTime.now(),
                "message", "No cached psychological state found"
            );
            
            return ResponseBuilder.success(defaultState, "Psychological state retrieved successfully");
            
        } catch (Exception e) {
            logger.error("Error getting psychological state for user {}: {}", userId, e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to get psychological state");
        }
    }
    
    /**
     * Health check for AI psychology services
     * GET /api/ai/psychology/health
     */
    @GetMapping("/psychology/health")
    @Operation(
        summary = "AI Psychology Health Check",
        description = "Check the health status of AI psychology services"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychology services are healthy"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Psychology services are unhealthy")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPsychologyHealth() {
        try {
            logger.debug("Performing AI psychology health check");
            
            Map<String, Object> health = Map.of(
                "status", "HEALTHY",
                "services", Map.of(
                    "psychologicalVectorGenerator", "AVAILABLE",
                    "buddhistFactorAnalyzer", "AVAILABLE",
                    "intensityPredictor", "AVAILABLE",
                    "stateVectorProcessor", "AVAILABLE",
                    "psychologicalStateCacheService", "AVAILABLE",
                    "aiPostAnalysisService", "AVAILABLE"
                ),
                "cacheStatistics", psychologicalStateCacheService.getCacheStatistics(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseBuilder.success(health, "AI psychology services are healthy");
            
        } catch (Exception e) {
            logger.error("Error performing AI psychology health check: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("AI psychology services are unhealthy");
        }
    }
    
    /**
     * Health check endpoint for enhanced AI analysis services
     * GET /api/ai-analysis/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Enhanced AI Analysis Health Check",
        description = "Check the health status of enhanced AI analysis services"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI analysis services are healthy"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI analysis services are unhealthy")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            logger.debug("Performing enhanced AI analysis health check");
            
            Map<String, Object> health = Map.of(
                "status", "HEALTHY",
                "services", Map.of(
                    "psychologicalVectorGenerator", "AVAILABLE",
                    "buddhistFactorAnalyzer", "AVAILABLE",
                    "intensityPredictor", "AVAILABLE",
                    "stateVectorProcessor", "AVAILABLE",
                    "aiPostAnalysisService", "AVAILABLE"
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseBuilder.success(health, "Enhanced AI analysis services are healthy");
            
        } catch (Exception e) {
            logger.error("Error performing enhanced AI analysis health check: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Enhanced AI analysis services are unhealthy");
        }
    }
}
