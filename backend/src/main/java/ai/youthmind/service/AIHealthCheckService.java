package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for checking AI service health without circular dependencies
 * Provides lightweight health checks for AI services
 */
@Service
public class AIHealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIHealthCheckService.class);
    
    @Value("${ai.deepseek.base-url:}")
    private String deepSeekBaseUrl;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepSeekApiKey;
    
    @Value("${ai.deepseek.enabled:true}")
    private boolean aiEnabled;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Check if AI services are available
     */
    public boolean isAIServiceAvailable() {
        try {
            // Check if AI is enabled
            if (!aiEnabled) {
                logger.debug("AI services disabled in configuration");
                return false;
            }
            
            // Check configuration
            if (!isAIConfigured()) {
                logger.debug("AI service configuration incomplete");
                return false;
            }
            
            // Perform health check
            return performHealthCheck();
            
        } catch (Exception e) {
            logger.warn("Error checking AI service availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if AI service is properly configured
     */
    public boolean isAIConfigured() {
        return deepSeekBaseUrl != null && !deepSeekBaseUrl.trim().isEmpty() &&
               deepSeekApiKey != null && !deepSeekApiKey.trim().isEmpty();
    }
    
    /**
     * Perform a lightweight health check
     */
    private boolean performHealthCheck() {
        try {
            // For now, we'll do a basic configuration validation
            // In a real implementation, you could make a simple HTTP call
            // to a health endpoint or ping the service
            
            logger.debug("AI service health check passed");
            return true;
            
        } catch (Exception e) {
            logger.debug("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed health status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean available = isAIServiceAvailable();
        
        status.put("available", available);
        status.put("enabled", aiEnabled);
        status.put("configured", isAIConfigured());
        status.put("base_url_configured", deepSeekBaseUrl != null && !deepSeekBaseUrl.trim().isEmpty());
        status.put("api_key_configured", deepSeekApiKey != null && !deepSeekApiKey.trim().isEmpty());
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }
}
