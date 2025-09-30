package ai.youthmind.controller;

import ai.youthmind.service.FeatureFlagManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Feature Flag Management.
 * Provides endpoints for:
 * - Feature flag creation and management
 * - Gradual rollout and A/B testing
 * - Analytics and monitoring
 * - Configuration management
 * - Emergency controls
 */
@RestController
@RequestMapping("/v1/feature-flags")
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagController {

    private final FeatureFlagManager featureFlagManager;

    /**
     * Check if feature flag is enabled for user
     */
    @GetMapping("/{flagName}/enabled")
    public ResponseEntity<Map<String, Object>> isFeatureEnabled(
            @PathVariable String flagName,
            @RequestParam String userId,
            @RequestParam(required = false) Map<String, Object> context) {
        try {
            CompletableFuture<Boolean> enabledFuture = featureFlagManager.isFeatureEnabled(flagName, userId, context);
            Boolean enabled = enabledFuture.get();
            
            Map<String, Object> response = Map.of(
                "flagName", flagName,
                "userId", userId,
                "enabled", enabled,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking feature flag: {} for user: {}", flagName, userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get feature flag configuration
     */
    @GetMapping("/{flagName}/config")
    public ResponseEntity<FeatureFlagManager.FeatureFlagConfig> getFeatureFlagConfig(@PathVariable String flagName) {
        try {
            FeatureFlagManager.FeatureFlagConfig config = featureFlagManager.getFeatureFlagConfig(flagName);
            if (config != null) {
                return ResponseEntity.ok(config);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting feature flag configuration: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update feature flag configuration
     */
    @PutMapping("/{flagName}/config")
    public ResponseEntity<String> updateFeatureFlagConfig(
            @PathVariable String flagName,
            @RequestBody FeatureFlagManager.FeatureFlagConfig config) {
        try {
            config.setName(flagName);
            CompletableFuture<Void> updateFuture = featureFlagManager.updateFeatureFlagConfig(flagName, config);
            updateFuture.get();
            
            return ResponseEntity.ok("Feature flag configuration updated successfully");
        } catch (Exception e) {
            log.error("Error updating feature flag configuration: {}", flagName, e);
            return ResponseEntity.internalServerError().body("Error updating configuration: " + e.getMessage());
        }
    }

    /**
     * Create new feature flag
     */
    @PostMapping("/{flagName}")
    public ResponseEntity<FeatureFlagManager.FeatureFlag> createFeatureFlag(
            @PathVariable String flagName,
            @RequestBody FeatureFlagManager.FeatureFlagConfig config) {
        try {
            config.setName(flagName);
            CompletableFuture<FeatureFlagManager.FeatureFlag> createFuture = featureFlagManager.createFeatureFlag(flagName, config);
            FeatureFlagManager.FeatureFlag flag = createFuture.get();
            
            return ResponseEntity.ok(flag);
        } catch (Exception e) {
            log.error("Error creating feature flag: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Toggle feature flag (enable/disable)
     */
    @PostMapping("/{flagName}/toggle")
    public ResponseEntity<String> toggleFeatureFlag(
            @PathVariable String flagName,
            @RequestParam boolean enabled) {
        try {
            CompletableFuture<Void> toggleFuture = featureFlagManager.toggleFeatureFlag(flagName, enabled);
            toggleFuture.get();
            
            return ResponseEntity.ok("Feature flag toggled successfully");
        } catch (Exception e) {
            log.error("Error toggling feature flag: {}", flagName, e);
            return ResponseEntity.internalServerError().body("Error toggling feature flag: " + e.getMessage());
        }
    }

    /**
     * Start A/B test for feature flag
     */
    @PostMapping("/{flagName}/ab-test")
    public ResponseEntity<FeatureFlagManager.ABTest> startABTest(
            @PathVariable String flagName,
            @RequestBody FeatureFlagManager.ABTestConfig config) {
        try {
            CompletableFuture<FeatureFlagManager.ABTest> abTestFuture = featureFlagManager.startABTest(flagName, config);
            FeatureFlagManager.ABTest abTest = abTestFuture.get();
            
            return ResponseEntity.ok(abTest);
        } catch (Exception e) {
            log.error("Error starting A/B test for feature flag: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get feature flag analytics
     */
    @GetMapping("/{flagName}/analytics")
    public ResponseEntity<FeatureFlagManager.FeatureFlagAnalytics> getFeatureFlagAnalytics(@PathVariable String flagName) {
        try {
            FeatureFlagManager.FeatureFlagAnalytics analytics = featureFlagManager.getFeatureFlagAnalytics(flagName);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting feature flag analytics: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get A/B test results
     */
    @GetMapping("/{flagName}/ab-test/results")
    public ResponseEntity<FeatureFlagManager.ABTestResult> getABTestResults(@PathVariable String flagName) {
        try {
            FeatureFlagManager.ABTestResult results = featureFlagManager.getABTestResults(flagName);
            if (results != null) {
                return ResponseEntity.ok(results);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting A/B test results for feature flag: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all feature flags
     */
    @GetMapping
    public ResponseEntity<Map<String, FeatureFlagManager.FeatureFlag>> getAllFeatureFlags() {
        try {
            Map<String, FeatureFlagManager.FeatureFlag> flags = featureFlagManager.getAllFeatureFlags();
            return ResponseEntity.ok(flags);
        } catch (Exception e) {
            log.error("Error getting all feature flags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get feature flag events
     */
    @GetMapping("/{flagName}/events")
    public ResponseEntity<List<FeatureFlagManager.FeatureFlagEvent>> getFeatureFlagEvents(
            @PathVariable String flagName,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FeatureFlagManager.FeatureFlagEvent> events = featureFlagManager.getFeatureFlagEvents(flagName, limit);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error getting feature flag events: {}", flagName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Emergency disable feature flag
     */
    @PostMapping("/{flagName}/emergency-disable")
    public ResponseEntity<String> emergencyDisableFeatureFlag(
            @PathVariable String flagName,
            @RequestParam String reason) {
        try {
            CompletableFuture<Void> disableFuture = featureFlagManager.emergencyDisableFeatureFlag(flagName, reason);
            disableFuture.get();
            
            return ResponseEntity.ok("Feature flag emergency disabled successfully");
        } catch (Exception e) {
            log.error("Error emergency disabling feature flag: {}", flagName, e);
            return ResponseEntity.internalServerError().body("Error emergency disabling feature flag: " + e.getMessage());
        }
    }

    /**
     * Initialize feature flag manager
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeFeatureFlagManager() {
        try {
            featureFlagManager.initialize();
            return ResponseEntity.ok("Feature flag manager initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing feature flag manager", e);
            return ResponseEntity.internalServerError().body("Error initializing feature flag manager: " + e.getMessage());
        }
    }

    /**
     * Get feature flag health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getFeatureFlagHealth() {
        try {
            Map<String, FeatureFlagManager.FeatureFlag> flags = featureFlagManager.getAllFeatureFlags();
            
            long totalFlags = flags.size();
            long enabledFlags = flags.values().stream()
                .mapToLong(flag -> flag.isEnabled() ? 1 : 0)
                .sum();
            long emergencyDisabledFlags = flags.values().stream()
                .mapToLong(flag -> flag.isEmergencyDisabled() ? 1 : 0)
                .sum();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "totalFlags", totalFlags,
                "enabledFlags", enabledFlags,
                "emergencyDisabledFlags", emergencyDisabledFlags,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting feature flag health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
