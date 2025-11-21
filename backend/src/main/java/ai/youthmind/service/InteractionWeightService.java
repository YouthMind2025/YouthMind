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
 * Service for managing interaction weight (β parameter) for interaction intensity
 * Implements sophisticated weight calculation and adjustment algorithms
 */
@Service
@Transactional
public class InteractionWeightService {
    
    private static final Logger logger = LoggerFactory.getLogger(InteractionWeightService.class);
    
    // Interaction weight algorithm constants
    private static final double BASE_BETA = 1.0; // Base β parameter
    private static final double MIN_BETA = 0.1;  // Minimum β value
    private static final double MAX_BETA = 3.0;  // Maximum β value
    private static final double WEIGHT_ADJUSTMENT_FACTOR = 0.1;
    private static final long WEIGHT_TRACKING_HOURS = 24;
    private static final long WEIGHT_HISTORY_DAYS = 7;
    
    // Redis keys
    private static final String WEIGHT_STATE_KEY = "interaction_weight:";
    private static final String WEIGHT_HISTORY_KEY = "weight_history:";
    private static final String WEIGHT_STATISTICS_KEY = "weight_stats:";
    private static final String WEIGHT_PATTERN_KEY = "weight_pattern:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate β parameter for interaction intensity
     * @return Beta value for interaction intensity
     */
    public double calculateBeta() {
        try {
            logger.debug("Calculating beta parameter for interaction intensity");
            
            // Get global interaction statistics
            Map<String, Object> globalStats = getGlobalInteractionStatistics();
            
            // Calculate base beta
            double baseBeta = calculateBaseBeta(globalStats);
            
            // Apply temporal adjustments
            double temporalAdjustment = calculateTemporalAdjustment();
            
            // Apply user engagement adjustments
            double engagementAdjustment = calculateEngagementAdjustment(globalStats);
            
            // Apply system load adjustments
            double loadAdjustment = calculateSystemLoadAdjustment();
            
            // Calculate final beta
            double finalBeta = baseBeta * temporalAdjustment * engagementAdjustment * loadAdjustment;
            
            // Normalize to valid range
            finalBeta = Math.max(MIN_BETA, Math.min(MAX_BETA, finalBeta));
            
            logger.debug("Beta parameter calculated: {}", finalBeta);
            return finalBeta;
            
        } catch (Exception e) {
            logger.error("Error calculating beta parameter: {}", e.getMessage(), e);
            return BASE_BETA; // Return default value on error
        }
    }
    
    /**
     * Calculate β parameter for specific user
     * @param userId User ID
     * @return User-specific beta value
     */
    public double calculateBeta(UUID userId) {
        try {
            logger.debug("Calculating user-specific beta parameter for user {}", userId);
            
            // Get user interaction statistics
            Map<String, Object> userStats = getUserInteractionStatistics(userId);
            
            // Calculate base beta for user
            double baseBeta = calculateUserBaseBeta(userStats);
            
            // Apply user-specific adjustments
            double userAdjustment = calculateUserSpecificAdjustment(userId, userStats);
            
            // Apply temporal adjustments
            double temporalAdjustment = calculateUserTemporalAdjustment(userId);
            
            // Calculate final beta
            double finalBeta = baseBeta * userAdjustment * temporalAdjustment;
            
            // Normalize to valid range
            finalBeta = Math.max(MIN_BETA, Math.min(MAX_BETA, finalBeta));
            
            logger.debug("User-specific beta parameter calculated: {} for user {}", finalBeta, userId);
            return finalBeta;
            
        } catch (Exception e) {
            logger.error("Error calculating user-specific beta parameter for user {}: {}", userId, e.getMessage(), e);
            return BASE_BETA; // Return default value on error
        }
    }
    
