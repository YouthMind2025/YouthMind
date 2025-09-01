package ai.youthmind.service;

import ai.youthmind.dto.FeedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "localhost", matchIfMissing = true)
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private String postCreatedChannel;

    @Autowired
    private String aiProcessedChannel;

    // Cache TTL settings (coordinated with Harley)
    private static final long FEED_CACHE_TTL_MINUTES = 5;
    private static final long POST_CACHE_TTL_HOURS = 24;
    private static final long USER_SESSION_TTL_HOURS = 24;
    private static final long RATE_LIMIT_TTL_MINUTES = 5;

    // Cache key structure (coordinated with Harley)
    private static final String FEED_KEY_PREFIX = "youthmind:feed:";
    private static final String POST_KEY_PREFIX = "youthmind:post:";
    private static final String USER_KEY_PREFIX = "youthmind:user:";
    private static final String RATE_KEY_PREFIX = "youthmind:rate:";

    // Basic operations
    public void setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            System.err.println("⚠️ Redis SET operation failed: " + e.getMessage());
        }
    }

    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            System.err.println("⚠️ Redis SET with TTL operation failed: " + e.getMessage());
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("⚠️ Redis GET operation failed: " + e.getMessage());
            return null;
        }
    }

    public Boolean deleteValue(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("⚠️ Redis DELETE operation failed: " + e.getMessage());
            return false;
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            System.err.println("⚠️ Redis INCREMENT operation failed: " + e.getMessage());
            return 0L;
        }
    }

    // Feed caching methods
    public void cacheFeed(String cacheKey, FeedResponse feedResponse) {
        String key = FEED_KEY_PREFIX + cacheKey;
        setValue(key, feedResponse, FEED_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public FeedResponse getCachedFeed(String cacheKey) {
        String key = FEED_KEY_PREFIX + cacheKey;
        Object cached = redisTemplate.opsForValue().get(key);
        return cached instanceof FeedResponse ? (FeedResponse) cached : null;
    }

    public void invalidateFeedCache() {
        String pattern = FEED_KEY_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // Post caching methods
    public void cachePost(String postId, Object post) {
        String key = POST_KEY_PREFIX + postId;
        setValue(key, post, POST_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public Object getCachedPost(String postId) {
        String key = POST_KEY_PREFIX + postId;
        return getValue(key);
    }

    public void invalidatePostCache(String postId) {
        String key = POST_KEY_PREFIX + postId;
        deleteValue(key);
    }

    // User session caching
    public void cacheUserSession(String userId, Object sessionData) {
        String key = USER_KEY_PREFIX + userId;
        setValue(key, sessionData, USER_SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    public Object getCachedUserSession(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return getValue(key);
    }

    // Rate limiting (coordinated with Harley)
    public void setRateLimit(String identifier, int count) {
        String key = RATE_KEY_PREFIX + identifier;
        setValue(key, count, RATE_LIMIT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Integer getRateLimit(String identifier) {
        String key = RATE_KEY_PREFIX + identifier;
        Object value = getValue(key);
        return value instanceof Integer ? (Integer) value : null;
    }

    public void incrementRateLimit(String identifier) {
        String key = RATE_KEY_PREFIX + identifier;
        increment(key);
    }

    // Hot post scoring (coordinated with Harley)
    public void updateHotPostScore(String postId, double score) {
        String key = "youthmind:hot_posts";
        try {
            redisTemplate.opsForZSet().add(key, postId, score);
        } catch (Exception e) {
            System.err.println("⚠️ Redis ZSET operation failed: " + e.getMessage());
        }
    }

    public Set<Object> getHotPosts(int limit) {
        String key = "youthmind:hot_posts";
        try {
            return redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        } catch (Exception e) {
            System.err.println("⚠️ Redis ZSET operation failed: " + e.getMessage());
            return null;
        }
    }

    // Pub/Sub messaging
    public void publishPostCreated(Object message) {
        try {
            redisTemplate.convertAndSend(postCreatedChannel, message);
        } catch (Exception e) {
            System.err.println("⚠️ Redis PUBLISH operation failed: " + e.getMessage());
        }
    }

    public void publishAiProcessed(Object message) {
        try {
            redisTemplate.convertAndSend(aiProcessedChannel, message);
        } catch (Exception e) {
            System.err.println("⚠️ Redis PUBLISH operation failed: " + e.getMessage());
        }
    }

    // Enhanced event publishing with structured data
    public void publishPostCreatedEvent(String postId, String userId, String emotion) {
        PostCreatedEvent event = new PostCreatedEvent(postId, userId, emotion, System.currentTimeMillis());
        publishPostCreated(event);
    }

    public void publishAiProcessedEvent(String postId, String analysisResult) {
        AiProcessedEvent event = new AiProcessedEvent(postId, analysisResult, System.currentTimeMillis());
        publishAiProcessed(event);
    }

    public void setExpiration(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            System.err.println("⚠️ Redis EXPIRE operation failed: " + e.getMessage());
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            System.err.println("⚠️ Redis EXISTS operation failed: " + e.getMessage());
            return false;
        }
    }

    // Event classes for structured pub/sub
    public static class PostCreatedEvent {
        private String postId;
        private String userId;
        private String emotion;
        private long timestamp;

        public PostCreatedEvent(String postId, String userId, String emotion, long timestamp) {
            this.postId = postId;
            this.userId = userId;
            this.emotion = emotion;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getEmotion() { return emotion; }
        public void setEmotion(String emotion) { this.emotion = emotion; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class AiProcessedEvent {
        private String postId;
        private String analysisResult;
        private long timestamp;

        public AiProcessedEvent(String postId, String analysisResult, long timestamp) {
            this.postId = postId;
            this.analysisResult = analysisResult;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
        
        public String getAnalysisResult() { return analysisResult; }
        public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
} 