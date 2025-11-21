package ai.youthmind.config;

import ai.youthmind.util.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for error handling and fault tolerance features
 */
@Configuration
public class ErrorHandlingConfig {
    
    /**
     * Circuit breaker for AI service calls
     */
    @Bean
    public CircuitBreaker aiServiceCircuitBreaker() {
        // Configuration: 5 failures, 60s timeout, 3 half-open calls
        return new CircuitBreaker(5, 60000, 3);
    }
    
    /**
     * Circuit breaker for moderation service calls
     */
    @Bean
    public CircuitBreaker moderationCircuitBreaker() {
        // Configuration: 3 failures, 30s timeout, 2 half-open calls
        return new CircuitBreaker(3, 30000, 2);
    }
    
    /**
     * Circuit breaker for chat service calls
     */
    @Bean
    public CircuitBreaker chatCircuitBreaker() {
        // Configuration: 4 failures, 45s timeout, 2 half-open calls
        return new CircuitBreaker(4, 45000, 2);
    }
}
