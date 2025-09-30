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
 * Service for predicting psychological impact intensity
 * Implements sophisticated intensity prediction algorithms
 */
@Service
@Transactional
public class IntensityPredictor {
    
    private static final Logger logger = LoggerFactory.getLogger(IntensityPredictor.class);
    
    // Intensity prediction constants
    public static final double HIGH_INTENSITY_THRESHOLD = 0.7;
    public static final double MODERATE_INTENSITY_THRESHOLD = 0.4;
    public static final double LOW_INTENSITY_THRESHOLD = 0.2;
    
    // Redis keys
    private static final String INTENSITY_PREDICTION_KEY = "intensity_prediction:";
    private static final String PREDICTION_HISTORY_KEY = "prediction_history:";
    private static final String PREDICTION_ACCURACY_KEY = "prediction_accuracy:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Predict psychological impact intensity
     * @param content Text content to analyze
     * @param userId User ID for personalization
     * @param psychologicalVector 37-dimensional psychological vector
     * @param buddhistFactors Buddhist factors analysis
     * @return Intensity prediction result
     */
    public Map<String, Object> predictIntensity(String content, UUID userId, 
                                              List<Double> psychologicalVector, 
                                              Map<String, Object> buddhistFactors) {
        try {
            logger.debug("Predicting psychological intensity for user {} with content length {}", userId, content.length());
            
            // Check cache first
            String cacheKey = INTENSITY_PREDICTION_KEY + userId + ":" + content.hashCode();
            Map<String, Object> cachedPrediction = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedPrediction != null) {
                logger.debug("Using cached intensity prediction for user {}", userId);
                return cachedPrediction;
            }
            
            // Calculate intensity score
            double intensityScore = calculateIntensityScore(content, psychologicalVector, buddhistFactors);
            
            // Calculate impact score
            double impactScore = calculateImpactScore(intensityScore, psychologicalVector, buddhistFactors);
            
            // Validate prediction
            boolean isValid = validatePrediction(intensityScore, impactScore);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("intensityScore", intensityScore);
            result.put("impactScore", impactScore);
            result.put("intensityLevel", classifyIntensityLevel(intensityScore));
            result.put("impactLevel", classifyImpactLevel(impactScore));
            result.put("isValid", isValid);
            result.put("confidence", calculatePredictionConfidence(intensityScore, impactScore));
            result.put("predictionMethod", "MULTI_FACTOR_ANALYSIS");
            result.put("timestamp", LocalDateTime.now());
            
            // Store prediction
            storePrediction(userId, result);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, result, 2, TimeUnit.HOURS);
            
