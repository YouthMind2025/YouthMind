package ai.youthmind.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable recommendation engine properties.
 */
@Configuration
@EnableConfigurationProperties(RecommendationEngineProperties.class)
public class RecommendationEngineConfig {
    // Configuration properties are automatically wired by Spring Boot
}
