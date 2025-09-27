package ai.youthmind.controller;

import ai.youthmind.service.FeatureFlagAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Feature Flag Analytics.
 * Provides endpoints for:
 * - Feature flag usage analytics
 * - A/B test results and statistical analysis
 * - User behavior analytics
 * - Conversion tracking
 * - Analytics dashboard
 */
@RestController
@RequestMapping("/v1/feature-flags/analytics")
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagAnalyticsController {

    private final FeatureFlagAnalyticsService analyticsService;

    /**
     * Record feature flag usage
     */
    @PostMapping("/usage")
    public ResponseEntity<String> recordFeatureFlagUsage(
            @RequestParam String flagName,
            @RequestParam String userId,
            @RequestParam boolean enabled,
            @RequestParam(required = false) Map<String, Object> context) {
        try {
            CompletableFuture<Void> recordFuture = analyticsService.recordFeatureFlagUsage(flagName, userId, enabled, context);
            recordFuture.get();
            
            return ResponseEntity.ok("Feature flag usage recorded successfully");
        } catch (Exception e) {
            log.error("Error recording feature flag usage for flag: {} user: {}", flagName, userId, e);
            return ResponseEntity.internalServerError().body("Error recording usage: " + e.getMessage());
        }
    }

    /**
     * Record conversion event
     */
    @PostMapping("/conversion")
    public ResponseEntity<String> recordConversion(
            @RequestParam String flagName,
            @RequestParam String userId,
            @RequestParam String conversionType,
            @RequestParam double value) {
        try {
            CompletableFuture<Void> recordFuture = analyticsService.recordConversion(flagName, userId, conversionType, value);
            recordFuture.get();
            
            return ResponseEntity.ok("Conversion recorded successfully");
        } catch (Exception e) {
            log.error("Error recording conversion for flag: {} user: {}", flagName, userId, e);
            return ResponseEntity.internalServerError().body("Error recording conversion: " + e.getMessage());
        }
    }

    /**
     * Get feature flag analytics
     */
    @GetMapping("/{flagName}")
    public ResponseEntity<FeatureFlagAnalyticsService.FeatureFlagAnalytics> getFeatureFlagAnalytics(@PathVariable String flagName) {
        try {
            FeatureFlagAnalyticsService.FeatureFlagAnalytics analytics = analyticsService.getFeatureFlagAnalytics(flagName);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting feature flag analytics: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get A/B test analytics
     */
    @GetMapping("/{flagName}/ab-test")
    public ResponseEntity<FeatureFlagAnalyticsService.ABTestAnalytics> getABTestAnalytics(@PathVariable String flagName) {
        try {
            FeatureFlagAnalyticsService.ABTestAnalytics analytics = analyticsService.getABTestAnalytics(flagName);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting A/B test analytics: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user behavior analytics
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<FeatureFlagAnalyticsService.UserBehaviorAnalytics> getUserBehaviorAnalytics(@PathVariable String userId) {
        try {
            FeatureFlagAnalyticsService.UserBehaviorAnalytics analytics = analyticsService.getUserBehaviorAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting user behavior analytics: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get comprehensive analytics dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FeatureFlagAnalyticsService.AnalyticsDashboard> getAnalyticsDashboard() {
        try {
            FeatureFlagAnalyticsService.AnalyticsDashboard dashboard = analyticsService.getAnalyticsDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error getting analytics dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Initialize analytics service
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeAnalyticsService() {
        try {
            analyticsService.initialize();
            return ResponseEntity.ok("Feature flag analytics service initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing analytics service", e);
            return ResponseEntity.internalServerError().body("Error initializing analytics service: " + e.getMessage());
        }
    }

    /**
     * Get analytics health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAnalyticsHealth() {
        try {
            FeatureFlagAnalyticsService.AnalyticsDashboard dashboard = analyticsService.getAnalyticsDashboard();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "totalFeatureFlags", dashboard.getTotalFeatureFlags(),
                "totalRequests", dashboard.getTotalRequests(),
                "totalConversions", dashboard.getTotalConversions(),
                "overallConversionRate", dashboard.getOverallConversionRate(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting analytics health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
