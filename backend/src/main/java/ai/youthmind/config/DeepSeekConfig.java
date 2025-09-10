package ai.youthmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
    
    /**
     * Check if the configuration is valid
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty() &&
               timeout > 0 && maxRetries > 0 &&
               model != null && !model.trim().isEmpty();
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
}
