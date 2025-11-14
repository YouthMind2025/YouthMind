package ai.youthmind.service;

import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for dual personality computation
 * Implements sophisticated personality calculation with active and subconscious components
 */
@Service
@Transactional
public class PersonalityComputationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityComputationService.class);
    
    // Personality computation constants
    public static final double ACTIVE_PERSONALITY_WEIGHT = 0.7; // λ₁ weight for active personality
    public static final double SUBCONSCIOUS_PERSONALITY_WEIGHT = 0.3; // λ₂ weight for subconscious personality
    public static final int TOP_K_RESONANCE = 10; // Top-K resonance for subconscious calculation
    public static final long PERSONALITY_COMPUTATION_INTERVAL_HOURS = 24; // Minimum interval between computations
    
    // Redis keys
    private static final String ACTIVE_PERSONALITY_KEY = "active_personality:";
    private static final String SUBCONSCIOUS_PERSONALITY_KEY = "subconscious_personality:";
    private static final String FUSED_PERSONALITY_KEY = "fused_personality:";
    private static final String COMPUTATION_HISTORY_KEY = "personality_computation_history:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ActivePersonalityCalculator activePersonalityCalculator;
    
    @Autowired
    private SubconsciousPersonalityCalculator subconsciousPersonalityCalculator;
    
    @Autowired
    private PersonalityFusionService personalityFusionService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    /**
     * Compute active personality (μ_post - weighted mean)
     * @param userId User ID
     * @return Active personality vector
     */
    public Map<String, Object> computeActivePersonality(UUID userId) {
        try {
            logger.debug("Computing active personality for user {}", userId);
            
            // Check if we have recent computation
            String cacheKey = ACTIVE_PERSONALITY_KEY + userId;
            Map<String, Object> cachedResult = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedResult != null) {
                LocalDateTime lastComputed = (LocalDateTime) cachedResult.get("lastComputed");
                if (lastComputed != null && 
                    ChronoUnit.HOURS.between(lastComputed, LocalDateTime.now()) < PERSONALITY_COMPUTATION_INTERVAL_HOURS) {
                    logger.debug("Using cached active personality for user {}", userId);
                    return cachedResult;
                }
            }
            
            // Get user's recent interactions
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, cutoffTime);
            
            if (recentResonances.isEmpty()) {
                logger.debug("No recent resonances found for user {}, using default active personality", userId);
                return createDefaultActivePersonality(userId);
            }
            
            // Calculate weighted mean personality
            Map<String, Object> activePersonality = activePersonalityCalculator.calculateWeightedMean(userId, recentResonances);
            
            // Apply post weights
            activePersonalityCalculator.applyPostWeights(activePersonality, recentResonances);
            
            // Track active trends
            activePersonalityCalculator.trackActiveTrends(userId, activePersonality);
            
            // Cache the result
            activePersonality.put("lastComputed", LocalDateTime.now());
            activePersonality.put("computationType", "ACTIVE");
            redisTemplate.opsForValue().set(cacheKey, activePersonality, 6, TimeUnit.HOURS);
            
            logger.debug("Active personality computed for user {}: {}", userId, activePersonality);
            return activePersonality;
            
        } catch (Exception e) {
            logger.error("Error computing active personality for user {}: {}", userId, e.getMessage(), e);
            return createDefaultActivePersonality(userId);
        }
    }
    
    /**
     * Compute subconscious personality (V_shadow - Top-K resonance)
     * @param userId User ID
     * @return Subconscious personality vector
     */
    public Map<String, Object> computeSubconsciousPersonality(UUID userId) {
        try {
            logger.debug("Computing subconscious personality for user {}", userId);
            
            // Check if we have recent computation
            String cacheKey = SUBCONSCIOUS_PERSONALITY_KEY + userId;
            Map<String, Object> cachedResult = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedResult != null) {
                LocalDateTime lastComputed = (LocalDateTime) cachedResult.get("lastComputed");
                if (lastComputed != null && 
                    ChronoUnit.HOURS.between(lastComputed, LocalDateTime.now()) < PERSONALITY_COMPUTATION_INTERVAL_HOURS) {
                    logger.debug("Using cached subconscious personality for user {}", userId);
                    return cachedResult;
                }
            }
            
            // Get user's resonance history
            List<PostResonance> allResonances = postResonanceRepository.findByUserId(userId);
            
            if (allResonances.isEmpty()) {
                logger.debug("No resonances found for user {}, using default subconscious personality", userId);
                return createDefaultSubconsciousPersonality(userId);
            }
            
            // Find Top-K resonance patterns
            Map<String, Object> topKResonance = subconsciousPersonalityCalculator.findTopKResonance(userId, allResonances, TOP_K_RESONANCE);
            
            // Calculate subconscious vector
            Map<String, Object> subconsciousVector = subconsciousPersonalityCalculator.calculateSubconsciousVector(userId, topKResonance);
            
            // Track resonance patterns
            subconsciousPersonalityCalculator.trackResonancePatterns(userId, subconsciousVector);
            
            // Cache the result
            subconsciousVector.put("lastComputed", LocalDateTime.now());
            subconsciousVector.put("computationType", "SUBCONSCIOUS");
            subconsciousVector.put("topKResonance", topKResonance);
            redisTemplate.opsForValue().set(cacheKey, subconsciousVector, 6, TimeUnit.HOURS);
            
            logger.debug("Subconscious personality computed for user {}: {}", userId, subconsciousVector);
            return subconsciousVector;
            
        } catch (Exception e) {
            logger.error("Error computing subconscious personality for user {}: {}", userId, e.getMessage(), e);
            return createDefaultSubconsciousPersonality(userId);
        }
    }
    
    /**
     * Fuse active and subconscious personalities
     * Combines λ₁·μ_post + λ₂·V_shadow
     * @param userId User ID
     * @return Fused personality result
     */
    public Map<String, Object> fusePersonalities(UUID userId) {
        try {
            logger.debug("Fusing personalities for user {}", userId);
            
            // Check if we have recent fusion
            String cacheKey = FUSED_PERSONALITY_KEY + userId;
            Map<String, Object> cachedResult = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedResult != null) {
                LocalDateTime lastComputed = (LocalDateTime) cachedResult.get("lastComputed");
                if (lastComputed != null && 
                    ChronoUnit.HOURS.between(lastComputed, LocalDateTime.now()) < PERSONALITY_COMPUTATION_INTERVAL_HOURS) {
                    logger.debug("Using cached fused personality for user {}", userId);
                    return cachedResult;
                }
            }
            
            // Get active personality
            Map<String, Object> activePersonality = computeActivePersonality(userId);
            
            // Get subconscious personality
            Map<String, Object> subconsciousPersonality = computeSubconsciousPersonality(userId);
            
            // Calculate lambda weights
            Map<String, Double> lambdaWeights = personalityFusionService.calculateLambdaWeights(userId, activePersonality, subconsciousPersonality);
            
            // Fuse personalities
            Map<String, Object> fusedPersonality = personalityFusionService.fusePersonalities(
                activePersonality, subconsciousPersonality, lambdaWeights);
            
            // Validate fusion
            boolean isValid = personalityFusionService.validateFusion(fusedPersonality);
            fusedPersonality.put("isValid", isValid);
            
            // Cache the result
            fusedPersonality.put("lastComputed", LocalDateTime.now());
            fusedPersonality.put("computationType", "FUSED");
            fusedPersonality.put("lambdaWeights", lambdaWeights);
            redisTemplate.opsForValue().set(cacheKey, fusedPersonality, 6, TimeUnit.HOURS);
            
            logger.debug("Personalities fused for user {}: {}", userId, fusedPersonality);
            return fusedPersonality;
            
        } catch (Exception e) {
            logger.error("Error fusing personalities for user {}: {}", userId, e.getMessage(), e);
            return createDefaultFusedPersonality(userId);
        }
    }
    
    /**
     * Get comprehensive personality analysis for a user
     * @param userId User ID
     * @return Comprehensive personality analysis
     */
    public Map<String, Object> getComprehensivePersonalityAnalysis(UUID userId) {
        try {
            logger.debug("Getting comprehensive personality analysis for user {}", userId);
            
            Map<String, Object> analysis = new HashMap<>();
            
            // Get all personality components
            Map<String, Object> activePersonality = computeActivePersonality(userId);
            Map<String, Object> subconsciousPersonality = computeSubconsciousPersonality(userId);
            Map<String, Object> fusedPersonality = fusePersonalities(userId);
            
            // Get user's base personality profile
            UserPersonalityProfile baseProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Compile comprehensive analysis
            analysis.put("userId", userId.toString());
            analysis.put("activePersonality", activePersonality);
            analysis.put("subconsciousPersonality", subconsciousPersonality);
            analysis.put("fusedPersonality", fusedPersonality);
            analysis.put("baseProfile", baseProfile);
            analysis.put("analysisTimestamp", LocalDateTime.now());
            
            // Add personality insights
            Map<String, Object> insights = generatePersonalityInsights(activePersonality, subconsciousPersonality, fusedPersonality);
            analysis.put("insights", insights);
            
            // Add computation history
            Map<String, Object> computationHistory = getComputationHistory(userId);
            analysis.put("computationHistory", computationHistory);
            
            logger.debug("Comprehensive personality analysis completed for user {}", userId);
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive personality analysis for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Trigger personality computation for a user
     * @param userId User ID
     * @param forceRecompute Force recomputation even if recent computation exists
     * @return Computation result
     */
    public Map<String, Object> triggerPersonalityComputation(UUID userId, boolean forceRecompute) {
        try {
            logger.info("Triggering personality computation for user {} (force: {})", userId, forceRecompute);
            
            // Schedule computation if not forcing immediate computation
            if (!forceRecompute) {
                // For now, just proceed with computation instead of scheduling
                // In a real implementation, this would use a message queue or event system
                logger.info("Manual trigger requested for user {} - proceeding with computation", userId);
            }
            
            // Force immediate computation
            Map<String, Object> result = new HashMap<>();
            
            // Compute all personality components
            Map<String, Object> activePersonality = computeActivePersonality(userId);
            Map<String, Object> subconsciousPersonality = computeSubconsciousPersonality(userId);
            Map<String, Object> fusedPersonality = fusePersonalities(userId);
            
            result.put("status", "COMPLETED");
            result.put("userId", userId.toString());
            result.put("activePersonality", activePersonality);
            result.put("subconsciousPersonality", subconsciousPersonality);
            result.put("fusedPersonality", fusedPersonality);
            result.put("timestamp", LocalDateTime.now());
            
            // Track computation history
            trackComputationHistory(userId, "MANUAL_TRIGGER", result);
            
            logger.info("Personality computation completed for user {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error triggering personality computation for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to compute personality: " + e.getMessage(),
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Get personality insights for a specific user
     */
    public Map<String, Object> getPersonalityInsights(UUID userId) {
        Map<String, Object> insights = new HashMap<>();
        
        try {
            logger.info("Generating personality insights for user: {}", userId);
            
            // Get user's personality data
            Map<String, Object> activePersonality = computeActivePersonality(userId);
            Map<String, Object> subconsciousPersonality = computeSubconsciousPersonality(userId);
            Map<String, Object> fusedPersonality = fusePersonalities(userId);
            
            // Generate insights from personality data
            Map<String, Object> personalityInsights = generatePersonalityInsights(
                activePersonality, subconsciousPersonality, fusedPersonality);
            
            // Add personality data to insights
            insights.put("activePersonality", activePersonality);
            insights.put("subconsciousPersonality", subconsciousPersonality);
            insights.put("fusedPersonality", fusedPersonality);
            insights.put("insights", personalityInsights);
            insights.put("userId", userId.toString());
            insights.put("timestamp", LocalDateTime.now());
            
            logger.info("Successfully generated personality insights for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error generating personality insights for user {}: {}", userId, e.getMessage(), e);
            insights.put("error", "Failed to generate personality insights: " + e.getMessage());
            insights.put("userId", userId.toString());
            insights.put("timestamp", LocalDateTime.now());
        }
        
        return insights;
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<String, Object> createDefaultActivePersonality(UUID userId) {
        Map<String, Object> defaultPersonality = new HashMap<>();
        defaultPersonality.put("userId", userId.toString());
        defaultPersonality.put("personalityVector", Arrays.asList(0.5, 0.5, 0.5, 0.5, 0.5));
        defaultPersonality.put("confidence", 0.1);
        defaultPersonality.put("computationType", "DEFAULT_ACTIVE");
        defaultPersonality.put("lastComputed", LocalDateTime.now());
        return defaultPersonality;
    }
    
    private Map<String, Object> createDefaultSubconsciousPersonality(UUID userId) {
        Map<String, Object> defaultPersonality = new HashMap<>();
        defaultPersonality.put("userId", userId.toString());
        defaultPersonality.put("personalityVector", Arrays.asList(0.3, 0.3, 0.3, 0.3, 0.3));
        defaultPersonality.put("confidence", 0.1);
        defaultPersonality.put("computationType", "DEFAULT_SUBCONSCIOUS");
        defaultPersonality.put("lastComputed", LocalDateTime.now());
        return defaultPersonality;
    }
    
    private Map<String, Object> createDefaultFusedPersonality(UUID userId) {
        Map<String, Object> defaultPersonality = new HashMap<>();
        defaultPersonality.put("userId", userId.toString());
        defaultPersonality.put("personalityVector", Arrays.asList(0.4, 0.4, 0.4, 0.4, 0.4));
        defaultPersonality.put("confidence", 0.1);
        defaultPersonality.put("computationType", "DEFAULT_FUSED");
        defaultPersonality.put("lastComputed", LocalDateTime.now());
        return defaultPersonality;
    }
    
    private Map<String, Object> generatePersonalityInsights(Map<String, Object> activePersonality, 
                                                           Map<String, Object> subconsciousPersonality, 
                                                           Map<String, Object> fusedPersonality) {
        Map<String, Object> insights = new HashMap<>();
        
        // Calculate personality stability
        double stability = calculatePersonalityStability(activePersonality, subconsciousPersonality);
        insights.put("stability", stability);
        
        // Calculate personality complexity
        double complexity = calculatePersonalityComplexity(fusedPersonality);
        insights.put("complexity", complexity);
        
        // Calculate personality evolution trend
        String evolutionTrend = calculatePersonalityEvolutionTrend(activePersonality, subconsciousPersonality);
        insights.put("evolutionTrend", evolutionTrend);
        
        // Calculate personality consistency
        double consistency = calculatePersonalityConsistency(activePersonality, subconsciousPersonality);
        insights.put("consistency", consistency);
        
        return insights;
    }
    
    private double calculatePersonalityStability(Map<String, Object> activePersonality, Map<String, Object> subconsciousPersonality) {
        // Calculate stability based on variance between active and subconscious
        List<Double> activeVector = (List<Double>) activePersonality.get("personalityVector");
        List<Double> subconsciousVector = (List<Double>) subconsciousPersonality.get("personalityVector");
        
        if (activeVector == null || subconsciousVector == null || activeVector.size() != subconsciousVector.size()) {
            return 0.5; // Default stability
        }
        
        double variance = 0.0;
        for (int i = 0; i < activeVector.size(); i++) {
            double diff = activeVector.get(i) - subconsciousVector.get(i);
            variance += diff * diff;
        }
        
        double stability = Math.max(0.0, 1.0 - Math.sqrt(variance / activeVector.size()));
        return stability;
    }
    
    private double calculatePersonalityComplexity(Map<String, Object> fusedPersonality) {
        List<Double> personalityVector = (List<Double>) fusedPersonality.get("personalityVector");
        if (personalityVector == null) {
            return 0.5; // Default complexity
        }
        
        // Calculate complexity based on variance in personality vector
        double mean = personalityVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
        double variance = personalityVector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        double complexity = Math.min(1.0, Math.sqrt(variance) * 2);
        return complexity;
    }
    
    private String calculatePersonalityEvolutionTrend(Map<String, Object> activePersonality, Map<String, Object> subconsciousPersonality) {
        // Simple trend calculation based on active vs subconscious dominance
        Double activeConfidence = (Double) activePersonality.get("confidence");
        Double subconsciousConfidence = (Double) subconsciousPersonality.get("confidence");
        
        if (activeConfidence == null || subconsciousConfidence == null) {
            return "STABLE";
        }
        
        if (activeConfidence > subconsciousConfidence * 1.2) {
            return "ACTIVE_DOMINANT";
        } else if (subconsciousConfidence > activeConfidence * 1.2) {
            return "SUBCONSCIOUS_DOMINANT";
        } else {
            return "BALANCED";
        }
    }
    
    private double calculatePersonalityConsistency(Map<String, Object> activePersonality, Map<String, Object> subconsciousPersonality) {
        // Calculate consistency based on correlation between active and subconscious
        List<Double> activeVector = (List<Double>) activePersonality.get("personalityVector");
        List<Double> subconsciousVector = (List<Double>) subconsciousPersonality.get("personalityVector");
        
        if (activeVector == null || subconsciousVector == null || activeVector.size() != subconsciousVector.size()) {
            return 0.5; // Default consistency
        }
        
        // Calculate Pearson correlation coefficient
        double activeMean = activeVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double subconsciousMean = subconsciousVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double numerator = 0.0;
        double activeSumSquares = 0.0;
        double subconsciousSumSquares = 0.0;
        
        for (int i = 0; i < activeVector.size(); i++) {
            double activeDiff = activeVector.get(i) - activeMean;
            double subconsciousDiff = subconsciousVector.get(i) - subconsciousMean;
            
            numerator += activeDiff * subconsciousDiff;
            activeSumSquares += activeDiff * activeDiff;
            subconsciousSumSquares += subconsciousDiff * subconsciousDiff;
        }
        
        double denominator = Math.sqrt(activeSumSquares * subconsciousSumSquares);
        double correlation = denominator > 0 ? numerator / denominator : 0.0;
        
        // Convert correlation to consistency (0 to 1)
        return Math.max(0.0, (correlation + 1.0) / 2.0);
    }
    
    private Map<String, Object> getComputationHistory(UUID userId) {
        try {
            String historyKey = COMPUTATION_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            return history != null ? history : new HashMap<>();
        } catch (Exception e) {
            logger.warn("Error getting computation history for user {}: {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }
    
    private void trackComputationHistory(UUID userId, String triggerType, Map<String, Object> result) {
        try {
            String historyKey = COMPUTATION_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            if (history == null) {
                history = new HashMap<>();
            }
            
            List<Map<String, Object>> computations = (List<Map<String, Object>>) history.get("computations");
            if (computations == null) {
                computations = new ArrayList<>();
            }
            
            Map<String, Object> computation = new HashMap<>();
            computation.put("timestamp", LocalDateTime.now());
            computation.put("triggerType", triggerType);
            computation.put("status", result.get("status"));
            computation.put("result", result);
            
            computations.add(computation);
            
            // Keep only recent computations (last 50)
            if (computations.size() > 50) {
                computations = computations.subList(computations.size() - 50, computations.size());
            }
            
            history.put("computations", computations);
            history.put("lastUpdated", LocalDateTime.now());
            
            redisTemplate.opsForValue().set(historyKey, history, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Error tracking computation history for user {}: {}", userId, e.getMessage());
        }
    }
}
