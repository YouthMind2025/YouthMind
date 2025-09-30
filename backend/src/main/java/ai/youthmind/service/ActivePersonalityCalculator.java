package ai.youthmind.service;

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
 * Service for calculating active personality (μ_post - weighted mean)
 * Implements sophisticated active personality computation algorithms
 */
@Service
@Transactional
public class ActivePersonalityCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivePersonalityCalculator.class);
    
    // Active personality algorithm constants
    private static final double RECENCY_WEIGHT_FACTOR = 0.8; // Weight factor for recency
    private static final double ENGAGEMENT_WEIGHT_FACTOR = 1.2; // Weight factor for engagement
    private static final double EMOTIONAL_WEIGHT_FACTOR = 1.0; // Weight factor for emotional content
    private static final int PERSONALITY_VECTOR_DIMENSIONS = 5; // Dimensions of personality vector
    
    // Redis keys
    private static final String ACTIVE_TRENDS_KEY = "active_trends:";
    private static final String WEIGHTED_MEAN_KEY = "weighted_mean:";
    private static final String POST_WEIGHTS_KEY = "post_weights:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate weighted mean personality vector (μ_post)
     * @param userId User ID
     * @param resonances List of recent resonances
     * @return Weighted mean personality vector
     */
    public Map<String, Object> calculateWeightedMean(UUID userId, List<PostResonance> resonances) {
        try {
            logger.debug("Calculating weighted mean personality for user {} with {} resonances", userId, resonances.size());
            
            if (resonances.isEmpty()) {
                return createDefaultPersonalityVector(userId);
            }
            
            // Get posts for resonances
            List<UUID> postIds = resonances.stream().map(PostResonance::getPostId).collect(Collectors.toList());
            List<Post> posts = postRepository.findAllById(postIds);
            
            if (posts.isEmpty()) {
                return createDefaultPersonalityVector(userId);
            }
            
            // Calculate weights for each resonance
            Map<UUID, Double> resonanceWeights = calculateResonanceWeights(resonances, posts);
            
            // Calculate weighted personality vector
            List<Double> weightedVector = calculateWeightedPersonalityVector(resonances, posts, resonanceWeights);
            
            // Calculate confidence score
            double confidence = calculateConfidenceScore(resonances, posts, resonanceWeights);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("personalityVector", weightedVector);
            result.put("confidence", confidence);
            result.put("resonanceCount", resonances.size());
            result.put("computationMethod", "WEIGHTED_MEAN");
            result.put("weights", resonanceWeights);
            result.put("timestamp", LocalDateTime.now());
            
            // Cache the result
            String cacheKey = WEIGHTED_MEAN_KEY + userId;
            redisTemplate.opsForValue().set(cacheKey, result, 2, TimeUnit.HOURS);
            
            logger.debug("Weighted mean personality calculated for user {}: {}", userId, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error calculating weighted mean personality for user {}: {}", userId, e.getMessage(), e);
            return createDefaultPersonalityVector(userId);
        }
    }
    
    /**
     * Apply post weights to personality calculation
     * @param personalityVector Personality vector to modify
     * @param resonances List of resonances used in calculation
     */
    public void applyPostWeights(Map<String, Object> personalityVector, List<PostResonance> resonances) {
        try {
            logger.debug("Applying post weights to personality vector");
            
            // Get posts for resonances
            List<UUID> postIds = resonances.stream().map(PostResonance::getPostId).collect(Collectors.toList());
            List<Post> posts = postRepository.findAllById(postIds);
            
            if (posts.isEmpty()) {
                return;
            }
            
            // Calculate post-specific weights
            Map<String, Double> postWeights = calculatePostSpecificWeights(posts);
            
            // Apply weights to personality vector
            List<Double> originalVector = (List<Double>) personalityVector.get("personalityVector");
            if (originalVector != null) {
                List<Double> weightedVector = applyWeightsToVector(originalVector, postWeights);
                personalityVector.put("personalityVector", weightedVector);
                personalityVector.put("postWeights", postWeights);
            }
            
            // Update confidence based on post weights
            double weightConfidence = calculateWeightConfidence(postWeights);
            Double originalConfidence = (Double) personalityVector.get("confidence");
            if (originalConfidence != null) {
                personalityVector.put("confidence", originalConfidence * weightConfidence);
            }
            
            logger.debug("Post weights applied to personality vector");
            
        } catch (Exception e) {
            logger.error("Error applying post weights: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Track active personality trends
     * @param userId User ID
     * @param personalityVector Current personality vector
     */
    public void trackActiveTrends(UUID userId, Map<String, Object> personalityVector) {
        try {
            logger.debug("Tracking active trends for user {}", userId);
            
            String trendsKey = ACTIVE_TRENDS_KEY + userId;
            
            // Get existing trends
            Map<String, Object> trends = (Map<String, Object>) redisTemplate.opsForValue().get(trendsKey);
            if (trends == null) {
                trends = new HashMap<>();
            }
            
            // Update trends
            List<Map<String, Object>> trendHistory = (List<Map<String, Object>>) trends.get("trendHistory");
            if (trendHistory == null) {
                trendHistory = new ArrayList<>();
            }
            
            Map<String, Object> trendEntry = new HashMap<>();
            trendEntry.put("timestamp", LocalDateTime.now());
            trendEntry.put("personalityVector", personalityVector.get("personalityVector"));
            trendEntry.put("confidence", personalityVector.get("confidence"));
            
            trendHistory.add(trendEntry);
            
            // Keep only recent trends (last 100 entries)
            if (trendHistory.size() > 100) {
                trendHistory = trendHistory.subList(trendHistory.size() - 100, trendHistory.size());
            }
            
            // Calculate trend metrics
            Map<String, Object> trendMetrics = calculateTrendMetrics(trendHistory);
            
            trends.put("trendHistory", trendHistory);
            trends.put("trendMetrics", trendMetrics);
            trends.put("lastUpdated", LocalDateTime.now());
            
            // Store updated trends
            redisTemplate.opsForValue().set(trendsKey, trends, 7, TimeUnit.DAYS);
            
            logger.debug("Active trends tracked for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error tracking active trends for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get active personality trends for a user
     * @param userId User ID
     * @return Active personality trends
     */
    public Map<String, Object> getActiveTrends(UUID userId) {
        try {
            logger.debug("Getting active trends for user {}", userId);
            
            String trendsKey = ACTIVE_TRENDS_KEY + userId;
            Map<String, Object> trends = (Map<String, Object>) redisTemplate.opsForValue().get(trendsKey);
            
            return trends != null ? trends : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting active trends for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<UUID, Double> calculateResonanceWeights(List<PostResonance> resonances, List<Post> posts) {
        Map<UUID, Double> weights = new HashMap<>();
        Map<UUID, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        
        for (PostResonance resonance : resonances) {
            Post post = postMap.get(resonance.getPostId());
            if (post == null) continue;
            
            // Calculate weight based on multiple factors
            double recencyWeight = calculateRecencyWeight(resonance.getResonancedAt());
            double engagementWeight = calculateEngagementWeight(post);
            double emotionalWeight = calculateEmotionalWeight(post);
            
            // Combined weight
            double totalWeight = recencyWeight * RECENCY_WEIGHT_FACTOR + 
                               engagementWeight * ENGAGEMENT_WEIGHT_FACTOR + 
                               emotionalWeight * EMOTIONAL_WEIGHT_FACTOR;
            
            weights.put(resonance.getPostId(), totalWeight);
        }
        
        return weights;
    }
    
    private double calculateRecencyWeight(LocalDateTime resonancedAt) {
        long hoursAgo = ChronoUnit.HOURS.between(resonancedAt, LocalDateTime.now());
        
        // Exponential decay for recency
        if (hoursAgo <= 24) {
            return 1.0; // Full weight for recent resonances
        } else if (hoursAgo <= 168) { // 1 week
            return Math.exp(-hoursAgo / 48.0); // Exponential decay
        } else {
            return 0.1; // Minimal weight for old resonances
        }
    }
    
    private double calculateEngagementWeight(Post post) {
        // Weight based on post engagement metrics
        double engagementScore = 0.0;
        
        if (post.getResonance() > 0) {
            engagementScore += Math.log(post.getResonance() + 1) * 0.3;
        }
        
        if (post.getView() > 0) {
            engagementScore += Math.log(post.getView() + 1) * 0.2;
        }
        
        if (post.getCollect() > 0) {
            engagementScore += Math.log(post.getCollect() + 1) * 0.5;
        }
        
        // Normalize to [0, 1] range
        return Math.min(1.0, engagementScore / 5.0);
    }
    
    private double calculateEmotionalWeight(Post post) {
        // Weight based on emotional content
        String emotion = post.getEmotion();
        if (emotion == null) {
            return 0.5; // Neutral weight
        }
        
        // Different emotions have different weights
        Map<String, Double> emotionWeights = Map.of(
            "happy", 1.0,
            "sad", 1.2,
            "angry", 1.1,
            "anxious", 1.3,
            "excited", 0.9,
            "calm", 0.8,
            "confused", 1.1,
            "grateful", 1.0
        );
        
        return emotionWeights.getOrDefault(emotion.toLowerCase(), 1.0);
    }
    
    private List<Double> calculateWeightedPersonalityVector(List<PostResonance> resonances, 
                                                          List<Post> posts, 
                                                          Map<UUID, Double> weights) {
        // Initialize personality vector
        List<Double> personalityVector = new ArrayList<>();
        for (int i = 0; i < PERSONALITY_VECTOR_DIMENSIONS; i++) {
            personalityVector.add(0.0);
        }
        
        Map<UUID, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        double totalWeight = 0.0;
        
        for (PostResonance resonance : resonances) {
            Post post = postMap.get(resonance.getPostId());
            if (post == null) continue;
            
            Double weight = weights.get(resonance.getPostId());
            if (weight == null) continue;
            
            // Convert post to personality vector components
            List<Double> postVector = convertPostToPersonalityVector(post);
            
            // Add weighted contribution
            for (int i = 0; i < PERSONALITY_VECTOR_DIMENSIONS; i++) {
                personalityVector.set(i, personalityVector.get(i) + postVector.get(i) * weight);
            }
            
            totalWeight += weight;
        }
        
        // Normalize by total weight
        if (totalWeight > 0) {
            for (int i = 0; i < PERSONALITY_VECTOR_DIMENSIONS; i++) {
                personalityVector.set(i, personalityVector.get(i) / totalWeight);
            }
        }
        
        return personalityVector;
    }
    
    private List<Double> convertPostToPersonalityVector(Post post) {
        List<Double> vector = new ArrayList<>();
        
        // Dimension 1: Emotional intensity
        double emotionalIntensity = calculateEmotionalIntensity(post);
        vector.add(emotionalIntensity);
        
        // Dimension 2: Social engagement
        double socialEngagement = calculateSocialEngagement(post);
        vector.add(socialEngagement);
        
        // Dimension 3: Content complexity
        double contentComplexity = calculateContentComplexity(post);
        vector.add(contentComplexity);
        
        // Dimension 4: Positivity
        double positivity = calculatePositivity(post);
        vector.add(positivity);
        
        // Dimension 5: Vulnerability
        double vulnerability = calculateVulnerability(post);
        vector.add(vulnerability);
        
        return vector;
    }
    
    private double calculateEmotionalIntensity(Post post) {
        String emotion = post.getEmotion();
        if (emotion == null) {
            return 0.5; // Neutral
        }
        
        Map<String, Double> intensityMap = Map.of(
            "happy", 0.8,
            "sad", 0.9,
            "angry", 1.0,
            "anxious", 0.9,
            "excited", 0.7,
            "calm", 0.3,
            "confused", 0.6,
            "grateful", 0.7
        );
        
        return intensityMap.getOrDefault(emotion.toLowerCase(), 0.5);
    }
    
    private double calculateSocialEngagement(Post post) {
        // Based on post engagement metrics
        double engagement = 0.0;
        
        if (post.getResonance() > 0) {
            engagement += Math.min(1.0, post.getResonance() / 10.0);
        }
        
        if (post.getView() > 0) {
            engagement += Math.min(1.0, post.getView() / 50.0) * 0.5;
        }
        
        return Math.min(1.0, engagement);
    }
    
    private double calculateContentComplexity(Post post) {
        String content = post.getContent();
        if (content == null) {
            return 0.5;
        }
        
        // Simple complexity calculation based on content length and structure
        int length = content.length();
        int sentences = content.split("[.!?]+").length;
        int words = content.split("\\s+").length;
        
        double complexity = 0.0;
        
        // Length factor
        complexity += Math.min(1.0, length / 500.0) * 0.3;
        
        // Sentence complexity
        if (sentences > 0) {
            double avgWordsPerSentence = (double) words / sentences;
            complexity += Math.min(1.0, avgWordsPerSentence / 20.0) * 0.4;
        }
        
        // Vocabulary diversity (simplified)
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(content.toLowerCase().split("\\s+")));
        double vocabularyDiversity = (double) uniqueWords.size() / words;
        complexity += vocabularyDiversity * 0.3;
        
        return Math.min(1.0, complexity);
    }
    
    private double calculatePositivity(Post post) {
        String emotion = post.getEmotion();
        if (emotion == null) {
            return 0.5; // Neutral
        }
        
        Map<String, Double> positivityMap = Map.of(
            "happy", 1.0,
            "sad", 0.2,
            "angry", 0.1,
            "anxious", 0.3,
            "excited", 0.9,
            "calm", 0.7,
            "confused", 0.4,
            "grateful", 0.95
        );
        
        return positivityMap.getOrDefault(emotion.toLowerCase(), 0.5);
    }
    
    private double calculateVulnerability(Post post) {
        String emotion = post.getEmotion();
        if (emotion == null) {
            return 0.5; // Neutral
        }
        
        Map<String, Double> vulnerabilityMap = Map.of(
            "happy", 0.3,
            "sad", 0.9,
            "angry", 0.4,
            "anxious", 0.8,
            "excited", 0.2,
            "calm", 0.1,
            "confused", 0.7,
            "grateful", 0.6
        );
        
        return vulnerabilityMap.getOrDefault(emotion.toLowerCase(), 0.5);
    }
    
    private double calculateConfidenceScore(List<PostResonance> resonances, 
                                          List<Post> posts, 
                                          Map<UUID, Double> weights) {
        // Confidence based on number of resonances and weight distribution
        double resonanceCount = resonances.size();
        double weightVariance = calculateWeightVariance(weights);
        
        // Higher confidence with more resonances and lower weight variance
        double confidence = Math.min(1.0, resonanceCount / 20.0) * (1.0 - weightVariance);
        
        return Math.max(0.1, confidence);
    }
    
    private double calculateWeightVariance(Map<UUID, Double> weights) {
        if (weights.isEmpty()) {
            return 0.0;
        }
        
        double mean = weights.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = weights.values().stream()
            .mapToDouble(weight -> Math.pow(weight - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private Map<String, Double> calculatePostSpecificWeights(List<Post> posts) {
        Map<String, Double> weights = new HashMap<>();
        
        for (Post post : posts) {
            String emotion = post.getEmotion();
            if (emotion != null) {
                weights.merge(emotion, 1.0, Double::sum);
            }
        }
        
        // Normalize weights
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight > 0) {
            weights.replaceAll((k, v) -> v / totalWeight);
        }
        
        return weights;
    }
    
    private List<Double> applyWeightsToVector(List<Double> originalVector, Map<String, Double> weights) {
        List<Double> weightedVector = new ArrayList<>(originalVector);
        
        // Apply emotion-based weights to different dimensions
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String emotion = entry.getKey();
            Double weight = entry.getValue();
            
            // Apply weight to relevant dimensions based on emotion
            if ("sad".equals(emotion) || "anxious".equals(emotion)) {
                // Increase vulnerability dimension
                if (weightedVector.size() > 4) {
                    weightedVector.set(4, weightedVector.get(4) * (1.0 + weight));
                }
            } else if ("happy".equals(emotion) || "excited".equals(emotion)) {
                // Increase positivity dimension
                if (weightedVector.size() > 3) {
                    weightedVector.set(3, weightedVector.get(3) * (1.0 + weight));
                }
            }
        }
        
        // Normalize to [0, 1] range
        for (int i = 0; i < weightedVector.size(); i++) {
            weightedVector.set(i, Math.max(0.0, Math.min(1.0, weightedVector.get(i))));
        }
        
        return weightedVector;
    }
    
    private double calculateWeightConfidence(Map<String, Double> weights) {
        // Confidence based on weight distribution
        if (weights.isEmpty()) {
            return 0.5;
        }
        
        double maxWeight = weights.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double weightSum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Higher confidence when weights are more evenly distributed
        double concentration = maxWeight / weightSum;
        return 1.0 - concentration;
    }
    
    private Map<String, Object> calculateTrendMetrics(List<Map<String, Object>> trendHistory) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (trendHistory.isEmpty()) {
            metrics.put("trendDirection", "STABLE");
            metrics.put("trendStrength", 0.0);
            metrics.put("volatility", 0.0);
            return metrics;
        }
        
        // Calculate trend direction and strength
        if (trendHistory.size() >= 2) {
            Map<String, Object> firstEntry = trendHistory.get(0);
            Map<String, Object> lastEntry = trendHistory.get(trendHistory.size() - 1);
            
            List<Double> firstVector = (List<Double>) firstEntry.get("personalityVector");
            List<Double> lastVector = (List<Double>) lastEntry.get("personalityVector");
            
            if (firstVector != null && lastVector != null && firstVector.size() == lastVector.size()) {
                double trendStrength = 0.0;
                for (int i = 0; i < firstVector.size(); i++) {
                    trendStrength += Math.abs(lastVector.get(i) - firstVector.get(i));
                }
                
                metrics.put("trendStrength", trendStrength);
                
                if (trendStrength > 0.5) {
                    metrics.put("trendDirection", "CHANGING");
                } else if (trendStrength > 0.2) {
                    metrics.put("trendDirection", "EVOLVING");
                } else {
                    metrics.put("trendDirection", "STABLE");
                }
            }
        }
        
        // Calculate volatility
        double volatility = calculateVolatility(trendHistory);
        metrics.put("volatility", volatility);
        
        return metrics;
    }
    
    private double calculateVolatility(List<Map<String, Object>> trendHistory) {
        if (trendHistory.size() < 2) {
            return 0.0;
        }
        
        List<Double> confidences = trendHistory.stream()
            .map(entry -> (Double) entry.get("confidence"))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (confidences.size() < 2) {
            return 0.0;
        }
        
        double mean = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = confidences.stream()
            .mapToDouble(conf -> Math.pow(conf - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private Map<String, Object> createDefaultPersonalityVector(UUID userId) {
        Map<String, Object> defaultVector = new HashMap<>();
        defaultVector.put("userId", userId.toString());
        defaultVector.put("personalityVector", Arrays.asList(0.5, 0.5, 0.5, 0.5, 0.5));
        defaultVector.put("confidence", 0.1);
        defaultVector.put("computationMethod", "DEFAULT");
        defaultVector.put("timestamp", LocalDateTime.now());
        return defaultVector;
    }
}
