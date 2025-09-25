package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import ai.youthmind.dto.UserPersonalityProfile;

/**
 * State normalization service implementing L2 normalization, softmax normalization,
 * thresholding to suppress weak signals, and vector clipping
 */
@Service
public class StateNormalizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StateNormalizationService.class);
    
    // Normalization constants
    public static final int VECTOR_DIMENSIONS = 43;
    public static final double THRESHOLD_MIN_VALUE = 0.1;
    public static final double CLIPPING_LOWER_BOUND = -2.0;
    public static final double CLIPPING_UPPER_BOUND = 2.0;
    public static final double SOFTMAX_TEMPERATURE = 1.0;
    
    @Autowired
    private StateVectorCalculator stateVectorCalculator;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityFactorCalculator intensityFactorCalculator;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private AIPostAnalysisService aiPostAnalysisService;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    // Redis keys
    private static final String NORMALIZATION_CACHE_KEY = "normalization:";
    private static final String THRESHOLD_CACHE_KEY = "threshold:";
    
    /**
     * Normalize vector using L2 normalization
     * 
     * @param vector Input vector
     * @return L2 normalized vector
     */
    public List<Double> normalizeVector(List<Double> vector) {
        try {
            logger.debug("Applying L2 normalization to vector");
            
            validateVector(vector);
            
            // Calculate L2 norm
            double norm = stateVectorCalculator.calculateL2Norm(vector);
            
            if (norm == 0.0) {
                logger.warn("Vector has zero norm, returning original vector");
                return new ArrayList<>(vector);
            }
            
            // Normalize by dividing each element by the norm
            List<Double> normalized = new ArrayList<>();
            for (Double value : vector) {
                normalized.add(value / norm);
            }
            
            logger.debug("Successfully applied L2 normalization");
            return normalized;
            
        } catch (Exception e) {
            logger.error("Error applying L2 normalization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply L2 normalization", e);
        }
    }
    
    /**
     * Apply softmax normalization to vector
     * 
     * @param vector Input vector
     * @return Softmax normalized vector
     */
    public List<Double> applySoftmaxNormalization(List<Double> vector) {
        try {
            logger.debug("Applying softmax normalization to vector");
            
            validateVector(vector);
            
            // Apply temperature scaling
            List<Double> scaledVector = stateVectorCalculator.multiplyByScalar(vector, SOFTMAX_TEMPERATURE);
            
            // Find maximum value for numerical stability
            double maxValue = scaledVector.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            
            // Calculate exponentials
            List<Double> exponentials = new ArrayList<>();
            for (Double value : scaledVector) {
                exponentials.add(Math.exp(value - maxValue));
            }
            
            // Calculate sum of exponentials
            double sumExp = exponentials.stream().mapToDouble(Double::doubleValue).sum();
            
            if (sumExp == 0.0) {
                logger.warn("Sum of exponentials is zero, returning uniform distribution");
                return createUniformVector();
            }
            
            // Calculate softmax probabilities
            List<Double> softmax = new ArrayList<>();
            for (Double exp : exponentials) {
                softmax.add(exp / sumExp);
            }
            
            logger.debug("Successfully applied softmax normalization");
            return softmax;
            
        } catch (Exception e) {
            logger.error("Error applying softmax normalization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply softmax normalization", e);
        }
    }
    
    /**
     * Apply softmax normalization with custom temperature
     * 
     * @param vector Input vector
     * @param temperature Temperature parameter
     * @return Softmax normalized vector
     */
    public List<Double> applySoftmaxNormalization(List<Double> vector, double temperature) {
        try {
            logger.debug("Applying softmax normalization with temperature: {}", temperature);
            
            validateVector(vector);
            
            if (temperature <= 0.0) {
                throw new IllegalArgumentException("Temperature must be positive");
            }
            
            // Apply temperature scaling
            List<Double> scaledVector = stateVectorCalculator.multiplyByScalar(vector, temperature);
            
            // Find maximum value for numerical stability
            double maxValue = scaledVector.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            
            // Calculate exponentials
            List<Double> exponentials = new ArrayList<>();
            for (Double value : scaledVector) {
                exponentials.add(Math.exp(value - maxValue));
            }
            
            // Calculate sum of exponentials
            double sumExp = exponentials.stream().mapToDouble(Double::doubleValue).sum();
            
            if (sumExp == 0.0) {
                logger.warn("Sum of exponentials is zero, returning uniform distribution");
                return createUniformVector();
            }
            
            // Calculate softmax probabilities
            List<Double> softmax = new ArrayList<>();
            for (Double exp : exponentials) {
                softmax.add(exp / sumExp);
            }
            
            logger.debug("Successfully applied softmax normalization with temperature: {}", temperature);
            return softmax;
            
        } catch (Exception e) {
            logger.error("Error applying softmax normalization with temperature: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply softmax normalization", e);
        }
    }
    
    /**
     * Apply thresholding to suppress weak signals
     * 
     * @param vector Input vector
     * @param threshold Threshold value
     * @return Thresholded vector
     */
    public List<Double> applyThresholding(List<Double> vector, double threshold) {
        try {
            logger.debug("Applying thresholding with threshold: {}", threshold);
            
            validateVector(vector);
            
            List<Double> thresholded = new ArrayList<>();
            for (Double value : vector) {
                if (Math.abs(value) < threshold) {
                    thresholded.add(0.0);
                } else {
                    thresholded.add(value);
                }
            }
            
            logger.debug("Successfully applied thresholding");
            return thresholded;
            
        } catch (Exception e) {
            logger.error("Error applying thresholding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply thresholding", e);
        }
    }
    
    /**
     * Apply thresholding with default threshold
     * 
     * @param vector Input vector
     * @return Thresholded vector
     */
    public List<Double> applyThresholding(List<Double> vector) {
        return applyThresholding(vector, THRESHOLD_MIN_VALUE);
    }
    
    /**
     * Apply vector clipping to prevent value explosion
     * 
     * @param vector Input vector
     * @return Clipped vector
     */
    public List<Double> applyClipping(List<Double> vector) {
        try {
            logger.debug("Applying vector clipping");
            
            validateVector(vector);
            
            List<Double> clipped = stateVectorCalculator.applyClipping(vector, 
                CLIPPING_LOWER_BOUND, CLIPPING_UPPER_BOUND);
            
            logger.debug("Successfully applied vector clipping");
            return clipped;
            
        } catch (Exception e) {
            logger.error("Error applying vector clipping: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply vector clipping", e);
        }
    }
    
    /**
     * Apply custom vector clipping with specified bounds
     * 
     * @param vector Input vector
     * @param lowerBound Lower clipping bound
     * @param upperBound Upper clipping bound
     * @return Clipped vector
     */
    public List<Double> applyClipping(List<Double> vector, double lowerBound, double upperBound) {
        try {
            logger.debug("Applying custom vector clipping with bounds [{}, {}]", lowerBound, upperBound);
            
            validateVector(vector);
            
            List<Double> clipped = stateVectorCalculator.applyClipping(vector, lowerBound, upperBound);
            
            logger.debug("Successfully applied custom vector clipping");
            return clipped;
            
        } catch (Exception e) {
            logger.error("Error applying custom vector clipping: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply custom vector clipping", e);
        }
    }
    
    /**
     * Apply comprehensive normalization pipeline
     * 1. Clipping to prevent explosion
     * 2. Thresholding to suppress weak signals
     * 3. L2 normalization
     * 
     * @param vector Input vector
     * @return Fully normalized vector
     */
    public List<Double> applyComprehensiveNormalization(List<Double> vector) {
        try {
            logger.debug("Applying comprehensive normalization pipeline");
            
            validateVector(vector);
            
            // Step 1: Apply clipping to prevent explosion
            List<Double> clipped = applyClipping(vector);
            
            // Step 2: Apply thresholding to suppress weak signals
            List<Double> thresholded = applyThresholding(clipped);
            
            // Step 3: Apply L2 normalization
            List<Double> normalized = normalizeVector(thresholded);
            
            logger.debug("Successfully applied comprehensive normalization pipeline");
            return normalized;
            
        } catch (Exception e) {
            logger.error("Error applying comprehensive normalization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply comprehensive normalization", e);
        }
    }
    
    /**
     * Apply softmax normalization pipeline
     * 1. Clipping to prevent explosion
     * 2. Softmax normalization
     * 
     * @param vector Input vector
     * @return Softmax normalized vector
     */
    public List<Double> applySoftmaxNormalizationPipeline(List<Double> vector) {
        try {
            logger.debug("Applying softmax normalization pipeline");
            
            validateVector(vector);
            
            // Step 1: Apply clipping to prevent explosion
            List<Double> clipped = applyClipping(vector);
            
            // Step 2: Apply softmax normalization
            List<Double> softmax = applySoftmaxNormalization(clipped);
            
            logger.debug("Successfully applied softmax normalization pipeline");
            return softmax;
            
        } catch (Exception e) {
            logger.error("Error applying softmax normalization pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply softmax normalization pipeline", e);
        }
    }
    
    /**
     * Apply adaptive normalization based on vector characteristics
     * 
     * @param vector Input vector
     * @return Adaptively normalized vector
     */
    public List<Double> applyAdaptiveNormalization(List<Double> vector) {
        try {
            logger.debug("Applying adaptive normalization");
            
            validateVector(vector);
            
            // Calculate vector statistics
            double mean = stateVectorCalculator.calculateMean(vector);
            double stdDev = stateVectorCalculator.calculateStandardDeviation(vector);
            double magnitude = stateVectorCalculator.calculateL2Norm(vector);
            
            // Choose normalization strategy based on vector characteristics
            if (magnitude > 5.0) {
                // High magnitude - apply clipping first, then L2 normalization
                logger.debug("High magnitude vector detected, applying clipping + L2 normalization");
                List<Double> clipped = applyClipping(vector);
                return normalizeVector(clipped);
            } else if (stdDev < 0.1) {
                // Low variance - apply softmax normalization
                logger.debug("Low variance vector detected, applying softmax normalization");
                return applySoftmaxNormalization(vector);
            } else {
                // Normal case - apply comprehensive normalization
                logger.debug("Normal vector detected, applying comprehensive normalization");
                return applyComprehensiveNormalization(vector);
            }
            
        } catch (Exception e) {
            logger.error("Error applying adaptive normalization: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply adaptive normalization", e);
        }
    }
    
    /**
     * Check if vector needs normalization
     * 
     * @param vector Input vector
     * @return true if vector needs normalization
     */
    public boolean needsNormalization(List<Double> vector) {
        try {
            validateVector(vector);
            
            double magnitude = stateVectorCalculator.calculateL2Norm(vector);
            double maxValue = vector.stream().mapToDouble(Math::abs).max().orElse(0.0);
            
            // Check if magnitude is too high or if any value exceeds bounds
            return magnitude > 2.0 || maxValue > CLIPPING_UPPER_BOUND || maxValue < CLIPPING_LOWER_BOUND;
            
        } catch (Exception e) {
            logger.error("Error checking if vector needs normalization: {}", e.getMessage(), e);
            return true; // Assume it needs normalization if we can't check
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private void validateVector(List<Double> vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be null");
        }
        if (vector.size() != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Vector must have exactly " + VECTOR_DIMENSIONS + " dimensions");
        }
    }
    
    private List<Double> createUniformVector() {
        List<Double> uniform = new ArrayList<>();
        double uniformValue = 1.0 / VECTOR_DIMENSIONS;
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            uniform.add(uniformValue);
        }
        return uniform;
    }
    
    /**
     * Enhanced normalization with AI analysis and personality-based adjustments
     */
    public List<Double> normalizeVectorEnhanced(List<Double> vector, UUID userId, String content) {
        try {
            logger.debug("Applying enhanced normalization for user {}", userId);
            
            // Check cache first
            String cacheKey = NORMALIZATION_CACHE_KEY + userId + ":" + vector.hashCode();
            @SuppressWarnings("unchecked")
            List<Double> cachedResult = (List<Double>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                logger.debug("Using cached enhanced normalization");
                return cachedResult;
            }
            
            // Start with basic L2 normalization
            List<Double> normalizedVector = normalizeVector(vector);
            
            // Apply AI analysis-based adjustments
            if (content != null && !content.isEmpty()) {
                normalizedVector = applyAIAnalysisNormalization(normalizedVector, content, userId);
            }
            
            // Apply personality-based normalization
            normalizedVector = applyPersonalityNormalization(normalizedVector, userId);
            
            // Apply Buddhist factor-based normalization
            normalizedVector = applyBuddhistFactorNormalization(normalizedVector, userId);
            
            // Apply intensity-based normalization
            normalizedVector = applyIntensityNormalization(normalizedVector, userId);
            
            // Final validation and clipping
            normalizedVector = clipVector(normalizedVector);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, normalizedVector, 1, TimeUnit.HOURS);
            
            logger.debug("Applied enhanced normalization with {} dimensions", normalizedVector.size());
            return normalizedVector;
            
        } catch (Exception e) {
            logger.error("Error applying enhanced normalization: {}", e.getMessage(), e);
            return normalizeVector(vector); // Fallback to basic normalization
        }
    }
    
    /**
     * Apply AI analysis-based normalization adjustments
     */
    private List<Double> applyAIAnalysisNormalization(List<Double> vector, String content, UUID userId) {
        try {
            // Get AI analysis
            Map<String, Object> aiAnalysis = aiPostAnalysisService.processPsychologicalContent(content, userId);
            
            // Extract processed vector insights
            Map<String, Object> processedVector = (Map<String, Object>) aiAnalysis.get("processedVector");
            if (processedVector != null) {
                @SuppressWarnings("unchecked")
                List<Double> processedVectorData = (List<Double>) processedVector.get("processedVector");
                if (processedVectorData != null && processedVectorData.size() == vector.size()) {
                    // Blend with processed vector (10% weight)
                    for (int i = 0; i < vector.size(); i++) {
                        vector.set(i, vector.get(i) * 0.9 + processedVectorData.get(i) * 0.1);
                    }
                }
            }
            
            // Extract Buddhist factors for additional adjustments
            Map<String, Object> buddhistFactors = (Map<String, Object>) aiAnalysis.get("buddhistFactors");
            if (buddhistFactors != null) {
                @SuppressWarnings("unchecked")
                Map<String, Double> factorScores = (Map<String, Double>) buddhistFactors.get("factorScores");
                if (factorScores != null) {
                    // Apply factor-specific normalization
                    applyFactorSpecificNormalization(vector, factorScores);
                }
            }
            
            return vector;
            
        } catch (Exception e) {
            logger.warn("Error applying AI analysis normalization: {}", e.getMessage());
            return vector;
        }
    }
    
    /**
     * Apply personality-based normalization adjustments
     */
    private List<Double> applyPersonalityNormalization(List<Double> vector, UUID userId) {
        try {
            // Get user personality profile
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double extraversion = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            double conscientiousness = preferences != null ? (Double) preferences.getOrDefault("conscientiousness", 0.5) : 0.5;
            double agreeableness = preferences != null ? (Double) preferences.getOrDefault("agreeableness", 0.5) : 0.5;
            
            // Apply personality-based normalization factors
            List<Double> adjustedVector = new ArrayList<>(vector);
            
            // Openness affects cognitive dimensions (0-4) - more open = less aggressive normalization
            double opennessFactor = 1.0 + (openness - 0.5) * 0.2;
            for (int i = 0; i < 5; i++) {
                adjustedVector.set(i, adjustedVector.get(i) * opennessFactor);
            }
            
            // Neuroticism affects emotional dimensions (1-2, 7-8) - more neurotic = more aggressive normalization
            double neuroticismFactor = 1.0 - (neuroticism - 0.5) * 0.3;
            List<Integer> emotionalDims = List.of(1, 2, 7, 8);
            for (int dim : emotionalDims) {
                if (dim < adjustedVector.size()) {
                    adjustedVector.set(dim, adjustedVector.get(dim) * neuroticismFactor);
                }
            }
            
            // Extraversion affects social dimensions (5-6) - more extraverted = less aggressive normalization
            double extraversionFactor = 1.0 + (extraversion - 0.5) * 0.15;
            for (int i = 5; i < 7; i++) {
                if (i < adjustedVector.size()) {
                    adjustedVector.set(i, adjustedVector.get(i) * extraversionFactor);
                }
            }
            
            // Conscientiousness affects all dimensions - more conscientious = more conservative normalization
            double conscientiousnessFactor = 1.0 - (conscientiousness - 0.5) * 0.1;
            for (int i = 0; i < adjustedVector.size(); i++) {
                adjustedVector.set(i, adjustedVector.get(i) * conscientiousnessFactor);
            }
            
            return adjustedVector;
            
        } catch (Exception e) {
            logger.warn("Error applying personality normalization: {}", e.getMessage());
            return vector;
        }
    }
    
    /**
     * Apply Buddhist factor-based normalization
     */
    private List<Double> applyBuddhistFactorNormalization(List<Double> vector, UUID userId) {
        try {
            // Get Buddhist factor analysis
            Map<String, Object> buddhistAnalysis = buddhistFactorAnalyzer.analyzeFactors("", userId);
            @SuppressWarnings("unchecked")
            Map<String, Double> factorScores = (Map<String, Double>) buddhistAnalysis.get("factorScores");
            
            if (factorScores == null) {
                return vector;
            }
            
            List<Double> adjustedVector = new ArrayList<>(vector);
            
            // Apply factor-specific normalization based on Buddhist mental factors
            applyFactorSpecificNormalization(adjustedVector, factorScores);
            
            return adjustedVector;
            
        } catch (Exception e) {
            logger.warn("Error applying Buddhist factor normalization: {}", e.getMessage());
            return vector;
        }
    }
    
    /**
     * Apply factor-specific normalization based on factor scores
     */
    private void applyFactorSpecificNormalization(List<Double> vector, Map<String, Double> factorScores) {
        try {
            // Faith factor (dimension 5) - high faith = more conservative normalization
            if (factorScores.containsKey("faith") && 5 < vector.size()) {
                double faithScore = factorScores.get("faith");
                double faithFactor = 1.0 - faithScore * 0.2;
                vector.set(5, vector.get(5) * faithFactor);
            }
            
            // Vigor factor (dimension 6) - high vigor = less aggressive normalization
            if (factorScores.containsKey("vigor") && 6 < vector.size()) {
                double vigorScore = factorScores.get("vigor");
                double vigorFactor = 1.0 + vigorScore * 0.15;
                vector.set(6, vector.get(6) * vigorFactor);
            }
            
            // Greed factor (dimension 16) - high greed = more aggressive normalization
            if (factorScores.containsKey("greed") && 16 < vector.size()) {
                double greedScore = factorScores.get("greed");
                double greedFactor = 1.0 - greedScore * 0.25;
                vector.set(16, vector.get(16) * greedFactor);
            }
            
            // Anger factor (dimension 17) - high anger = more aggressive normalization
            if (factorScores.containsKey("anger") && 17 < vector.size()) {
                double angerScore = factorScores.get("anger");
                double angerFactor = 1.0 - angerScore * 0.3;
                vector.set(17, vector.get(17) * angerFactor);
            }
            
            // Arrogance factor (dimension 18) - high arrogance = more aggressive normalization
            if (factorScores.containsKey("arrogance") && 18 < vector.size()) {
                double arroganceScore = factorScores.get("arrogance");
                double arroganceFactor = 1.0 - arroganceScore * 0.2;
                vector.set(18, vector.get(18) * arroganceFactor);
            }
            
            // Doubt factor (dimension 20) - high doubt = more conservative normalization
            if (factorScores.containsKey("doubt") && 20 < vector.size()) {
                double doubtScore = factorScores.get("doubt");
                double doubtFactor = 1.0 - doubtScore * 0.15;
                vector.set(20, vector.get(20) * doubtFactor);
            }
            
        } catch (Exception e) {
            logger.warn("Error applying factor-specific normalization: {}", e.getMessage());
        }
    }
    
    /**
     * Apply intensity-based normalization
     */
    private List<Double> applyIntensityNormalization(List<Double> vector, UUID userId) {
        try {
            // Calculate intensity factor for the vector
            double intensityFactor = intensityFactorCalculator.calculateBuddhistFactorIntensity(vector, userId);
            
            // Apply intensity-based normalization
            // Higher intensity = more conservative normalization to preserve signal strength
            double intensityNormalizationFactor = 1.0 - (intensityFactor - 0.5) * 0.2;
            
            List<Double> adjustedVector = new ArrayList<>(vector);
            for (int i = 0; i < adjustedVector.size(); i++) {
                adjustedVector.set(i, adjustedVector.get(i) * intensityNormalizationFactor);
            }
            
            return adjustedVector;
            
        } catch (Exception e) {
            logger.warn("Error applying intensity normalization: {}", e.getMessage());
            return vector;
        }
    }
    
    /**
     * Calculate adaptive threshold based on user characteristics
     */
    public double calculateAdaptiveThreshold(UUID userId) {
        try {
            // Check cache first
            String cacheKey = THRESHOLD_CACHE_KEY + userId;
            Double cachedThreshold = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedThreshold != null) {
                return cachedThreshold;
            }
            
            // Get user personality profile
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double conscientiousness = preferences != null ? (Double) preferences.getOrDefault("conscientiousness", 0.5) : 0.5;
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            
            // Calculate adaptive threshold based on personality
            // Higher neuroticism = higher threshold (more filtering)
            // Higher conscientiousness = higher threshold (more conservative)
            // Higher openness = lower threshold (more permissive)
            double adaptiveThreshold = THRESHOLD_MIN_VALUE + 
                                     (neuroticism * 0.1) + 
                                     (conscientiousness * 0.05) - 
                                     (openness * 0.03);
            
            // Clamp to reasonable range
            adaptiveThreshold = Math.max(0.05, Math.min(0.3, adaptiveThreshold));
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, adaptiveThreshold, 2, TimeUnit.HOURS);
            
            logger.debug("Calculated adaptive threshold: {} for user {}", adaptiveThreshold, userId);
            return adaptiveThreshold;
            
        } catch (Exception e) {
            logger.warn("Error calculating adaptive threshold: {}", e.getMessage());
            return THRESHOLD_MIN_VALUE;
        }
    }
    
    /**
     * Apply adaptive thresholding based on user characteristics
     */
    public List<Double> applyAdaptiveThresholding(List<Double> vector, UUID userId) {
        try {
            double adaptiveThreshold = calculateAdaptiveThreshold(userId);
            return applyThresholding(vector, adaptiveThreshold);
            
        } catch (Exception e) {
            logger.warn("Error applying adaptive thresholding: {}", e.getMessage());
            return applyThresholding(vector, THRESHOLD_MIN_VALUE);
        }
    }
    
    /**
     * Get normalization statistics for monitoring
     */
    public Map<String, Object> getNormalizationStatistics(List<Double> originalVector, List<Double> normalizedVector, UUID userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Basic statistics
            stats.put("originalMagnitude", stateVectorCalculator.calculateL2Norm(originalVector));
            stats.put("normalizedMagnitude", stateVectorCalculator.calculateL2Norm(normalizedVector));
            stats.put("originalMaxValue", originalVector.stream().mapToDouble(Math::abs).max().orElse(0.0));
            stats.put("normalizedMaxValue", normalizedVector.stream().mapToDouble(Math::abs).max().orElse(0.0));
            stats.put("originalMinValue", originalVector.stream().mapToDouble(Math::abs).min().orElse(0.0));
            stats.put("normalizedMinValue", normalizedVector.stream().mapToDouble(Math::abs).min().orElse(0.0));
            
            // Calculate change metrics
            double normalizedMagnitude = (Double) stats.get("normalizedMagnitude");
            double originalMagnitude = (Double) stats.get("originalMagnitude");
            double magnitudeChange = Math.abs(normalizedMagnitude - originalMagnitude);
            stats.put("magnitudeChange", magnitudeChange);
            stats.put("magnitudeChangePercent", (magnitudeChange / originalMagnitude) * 100);
            
            // User-specific metrics
            stats.put("userId", userId.toString());
            stats.put("adaptiveThreshold", calculateAdaptiveThreshold(userId));
            stats.put("normalizedAt", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error calculating normalization statistics: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Clip vector values to valid range [0, 1]
     * 
     * @param vector Input vector
     * @return Clipped vector
     */
    private List<Double> clipVector(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return vector;
        }
        
        List<Double> clippedVector = new ArrayList<>();
        for (Double value : vector) {
            // Clip to [0, 1] range
            double clippedValue = Math.max(0.0, Math.min(1.0, value));
            clippedVector.add(clippedValue);
        }
        
        return clippedVector;
    }
}
