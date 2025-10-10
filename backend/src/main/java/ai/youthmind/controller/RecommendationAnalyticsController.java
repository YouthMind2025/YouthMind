package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.EmotionDistributionResponse;
import ai.youthmind.dto.RecommendationPerformanceResponse;
import ai.youthmind.dto.UserRecommendationAnalyticsResponse;
import ai.youthmind.events.consumer.BlockPreCalculationConsumer;
import ai.youthmind.events.consumer.RVectorUpdateConsumer;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import ai.youthmind.service.FeedBlockService;
import ai.youthmind.service.RecommendationAnalyticsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for recommendation analytics endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/v1/analytics/recommendation")
@RequiredArgsConstructor
@Tag(name = "Recommendation Analytics", description = "Analytics endpoints for recommendation engine performance and insights")
@SecurityRequirement(name = "bearer-jwt")
public class RecommendationAnalyticsController {

    private final RecommendationAnalyticsService analyticsService;
    private final RecommendationEventPublisher eventPublisher;
    private final RVectorUpdateConsumer rVectorUpdateConsumer;
    private final BlockPreCalculationConsumer blockPreCalculationConsumer;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter performanceRequests;
    private final Counter userAnalyticsRequests;
    private final Counter emotionDistributionRequests;
    private final Timer analyticsResponseLatency;

    public RecommendationAnalyticsController(RecommendationAnalyticsService analyticsService,
                                           RecommendationEventPublisher eventPublisher,
                                           RVectorUpdateConsumer rVectorUpdateConsumer,
                                           BlockPreCalculationConsumer blockPreCalculationConsumer,
                                           MeterRegistry meterRegistry) {
        this.analyticsService = analyticsService;
        this.eventPublisher = eventPublisher;
        this.rVectorUpdateConsumer = rVectorUpdateConsumer;
        this.blockPreCalculationConsumer = blockPreCalculationConsumer;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.performanceRequests = Counter.builder("recommendation.analytics.performance_requests")
                .register(meterRegistry);
        this.userAnalyticsRequests = Counter.builder("recommendation.analytics.user_requests")
                .register(meterRegistry);
        this.emotionDistributionRequests = Counter.builder("recommendation.analytics.emotion_distribution_requests")
                .register(meterRegistry);
        this.analyticsResponseLatency = Timer.builder("recommendation.analytics.response_latency")
                .register(meterRegistry);
    }

    /**
     * Get recommendation system performance metrics
     * GET /v1/analytics/recommendation/performance
     */
    @GetMapping("/performance")
    @Operation(
        summary = "Get recommendation system performance metrics",
        description = "Retrieve comprehensive performance metrics for the recommendation engine"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<RecommendationPerformanceResponse>> getPerformanceMetrics() {
        return Timer.Sample.start(meterRegistry)
                .stop(analyticsResponseLatency, () -> {
                    try {
                        log.debug("Getting recommendation performance metrics");
                        
                        // Get performance metrics from service
                        RecommendationPerformanceResponse response = analyticsService.getPerformanceMetrics();
                        
                        // Add real-time consumer health status
                        Map<String, Boolean> consumerHealth = Map.of(
                                "rvector-consumer", rVectorUpdateConsumer.isHealthy(),
                                "block-precalc-consumer", blockPreCalculationConsumer.isHealthy()
                        );
                        response.setConsumerHealth(consumerHealth);
                        
                        // Add stream metrics
                        RecommendationPerformanceResponse.StreamMetrics streamMetrics = 
                                new RecommendationPerformanceResponse.StreamMetrics(
                                        eventPublisher.getStreamLength("recommendation:rvector-updates"),
                                        eventPublisher.getStreamLength("recommendation:block-precalc"),
                                        0L, // TODO: Calculate actual consumer lag
                                        0L  // TODO: Get DLQ size
                                );
                        response.setStreamMetrics(streamMetrics);
                        
                        performanceRequests.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<RecommendationPerformanceResponse>builder()
                                .success(true)
                                .data(response)
                                .message("Performance metrics retrieved successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to get performance metrics: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<RecommendationPerformanceResponse>builder()
                                        .success(false)
                                        .message("Failed to retrieve performance metrics")
                                        .build());
                    }
                });
    }

    /**
     * Get user-specific recommendation analytics
     * GET /v1/analytics/recommendation/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user-specific recommendation analytics",
        description = "Retrieve detailed analytics for a specific user's recommendation interactions"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - can only access own analytics or admin role required")
    })
    public ResponseEntity<ApiResponse<UserRecommendationAnalyticsResponse>> getUserAnalytics(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(analyticsResponseLatency, () -> {
                    try {
                        UUID currentUserId = getCurrentUserId();
                        
                        // Check if user can access this analytics (own data or admin)
                        if (!userId.equals(currentUserId) && !hasAdminRole()) {
                            return ResponseEntity.status(403)
                                    .body(ApiResponse.<UserRecommendationAnalyticsResponse>builder()
                                            .success(false)
                                            .message("Access denied - can only access own analytics")
                                            .build());
                        }
                        
                        log.debug("Getting analytics for user {}", userId);
                        
                        // Get user analytics from service
                        UserRecommendationAnalyticsResponse response = analyticsService.getUserAnalytics(userId);
                        
                        userAnalyticsRequests.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<UserRecommendationAnalyticsResponse>builder()
                                .success(true)
                                .data(response)
                                .message("User analytics retrieved successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to get user analytics: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<UserRecommendationAnalyticsResponse>builder()
                                        .success(false)
                                        .message("Failed to retrieve user analytics")
                                        .build());
                    }
                });
    }

    /**
     * Get platform-wide emotion distribution analytics
     * GET /v1/analytics/recommendation/emotion-distribution
     */
    @GetMapping("/emotion-distribution")
    @Operation(
        summary = "Get platform-wide emotion distribution",
        description = "Retrieve analytics about emotion distribution across all users on the platform"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Emotion distribution retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<EmotionDistributionResponse>> getEmotionDistribution() {
        return Timer.Sample.start(meterRegistry)
                .stop(analyticsResponseLatency, () -> {
                    try {
                        log.debug("Getting emotion distribution analytics");
                        
                        // Get emotion distribution from service
                        EmotionDistributionResponse response = analyticsService.getEmotionDistribution();
                        
                        emotionDistributionRequests.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<EmotionDistributionResponse>builder()
                                .success(true)
                                .data(response)
                                .message("Emotion distribution retrieved successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to get emotion distribution: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<EmotionDistributionResponse>builder()
                                        .success(false)
                                        .message("Failed to retrieve emotion distribution")
                                        .build());
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
     * Check if current user has admin role
     */
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // TODO: Implement actual role checking logic
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