    /**
     * Adjust interaction weight based on recent performance
     * @param userId User ID (optional, null for global adjustment)
     * @param performanceMetrics Performance metrics
     */
    public void adjustWeight(UUID userId, Map<String, Object> performanceMetrics) {
        try {
            logger.debug("Adjusting interaction weight for user {} with metrics {}", userId, performanceMetrics);
            
            // Calculate weight adjustment
            double adjustment = calculateWeightAdjustment(performanceMetrics);
            
            // Apply adjustment
            if (userId != null) {
                adjustUserWeight(userId, adjustment);
            } else {
                adjustGlobalWeight(adjustment);
            }
            
            // Track weight history
            trackWeightHistory(userId, adjustment, performanceMetrics);
            
            logger.debug("Interaction weight adjusted by {} for user {}", adjustment, userId);
            
        } catch (Exception e) {
            logger.error("Error adjusting interaction weight for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Track weight history for analysis
     * @param userId User ID (optional, null for global tracking)
     * @param adjustment Weight adjustment made
     * @param performanceMetrics Performance metrics that triggered adjustment
     */
    public void trackWeightHistory(UUID userId, double adjustment, Map<String, Object> performanceMetrics) {
        try {
            logger.debug("Tracking weight history for user {} with adjustment {}", userId, adjustment);
            
            String historyKey = userId != null ? WEIGHT_HISTORY_KEY + userId : WEIGHT_HISTORY_KEY + "global";
            
            // Get existing history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            if (history == null) {
                history = new HashMap<>();
            }
            
            // Add new entry
            List<Map<String, Object>> weightHistory = (List<Map<String, Object>>) history.get("weight_history");
            if (weightHistory == null) {
                weightHistory = new ArrayList<>();
            }
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("timestamp", LocalDateTime.now());
            entry.put("adjustment", adjustment);
            entry.put("performance_metrics", performanceMetrics);
            entry.put("beta_after", calculateBeta(userId));
            
            weightHistory.add(entry);
            
            // Keep only recent history
            if (weightHistory.size() > 100) {
                weightHistory = weightHistory.subList(weightHistory.size() - 100, weightHistory.size());
            }
            
            history.put("weight_history", weightHistory);
            history.put("last_updated", LocalDateTime.now());
            
            // Store updated history
            redisTemplate.opsForValue().set(historyKey, history, WEIGHT_HISTORY_DAYS, TimeUnit.DAYS);
            
            logger.debug("Weight history tracked for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error tracking weight history for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get weight statistics for a user
     * @param userId User ID (optional, null for global statistics)
     * @return Weight statistics
     */
    public Map<String, Object> getWeightStatistics(UUID userId) {
        try {
            logger.debug("Getting weight statistics for user {}", userId);
            
            String statsKey = userId != null ? WEIGHT_STATISTICS_KEY + userId : WEIGHT_STATISTICS_KEY + "global";
            
            // Try to get cached statistics first
            Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
            if (cachedStats != null) {
                return cachedStats;
            }
            
            // Calculate fresh statistics
            Map<String, Object> stats = calculateWeightStatistics(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(statsKey, stats, 1, TimeUnit.HOURS);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting weight statistics for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get weight patterns for a user
     * @param userId User ID (optional, null for global patterns)
     * @return Weight patterns
     */
    public Map<String, Object> getWeightPatterns(UUID userId) {
        try {
            logger.debug("Getting weight patterns for user {}", userId);
            
            String patternKey = userId != null ? WEIGHT_PATTERN_KEY + userId : WEIGHT_PATTERN_KEY + "global";
            
            // Try to get cached patterns first
            Map<String, Object> cachedPatterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternKey);
            if (cachedPatterns != null) {
                return cachedPatterns;
            }
            
            // Calculate fresh patterns
            Map<String, Object> patterns = calculateWeightPatterns(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(patternKey, patterns, 2, TimeUnit.HOURS);
            
            return patterns;
            
        } catch (Exception e) {
            logger.error("Error getting weight patterns for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<String, Object> getGlobalInteractionStatistics() {
        try {
            // Get recent interaction statistics
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(WEIGHT_TRACKING_HOURS);
            
            // Count recent resonances
            long recentResonances = postResonanceRepository.countByResonancedAtAfter(cutoffTime);
            
            // Count total posts
            long totalPosts = postRepository.count();
            
            // Calculate interaction rate
            double interactionRate = totalPosts > 0 ? (double) recentResonances / totalPosts : 0.0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("recent_resonances", recentResonances);
            stats.put("total_posts", totalPosts);
            stats.put("interaction_rate", interactionRate);
            stats.put("timestamp", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.warn("Error getting global interaction statistics: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    private double calculateBaseBeta(Map<String, Object> globalStats) {
        // Calculate base beta based on global interaction statistics
        Double interactionRate = (Double) globalStats.get("interaction_rate");
        if (interactionRate == null) {
            return BASE_BETA;
        }
        
        // Adjust base beta based on interaction rate
        if (interactionRate > 0.1) {
            return BASE_BETA * 1.2; // Increase beta for high interaction rates
        } else if (interactionRate < 0.01) {
            return BASE_BETA * 0.8; // Decrease beta for low interaction rates
        }
        
        return BASE_BETA;
    }
    
    private double calculateTemporalAdjustment() {
        // Calculate temporal adjustment based on time of day
        int currentHour = LocalDateTime.now().getHour();
        
        // Peak hours (9-11 AM, 7-9 PM) get higher beta
        if ((currentHour >= 9 && currentHour <= 11) || (currentHour >= 19 && currentHour <= 21)) {
            return 1.1;
        }
        // Off-peak hours (2-6 AM) get lower beta
        else if (currentHour >= 2 && currentHour <= 6) {
            return 0.9;
        }
        
        return 1.0; // Normal adjustment for other hours
    }
    
    private double calculateEngagementAdjustment(Map<String, Object> globalStats) {
        // Calculate engagement adjustment based on recent activity
        Long recentResonances = (Long) globalStats.get("recent_resonances");
        if (recentResonances == null) {
            return 1.0;
        }
        
        // Adjust based on recent resonance count
        if (recentResonances > 100) {
            return 1.15; // High engagement
        } else if (recentResonances > 50) {
            return 1.05; // Medium engagement
        } else if (recentResonances < 10) {
            return 0.95; // Low engagement
        }
        
        return 1.0; // Normal engagement
    }
    
    private double calculateSystemLoadAdjustment() {
        // Calculate system load adjustment (simplified implementation)
        // In a real implementation, this would consider CPU usage, memory usage, etc.
        return 1.0; // Placeholder for system load adjustment
    }
    
    private Map<String, Object> getUserInteractionStatistics(UUID userId) {
        try {
            // Get user's recent interactions
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(WEIGHT_TRACKING_HOURS);
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, cutoffTime);
            
            // Get user's total interactions
            long totalResonances = postResonanceRepository.countByUserId(userId);
            
            // Calculate user interaction rate
            double userInteractionRate = recentResonances.size() / (double) WEIGHT_TRACKING_HOURS;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("recent_resonances", recentResonances.size());
            stats.put("total_resonances", totalResonances);
            stats.put("interaction_rate", userInteractionRate);
            stats.put("timestamp", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.warn("Error getting user interaction statistics: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    private double calculateUserBaseBeta(Map<String, Object> userStats) {
        // Calculate base beta for user based on their interaction patterns
        Double interactionRate = (Double) userStats.get("interaction_rate");
        if (interactionRate == null) {
            return BASE_BETA;
        }
        
        // Adjust base beta based on user's interaction rate
        if (interactionRate > 2.0) {
            return BASE_BETA * 1.3; // High-activity user
        } else if (interactionRate > 1.0) {
            return BASE_BETA * 1.1; // Medium-activity user
        } else if (interactionRate < 0.1) {
            return BASE_BETA * 0.9; // Low-activity user
        }
        
        return BASE_BETA;
    }
    
    private double calculateUserSpecificAdjustment(UUID userId, Map<String, Object> userStats) {
        // Calculate user-specific adjustment based on their behavior patterns
        Long totalResonances = (Long) userStats.get("total_resonances");
        if (totalResonances == null) {
            return 1.0;
        }
        
        // Adjust based on user's total interaction history
        if (totalResonances > 100) {
            return 1.1; // Experienced user
        } else if (totalResonances < 10) {
            return 0.95; // New user
        }
        
        return 1.0; // Regular user
    }
    
    private double calculateUserTemporalAdjustment(UUID userId) {
        // Calculate user-specific temporal adjustment
        // This could be based on user's historical activity patterns
        return 1.0; // Placeholder for user temporal adjustment
    }
    
    private double calculateWeightAdjustment(Map<String, Object> performanceMetrics) {
        // Calculate weight adjustment based on performance metrics
        Double successRate = (Double) performanceMetrics.get("success_rate");
        Double responseTime = (Double) performanceMetrics.get("response_time");
        Double userSatisfaction = (Double) performanceMetrics.get("user_satisfaction");
        
        double adjustment = 0.0;
        
        // Adjust based on success rate
        if (successRate != null) {
            if (successRate > 0.9) {
                adjustment += 0.05; // Increase weight for high success
            } else if (successRate < 0.7) {
                adjustment -= 0.05; // Decrease weight for low success
            }
        }
        
        // Adjust based on response time
        if (responseTime != null) {
            if (responseTime < 1000) { // Less than 1 second
                adjustment += 0.03; // Increase weight for fast response
            } else if (responseTime > 5000) { // More than 5 seconds
                adjustment -= 0.03; // Decrease weight for slow response
            }
        }
        
        // Adjust based on user satisfaction
        if (userSatisfaction != null) {
            if (userSatisfaction > 0.8) {
                adjustment += 0.02; // Increase weight for high satisfaction
            } else if (userSatisfaction < 0.5) {
                adjustment -= 0.02; // Decrease weight for low satisfaction
            }
        }
        
        // Limit adjustment to reasonable range
        return Math.max(-0.1, Math.min(0.1, adjustment));
    }
    
    private void adjustUserWeight(UUID userId, double adjustment) {
        try {
            // Get current user weight state
            String stateKey = WEIGHT_STATE_KEY + userId;
            Map<String, Object> weightState = (Map<String, Object>) redisTemplate.opsForValue().get(stateKey);
            
            if (weightState == null) {
                weightState = new HashMap<>();
            }
            
            // Update weight
            Double currentWeight = (Double) weightState.get("current_weight");
            if (currentWeight == null) {
                currentWeight = BASE_BETA;
            }
            
            double newWeight = currentWeight + adjustment;
            newWeight = Math.max(MIN_BETA, Math.min(MAX_BETA, newWeight));
            
            weightState.put("current_weight", newWeight);
            weightState.put("last_adjustment", adjustment);
            weightState.put("last_updated", LocalDateTime.now());
            
            // Store updated state
            redisTemplate.opsForValue().set(stateKey, weightState, WEIGHT_HISTORY_DAYS, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Error adjusting user weight: {}", e.getMessage());
        }
    }
    
    private void adjustGlobalWeight(double adjustment) {
        try {
            // Get current global weight state
            String stateKey = WEIGHT_STATE_KEY + "global";
            Map<String, Object> weightState = (Map<String, Object>) redisTemplate.opsForValue().get(stateKey);
            
            if (weightState == null) {
                weightState = new HashMap<>();
            }
            
            // Update global weight
            Double currentWeight = (Double) weightState.get("current_weight");
            if (currentWeight == null) {
                currentWeight = BASE_BETA;
            }
            
            double newWeight = currentWeight + adjustment;
            newWeight = Math.max(MIN_BETA, Math.min(MAX_BETA, newWeight));
            
            weightState.put("current_weight", newWeight);
            weightState.put("last_adjustment", adjustment);
            weightState.put("last_updated", LocalDateTime.now());
            
            // Store updated state
            redisTemplate.opsForValue().set(stateKey, weightState, WEIGHT_HISTORY_DAYS, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Error adjusting global weight: {}", e.getMessage());
        }
    }
    
    private Map<String, Object> calculateWeightStatistics(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get weight history
        String historyKey = userId != null ? WEIGHT_HISTORY_KEY + userId : WEIGHT_HISTORY_KEY + "global";
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            stats.put("total_adjustments", 0);
            stats.put("avg_adjustment", 0.0);
            stats.put("current_weight", BASE_BETA);
            return stats;
        }
        
        List<Map<String, Object>> weightHistory = (List<Map<String, Object>>) history.get("weight_history");
        if (weightHistory == null || weightHistory.isEmpty()) {
            stats.put("total_adjustments", 0);
            stats.put("avg_adjustment", 0.0);
            stats.put("current_weight", BASE_BETA);
            return stats;
        }
        
        // Calculate statistics
        int totalAdjustments = weightHistory.size();
        double avgAdjustment = weightHistory.stream()
            .mapToDouble(entry -> ((Number) entry.get("adjustment")).doubleValue())
            .average().orElse(0.0);
        
        double currentWeight = calculateBeta(userId);
        
        stats.put("total_adjustments", totalAdjustments);
        stats.put("avg_adjustment", avgAdjustment);
        stats.put("current_weight", currentWeight);
        
        // Calculate weight volatility
        double weightVariance = weightHistory.stream()
            .mapToDouble(entry -> {
                double weight = ((Number) entry.get("beta_after")).doubleValue();
                return Math.pow(weight - currentWeight, 2);
            })
            .average().orElse(0.0);
        
        double weightVolatility = Math.sqrt(weightVariance);
        stats.put("weight_volatility", weightVolatility);
        
        return stats;
    }
    
    private Map<String, Object> calculateWeightPatterns(UUID userId) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Get weight history
        String historyKey = userId != null ? WEIGHT_HISTORY_KEY + userId : WEIGHT_HISTORY_KEY + "global";
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            return patterns;
        }
        
        List<Map<String, Object>> weightHistory = (List<Map<String, Object>>) history.get("weight_history");
        if (weightHistory == null || weightHistory.isEmpty()) {
            return patterns;
        }
        
        // Analyze temporal patterns
        Map<String, Object> temporalPatterns = analyzeTemporalWeightPatterns(weightHistory);
        patterns.put("temporal", temporalPatterns);
        
        // Analyze adjustment patterns
        Map<String, Object> adjustmentPatterns = analyzeAdjustmentPatterns(weightHistory);
        patterns.put("adjustment", adjustmentPatterns);
        
        // Analyze performance patterns
        Map<String, Object> performancePatterns = analyzePerformancePatterns(weightHistory);
        patterns.put("performance", performancePatterns);
        
        return patterns;
    }
    
    private Map<String, Object> analyzeTemporalWeightPatterns(List<Map<String, Object>> weightHistory) {
        Map<String, Object> temporalPatterns = new HashMap<>();
        
        // Analyze hour-of-day patterns
        Map<Integer, Long> hourDistribution = new HashMap<>();
        for (Map<String, Object> entry : weightHistory) {
            LocalDateTime timestamp = (LocalDateTime) entry.get("timestamp");
            int hour = timestamp.getHour();
            hourDistribution.merge(hour, 1L, Long::sum);
        }
        
        temporalPatterns.put("hour_distribution", hourDistribution);
        
        // Find peak adjustment hours
        Optional<Map.Entry<Integer, Long>> peakHour = hourDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (peakHour.isPresent()) {
            temporalPatterns.put("peak_adjustment_hour", peakHour.get().getKey());
        }
        
        return temporalPatterns;
    }
    
    private Map<String, Object> analyzeAdjustmentPatterns(List<Map<String, Object>> weightHistory) {
        Map<String, Object> adjustmentPatterns = new HashMap<>();
        
        // Analyze adjustment directions
        long positiveAdjustments = weightHistory.stream()
            .mapToLong(entry -> ((Number) entry.get("adjustment")).doubleValue() > 0 ? 1 : 0)
            .sum();
        
        long negativeAdjustments = weightHistory.stream()
            .mapToLong(entry -> ((Number) entry.get("adjustment")).doubleValue() < 0 ? 1 : 0)
            .sum();
        
        adjustmentPatterns.put("positive_adjustments", positiveAdjustments);
        adjustmentPatterns.put("negative_adjustments", negativeAdjustments);
        adjustmentPatterns.put("adjustment_ratio", positiveAdjustments / (double) Math.max(1, negativeAdjustments));
        
        return adjustmentPatterns;
    }
    
    private Map<String, Object> analyzePerformancePatterns(List<Map<String, Object>> weightHistory) {
        Map<String, Object> performancePatterns = new HashMap<>();
        
        // Analyze performance metrics correlation
        double avgAdjustment = weightHistory.stream()
            .mapToDouble(entry -> ((Number) entry.get("adjustment")).doubleValue())
            .average().orElse(0.0);
        
        performancePatterns.put("avg_adjustment", avgAdjustment);
        
        // Calculate adjustment consistency
        double adjustmentVariance = weightHistory.stream()
            .mapToDouble(entry -> {
                double adjustment = ((Number) entry.get("adjustment")).doubleValue();
                return Math.pow(adjustment - avgAdjustment, 2);
            })
            .average().orElse(0.0);
        
        double adjustmentConsistency = Math.max(0.0, 1.0 - Math.sqrt(adjustmentVariance));
        performancePatterns.put("adjustment_consistency", adjustmentConsistency);
        
        return performancePatterns;
    }
}
