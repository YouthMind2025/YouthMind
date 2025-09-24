package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced psychological state cache service integrating Youseef's caching approach
 * with our existing Redis infrastructure and 43-dimensional psychological vectors
 */
@Service
public class PsychologicalStateCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalStateCacheService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Cache keys
    private static final String PSYCHOLOGICAL_STATE_KEY = "psychological_state:";
    private static final String BUDDHIST_FACTORS_KEY = "buddhist_factors:";
    private static final String PERSONALITY_PROFILE_KEY = "personality_profile:";
    private static final String STATE_HISTORY_KEY = "state_history:";
    
    // TTL configurations
    private static final Duration STATE_TTL = Duration.ofMinutes(30);
    private static final Duration FACTORS_TTL = Duration.ofHours(2);
    private static final Duration PROFILE_TTL = Duration.ofHours(1);
    private static final Duration HISTORY_TTL = Duration.ofDays(7);
    
    /**
     * Cache psychological state vector with TTL
     */
    public void cachePsychologicalState(UUID userId, List<Double> psychologicalVector, Map<String, Double> buddhistFactors) {
        try {
            String stateKey = PSYCHOLOGICAL_STATE_KEY + userId;
            String factorsKey = BUDDHIST_FACTORS_KEY + userId;
            
            // Cache psychological state vector
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("vector", psychologicalVector);
            stateData.put("buddhistFactors", buddhistFactors);
            stateData.put("timestamp", LocalDateTime.now());
            stateData.put("vectorDimensions", psychologicalVector.size());
            
            redisTemplate.opsForValue().set(stateKey, stateData, STATE_TTL.toMinutes(), TimeUnit.MINUTES);
            
            // Cache Buddhist factors separately for quick access
            redisTemplate.opsForValue().set(factorsKey, buddhistFactors, FACTORS_TTL.toMinutes(), TimeUnit.MINUTES);
            
            // Add to state history
            addToStateHistory(userId, psychologicalVector, buddhistFactors);
            
            logger.debug("Cached psychological state for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error caching psychological state for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get cached psychological state
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPsychologicalState(UUID userId) {
        try {
            String stateKey = PSYCHOLOGICAL_STATE_KEY + userId;
            Object cachedState = redisTemplate.opsForValue().get(stateKey);
            
            if (cachedState != null) {
                logger.debug("Cache hit for psychological state: {}", userId);
                return (Map<String, Object>) cachedState;
            } else {
                logger.debug("Cache miss for psychological state: {}", userId);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving psychological state for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get cached Buddhist factors
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> getBuddhistFactors(UUID userId) {
        try {
            String factorsKey = BUDDHIST_FACTORS_KEY + userId;
            Object cachedFactors = redisTemplate.opsForValue().get(factorsKey);
            
            if (cachedFactors != null) {
                logger.debug("Cache hit for Buddhist factors: {}", userId);
                return (Map<String, Double>) cachedFactors;
            } else {
                logger.debug("Cache miss for Buddhist factors: {}", userId);
                return getDefaultBuddhistFactors();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving Buddhist factors for user {}: {}", userId, e.getMessage(), e);
            return getDefaultBuddhistFactors();
        }
    }
    
    /**
     * Cache personality profile with psychological insights
     */
    public void cachePersonalityProfile(UUID userId, Map<String, Object> personalityProfile) {
        try {
            String profileKey = PERSONALITY_PROFILE_KEY + userId;
            
            // Add timestamp to profile
            Map<String, Object> profileWithTimestamp = new HashMap<>();
            profileWithTimestamp.put("profile", personalityProfile);
            profileWithTimestamp.put("timestamp", LocalDateTime.now());
            profileWithTimestamp.put("cachedBy", "PsychologicalStateCacheService");
            
            redisTemplate.opsForValue().set(profileKey, profileWithTimestamp, PROFILE_TTL.toMinutes(), TimeUnit.MINUTES);
            
            logger.debug("Cached personality profile for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error caching personality profile for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get cached personality profile
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPersonalityProfile(UUID userId) {
        try {
            String profileKey = PERSONALITY_PROFILE_KEY + userId;
            Object cachedProfile = redisTemplate.opsForValue().get(profileKey);
            
            if (cachedProfile != null) {
                Map<String, Object> profileData = (Map<String, Object>) cachedProfile;
                logger.debug("Cache hit for personality profile: {}", userId);
                return (Map<String, Object>) profileData.get("profile");
            } else {
                logger.debug("Cache miss for personality profile: {}", userId);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving personality profile for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Add state to history for trend analysis
     */
    private void addToStateHistory(UUID userId, List<Double> psychologicalVector, Map<String, Double> buddhistFactors) {
        try {
            String historyKey = STATE_HISTORY_KEY + userId;
            
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("vector", psychologicalVector);
            historyEntry.put("buddhistFactors", buddhistFactors);
            historyEntry.put("timestamp", LocalDateTime.now());
            
            // Add to list (Redis List)
            redisTemplate.opsForList().leftPush(historyKey, historyEntry);
            
            // Keep only last 100 entries
            redisTemplate.opsForList().trim(historyKey, 0, 99);
            
            // Set TTL for the entire history
            redisTemplate.expire(historyKey, HISTORY_TTL.toMinutes(), TimeUnit.MINUTES);
            
            logger.debug("Added state to history for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error adding state to history for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get state history for trend analysis
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStateHistory(UUID userId, int limit) {
        try {
            String historyKey = STATE_HISTORY_KEY + userId;
            List<Object> historyObjects = redisTemplate.opsForList().range(historyKey, 0, limit - 1);
            
            if (historyObjects != null && !historyObjects.isEmpty()) {
                logger.debug("Retrieved {} history entries for user: {}", historyObjects.size(), userId);
                return historyObjects.stream()
                    .map(obj -> (Map<String, Object>) obj)
                    .toList();
            } else {
                logger.debug("No history found for user: {}", userId);
                return List.of();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving state history for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Invalidate psychological state cache
     */
    public void invalidatePsychologicalState(UUID userId) {
        try {
            String stateKey = PSYCHOLOGICAL_STATE_KEY + userId;
            String factorsKey = BUDDHIST_FACTORS_KEY + userId;
            
            redisTemplate.delete(stateKey);
            redisTemplate.delete(factorsKey);
            
            logger.debug("Invalidated psychological state cache for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error invalidating psychological state cache for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Invalidate personality profile cache
     */
    public void invalidatePersonalityProfile(UUID userId) {
        try {
            String profileKey = PERSONALITY_PROFILE_KEY + userId;
            redisTemplate.delete(profileKey);
            
            logger.debug("Invalidated personality profile cache for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error invalidating personality profile cache for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Invalidate all caches for a user
     */
    public void invalidateAllCaches(UUID userId) {
        try {
            invalidatePsychologicalState(userId);
            invalidatePersonalityProfile(userId);
            
            String historyKey = STATE_HISTORY_KEY + userId;
            redisTemplate.delete(historyKey);
            
            logger.debug("Invalidated all caches for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error invalidating all caches for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("psychologicalStateTTL", STATE_TTL.toMinutes());
            stats.put("buddhistFactorsTTL", FACTORS_TTL.toMinutes());
            stats.put("personalityProfileTTL", PROFILE_TTL.toMinutes());
            stats.put("stateHistoryTTL", HISTORY_TTL.toMinutes());
            stats.put("timestamp", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return errorMap;
        }
    }
    
    /**
     * Get default Buddhist factors when cache miss occurs
     */
    private Map<String, Double> getDefaultBuddhistFactors() {
        Map<String, Double> factors = new HashMap<>();
        factors.put("mindfulness", 0.4);
        factors.put("compassion", 0.3);
        factors.put("equanimity", 0.2);
        factors.put("nonAttachment", 0.1);
        factors.put("wisdom", 0.3);
        factors.put("vigor", 0.2);
        factors.put("faith", 0.4);
        factors.put("innerShame", 0.1);
        factors.put("shame", 0.1);
        factors.put("absenceOfCraving", 0.2);
        factors.put("absenceOfHatred", 0.2);
        factors.put("absenceOfIgnorance", 0.3);
        factors.put("lightEase", 0.2);
        factors.put("nonLaxness", 0.3);
        factors.put("renunciation", 0.2);
        factors.put("nonHarming", 0.4);
        return factors;
    }
    
    /**
     * Warm up cache with default values
     */
    public void warmUpCache(UUID userId) {
        try {
            // Cache default Buddhist factors
            String factorsKey = BUDDHIST_FACTORS_KEY + userId;
            Map<String, Double> defaultFactors = getDefaultBuddhistFactors();
            redisTemplate.opsForValue().set(factorsKey, defaultFactors, FACTORS_TTL.toMinutes(), TimeUnit.MINUTES);
            
            logger.debug("Warmed up cache for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error warming up cache for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Check if psychological state is cached
     */
    public boolean isPsychologicalStateCached(UUID userId) {
        try {
            String stateKey = PSYCHOLOGICAL_STATE_KEY + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(stateKey));
        } catch (Exception e) {
            logger.error("Error checking cache status for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get cache hit rate (simplified implementation)
     */
    public double getCacheHitRate() {
        // This would be implemented with proper metrics collection
        // For now, return a placeholder value
        return 0.85; // 85% cache hit rate
    }
}
