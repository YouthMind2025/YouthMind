package ai.youthmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for DeepSeek API integration
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
@Validated
public class DeepSeekConfig {
    
    @NotBlank(message = "DeepSeek API base URL is required")
    private String baseUrl = "https://api.deepseek.com/v1";
    
    private String apiKey;
    
    @Positive(message = "Timeout must be positive")
    private int timeout = 30000; // 30 seconds
    
    @Positive(message = "Max retries must be positive")
    private int maxRetries = 3;
    
    @NotBlank(message = "Model name is required")
    private String model = "deepseek-chat";
    
    // Rate limiting configuration
    @Positive(message = "Rate limit must be positive")
    private int rateLimitPerMinute = 60;
    
    @Positive(message = "Rate limit burst must be positive")
    private int rateLimitBurst = 10;
    
    // Cost optimization settings
    @DecimalMin(value = "0.0", message = "Cache hit discount must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Cache hit discount must be between 0.0 and 1.0")
    private double cacheHitDiscount = 0.5; // 50% discount for cache hits
    
    private boolean enableCacheHitPricing = true;
    
    // Off-peak window configuration
    private boolean enableOffPeakRouting = true;
    private String offPeakStartTime = "22:00"; // 10 PM
    private String offPeakEndTime = "06:00"; // 6 AM
    private String offPeakTimezone = "UTC";
    
    // Model selection configuration
    private Map<String, ModelConfig> models = Map.of(
        "deepseek-chat", new ModelConfig("deepseek-chat", 0.14, 0.28, 0.7),
        "deepseek-reasoner", new ModelConfig("deepseek-reasoner", 0.55, 2.19, 0.8),
        "deepseek-coder", new ModelConfig("deepseek-coder", 0.14, 0.28, 0.6)
    );
    
    private String defaultModel = "deepseek-chat";
    private String fallbackModel = "deepseek-chat";
    
    // Authentication configuration
    private boolean enableApiKeyRotation = false;
    private List<String> backupApiKeys = List.of();
    private int apiKeyRotationIntervalHours = 24;
    
    // Monitoring and logging
    private boolean enableDetailedLogging = true;
    private boolean enableCostTracking = true;
    private boolean enablePerformanceMetrics = true;
    
    // Circuit breaker configuration
    private int circuitBreakerFailureThreshold = 5;
    private int circuitBreakerRecoveryTimeout = 30; // seconds
    private int circuitBreakerHalfOpenMaxCalls = 3;
    
    // Getters and setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    // Rate limiting getters and setters
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
    
    public int getRateLimitBurst() { return rateLimitBurst; }
    public void setRateLimitBurst(int rateLimitBurst) { this.rateLimitBurst = rateLimitBurst; }
    
    // Cost optimization getters and setters
    public double getCacheHitDiscount() { return cacheHitDiscount; }
    public void setCacheHitDiscount(double cacheHitDiscount) { this.cacheHitDiscount = cacheHitDiscount; }
    
    public boolean isEnableCacheHitPricing() { return enableCacheHitPricing; }
    public void setEnableCacheHitPricing(boolean enableCacheHitPricing) { this.enableCacheHitPricing = enableCacheHitPricing; }
    
    // Off-peak window getters and setters
    public boolean isEnableOffPeakRouting() { return enableOffPeakRouting; }
    public void setEnableOffPeakRouting(boolean enableOffPeakRouting) { this.enableOffPeakRouting = enableOffPeakRouting; }
    
    public String getOffPeakStartTime() { return offPeakStartTime; }
    public void setOffPeakStartTime(String offPeakStartTime) { this.offPeakStartTime = offPeakStartTime; }
    
    public String getOffPeakEndTime() { return offPeakEndTime; }
    public void setOffPeakEndTime(String offPeakEndTime) { this.offPeakEndTime = offPeakEndTime; }
    
    public String getOffPeakTimezone() { return offPeakTimezone; }
    public void setOffPeakTimezone(String offPeakTimezone) { this.offPeakTimezone = offPeakTimezone; }
    
    // Model selection getters and setters
    public Map<String, ModelConfig> getModels() { return models; }
    public void setModels(Map<String, ModelConfig> models) { this.models = models; }
    
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    
    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
    
