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
 * Service for processing resonance-based state modulation
 * Implements sophisticated resonance algorithms with Ws = 0.2 weight
 */
@Service
@Transactional
public class ResonanceProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ResonanceProcessor.class);
    
    // Resonance algorithm constants
    private static final double RESONANCE_WEIGHT = 0.2; // Ws = 0.2
    private static final double RESONANCE_DECAY_FACTOR = 0.95;
    private static final long RESONANCE_TRACKING_HOURS = 24;
    private static final long PATTERN_ANALYSIS_DAYS = 7;
    
    // Redis keys
    private static final String RESONANCE_STATE_KEY = "resonance_state:";
    private static final String RESONANCE_PATTERN_KEY = "resonance_pattern:";
    private static final String RESONANCE_IMPACT_KEY = "resonance_impact:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Calculate resonance impact on user state
     * @param userId User ID
     * @param postId Post ID
     * @return Resonance impact value
     */
    public double calculateResonanceImpact(UUID userId, UUID postId) {
        try {
            logger.debug("Calculating resonance impact for user {} and post {}", userId, postId);
            
            // Get post information
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                logger.warn("Post not found for resonance impact calculation: {}", postId);
                return 0.0;
            }
            
            Post post = postOpt.get();
            
            // Calculate base resonance impact
            double baseImpact = calculateBaseResonanceImpact(post);
            
            // Apply user-specific resonance patterns
            double userPatternFactor = getUserResonancePatternFactor(userId);
            
            // Apply temporal decay
            double temporalFactor = calculateTemporalDecayFactor(post);
            
            // Calculate final impact
            double impact = baseImpact * userPatternFactor * temporalFactor * RESONANCE_WEIGHT;
            
            // Cache the result
            String cacheKey = RESONANCE_IMPACT_KEY + userId + ":" + postId;
            redisTemplate.opsForValue().set(cacheKey, impact, 1, TimeUnit.HOURS);
            
            logger.debug("Resonance impact calculated: {} for user {} and post {}", impact, userId, postId);
            return impact;
            
        } catch (Exception e) {
            logger.error("Error calculating resonance impact for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Update user state based on resonance interaction
     * @param userId User ID
     * @param postId Post ID
     * @return State update map
     */
    public Map<String, Object> updateStateFromResonance(UUID userId, UUID postId) {
        try {
            logger.debug("Updating state from resonance for user {} and post {}", userId, postId);
            
            Map<String, Object> stateUpdate = new HashMap<>();
            
            // Calculate resonance impact
            double resonanceImpact = calculateResonanceImpact(userId, postId);
            
            // Update resonance count
            long resonanceCount = postResonanceRepository.countByUserId(userId);
            stateUpdate.put("resonance_count", resonanceCount);
            
            // Update resonance intensity
            double resonanceIntensity = calculateResonanceIntensity(userId);
            stateUpdate.put("resonance_intensity", resonanceIntensity);
            
            // Update resonance patterns
            Map<String, Object> patterns = updateResonancePatterns(userId, postId);
            stateUpdate.put("resonance_patterns", patterns);
            
            // Update emotional resonance state
            Map<String, Object> emotionalState = updateEmotionalResonanceState(userId, postId);
            stateUpdate.put("emotional_resonance", emotionalState);
            
            // Store state update in Redis
            String stateKey = RESONANCE_STATE_KEY + userId;
            redisTemplate.opsForValue().set(stateKey, stateUpdate, 2, TimeUnit.HOURS);
            
            logger.debug("State updated from resonance for user {}: {}", userId, stateUpdate);
            return stateUpdate;
            
        } catch (Exception e) {
            logger.error("Error updating state from resonance for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Track and analyze resonance patterns for a user
     * @param userId User ID
     * @return Resonance patterns map
     */
    public Map<String, Object> trackResonancePatterns(UUID userId) {
        try {
            logger.debug("Tracking resonance patterns for user {}", userId);
            
            // Get recent resonances
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(PATTERN_ANALYSIS_DAYS);
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, cutoffTime);
            
            Map<String, Object> patterns = new HashMap<>();
            
            // Analyze temporal patterns
            Map<String, Object> temporalPatterns = analyzeTemporalPatterns(recentResonances);
            patterns.put("temporal", temporalPatterns);
            
            // Analyze content patterns
            Map<String, Object> contentPatterns = analyzeContentPatterns(userId, recentResonances);
            patterns.put("content", contentPatterns);
            
            // Analyze emotional patterns
            Map<String, Object> emotionalPatterns = analyzeEmotionalPatterns(userId, recentResonances);
            patterns.put("emotional", emotionalPatterns);
            
            // Calculate pattern consistency
            double consistency = calculatePatternConsistency(recentResonances);
            patterns.put("consistency", consistency);
            
            // Calculate resonance frequency
            double frequency = calculateResonanceFrequency(recentResonances);
            patterns.put("frequency", frequency);
            
            // Store patterns in Redis
            String patternKey = RESONANCE_PATTERN_KEY + userId;
            redisTemplate.opsForValue().set(patternKey, patterns, 6, TimeUnit.HOURS);
            
            logger.debug("Resonance patterns tracked for user {}: {}", userId, patterns);
            return patterns;
            
        } catch (Exception e) {
            logger.error("Error tracking resonance patterns for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get resonance statistics for a user
     * @param userId User ID
     * @return Resonance statistics
     */
    public Map<String, Object> getResonanceStatistics(UUID userId) {
        try {
            logger.debug("Getting resonance statistics for user {}", userId);
            
            Map<String, Object> stats = new HashMap<>();
            
            // Total resonance count
            long totalResonances = postResonanceRepository.countByUserId(userId);
            stats.put("total_resonances", totalResonances);
            
            // Recent resonance count (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long recentResonances = postResonanceRepository.countByUserIdAndResonancedAtAfter(userId, weekAgo);
            stats.put("recent_resonances", recentResonances);
            
            // Average resonances per day
            double avgPerDay = recentResonances / 7.0;
            stats.put("avg_resonances_per_day", avgPerDay);
            
            // Resonance intensity
            double intensity = calculateResonanceIntensity(userId);
            stats.put("resonance_intensity", intensity);
            
            // Pattern consistency
            Map<String, Object> patterns = trackResonancePatterns(userId);
            stats.put("pattern_consistency", patterns.get("consistency"));
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting resonance statistics for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private double calculateBaseResonanceImpact(Post post) {
        // Base impact calculation based on post characteristics
        double baseImpact = 1.0;
        
        // Factor in post engagement metrics
        if (post.getResonance() > 0) {
            baseImpact += Math.log(post.getResonance() + 1) * 0.1;
        }
        
        if (post.getView() > 0) {
            baseImpact += Math.log(post.getView() + 1) * 0.05;
        }
        
        // Factor in post age (newer posts have higher impact)
        long hoursSinceCreation = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        double ageFactor = Math.max(0.1, 1.0 - (hoursSinceCreation / 168.0)); // Decay over a week
        baseImpact *= ageFactor;
        
        return baseImpact;
    }
    
    private double getUserResonancePatternFactor(UUID userId) {
        try {
            // Get cached resonance patterns
            String patternKey = RESONANCE_PATTERN_KEY + userId;
            Map<String, Object> patterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternKey);
            
            if (patterns != null) {
                Double consistency = (Double) patterns.get("consistency");
                if (consistency != null) {
                    return Math.max(0.5, consistency); // Factor between 0.5 and 1.0
                }
            }
            
            return 1.0; // Default factor
            
        } catch (Exception e) {
            logger.warn("Error getting user resonance pattern factor: {}", e.getMessage());
            return 1.0;
        }
    }
    
    private double calculateTemporalDecayFactor(Post post) {
        // Calculate temporal decay based on post age
        long hoursSinceCreation = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
        
        if (hoursSinceCreation <= 24) {
            return 1.0; // Full impact for posts less than 24 hours old
        } else if (hoursSinceCreation <= 168) { // 1 week
            return Math.pow(RESONANCE_DECAY_FACTOR, hoursSinceCreation / 24.0);
        } else {
            return 0.1; // Minimal impact for posts older than a week
        }
    }
    
    private double calculateResonanceIntensity(UUID userId) {
        try {
            // Get recent resonances
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(RESONANCE_TRACKING_HOURS);
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, cutoffTime);
            
            if (recentResonances.isEmpty()) {
                return 0.0;
            }
            
            // Calculate intensity based on frequency and recency
            double frequency = recentResonances.size() / (double) RESONANCE_TRACKING_HOURS;
            
            // Apply recency weighting
            double recencyWeight = 0.0;
            for (PostResonance resonance : recentResonances) {
                long hoursAgo = ChronoUnit.HOURS.between(resonance.getResonancedAt(), LocalDateTime.now());
                recencyWeight += Math.exp(-hoursAgo / 12.0); // Exponential decay
            }
            
            return frequency * recencyWeight;
            
        } catch (Exception e) {
            logger.error("Error calculating resonance intensity: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    private Map<String, Object> updateResonancePatterns(UUID userId, UUID postId) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Get post emotion for emotional pattern tracking
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            patterns.put("last_resonated_emotion", post.getEmotion());
            patterns.put("last_resonated_timestamp", LocalDateTime.now());
        }
        
        return patterns;
    }
    
    private Map<String, Object> updateEmotionalResonanceState(UUID userId, UUID postId) {
        Map<String, Object> emotionalState = new HashMap<>();
        
        // Get post emotion
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            emotionalState.put("resonated_emotion", post.getEmotion());
            emotionalState.put("emotional_resonance_strength", RESONANCE_WEIGHT);
        }
        
        return emotionalState;
    }
    
    private Map<String, Object> analyzeTemporalPatterns(List<PostResonance> resonances) {
        Map<String, Object> temporalPatterns = new HashMap<>();
        
        if (resonances.isEmpty()) {
            return temporalPatterns;
        }
        
        // Analyze hour-of-day patterns
        Map<Integer, Long> hourDistribution = resonances.stream()
            .collect(Collectors.groupingBy(
                r -> r.getResonancedAt().getHour(),
                Collectors.counting()
            ));
        
        temporalPatterns.put("hour_distribution", hourDistribution);
        
        // Find peak resonance hours
        Optional<Map.Entry<Integer, Long>> peakHour = hourDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (peakHour.isPresent()) {
            temporalPatterns.put("peak_hour", peakHour.get().getKey());
        }
        
        return temporalPatterns;
    }
    
    private Map<String, Object> analyzeContentPatterns(UUID userId, List<PostResonance> resonances) {
        Map<String, Object> contentPatterns = new HashMap<>();
        
        if (resonances.isEmpty()) {
            return contentPatterns;
        }
        
        // Analyze emotion patterns
        Map<String, Long> emotionDistribution = new HashMap<>();
        for (PostResonance resonance : resonances) {
            Optional<Post> postOpt = postRepository.findById(resonance.getPostId());
            if (postOpt.isPresent()) {
                String emotion = postOpt.get().getEmotion();
                emotionDistribution.merge(emotion, 1L, Long::sum);
            }
        }
        
        contentPatterns.put("emotion_distribution", emotionDistribution);
        
        // Find preferred emotion
        Optional<Map.Entry<String, Long>> preferredEmotion = emotionDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        if (preferredEmotion.isPresent()) {
            contentPatterns.put("preferred_emotion", preferredEmotion.get().getKey());
        }
        
        return contentPatterns;
    }
    
    private Map<String, Object> analyzeEmotionalPatterns(UUID userId, List<PostResonance> resonances) {
        Map<String, Object> emotionalPatterns = new HashMap<>();
        
        if (resonances.isEmpty()) {
            return emotionalPatterns;
        }
        
        // Calculate emotional diversity
        Set<String> emotions = resonances.stream()
            .map(r -> {
                Optional<Post> postOpt = postRepository.findById(r.getPostId());
                return postOpt.map(Post::getEmotion).orElse("unknown");
            })
            .collect(Collectors.toSet());
        
        emotionalPatterns.put("emotional_diversity", emotions.size());
        emotionalPatterns.put("unique_emotions", emotions);
        
        return emotionalPatterns;
    }
    
    private double calculatePatternConsistency(List<PostResonance> resonances) {
        if (resonances.size() < 2) {
            return 1.0;
        }
        
        // Calculate consistency based on temporal spacing
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < resonances.size(); i++) {
            long interval = ChronoUnit.HOURS.between(
                resonances.get(i-1).getResonancedAt(),
                resonances.get(i).getResonancedAt()
            );
            intervals.add(interval);
        }
        
        // Calculate coefficient of variation
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .average().orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = mean > 0 ? stdDev / mean : 0.0;
        
        // Convert to consistency score (lower CV = higher consistency)
        return Math.max(0.0, 1.0 - coefficientOfVariation);
    }
    
    private double calculateResonanceFrequency(List<PostResonance> resonances) {
        if (resonances.isEmpty()) {
            return 0.0;
        }
        
        // Calculate average resonances per day
        LocalDateTime oldest = resonances.stream()
            .map(PostResonance::getResonancedAt)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        long days = ChronoUnit.DAYS.between(oldest, LocalDateTime.now());
        return days > 0 ? resonances.size() / (double) days : 0.0;
    }
}