            logger.debug("Intensity prediction completed for user {}: intensity={}, impact={}", userId, intensityScore, impactScore);
            return result;
            
        } catch (Exception e) {
            logger.error("Error predicting intensity for user {}: {}", userId, e.getMessage(), e);
            return createDefaultPrediction(userId);
        }
    }
    
    /**
     * Calculate impact score
     * @param intensityScore Base intensity score
     * @param psychologicalVector Psychological vector
     * @param buddhistFactors Buddhist factors
     * @return Impact score
     */
    public double calculateImpactScore(double intensityScore, List<Double> psychologicalVector, Map<String, Object> buddhistFactors) {
        try {
            logger.debug("Calculating impact score");
            
            // Base impact from intensity
            double baseImpact = intensityScore;
            
            // Adjust based on psychological vector
            double vectorAdjustment = calculateVectorAdjustment(psychologicalVector);
            
            // Adjust based on Buddhist factors
            double factorAdjustment = calculateFactorAdjustment(buddhistFactors);
            
            // Calculate final impact score
            double impactScore = baseImpact * (1.0 + vectorAdjustment) * (1.0 + factorAdjustment);
            
            // Normalize to [0, 1] range
            impactScore = Math.max(0.0, Math.min(1.0, impactScore));
            
            logger.debug("Impact score calculated: {}", impactScore);
            return impactScore;
            
        } catch (Exception e) {
            logger.error("Error calculating impact score: {}", e.getMessage(), e);
            return intensityScore; // Return base intensity as fallback
        }
    }
    
    /**
     * Validate prediction
     * @param intensityScore Intensity score
     * @param impactScore Impact score
     * @return Validation result
     */
    public boolean validatePrediction(double intensityScore, double impactScore) {
        try {
            logger.debug("Validating prediction");
            
            // Check score ranges
            if (intensityScore < 0.0 || intensityScore > 1.0) {
                logger.warn("Invalid intensity score: {}", intensityScore);
                return false;
            }
            
            if (impactScore < 0.0 || impactScore > 1.0) {
                logger.warn("Invalid impact score: {}", impactScore);
                return false;
            }
            
            // Check for reasonable correlation
            double correlation = Math.abs(intensityScore - impactScore);
            if (correlation > 0.8) {
                logger.warn("Unusual correlation between intensity and impact scores");
                return false;
            }
            
            logger.debug("Prediction validation passed");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating prediction: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Track prediction accuracy
     * @param userId User ID
     * @param prediction Prediction result
     * @param actualIntensity Actual observed intensity
     */
    public void trackPredictionAccuracy(UUID userId, Map<String, Object> prediction, Double actualIntensity) {
        try {
            logger.debug("Tracking prediction accuracy for user {}", userId);
            
            String accuracyKey = PREDICTION_ACCURACY_KEY + userId;
            
            // Get existing accuracy data
            Map<String, Object> accuracyData = (Map<String, Object>) redisTemplate.opsForValue().get(accuracyKey);
            if (accuracyData == null) {
                accuracyData = new HashMap<>();
            }
            
            // Add new accuracy measurement
            List<Map<String, Object>> accuracyHistory = (List<Map<String, Object>>) accuracyData.get("accuracyHistory");
            if (accuracyHistory == null) {
                accuracyHistory = new ArrayList<>();
            }
            
            Double predictedIntensity = (Double) prediction.get("intensityScore");
            if (predictedIntensity != null && actualIntensity != null) {
                double accuracy = calculateAccuracy(predictedIntensity, actualIntensity);
                
                Map<String, Object> accuracyEntry = new HashMap<>();
                accuracyEntry.put("timestamp", LocalDateTime.now());
                accuracyEntry.put("predictedIntensity", predictedIntensity);
                accuracyEntry.put("actualIntensity", actualIntensity);
                accuracyEntry.put("accuracy", accuracy);
                
                accuracyHistory.add(accuracyEntry);
                
                // Keep only recent accuracy measurements (last 100)
                if (accuracyHistory.size() > 100) {
                    accuracyHistory = accuracyHistory.subList(accuracyHistory.size() - 100, accuracyHistory.size());
                }
                
                // Calculate overall accuracy statistics
                Map<String, Object> accuracyStats = calculateAccuracyStatistics(accuracyHistory);
                
                accuracyData.put("accuracyHistory", accuracyHistory);
                accuracyData.put("accuracyStats", accuracyStats);
                accuracyData.put("lastUpdated", LocalDateTime.now());
                
                // Store updated accuracy data
                redisTemplate.opsForValue().set(accuracyKey, accuracyData, 30, TimeUnit.DAYS);
                
                logger.debug("Prediction accuracy tracked for user {}", userId);
            }
            
        } catch (Exception e) {
            logger.error("Error tracking prediction accuracy for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get prediction accuracy for a user
     * @param userId User ID
     * @return Prediction accuracy data
     */
    public Map<String, Object> getPredictionAccuracy(UUID userId) {
        try {
            logger.debug("Getting prediction accuracy for user {}", userId);
            
            String accuracyKey = PREDICTION_ACCURACY_KEY + userId;
            Map<String, Object> accuracyData = (Map<String, Object>) redisTemplate.opsForValue().get(accuracyKey);
            
            return accuracyData != null ? accuracyData : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting prediction accuracy for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get intensity prediction for a user
     * @param userId User ID
     * @return Latest intensity prediction
     */
    public Map<String, Object> getIntensityPrediction(UUID userId) {
        try {
            logger.debug("Getting intensity prediction for user {}", userId);
            
            String predictionKey = INTENSITY_PREDICTION_KEY + userId;
            Map<String, Object> prediction = (Map<String, Object>) redisTemplate.opsForValue().get(predictionKey);
            
            return prediction != null ? prediction : createDefaultPrediction(userId);
            
        } catch (Exception e) {
            logger.error("Error getting intensity prediction for user {}: {}", userId, e.getMessage(), e);
            return createDefaultPrediction(userId);
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private double calculateIntensityScore(String content, List<Double> psychologicalVector, Map<String, Object> buddhistFactors) {
        // Base intensity from content analysis
        double contentIntensity = calculateContentIntensity(content);
        
        // Psychological vector intensity
        double vectorIntensity = calculateVectorIntensity(psychologicalVector);
        
        // Buddhist factors intensity
        double factorIntensity = calculateFactorIntensity(buddhistFactors);
        
        // Combine intensities with weights
        double intensityScore = contentIntensity * 0.4 + vectorIntensity * 0.3 + factorIntensity * 0.3;
        
        return Math.max(0.0, Math.min(1.0, intensityScore));
    }
    
    private double calculateContentIntensity(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        // Analyze content for intensity indicators
        String lowerContent = content.toLowerCase();
        
        // High intensity keywords
        List<String> highIntensityKeywords = Arrays.asList("overwhelming", "intense", "extreme", "severe", "crisis", "emergency", "urgent", "critical");
        double highIntensity = calculateKeywordIntensity(lowerContent, highIntensityKeywords);
        
        // Emotional intensity keywords
        List<String> emotionalKeywords = Arrays.asList("devastated", "crushed", "broken", "shattered", "destroyed", "traumatized", "shocked", "stunned");
        double emotionalIntensity = calculateKeywordIntensity(lowerContent, emotionalKeywords);
        
        // Stress indicators
        List<String> stressKeywords = Arrays.asList("stressed", "pressure", "overwhelmed", "burdened", "exhausted", "drained", "burned out");
        double stressIntensity = calculateKeywordIntensity(lowerContent, stressKeywords);
        
        // Combine intensities
        double contentIntensity = (highIntensity * 0.5 + emotionalIntensity * 0.3 + stressIntensity * 0.2);
        
        return Math.max(0.0, Math.min(1.0, contentIntensity));
    }
    
    private double calculateVectorIntensity(List<Double> psychologicalVector) {
        if (psychologicalVector == null || psychologicalVector.isEmpty()) {
            return 0.0;
        }
        
        // Calculate intensity based on psychological vector
        double mean = psychologicalVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = psychologicalVector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Higher variance indicates higher intensity
        double vectorIntensity = Math.sqrt(variance);
        
        return Math.max(0.0, Math.min(1.0, vectorIntensity));
    }
    
    private double calculateFactorIntensity(Map<String, Object> buddhistFactors) {
        if (buddhistFactors == null) {
            return 0.0;
        }
        
        // Extract factor scores
        Map<String, Double> factorScores = (Map<String, Double>) buddhistFactors.get("factorScores");
        if (factorScores == null) {
            return 0.0;
        }
        
        // Calculate intensity based on hindrances (negative factors)
        List<String> hindrances = Arrays.asList("sensual_desire", "ill_will", "sloth_torpor", "restlessness_worry", "doubt");
        double hindranceIntensity = hindrances.stream()
            .mapToDouble(hindrance -> factorScores.getOrDefault(hindrance, 0.0))
            .average().orElse(0.0);
        
        // Calculate intensity based on enlightenment factors (positive factors)
        List<String> enlightenmentFactors = Arrays.asList("mindfulness", "investigation", "energy", "joy", "tranquility", "concentration", "equanimity");
        double enlightenmentIntensity = enlightenmentFactors.stream()
            .mapToDouble(factor -> factorScores.getOrDefault(factor, 0.0))
            .average().orElse(0.0);
        
        // Combine intensities (hindrances increase intensity, enlightenment factors decrease it)
        double factorIntensity = hindranceIntensity * 0.7 - enlightenmentIntensity * 0.3;
        
        return Math.max(0.0, Math.min(1.0, factorIntensity));
    }
    
    private double calculateKeywordIntensity(String content, List<String> keywords) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        int totalOccurrences = 0;
        int totalWords = content.split("\\s+").length;
        
        for (String keyword : keywords) {
            int occurrences = countOccurrences(content, keyword);
            totalOccurrences += occurrences;
        }
        
        // Normalize to [0, 1] range
        double intensity = Math.min(1.0, (double) totalOccurrences / Math.max(1, totalWords / 15));
        return intensity;
    }
    
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
    private double calculateVectorAdjustment(List<Double> psychologicalVector) {
        if (psychologicalVector == null || psychologicalVector.isEmpty()) {
            return 0.0;
        }
        
        // Calculate adjustment based on psychological vector characteristics
        double mean = psychologicalVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = psychologicalVector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Higher variance increases impact
        double adjustment = Math.sqrt(variance) * 0.5;
        
        return Math.max(-0.5, Math.min(0.5, adjustment));
    }
    
    private double calculateFactorAdjustment(Map<String, Object> buddhistFactors) {
        if (buddhistFactors == null) {
            return 0.0;
        }
        
        // Extract factor intensity
        Map<String, Object> factorIntensity = (Map<String, Object>) buddhistFactors.get("factorIntensity");
        if (factorIntensity == null) {
            return 0.0;
        }
        
        // Get overall intensity
        Double overallIntensity = (Double) factorIntensity.get("overallIntensity");
        if (overallIntensity == null) {
            return 0.0;
        }
        
        // Higher overall intensity increases impact
        double adjustment = overallIntensity * 0.3;
        
        return Math.max(-0.3, Math.min(0.3, adjustment));
    }
    
    private String classifyIntensityLevel(double intensityScore) {
        if (intensityScore >= HIGH_INTENSITY_THRESHOLD) {
            return "HIGH";
        } else if (intensityScore >= MODERATE_INTENSITY_THRESHOLD) {
            return "MODERATE";
        } else if (intensityScore >= LOW_INTENSITY_THRESHOLD) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private String classifyImpactLevel(double impactScore) {
        if (impactScore >= HIGH_INTENSITY_THRESHOLD) {
            return "HIGH";
        } else if (impactScore >= MODERATE_INTENSITY_THRESHOLD) {
            return "MODERATE";
        } else if (impactScore >= LOW_INTENSITY_THRESHOLD) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private double calculatePredictionConfidence(double intensityScore, double impactScore) {
        // Confidence based on score consistency and magnitude
        double consistency = 1.0 - Math.abs(intensityScore - impactScore);
        double magnitude = Math.max(intensityScore, impactScore);
        
        // Higher confidence for consistent and moderate scores
        double confidence = consistency * 0.6 + magnitude * 0.4;
        
        return Math.max(0.1, Math.min(1.0, confidence));
    }
    
    private double calculateAccuracy(double predictedIntensity, double actualIntensity) {
        // Calculate accuracy as inverse of absolute difference
        double difference = Math.abs(predictedIntensity - actualIntensity);
        double accuracy = Math.max(0.0, 1.0 - difference);
        
        return accuracy;
    }
    
    private Map<String, Object> calculateAccuracyStatistics(List<Map<String, Object>> accuracyHistory) {
        Map<String, Object> stats = new HashMap<>();
        
        if (accuracyHistory.isEmpty()) {
            stats.put("totalPredictions", 0);
            stats.put("averageAccuracy", 0.0);
            stats.put("accuracyTrend", "STABLE");
            return stats;
        }
        
        int totalPredictions = accuracyHistory.size();
        double averageAccuracy = accuracyHistory.stream()
            .mapToDouble(entry -> (Double) entry.get("accuracy"))
            .average().orElse(0.0);
        
        // Calculate accuracy trend
        String accuracyTrend = calculateAccuracyTrend(accuracyHistory);
        
        stats.put("totalPredictions", totalPredictions);
        stats.put("averageAccuracy", averageAccuracy);
        stats.put("accuracyTrend", accuracyTrend);
        
        return stats;
    }
    
    private String calculateAccuracyTrend(List<Map<String, Object>> accuracyHistory) {
        if (accuracyHistory.size() < 2) {
            return "STABLE";
        }
        
        // Compare recent accuracy with older accuracy
        int recentCount = Math.min(10, accuracyHistory.size() / 2);
        int olderCount = Math.min(10, accuracyHistory.size() / 2);
        
        double recentAccuracy = accuracyHistory.subList(accuracyHistory.size() - recentCount, accuracyHistory.size())
            .stream().mapToDouble(entry -> (Double) entry.get("accuracy"))
            .average().orElse(0.0);
        
        double olderAccuracy = accuracyHistory.subList(0, olderCount)
            .stream().mapToDouble(entry -> (Double) entry.get("accuracy"))
            .average().orElse(0.0);
        
        if (recentAccuracy > olderAccuracy + 0.1) {
            return "IMPROVING";
        } else if (recentAccuracy < olderAccuracy - 0.1) {
            return "DECLINING";
        } else {
            return "STABLE";
        }
    }
    
    private void storePrediction(UUID userId, Map<String, Object> prediction) {
        try {
            logger.debug("Storing intensity prediction for user {}", userId);
            
            String storageKey = INTENSITY_PREDICTION_KEY + userId;
            String historyKey = PREDICTION_HISTORY_KEY + userId;
            
            // Store current prediction
            redisTemplate.opsForValue().set(storageKey, prediction, 6, TimeUnit.HOURS);
            
            // Update history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            if (history == null) {
                history = new HashMap<>();
            }
            
            List<Map<String, Object>> predictionHistory = (List<Map<String, Object>>) history.get("predictions");
            if (predictionHistory == null) {
                predictionHistory = new ArrayList<>();
            }
            
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("timestamp", LocalDateTime.now());
            historyEntry.put("prediction", prediction);
            
            predictionHistory.add(historyEntry);
            
            // Keep only recent predictions (last 50)
            if (predictionHistory.size() > 50) {
                predictionHistory = predictionHistory.subList(predictionHistory.size() - 50, predictionHistory.size());
            }
            
            history.put("predictions", predictionHistory);
            history.put("lastUpdated", LocalDateTime.now());
            
            redisTemplate.opsForValue().set(historyKey, history, 30, TimeUnit.DAYS);
            
            logger.debug("Intensity prediction stored for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error storing intensity prediction for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    private Map<String, Object> createDefaultPrediction(UUID userId) {
        Map<String, Object> defaultPrediction = new HashMap<>();
        defaultPrediction.put("userId", userId.toString());
        defaultPrediction.put("intensityScore", 0.5);
        defaultPrediction.put("impactScore", 0.5);
        defaultPrediction.put("intensityLevel", "MODERATE");
        defaultPrediction.put("impactLevel", "MODERATE");
        defaultPrediction.put("isValid", false);
        defaultPrediction.put("confidence", 0.1);
        defaultPrediction.put("predictionMethod", "DEFAULT");
        defaultPrediction.put("timestamp", LocalDateTime.now());
        return defaultPrediction;
    }
}
