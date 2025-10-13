package ai.youthmind.controller;

import ai.youthmind.dto.RVectorHistoryResponse;
import ai.youthmind.dto.RVectorResetResponse;
import ai.youthmind.dto.RVectorResponse;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.service.RVectorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for R vector management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/v1/recommendation/rvector")
@RequiredArgsConstructor
public class RVectorController {

    private final RVectorService rVectorService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter rVectorRequests;
    private final Counter rVectorHistoryRequests;
    private final Counter rVectorResets;
    private final Timer rVectorResponseLatency;

    public RVectorController(RVectorService rVectorService, MeterRegistry meterRegistry) {
        this.rVectorService = rVectorService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.rVectorRequests = Counter.builder("recommendation.rvector.requests")
                .register(meterRegistry);
        this.rVectorHistoryRequests = Counter.builder("recommendation.rvector.history_requests")
                .register(meterRegistry);
        this.rVectorResets = Counter.builder("recommendation.rvector.resets")
                .register(meterRegistry);
        this.rVectorResponseLatency = Timer.builder("recommendation.rvector.response_latency")
                .register(meterRegistry);
    }

    /**
     * Get current R vector for the authenticated user
     * GET /v1/recommendation/rvector
     */
    @GetMapping
    public ResponseEntity<RVectorResponse> getRVector() {
        Timer.Sample sample = Timer.start();
        try {
            UUID userId = getCurrentUserId();
            
            log.debug("Getting R vector for user {}", userId);
            
            // Get R vector from service
            List<Double> rVector = rVectorService.getRVector(userId);
            List<String> topEmotions = rVectorService.getTopEmotions(userId, 3).stream()
                    .map(Emotion::getValue)
                    .toList();
            
            RVectorResponse response = new RVectorResponse(
                    userId,
                    rVector,
                    topEmotions,
                    LocalDateTime.now(), // TODO: Get actual last updated time
                    10, // TODO: Get actual interaction count
                    "v1.0", // TODO: Get actual version
                    0.8 // TODO: Get actual confidence score
            );
            
            rVectorRequests.increment();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get R vector: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        } finally {
            sample.stop(rVectorResponseLatency);
        }
    }

    /**
     * Get R vector history for the authenticated user
     * GET /v1/recommendation/rvector/history
     */
    @GetMapping("/history")
    public ResponseEntity<RVectorHistoryResponse> getRVectorHistory(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        
        Timer.Sample sample = Timer.start();
        try {
            UUID userId = getCurrentUserId();
            
            log.debug("Getting R vector history for user {} with limit {}", userId, limit);
            
            // TODO: Implement actual history retrieval
            RVectorHistoryResponse response = createPlaceholderHistory(userId, limit);
            
            rVectorHistoryRequests.increment();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get R vector history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        } finally {
            sample.stop(rVectorResponseLatency);
        }
    }

    /**
     * Reset R vector for the authenticated user
     * POST /v1/recommendation/rvector/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<RVectorResetResponse> resetRVector(
            @RequestParam(value = "reason", required = false) String reason) {
        
        Timer.Sample sample = Timer.start();
        try {
            UUID userId = getCurrentUserId();
            
            log.info("Resetting R vector for user {} with reason: {}", userId, reason);
            
            // Reset R vector
            rVectorService.resetRVector(userId);
            
            // Get new default R vector
            List<Double> newRVector = rVectorService.getRVector(userId);
            
            RVectorResetResponse response = new RVectorResetResponse(
                    userId,
                    true,
                    LocalDateTime.now(),
                    reason != null ? reason : "User requested reset",
                    newRVector
            );
            
            rVectorResets.increment();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to reset R vector: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        } finally {
            sample.stop(rVectorResponseLatency);
        }
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
     * Create placeholder R vector history response
     */
    private RVectorHistoryResponse createPlaceholderHistory(UUID userId, Integer limit) {
        // TODO: Implement actual history retrieval from database
        List<RVectorHistoryResponse.RVectorHistoryItem> history = List.of(
                new RVectorHistoryResponse.RVectorHistoryItem(
                        List.of(0.2, 0.1, 0.3, 0.1, 0.2, 0.1),
                        List.of("joy", "anger", "sadness"),
                        LocalDateTime.now().minusHours(1),
                        5,
                        "POST_CREATED",
                        0.7
                ),
                new RVectorHistoryResponse.RVectorHistoryItem(
                        List.of(0.3, 0.1, 0.2, 0.1, 0.2, 0.1),
                        List.of("joy", "sadness", "anger"),
                        LocalDateTime.now().minusMinutes(30),
                        8,
                        "POST_LIKED",
                        0.8
                )
        );
        
        return new RVectorHistoryResponse(userId, history);
    }
}