    // Authentication getters and setters
    public boolean isEnableApiKeyRotation() { return enableApiKeyRotation; }
    public void setEnableApiKeyRotation(boolean enableApiKeyRotation) { this.enableApiKeyRotation = enableApiKeyRotation; }
    
    public List<String> getBackupApiKeys() { return backupApiKeys; }
    public void setBackupApiKeys(List<String> backupApiKeys) { this.backupApiKeys = backupApiKeys; }
    
    public int getApiKeyRotationIntervalHours() { return apiKeyRotationIntervalHours; }
    public void setApiKeyRotationIntervalHours(int apiKeyRotationIntervalHours) { this.apiKeyRotationIntervalHours = apiKeyRotationIntervalHours; }
    
    // Monitoring getters and setters
    public boolean isEnableDetailedLogging() { return enableDetailedLogging; }
    public void setEnableDetailedLogging(boolean enableDetailedLogging) { this.enableDetailedLogging = enableDetailedLogging; }
    
    public boolean isEnableCostTracking() { return enableCostTracking; }
    public void setEnableCostTracking(boolean enableCostTracking) { this.enableCostTracking = enableCostTracking; }
    
    public boolean isEnablePerformanceMetrics() { return enablePerformanceMetrics; }
    public void setEnablePerformanceMetrics(boolean enablePerformanceMetrics) { this.enablePerformanceMetrics = enablePerformanceMetrics; }
    
    // Circuit breaker getters and setters
    public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) { this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold; }
    
    public int getCircuitBreakerRecoveryTimeout() { return circuitBreakerRecoveryTimeout; }
    public void setCircuitBreakerRecoveryTimeout(int circuitBreakerRecoveryTimeout) { this.circuitBreakerRecoveryTimeout = circuitBreakerRecoveryTimeout; }
    
    public int getCircuitBreakerHalfOpenMaxCalls() { return circuitBreakerHalfOpenMaxCalls; }
    public void setCircuitBreakerHalfOpenMaxCalls(int circuitBreakerHalfOpenMaxCalls) { this.circuitBreakerHalfOpenMaxCalls = circuitBreakerHalfOpenMaxCalls; }
    
    /**
     * Check if the configuration is valid
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty() &&
               timeout > 0 && maxRetries > 0 &&
               model != null && !model.trim().isEmpty() &&
               rateLimitPerMinute > 0 && rateLimitBurst > 0 &&
               cacheHitDiscount >= 0.0 && cacheHitDiscount <= 1.0;
    }
    
    /**
     * Get masked API key for logging (shows only first 4 and last 4 characters)
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
    
    /**
     * Get model configuration for a specific model
     */
    public ModelConfig getModelConfig(String modelName) {
        return models.getOrDefault(modelName, models.get(defaultModel));
    }
    
    /**
     * Check if a model is available
     */
    public boolean isModelAvailable(String modelName) {
        return models.containsKey(modelName);
    }
    
    /**
     * Get all available model names
     */
    public List<String> getAvailableModels() {
        return List.copyOf(models.keySet());
    }
    
    /**
     * Model configuration class
     */
    public static class ModelConfig {
        private final String name;
        private final double inputCostPer1M; // Cost per 1M input tokens
        private final double outputCostPer1M; // Cost per 1M output tokens
        private final double performanceScore; // Performance score (0.0 to 1.0)
        
        public ModelConfig(String name, double inputCostPer1M, double outputCostPer1M, double performanceScore) {
            this.name = name;
            this.inputCostPer1M = inputCostPer1M;
            this.outputCostPer1M = outputCostPer1M;
            this.performanceScore = performanceScore;
        }
        
        // Getters
        public String getName() { return name; }
        public double getInputCostPer1M() { return inputCostPer1M; }
        public double getOutputCostPer1M() { return outputCostPer1M; }
        public double getPerformanceScore() { return performanceScore; }
        
        /**
         * Calculate cost for given token usage
         */
        public double calculateCost(int inputTokens, int outputTokens) {
            return (inputTokens / 1_000_000.0) * inputCostPer1M + 
                   (outputTokens / 1_000_000.0) * outputCostPer1M;
        }
    }
}
