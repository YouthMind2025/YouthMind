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
import java.util.stream.Collectors;

/**
 * Service for post-processing and normalization of psychological vectors
 * Implements sophisticated vector processing algorithms
 */
@Service
@Transactional
public class StateVectorProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(StateVectorProcessor.class);
    
    // Vector processing constants
    public static final double NORMALIZATION_THRESHOLD = 0.1;
    public static final double VALIDATION_THRESHOLD = 0.05;
    public static final int VECTOR_DIMENSIONS = 43;
    
    // Redis keys
    private static final String PROCESSED_VECTOR_KEY = "processed_vector:";
    private static final String PROCESSING_HISTORY_KEY = "processing_history:";
    private static final String VECTOR_STATISTICS_KEY = "vector_statistics:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Process psychological vector
     * @param vector Raw psychological vector
     * @param userId User ID for personalization
     * @return Processed vector result
     */
    public Map<String, Object> processVector(List<Double> vector, UUID userId) {
        try {
            logger.debug("Processing psychological vector for user {} with {} dimensions", userId, vector.size());
            
            // Validate input vector
            if (!validateInputVector(vector)) {
                logger.warn("Invalid input vector for user {}", userId);
                return createDefaultProcessedVector(userId);
            }
            
            // Apply normalization
            List<Double> normalizedVector = normalizeVector(vector);
            
            // Apply thresholds
            List<Double> thresholdedVector = applyThresholds(normalizedVector);
            
            // Validate output
            boolean isValid = validateOutput(thresholdedVector);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("originalVector", vector);
            result.put("processedVector", thresholdedVector);
            result.put("normalizedVector", normalizedVector);
            result.put("isValid", isValid);
            result.put("processingMethod", "COMPREHENSIVE_PROCESSING");
            result.put("timestamp", LocalDateTime.now());
            
            // Store processed vector
            storeProcessedVector(userId, result);
            
            logger.debug("Psychological vector processed for user {}: valid={}", userId, isValid);
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultProcessedVector(userId);
        }
    }
    
    /**
     * Normalize psychological vector
     * @param vector Vector to normalize
     * @return Normalized vector
     */
    public List<Double> normalizeVector(List<Double> vector) {
        try {
            logger.debug("Normalizing psychological vector");
            
            if (vector == null || vector.isEmpty()) {
                return createDefaultVector();
            }
            
            // Apply min-max normalization
            List<Double> minMaxNormalized = applyMinMaxNormalization(vector);
            
            // Apply z-score normalization
            List<Double> zScoreNormalized = applyZScoreNormalization(minMaxNormalized);
            
            // Apply sigmoid normalization
            List<Double> sigmoidNormalized = applySigmoidNormalization(zScoreNormalized);
            
            logger.debug("Psychological vector normalized");
            return sigmoidNormalized;
            
        } catch (Exception e) {
            logger.error("Error normalizing vector: {}", e.getMessage(), e);
            return vector != null ? vector : createDefaultVector();
        }
    }
    
    /**
     * Apply thresholds to vector
     * @param vector Vector to apply thresholds to
     * @return Thresholded vector
     */
    public List<Double> applyThresholds(List<Double> vector) {
        try {
            logger.debug("Applying thresholds to psychological vector");
            
            if (vector == null || vector.isEmpty()) {
                return createDefaultVector();
            }
            
            List<Double> thresholdedVector = new ArrayList<>();
            
            for (Double value : vector) {
                // Apply soft thresholding
                double thresholdedValue = applySoftThreshold(value);
                thresholdedVector.add(thresholdedValue);
            }
            
            logger.debug("Thresholds applied to psychological vector");
            return thresholdedVector;
            
        } catch (Exception e) {
            logger.error("Error applying thresholds: {}", e.getMessage(), e);
            return vector != null ? vector : createDefaultVector();
        }
    }
    
    /**
     * Validate output vector
     * @param vector Vector to validate
     * @return Validation result
     */
    public boolean validateOutput(List<Double> vector) {
        try {
            logger.debug("Validating output vector");
            
            // Check dimensions
            if (vector == null || vector.size() != VECTOR_DIMENSIONS) {
                logger.warn("Invalid vector dimensions: expected {}, got {}", VECTOR_DIMENSIONS, 
                    vector != null ? vector.size() : 0);
                return false;
            }
            
            // Check value ranges
            for (int i = 0; i < vector.size(); i++) {
                Double value = vector.get(i);
                if (value == null || value < 0.0 || value > 1.0) {
                    logger.warn("Invalid vector value at index {}: {}", i, value);
                    return false;
                }
            }
            
            // Check for reasonable distribution
            double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (mean < VALIDATION_THRESHOLD || mean > 1.0 - VALIDATION_THRESHOLD) {
                logger.warn("Vector mean too extreme: {}", mean);
                return false;
            }
            
            // Check for reasonable variance
            double variance = vector.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average().orElse(0.0);
            
            if (variance < VALIDATION_THRESHOLD) {
                logger.warn("Vector variance too low: {}", variance);
                return false;
            }
            
            logger.debug("Output vector validation passed");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating output vector: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get processed vector for a user
     * @param userId User ID
     * @return Latest processed vector
     */
    public Map<String, Object> getProcessedVector(UUID userId) {
        try {
            logger.debug("Getting processed vector for user {}", userId);
            
            String storageKey = PROCESSED_VECTOR_KEY + userId;
            Map<String, Object> processedVector = (Map<String, Object>) redisTemplate.opsForValue().get(storageKey);
            
            return processedVector != null ? processedVector : createDefaultProcessedVector(userId);
            
        } catch (Exception e) {
            logger.error("Error getting processed vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultProcessedVector(userId);
        }
    }
    
    /**
     * Get vector processing history for a user
     * @param userId User ID
     * @return Processing history
     */
    public Map<String, Object> getProcessingHistory(UUID userId) {
        try {
            logger.debug("Getting processing history for user {}", userId);
            
            String historyKey = PROCESSING_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            return history != null ? history : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting processing history for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get vector statistics for a user
     * @param userId User ID
     * @return Vector statistics
     */
    public Map<String, Object> getVectorStatistics(UUID userId) {
        try {
            logger.debug("Getting vector statistics for user {}", userId);
            
            String statisticsKey = VECTOR_STATISTICS_KEY + userId;
            Map<String, Object> statistics = (Map<String, Object>) redisTemplate.opsForValue().get(statisticsKey);
            
            return statistics != null ? statistics : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting vector statistics for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private boolean validateInputVector(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return false;
        }
        
        // Check dimensions
        if (vector.size() != VECTOR_DIMENSIONS) {
            return false;
        }
        
        // Check for null values
        for (Double value : vector) {
            if (value == null) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<Double> applyMinMaxNormalization(List<Double> vector) {
        double min = vector.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = vector.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        if (max == min) {
            // All values are the same, return normalized vector
            return vector.stream().map(value -> 0.5).collect(Collectors.toList());
        }
        
        return vector.stream()
            .map(value -> (value - min) / (max - min))
            .collect(Collectors.toList());
    }
    
    private List<Double> applyZScoreNormalization(List<Double> vector) {
        double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = vector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        if (stdDev == 0) {
            // No variation, return normalized vector
            return vector.stream().map(value -> 0.5).collect(Collectors.toList());
        }
        
        return vector.stream()
            .map(value -> (value - mean) / stdDev)
            .collect(Collectors.toList());
    }
    
    private List<Double> applySigmoidNormalization(List<Double> vector) {
        return vector.stream()
            .map(value -> {
                // Apply sigmoid function and scale to [0, 1]
                double sigmoid = 1.0 / (1.0 + Math.exp(-value));
                return sigmoid;
            })
            .collect(Collectors.toList());
    }
    
    private double applySoftThreshold(double value) {
        // Apply soft thresholding to keep values in [0, 1] range
        if (value < NORMALIZATION_THRESHOLD) {
            return NORMALIZATION_THRESHOLD;
        } else if (value > 1.0 - NORMALIZATION_THRESHOLD) {
            return 1.0 - NORMALIZATION_THRESHOLD;
        } else {
            return value;
        }
    }
    
    private void storeProcessedVector(UUID userId, Map<String, Object> processedVector) {
        try {
            logger.debug("Storing processed vector for user {}", userId);
            
            String storageKey = PROCESSED_VECTOR_KEY + userId;
            String historyKey = PROCESSING_HISTORY_KEY + userId;
            String statisticsKey = VECTOR_STATISTICS_KEY + userId;
            
            // Store current processed vector
            redisTemplate.opsForValue().set(storageKey, processedVector, 6, TimeUnit.HOURS);
            
            // Update processing history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            if (history == null) {
                history = new HashMap<>();
            }
            
            List<Map<String, Object>> processingHistory = (List<Map<String, Object>>) history.get("processings");
            if (processingHistory == null) {
                processingHistory = new ArrayList<>();
            }
            
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("timestamp", LocalDateTime.now());
            historyEntry.put("processedVector", processedVector);
            
            processingHistory.add(historyEntry);
            
            // Keep only recent processings (last 50)
            if (processingHistory.size() > 50) {
                processingHistory = processingHistory.subList(processingHistory.size() - 50, processingHistory.size());
            }
            
            history.put("processings", processingHistory);
            history.put("lastUpdated", LocalDateTime.now());
            
            redisTemplate.opsForValue().set(historyKey, history, 30, TimeUnit.DAYS);
            
            // Update vector statistics
            updateVectorStatistics(userId, processedVector, statisticsKey);
            
            logger.debug("Processed vector stored for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error storing processed vector for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    private void updateVectorStatistics(UUID userId, Map<String, Object> processedVector, String statisticsKey) {
        try {
            Map<String, Object> statistics = (Map<String, Object>) redisTemplate.opsForValue().get(statisticsKey);
            if (statistics == null) {
                statistics = new HashMap<>();
            }
            
            List<Double> vector = (List<Double>) processedVector.get("processedVector");
            if (vector != null) {
                // Calculate statistics
                double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double variance = vector.stream()
                    .mapToDouble(value -> Math.pow(value - mean, 2))
                    .average().orElse(0.0);
                double stdDev = Math.sqrt(variance);
                
                double min = vector.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double max = vector.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
                
                // Update statistics
                statistics.put("mean", mean);
                statistics.put("variance", variance);
                statistics.put("stdDev", stdDev);
                statistics.put("min", min);
                statistics.put("max", max);
                statistics.put("lastUpdated", LocalDateTime.now());
                
                // Calculate processing quality metrics
                Map<String, Object> qualityMetrics = calculateQualityMetrics(vector);
                statistics.put("qualityMetrics", qualityMetrics);
                
                redisTemplate.opsForValue().set(statisticsKey, statistics, 30, TimeUnit.DAYS);
            }
            
        } catch (Exception e) {
            logger.error("Error updating vector statistics for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    private Map<String, Object> calculateQualityMetrics(List<Double> vector) {
        Map<String, Object> qualityMetrics = new HashMap<>();
        
        // Calculate distribution quality
        double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = vector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Quality based on variance (moderate variance is good)
        double distributionQuality = Math.max(0.0, 1.0 - Math.abs(variance - 0.1));
        qualityMetrics.put("distributionQuality", distributionQuality);
        
        // Calculate range quality
        double min = vector.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = vector.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double range = max - min;
        
        // Quality based on range (good range is 0.3 to 0.8)
        double rangeQuality = Math.max(0.0, 1.0 - Math.abs(range - 0.55) / 0.25);
        qualityMetrics.put("rangeQuality", rangeQuality);
        
        // Calculate overall quality
        double overallQuality = (distributionQuality + rangeQuality) / 2.0;
        qualityMetrics.put("overallQuality", overallQuality);
        
        return qualityMetrics;
    }
    
    private List<Double> createDefaultVector() {
        List<Double> defaultVector = new ArrayList<>();
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            defaultVector.add(0.5); // Neutral values
        }
        return defaultVector;
    }
    
    private Map<String, Object> createDefaultProcessedVector(UUID userId) {
        Map<String, Object> defaultProcessed = new HashMap<>();
        defaultProcessed.put("userId", userId.toString());
        defaultProcessed.put("originalVector", createDefaultVector());
        defaultProcessed.put("processedVector", createDefaultVector());
        defaultProcessed.put("normalizedVector", createDefaultVector());
        defaultProcessed.put("isValid", false);
        defaultProcessed.put("processingMethod", "DEFAULT");
        defaultProcessed.put("timestamp", LocalDateTime.now());
        return defaultProcessed;
    }
}
