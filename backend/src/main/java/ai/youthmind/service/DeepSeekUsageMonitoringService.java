package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.entity.AiApiUsage;
import ai.youthmind.repository.AiApiUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for comprehensive monitoring and tracking of DeepSeek API usage,
 * costs, and performance metrics.
 */
@Service
public class DeepSeekUsageMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekUsageMonitoringService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private AiApiUsageRepository aiApiUsageRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Real-time metrics tracking
    private final Map<String, UsageMetrics> realtimeMetrics = new ConcurrentHashMap<>();
    
    // Redis keys for persistent metrics
    private static final String DAILY_USAGE_KEY = "deepseek:usage:daily:";
    private static final String HOURLY_USAGE_KEY = "deepseek:usage:hourly:";
    private static final String USER_USAGE_KEY = "deepseek:usage:user:";
    private static final String MODEL_USAGE_KEY = "deepseek:usage:model:";
    
    /**
     * Record API usage for monitoring and billing
     * 
     * @param userId User ID (optional)
     * @param modelName Model used
     * @param requestType Type of request
     * @param inputTokens Input tokens used
     * @param outputTokens Output tokens generated
     * @param responseTimeMs Response time in milliseconds
     * @param success Whether request was successful
     * @param cost Cost of the request
     * @param isCacheHit Whether response was from cache
     */
    public void recordUsage(UUID userId, String modelName, String requestType, 
                           int inputTokens, int outputTokens, long responseTimeMs, 
                           boolean success, double cost, boolean isCacheHit) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Create usage record
            AiApiUsage usage = new AiApiUsage();
            usage.setUserId(userId);
            usage.setModelName(modelName);
            usage.setRequestType(AiApiUsage.RequestType.valueOf(requestType.toUpperCase()));
            usage.setPromptTokens(inputTokens);
            usage.setCompletionTokens(outputTokens);
            usage.setTotalTokens(inputTokens + outputTokens);
            usage.setRequestDurationMs((int) responseTimeMs);
            usage.setSuccess(success);
            usage.setTotalCost(BigDecimal.valueOf(cost));
            usage.setCreatedAt(now);
            usage.setUpdatedAt(now);
            
            // Save to database
            aiApiUsageRepository.save(usage);
            
            // Update real-time metrics
            updateRealtimeMetrics(userId, modelName, requestType, inputTokens, outputTokens, 
                                responseTimeMs, success, cost, isCacheHit);
            
            // Update Redis metrics
            updateRedisMetrics(userId, modelName, requestType, inputTokens, outputTokens, 
                             responseTimeMs, success, cost, isCacheHit);
            
            logger.debug("Recorded usage: user={}, model={}, tokens={}, cost=${}, success={}", 
                userId, modelName, inputTokens + outputTokens, cost, success);
            
        } catch (Exception e) {
            logger.error("Error recording usage: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update real-time metrics
     */
    private void updateRealtimeMetrics(UUID userId, String modelName, String requestType, 
                                     int inputTokens, int outputTokens, long responseTimeMs, 
                                     boolean success, double cost, boolean isCacheHit) {
        try {
            // Update global metrics
            String globalKey = "global";
            UsageMetrics globalMetrics = realtimeMetrics.computeIfAbsent(globalKey, k -> new UsageMetrics());
            globalMetrics.update(inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
            // Update model-specific metrics
            String modelKey = "model:" + modelName;
            UsageMetrics modelMetrics = realtimeMetrics.computeIfAbsent(modelKey, k -> new UsageMetrics());
            modelMetrics.update(inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
            // Update user-specific metrics
            if (userId != null) {
                String userKey = "user:" + userId;
                UsageMetrics userMetrics = realtimeMetrics.computeIfAbsent(userKey, k -> new UsageMetrics());
                userMetrics.update(inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            }
            
            // Update request type metrics
            String typeKey = "type:" + requestType;
            UsageMetrics typeMetrics = realtimeMetrics.computeIfAbsent(typeKey, k -> new UsageMetrics());
            typeMetrics.update(inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
        } catch (Exception e) {
            logger.error("Error updating real-time metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update Redis metrics for persistence
     */
    private void updateRedisMetrics(UUID userId, String modelName, String requestType, 
                                   int inputTokens, int outputTokens, long responseTimeMs, 
                                   boolean success, double cost, boolean isCacheHit) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Daily metrics
            String dailyKey = DAILY_USAGE_KEY + now.toLocalDate();
            updateRedisUsageRecord(dailyKey, inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
            // Hourly metrics
            String hourlyKey = HOURLY_USAGE_KEY + now.toLocalDate() + ":" + now.getHour();
            updateRedisUsageRecord(hourlyKey, inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
            // User metrics
            if (userId != null) {
                String userKey = USER_USAGE_KEY + userId + ":" + now.toLocalDate();
                updateRedisUsageRecord(userKey, inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            }
            
            // Model metrics
            String modelKey = MODEL_USAGE_KEY + modelName + ":" + now.toLocalDate();
            updateRedisUsageRecord(modelKey, inputTokens, outputTokens, responseTimeMs, success, cost, isCacheHit);
            
        } catch (Exception e) {
            logger.error("Error updating Redis metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update Redis usage record
     */
    private void updateRedisUsageRecord(String key, int inputTokens, int outputTokens, 
                                      long responseTimeMs, boolean success, double cost, boolean isCacheHit) {
        try {
            // Use Redis hash to store metrics
            redisTemplate.opsForHash().increment(key, "total_requests", 1);
            redisTemplate.opsForHash().increment(key, "input_tokens", inputTokens);
            redisTemplate.opsForHash().increment(key, "output_tokens", outputTokens);
            redisTemplate.opsForHash().increment(key, "total_tokens", inputTokens + outputTokens);
            redisTemplate.opsForHash().increment(key, "total_response_time", responseTimeMs);
            redisTemplate.opsForHash().increment(key, "total_cost", cost);
            
            if (success) {
                redisTemplate.opsForHash().increment(key, "successful_requests", 1);
            }
            
            if (isCacheHit) {
                redisTemplate.opsForHash().increment(key, "cache_hits", 1);
            }
            
            // Set TTL for metrics (keep for 30 days)
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("Error updating Redis usage record: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get usage statistics for a specific period
     * 
     * @param userId User ID (optional, null for global stats)
     * @param modelName Model name (optional, null for all models)
     * @param days Number of days to look back
     * @return UsageStatistics with aggregated data
     */
    public UsageStatistics getUsageStatistics(UUID userId, String modelName, int days) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            
            // Query database for usage records
            List<AiApiUsage> usageRecords = aiApiUsageRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startDate, endDate);
            
            // Filter by model if specified
            if (modelName != null) {
                usageRecords = usageRecords.stream()
                    .filter(record -> modelName.equals(record.getModelName()))
                    .toList();
            }
            
            // Calculate statistics
            return calculateUsageStatistics(usageRecords);
            
        } catch (Exception e) {
            logger.error("Error getting usage statistics: {}", e.getMessage(), e);
            return new UsageStatistics();
        }
    }
    
    /**
     * Calculate usage statistics from records
     */
    private UsageStatistics calculateUsageStatistics(List<AiApiUsage> records) {
        UsageStatistics stats = new UsageStatistics();
        
        if (records.isEmpty()) {
            return stats;
        }
        
        int totalRequests = records.size();
        int successfulRequests = (int) records.stream().filter(AiApiUsage::getSuccess).count();
        int cacheHits = 0; // Would need to track this separately
        
        long totalInputTokens = records.stream().mapToLong(AiApiUsage::getPromptTokens).sum();
        long totalOutputTokens = records.stream().mapToLong(AiApiUsage::getCompletionTokens).sum();
        long totalTokens = totalInputTokens + totalOutputTokens;
        
        long totalResponseTime = records.stream().mapToLong(AiApiUsage::getRequestDurationMs).sum();
        double totalCost = records.stream().mapToDouble(r -> r.getTotalCost().doubleValue()).sum();
        
        // Calculate averages
        double avgResponseTime = totalResponseTime / (double) totalRequests;
        double avgCost = totalCost / totalRequests;
        double successRate = (double) successfulRequests / totalRequests;
        
        // Set statistics
        stats.setTotalRequests(totalRequests);
        stats.setSuccessfulRequests(successfulRequests);
        stats.setCacheHits(cacheHits);
        stats.setTotalInputTokens(totalInputTokens);
        stats.setTotalOutputTokens(totalOutputTokens);
        stats.setTotalTokens(totalTokens);
        stats.setTotalResponseTime(totalResponseTime);
        stats.setTotalCost(totalCost);
        stats.setAverageResponseTime(avgResponseTime);
        stats.setAverageCost(avgCost);
        stats.setSuccessRate(successRate);
        stats.setCacheHitRate(cacheHits / (double) totalRequests);
        
        return stats;
    }
    
    /**
     * Get real-time metrics
     * 
     * @param key Metrics key (global, model:name, user:id, type:name)
     * @return UsageMetrics for the specified key
     */
    public UsageMetrics getRealtimeMetrics(String key) {
        return realtimeMetrics.getOrDefault(key, new UsageMetrics());
    }
    
    /**
     * Get all real-time metrics
     */
    public Map<String, UsageMetrics> getAllRealtimeMetrics() {
        return Map.copyOf(realtimeMetrics);
    }
    
    /**
     * Get cost analysis for a period
     * 
     * @param userId User ID (optional)
     * @param days Number of days
     * @return CostAnalysis with cost breakdown
     */
    public CostAnalysis getCostAnalysis(UUID userId, int days) {
        try {
            UsageStatistics stats = getUsageStatistics(userId, null, days);
            
            CostAnalysis analysis = new CostAnalysis();
            analysis.setTotalCost(stats.getTotalCost());
            analysis.setAverageCostPerRequest(stats.getAverageCost());
            analysis.setTotalRequests(stats.getTotalRequests());
            analysis.setCostPerToken(stats.getTotalTokens() > 0 ? stats.getTotalCost() / stats.getTotalTokens() : 0.0);
            
            // Calculate cost by model
            Map<String, Double> costByModel = calculateCostByModel(userId, days);
            analysis.setCostByModel(costByModel);
            
            // Calculate cost trends (daily)
            Map<String, Double> dailyCosts = calculateDailyCosts(userId, days);
            analysis.setDailyCosts(dailyCosts);
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error getting cost analysis: {}", e.getMessage(), e);
            return new CostAnalysis();
        }
    }
    
    /**
     * Calculate cost by model
     */
    private Map<String, Double> calculateCostByModel(UUID userId, int days) {
        Map<String, Double> costByModel = new HashMap<>();
        
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            
            List<AiApiUsage> records = aiApiUsageRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);
            
            for (AiApiUsage record : records) {
                String modelName = record.getModelName();
                double cost = record.getTotalCost().doubleValue();
                costByModel.merge(modelName, cost, Double::sum);
            }
            
        } catch (Exception e) {
            logger.error("Error calculating cost by model: {}", e.getMessage(), e);
        }
        
        return costByModel;
    }
    
    /**
     * Calculate daily costs
     */
    private Map<String, Double> calculateDailyCosts(UUID userId, int days) {
        Map<String, Double> dailyCosts = new HashMap<>();
        
        try {
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String dateKey = date.toLocalDate().toString();
                
                LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                
                List<AiApiUsage> records = aiApiUsageRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startOfDay, endOfDay);
                
                double dailyCost = records.stream()
                    .mapToDouble(r -> r.getTotalCost().doubleValue())
                    .sum();
                
                dailyCosts.put(dateKey, dailyCost);
            }
            
        } catch (Exception e) {
            logger.error("Error calculating daily costs: {}", e.getMessage(), e);
        }
        
        return dailyCosts;
    }
    
    /**
     * Get performance metrics
     * 
     * @param userId User ID (optional)
     * @param days Number of days
     * @return PerformanceMetrics with performance data
     */
    public PerformanceMetrics getPerformanceMetrics(UUID userId, int days) {
        try {
            UsageStatistics stats = getUsageStatistics(userId, null, days);
            
            PerformanceMetrics metrics = new PerformanceMetrics();
            metrics.setAverageResponseTime(stats.getAverageResponseTime());
            metrics.setSuccessRate(stats.getSuccessRate());
            metrics.setTotalRequests(stats.getTotalRequests());
            metrics.setSuccessfulRequests(stats.getSuccessfulRequests());
            
            // Calculate percentiles
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            
            List<AiApiUsage> records = aiApiUsageRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);
            
            if (!records.isEmpty()) {
                List<Integer> responseTimes = records.stream()
                    .map(AiApiUsage::getRequestDurationMs)
                    .sorted()
                    .toList();
                
                metrics.setP50ResponseTime(calculatePercentile(responseTimes, 50));
                metrics.setP95ResponseTime(calculatePercentile(responseTimes, 95));
                metrics.setP99ResponseTime(calculatePercentile(responseTimes, 99));
            }
            
            return metrics;
            
        } catch (Exception e) {
            logger.error("Error getting performance metrics: {}", e.getMessage(), e);
            return new PerformanceMetrics();
        }
    }
    
    /**
     * Calculate percentile
     */
    private int calculatePercentile(List<Integer> values, int percentile) {
        if (values.isEmpty()) return 0;
        
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        return values.get(Math.max(0, index));
    }
    
    /**
     * Usage metrics for real-time tracking
     */
    public static class UsageMetrics {
        private int totalRequests = 0;
        private int successfulRequests = 0;
        private int cacheHits = 0;
        private long totalInputTokens = 0;
        private long totalOutputTokens = 0;
        private long totalResponseTime = 0;
        private double totalCost = 0.0;
        private LocalDateTime lastUpdated = LocalDateTime.now();
        
        public void update(int inputTokens, int outputTokens, long responseTimeMs, 
                          boolean success, double cost, boolean isCacheHit) {
            totalRequests++;
            if (success) successfulRequests++;
            if (isCacheHit) cacheHits++;
            
            totalInputTokens += inputTokens;
            totalOutputTokens += outputTokens;
            totalResponseTime += responseTimeMs;
            totalCost += cost;
            
            lastUpdated = LocalDateTime.now();
        }
        
        // Getters
        public int getTotalRequests() { return totalRequests; }
        public int getSuccessfulRequests() { return successfulRequests; }
        public int getCacheHits() { return cacheHits; }
        public long getTotalInputTokens() { return totalInputTokens; }
        public long getTotalOutputTokens() { return totalOutputTokens; }
        public long getTotalResponseTime() { return totalResponseTime; }
        public double getTotalCost() { return totalCost; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        
        public long getTotalTokens() { return totalInputTokens + totalOutputTokens; }
        public double getSuccessRate() { return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0; }
        public double getCacheHitRate() { return totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0; }
        public double getAverageResponseTime() { return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0.0; }
        public double getAverageCost() { return totalRequests > 0 ? totalCost / totalRequests : 0.0; }
    }
    
    /**
     * Usage statistics
     */
    public static class UsageStatistics {
        private int totalRequests = 0;
        private int successfulRequests = 0;
        private int cacheHits = 0;
        private long totalInputTokens = 0;
        private long totalOutputTokens = 0;
        private long totalTokens = 0;
        private long totalResponseTime = 0;
        private double totalCost = 0.0;
        private double averageResponseTime = 0.0;
        private double averageCost = 0.0;
        private double successRate = 0.0;
        private double cacheHitRate = 0.0;
        
        // Getters and setters
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }
        
        public int getCacheHits() { return cacheHits; }
        public void setCacheHits(int cacheHits) { this.cacheHits = cacheHits; }
        
        public long getTotalInputTokens() { return totalInputTokens; }
        public void setTotalInputTokens(long totalInputTokens) { this.totalInputTokens = totalInputTokens; }
        
        public long getTotalOutputTokens() { return totalOutputTokens; }
        public void setTotalOutputTokens(long totalOutputTokens) { this.totalOutputTokens = totalOutputTokens; }
        
        public long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
        
        public long getTotalResponseTime() { return totalResponseTime; }
        public void setTotalResponseTime(long totalResponseTime) { this.totalResponseTime = totalResponseTime; }
        
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public double getAverageCost() { return averageCost; }
        public void setAverageCost(double averageCost) { this.averageCost = averageCost; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
    }
    
    /**
     * Cost analysis
     */
    public static class CostAnalysis {
        private double totalCost = 0.0;
        private double averageCostPerRequest = 0.0;
        private int totalRequests = 0;
        private double costPerToken = 0.0;
        private Map<String, Double> costByModel = new HashMap<>();
        private Map<String, Double> dailyCosts = new HashMap<>();
        
        // Getters and setters
        public double getTotalCost() { return totalCost; }
        public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
        
        public double getAverageCostPerRequest() { return averageCostPerRequest; }
        public void setAverageCostPerRequest(double averageCostPerRequest) { this.averageCostPerRequest = averageCostPerRequest; }
        
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public double getCostPerToken() { return costPerToken; }
        public void setCostPerToken(double costPerToken) { this.costPerToken = costPerToken; }
        
        public Map<String, Double> getCostByModel() { return costByModel; }
        public void setCostByModel(Map<String, Double> costByModel) { this.costByModel = costByModel; }
        
        public Map<String, Double> getDailyCosts() { return dailyCosts; }
        public void setDailyCosts(Map<String, Double> dailyCosts) { this.dailyCosts = dailyCosts; }
    }
    
    /**
     * Performance metrics
     */
    public static class PerformanceMetrics {
        private double averageResponseTime = 0.0;
        private double successRate = 0.0;
        private int totalRequests = 0;
        private int successfulRequests = 0;
        private int p50ResponseTime = 0;
        private int p95ResponseTime = 0;
        private int p99ResponseTime = 0;
        
        // Getters and setters
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getSuccessfulRequests() { return successfulRequests; }
        public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }
        
        public int getP50ResponseTime() { return p50ResponseTime; }
        public void setP50ResponseTime(int p50ResponseTime) { this.p50ResponseTime = p50ResponseTime; }
        
        public int getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(int p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
        
        public int getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(int p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
    }
}

