package ai.youthmind.service;

import ai.youthmind.dto.RecommendationRuleDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing recommendation engine caching.
 * Handles caching of mapping rules, R vectors, feed blocks, and shown posts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "youthmind.recommendation.enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendationRuleService recommendationRuleService;
    private final MeterRegistry meterRegistry;

    // Cache key patterns
    private static final String MAPPING_RULES_KEY_PREFIX = "recommendation:mapping:";
    private static final String RVECTOR_KEY_PREFIX = "recommendation:rvector:";
    private static final String FEED_BLOCK_KEY_PREFIX = "recommendation:block:";
    private static final String SHOWN_POSTS_KEY_PREFIX = "recommendation:shown:";

    // TTL values (in seconds)
    private static final long MAPPING_RULES_TTL = 3600; // 1 hour
    private static final long RVECTOR_TTL = 1800; // 30 minutes
    private static final long FEED_BLOCK_TTL = 3600; // 1 hour
    private static final long SHOWN_POSTS_TTL = 604800; // 7 days

    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheEvictions;
    private final Timer cacheLatency;

    public RecommendationCacheService(RedisTemplate<String, Object> redisTemplate,
                                    ObjectMapper objectMapper,
                                    RecommendationRuleService recommendationRuleService,
                                    MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.recommendationRuleService = recommendationRuleService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.cacheHits = Counter.builder("recommendation.cache.hits")
                .register(meterRegistry);
        this.cacheMisses = Counter.builder("recommendation.cache.misses")
                .register(meterRegistry);
        this.cacheEvictions = Counter.builder("recommendation.cache.evictions")
                .register(meterRegistry);
        this.cacheLatency = Timer.builder("recommendation.cache.latency")
                .register(meterRegistry);
    }

    @PostConstruct
    public void warmUpCache() {
        try {
            log.info("Warming up recommendation cache...");
            warmUpMappingRules();
            log.info("Cache warm-up completed successfully");
        } catch (Exception e) {
            log.error("Failed to warm up cache: {}", e.getMessage(), e);
        }
    }

    // ===== MAPPING RULES CACHING =====

    /**
     * Get mapping rule from cache
     */
    public RecommendationRuleDto getMappingRule(String emotionCombination) {
        return cacheLatency.record(() -> {
            try {
                String key = MAPPING_RULES_KEY_PREFIX + emotionCombination;
                Object cached = redisTemplate.opsForValue().get(key);
                
                if (cached != null) {
                    cacheHits.increment();
                    return objectMapper.readValue(cached.toString(), RecommendationRuleDto.class);
                } else {
                    cacheMisses.increment();
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to get mapping rule from cache: {}", e.getMessage());
                cacheMisses.increment();
                return null;
            }
        });
    }

    /**
     * Cache mapping rule
     */
    public void cacheMappingRule(RecommendationRuleDto rule) {
        try {
            String key = MAPPING_RULES_KEY_PREFIX + rule.getEmotionCombination();
            String json = objectMapper.writeValueAsString(rule);
            redisTemplate.opsForValue().set(key, json, MAPPING_RULES_TTL, TimeUnit.SECONDS);
            log.debug("Cached mapping rule: {}", rule.getEmotionCombination());
        } catch (Exception e) {
            log.error("Failed to cache mapping rule: {}", e.getMessage());
        }
    }

    /**
     * Invalidate mapping rule cache
     */
    public void invalidateMappingRule(String emotionCombination) {
        try {
            String key = MAPPING_RULES_KEY_PREFIX + emotionCombination;
            redisTemplate.delete(key);
            log.debug("Invalidated mapping rule cache: {}", emotionCombination);
        } catch (Exception e) {
            log.error("Failed to invalidate mapping rule cache: {}", e.getMessage());
        }
    }

    /**
     * Warm up mapping rules cache
     */
    private void warmUpMappingRules() {
        try {
            List<RecommendationRuleDto> rules = recommendationRuleService.getAllRules();
            for (RecommendationRuleDto rule : rules) {
                cacheMappingRule(rule);
            }
            log.info("Warmed up {} mapping rules", rules.size());
        } catch (Exception e) {
            log.error("Failed to warm up mapping rules: {}", e.getMessage());
        }
    }

    // ===== R VECTOR CACHING =====

    /**
     * Get R vector from cache
     */
    public List<Double> getRVector(UUID userId) {
        return cacheLatency.record(() -> {
            try {
                String key = RVECTOR_KEY_PREFIX + userId;
                Object cached = redisTemplate.opsForValue().get(key);
                
                if (cached != null) {
                    cacheHits.increment();
                    return objectMapper.readValue(cached.toString(), List.class);
                } else {
                    cacheMisses.increment();
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to get R vector from cache: {}", e.getMessage());
                cacheMisses.increment();
                return null;
            }
        });
    }

    /**
     * Cache R vector
     */
    public void cacheRVector(UUID userId, List<Double> rVector) {
        try {
            String key = RVECTOR_KEY_PREFIX + userId;
            String json = objectMapper.writeValueAsString(rVector);
            redisTemplate.opsForValue().set(key, json, RVECTOR_TTL, TimeUnit.SECONDS);
            log.debug("Cached R vector for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to cache R vector: {}", e.getMessage());
        }
    }

    /**
     * Invalidate R vector cache
     */
    public void invalidateRVector(UUID userId) {
        try {
            String key = RVECTOR_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.debug("Invalidated R vector cache for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate R vector cache: {}", e.getMessage());
        }
    }

    // ===== FEED BLOCK CACHING =====

    /**
     * Get feed block from cache
     */
    public Object getFeedBlock(UUID userId, Integer blockNumber) {
        return cacheLatency.record(() -> {
            try {
                String key = FEED_BLOCK_KEY_PREFIX + userId + ":" + blockNumber;
                Object cached = redisTemplate.opsForValue().get(key);
                
                if (cached != null) {
                    cacheHits.increment();
                    return cached;
                } else {
                    cacheMisses.increment();
                    return null;
                }
            } catch (Exception e) {
                log.error("Failed to get feed block from cache: {}", e.getMessage());
                cacheMisses.increment();
                return null;
            }
        });
    }

    /**
     * Cache feed block
     */
    public void cacheFeedBlock(UUID userId, Integer blockNumber, Object feedBlock) {
        try {
            String key = FEED_BLOCK_KEY_PREFIX + userId + ":" + blockNumber;
            redisTemplate.opsForValue().set(key, feedBlock, FEED_BLOCK_TTL, TimeUnit.SECONDS);
            log.debug("Cached feed block {} for user: {}", blockNumber, userId);
        } catch (Exception e) {
            log.error("Failed to cache feed block: {}", e.getMessage());
        }
    }

    /**
     * Invalidate feed block cache
     */
    public void invalidateFeedBlock(UUID userId, Integer blockNumber) {
        try {
            String key = FEED_BLOCK_KEY_PREFIX + userId + ":" + blockNumber;
            redisTemplate.delete(key);
            log.debug("Invalidated feed block cache {} for user: {}", blockNumber, userId);
        } catch (Exception e) {
            log.error("Failed to invalidate feed block cache: {}", e.getMessage());
        }
    }

    // ===== SHOWN POSTS CACHING =====

    /**
     * Get shown posts from cache
     */
    public Set<String> getShownPosts(UUID userId) {
        return cacheLatency.record(() -> {
            try {
                String key = SHOWN_POSTS_KEY_PREFIX + userId;
                Set<Object> cached = redisTemplate.opsForSet().members(key);
                
                if (cached != null && !cached.isEmpty()) {
                    cacheHits.increment();
                    return cached.stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.toSet());
                } else {
                    cacheMisses.increment();
                    return Set.of();
                }
            } catch (Exception e) {
                log.error("Failed to get shown posts from cache: {}", e.getMessage());
                cacheMisses.increment();
                return Set.of();
            }
        });
    }

    /**
     * Add shown post to cache
     */
    public void addShownPost(UUID userId, UUID postId) {
        try {
            String key = SHOWN_POSTS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(key, postId.toString());
            redisTemplate.expire(key, SHOWN_POSTS_TTL, TimeUnit.SECONDS);
            
            // Maintain sliding window of last 500 posts
            Long size = redisTemplate.opsForSet().size(key);
            if (size != null && size > 500) {
                // Remove oldest entries (Redis Set doesn't have order, so we'll use a different approach)
                // For now, we'll just set TTL to maintain the sliding window
                log.debug("Added shown post {} for user: {} (total: {})", postId, userId, size);
            }
        } catch (Exception e) {
            log.error("Failed to add shown post to cache: {}", e.getMessage());
        }
    }

    /**
     * Check if post is shown to user
     */
    public boolean isPostShown(UUID userId, UUID postId) {
        try {
            String key = SHOWN_POSTS_KEY_PREFIX + userId;
            Boolean isMember = redisTemplate.opsForSet().isMember(key, postId.toString());
            return isMember != null && isMember;
        } catch (Exception e) {
            log.error("Failed to check if post is shown: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Clear shown posts cache for user
     */
    public void clearShownPosts(UUID userId) {
        try {
            String key = SHOWN_POSTS_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.debug("Cleared shown posts cache for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to clear shown posts cache: {}", e.getMessage());
        }
    }

    // ===== CACHE MANAGEMENT =====

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        try {
            return Map.of(
                    "cacheHits", cacheHits.count(),
                    "cacheMisses", cacheMisses.count(),
                    "cacheEvictions", cacheEvictions.count(),
                    "hitRate", cacheHits.count() / (cacheHits.count() + cacheMisses.count()) * 100
            );
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Clear all recommendation caches
     */
    public void clearAllCaches() {
        try {
            Set<String> keys = redisTemplate.keys("recommendation:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} recommendation cache keys", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear all caches: {}", e.getMessage());
        }
    }
}
