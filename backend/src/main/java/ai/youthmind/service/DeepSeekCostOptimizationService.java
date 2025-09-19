package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for optimizing DeepSeek API costs through caching and intelligent pricing.
 * Implements cache-hit pricing and cost tracking.
 */
@Service
public class DeepSeekCostOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekCostOptimizationService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Cache configuration
    private static final String CACHE_PREFIX = "deepseek:cache:";
    private static final String COST_TRACKING_PREFIX = "deepseek:cost:";
    private static final int DEFAULT_CACHE_TTL_HOURS = 24;
    
    /**
     * Calculate optimized cost for API request
     * 
     * @param modelName The model being used
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @param isCacheHit Whether this is a cache hit
     * @param userId User ID for cost tracking
     * @return CostOptimizationResult with cost details
     */
    public CostOptimizationResult calculateOptimizedCost(String modelName, int inputTokens, int outputTokens, 
                                                        boolean isCacheHit, UUID userId) {
        try {
            DeepSeekConfig.ModelConfig modelConfig = deepSeekConfig.getModelConfig(modelName);
            
            // Calculate base cost
            double baseCost = modelConfig.calculateCost(inputTokens, outputTokens);
            
            // Apply cache hit discount if enabled
            double finalCost = baseCost;
            double discountApplied = 0.0;
            
            if (isCacheHit && deepSeekConfig.isEnableCacheHitPricing()) {
                discountApplied = baseCost * deepSeekConfig.getCacheHitDiscount();
                finalCost = baseCost - discountApplied;
                
                logger.debug("Cache hit discount applied: ${} ({}% off)", 
                    discountApplied, deepSeekConfig.getCacheHitDiscount() * 100);
            }
            
            // Round to 6 decimal places for precision
            BigDecimal roundedCost = BigDecimal.valueOf(finalCost).setScale(6, RoundingMode.HALF_UP);
            BigDecimal roundedDiscount = BigDecimal.valueOf(discountApplied).setScale(6, RoundingMode.HALF_UP);
            
            // Track cost if enabled
            if (deepSeekConfig.isEnableCostTracking()) {
                trackCost(userId, modelName, inputTokens, outputTokens, roundedCost.doubleValue(), isCacheHit);
            }
            
            return new CostOptimizationResult(
                roundedCost.doubleValue(),
                roundedDiscount.doubleValue(),
                baseCost,
                isCacheHit,
                modelName,
                inputTokens,
                outputTokens
            );
            
        } catch (Exception e) {
            logger.error("Error calculating optimized cost: {}", e.getMessage(), e);
            // Return fallback cost calculation
            return new CostOptimizationResult(0.0, 0.0, 0.0, false, modelName, inputTokens, outputTokens);
        }
    }
    
    /**
     * Check if request can be served from cache
     * 
     * @param requestHash Hash of the request content
     * @return CacheCheckResult with cache status
     */
    public CacheCheckResult checkCache(String requestHash) {
        try {
            String cacheKey = CACHE_PREFIX + requestHash;
            Object cachedResponse = redisTemplate.opsForValue().get(cacheKey);
            
            boolean isCacheHit = cachedResponse != null;
            LocalDateTime cacheTimestamp = null;
            
            if (isCacheHit) {
                // Get cache timestamp
                String timestampKey = cacheKey + ":timestamp";
                Object timestamp = redisTemplate.opsForValue().get(timestampKey);
                if (timestamp instanceof LocalDateTime) {
                    cacheTimestamp = (LocalDateTime) timestamp;
                }
                
                logger.debug("Cache hit for request hash: {}", requestHash);
            } else {
                logger.debug("Cache miss for request hash: {}", requestHash);
            }
            
            return new CacheCheckResult(isCacheHit, cachedResponse, cacheTimestamp, requestHash);
            
        } catch (Exception e) {
            logger.error("Error checking cache: {}", e.getMessage(), e);
            return new CacheCheckResult(false, null, null, requestHash);
        }
    }
    
    /**
     * Store response in cache
     * 
     * @param requestHash Hash of the request content
     * @param response Response to cache
     * @param ttlHours TTL in hours (optional, uses default if null)
     */
    public void storeInCache(String requestHash, Object response, Integer ttlHours) {
        try {
            String cacheKey = CACHE_PREFIX + requestHash;
            int ttl = ttlHours != null ? ttlHours : DEFAULT_CACHE_TTL_HOURS;
            
            // Store response
            redisTemplate.opsForValue().set(cacheKey, response, ttl, TimeUnit.HOURS);
            
            // Store timestamp
            String timestampKey = cacheKey + ":timestamp";
            redisTemplate.opsForValue().set(timestampKey, LocalDateTime.now(), ttl, TimeUnit.HOURS);
            
            logger.debug("Stored response in cache for request hash: {} (TTL: {} hours)", requestHash, ttl);
            
        } catch (Exception e) {
            logger.error("Error storing in cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Track cost for analytics and billing
     */
    private void trackCost(UUID userId, String modelName, int inputTokens, int outputTokens, 
                         double cost, boolean isCacheHit) {
        try {
            String costKey = COST_TRACKING_PREFIX + "daily:" + LocalDateTime.now().toLocalDate();
            
            // Create cost record
            CostRecord costRecord = new CostRecord(
                userId, modelName, inputTokens, outputTokens, cost, isCacheHit, LocalDateTime.now()
            );
            
            // Store in Redis list
            redisTemplate.opsForList().leftPush(costKey, costRecord);
            
            // Set TTL for cost tracking (keep for 30 days)
            redisTemplate.expire(costKey, 30, TimeUnit.DAYS);
            
            logger.debug("Tracked cost: ${} for user {} model {} (cache hit: {})", 
                cost, userId, modelName, isCacheHit);
            
        } catch (Exception e) {
            logger.error("Error tracking cost: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get cost summary for a user
     * 
     * @param userId User ID
     * @param days Number of days to look back
     * @return CostSummary with aggregated costs
     */
    public CostSummary getCostSummary(UUID userId, int days) {
        try {
            double totalCost = 0.0;
            double cacheHitSavings = 0.0;
            int totalRequests = 0;
            int cacheHits = 0;
            
            // Aggregate costs for the specified period
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String costKey = COST_TRACKING_PREFIX + "daily:" + date.toLocalDate();
                
                java.util.List<Object> costRecords = redisTemplate.opsForList().range(costKey, 0, -1);
                if (costRecords != null) {
                    for (Object record : costRecords) {
                        if (record instanceof CostRecord) {
                            CostRecord costRecord = (CostRecord) record;
                            if (userId == null || userId.equals(costRecord.getUserId())) {
                                totalCost += costRecord.getCost();
                                totalRequests++;
                                
                                if (costRecord.isCacheHit()) {
                                    cacheHits++;
                                    // Calculate savings (would have been full cost)
                                    double fullCost = calculateFullCost(costRecord.getModelName(), 
                                        costRecord.getInputTokens(), costRecord.getOutputTokens());
                                    cacheHitSavings += fullCost - costRecord.getCost();
                                }
                            }
                        }
                    }
                }
            }
            
            double cacheHitRate = totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0;
            
            return new CostSummary(totalCost, cacheHitSavings, totalRequests, cacheHits, cacheHitRate);
            
        } catch (Exception e) {
            logger.error("Error getting cost summary: {}", e.getMessage(), e);
            return new CostSummary(0.0, 0.0, 0, 0, 0.0);
        }
    }
    
    /**
     * Calculate full cost without cache discount
     */
    private double calculateFullCost(String modelName, int inputTokens, int outputTokens) {
        DeepSeekConfig.ModelConfig modelConfig = deepSeekConfig.getModelConfig(modelName);
        return modelConfig.calculateCost(inputTokens, outputTokens);
    }
    
    /**
     * Generate cache key hash for request
     * 
     * @param content Request content
     * @param model Model name
     * @param temperature Temperature setting
     * @return Hash string for caching
     */
    public String generateCacheHash(String content, String model, double temperature) {
        try {
            String input = content + "|" + model + "|" + temperature;
            return String.valueOf(input.hashCode());
        } catch (Exception e) {
            logger.error("Error generating cache hash: {}", e.getMessage(), e);
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Clear cache for a specific request hash
     */
    public void clearCache(String requestHash) {
        try {
            String cacheKey = CACHE_PREFIX + requestHash;
            String timestampKey = cacheKey + ":timestamp";
            
            redisTemplate.delete(cacheKey);
            redisTemplate.delete(timestampKey);
            
            logger.debug("Cleared cache for request hash: {}", requestHash);
            
        } catch (Exception e) {
            logger.error("Error clearing cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear all caches (for maintenance)
     */
    public void clearAllCaches() {
        try {
            java.util.Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Cleared {} cache entries", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error clearing all caches: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Result of cost optimization calculation
     */
    public static class CostOptimizationResult {
        private final double finalCost;
        private final double discountApplied;
        private final double baseCost;
        private final boolean isCacheHit;
        private final String modelName;
        private final int inputTokens;
        private final int outputTokens;
        
        public CostOptimizationResult(double finalCost, double discountApplied, double baseCost, 
                                    boolean isCacheHit, String modelName, int inputTokens, int outputTokens) {
            this.finalCost = finalCost;
            this.discountApplied = discountApplied;
            this.baseCost = baseCost;
            this.isCacheHit = isCacheHit;
            this.modelName = modelName;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }
        
        // Getters
        public double getFinalCost() { return finalCost; }
        public double getDiscountApplied() { return discountApplied; }
        public double getBaseCost() { return baseCost; }
        public boolean isCacheHit() { return isCacheHit; }
        public String getModelName() { return modelName; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        
        public double getSavingsPercentage() {
            return baseCost > 0 ? (discountApplied / baseCost) * 100 : 0.0;
        }
    }
    
    /**
     * Result of cache check
     */
    public static class CacheCheckResult {
        private final boolean isCacheHit;
        private final Object cachedResponse;
        private final LocalDateTime cacheTimestamp;
        private final String requestHash;
        
        public CacheCheckResult(boolean isCacheHit, Object cachedResponse, LocalDateTime cacheTimestamp, String requestHash) {
            this.isCacheHit = isCacheHit;
            this.cachedResponse = cachedResponse;
            this.cacheTimestamp = cacheTimestamp;
            this.requestHash = requestHash;
        }
        
        // Getters
        public boolean isCacheHit() { return isCacheHit; }
        public Object getCachedResponse() { return cachedResponse; }
        public LocalDateTime getCacheTimestamp() { return cacheTimestamp; }
        public String getRequestHash() { return requestHash; }
    }
    
    /**
     * Cost record for tracking
     */
    public static class CostRecord {
        private final UUID userId;
        private final String modelName;
        private final int inputTokens;
        private final int outputTokens;
        private final double cost;
        private final boolean isCacheHit;
        private final LocalDateTime timestamp;
        
        public CostRecord(UUID userId, String modelName, int inputTokens, int outputTokens, 
                         double cost, boolean isCacheHit, LocalDateTime timestamp) {
            this.userId = userId;
            this.modelName = modelName;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.cost = cost;
            this.isCacheHit = isCacheHit;
            this.timestamp = timestamp;
        }
        
        // Getters
        public UUID getUserId() { return userId; }
        public String getModelName() { return modelName; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public double getCost() { return cost; }
        public boolean isCacheHit() { return isCacheHit; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Cost summary for analytics
     */
    public static class CostSummary {
        private final double totalCost;
        private final double cacheHitSavings;
        private final int totalRequests;
        private final int cacheHits;
        private final double cacheHitRate;
        
        public CostSummary(double totalCost, double cacheHitSavings, int totalRequests, 
                          int cacheHits, double cacheHitRate) {
            this.totalCost = totalCost;
            this.cacheHitSavings = cacheHitSavings;
            this.totalRequests = totalRequests;
            this.cacheHits = cacheHits;
            this.cacheHitRate = cacheHitRate;
        }
        
        // Getters
        public double getTotalCost() { return totalCost; }
        public double getCacheHitSavings() { return cacheHitSavings; }
        public int getTotalRequests() { return totalRequests; }
        public int getCacheHits() { return cacheHits; }
        public double getCacheHitRate() { return cacheHitRate; }
        
        public double getEffectiveCost() {
            return totalCost + cacheHitSavings; // What it would have cost without caching
        }
    }
}
