package ai.youthmind.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled service for generating feed blocks for active users.
 * Runs every 30 seconds as a fallback when event-driven pre-calculation is backed up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "youthmind.recommendation.enabled", havingValue = "true", matchIfMissing = true)
public class FeedBlockScheduledGenerator {

    private final FeedBlockPreCalculationService feedBlockPreCalculationService;
    private final MeterRegistry meterRegistry;

    // Circuit breaker for preventing resource exhaustion
    private CircuitBreaker circuitBreaker;
    
    // Metrics
    private final Counter scheduledGenerations;
    private final Counter activeUsersProcessed;
    private final Counter blocksGenerated;
    private final Counter circuitBreakerTrips;
    private final Timer generationLatency;

    public FeedBlockScheduledGenerator(FeedBlockPreCalculationService feedBlockPreCalculationService,
                                     MeterRegistry meterRegistry) {
        this.feedBlockPreCalculationService = feedBlockPreCalculationService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.scheduledGenerations = Counter.builder("recommendation.scheduler.generations")
                .register(meterRegistry);
        this.activeUsersProcessed = Counter.builder("recommendation.scheduler.users_processed")
                .register(meterRegistry);
        this.blocksGenerated = Counter.builder("recommendation.scheduler.blocks_generated")
                .register(meterRegistry);
        this.circuitBreakerTrips = Counter.builder("recommendation.scheduler.circuit_breaker_trips")
                .register(meterRegistry);
        this.generationLatency = Timer.builder("recommendation.scheduler.generation_latency")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initializeCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Trip if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls to calculate failure rate
                .build();

        this.circuitBreaker = CircuitBreaker.of("feed-block-generator", config);
        
        // Add event handlers for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState());
                    circuitBreakerTrips.increment();
                });
    }

    /**
     * Scheduled task that runs every 30 seconds to generate blocks for active users
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void generateBlocksForActiveUsers() {
        scheduledGenerations.increment();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.debug("Starting scheduled block generation for active users");
            
            // Use circuit breaker to prevent resource exhaustion
            circuitBreaker.executeSupplier(() -> {
                processActiveUsers();
                return null;
            });
            
            log.debug("Completed scheduled block generation for active users");
            
        } catch (Exception e) {
            log.error("Scheduled block generation failed: {}", e.getMessage(), e);
        } finally {
            sample.stop(generationLatency);
        }
    }

    /**
     * Process active users and generate blocks for them
     */
    private void processActiveUsers() {
        try {
            // Get list of active users (scrolled in last 5 minutes)
            List<UUID> activeUsers = feedBlockPreCalculationService.getActiveUsers();
            
            if (activeUsers.isEmpty()) {
                log.debug("No active users found for block generation");
                return;
            }

            log.info("Processing {} active users for block generation", activeUsers.size());
            
            int processedCount = 0;
            int generatedBlocks = 0;
            
            for (UUID userId : activeUsers) {
                try {
                    // Check if user is still active
                    if (!feedBlockPreCalculationService.isUserActive(userId)) {
                        continue;
                    }
                    
                    // Generate blocks for this user
                    int blocksForUser = generateBlocksForUser(userId);
                    generatedBlocks += blocksForUser;
                    processedCount++;
                    
                    // Prevent overwhelming the system
                    if (processedCount >= 50) { // Limit to 50 users per run
                        log.debug("Reached user processing limit (50), stopping for this cycle");
                        break;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to generate blocks for user {}: {}", userId, e.getMessage());
                    // Continue with next user
                }
            }
            
            activeUsersProcessed.increment(processedCount);
            blocksGenerated.increment(generatedBlocks);
            
            log.info("Scheduled generation completed: {} users processed, {} blocks generated", 
                    processedCount, generatedBlocks);
            
        } catch (Exception e) {
            log.error("Failed to process active users: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger circuit breaker
        }
    }

    /**
     * Generate blocks for a specific user
     */
    private int generateBlocksForUser(UUID userId) {
        int generatedBlocks = 0;
        
        try {
            // Check if user has blocks that need generation
            // For now, we'll generate 2 blocks as a fallback
            // In a real implementation, this would check the user's current state
            
            // Generate next 2 blocks for active users
            feedBlockPreCalculationService.preCalculateBlocks(userId, 0, 2);
            generatedBlocks += 2;
            
            log.debug("Generated {} blocks for active user {}", generatedBlocks, userId);
            
        } catch (Exception e) {
            log.error("Failed to generate blocks for user {}: {}", userId, e.getMessage());
            throw e;
        }
        
        return generatedBlocks;
    }

    /**
     * Get circuit breaker state for monitoring
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Get circuit breaker metrics
     */
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }

    /**
     * Check if the scheduled generator is healthy
     */
    public boolean isHealthy() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    /**
     * Get processing statistics
     */
    public long getTotalGenerations() {
        return (long) scheduledGenerations.count();
    }

    /**
     * Get total users processed
     */
    public long getTotalUsersProcessed() {
        return (long) activeUsersProcessed.count();
    }

    /**
     * Get total blocks generated
     */
    public long getTotalBlocksGenerated() {
        return (long) blocksGenerated.count();
    }

    /**
     * Get circuit breaker trip count
     */
    public long getCircuitBreakerTripCount() {
        return (long) circuitBreakerTrips.count();
    }
}
