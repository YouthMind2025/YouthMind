package ai.youthmind.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Retry utility with exponential backoff for AI service calls
 * Provides configurable retry logic with jitter to prevent thundering herd
 */
@Component
public class RetryUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryUtil.class);
    
    // Default configuration
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(1000);
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    private static final double DEFAULT_JITTER_FACTOR = 0.1;
    
    /**
     * Execute operation with retry logic
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        return executeWithRetry(operation, DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_DELAY, 
                              DEFAULT_BACKOFF_MULTIPLIER, DEFAULT_MAX_DELAY, DEFAULT_JITTER_FACTOR);
    }
    
    /**
     * Execute operation with custom retry configuration
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation, 
                                       int maxAttempts, 
                                       Duration initialDelay,
                                       double backoffMultiplier,
                                       Duration maxDelay,
                                       double jitterFactor) throws Exception {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("Executing operation - attempt {}/{}", attempt, maxAttempts);
                T result = operation.execute();
                
                if (attempt > 1) {
                    logger.info("Operation succeeded on attempt {}", attempt);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                
                // Check if we should retry
                if (!shouldRetry(e, attempt, maxAttempts)) {
                    logger.error("Operation failed and should not be retried: {}", e.getMessage());
                    throw e;
                }
                
                // Calculate delay for next attempt
                if (attempt < maxAttempts) {
                    Duration delay = calculateDelay(attempt, initialDelay, backoffMultiplier, maxDelay, jitterFactor);
                    logger.debug("Waiting {}ms before retry attempt {}", delay.toMillis(), attempt + 1);
                    
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted");
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        logger.error("Operation failed after {} attempts", maxAttempts);
        throw new RetryExhaustedException("Operation failed after " + maxAttempts + " attempts", lastException);
    }
    
    /**
     * Execute operation with custom retry predicate
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation, 
                                       int maxAttempts,
                                       Predicate<Exception> retryPredicate) throws Exception {
        return executeWithRetry(operation, maxAttempts, retryPredicate, DEFAULT_INITIAL_DELAY, 
                              DEFAULT_BACKOFF_MULTIPLIER, DEFAULT_MAX_DELAY, DEFAULT_JITTER_FACTOR);
    }
    
    /**
     * Execute operation with custom retry predicate and configuration
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation, 
                                       int maxAttempts,
                                       Predicate<Exception> retryPredicate,
                                       Duration initialDelay,
                                       double backoffMultiplier,
                                       Duration maxDelay,
                                       double jitterFactor) throws Exception {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("Executing operation - attempt {}/{}", attempt, maxAttempts);
                T result = operation.execute();
                
                if (attempt > 1) {
                    logger.info("Operation succeeded on attempt {}", attempt);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                
                // Check if we should retry based on predicate
                if (!retryPredicate.test(e) || attempt >= maxAttempts) {
                    logger.error("Operation failed and should not be retried: {}", e.getMessage());
                    throw e;
                }
                
                // Calculate delay for next attempt
                Duration delay = calculateDelay(attempt, initialDelay, backoffMultiplier, maxDelay, jitterFactor);
                logger.debug("Waiting {}ms before retry attempt {}", delay.toMillis(), attempt + 1);
                
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry interrupted");
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        logger.error("Operation failed after {} attempts", maxAttempts);
        throw new RetryExhaustedException("Operation failed after " + maxAttempts + " attempts", lastException);
    }
    
    /**
     * Determine if operation should be retried
     */
    private static boolean shouldRetry(Exception e, int attempt, int maxAttempts) {
        // Don't retry if we've reached max attempts
        if (attempt >= maxAttempts) {
            return false;
        }
        
        // Don't retry for certain types of exceptions
        if (e instanceof IllegalArgumentException || 
            e instanceof SecurityException ||
            e instanceof UnsupportedOperationException) {
            return false;
        }
        
        // Retry for network, timeout, and service errors
        return true;
    }
    
    /**
     * Calculate delay for next retry attempt with exponential backoff and jitter
     */
    private static Duration calculateDelay(int attempt, 
                                         Duration initialDelay, 
                                         double backoffMultiplier,
                                         Duration maxDelay,
                                         double jitterFactor) {
        
        // Calculate exponential backoff
        long delayMs = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        
        // Apply maximum delay limit
        delayMs = Math.min(delayMs, maxDelay.toMillis());
        
        // Add jitter to prevent thundering herd
        if (jitterFactor > 0) {
            double jitter = delayMs * jitterFactor;
            long jitterMs = (long) (ThreadLocalRandom.current().nextDouble(-jitter, jitter));
            delayMs = Math.max(0, delayMs + jitterMs);
        }
        
        return Duration.ofMillis(delayMs);
    }
    
    /**
     * Create a retry predicate for AI service exceptions
     */
    public static Predicate<Exception> createAIServiceRetryPredicate() {
        return e -> {
            // Retry for network issues, timeouts, and temporary service errors
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("network") ||
                   message.contains("temporary") ||
                   message.contains("service unavailable") ||
                   message.contains("rate limit") ||
                   e instanceof java.net.ConnectException ||
                   e instanceof java.net.SocketTimeoutException ||
                   e instanceof java.util.concurrent.TimeoutException;
        };
    }
    
    /**
     * Create a retry predicate for HTTP errors
     */
    public static Predicate<Exception> createHttpRetryPredicate() {
        return e -> {
            // Retry for 5xx server errors and 429 (rate limit)
            if (e.getMessage() != null) {
                String message = e.getMessage().toLowerCase();
                return message.contains("500") ||
                       message.contains("502") ||
                       message.contains("503") ||
                       message.contains("504") ||
                       message.contains("429");
            }
            return false;
        };
    }
    
    /**
     * Functional interface for retryable operations
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Exception thrown when all retry attempts are exhausted
     */
    public static class RetryExhaustedException extends RuntimeException {
        private final int attempts;
        
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
            this.attempts = 0;
        }
        
        public RetryExhaustedException(String message, Throwable cause, int attempts) {
            super(message, cause);
            this.attempts = attempts;
        }
        
        public int getAttempts() {
            return attempts;
        }
    }
}
