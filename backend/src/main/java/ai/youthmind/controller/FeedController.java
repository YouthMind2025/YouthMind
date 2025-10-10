package ai.youthmind.controller;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.FeedScrollResponse;
import ai.youthmind.dto.FeedStatusResponse;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import ai.youthmind.service.FeedBlockService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for feed-related endpoints in the recommendation engine.
 */
@Slf4j
@RestController
@RequestMapping("/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedBlockService feedBlockService;
    private final RecommendationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    // Rate limiting buckets per user
    private final Map<UUID, Bucket> userBuckets = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    // Metrics
    private final Counter feedBlocksRequested;
    private final Counter feedScrollEvents;
    private final Counter feedStatusRequests;
    private final Counter rateLimitHits;
    private final Timer feedResponseLatency;

    public FeedController(FeedBlockService feedBlockService,
                         RecommendationEventPublisher eventPublisher,
                         MeterRegistry meterRegistry) {
        this.feedBlockService = feedBlockService;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.feedBlocksRequested = Counter.builder("recommendation.feed.blocks_requested")
                .register(meterRegistry);
        this.feedScrollEvents = Counter.builder("recommendation.feed.scroll_events")
                .register(meterRegistry);
        this.feedStatusRequests = Counter.builder("recommendation.feed.status_requests")
                .register(meterRegistry);
        this.rateLimitHits = Counter.builder("recommendation.feed.rate_limit_hits")
                .register(meterRegistry);
        this.feedResponseLatency = Timer.builder("recommendation.feed.response_latency")
                .register(meterRegistry);
    }

    /**
     * Get next feed block for the authenticated user
     * GET /v1/feed/blocks?blockNumber=...
     */
    @GetMapping("/blocks")
    public ResponseEntity<FeedBlockResponse> getFeedBlocks(
            @RequestParam(value = "blockNumber", required = false) Integer blockNumber,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) Integer size) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(feedResponseLatency, () -> {
                    try {
                        UUID userId = getCurrentUserId();
                        
                        // Check rate limiting
                        if (!checkRateLimit(userId)) {
                            rateLimitHits.increment();
                            return ResponseEntity.status(429).body(null);
                        }

                        log.debug("Fetching feed block for user {} - block: {}, size: {}", userId, blockNumber, size);
                        
                        // Get feed block from service
                        FeedBlockResponse response = feedBlockService.getFeedBlock(userId, blockNumber, size);
                        
                        feedBlocksRequested.increment();
                        
                        return ResponseEntity.ok(response);
                        
                    } catch (Exception e) {
                        log.error("Failed to get feed blocks: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError().body(null);
                    }
                });
    }

    /**
     * Notify system of user scroll event
     * POST /v1/feed/scroll
     */
    @PostMapping("/scroll")
    public ResponseEntity<FeedScrollResponse> scrollFeed(
            @RequestParam(value = "currentBlock", required = false) Integer currentBlock) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(feedResponseLatency, () -> {
                    try {
                        UUID userId = getCurrentUserId();
                        
                        // Check rate limiting
                        if (!checkRateLimit(userId)) {
                            rateLimitHits.increment();
                            return ResponseEntity.status(429).body(null);
                        }

                        // Default to block 0 if not specified
                        if (currentBlock == null) {
                            currentBlock = 0;
                        }

                        log.debug("User {} scrolled to block {}", userId, currentBlock);
                        
                        // Publish scroll event
                        eventPublisher.publishUserScrolled(userId, currentBlock);
                        
                        // Check if next block is ready
                        boolean nextBlockReady = feedBlockService.isNextBlockReady(userId, currentBlock);
                        int estimatedWaitTime = nextBlockReady ? 0 : feedBlockService.getEstimatedWaitTime(userId);
                        
                        FeedScrollResponse response = new FeedScrollResponse(
                                nextBlockReady,
                                estimatedWaitTime,
                                currentBlock,
                                feedBlockService.isGeneratingBlocks(userId)
                        );
                        
                        feedScrollEvents.increment();
                        
                        return ResponseEntity.ok(response);
                        
                    } catch (Exception e) {
                        log.error("Failed to process scroll event: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError().body(null);
                    }
                });
    }

    /**
     * Get user's feed status
     * GET /v1/feed/status
     */
    @GetMapping("/status")
    public ResponseEntity<FeedStatusResponse> getFeedStatus() {
        return Timer.Sample.start(meterRegistry)
                .stop(feedResponseLatency, () -> {
                    try {
                        UUID userId = getCurrentUserId();
                        
                        // Check rate limiting
                        if (!checkRateLimit(userId)) {
                            rateLimitHits.increment();
                            return ResponseEntity.status(429).body(null);
                        }

                        log.debug("Getting feed status for user {}", userId);
                        
                        // Get feed status from service
                        FeedStatusResponse response = feedBlockService.getFeedStatus(userId);
                        
                        feedStatusRequests.increment();
                        
                        return ResponseEntity.ok(response);
                        
                    } catch (Exception e) {
                        log.error("Failed to get feed status: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError().body(null);
                    }
                });
    }

    /**
     * Get current user ID from security context
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        // TODO: Extract user ID from JWT token or authentication principal
        // For now, return a placeholder - this should be implemented based on your auth system
        String username = authentication.getName();
        log.debug("Getting user ID for username: {}", username);
        
        // This is a placeholder - implement actual user ID extraction
        return UUID.randomUUID(); // TODO: Replace with actual user ID extraction
    }

    /**
     * Check rate limiting for user
     */
    private boolean checkRateLimit(UUID userId) {
        Bucket bucket = userBuckets.computeIfAbsent(userId, this::createBucket);
        return bucket.tryConsume(1);
    }

    /**
     * Create rate limiting bucket for user
     */
    private Bucket createBucket(UUID userId) {
        Bandwidth limit = Bandwidth.classic(MAX_REQUESTS_PER_MINUTE, 
                Refill.intervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}
