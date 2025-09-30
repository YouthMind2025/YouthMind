package ai.youthmind.controller;

import ai.youthmind.service.InteractionModulationService;
import ai.youthmind.service.ResonanceProcessor;
import ai.youthmind.service.LingerProcessor;
import ai.youthmind.service.PositiveBalanceCalculator;
import ai.youthmind.service.InteractionWeightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for interaction processing services
 * Provides REST endpoints for interaction modulation, resonance, linger, and balance calculations
 */
@RestController
@RequestMapping("/v1/interaction")
@CrossOrigin(origins = "*")
public class InteractionProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(InteractionProcessingController.class);
    
    @Autowired
    private InteractionModulationService interactionModulationService;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private PositiveBalanceCalculator positiveBalanceCalculator;
    
    @Autowired
    private InteractionWeightService interactionWeightService;
    
    /**
     * Process resonance interaction
     * POST /v1/interaction/resonance
     */
    @PostMapping("/resonance")
    public ResponseEntity<InteractionModulationService.InteractionModulationResult> processResonance(
            @RequestParam UUID userId,
            @RequestParam UUID postId) {
        try {
            logger.info("Processing resonance for user {} and post {}", userId, postId);
            
            InteractionModulationService.InteractionModulationResult result = 
                interactionModulationService.processResonance(userId, postId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error processing resonance: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Process linger interaction
     * POST /v1/interaction/linger
     */
    @PostMapping("/linger")
    public ResponseEntity<InteractionModulationService.InteractionModulationResult> processLinger(
            @RequestParam UUID userId,
            @RequestParam UUID postId,
            @RequestParam long lingerDuration) {
        try {
            logger.info("Processing linger for user {} and post {} with duration {}", userId, postId, lingerDuration);
            
            InteractionModulationService.InteractionModulationResult result = 
                interactionModulationService.processLinger(userId, postId, lingerDuration);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error processing linger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate interaction modulation
     * POST /v1/interaction/modulation
     */
    @PostMapping("/modulation")
    public ResponseEntity<Map<String, Object>> calculateModulation(
            @RequestParam double resonanceWeight,
            @RequestParam double lingerWeight,
            @RequestParam double balanceWeight) {
        try {
            logger.info("Calculating modulation with weights: resonance={}, linger={}, balance={}", 
                resonanceWeight, lingerWeight, balanceWeight);
            
            double modulation = interactionModulationService.calculateModulation(
                resonanceWeight, lingerWeight, balanceWeight);
            
            Map<String, Object> result = Map.of(
                "modulation", modulation,
                "resonanceWeight", resonanceWeight,
                "lingerWeight", lingerWeight,
                "balanceWeight", balanceWeight
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating modulation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Apply positive balance
     * POST /v1/interaction/positive-balance
     */
    @PostMapping("/positive-balance")
    public ResponseEntity<Map<String, Object>> applyPositiveBalance(
            @RequestParam UUID userId,
            @RequestParam UUID postId) {
        try {
            logger.info("Applying positive balance for user {} and post {}", userId, postId);
            
            double balance = interactionModulationService.applyPositiveBalance(userId, postId);
            
            Map<String, Object> result = Map.of(
                "positiveBalance", balance,
                "userId", userId.toString(),
                "postId", postId.toString()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error applying positive balance: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get comprehensive interaction analysis
     * GET /v1/interaction/analysis/{userId}
     */
    @GetMapping("/analysis/{userId}")
    public ResponseEntity<InteractionModulationService.ComprehensiveInteractionAnalysis> getComprehensiveAnalysis(
            @PathVariable UUID userId) {
        try {
            logger.info("Getting comprehensive analysis for user {}", userId);
            
            InteractionModulationService.ComprehensiveInteractionAnalysis analysis = 
                interactionModulationService.getComprehensiveAnalysis(userId);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate resonance impact
     * GET /v1/interaction/resonance/impact
     */
    @GetMapping("/resonance/impact")
    public ResponseEntity<Map<String, Object>> calculateResonanceImpact(
            @RequestParam UUID userId,
            @RequestParam UUID postId) {
        try {
            logger.info("Calculating resonance impact for user {} and post {}", userId, postId);
            
            double impact = resonanceProcessor.calculateResonanceImpact(userId, postId);
            Map<String, Object> stateUpdate = resonanceProcessor.updateStateFromResonance(userId, postId);
            Map<String, Object> patterns = resonanceProcessor.trackResonancePatterns(userId);
            
            Map<String, Object> result = Map.of(
                "impact", impact,
                "stateUpdate", stateUpdate,
                "patterns", patterns
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating resonance impact: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get resonance statistics
     * GET /v1/interaction/resonance/statistics/{userId}
     */
    @GetMapping("/resonance/statistics/{userId}")
    public ResponseEntity<Map<String, Object>> getResonanceStatistics(@PathVariable UUID userId) {
        try {
            logger.info("Getting resonance statistics for user {}", userId);
            
            Map<String, Object> statistics = resonanceProcessor.getResonanceStatistics(userId);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting resonance statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate linger impact
     * GET /v1/interaction/linger/impact
     */
    @GetMapping("/linger/impact")
    public ResponseEntity<Map<String, Object>> calculateLingerImpact(@RequestParam long lingerDuration) {
        try {
            logger.info("Calculating linger impact for duration {}", lingerDuration);
            
            double impact = lingerProcessor.calculateLingerImpact(lingerDuration);
            
            Map<String, Object> result = Map.of(
                "impact", impact,
                "duration", lingerDuration
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating linger impact: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get linger statistics
     * GET /v1/interaction/linger/statistics/{userId}
     */
    @GetMapping("/linger/statistics/{userId}")
    public ResponseEntity<Map<String, Object>> getLingerStatistics(@PathVariable UUID userId) {
        try {
            logger.info("Getting linger statistics for user {}", userId);
            
            Map<String, Object> statistics = lingerProcessor.getLingerStatistics(userId);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting linger statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate positive balance
     * GET /v1/interaction/balance/positive
     */
    @GetMapping("/balance/positive")
    public ResponseEntity<Map<String, Object>> calculatePositiveBalance(
            @RequestParam UUID userId,
            @RequestParam UUID postId) {
        try {
            logger.info("Calculating positive balance for user {} and post {}", userId, postId);
            
            double balance = positiveBalanceCalculator.calculatePositiveBalance(userId, postId);
            
            Map<String, Object> result = Map.of(
                "positiveBalance", balance,
                "userId", userId.toString(),
                "postId", postId.toString()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating positive balance: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get balance trends
     * GET /v1/interaction/balance/trends/{userId}
     */
    @GetMapping("/balance/trends/{userId}")
    public ResponseEntity<Map<String, Object>> getBalanceTrends(@PathVariable UUID userId) {
        try {
            logger.info("Getting balance trends for user {}", userId);
            
            Map<String, Object> trends = positiveBalanceCalculator.getBalanceTrends(userId);
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            logger.error("Error getting balance trends: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate beta parameter
     * GET /v1/interaction/weight/beta
     */
    @GetMapping("/weight/beta")
    public ResponseEntity<Map<String, Object>> calculateBeta(@RequestParam(required = false) UUID userId) {
        try {
            logger.info("Calculating beta parameter for user {}", userId);
            
            double beta = userId != null ? 
                interactionWeightService.calculateBeta(userId) : 
                interactionWeightService.calculateBeta();
            
            Map<String, Object> result = Map.of(
                "beta", beta,
                "userId", userId != null ? userId.toString() : "global"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error calculating beta parameter: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get weight statistics
     * GET /v1/interaction/weight/statistics
     */
    @GetMapping("/weight/statistics")
    public ResponseEntity<Map<String, Object>> getWeightStatistics(@RequestParam(required = false) UUID userId) {
        try {
            logger.info("Getting weight statistics for user {}", userId);
            
            Map<String, Object> statistics = interactionWeightService.getWeightStatistics(userId);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error getting weight statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Adjust interaction weight
     * POST /v1/interaction/weight/adjust
     */
    @PostMapping("/weight/adjust")
    public ResponseEntity<Map<String, Object>> adjustWeight(
            @RequestParam(required = false) UUID userId,
            @RequestBody Map<String, Object> performanceMetrics) {
        try {
            logger.info("Adjusting interaction weight for user {} with metrics {}", userId, performanceMetrics);
            
            interactionWeightService.adjustWeight(userId, performanceMetrics);
            
            Map<String, Object> result = Map.of(
                "message", "Weight adjustment completed",
                "userId", userId != null ? userId.toString() : "global",
                "performanceMetrics", performanceMetrics
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error adjusting interaction weight: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check endpoint
     * GET /v1/interaction/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = Map.of(
                "status", "healthy",
                "services", Map.of(
                    "interactionModulation", "active",
                    "resonanceProcessor", "active",
                    "lingerProcessor", "active",
                    "positiveBalanceCalculator", "active",
                    "interactionWeightService", "active"
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
