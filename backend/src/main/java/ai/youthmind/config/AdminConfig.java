package ai.youthmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for admin-related settings
 */
@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {
    
    private String secretKey;
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
