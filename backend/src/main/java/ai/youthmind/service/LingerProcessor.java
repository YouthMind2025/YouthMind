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
 * Service for processing linger-based state modulation
 * Implements sophisticated linger algorithms with Ws = 0.01 weight
 */
@Service
@Transactional
public class LingerProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(LingerProcessor.class);
    
    // Linger algorithm constants
    private static final double LINGER_WEIGHT = 0.01; // Ws = 0.01
    private static final long MIN_LINGER_SECONDS = 5; // Minimum linger time
    private static final long MAX_LINGER_SECONDS = 3600; // Maximum linger time (1 hour)
    private static final long LINGER_TRACKING_HOURS = 72; // Track linger for 3 days
    private static final double LINGER_DECAY_FACTOR = 0.98;
    
    // Redis keys
    private static final String LINGER_STATE_KEY = "linger_state:";
    private static final String LINGER_DURATION_KEY = "linger_duration:";
    private static final String LINGER_PATTERN_KEY = "linger_pattern:";
    private static final String LINGER_STATISTICS_KEY = "linger_stats:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate linger impact on user state
     * @param lingerDuration Duration in seconds
     * @return Linger impact value
     */
    public double calculateLingerImpact(long lingerDuration) {
        try {
            logger.debug("Calculating linger impact for duration: {} seconds", lingerDuration);
            
            // Normalize linger duration to [0, 1] range
            double normalizedDuration = normalizeLingerDuration(lingerDuration);
            
            // Apply logarithmic scaling for diminishing returns
            double logScaledDuration = Math.log(normalizedDuration + 1) / Math.log(2);
            
            // Calculate base impact
            double baseImpact = logScaledDuration * LINGER_WEIGHT;
            
            // Apply engagement threshold
            if (lingerDuration < MIN_LINGER_SECONDS) {
                baseImpact *= 0.1; // Minimal impact for very short linger
            } else if (lingerDuration > MAX_LINGER_SECONDS) {
                baseImpact *= 0.8; // Slightly reduced impact for very long linger
            }
            
            logger.debug("Linger impact calculated: {} for duration: {} seconds", baseImpact, lingerDuration);
            return baseImpact;
            
        } catch (Exception e) {
            logger.error("Error calculating linger impact for duration {}: {}", lingerDuration, e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Update user state based on linger interaction
     * @param userId User ID
     * @param postId Post ID
     * @param lingerDuration Duration in seconds
     * @return State update map
     */
    public Map<String, Object> updateStateFromLinger(UUID userId, UUID postId, long lingerDuration) {
        try {
            logger.debug("Updating state from linger for user {} and post {} with duration {}", userId, postId, lingerDuration);
            
            Map<String, Object> stateUpdate = new HashMap<>();
            
            // Calculate linger impact
            double lingerImpact = calculateLingerImpact(lingerDuration);
            
            // Update linger statistics
            Map<String, Object> lingerStats = updateLingerStatistics(userId, lingerDuration);
            stateUpdate.put("linger_statistics", lingerStats);
            
            // Update linger patterns
            Map<String, Object> lingerPatterns = updateLingerPatterns(userId, postId, lingerDuration);
            stateUpdate.put("linger_patterns", lingerPatterns);
            
            // Update attention state
            Map<String, Object> attentionState = updateAttentionState(userId, lingerDuration);
            stateUpdate.put("attention_state", attentionState);
            
            // Update engagement level
            double engagementLevel = calculateEngagementLevel(userId, lingerDuration);
            stateUpdate.put("engagement_level", engagementLevel);
            
            // Store state update in Redis
            String stateKey = LINGER_STATE_KEY + userId;
            redisTemplate.opsForValue().set(stateKey, stateUpdate, 3, TimeUnit.HOURS);
            
            logger.debug("State updated from linger for user {}: {}", userId, stateUpdate);
            return stateUpdate;
            
        } catch (Exception e) {
            logger.error("Error updating state from linger for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Track linger duration for analysis
     * @param userId User ID
     * @param postId Post ID
     * @param lingerDuration Duration in seconds
     */
    public void trackLingerDuration(UUID userId, UUID postId, long lingerDuration) {
        try {
            logger.debug("Tracking linger duration for user {} and post {}: {} seconds", userId, postId, lingerDuration);
            
            // Create linger record
            Map<String, Object> lingerRecord = new HashMap<>();
            lingerRecord.put("userId", userId.toString());
            lingerRecord.put("postId", postId.toString());
            lingerRecord.put("duration", lingerDuration);
            lingerRecord.put("timestamp", LocalDateTime.now());
            lingerRecord.put("impact", calculateLingerImpact(lingerDuration));
            
            // Store in Redis with TTL
            String durationKey = LINGER_DURATION_KEY + userId + ":" + postId;
            redisTemplate.opsForValue().set(durationKey, lingerRecord, LINGER_TRACKING_HOURS, TimeUnit.HOURS);
            
            // Update user's linger history
            updateUserLingerHistory(userId, lingerRecord);
            
            logger.debug("Linger duration tracked for user {} and post {}", userId, postId);
            
        } catch (Exception e) {
            logger.error("Error tracking linger duration for user {} and post {}: {}", userId, postId, e.getMessage(), e);
        }
    }
    
    /**
     * Get linger statistics for a user
     * @param userId User ID
     * @return Linger statistics
     */
    public Map<String, Object> getLingerStatistics(UUID userId) {
        try {
            logger.debug("Getting linger statistics for user {}", userId);
            
            // Try to get cached statistics first
            String statsKey = LINGER_STATISTICS_KEY + userId;
            Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
            
            if (cachedStats != null) {
                return cachedStats;
            }
            
            // Calculate fresh statistics
            Map<String, Object> stats = calculateLingerStatistics(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(statsKey, stats, 1, TimeUnit.HOURS);
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting linger statistics for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get linger patterns for a user
     * @param userId User ID
     * @return Linger patterns
     */
    public Map<String, Object> getLingerPatterns(UUID userId) {
        try {
            logger.debug("Getting linger patterns for user {}", userId);
            
            // Try to get cached patterns first
            String patternKey = LINGER_PATTERN_KEY + userId;
            Map<String, Object> cachedPatterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternKey);
            
            if (cachedPatterns != null) {
                return cachedPatterns;
            }
            
            // Calculate fresh patterns
            Map<String, Object> patterns = calculateLingerPatterns(userId);
            
            // Cache the results
            redisTemplate.opsForValue().set(patternKey, patterns, 2, TimeUnit.HOURS);
            
            return patterns;
            
        } catch (Exception e) {
            logger.error("Error getting linger patterns for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private double normalizeLingerDuration(long lingerDuration) {
        // Normalize to [0, 1] range using sigmoid-like function
        double normalized = (double) lingerDuration / MAX_LINGER_SECONDS;
        return Math.max(0.0, Math.min(1.0, normalized));
    }
    
    private Map<String, Object> updateLingerStatistics(UUID userId, long lingerDuration) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get existing statistics
        String statsKey = LINGER_STATISTICS_KEY + userId;
        Map<String, Object> existingStats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
        
        if (existingStats != null) {
            stats.putAll(existingStats);
        }
        
        // Update statistics
        stats.put("last_linger_duration", lingerDuration);
        stats.put("last_linger_timestamp", LocalDateTime.now());
        
        // Update running averages
        updateRunningAverages(stats, lingerDuration);
        
        return stats;
    }
    
    private void updateRunningAverages(Map<String, Object> stats, long lingerDuration) {
        // Update average linger duration
        Double avgDuration = (Double) stats.get("avg_linger_duration");
        Integer count = (Integer) stats.get("linger_count");
        
        if (avgDuration == null) {
            avgDuration = 0.0;
            count = 0;
        }
        
        count++;
        avgDuration = (avgDuration * (count - 1) + lingerDuration) / count;
        
        stats.put("avg_linger_duration", avgDuration);
        stats.put("linger_count", count);
        
        // Update max linger duration
        Long maxDuration = (Long) stats.get("max_linger_duration");
        if (maxDuration == null || lingerDuration > maxDuration) {
            stats.put("max_linger_duration", lingerDuration);
        }
        
        // Update min linger duration
        Long minDuration = (Long) stats.get("min_linger_duration");
        if (minDuration == null || lingerDuration < minDuration) {
            stats.put("min_linger_duration", lingerDuration);
        }
    }
    
    private Map<String, Object> updateLingerPatterns(UUID userId, UUID postId, long lingerDuration) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Get post information for pattern analysis
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            patterns.put("last_lingered_emotion", post.getEmotion());
            patterns.put("last_lingered_post_type", post.getEmotion()); // Using emotion as post type proxy
        }
        
        // Update temporal patterns
        patterns.put("last_linger_timestamp", LocalDateTime.now());
        patterns.put("last_linger_duration", lingerDuration);
        
        return patterns;
    }
    
    private Map<String, Object> updateAttentionState(UUID userId, long lingerDuration) {
        Map<String, Object> attentionState = new HashMap<>();
        
        // Calculate attention level based on linger duration
        double attentionLevel = calculateAttentionLevel(lingerDuration);
        attentionState.put("attention_level", attentionLevel);
        
        // Calculate attention consistency
        double attentionConsistency = calculateAttentionConsistency(userId);
        attentionState.put("attention_consistency", attentionConsistency);
        
        return attentionState;
    }
    
    private double calculateAttentionLevel(long lingerDuration) {
        // Calculate attention level based on linger duration
        double normalizedDuration = normalizeLingerDuration(lingerDuration);
        
        // Use sigmoid function for attention level
        double attentionLevel = 1.0 / (1.0 + Math.exp(-10 * (normalizedDuration - 0.5)));
        
        return attentionLevel;
    }
    
    private double calculateAttentionConsistency(UUID userId) {
        try {
            // Get recent linger records
            String patternKey = LINGER_PATTERN_KEY + userId;
            Map<String, Object> patterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternKey);
            
            if (patterns == null) {
                return 0.5; // Default consistency
            }
            
            // Calculate consistency based on recent linger patterns
            // This is a simplified implementation
            return 0.7; // Placeholder for consistency calculation
            
        } catch (Exception e) {
            logger.warn("Error calculating attention consistency: {}", e.getMessage());
            return 0.5;
        }
    }
    
    private double calculateEngagementLevel(UUID userId, long lingerDuration) {
        // Calculate engagement level based on linger duration and user history
        double baseEngagement = calculateAttentionLevel(lingerDuration);
        
        // Factor in user's historical engagement patterns
        Map<String, Object> stats = getLingerStatistics(userId);
        Double avgDuration = (Double) stats.get("avg_linger_duration");
        
        if (avgDuration != null && avgDuration > 0) {
            double durationRatio = (double) lingerDuration / avgDuration;
            baseEngagement *= Math.min(2.0, durationRatio); // Cap at 2x
        }
        
        return Math.max(0.0, Math.min(1.0, baseEngagement));
    }
    
    private void updateUserLingerHistory(UUID userId, Map<String, Object> lingerRecord) {
        try {
            // Get existing history
            String historyKey = LINGER_PATTERN_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            if (history == null) {
                history = new HashMap<>();
            }
            
            // Add new record to history
            List<Map<String, Object>> lingerHistory = (List<Map<String, Object>>) history.get("linger_history");
            if (lingerHistory == null) {
                lingerHistory = new ArrayList<>();
            }
            
            lingerHistory.add(lingerRecord);
            
            // Keep only recent records (last 50)
            if (lingerHistory.size() > 50) {
                lingerHistory = lingerHistory.subList(lingerHistory.size() - 50, lingerHistory.size());
            }
            
            history.put("linger_history", lingerHistory);
            history.put("last_updated", LocalDateTime.now());
            
            // Store updated history
            redisTemplate.opsForValue().set(historyKey, history, 6, TimeUnit.HOURS);
            
        } catch (Exception e) {
            logger.warn("Error updating user linger history: {}", e.getMessage());
        }
    }
    
    private Map<String, Object> calculateLingerStatistics(UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get user's linger history
        String historyKey = LINGER_PATTERN_KEY + userId;
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            // Return default statistics
            stats.put("total_linger_sessions", 0);
            stats.put("avg_linger_duration", 0.0);
            stats.put("max_linger_duration", 0L);
            stats.put("min_linger_duration", 0L);
            stats.put("total_linger_time", 0L);
            return stats;
        }
        
        List<Map<String, Object>> lingerHistory = (List<Map<String, Object>>) history.get("linger_history");
        if (lingerHistory == null || lingerHistory.isEmpty()) {
            stats.put("total_linger_sessions", 0);
            stats.put("avg_linger_duration", 0.0);
            stats.put("max_linger_duration", 0L);
            stats.put("min_linger_duration", 0L);
            stats.put("total_linger_time", 0L);
            return stats;
        }
        
        // Calculate statistics
        int totalSessions = lingerHistory.size();
        long totalTime = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .sum();
        
        double avgDuration = totalTime / (double) totalSessions;
        
        long maxDuration = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .max()
            .orElse(0L);
        
        long minDuration = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .min()
            .orElse(0L);
        
        stats.put("total_linger_sessions", totalSessions);
        stats.put("avg_linger_duration", avgDuration);
        stats.put("max_linger_duration", maxDuration);
        stats.put("min_linger_duration", minDuration);
        stats.put("total_linger_time", totalTime);
        
        return stats;
    }
    
    private Map<String, Object> calculateLingerPatterns(UUID userId) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Get user's linger history
        String historyKey = LINGER_PATTERN_KEY + userId;
        Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
        
        if (history == null) {
            return patterns;
        }
        
        List<Map<String, Object>> lingerHistory = (List<Map<String, Object>>) history.get("linger_history");
        if (lingerHistory == null || lingerHistory.isEmpty()) {
            return patterns;
        }
        
        // Analyze temporal patterns
        Map<String, Object> temporalPatterns = analyzeTemporalLingerPatterns(lingerHistory);
        patterns.put("temporal", temporalPatterns);
        
        // Analyze duration patterns
        Map<String, Object> durationPatterns = analyzeDurationPatterns(lingerHistory);
        patterns.put("duration", durationPatterns);
        
        // Analyze engagement patterns
        Map<String, Object> engagementPatterns = analyzeEngagementPatterns(lingerHistory);
        patterns.put("engagement", engagementPatterns);
        
        return patterns;
    }
    
    private Map<String, Object> analyzeTemporalLingerPatterns(List<Map<String, Object>> lingerHistory) {
        Map<String, Object> temporalPatterns = new HashMap<>();
        
        // Analyze hour-of-day patterns
        Map<Integer, Long> hourDistribution = new HashMap<>();
        for (Map<String, Object> record : lingerHistory) {
            LocalDateTime timestamp = (LocalDateTime) record.get("timestamp");
            int hour = timestamp.getHour();
            hourDistribution.merge(hour, 1L, Long::sum);
        }
        
        temporalPatterns.put("hour_distribution", hourDistribution);
        
        // Find peak linger hours
        Optional<Map.Entry<Integer, Long>> peakHour = hourDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (peakHour.isPresent()) {
            temporalPatterns.put("peak_hour", peakHour.get().getKey());
        }
        
        return temporalPatterns;
    }
    
    private Map<String, Object> analyzeDurationPatterns(List<Map<String, Object>> lingerHistory) {
        Map<String, Object> durationPatterns = new HashMap<>();
        
        // Categorize linger durations
        long shortLinger = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .filter(duration -> duration < 30)
            .count();
        
        long mediumLinger = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .filter(duration -> duration >= 30 && duration < 300)
            .count();
        
        long longLinger = lingerHistory.stream()
            .mapToLong(record -> ((Number) record.get("duration")).longValue())
            .filter(duration -> duration >= 300)
            .count();
        
        durationPatterns.put("short_linger_count", shortLinger);
        durationPatterns.put("medium_linger_count", mediumLinger);
        durationPatterns.put("long_linger_count", longLinger);
        
        return durationPatterns;
    }
    
    private Map<String, Object> analyzeEngagementPatterns(List<Map<String, Object>> lingerHistory) {
        Map<String, Object> engagementPatterns = new HashMap<>();
        
        // Calculate engagement trends
        double avgImpact = lingerHistory.stream()
            .mapToDouble(record -> ((Number) record.get("impact")).doubleValue())
            .average()
            .orElse(0.0);
        
        engagementPatterns.put("avg_engagement_impact", avgImpact);
        
        // Calculate engagement consistency
        double impactVariance = lingerHistory.stream()
            .mapToDouble(record -> {
                double impact = ((Number) record.get("impact")).doubleValue();
                return Math.pow(impact - avgImpact, 2);
            })
            .average()
            .orElse(0.0);
        
        double impactStdDev = Math.sqrt(impactVariance);
        double consistency = avgImpact > 0 ? 1.0 - (impactStdDev / avgImpact) : 0.0;
        
        engagementPatterns.put("engagement_consistency", Math.max(0.0, consistency));
        
        return engagementPatterns;
    }
}
