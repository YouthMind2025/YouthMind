package ai.youthmind.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validates admin secret key configuration on application startup
 */
@Component
public class AdminSecretValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminSecretValidator.class);
    
    @Autowired
    private AdminConfig adminConfig;
    
    @Autowired
    private Environment environment;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateAdminSecret() {
        String adminSecret = adminConfig.getSecretKey();
        String environmentProfile = environment.getActiveProfiles().length > 0 ? 
            String.join(",", environment.getActiveProfiles()) : "default";
        
        if (adminSecret == null || adminSecret.trim().isEmpty()) {
            logger.error("❌ ADMIN_SECRET_KEY is not configured! Admin registration will fail.");
            logger.error("Please set ADMIN_SECRET_KEY environment variable or configure it in application properties.");
            logger.error("Current active profile(s): {}", environmentProfile);
            throw new IllegalStateException("ADMIN_SECRET_KEY must be configured for admin functionality");
        }
        
        if (adminSecret.equals("YOUTHMIND_ADMIN_2024")) {
            if ("default".equals(environmentProfile) || "dev".equals(environmentProfile)) {
                logger.warn("⚠️  Using default admin secret key in {} environment. This is acceptable for development.", environmentProfile);
            } else {
                logger.error("❌ Using default admin secret key in {} environment! This is a security risk.", environmentProfile);
                logger.error("Please set a secure ADMIN_SECRET_KEY for production/staging environments.");
                throw new IllegalStateException("Default admin secret key cannot be used in production/staging environments");
            }
        }
        
        logger.info("✅ Admin secret key validation passed for profile(s): {}", environmentProfile);
        logger.debug("Admin secret key is configured (length: {})", adminSecret.length());
    }
}
