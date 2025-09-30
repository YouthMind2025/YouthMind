package ai.youthmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Configuration properties for content moderation
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek.moderation")
@Validated
public class ModerationConfig {
    
    private boolean enabled = true;
    
    @DecimalMin(value = "0.0", message = "Moderation threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Moderation threshold must be between 0.0 and 1.0")
    private double threshold = 0.7;
    
    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
