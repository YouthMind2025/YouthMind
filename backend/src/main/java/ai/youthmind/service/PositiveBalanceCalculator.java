package ai.youthmind.service;

import ai.youthmind.entity.PostResonance;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.entity.Post;
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
 * Service for calculating positive balance vector (μ parameter)
 * Implements sophisticated positive balance algorithms for interaction modulation
 */
@Service
@Transactional
public class PositiveBalanceCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(PositiveBalanceCalculator.class);
    
    // Positive balance algorithm constants
    private static final double POSITIVE_BALANCE_THRESHOLD = 0.7;
    private static final double BALANCE_DECAY_FACTOR = 0.95;
    private static final long BALANCE_TRACKING_DAYS = 30;
    private static final double POSITIVE_WEIGHT = 1.0;
    private static final double NEGATIVE_WEIGHT = -0.5;
    
    // Redis keys
    private static final String BALANCE_VECTOR_KEY = "balance_vector:";
    private static final String BALANCE_TREND_KEY = "balance_trend:";
    private static final String BALANCE_HISTORY_KEY = "balance_history:";
    private static final String BALANCE_STATISTICS_KEY = "balance_stats:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate positive balance for user-post interaction
     * @param userId User ID
     * @param postId Post ID
     * @return Positive balance value (μ parameter)
     */
    public double calculatePositiveBalance(UUID userId, UUID postId) {
        try {
            logger.debug("Calculating positive balance for user {} and post {}", userId, postId);
            
            // Get post information
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                logger.warn("Post not found for positive balance calculation: {}", postId);
                return 0.0;
            }
            
            Post post = postOpt.get();
            
            // Calculate base positive balance
            double baseBalance = calculateBasePositiveBalance(post);
            
            // Apply user-specific balance factors
            double userBalanceFactor = getUserBalanceFactor(userId);
            
            // Apply temporal balance factors
            double temporalFactor = calculateTemporalBalanceFactor(post);
            
            // Apply interaction history balance
            double historyBalance = calculateInteractionHistoryBalance(userId, postId);
            
            // Calculate final positive balance (μ parameter)
            double positiveBalance = (baseBalance * userBalanceFactor * temporalFactor) + historyBalance;
            
            // Normalize to [0, 1] range
            positiveBalance = Math.max(0.0, Math.min(1.0, positiveBalance));
            
            logger.debug("Positive balance calculated: {} for user {} and post {}", positiveBalance, userId, postId);
            return positiveBalance;
            
        } catch (Exception e) {
            logger.error("Error calculating positive balance for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Update balance vector for user-post interaction
     * @param userId User ID
     * @param postId Post ID
     * @param balanceValue Balance value to update
     */
    public void updateBalanceVector(UUID userId, UUID postId, double balanceValue) {
        try {
            logger.debug("Updating balance vector for user {} and post {} with value {}", userId, postId, balanceValue);
            
            // Get existing balance vector
            String vectorKey = BALANCE_VECTOR_KEY + userId;
            Map<String, Object> balanceVector = (Map<String, Object>) redisTemplate.opsForValue().get(vectorKey);
            
            if (balanceVector == null) {
                balanceVector = new HashMap<>();
            }
            
            // Update balance vector
            balanceVector.put("current_balance", balanceValue);
            balanceVector.put("last_updated", LocalDateTime.now());
            balanceVector.put("last_post_id", postId.toString());
            
            // Update balance components
            updateBalanceComponents(balanceVector, userId, postId, balanceValue);
            
            // Store updated balance vector
            redisTemplate.opsForValue().set(vectorKey, balanceVector, BALANCE_TRACKING_DAYS, TimeUnit.DAYS);
            
            // Update balance history
            updateBalanceHistory(userId, postId, balanceValue);
            
            logger.debug("Balance vector updated for user {}: {}", userId, balanceVector);
            
        } catch (Exception e) {
            logger.error("Error updating balance vector for user {} and post {}: {}", userId, postId, e.getMessage(), e);
        }
    }
    
    /**
     * Track balance trends for a user
     * @param userId User ID
     * @param balanceValue Current balance value
     */
    public void trackBalanceTrends(UUID userId, double balanceValue) {
        try {
            logger.debug("Tracking balance trends for user {} with value {}", userId, balanceValue);
            
            // Get existing trend data
            String trendKey = BALANCE_TREND_KEY + userId;
            Map<String, Object> trendData = (Map<String, Object>) redisTemplate.opsForValue().get(trendKey);
            
            if (trendData == null) {
                trendData = new HashMap<>();
            }
            
            // Update trend data
            List<Double> balanceHistory = (List<Double>) trendData.get("balance_history");
            if (balanceHistory == null) {
                balanceHistory = new ArrayList<>();
            }
            
            balanceHistory.add(balanceValue);
            
            // Keep only recent history (last 100 points)
            if (balanceHistory.size() > 100) {
                balanceHistory = balanceHistory.subList(balanceHistory.size() - 100, balanceHistory.size());
            }
            
            trendData.put("balance_history", balanceHistory);
            trendData.put("last_updated", LocalDateTime.now());
            
            // Calculate trend metrics
            calculateTrendMetrics(trendData, balanceHistory);
            
            // Store updated trend data
            redisTemplate.opsForValue().set(trendKey, trendData, BALANCE_TRACKING_DAYS, TimeUnit.DAYS);
            
            logger.debug("Balance trends tracked for user {}: {}", userId, trendData);
            
        } catch (Exception e) {
            logger.error("Error tracking balance trends for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get balance trends for a user
     * @param userId User ID
     * @return Balance trends map
     */
    public Map<String, Object> getBalanceTrends(UUID userId) {
        try {
            logger.debug("Getting balance trends for user {}", userId);
            
            // Try to get cached trends first
            String trendKey = BALANCE_TREND_KEY + userId;
            Map<String, Object> cachedTrends = (Map<String, Object>) redisTemplate.opsForValue().get(trendKey);
            
            if (cachedTrends != null) {
                return cachedTrends;
            }
            
            // Calculate fresh trends
            Map<String, Object> trends = calculateBalanceTrends(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(trendKey, trends, 1, TimeUnit.HOURS);
            
            return trends;
            
        } catch (Exception e) {
            logger.error("Error getting balance trends for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get balance statistics for a user
     * @param userId User ID
     * @return Balance statistics
     */
    public Map<String, Object> getBalanceStatistics(UUID userId) {
        try {
            logger.debug("Getting balance statistics for user {}", userId);
            
            // Try to get cached statistics first
            String statsKey = BALANCE_STATISTICS_KEY + userId;
            Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
            
            if (cachedStats != null) {
                return cachedStats;
            }
            
            // Calculate fresh statistics
            Map<String, Object> stats = calculateBalanceStatistics(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(statsKey, stats, 2, TimeUnit.HOURS);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting balance statistics for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private double calculateBasePositiveBalance(Post post) {
        // Calculate base positive balance based on post characteristics
        double baseBalance = 0.5; // Start with neutral balance
        
        // Factor in post engagement metrics
        if (post.getResonance() > 0) {
            baseBalance += Math.log(post.getResonance() + 1) * 0.1;
        }
        
        if (post.getView() > 0) {
            baseBalance += Math.log(post.getView() + 1) * 0.05;
        }
        
        if (post.getCollect() > 0) {
            baseBalance += Math.log(post.getCollect() + 1) * 0.08;
        }
        
        // Factor in post age (newer posts have higher balance potential)
        long hoursSinceCreation = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        double ageFactor = Math.max(0.1, 1.0 - (hoursSinceCreation / 168.0)); // Decay over a week
        baseBalance *= ageFactor;
        
        return Math.max(0.0, Math.min(1.0, baseBalance));
    }
    
    private double getUserBalanceFactor(UUID userId) {
        try {
            // Get user's balance history
            String historyKey = BALANCE_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            if (history == null) {
                return 1.0; // Default factor
            }
            
            // Calculate user's historical balance tendency
            List<Double> balanceHistory = (List<Double>) history.get("balance_values");
            if (balanceHistory == null || balanceHistory.isEmpty()) {
                return 1.0;
            }
            
            // Calculate average balance
            double avgBalance = balanceHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
            
            // Return factor based on user's balance tendency
            return Math.max(0.5, Math.min(1.5, avgBalance));
            
        } catch (Exception e) {
            logger.warn("Error getting user balance factor: {}", e.getMessage());
            return 1.0;
        }
    }
    
    private double calculateTemporalBalanceFactor(Post post) {
        // Calculate temporal factor based on post age and engagement patterns
        long hoursSinceCreation = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        
        if (hoursSinceCreation <= 24) {
            return 1.0; // Full factor for posts less than 24 hours old
        } else if (hoursSinceCreation <= 168) { // 1 week
            return Math.pow(BALANCE_DECAY_FACTOR, hoursSinceCreation / 24.0);
        } else {
            return 0.2; // Reduced factor for posts older than a week
        }
    }
    
    private double calculateInteractionHistoryBalance(UUID userId, UUID postId) {
        try {
            // Check if user has interacted with this post before
            boolean hasResonanced = postResonanceRepository.existsByUserIdAndPostId(userId, postId);
            
            if (hasResonanced) {
                return POSITIVE_WEIGHT * 0.3; // Positive contribution for previous interaction
            }
            
            // Check user's recent interaction patterns
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, cutoffTime);
            
            if (recentResonances.isEmpty()) {
                return 0.0; // No recent interaction history
            }
            
            // Calculate interaction frequency factor
            double interactionFrequency = recentResonances.size() / 7.0; // Interactions per day
            double frequencyFactor = Math.min(1.0, interactionFrequency / 5.0); // Normalize to max 5 interactions per day
            
            return frequencyFactor * POSITIVE_WEIGHT * 0.1;
            
        } catch (Exception e) {
            logger.warn("Error calculating interaction history balance: {}", e.getMessage());
            return 0.0;
        }
    }
    
    private void updateBalanceComponents(Map<String, Object> balanceVector, UUID userId, UUID postId, double balanceValue) {
        // Update emotional balance component
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            balanceVector.put("emotional_balance", post.getEmotion());
        }
        
        // Update engagement balance component
        Map<String, Object> engagementComponents = new HashMap<>();
        engagementComponents.put("resonance_balance", balanceValue * 0.4);
        engagementComponents.put("linger_balance", balanceValue * 0.3);
        engagementComponents.put("interaction_balance", balanceValue * 0.3);
        
        balanceVector.put("engagement_components", engagementComponents);
        
        // Update balance confidence
        double confidence = calculateBalanceConfidence(userId, balanceValue);
        balanceVector.put("balance_confidence", confidence);
    }
    
    private double calculateBalanceConfidence(UUID userId, double balanceValue) {
        try {
            // Get user's balance history for confidence calculation
            String historyKey = BALANCE_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            if (history == null) {
                return 0.5; // Default confidence
            }
            
            List<Double> balanceHistory = (List<Double>) history.get("balance_values");
            if (balanceHistory == null || balanceHistory.size() < 3) {
                return 0.5;
            }
            
            // Calculate confidence based on consistency
            double avgBalance = balanceHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
            double variance = balanceHistory.stream()
                .mapToDouble(value -> Math.pow(value - avgBalance, 2))
                .average().orElse(0.0);
            
            double stdDev = Math.sqrt(variance);
            double confidence = Math.max(0.0, 1.0 - (stdDev / 0.5)); // Normalize by expected std dev
            
            return confidence;
            
        } catch (Exception e) {
            logger.warn("Error calculating balance confidence: {}", e.getMessage());
            return 0.5;
        }
    }
    
    private void updateBalanceHistory(UUID userId, UUID postId, double balanceValue) {
        try {
            // Get existing history
            String historyKey = BALANCE_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            if (history == null) {
                history = new HashMap<>();
            }
            
            // Update balance values
            List<Double> balanceValues = (List<Double>) history.get("balance_values");
            if (balanceValues == null) {
                balanceValues = new ArrayList<>();
            }
            
            balanceValues.add(balanceValue);
            
            // Keep only recent history (last 100 values)
            if (balanceValues.size() > 100) {
                balanceValues = balanceValues.subList(balanceValues.size() - 100, balanceValues.size());
            }
            
            history.put("balance_values", balanceValues);
            history.put("last_updated", LocalDateTime.now());
            
            // Update post history
            List<String> postHistory = (List<String>) history.get("post_history");
            if (postHistory == null) {
                postHistory = new ArrayList<>();
            }
            
            postHistory.add(postId.toString());
            
            // Keep only recent post history (last 50 posts)
            if (postHistory.size() > 50) {
                postHistory = postHistory.subList(postHistory.size() - 50, postHistory.size());
            }
            
            history.put("post_history", postHistory);
            
            // Store updated history
            redisTemplate.opsForValue().set(historyKey, history, BALANCE_TRACKING_DAYS, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Error updating balance history: {}", e.getMessage());
        }
    }
    
    private void calculateTrendMetrics(Map<String, Object> trendData, List<Double> balanceHistory) {
        if (balanceHistory.size() < 2) {
            trendData.put("trend_direction", "stable");
            trendData.put("trend_strength", 0.0);
            return;
        }
        
        // Calculate trend direction and strength
        double firstHalf = balanceHistory.subList(0, balanceHistory.size() / 2).stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
        double secondHalf = balanceHistory.subList(balanceHistory.size() / 2, balanceHistory.size()).stream()
            .mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double trendStrength = Math.abs(secondHalf - firstHalf);
        String trendDirection = secondHalf > firstHalf ? "increasing" : 
                               secondHalf < firstHalf ? "decreasing" : "stable";
        
        trendData.put("trend_direction", trendDirection);
        trendData.put("trend_strength", trendStrength);
        
        // Calculate trend consistency
        double consistency = calculateTrendConsistency(balanceHistory);
        trendData.put("trend_consistency", consistency);
    }
    
    private double calculateTrendConsistency(List<Double> balanceHistory) {
        if (balanceHistory.size() < 3) {
            return 0.5;
        }
        
        // Calculate consistency based on consecutive differences
        List<Double> differences = new ArrayList<>();
        for (int i = 1; i < balanceHistory.size(); i++) {
            differences.add(balanceHistory.get(i) - balanceHistory.get(i-1));
        }
        
        // Calculate consistency based on sign consistency
        long positiveCount = differences.stream().mapToLong(d -> d > 0 ? 1 : 0).sum();
        long negativeCount = differences.stream().mapToLong(d -> d < 0 ? 1 : 0).sum();
        
        double consistency = Math.max(positiveCount, negativeCount) / (double) differences.size();
        return consistency;
    }
    
    private Map<String, Object> calculateBalanceTrends(UUID userId) {
        Map<String, Object> trends = new HashMap<>();
        
        // Get user's balance history
        String historyKey = BALANCE_HISTORY_KEY + userId;
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            trends.put("trend_direction", "stable");
            trends.put("trend_strength", 0.0);
            trends.put("trend_consistency", 0.5);
            return trends;
        }
        
        List<Double> balanceValues = (List<Double>) history.get("balance_values");
        if (balanceValues == null || balanceValues.isEmpty()) {
            trends.put("trend_direction", "stable");
            trends.put("trend_strength", 0.0);
            trends.put("trend_consistency", 0.5);
            return trends;
        }
        
        // Calculate trend metrics
        calculateTrendMetrics(trends, balanceValues);
        
        // Calculate balance statistics
        double avgBalance = balanceValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double maxBalance = balanceValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minBalance = balanceValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        
        trends.put("avg_balance", avgBalance);
        trends.put("max_balance", maxBalance);
        trends.put("min_balance", minBalance);
        trends.put("balance_range", maxBalance - minBalance);
        
        return trends;
    }
    
    private Map<String, Object> calculateBalanceStatistics(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get user's balance history
        String historyKey = BALANCE_HISTORY_KEY + userId;
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            stats.put("total_interactions", 0);
            stats.put("avg_balance", 0.0);
            stats.put("balance_volatility", 0.0);
            return stats;
        }
        
        List<Double> balanceValues = (List<Double>) history.get("balance_values");
        if (balanceValues == null || balanceValues.isEmpty()) {
            stats.put("total_interactions", 0);
            stats.put("avg_balance", 0.0);
            stats.put("balance_volatility", 0.0);
            return stats;
        }
        
        // Calculate statistics
        int totalInteractions = balanceValues.size();
        double avgBalance = balanceValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Calculate volatility (standard deviation)
        double variance = balanceValues.stream()
            .mapToDouble(value -> Math.pow(value - avgBalance, 2))
            .average().orElse(0.0);
        double volatility = Math.sqrt(variance);
        
        stats.put("total_interactions", totalInteractions);
        stats.put("avg_balance", avgBalance);
        stats.put("balance_volatility", volatility);
        
        // Calculate balance distribution
        long positiveCount = balanceValues.stream().mapToLong(value -> value > 0.5 ? 1 : 0).sum();
        long negativeCount = balanceValues.stream().mapToLong(value -> value < 0.5 ? 1 : 0).sum();
        
        stats.put("positive_interactions", positiveCount);
        stats.put("negative_interactions", negativeCount);
        stats.put("positive_ratio", positiveCount / (double) totalInteractions);
        
        return stats;
    }
}
