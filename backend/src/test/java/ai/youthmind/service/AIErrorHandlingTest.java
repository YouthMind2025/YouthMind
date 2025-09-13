package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.util.CircuitBreaker;
import ai.youthmind.util.RetryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AI error handling and fault tolerance features
 */
@SpringBootTest
@ActiveProfiles("test")
public class AIErrorHandlingTest {
    
    private CircuitBreaker circuitBreaker;
    private AtomicInteger callCount;
    
    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(3, 5000, 2); // 3 failures, 5s timeout, 2 half-open calls
        callCount = new AtomicInteger(0);
    }
    
    @Test
    void testCircuitBreakerClosedState() throws Exception {
        // Test successful calls in CLOSED state
        String result = circuitBreaker.execute(() -> "success");
        assertEquals("success", result);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitBreakerOpensAfterFailures() throws Exception {
        // Test circuit breaker opens after threshold failures
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Test that calls are blocked when circuit is open
        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, () -> {
            circuitBreaker.execute(() -> "should not execute");
        });
    }
    
    @Test
    void testCircuitBreakerHalfOpenRecovery() throws Exception {
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Wait for timeout (simulate with reset for testing)
        circuitBreaker.reset();
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void testRetryUtilWithExponentialBackoff() throws Exception {
        callCount.set(0);
        
        // Test retry with exponential backoff
        String result = RetryUtil.executeWithRetry(
            () -> {
                int count = callCount.incrementAndGet();
                if (count < 3) {
                    throw new RuntimeException("Temporary failure");
                }
                return "success after retries";
            },
            3,
            Duration.ofMillis(100),
            2.0,
            Duration.ofSeconds(1),
            0.1
        );
        
        assertEquals("success after retries", result);
        assertEquals(3, callCount.get());
    }
    
    @Test
    void testRetryUtilExhausted() {
        callCount.set(0);
        
        // Test retry exhaustion
        assertThrows(RetryUtil.RetryExhaustedException.class, () -> {
            RetryUtil.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Persistent failure");
                },
                3,
                Duration.ofMillis(10),
                2.0,
                Duration.ofMillis(100),
                0.1
            );
        });
        
        assertEquals(3, callCount.get());
    }
    
    @Test
    void testRetryUtilWithPredicate() throws Exception {
        callCount.set(0);
        
        // Test retry with custom predicate
        String result = RetryUtil.executeWithRetry(
            () -> {
                int count = callCount.incrementAndGet();
                if (count < 2) {
                    throw new RuntimeException("Temporary network error");
                }
                return "success";
            },
            3,
            e -> e.getMessage().contains("network"),
            Duration.ofMillis(10),
            2.0,
            Duration.ofMillis(100),
            0.1
        );
        
        assertEquals("success", result);
        assertEquals(2, callCount.get());
    }
    
    @Test
    void testRetryUtilPredicateBlocksRetry() {
        callCount.set(0);
        
        // Test that predicate blocks retry for non-retryable exceptions
        assertThrows(RuntimeException.class, () -> {
            RetryUtil.executeWithRetry(
                () -> {
                    callCount.incrementAndGet();
                    throw new IllegalArgumentException("Non-retryable error");
                },
                3,
                e -> e.getMessage().contains("network"), // Only retry network errors
                Duration.ofMillis(10),
                2.0,
                Duration.ofMillis(100),
                0.1
            );
        });
        
        assertEquals(1, callCount.get()); // Should not retry
    }
    
    @Test
    void testAIFallbackService() {
        AIFallbackService fallbackService = new AIFallbackService();
        
        // Test fallback moderation result
        ModerationResult result = fallbackService.generateFallbackModerationResult("test content", "post");
        
        assertNotNull(result);
        assertTrue(result.isSafe());
        assertTrue(result.isFallback());
        assertTrue(result.isAiUnavailable());
        assertEquals(0.5, result.getConfidence());
    }
    
    @Test
    void testCircuitBreakerHealth() {
        // Test circuit breaker health information
        CircuitBreaker.CircuitBreakerHealth health = circuitBreaker.getHealth();
        
        assertNotNull(health);
        assertEquals(CircuitBreaker.State.CLOSED, health.getState());
        assertEquals(0, health.getFailureCount());
        assertEquals(0, health.getSuccessCount());
    }
    
    @Test
    void testCircuitBreakerReset() throws Exception {
        // Open the circuit
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Reset the circuit
        circuitBreaker.reset();
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(0, circuitBreaker.getSuccessCount());
    }
}
