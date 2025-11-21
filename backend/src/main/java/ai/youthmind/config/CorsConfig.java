package ai.youthmind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Environment-specific CORS configuration for YouthMind Backend
 * 
 * This configuration provides different CORS settings based on the active profile:
 * - dev: Secure settings for development with explicit localhost origins
 * - staging: Restricted settings for staging environment
 * - prod: Strict production settings with domain allowlist
 * 
 * SECURITY: Never use wildcard origins (*) with credentials enabled.
 * This is a critical security vulnerability that allows external domains
 * to make authenticated requests.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.exposed-headers:Authorization,Content-Type,X-Total-Count}")
    private String exposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Development CORS configuration - secure with explicit localhost origins
     * SECURITY: Uses explicit localhost origins instead of wildcard to prevent
     * external domains from making authenticated requests.
     */
    @Bean("corsConfigurationSource")
    @Profile("dev")
    public CorsConfigurationSource devCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Use explicit localhost origins for development (SECURITY: no wildcard with credentials)
        List<String> devOrigins = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001"
        );
        configuration.setAllowedOrigins(devOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Staging CORS configuration - restricted but not as strict as production
     */
    @Bean("corsConfigurationSource")
    @Profile("staging")
    public CorsConfigurationSource stagingCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Staging-specific origins
        List<String> stagingOrigins = Arrays.asList(
            "https://staging.youthmind.ai",
            "https://staging-api.youthmind.ai",
            "http://localhost:3000",
            "http://localhost:3001"
        );
        configuration.setAllowedOrigins(stagingOrigins);
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", 
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L); // 30 minutes
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Production CORS configuration - strict security with domain allowlist
     */
    @Bean("corsConfigurationSource")
    @Profile("prod")
    public CorsConfigurationSource prodCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from environment variable or configuration
        List<String> origins = parseAllowedOrigins();
        if (origins.isEmpty()) {
            // Default production origins if none specified
            origins = Arrays.asList(
                "https://youthmind.ai",
                "https://www.youthmind.ai",
                "https://api.youthmind.ai"
            );
        }
        configuration.setAllowedOrigins(origins);
        
        // Strict method restrictions for production
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Specific headers only
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", 
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
            "X-API-Key", "X-Client-Version"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Total-Count", "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset", "X-Request-ID"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(600L); // 10 minutes for production
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Default CORS configuration (fallback)
     * SECURITY: Requires explicit origins when credentials are enabled.
     * Throws exception if wildcard origins are used with credentials.
     */
    @Bean("corsConfigurationSource")
    @Profile("!dev & !staging & !prod")
    public CorsConfigurationSource defaultCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse from configuration
        List<String> origins = parseAllowedOrigins();
        if (origins.isEmpty()) {
            // SECURITY: Cannot use wildcard with credentials enabled
            if (allowCredentials) {
                throw new IllegalStateException(
                    "CORS configuration error: Cannot use wildcard origins (*) when credentials are enabled. " +
                    "This is a critical security vulnerability. Please specify explicit origins in " +
                    "app.cors.allowed-origins configuration property."
                );
            }
            // Only allow wildcard if credentials are disabled
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setExposedHeaders(Arrays.asList(exposedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        
        // Validate configuration
        validateCorsConfiguration(configuration);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Parse allowed origins from configuration
     */
    private List<String> parseAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            return Arrays.asList();
        }
        
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * Validate CORS configuration to prevent security vulnerabilities
     * 
     * SECURITY: Throws IllegalStateException if wildcard origins are used with credentials enabled.
     * This is a critical security vulnerability that allows external domains to make authenticated requests.
     * 
     * @param configuration The CORS configuration to validate
     * @throws IllegalStateException if wildcard origins are used with credentials enabled
     */
    private void validateCorsConfiguration(CorsConfiguration configuration) {
        if (configuration.getAllowCredentials() != null && configuration.getAllowCredentials()) {
            // Check if wildcard patterns are used
            if (configuration.getAllowedOriginPatterns() != null && 
                !configuration.getAllowedOriginPatterns().isEmpty()) {
                boolean hasWildcard = configuration.getAllowedOriginPatterns().stream()
                    .anyMatch(pattern -> "*".equals(pattern) || "**".equals(pattern));
                
                if (hasWildcard) {
                    throw new IllegalStateException(
                        "CORS SECURITY VIOLATION: Cannot use wildcard origin patterns (* or **) " +
                        "when credentials are enabled. This allows external domains to make authenticated " +
                        "requests to your API. Please specify explicit origins instead of wildcards."
                    );
                }
            }
            
            // Check if wildcard origins are used (deprecated but still possible)
            if (configuration.getAllowedOrigins() != null && 
                !configuration.getAllowedOrigins().isEmpty()) {
                boolean hasWildcard = configuration.getAllowedOrigins().stream()
                    .anyMatch(origin -> "*".equals(origin));
                
                if (hasWildcard) {
                    throw new IllegalStateException(
                        "CORS SECURITY VIOLATION: Cannot use wildcard origin (*) " +
                        "when credentials are enabled. This allows external domains to make authenticated " +
                        "requests to your API. Please specify explicit origins instead of wildcards."
                    );
                }
            }
        }
    }
}