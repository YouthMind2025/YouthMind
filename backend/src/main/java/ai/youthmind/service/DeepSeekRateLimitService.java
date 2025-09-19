package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing DeepSeek API rate limits and request throttling.
 * Implements token bucket algorithm for rate limiting.
 */
@Service
public class DeepSeekRateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekRateLimitService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    // Rate limiting state
    private final ConcurrentHashMap<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();
    private final AtomicInteger globalRequestCount = new AtomicInteger(0);
    private LocalDateTime lastGlobalReset = LocalDateTime.now();
    
    /**
     * Rate limit bucket for token bucket algorithm
     */
    private static class RateLimitBucket {
        private final AtomicInteger tokens;
        private final int capacity;
        private final int refillRate; // tokens per minute
        private LocalDateTime lastRefill;
        
        public RateLimitBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefill = LocalDateTime.now();
        }
        
        public boolean tryConsume(int tokensToConsume) {
            refillTokens();
            return tokens.getAndUpdate(current -> {
                if (current >= tokensToConsume) {
                    return current - tokensToConsume;
                }
                return current;
            }) >= tokensToConsume;
        }
        
        public int getAvailableTokens() {
            refillTokens();
            return tokens.get();
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        private void refillTokens() {
            LocalDateTime now = LocalDateTime.now();
            long minutesSinceLastRefill = ChronoUnit.MINUTES.between(lastRefill, now);
            
            if (minutesSinceLastRefill > 0) {
                int tokensToAdd = (int) (minutesSinceLastRefill * refillRate);
                int newTokenCount = Math.min(capacity, tokens.get() + tokensToAdd);
                tokens.set(newTokenCount);
                lastRefill = now;
            }
        }
    }
    
    /**
     * Check if request is allowed based on rate limits
     * 
     * @param userId User ID for per-user rate limiting (optional)
     * @param requestType Type of request (moderation, chat, etc.)
     * @return RateLimitResult with decision and details
     */
    public RateLimitResult checkRateLimit(String userId, String requestType) {
        try {
            // Check global rate limit
            RateLimitResult globalResult = checkGlobalRateLimit();
            if (!globalResult.isAllowed()) {
                return globalResult;
            }
            
            // Check per-user rate limit if userId provided
            if (userId != null) {
                RateLimitResult userResult = checkUserRateLimit(userId, requestType);
                if (!userResult.isAllowed()) {
                    return userResult;
                }
            }
            
            // Check per-request-type rate limit
            RateLimitResult typeResult = checkRequestTypeRateLimit(requestType);
            if (!typeResult.isAllowed()) {
                return typeResult;
            }
            
            // All checks passed
            return new RateLimitResult(true, "Rate limit check passed", 
                getRateLimitStatus(userId, requestType));
            
        } catch (Exception e) {
            logger.error("Error checking rate limit: {}", e.getMessage(), e);
            // On error, allow request but log the issue
            return new RateLimitResult(true, "Rate limit check failed, allowing request", null);
        }
    }
    
    /**
     * Check global rate limit
     */
    private RateLimitResult checkGlobalRateLimit() {
        LocalDateTime now = LocalDateTime.now();
        long minutesSinceReset = ChronoUnit.MINUTES.between(lastGlobalReset, now);
        
        // Reset global counter every minute
        if (minutesSinceReset >= 1) {
            globalRequestCount.set(0);
            lastGlobalReset = now;
        }
        
        int currentCount = globalRequestCount.get();
        int limit = deepSeekConfig.getRateLimitPerMinute();
        
        if (currentCount >= limit) {
            return new RateLimitResult(false, 
                String.format("Global rate limit exceeded: %d/%d requests per minute", currentCount, limit),
                new RateLimitStatus(limit, currentCount, 0, "global"));
        }
        
        return new RateLimitResult(true, "Global rate limit check passed", null);
    }
    
    /**
     * Check per-user rate limit
     */
    private RateLimitResult checkUserRateLimit(String userId, String requestType) {
        String bucketKey = userId + ":" + requestType;
        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(bucketKey, 
            k -> new RateLimitBucket(deepSeekConfig.getRateLimitBurst(), 
                                   deepSeekConfig.getRateLimitPerMinute()));
        
        if (!bucket.tryConsume(1)) {
            return new RateLimitResult(false, 
                String.format("User rate limit exceeded for %s: %d/%d tokens available", 
                    requestType, bucket.getAvailableTokens(), bucket.getCapacity()),
                new RateLimitStatus(bucket.getCapacity(), bucket.getAvailableTokens(), 0, "user"));
        }
        
        return new RateLimitResult(true, "User rate limit check passed", null);
    }
    
    /**
     * Check per-request-type rate limit
     */
    private RateLimitResult checkRequestTypeRateLimit(String requestType) {
        // Different request types may have different limits
        int limit = switch (requestType.toLowerCase()) {
            case "moderation" -> deepSeekConfig.getRateLimitPerMinute();
            case "chat" -> deepSeekConfig.getRateLimitPerMinute() / 2; // Chat requests are more expensive
            case "analysis" -> deepSeekConfig.getRateLimitPerMinute() / 3; // Analysis requests are most expensive
            default -> deepSeekConfig.getRateLimitPerMinute();
        };
        
        String bucketKey = "type:" + requestType;
        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(bucketKey, 
            k -> new RateLimitBucket(deepSeekConfig.getRateLimitBurst(), limit));
        
        if (!bucket.tryConsume(1)) {
            return new RateLimitResult(false, 
                String.format("Request type rate limit exceeded for %s: %d/%d tokens available", 
                    requestType, bucket.getAvailableTokens(), bucket.getCapacity()),
                new RateLimitStatus(bucket.getCapacity(), bucket.getAvailableTokens(), 0, "type"));
        }
        
        return new RateLimitResult(true, "Request type rate limit check passed", null);
    }
    
    /**
     * Record a successful request
     */
    public void recordRequest(String userId, String requestType) {
        try {
            // Increment global counter
            globalRequestCount.incrementAndGet();
            
            // Update user bucket (already consumed in checkRateLimit)
            if (userId != null) {
                String bucketKey = userId + ":" + requestType;
                RateLimitBucket bucket = rateLimitBuckets.get(bucketKey);
                if (bucket != null) {
                    // Token already consumed, just log
                    logger.debug("Recorded request for user {} type {} - tokens remaining: {}", 
                        userId, requestType, bucket.getAvailableTokens());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error recording request: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get current rate limit status
     */
    public RateLimitStatus getRateLimitStatus(String userId, String requestType) {
        try {
            // Global status
            int globalLimit = deepSeekConfig.getRateLimitPerMinute();
            int globalUsed = globalRequestCount.get();
            int globalRemaining = Math.max(0, globalLimit - globalUsed);
            
            // User status
            int userRemaining = 0;
            if (userId != null) {
                String bucketKey = userId + ":" + requestType;
                RateLimitBucket bucket = rateLimitBuckets.get(bucketKey);
                if (bucket != null) {
                    userRemaining = bucket.getAvailableTokens();
                }
            }
            
            return new RateLimitStatus(globalLimit, globalRemaining, userRemaining, "combined");
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status: {}", e.getMessage(), e);
            return new RateLimitStatus(0, 0, 0, "error");
        }
    }
    
    /**
     * Get all rate limit buckets status
     */
    public java.util.Map<String, RateLimitStatus> getAllRateLimitStatus() {
        java.util.Map<String, RateLimitStatus> status = new ConcurrentHashMap<>();
        
        try {
            // Global status
            int globalLimit = deepSeekConfig.getRateLimitPerMinute();
            int globalUsed = globalRequestCount.get();
            int globalRemaining = Math.max(0, globalLimit - globalUsed);
            status.put("global", new RateLimitStatus(globalLimit, globalRemaining, 0, "global"));
            
            // User bucket status
            rateLimitBuckets.forEach((key, bucket) -> {
                status.put(key, new RateLimitStatus(bucket.getCapacity(), bucket.getAvailableTokens(), 0, "bucket"));
            });
            
        } catch (Exception e) {
            logger.error("Error getting all rate limit status: {}", e.getMessage(), e);
        }
        
        return status;
    }
    
    /**
     * Reset rate limits (for testing or emergency)
     */
    public void resetRateLimits() {
        try {
            globalRequestCount.set(0);
            lastGlobalReset = LocalDateTime.now();
            rateLimitBuckets.clear();
            logger.info("Rate limits reset");
        } catch (Exception e) {
            logger.error("Error resetting rate limits: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Result of rate limit check
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        private final RateLimitStatus status;
        
        public RateLimitResult(boolean allowed, String message, RateLimitStatus status) {
            this.allowed = allowed;
            this.message = message;
            this.status = status;
        }
        
        // Getters
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public RateLimitStatus getStatus() { return status; }
    }
    
    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        private final int limit;
        private final int remaining;
        private final int userRemaining;
        private final String type;
        
        public RateLimitStatus(int limit, int remaining, int userRemaining, String type) {
            this.limit = limit;
            this.remaining = remaining;
            this.userRemaining = userRemaining;
            this.type = type;
        }
        
        // Getters
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public int getUserRemaining() { return userRemaining; }
        public String getType() { return type; }
        
        public double getUtilizationPercentage() {
            return limit > 0 ? (double) (limit - remaining) / limit * 100 : 0.0;
        }
    }
}
