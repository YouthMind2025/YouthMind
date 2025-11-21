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
 * Service for calculating subconscious personality (V_shadow - Top-K resonance)
 * Implements sophisticated subconscious personality computation algorithms
 */
@Service
@Transactional
public class SubconsciousPersonalityCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(SubconsciousPersonalityCalculator.class);
    
    // Subconscious personality algorithm constants
    private static final double RESONANCE_INTENSITY_FACTOR = 1.5; // Factor for resonance intensity
    private static final double TEMPORAL_DECAY_FACTOR = 0.95; // Temporal decay factor
    private static final double PATTERN_CONSISTENCY_FACTOR = 1.2; // Pattern consistency factor
    private static final int PERSONALITY_VECTOR_DIMENSIONS = 5; // Dimensions of personality vector
    
    // Redis keys
    private static final String RESONANCE_PATTERNS_KEY = "resonance_patterns:";
    private static final String SUBCONSCIOUS_VECTOR_KEY = "subconscious_vector:";
    private static final String TOP_K_RESONANCE_KEY = "top_k_resonance:";
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Find Top-K resonance patterns for subconscious personality calculation
     * @param userId User ID
     * @param resonances List of all user resonances
     * @param k Number of top resonances to consider
     * @return Top-K resonance patterns
     */
    public Map<String, Object> findTopKResonance(UUID userId, List<PostResonance> resonances, int k) {
        try {
            logger.debug("Finding Top-{} resonance patterns for user {} with {} resonances", k, userId, resonances.size());
            
            if (resonances.isEmpty()) {
                return createDefaultTopKResonance(userId);
            }
            
            // Get posts for resonances
            List<UUID> postIds = resonances.stream().map(PostResonance::getPostId).collect(Collectors.toList());
            List<Post> posts = postRepository.findAllById(postIds);
            
            if (posts.isEmpty()) {
                return createDefaultTopKResonance(userId);
            }
            
            // Calculate resonance scores
            Map<UUID, Double> resonanceScores = calculateResonanceScores(resonances, posts);
            
            // Get top-K resonances
            List<PostResonance> topKResonances = getTopKResonances(resonances, resonanceScores, k);
            
            // Analyze patterns in top-K resonances
            Map<String, Object> patterns = analyzeTopKPatterns(topKResonances, posts);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("topKResonances", topKResonances.stream().map(PostResonance::getPostId).collect(Collectors.toList()));
            result.put("resonanceScores", resonanceScores);
            result.put("patterns", patterns);
            result.put("k", k);
            result.put("totalResonances", resonances.size());
            result.put("timestamp", LocalDateTime.now());
            
            // Cache the result
            String cacheKey = TOP_K_RESONANCE_KEY + userId;
            redisTemplate.opsForValue().set(cacheKey, result, 4, TimeUnit.HOURS);
            
            logger.debug("Top-{} resonance patterns found for user {}: {}", k, userId, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error finding Top-{} resonance patterns for user {}: {}", k, userId, e.getMessage(), e);
            return createDefaultTopKResonance(userId);
        }
    }
    
    /**
     * Calculate subconscious personality vector (V_shadow)
     * @param userId User ID
     * @param topKResonance Top-K resonance patterns
     * @return Subconscious personality vector
     */
    public Map<String, Object> calculateSubconsciousVector(UUID userId, Map<String, Object> topKResonance) {
        try {
            logger.debug("Calculating subconscious vector for user {}", userId);
            
            // Get top-K resonance data
            List<UUID> topKPostIds = (List<UUID>) topKResonance.get("topKResonances");
            Map<String, Object> patterns = (Map<String, Object>) topKResonance.get("patterns");
            
            if (topKPostIds == null || topKPostIds.isEmpty()) {
                return createDefaultSubconsciousVector(userId);
            }
            
            // Get posts for top-K resonances
            List<Post> topKPosts = postRepository.findAllById(topKPostIds);
            
            if (topKPosts.isEmpty()) {
                return createDefaultSubconsciousVector(userId);
            }
            
            // Calculate subconscious vector components
            List<Double> subconsciousVector = calculateSubconsciousComponents(topKPosts, patterns);
            
            // Calculate confidence based on pattern consistency
            double confidence = calculateSubconsciousConfidence(patterns, topKPosts);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("personalityVector", subconsciousVector);
            result.put("confidence", confidence);
            result.put("computationMethod", "TOP_K_RESONANCE");
            result.put("topKData", topKResonance);
            result.put("timestamp", LocalDateTime.now());
            
            // Cache the result
            String cacheKey = SUBCONSCIOUS_VECTOR_KEY + userId;
            redisTemplate.opsForValue().set(cacheKey, result, 4, TimeUnit.HOURS);
            
            logger.debug("Subconscious vector calculated for user {}: {}", userId, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error calculating subconscious vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultSubconsciousVector(userId);
        }
    }
    
    /**
     * Track resonance patterns for subconscious personality
     * @param userId User ID
     * @param subconsciousVector Current subconscious vector
     */
    public void trackResonancePatterns(UUID userId, Map<String, Object> subconsciousVector) {
        try {
            logger.debug("Tracking resonance patterns for user {}", userId);
            
            String patternsKey = RESONANCE_PATTERNS_KEY + userId;
            
            // Get existing patterns
            Map<String, Object> patterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternsKey);
            if (patterns == null) {
                patterns = new HashMap<>();
            }
            
            // Update pattern history
            List<Map<String, Object>> patternHistory = (List<Map<String, Object>>) patterns.get("patternHistory");
            if (patternHistory == null) {
                patternHistory = new ArrayList<>();
            }
            
            Map<String, Object> patternEntry = new HashMap<>();
            patternEntry.put("timestamp", LocalDateTime.now());
            patternEntry.put("subconsciousVector", subconsciousVector.get("personalityVector"));
            patternEntry.put("confidence", subconsciousVector.get("confidence"));
            
            patternHistory.add(patternEntry);
            
            // Keep only recent patterns (last 50 entries)
            if (patternHistory.size() > 50) {
                patternHistory = patternHistory.subList(patternHistory.size() - 50, patternHistory.size());
            }
            
            // Calculate pattern evolution
            Map<String, Object> patternEvolution = calculatePatternEvolution(patternHistory);
            
            patterns.put("patternHistory", patternHistory);
            patterns.put("patternEvolution", patternEvolution);
            patterns.put("lastUpdated", LocalDateTime.now());
            
            // Store updated patterns
            redisTemplate.opsForValue().set(patternsKey, patterns, 7, TimeUnit.DAYS);
            
            logger.debug("Resonance patterns tracked for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error tracking resonance patterns for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get resonance patterns for a user
     * @param userId User ID
     * @return Resonance patterns
     */
    public Map<String, Object> getResonancePatterns(UUID userId) {
        try {
            logger.debug("Getting resonance patterns for user {}", userId);
            
            String patternsKey = RESONANCE_PATTERNS_KEY + userId;
            Map<String, Object> patterns = (Map<String, Object>) redisTemplate.opsForValue().get(patternsKey);
            
            return patterns != null ? patterns : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting resonance patterns for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<UUID, Double> calculateResonanceScores(List<PostResonance> resonances, List<Post> posts) {
        Map<UUID, Double> scores = new HashMap<>();
        Map<UUID, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        
        for (PostResonance resonance : resonances) {
            Post post = postMap.get(resonance.getPostId());
            if (post == null) continue;
            
            // Calculate resonance score based on multiple factors
            double temporalScore = calculateTemporalScore(resonance.getResonancedAt());
            double emotionalScore = calculateEmotionalScore(post);
            double engagementScore = calculateEngagementScore(post);
            double consistencyScore = calculateConsistencyScore(resonance, resonances);
            
            // Combined score
            double totalScore = temporalScore * TEMPORAL_DECAY_FACTOR + 
                              emotionalScore * RESONANCE_INTENSITY_FACTOR + 
                              engagementScore + 
                              consistencyScore * PATTERN_CONSISTENCY_FACTOR;
            
            scores.put(resonance.getPostId(), totalScore);
        }
        
        return scores;
    }
    
    private double calculateTemporalScore(LocalDateTime resonancedAt) {
        long hoursAgo = ChronoUnit.HOURS.between(resonancedAt, LocalDateTime.now());
        
        // Temporal scoring with decay
        if (hoursAgo <= 24) {
            return 1.0; // Full score for recent resonances
        } else if (hoursAgo <= 168) { // 1 week
            return Math.pow(TEMPORAL_DECAY_FACTOR, hoursAgo / 24.0);
        } else if (hoursAgo <= 720) { // 1 month
            return Math.pow(TEMPORAL_DECAY_FACTOR, hoursAgo / 24.0) * 0.5;
        } else {
            return 0.1; // Minimal score for very old resonances
        }
    }
    
    private double calculateEmotionalScore(Post post) {
        String emotion = post.getEmotion();
        if (emotion == null) {
            return 0.5; // Neutral score
        }
        
        // Different emotions have different subconscious impact scores
        Map<String, Double> emotionalScores = Map.of(
            "happy", 0.8,
            "sad", 1.2,
            "angry", 1.0,
            "anxious", 1.3,
            "excited", 0.7,
            "calm", 0.4,
            "confused", 1.1,
            "grateful", 0.9
        );
        
        return emotionalScores.getOrDefault(emotion.toLowerCase(), 0.5);
    }
    
    private double calculateEngagementScore(Post post) {
        // Score based on post engagement metrics
        double engagement = 0.0;
        
        if (post.getResonance() > 0) {
            engagement += Math.log(post.getResonance() + 1) * 0.4;
        }
        
        if (post.getView() > 0) {
            engagement += Math.log(post.getView() + 1) * 0.2;
        }
        
        if (post.getCollect() > 0) {
            engagement += Math.log(post.getCollect() + 1) * 0.6;
        }
        
        // Normalize to [0, 1] range
        return Math.min(1.0, engagement / 3.0);
    }
    
    private double calculateConsistencyScore(PostResonance resonance, List<PostResonance> allResonances) {
        // Score based on consistency with user's resonance patterns
        String emotion = getPostEmotion(resonance.getPostId());
        if (emotion == null) {
            return 0.5; // Neutral consistency
        }
        
        // Count how many times user resonated with similar emotional content
        long similarResonances = allResonances.stream()
            .mapToLong(r -> {
                String rEmotion = getPostEmotion(r.getPostId());
                return emotion.equals(rEmotion) ? 1 : 0;
            })
            .sum();
        
        // Higher consistency score for emotions user resonates with frequently
        double consistency = Math.min(1.0, similarResonances / 5.0);
        return consistency;
    }
    
    private String getPostEmotion(UUID postId) {
        try {
            Optional<Post> postOpt = postRepository.findById(postId);
            return postOpt.map(Post::getEmotion).orElse(null);
        } catch (Exception e) {
            logger.warn("Error getting post emotion for post {}: {}", postId, e.getMessage());
            return null;
        }
    }
    
    private List<PostResonance> getTopKResonances(List<PostResonance> resonances, Map<UUID, Double> scores, int k) {
        return resonances.stream()
            .sorted((r1, r2) -> {
                Double score1 = scores.getOrDefault(r1.getPostId(), 0.0);
                Double score2 = scores.getOrDefault(r2.getPostId(), 0.0);
                return Double.compare(score2, score1); // Descending order
            })
            .limit(k)
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> analyzeTopKPatterns(List<PostResonance> topKResonances, List<Post> posts) {
        Map<String, Object> patterns = new HashMap<>();
        
        if (topKResonances.isEmpty()) {
            return patterns;
        }
        
        Map<UUID, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        
        // Analyze emotional patterns
        Map<String, Long> emotionDistribution = new HashMap<>();
        for (PostResonance resonance : topKResonances) {
            Post post = postMap.get(resonance.getPostId());
            if (post != null && post.getEmotion() != null) {
                emotionDistribution.merge(post.getEmotion(), 1L, Long::sum);
            }
        }
        patterns.put("emotionDistribution", emotionDistribution);
        
        // Analyze temporal patterns
        Map<String, Long> hourDistribution = new HashMap<>();
        for (PostResonance resonance : topKResonances) {
            int hour = resonance.getResonancedAt().getHour();
            hourDistribution.merge(String.valueOf(hour), 1L, Long::sum);
        }
        patterns.put("hourDistribution", hourDistribution);
        
        // Analyze content patterns
        List<String> emotions = topKResonances.stream()
            .map(r -> {
                Post post = postMap.get(r.getPostId());
                return post != null ? post.getEmotion() : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        patterns.put("dominantEmotions", emotions);
        patterns.put("emotionalDiversity", emotions.stream().distinct().count());
        
        // Calculate pattern consistency
        double consistency = calculatePatternConsistency(emotions);
        patterns.put("consistency", consistency);
        
        return patterns;
    }
    
    private double calculatePatternConsistency(List<String> emotions) {
        if (emotions.isEmpty()) {
            return 0.0;
        }
        
        // Calculate consistency based on emotion distribution
        Map<String, Long> emotionCounts = emotions.stream()
            .collect(Collectors.groupingBy(emotion -> emotion, Collectors.counting()));
        
        long totalEmotions = emotions.size();
        long maxCount = emotionCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        
        // Higher consistency when one emotion dominates
        return (double) maxCount / totalEmotions;
    }
    
    private List<Double> calculateSubconsciousComponents(List<Post> posts, Map<String, Object> patterns) {
        List<Double> vector = new ArrayList<>();
        
        // Initialize vector dimensions
        for (int i = 0; i < PERSONALITY_VECTOR_DIMENSIONS; i++) {
            vector.add(0.0);
        }
        
        // Dimension 1: Emotional depth
        double emotionalDepth = calculateEmotionalDepth(posts, patterns);
        vector.set(0, emotionalDepth);
        
        // Dimension 2: Resonance intensity
        double resonanceIntensity = calculateResonanceIntensity(posts);
        vector.set(1, resonanceIntensity);
        
        // Dimension 3: Pattern stability
        double patternStability = calculatePatternStability(patterns);
        vector.set(2, patternStability);
        
        // Dimension 4: Subconscious vulnerability
        double subconsciousVulnerability = calculateSubconsciousVulnerability(posts);
        vector.set(3, subconsciousVulnerability);
        
        // Dimension 5: Resonance consistency
        double resonanceConsistency = calculateResonanceConsistency(patterns);
        vector.set(4, resonanceConsistency);
        
        return vector;
    }
    
    private double calculateEmotionalDepth(List<Post> posts, Map<String, Object> patterns) {
        // Calculate emotional depth based on post content and patterns
        Map<String, Long> emotionDistribution = (Map<String, Long>) patterns.get("emotionDistribution");
        if (emotionDistribution == null) {
            return 0.5; // Default depth
        }
        
        // Higher depth for more complex emotional patterns
        long totalEmotions = emotionDistribution.values().stream().mapToLong(Long::longValue).sum();
        long uniqueEmotions = emotionDistribution.size();
        
        double diversity = (double) uniqueEmotions / totalEmotions;
        double depth = Math.min(1.0, diversity * 2.0);
        
        return depth;
    }
    
    private double calculateResonanceIntensity(List<Post> posts) {
        // Calculate resonance intensity based on post engagement
        double totalIntensity = 0.0;
        
        for (Post post : posts) {
            double postIntensity = 0.0;
            
            if (post.getResonance() > 0) {
                postIntensity += Math.log(post.getResonance() + 1) * 0.4;
            }
            
            if (post.getView() > 0) {
                postIntensity += Math.log(post.getView() + 1) * 0.2;
            }
            
            if (post.getCollect() > 0) {
                postIntensity += Math.log(post.getCollect() + 1) * 0.4;
            }
            
            totalIntensity += postIntensity;
        }
        
        // Normalize by number of posts
        double avgIntensity = posts.isEmpty() ? 0.0 : totalIntensity / posts.size();
        return Math.min(1.0, avgIntensity / 3.0);
    }
    
    private double calculatePatternStability(Map<String, Object> patterns) {
        // Calculate pattern stability based on consistency
        Double consistency = (Double) patterns.get("consistency");
        if (consistency == null) {
            return 0.5; // Default stability
        }
        
        // Higher stability with higher consistency
        return consistency;
    }
    
    private double calculateSubconsciousVulnerability(List<Post> posts) {
        // Calculate subconscious vulnerability based on emotional content
        Map<String, Long> emotionCounts = posts.stream()
            .filter(post -> post.getEmotion() != null)
            .collect(Collectors.groupingBy(Post::getEmotion, Collectors.counting()));
        
        if (emotionCounts.isEmpty()) {
            return 0.5; // Default vulnerability
        }
        
        // Higher vulnerability for posts with vulnerable emotions
        Map<String, Double> vulnerabilityScores = Map.of(
            "sad", 1.0,
            "anxious", 0.9,
            "confused", 0.8,
            "angry", 0.7,
            "happy", 0.2,
            "excited", 0.1,
            "calm", 0.3,
            "grateful", 0.4
        );
        
        long totalPosts = emotionCounts.values().stream().mapToLong(Long::longValue).sum();
        double vulnerability = 0.0;
        
        for (Map.Entry<String, Long> entry : emotionCounts.entrySet()) {
            String emotion = entry.getKey();
            long count = entry.getValue();
            double emotionVulnerability = vulnerabilityScores.getOrDefault(emotion.toLowerCase(), 0.5);
            vulnerability += emotionVulnerability * count;
        }
        
        return Math.min(1.0, vulnerability / totalPosts);
    }
    
    private double calculateResonanceConsistency(Map<String, Object> patterns) {
        // Calculate resonance consistency based on patterns
        Double consistency = (Double) patterns.get("consistency");
        if (consistency == null) {
            return 0.5; // Default consistency
        }
        
        return consistency;
    }
    
    private double calculateSubconsciousConfidence(Map<String, Object> patterns, List<Post> posts) {
        // Calculate confidence based on pattern quality and data availability
        double patternConfidence = calculatePatternConfidence(patterns);
        double dataConfidence = calculateDataConfidence(posts);
        
        // Combined confidence
        return (patternConfidence + dataConfidence) / 2.0;
    }
    
    private double calculatePatternConfidence(Map<String, Object> patterns) {
        // Confidence based on pattern consistency and diversity
        Double consistency = (Double) patterns.get("consistency");
        Long emotionalDiversity = (Long) patterns.get("emotionalDiversity");
        
        if (consistency == null || emotionalDiversity == null) {
            return 0.5; // Default confidence
        }
        
        // Higher confidence with good consistency and diversity
        double consistencyScore = consistency;
        double diversityScore = Math.min(1.0, emotionalDiversity / 5.0);
        
        return (consistencyScore + diversityScore) / 2.0;
    }
    
    private double calculateDataConfidence(List<Post> posts) {
        // Confidence based on amount and quality of data
        int postCount = posts.size();
        
        if (postCount == 0) {
            return 0.1; // Very low confidence with no data
        } else if (postCount < 5) {
            return 0.3; // Low confidence with little data
        } else if (postCount < 10) {
            return 0.6; // Medium confidence
        } else {
            return 0.9; // High confidence with sufficient data
        }
    }
    
    private Map<String, Object> calculatePatternEvolution(List<Map<String, Object>> patternHistory) {
        Map<String, Object> evolution = new HashMap<>();
        
        if (patternHistory.size() < 2) {
            evolution.put("evolutionDirection", "STABLE");
            evolution.put("evolutionStrength", 0.0);
            return evolution;
        }
        
        // Calculate evolution based on vector changes over time
        Map<String, Object> firstEntry = patternHistory.get(0);
        Map<String, Object> lastEntry = patternHistory.get(patternHistory.size() - 1);
        
        List<Double> firstVector = (List<Double>) firstEntry.get("subconsciousVector");
        List<Double> lastVector = (List<Double>) lastEntry.get("subconsciousVector");
        
        if (firstVector != null && lastVector != null && firstVector.size() == lastVector.size()) {
            double evolutionStrength = 0.0;
            for (int i = 0; i < firstVector.size(); i++) {
                evolutionStrength += Math.abs(lastVector.get(i) - firstVector.get(i));
            }
            
            evolution.put("evolutionStrength", evolutionStrength);
            
            if (evolutionStrength > 0.3) {
                evolution.put("evolutionDirection", "EVOLVING");
            } else if (evolutionStrength > 0.1) {
                evolution.put("evolutionDirection", "CHANGING");
            } else {
                evolution.put("evolutionDirection", "STABLE");
            }
        }
        
        return evolution;
    }
    
    private Map<String, Object> createDefaultTopKResonance(UUID userId) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("userId", userId.toString());
        defaultResult.put("topKResonances", new ArrayList<>());
        defaultResult.put("resonanceScores", new HashMap<>());
        defaultResult.put("patterns", new HashMap<>());
        defaultResult.put("k", 0);
        defaultResult.put("totalResonances", 0);
        defaultResult.put("timestamp", LocalDateTime.now());
        return defaultResult;
    }
    
    private Map<String, Object> createDefaultSubconsciousVector(UUID userId) {
        Map<String, Object> defaultVector = new HashMap<>();
        defaultVector.put("userId", userId.toString());
        defaultVector.put("personalityVector", Arrays.asList(0.3, 0.3, 0.3, 0.3, 0.3));
        defaultVector.put("confidence", 0.1);
        defaultVector.put("computationMethod", "DEFAULT");
        defaultVector.put("timestamp", LocalDateTime.now());
        return defaultVector;
    }
}
