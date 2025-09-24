package ai.youthmind.controller;

import ai.youthmind.service.AIPostAnalysisService;
import ai.youthmind.service.PsychologicalVectorGenerator;
import ai.youthmind.service.BuddhistFactorAnalyzer;
import ai.youthmind.service.IntensityPredictor;
import ai.youthmind.service.StateVectorProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for enhanced AI analysis services
 * Provides REST API endpoints for psychological analysis and vector processing
 */
@RestController
@RequestMapping("/api/ai-analysis")
@CrossOrigin(origins = "*")
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
    
    /**
     * Generate psychological vector for content
     * POST /api/ai-analysis/psychological-vector
     */
    @PostMapping("/psychological-vector")
    public ResponseEntity<Map<String, Object>> generatePsychologicalVector(
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
            logger.info("Generating psychological vector for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.generatePsychologicalVector(content, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error generating psychological vector: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to generate psychological vector",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Analyze Buddhist factors in content
     * POST /api/ai-analysis/buddhist-factors
     */
    @PostMapping("/buddhist-factors")
    public ResponseEntity<Map<String, Object>> analyzeBuddhistFactors(
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
            logger.info("Analyzing Buddhist factors for user {} with content length {}", userId, content.length());
            
            Map<String, Object> result = aiPostAnalysisService.analyzeBuddhistFactors(content, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error analyzing Buddhist factors: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to analyze Buddhist factors",
                "message", e.getMessage()
            ));
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
    
    /**
     * Get psychological vector for a user
     * GET /api/ai-analysis/psychological-vector/{userId}
     */
    @GetMapping("/psychological-vector/{userId}")
    public ResponseEntity<Map<String, Object>> getPsychologicalVector(@PathVariable UUID userId) {
        try {
            logger.info("Getting psychological vector for user {}", userId);
            
            Map<String, Object> result = psychologicalVectorGenerator.getVector(userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting psychological vector for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get psychological vector",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get Buddhist factors analysis for a user
     * GET /api/ai-analysis/buddhist-factors/{userId}
     */
    @GetMapping("/buddhist-factors/{userId}")
    public ResponseEntity<Map<String, Object>> getBuddhistFactors(@PathVariable UUID userId) {
        try {
            logger.info("Getting Buddhist factors analysis for user {}", userId);
            
            Map<String, Object> result = buddhistFactorAnalyzer.getFactorAnalysis(userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting Buddhist factors analysis for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get Buddhist factors analysis",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get intensity prediction for a user
     * GET /api/ai-analysis/intensity-prediction/{userId}
     */
    @GetMapping("/intensity-prediction/{userId}")
    public ResponseEntity<Map<String, Object>> getIntensityPrediction(@PathVariable UUID userId) {
        try {
            logger.info("Getting intensity prediction for user {}", userId);
            
            Map<String, Object> result = intensityPredictor.getIntensityPrediction(userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting intensity prediction for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get intensity prediction",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get processed vector for a user
     * GET /api/ai-analysis/processed-vector/{userId}
     */
    @GetMapping("/processed-vector/{userId}")
    public ResponseEntity<Map<String, Object>> getProcessedVector(@PathVariable UUID userId) {
        try {
            logger.info("Getting processed vector for user {}", userId);
            
            Map<String, Object> result = stateVectorProcessor.getProcessedVector(userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting processed vector for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get processed vector",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Get prediction accuracy for a user
     * GET /api/ai-analysis/prediction-accuracy/{userId}
     */
    @GetMapping("/prediction-accuracy/{userId}")
    public ResponseEntity<Map<String, Object>> getPredictionAccuracy(@PathVariable UUID userId) {
        try {
            logger.info("Getting prediction accuracy for user {}", userId);
            
            Map<String, Object> result = intensityPredictor.getPredictionAccuracy(userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting prediction accuracy for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get prediction accuracy",
                "message", e.getMessage(),
                "userId", userId.toString()
            ));
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
    
    /**
     * Health check endpoint for enhanced AI analysis services
     * GET /api/ai-analysis/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
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
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error performing enhanced AI analysis health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "UNHEALTHY",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
