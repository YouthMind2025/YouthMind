package ai.youthmind.service;

import ai.youthmind.dto.FeedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
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
        redisTemplate.opsForValue().set(key, value);
    }

    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean deleteValue(String key) {
        return redisTemplate.delete(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // Feed caching methods
    public void cacheFeed(String cacheKey, FeedResponse feedResponse) {
        String key = FEED_KEY_PREFIX + cacheKey;
        redisTemplate.opsForValue().set(key, feedResponse, FEED_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
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
        redisTemplate.opsForValue().set(key, post, POST_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public Object getCachedPost(String postId) {
        String key = POST_KEY_PREFIX + postId;
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidatePostCache(String postId) {
        String key = POST_KEY_PREFIX + postId;
        redisTemplate.delete(key);
    }

    // User session caching
    public void cacheUserSession(String userId, Object sessionData) {
        String key = USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, sessionData, USER_SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    public Object getCachedUserSession(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    // Rate limiting (coordinated with Harley)
    public void setRateLimit(String identifier, int count) {
        String key = RATE_KEY_PREFIX + identifier;
        redisTemplate.opsForValue().set(key, count, RATE_LIMIT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Integer getRateLimit(String identifier) {
        String key = RATE_KEY_PREFIX + identifier;
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof Integer ? (Integer) value : null;
    }

    public void incrementRateLimit(String identifier) {
        String key = RATE_KEY_PREFIX + identifier;
        redisTemplate.opsForValue().increment(key);
    }

    // Hot post scoring (coordinated with Harley)
    public void updateHotPostScore(String postId, double score) {
        String key = "youthmind:hot_posts";
        redisTemplate.opsForZSet().add(key, postId, score);
    }

    public Set<Object> getHotPosts(int limit) {
        String key = "youthmind:hot_posts";
        return redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
    }

    // Pub/Sub messaging
    public void publishPostCreated(Object message) {
        redisTemplate.convertAndSend(postCreatedChannel, message);
    }

    public void publishAiProcessed(Object message) {
        redisTemplate.convertAndSend(aiProcessedChannel, message);
    }

    // Enhanced event publishing with structured data
    public void publishPostCreatedEvent(String postId, String userId, String emotion) {
        PostCreatedEvent event = new PostCreatedEvent(postId, userId, emotion, System.currentTimeMillis());
        redisTemplate.convertAndSend(postCreatedChannel, event);
    }

    public void publishAiProcessedEvent(String postId, String analysisResult) {
        AiProcessedEvent event = new AiProcessedEvent(postId, analysisResult, System.currentTimeMillis());
        redisTemplate.convertAndSend(aiProcessedChannel, event);
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