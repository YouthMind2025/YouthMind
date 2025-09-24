package ai.youthmind.controller;

import ai.youthmind.service.AIChatService;
import ai.youthmind.service.AIErrorHandler;
import ai.youthmind.service.PsychologicalStateCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for psychological state-aware AI chat functionality
 * Integrates Youseef's psychological analysis with our existing AI chat system
 */
@RestController
@RequestMapping("/api/ai/psychology")
@CrossOrigin(origins = "*")
public class PsychologicalAIChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalAIChatController.class);
    
    @Autowired
    private AIChatService aiChatService;
    
    @Autowired
    private AIErrorHandler aiErrorHandler;
    
    @Autowired
    private PsychologicalStateCacheService cacheService;
    
    /**
     * Generate psychological state-aware AI response
     * Integrates Youseef's approach with our 43-dimensional psychological vectors
     */
    @PostMapping("/chat/{userId}")
    public ResponseEntity<Map<String, Object>> generatePsychologicalResponse(
            @PathVariable String userId, 
            @RequestBody(required = false) Map<String, String> payload) {
        
        try {
            String message = payload == null ? "" : payload.getOrDefault("message", "");
            logger.info("Generating psychological state-aware response for user: {}", userId);
            
            // Generate psychological state-aware response
            Map<String, Object> response = aiChatService.generatePsychologicalStateAwareResponse(userId, message);
            
            // Analyze and update psychological state asynchronously
            aiChatService.analyzeAndUpdatePsychologicalState(userId, message);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating psychological response for user {}: {}", userId, e.getMessage(), e);
            
            // Use AI error handler for sophisticated error management
            Map<String, Object> errorContext = Map.of(
                "userId", userId,
                "message", payload != null ? payload.getOrDefault("message", "") : "",
                "operation", "psychological_chat"
            );
            
            Map<String, Object> errorResult = aiErrorHandler.handleError(e, errorContext, UUID.fromString(userId));
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    /**
     * Get user's current psychological state
     */
    @GetMapping("/state/{userId}")
    public ResponseEntity<Map<String, Object>> getPsychologicalState(@PathVariable String userId) {
        try {
            logger.info("Retrieving psychological state for user: {}", userId);
            
            // Get cached psychological state using enhanced cache service
            Map<String, Object> cachedState = cacheService.getPsychologicalState(UUID.fromString(userId));
            
            if (cachedState != null) {
                return ResponseEntity.ok(cachedState);
            } else {
                // Return default state if no cached data
                Map<String, Object> defaultState = Map.of(
                    "stress", 0.3,
                    "mindfulness", 0.5,
                    "compassion", 0.6,
                    "equanimity", 0.4,
                    "intensityFactor", 0.5,
                    "timestamp", System.currentTimeMillis(),
                    "status", "default"
                );
                return ResponseEntity.ok(defaultState);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving psychological state for user {}: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorContext = Map.of(
                "userId", userId,
                "operation", "get_psychological_state"
            );
            
            Map<String, Object> errorResult = aiErrorHandler.handleError(e, errorContext, UUID.fromString(userId));
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    /**
     * Analyze message for psychological insights
     */
    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Map<String, Object>> analyzeMessage(
            @PathVariable String userId, 
            @RequestBody Map<String, String> payload) {
        
        try {
            String message = payload.getOrDefault("message", "");
            logger.info("Analyzing message for psychological insights for user: {}", userId);
            
            // Perform comprehensive psychological analysis
            CompletableFuture<Void> analysisFuture = aiChatService.analyzeAndUpdatePsychologicalState(userId, message);
            
            // Wait for analysis to complete (with timeout)
            analysisFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            
            // Get updated psychological state using enhanced cache service
            Map<String, Object> cachedState = cacheService.getPsychologicalState(UUID.fromString(userId));
            
            Map<String, Object> response = Map.of(
                "userId", userId,
                "message", message,
                "analysisCompleted", true,
                "psychologicalState", cachedState != null ? cachedState : Map.of("status", "analyzing"),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing message for user {}: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorContext = Map.of(
                "userId", userId,
                "message", payload.getOrDefault("message", ""),
                "operation", "psychological_analysis"
            );
            
            Map<String, Object> errorResult = aiErrorHandler.handleError(e, errorContext, UUID.fromString(userId));
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    
    /**
     * Health check endpoint for psychological AI services
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "Psychological AI Chat",
            "timestamp", System.currentTimeMillis(),
            "features", Map.of(
                "psychologicalStateAware", true,
                "buddhistFactors", true,
                "asyncAnalysis", true,
                "errorHandling", true,
                "circuitBreaker", true
            )
        );
        
        return ResponseEntity.ok(health);
    }
}
