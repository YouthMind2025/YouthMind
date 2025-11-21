package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for fusing active and subconscious personalities
 * Combines λ₁·μ_post + λ₂·V_shadow with sophisticated fusion algorithms
 */
@Service
@Transactional
public class PersonalityFusionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityFusionService.class);
    
    // Fusion algorithm constants
    private static final double DEFAULT_LAMBDA_ACTIVE = 0.7; // Default λ₁ weight for active personality
    private static final double DEFAULT_LAMBDA_SUBCONSCIOUS = 0.3; // Default λ₂ weight for subconscious personality
    private static final double FUSION_THRESHOLD = 0.1; // Minimum threshold for fusion validation
    private static final double CONFIDENCE_WEIGHT_FACTOR = 0.8; // Factor for confidence-based weighting
    
    // Redis keys
    private static final String FUSION_HISTORY_KEY = "fusion_history:";
    private static final String LAMBDA_WEIGHTS_KEY = "lambda_weights:";
    private static final String FUSION_VALIDATION_KEY = "fusion_validation:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate lambda weights for personality fusion
     * @param userId User ID
     * @param activePersonality Active personality vector
     * @param subconsciousPersonality Subconscious personality vector
     * @return Lambda weights map
     */
    public Map<String, Double> calculateLambdaWeights(UUID userId, Map<String, Object> activePersonality, 
                                                     Map<String, Object> subconsciousPersonality) {
        try {
            logger.debug("Calculating lambda weights for user {}", userId);
            
            // Check for cached weights
            String cacheKey = LAMBDA_WEIGHTS_KEY + userId;
            Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData != null) {
                Object lastCalculatedObj = cachedData.get("lastCalculated");
                if (lastCalculatedObj instanceof LocalDateTime) {
                    LocalDateTime lastCalculated = (LocalDateTime) lastCalculatedObj;
                    if (java.time.temporal.ChronoUnit.HOURS.between(lastCalculated, LocalDateTime.now()) < 6) {
                        logger.debug("Using cached lambda weights for user {}", userId);
                        // Extract only the weight values, excluding the timestamp
                        Map<String, Double> cachedWeights = new HashMap<>();
                        cachedData.forEach((key, value) -> {
                            if (!"lastCalculated".equals(key) && value instanceof Double) {
                                cachedWeights.put(key, (Double) value);
                            }
                        });
                        return cachedWeights;
                    }
                }
            }
            
            // Calculate dynamic lambda weights based on personality characteristics
            Map<String, Double> weights = calculateDynamicLambdaWeights(activePersonality, subconsciousPersonality);
            
            // Store weights in cache (create a separate map for caching with timestamp)
            Map<String, Object> cacheData = new HashMap<>(weights);
            cacheData.put("lastCalculated", LocalDateTime.now());
            redisTemplate.opsForValue().set(cacheKey, cacheData, 6, TimeUnit.HOURS);
            
            logger.debug("Lambda weights calculated for user {}: {}", userId, weights);
            return weights;
            
        } catch (Exception e) {
            logger.error("Error calculating lambda weights for user {}: {}", userId, e.getMessage(), e);
            return createDefaultLambdaWeights();
        }
    }
    
    /**
     * Fuse active and subconscious personalities
     * @param activePersonality Active personality vector (μ_post)
     * @param subconsciousPersonality Subconscious personality vector (V_shadow)
     * @param lambdaWeights Lambda weights for fusion
     * @return Fused personality result
     */
    public Map<String, Object> fusePersonalities(Map<String, Object> activePersonality, 
                                                Map<String, Object> subconsciousPersonality, 
                                                Map<String, Double> lambdaWeights) {
        try {
            logger.debug("Fusing personalities with weights: {}", lambdaWeights);
            
            // Extract personality vectors
            List<Double> activeVector = (List<Double>) activePersonality.get("personalityVector");
            List<Double> subconsciousVector = (List<Double>) subconsciousPersonality.get("personalityVector");
            
            if (activeVector == null || subconsciousVector == null) {
                logger.warn("Missing personality vectors for fusion");
                return createDefaultFusedPersonality();
            }
            
            // Get lambda weights
            double lambdaActive = lambdaWeights.getOrDefault("lambdaActive", DEFAULT_LAMBDA_ACTIVE);
            double lambdaSubconscious = lambdaWeights.getOrDefault("lambdaSubconscious", DEFAULT_LAMBDA_SUBCONSCIOUS);
            
            // Normalize lambda weights
            double totalLambda = lambdaActive + lambdaSubconscious;
            if (totalLambda > 0) {
                lambdaActive = lambdaActive / totalLambda;
                lambdaSubconscious = lambdaSubconscious / totalLambda;
            }
            
            // Fuse personality vectors: λ₁·μ_post + λ₂·V_shadow
            List<Double> fusedVector = fusePersonalityVectors(activeVector, subconsciousVector, lambdaActive, lambdaSubconscious);
            
            // Calculate fused confidence
            double fusedConfidence = calculateFusedConfidence(activePersonality, subconsciousPersonality, lambdaActive, lambdaSubconscious);
            
            // Create fusion result
            Map<String, Object> fusedPersonality = new HashMap<>();
            fusedPersonality.put("personalityVector", fusedVector);
            fusedPersonality.put("confidence", fusedConfidence);
            fusedPersonality.put("fusionMethod", "WEIGHTED_LINEAR");
            fusedPersonality.put("lambdaActive", lambdaActive);
            fusedPersonality.put("lambdaSubconscious", lambdaSubconscious);
            fusedPersonality.put("activeConfidence", activePersonality.get("confidence"));
            fusedPersonality.put("subconsciousConfidence", subconsciousPersonality.get("confidence"));
            fusedPersonality.put("timestamp", LocalDateTime.now());
            
            logger.debug("Personalities fused successfully: {}", fusedPersonality);
            return fusedPersonality;
            
        } catch (Exception e) {
            logger.error("Error fusing personalities: {}", e.getMessage(), e);
            return createDefaultFusedPersonality();
        }
    }
    
    /**
     * Validate fusion result
     * @param fusedPersonality Fused personality result
     * @return Validation result
     */
    public boolean validateFusion(Map<String, Object> fusedPersonality) {
        try {
            logger.debug("Validating fusion result");
            
            // Check if fused personality has required fields
            if (fusedPersonality == null) {
                logger.warn("Fusion validation failed: null fused personality");
                return false;
            }
            
            List<Double> personalityVector = (List<Double>) fusedPersonality.get("personalityVector");
            if (personalityVector == null || personalityVector.isEmpty()) {
                logger.warn("Fusion validation failed: missing personality vector");
                return false;
            }
            
            // Validate vector dimensions
            if (personalityVector.size() != 5) {
                logger.warn("Fusion validation failed: invalid vector dimensions: {}", personalityVector.size());
                return false;
            }
            
            // Validate vector values are in valid range [0, 1]
            for (int i = 0; i < personalityVector.size(); i++) {
                double value = personalityVector.get(i);
                if (value < 0.0 || value > 1.0) {
                    logger.warn("Fusion validation failed: invalid vector value at index {}: {}", i, value);
                    return false;
                }
            }
            
            // Validate confidence score
            Double confidence = (Double) fusedPersonality.get("confidence");
            if (confidence == null || confidence < 0.0 || confidence > 1.0) {
                logger.warn("Fusion validation failed: invalid confidence score: {}", confidence);
                return false;
            }
            
            // Validate lambda weights
            Double lambdaActive = (Double) fusedPersonality.get("lambdaActive");
            Double lambdaSubconscious = (Double) fusedPersonality.get("lambdaSubconscious");
            
            if (lambdaActive == null || lambdaSubconscious == null) {
                logger.warn("Fusion validation failed: missing lambda weights");
                return false;
            }
            
            if (Math.abs(lambdaActive + lambdaSubconscious - 1.0) > 0.01) {
                logger.warn("Fusion validation failed: lambda weights don't sum to 1.0: {} + {} = {}", 
                    lambdaActive, lambdaSubconscious, lambdaActive + lambdaSubconscious);
                return false;
            }
            
            // Check for reasonable fusion quality
            boolean isValidQuality = validateFusionQuality(fusedPersonality);
            if (!isValidQuality) {
                logger.warn("Fusion validation failed: poor fusion quality");
                return false;
            }
            
            logger.debug("Fusion validation passed");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating fusion: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get fusion history for a user
     * @param userId User ID
     * @return Fusion history
     */
    public Map<String, Object> getFusionHistory(UUID userId) {
        try {
            logger.debug("Getting fusion history for user {}", userId);
            
            String historyKey = FUSION_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            return history != null ? history : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting fusion history for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<String, Double> calculateDynamicLambdaWeights(Map<String, Object> activePersonality, 
                                                             Map<String, Object> subconsciousPersonality) {
        Map<String, Double> weights = new HashMap<>();
        
        // Get confidence scores
        Double activeConfidence = (Double) activePersonality.get("confidence");
        Double subconsciousConfidence = (Double) subconsciousPersonality.get("confidence");
        
        if (activeConfidence == null) activeConfidence = 0.5;
        if (subconsciousConfidence == null) subconsciousConfidence = 0.5;
        
        // Calculate dynamic weights based on confidence and other factors
        double confidenceRatio = activeConfidence / (activeConfidence + subconsciousConfidence);
        
        // Adjust weights based on confidence
        double lambdaActive = DEFAULT_LAMBDA_ACTIVE * confidenceRatio * CONFIDENCE_WEIGHT_FACTOR + 
                             DEFAULT_LAMBDA_ACTIVE * (1.0 - CONFIDENCE_WEIGHT_FACTOR);
        
        double lambdaSubconscious = DEFAULT_LAMBDA_SUBCONSCIOUS * (1.0 - confidenceRatio) * CONFIDENCE_WEIGHT_FACTOR + 
                                   DEFAULT_LAMBDA_SUBCONSCIOUS * (1.0 - CONFIDENCE_WEIGHT_FACTOR);
        
        // Apply additional adjustments based on personality characteristics
        lambdaActive = adjustLambdaBasedOnPersonalityCharacteristics(lambdaActive, activePersonality, "ACTIVE");
        lambdaSubconscious = adjustLambdaBasedOnPersonalityCharacteristics(lambdaSubconscious, subconsciousPersonality, "SUBCONSCIOUS");
        
        // Ensure weights sum to 1.0
        double totalWeight = lambdaActive + lambdaSubconscious;
        if (totalWeight > 0) {
            lambdaActive = lambdaActive / totalWeight;
            lambdaSubconscious = lambdaSubconscious / totalWeight;
        }
        
        weights.put("lambdaActive", lambdaActive);
        weights.put("lambdaSubconscious", lambdaSubconscious);
        weights.put("confidenceRatio", confidenceRatio);
        
        return weights;
    }
    
    private double adjustLambdaBasedOnPersonalityCharacteristics(double baseLambda, Map<String, Object> personality, String type) {
        // Adjust lambda based on personality vector characteristics
        List<Double> personalityVector = (List<Double>) personality.get("personalityVector");
        if (personalityVector == null) {
            return baseLambda;
        }
        
        // Calculate personality characteristics
        double personalityStability = calculatePersonalityStability(personalityVector);
        double personalityComplexity = calculatePersonalityComplexity(personalityVector);
        
        // Adjust lambda based on characteristics
        double adjustment = 0.0;
        
        if ("ACTIVE".equals(type)) {
            // Higher active weight for more stable personalities
            adjustment += personalityStability * 0.1;
            // Lower active weight for very complex personalities
            adjustment -= personalityComplexity * 0.05;
        } else if ("SUBCONSCIOUS".equals(type)) {
            // Higher subconscious weight for more complex personalities
            adjustment += personalityComplexity * 0.1;
            // Lower subconscious weight for very stable personalities
            adjustment -= personalityStability * 0.05;
        }
        
        return Math.max(0.1, Math.min(0.9, baseLambda + adjustment));
    }
    
    private double calculatePersonalityStability(List<Double> personalityVector) {
        // Calculate stability based on variance in personality vector
        double mean = personalityVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
        double variance = personalityVector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Lower variance = higher stability
        return Math.max(0.0, 1.0 - Math.sqrt(variance));
    }
    
    private double calculatePersonalityComplexity(List<Double> personalityVector) {
        // Calculate complexity based on range and distribution of values
        double min = personalityVector.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = personalityVector.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double range = max - min;
        
        // Calculate distribution complexity
        double mean = personalityVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
        double variance = personalityVector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Combine range and variance for complexity measure
        return Math.min(1.0, (range + Math.sqrt(variance)) / 2.0);
    }
    
    private List<Double> fusePersonalityVectors(List<Double> activeVector, List<Double> subconsciousVector, 
                                              double lambdaActive, double lambdaSubconscious) {
        List<Double> fusedVector = new ArrayList<>();
        
        // Ensure vectors have same dimensions
        int dimensions = Math.min(activeVector.size(), subconsciousVector.size());
        
        for (int i = 0; i < dimensions; i++) {
            double activeValue = activeVector.get(i);
            double subconsciousValue = subconsciousVector.get(i);
            
            // Linear fusion: λ₁·μ_post + λ₂·V_shadow
            double fusedValue = lambdaActive * activeValue + lambdaSubconscious * subconsciousValue;
            
            // Ensure value is in valid range [0, 1]
            fusedValue = Math.max(0.0, Math.min(1.0, fusedValue));
            
            fusedVector.add(fusedValue);
        }
        
        return fusedVector;
    }
    
    private double calculateFusedConfidence(Map<String, Object> activePersonality, 
                                         Map<String, Object> subconsciousPersonality, 
                                         double lambdaActive, double lambdaSubconscious) {
        // Calculate fused confidence based on individual confidences and lambda weights
        Double activeConfidence = (Double) activePersonality.get("confidence");
        Double subconsciousConfidence = (Double) subconsciousPersonality.get("confidence");
        
        if (activeConfidence == null) activeConfidence = 0.5;
        if (subconsciousConfidence == null) subconsciousConfidence = 0.5;
        
        // Weighted average of confidences
        double fusedConfidence = lambdaActive * activeConfidence + lambdaSubconscious * subconsciousConfidence;
        
        // Apply fusion penalty for very different personalities
        double personalityDifference = calculatePersonalityDifference(activePersonality, subconsciousPersonality);
        double fusionPenalty = personalityDifference * 0.1;
        
        fusedConfidence = Math.max(0.1, fusedConfidence - fusionPenalty);
        
        return Math.min(1.0, fusedConfidence);
    }
    
    private double calculatePersonalityDifference(Map<String, Object> activePersonality, Map<String, Object> subconsciousPersonality) {
        List<Double> activeVector = (List<Double>) activePersonality.get("personalityVector");
        List<Double> subconsciousVector = (List<Double>) subconsciousPersonality.get("personalityVector");
        
        if (activeVector == null || subconsciousVector == null) {
            return 0.5; // Default difference
        }
        
        // Calculate Euclidean distance between personality vectors
        double distance = 0.0;
        int dimensions = Math.min(activeVector.size(), subconsciousVector.size());
        
        for (int i = 0; i < dimensions; i++) {
            double diff = activeVector.get(i) - subconsciousVector.get(i);
            distance += diff * diff;
        }
        
        return Math.sqrt(distance) / Math.sqrt(dimensions);
    }
    
    private boolean validateFusionQuality(Map<String, Object> fusedPersonality) {
        // Validate fusion quality based on various criteria
        
        // Check confidence threshold
        Double confidence = (Double) fusedPersonality.get("confidence");
        if (confidence == null || confidence < FUSION_THRESHOLD) {
            return false;
        }
        
        // Check lambda weight balance
        Double lambdaActive = (Double) fusedPersonality.get("lambdaActive");
        Double lambdaSubconscious = (Double) fusedPersonality.get("lambdaSubconscious");
        
        if (lambdaActive == null || lambdaSubconscious == null) {
            return false;
        }
        
        // Check for extreme weight imbalance
        if (lambdaActive < 0.1 || lambdaActive > 0.9 || 
            lambdaSubconscious < 0.1 || lambdaSubconscious > 0.9) {
            return false;
        }
        
        // Check personality vector quality
        List<Double> personalityVector = (List<Double>) fusedPersonality.get("personalityVector");
        if (personalityVector == null) {
            return false;
        }
        
        // Check for reasonable vector distribution
        double mean = personalityVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean < 0.1 || mean > 0.9) {
            return false; // Too extreme
        }
        
        return true;
    }
    
    private Map<String, Double> createDefaultLambdaWeights() {
        Map<String, Double> defaultWeights = new HashMap<>();
        defaultWeights.put("lambdaActive", DEFAULT_LAMBDA_ACTIVE);
        defaultWeights.put("lambdaSubconscious", DEFAULT_LAMBDA_SUBCONSCIOUS);
        defaultWeights.put("confidenceRatio", 0.5);
        return defaultWeights;
    }
    
    private Map<String, Object> createDefaultFusedPersonality() {
        Map<String, Object> defaultFused = new HashMap<>();
        defaultFused.put("personalityVector", Arrays.asList(0.4, 0.4, 0.4, 0.4, 0.4));
        defaultFused.put("confidence", 0.1);
        defaultFused.put("fusionMethod", "DEFAULT");
        defaultFused.put("lambdaActive", DEFAULT_LAMBDA_ACTIVE);
        defaultFused.put("lambdaSubconscious", DEFAULT_LAMBDA_SUBCONSCIOUS);
        defaultFused.put("timestamp", LocalDateTime.now());
        return defaultFused;
    }
}
