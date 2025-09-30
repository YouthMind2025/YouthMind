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

/**
 * Mathematical operations for 43-dimensional psychological vectors
 * Provides core vector arithmetic and similarity calculations
 */
@Service
public class StateVectorCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(StateVectorCalculator.class);
    
    // Vector operation constants
    public static final int VECTOR_DIMENSIONS = 43;
    public static final double CLIPPING_LOWER_BOUND = -2.0;
    public static final double CLIPPING_UPPER_BOUND = 2.0;
    public static final double SIMILARITY_THRESHOLD = 0.7;
    
    // Redis keys
    private static final String VECTOR_CACHE_KEY = "vector_calculation:";
    private static final String SIMILARITY_CACHE_KEY = "vector_similarity:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityFactorCalculator intensityFactorCalculator;
    
    /**
     * Add two vectors element-wise
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Sum of the vectors
     */
    public List<Double> addVectors(List<Double> vector1, List<Double> vector2) {
        try {
            validateVectors(vector1, vector2);
            
            List<Double> result = new ArrayList<>();
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                result.add(vector1.get(i) + vector2.get(i));
            }
            
            logger.debug("Added two vectors successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Error adding vectors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add vectors", e);
        }
    }
    
    /**
     * Multiply vector by a scalar value
     * 
     * @param vector Input vector
     * @param scalar Scalar multiplier
     * @return Scaled vector
     */
    public List<Double> multiplyByScalar(List<Double> vector, double scalar) {
        try {
            validateVector(vector);
            
            List<Double> result = new ArrayList<>();
            for (Double value : vector) {
                result.add(value * scalar);
            }
            
            logger.debug("Multiplied vector by scalar {} successfully", scalar);
            return result;
            
        } catch (Exception e) {
            logger.error("Error multiplying vector by scalar: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to multiply vector by scalar", e);
        }
    }
    
    /**
     * Normalize vector using L2 normalization
     * 
     * @param vector Input vector
     * @return L2 normalized vector
     */
    public List<Double> normalizeVector(List<Double> vector) {
        try {
            validateVector(vector);
            
            // Calculate L2 norm
            double norm = calculateL2Norm(vector);
            
            if (norm == 0.0) {
                logger.warn("Vector has zero norm, returning original vector");
                return new ArrayList<>(vector);
            }
            
            // Normalize by dividing each element by the norm
            List<Double> result = new ArrayList<>();
            for (Double value : vector) {
                result.add(value / norm);
            }
            
            logger.debug("Normalized vector with L2 norm: {}", norm);
            return result;
            
        } catch (Exception e) {
            logger.error("Error normalizing vector: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to normalize vector", e);
        }
    }
    
    /**
     * Calculate Euclidean distance between two vectors
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Euclidean distance
     */
    public double calculateDistance(List<Double> vector1, List<Double> vector2) {
        try {
            validateVectors(vector1, vector2);
            
            double sumSquaredDiffs = 0.0;
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                double diff = vector1.get(i) - vector2.get(i);
                sumSquaredDiffs += diff * diff;
            }
            
            double distance = Math.sqrt(sumSquaredDiffs);
            logger.debug("Calculated distance between vectors: {}", distance);
            return distance;
            
        } catch (Exception e) {
            logger.error("Error calculating distance between vectors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate distance", e);
        }
    }
    
    /**
     * Calculate cosine similarity between two vectors
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity (-1 to 1)
     */
    public double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        try {
            validateVectors(vector1, vector2);
            
            // Calculate dot product
            double dotProduct = 0.0;
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                dotProduct += vector1.get(i) * vector2.get(i);
            }
            
            // Calculate magnitudes
            double magnitude1 = calculateL2Norm(vector1);
            double magnitude2 = calculateL2Norm(vector2);
            
            if (magnitude1 == 0.0 || magnitude2 == 0.0) {
                logger.warn("One or both vectors have zero magnitude");
                return 0.0;
            }
            
            double similarity = dotProduct / (magnitude1 * magnitude2);
            logger.debug("Calculated cosine similarity: {}", similarity);
            return similarity;
            
        } catch (Exception e) {
            logger.error("Error calculating cosine similarity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate cosine similarity", e);
        }
    }
    
    /**
     * Apply clipping to vector values to prevent explosion
     * 
     * @param vector Input vector
     * @return Clipped vector
     */
    public List<Double> applyClipping(List<Double> vector) {
        try {
            validateVector(vector);
            
            List<Double> result = new ArrayList<>();
            for (Double value : vector) {
                double clippedValue = Math.max(CLIPPING_LOWER_BOUND, 
                                             Math.min(CLIPPING_UPPER_BOUND, value));
                result.add(clippedValue);
            }
            
            logger.debug("Applied clipping to vector with bounds [{}, {}]", 
                        CLIPPING_LOWER_BOUND, CLIPPING_UPPER_BOUND);
            return result;
            
        } catch (Exception e) {
            logger.error("Error applying clipping to vector: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply clipping", e);
        }
    }
    
    /**
     * Apply custom clipping with specified bounds
     * 
     * @param vector Input vector
     * @param lowerBound Lower clipping bound
     * @param upperBound Upper clipping bound
     * @return Clipped vector
     */
    public List<Double> applyClipping(List<Double> vector, double lowerBound, double upperBound) {
        try {
            validateVector(vector);
            
            List<Double> result = new ArrayList<>();
            for (Double value : vector) {
                double clippedValue = Math.max(lowerBound, Math.min(upperBound, value));
                result.add(clippedValue);
            }
            
            logger.debug("Applied custom clipping to vector with bounds [{}, {}]", 
                        lowerBound, upperBound);
            return result;
            
        } catch (Exception e) {
            logger.error("Error applying custom clipping to vector: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply custom clipping", e);
        }
    }
    
    /**
     * Calculate dot product of two vectors
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Dot product
     */
    public double calculateDotProduct(List<Double> vector1, List<Double> vector2) {
        try {
            validateVectors(vector1, vector2);
            
            double dotProduct = 0.0;
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                dotProduct += vector1.get(i) * vector2.get(i);
            }
            
            logger.debug("Calculated dot product: {}", dotProduct);
            return dotProduct;
            
        } catch (Exception e) {
            logger.error("Error calculating dot product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate dot product", e);
        }
    }
    
    /**
     * Calculate L2 norm (magnitude) of a vector
     * 
     * @param vector Input vector
     * @return L2 norm
     */
    public double calculateL2Norm(List<Double> vector) {
        try {
            validateVector(vector);
            
            double sumSquared = 0.0;
            for (Double value : vector) {
                sumSquared += value * value;
            }
            
            double norm = Math.sqrt(sumSquared);
            logger.debug("Calculated L2 norm: {}", norm);
            return norm;
            
        } catch (Exception e) {
            logger.error("Error calculating L2 norm: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate L2 norm", e);
        }
    }
    
    /**
     * Calculate L1 norm (Manhattan distance) of a vector
     * 
     * @param vector Input vector
     * @return L1 norm
     */
    public double calculateL1Norm(List<Double> vector) {
        try {
            validateVector(vector);
            
            double sumAbs = 0.0;
            for (Double value : vector) {
                sumAbs += Math.abs(value);
            }
            
            logger.debug("Calculated L1 norm: {}", sumAbs);
            return sumAbs;
            
        } catch (Exception e) {
            logger.error("Error calculating L1 norm: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate L1 norm", e);
        }
    }
    
    /**
     * Calculate mean of a vector
     * 
     * @param vector Input vector
     * @return Mean value
     */
    public double calculateMean(List<Double> vector) {
        try {
            validateVector(vector);
            
            double sum = 0.0;
            for (Double value : vector) {
                sum += value;
            }
            
            double mean = sum / VECTOR_DIMENSIONS;
            logger.debug("Calculated mean: {}", mean);
            return mean;
            
        } catch (Exception e) {
            logger.error("Error calculating mean: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate mean", e);
        }
    }
    
    /**
     * Calculate variance of vector values
     * 
     * @param vector Input vector
     * @return Variance value
     */
    public double calculateVariance(List<Double> vector) {
        try {
            validateVector(vector);
            
            double mean = calculateMean(vector);
            double sumSquaredDiffs = 0.0;
            
            for (Double value : vector) {
                double diff = value - mean;
                sumSquaredDiffs += diff * diff;
            }
            
            double variance = sumSquaredDiffs / VECTOR_DIMENSIONS;
            logger.debug("Calculated variance: {}", variance);
            return variance;
            
        } catch (Exception e) {
            logger.error("Error calculating variance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate variance", e);
        }
    }
    
    /**
     * Calculate standard deviation of a vector
     * 
     * @param vector Input vector
     * @return Standard deviation
     */
    public double calculateStandardDeviation(List<Double> vector) {
        try {
            validateVector(vector);
            
            double mean = calculateMean(vector);
            double sumSquaredDiffs = 0.0;
            
            for (Double value : vector) {
                double diff = value - mean;
                sumSquaredDiffs += diff * diff;
            }
            
            double variance = sumSquaredDiffs / VECTOR_DIMENSIONS;
            double stdDev = Math.sqrt(variance);
            
            logger.debug("Calculated standard deviation: {}", stdDev);
            return stdDev;
            
        } catch (Exception e) {
            logger.error("Error calculating standard deviation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate standard deviation", e);
        }
    }
    
    /**
     * Check if two vectors are similar based on cosine similarity threshold
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return true if vectors are similar
     */
    public boolean areVectorsSimilar(List<Double> vector1, List<Double> vector2) {
        try {
            double similarity = calculateCosineSimilarity(vector1, vector2);
            return similarity >= SIMILARITY_THRESHOLD;
        } catch (Exception e) {
            logger.error("Error checking vector similarity: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create a zero vector
     * 
     * @return Zero vector with 43 dimensions
     */
    public List<Double> createZeroVector() {
        List<Double> zeroVector = new ArrayList<>();
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            zeroVector.add(0.0);
        }
        return zeroVector;
    }
    
    /**
     * Create a unit vector in a specific direction
     * 
     * @param dimensionIndex Dimension to set to 1.0 (0-42)
     * @return Unit vector
     */
    public List<Double> createUnitVector(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Dimension index must be between 0 and " + (VECTOR_DIMENSIONS - 1));
        }
        
        List<Double> unitVector = createZeroVector();
        unitVector.set(dimensionIndex, 1.0);
        return unitVector;
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
    
    private void validateVectors(List<Double> vector1, List<Double> vector2) {
        validateVector(vector1);
        validateVector(vector2);
    }
    
    /**
     * Calculate vector similarity with caching and enhanced analysis
     */
    public double calculateEnhancedSimilarity(List<Double> vector1, List<Double> vector2, UUID userId) {
        try {
            // Check cache first
            String cacheKey = SIMILARITY_CACHE_KEY + userId + ":" + 
                            vector1.hashCode() + ":" + vector2.hashCode();
            Double cachedSimilarity = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedSimilarity != null) {
                logger.debug("Using cached similarity: {}", cachedSimilarity);
                return cachedSimilarity;
            }
            
            // Calculate base cosine similarity
            double cosineSimilarity = calculateCosineSimilarity(vector1, vector2);
            
            // Enhance with psychological analysis
            double psychologicalSimilarity = calculatePsychologicalSimilarity(vector1, vector2, userId);
            
            // Combine similarities
            double enhancedSimilarity = (cosineSimilarity * 0.7) + (psychologicalSimilarity * 0.3);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, enhancedSimilarity, 1, TimeUnit.HOURS);
            
            logger.debug("Calculated enhanced similarity: {} (cosine: {}, psychological: {})", 
                        enhancedSimilarity, cosineSimilarity, psychologicalSimilarity);
            
            return enhancedSimilarity;
            
        } catch (Exception e) {
            logger.error("Error calculating enhanced similarity: {}", e.getMessage(), e);
            return calculateCosineSimilarity(vector1, vector2);
        }
    }
    
    /**
     * Calculate psychological similarity using Buddhist factors
     */
    private double calculatePsychologicalSimilarity(List<Double> vector1, List<Double> vector2, UUID userId) {
        try {
            // Analyze Buddhist factors for both vectors
            Map<String, Object> factors1 = buddhistFactorAnalyzer.analyzeFactors("", userId);
            Map<String, Object> factors2 = buddhistFactorAnalyzer.analyzeFactors("", userId);
            
            @SuppressWarnings("unchecked")
            Map<String, Double> factorScores1 = (Map<String, Double>) factors1.get("factorScores");
            @SuppressWarnings("unchecked")
            Map<String, Double> factorScores2 = (Map<String, Double>) factors2.get("factorScores");
            
            if (factorScores1 == null || factorScores2 == null) {
                return 0.5; // Default similarity
            }
            
            // Calculate factor-based similarity
            double factorSimilarity = 0.0;
            int factorCount = 0;
            
            for (String factor : factorScores1.keySet()) {
                if (factorScores2.containsKey(factor)) {
                    double score1 = factorScores1.get(factor);
                    double score2 = factorScores2.get(factor);
                    factorSimilarity += 1.0 - Math.abs(score1 - score2);
                    factorCount++;
                }
            }
            
            return factorCount > 0 ? factorSimilarity / factorCount : 0.5;
            
        } catch (Exception e) {
            logger.warn("Error calculating psychological similarity: {}", e.getMessage());
            return 0.5;
        }
    }
    
    /**
     * Generate vector from content with enhanced analysis
     */
    public List<Double> generateVectorFromContent(String content, UUID userId) {
        try {
            // Check cache first
            String cacheKey = VECTOR_CACHE_KEY + userId + ":" + content.hashCode();
            @SuppressWarnings("unchecked")
            List<Double> cachedVector = (List<Double>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedVector != null) {
                logger.debug("Using cached vector for content");
                return cachedVector;
            }
            
            // Generate vector using PsychologicalVectorGenerator
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(content, userId);
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) vectorResult.get("psychologicalVector");
            
            if (vector != null && vector.size() == VECTOR_DIMENSIONS) {
                // Cache the result
                redisTemplate.opsForValue().set(cacheKey, vector, 2, TimeUnit.HOURS);
                logger.debug("Generated vector from content with {} dimensions", vector.size());
                return vector;
            }
            
            // Fallback to default vector
            return createZeroVector();
            
        } catch (Exception e) {
            logger.error("Error generating vector from content: {}", e.getMessage(), e);
            return createZeroVector();
        }
    }
    
    /**
     * Calculate vector statistics with enhanced metrics
     */
    public Map<String, Object> calculateVectorStatistics(List<Double> vector, UUID userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Basic statistics
            stats.put("magnitude", calculateL2Norm(vector));
            stats.put("mean", calculateMean(vector));
            stats.put("variance", calculateVariance(vector));
            stats.put("maxValue", vector.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            stats.put("minValue", vector.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
            
            // Psychological statistics
            stats.put("positiveFactorIntensity", calculatePositiveFactorIntensity(vector));
            stats.put("negativeFactorIntensity", calculateNegativeFactorIntensity(vector));
            stats.put("emotionalIntensity", calculateEmotionalIntensity(vector));
            stats.put("cognitiveIntensity", calculateCognitiveIntensity(vector));
            
            // Buddhist factor analysis
            Map<String, Object> buddhistAnalysis = analyzeBuddhistFactorsInVector(vector, userId);
            stats.put("buddhistAnalysis", buddhistAnalysis);
            
            stats.put("calculatedAt", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error calculating vector statistics: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Calculate positive factor intensity (dimensions 0-15)
     */
    private double calculatePositiveFactorIntensity(List<Double> vector) {
        double sum = 0.0;
        for (int i = 0; i < 16 && i < vector.size(); i++) {
            sum += Math.abs(vector.get(i));
        }
        return sum / 16.0;
    }
    
    /**
     * Calculate negative factor intensity (dimensions 16-42)
     */
    private double calculateNegativeFactorIntensity(List<Double> vector) {
        double sum = 0.0;
        for (int i = 16; i < vector.size(); i++) {
            sum += Math.abs(vector.get(i));
        }
        return sum / (vector.size() - 16);
    }
    
    /**
     * Calculate emotional intensity (dimensions 1, 2, 7, 8)
     */
    private double calculateEmotionalIntensity(List<Double> vector) {
        List<Integer> emotionalDims = List.of(1, 2, 7, 8);
        double sum = 0.0;
        for (int dim : emotionalDims) {
            if (dim < vector.size()) {
                sum += Math.abs(vector.get(dim));
            }
        }
        return sum / emotionalDims.size();
    }
    
    /**
     * Calculate cognitive intensity (dimensions 0, 3, 4, 5)
     */
    private double calculateCognitiveIntensity(List<Double> vector) {
        List<Integer> cognitiveDims = List.of(0, 3, 4, 5);
        double sum = 0.0;
        for (int dim : cognitiveDims) {
            if (dim < vector.size()) {
                sum += Math.abs(vector.get(dim));
            }
        }
        return sum / cognitiveDims.size();
    }
    
    /**
     * Analyze Buddhist factors in vector
     */
    private Map<String, Object> analyzeBuddhistFactorsInVector(List<Double> vector, UUID userId) {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // Analyze specific high-impact factors
            analysis.put("faith", vector.get(5));
            analysis.put("vigor", vector.get(6));
            analysis.put("greed", vector.get(16));
            analysis.put("anger", vector.get(17));
            analysis.put("arrogance", vector.get(18));
            analysis.put("doubt", vector.get(20));
            
            // Calculate factor balance
            double positiveBalance = calculatePositiveFactorIntensity(vector);
            double negativeBalance = calculateNegativeFactorIntensity(vector);
            analysis.put("positiveBalance", positiveBalance);
            analysis.put("negativeBalance", negativeBalance);
            analysis.put("balanceRatio", positiveBalance / (negativeBalance + 0.001));
            
            return analysis;
            
        } catch (Exception e) {
            logger.warn("Error analyzing Buddhist factors in vector: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
