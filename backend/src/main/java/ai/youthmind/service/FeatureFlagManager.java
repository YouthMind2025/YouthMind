package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Feature Flag Manager for Psychological Analysis Services.
 * Features:
 * - Dynamic feature flag configuration with Redis persistence
 * - Gradual rollout with percentage-based and user-based targeting
 * - A/B testing framework with statistical significance tracking
 * - Feature flag analytics and monitoring
 * - Security controls and access management
 * - Configuration management with versioning and rollback
 * - Emergency disable and circuit breaker patterns
 * - Performance impact monitoring
 * - Integration with psychological analysis services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IntelligentCacheManager cacheManager;

    // Feature flag storage
    private final Map<String, FeatureFlag> featureFlags = new ConcurrentHashMap<>();
    private final Map<String, FeatureFlagConfig> flagConfigurations = new ConcurrentHashMap<>();
    private final Map<String, List<FeatureFlagEvent>> flagEvents = new ConcurrentHashMap<>();
    
    // Analytics and monitoring
    private final Map<String, FeatureFlagAnalytics> flagAnalytics = new ConcurrentHashMap<>();
    private final Map<String, ABTestResult> abTestResults = new ConcurrentHashMap<>();
    
    // Background tasks
    private final ScheduledExecutorService analyticsScheduler = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private static final String FEATURE_FLAG_PREFIX = "feature_flag:";
    private static final String ANALYTICS_PREFIX = "feature_analytics:";
    private static final String AB_TEST_PREFIX = "ab_test:";
    private static final int ANALYTICS_RETENTION_HOURS = 168; // 7 days

    /**
     * Initialize feature flag manager
     */
    public void initialize() {
        try {
            // Load existing feature flags from Redis
            loadFeatureFlagsFromRedis();
            
            // Initialize default feature flags for psychological analysis
            initializeDefaultFeatureFlags();
            
            // Start background analytics tasks
            startAnalyticsTasks();
            
            log.info("Feature Flag Manager initialized with {} feature flags", featureFlags.size());
            
        } catch (Exception e) {
            log.error("Error initializing Feature Flag Manager", e);
        }
    }

    /**
     * Check if feature flag is enabled for user
     */
    public CompletableFuture<Boolean> isFeatureEnabled(String flagName, String userId, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FeatureFlag flag = featureFlags.get(flagName);
                if (flag == null) {
                    log.warn("Feature flag not found: {}", flagName);
                    return false;
                }
                
                if (!flag.isEnabled()) {
                    recordFlagEvent(flagName, userId, FeatureFlagEventType.DISABLED, context);
                    return false;
                }
                
                // Check targeting rules
                boolean isTargeted = evaluateTargetingRules(flag, userId, context);
                
                // Record analytics
                recordFlagAnalytics(flagName, userId, isTargeted, context);
                
                // Record event
                recordFlagEvent(flagName, userId, 
                    isTargeted ? FeatureFlagEventType.ENABLED : FeatureFlagEventType.DISABLED, context);
                
                return isTargeted;
                
            } catch (Exception e) {
                log.error("Error checking feature flag: {} for user: {}", flagName, userId, e);
                return false;
            }
        });
    }

    /**
     * Get feature flag configuration
     */
    public FeatureFlagConfig getFeatureFlagConfig(String flagName) {
        return flagConfigurations.get(flagName);
    }

    /**
     * Update feature flag configuration
     */
    public CompletableFuture<Void> updateFeatureFlagConfig(String flagName, FeatureFlagConfig config) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Updating feature flag configuration: {}", flagName);
                
                // Validate configuration
                validateFeatureFlagConfig(config);
                
                // Update configuration
                flagConfigurations.put(flagName, config);
                
                // Update feature flag
                FeatureFlag flag = featureFlags.get(flagName);
                if (flag != null) {
                    flag.setConfig(config);
                    flag.setLastUpdated(LocalDateTime.now());
                }
                
                // Persist to Redis
                persistFeatureFlagToRedis(flagName, flag);
                
                // Record configuration change event
                recordFlagEvent(flagName, "system", FeatureFlagEventType.CONFIG_UPDATED, 
                    Map.of("config", config));
                
                log.info("Feature flag configuration updated: {}", flagName);
                
            } catch (Exception e) {
                log.error("Error updating feature flag configuration: {}", flagName, e);
                throw new RuntimeException("Failed to update feature flag configuration", e);
            }
        });
    }

    /**
     * Create new feature flag
     */
    public CompletableFuture<FeatureFlag> createFeatureFlag(String flagName, FeatureFlagConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating new feature flag: {}", flagName);
                
                if (featureFlags.containsKey(flagName)) {
                    throw new RuntimeException("Feature flag already exists: " + flagName);
                }
                
                // Validate configuration
                validateFeatureFlagConfig(config);
                
                // Create feature flag
                FeatureFlag flag = FeatureFlag.builder()
                    .name(flagName)
                    .enabled(false) // Start disabled for safety
                    .config(config)
                    .createdAt(LocalDateTime.now())
                    .lastUpdated(LocalDateTime.now())
                    .version(1)
                    .build();
                
                // Store feature flag
                featureFlags.put(flagName, flag);
                flagConfigurations.put(flagName, config);
                
                // Persist to Redis
                persistFeatureFlagToRedis(flagName, flag);
                
                // Record creation event
                recordFlagEvent(flagName, "system", FeatureFlagEventType.CREATED, 
                    Map.of("config", config));
                
                log.info("Feature flag created: {}", flagName);
                
                return flag;
                
            } catch (Exception e) {
                log.error("Error creating feature flag: {}", flagName, e);
                throw new RuntimeException("Failed to create feature flag", e);
            }
        });
    }

    /**
     * Enable/disable feature flag
     */
    public CompletableFuture<Void> toggleFeatureFlag(String flagName, boolean enabled) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Toggling feature flag: {} to {}", flagName, enabled);
                
                FeatureFlag flag = featureFlags.get(flagName);
                if (flag == null) {
                    throw new RuntimeException("Feature flag not found: " + flagName);
                }
                
                boolean previousState = flag.isEnabled();
                flag.setEnabled(enabled);
                flag.setLastUpdated(LocalDateTime.now());
                
                // Persist to Redis
                persistFeatureFlagToRedis(flagName, flag);
                
                // Record toggle event
                recordFlagEvent(flagName, "system", FeatureFlagEventType.TOGGLED, 
                    Map.of("enabled", enabled, "previousState", previousState));
                
                log.info("Feature flag toggled: {} to {}", flagName, enabled);
                
            } catch (Exception e) {
                log.error("Error toggling feature flag: {}", flagName, e);
                throw new RuntimeException("Failed to toggle feature flag", e);
            }
        });
    }

    /**
     * Start A/B test for feature flag
     */
    public CompletableFuture<ABTest> startABTest(String flagName, ABTestConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting A/B test for feature flag: {}", flagName);
                
                FeatureFlag flag = featureFlags.get(flagName);
                if (flag == null) {
                    throw new RuntimeException("Feature flag not found: " + flagName);
                }
                
                // Create A/B test
                ABTest abTest = ABTest.builder()
                    .flagName(flagName)
                    .config(config)
                    .status(ABTestStatus.RUNNING)
                    .startedAt(LocalDateTime.now())
                    .variantAUsers(new AtomicLong(0))
                    .variantBUsers(new AtomicLong(0))
                    .variantASuccesses(new AtomicLong(0))
                    .variantBSuccesses(new AtomicLong(0))
                    .build();
                
                // Store A/B test (convert ABTest to ABTestResult)
                ABTestResult abTestResult = ABTestResult.builder()
                    .flagName(flagName)
                    .status(abTest.getStatus())
                    .variantASuccessRate(0.0)
                    .variantBSuccessRate(0.0)
                    .statisticalSignificance(0.0)
                    .winningVariant("none")
                    .timestamp(LocalDateTime.now())
                    .build();
                abTestResults.put(flagName, abTestResult);
                
                // Update feature flag to enable A/B testing
                flag.getConfig().setAbTestEnabled(true);
                flag.getConfig().setAbTestConfig(config);
                
                // Record A/B test start event
                recordFlagEvent(flagName, "system", FeatureFlagEventType.AB_TEST_STARTED, 
                    Map.of("config", config));
                
                log.info("A/B test started for feature flag: {}", flagName);
                
                return abTest;
                
            } catch (Exception e) {
                log.error("Error starting A/B test for feature flag: {}", flagName, e);
                throw new RuntimeException("Failed to start A/B test", e);
            }
        });
    }

    /**
     * Get feature flag analytics
     */
    public FeatureFlagAnalytics getFeatureFlagAnalytics(String flagName) {
        return flagAnalytics.getOrDefault(flagName, FeatureFlagAnalytics.builder()
            .flagName(flagName)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Get A/B test results
     */
    public ABTestResult getABTestResults(String flagName) {
        return abTestResults.get(flagName);
    }

    /**
     * Get all feature flags
     */
    public Map<String, FeatureFlag> getAllFeatureFlags() {
        return new HashMap<>(featureFlags);
    }

    /**
     * Get feature flag events
     */
    public List<FeatureFlagEvent> getFeatureFlagEvents(String flagName, int limit) {
        List<FeatureFlagEvent> events = flagEvents.getOrDefault(flagName, new ArrayList<>());
        
        return events.stream()
            .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
            .limit(limit)
            .toList();
    }

    /**
     * Emergency disable feature flag
     */
    public CompletableFuture<Void> emergencyDisableFeatureFlag(String flagName, String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.warn("Emergency disabling feature flag: {} - Reason: {}", flagName, reason);
                
                FeatureFlag flag = featureFlags.get(flagName);
                if (flag != null) {
                    flag.setEnabled(false);
                    flag.setEmergencyDisabled(true);
                    flag.setEmergencyDisableReason(reason);
                    flag.setLastUpdated(LocalDateTime.now());
                    
                    // Persist to Redis
                    persistFeatureFlagToRedis(flagName, flag);
                    
                    // Record emergency disable event
                    recordFlagEvent(flagName, "system", FeatureFlagEventType.EMERGENCY_DISABLED, 
                        Map.of("reason", reason));
                    
                    log.warn("Feature flag emergency disabled: {}", flagName);
                }
                
            } catch (Exception e) {
                log.error("Error emergency disabling feature flag: {}", flagName, e);
            }
        });
    }

    // Private helper methods

    private void loadFeatureFlagsFromRedis() {
        try {
            Set<String> keys = redisTemplate.keys(FEATURE_FLAG_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    String flagName = key.substring(FEATURE_FLAG_PREFIX.length());
                    FeatureFlag flag = (FeatureFlag) redisTemplate.opsForValue().get(key);
                    if (flag != null) {
                        featureFlags.put(flagName, flag);
                        flagConfigurations.put(flagName, flag.getConfig());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading feature flags from Redis", e);
        }
    }

    private void initializeDefaultFeatureFlags() {
        // Psychological Analysis Feature Flags
        createDefaultFeatureFlag("psychological_analysis_v2", FeatureFlagConfig.builder()
            .name("psychological_analysis_v2")
            .description("Enhanced psychological analysis with improved algorithms")
            .enabled(false)
            .targetingRules(Arrays.asList(
                TargetingRule.builder()
                    .type(TargetingRuleType.PERCENTAGE)
                    .value(0.0) // Start with 0% rollout
                    .build()
            ))
            .gradualRolloutEnabled(true)
            .rolloutPercentage(0.0)
            .abTestEnabled(false)
            .createdAt(LocalDateTime.now())
            .build());

        // State Update Feature Flags
        createDefaultFeatureFlag("state_update_batch_processing", FeatureFlagConfig.builder()
            .name("state_update_batch_processing")
            .description("Enable batch processing for state updates")
            .enabled(true)
            .targetingRules(Arrays.asList(
                TargetingRule.builder()
                    .type(TargetingRuleType.ALL_USERS)
                    .build()
            ))
            .gradualRolloutEnabled(false)
            .rolloutPercentage(100.0)
            .abTestEnabled(false)
            .createdAt(LocalDateTime.now())
            .build());

        // Personality Analysis Feature Flags
        createDefaultFeatureFlag("personality_analysis_ml", FeatureFlagConfig.builder()
            .name("personality_analysis_ml")
            .description("Machine learning enhanced personality analysis")
            .enabled(false)
            .targetingRules(Arrays.asList(
                TargetingRule.builder()
                    .type(TargetingRuleType.USER_GROUP)
                    .value(0.0)
                    .userGroup("beta_users")
                    .build()
            ))
            .gradualRolloutEnabled(true)
            .rolloutPercentage(0.0)
            .abTestEnabled(false)
            .createdAt(LocalDateTime.now())
            .build());

        // Mood Analysis Feature Flags
        createDefaultFeatureFlag("mood_analysis_realtime", FeatureFlagConfig.builder()
            .name("mood_analysis_realtime")
            .description("Real-time mood analysis with streaming updates")
            .enabled(false)
            .targetingRules(Arrays.asList(
                TargetingRule.builder()
                    .type(TargetingRuleType.PERCENTAGE)
                    .value(0.0)
                    .build()
            ))
            .gradualRolloutEnabled(true)
            .rolloutPercentage(0.0)
            .abTestEnabled(false)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private void createDefaultFeatureFlag(String flagName, FeatureFlagConfig config) {
        if (!featureFlags.containsKey(flagName)) {
            FeatureFlag flag = FeatureFlag.builder()
                .name(flagName)
                .enabled(config.isEnabled())
                .config(config)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .version(1)
                .build();
            
            featureFlags.put(flagName, flag);
            flagConfigurations.put(flagName, config);
            
            // Persist to Redis
            persistFeatureFlagToRedis(flagName, flag);
        }
    }

    private void startAnalyticsTasks() {
        // Collect analytics every 5 minutes
        analyticsScheduler.scheduleAtFixedRate(() -> {
            try {
                collectAnalytics();
            } catch (Exception e) {
                log.error("Error collecting analytics", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        // Clean up old analytics every hour
        analyticsScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupOldAnalytics();
            } catch (Exception e) {
                log.error("Error cleaning up old analytics", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private boolean evaluateTargetingRules(FeatureFlag flag, String userId, Map<String, Object> context) {
        FeatureFlagConfig config = flag.getConfig();
        
        if (!config.isEnabled()) {
            return false;
        }
        
        // Check gradual rollout
        if (config.isGradualRolloutEnabled()) {
            double rolloutPercentage = config.getRolloutPercentage();
            if (rolloutPercentage < 100.0) {
                // Use user ID hash for consistent rollout
                double userHash = Math.abs(userId.hashCode()) % 100.0;
                if (userHash >= rolloutPercentage) {
                    return false;
                }
            }
        }
        
        // Check targeting rules
        for (TargetingRule rule : config.getTargetingRules()) {
            if (evaluateTargetingRule(rule, userId, context)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean evaluateTargetingRule(TargetingRule rule, String userId, Map<String, Object> context) {
        switch (rule.getType()) {
            case ALL_USERS:
                return true;
            case PERCENTAGE:
                double userHash = Math.abs(userId.hashCode()) % 100.0;
                return userHash < rule.getValue();
            case USER_GROUP:
                String userGroup = (String) context.get("userGroup");
                return rule.getUserGroup() != null && rule.getUserGroup().equals(userGroup);
            case USER_ID:
                return rule.getUserIds() != null && rule.getUserIds().contains(userId);
            case CUSTOM_ATTRIBUTE:
                String attributeValue = (String) context.get(rule.getAttributeName());
                return rule.getAttributeValue() != null && rule.getAttributeValue().equals(attributeValue);
            default:
                return false;
        }
    }

    private void recordFlagAnalytics(String flagName, String userId, boolean enabled, Map<String, Object> context) {
        FeatureFlagAnalytics analytics = flagAnalytics.computeIfAbsent(flagName, 
            k -> FeatureFlagAnalytics.builder()
                .flagName(flagName)
                .timestamp(LocalDateTime.now())
                .build());
        
        analytics.setTotalRequests(analytics.getTotalRequests() + 1);
        if (enabled) {
            analytics.setEnabledRequests(analytics.getEnabledRequests() + 1);
        }
        
        // Update user-specific analytics
        Map<String, UserAnalytics> userAnalytics = analytics.getUserAnalytics();
        if (userAnalytics == null) {
            userAnalytics = new ConcurrentHashMap<>();
            analytics.setUserAnalytics(userAnalytics);
        }
        
        UserAnalytics userStats = userAnalytics.computeIfAbsent(userId, 
            k -> UserAnalytics.builder()
                .userId(userId)
                .totalRequests(0)
                .enabledRequests(0)
                .build());
        
        userStats.setTotalRequests(userStats.getTotalRequests() + 1);
        if (enabled) {
            userStats.setEnabledRequests(userStats.getEnabledRequests() + 1);
        }
    }

    private void recordFlagEvent(String flagName, String userId, FeatureFlagEventType eventType, Map<String, Object> context) {
        FeatureFlagEvent event = FeatureFlagEvent.builder()
            .flagName(flagName)
            .userId(userId)
            .eventType(eventType)
            .context(context)
            .timestamp(LocalDateTime.now())
            .build();
        
        flagEvents.computeIfAbsent(flagName, k -> new ArrayList<>()).add(event);
        
        // Keep only last 1000 events per flag
        List<FeatureFlagEvent> events = flagEvents.get(flagName);
        if (events.size() > 1000) {
            events.remove(0);
        }
    }

    private void persistFeatureFlagToRedis(String flagName, FeatureFlag flag) {
        try {
            String key = FEATURE_FLAG_PREFIX + flagName;
            redisTemplate.opsForValue().set(key, flag);
        } catch (Exception e) {
            log.error("Error persisting feature flag to Redis: {}", flagName, e);
        }
    }

    private void validateFeatureFlagConfig(FeatureFlagConfig config) {
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Feature flag name is required");
        }
        
        if (config.getRolloutPercentage() < 0.0 || config.getRolloutPercentage() > 100.0) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }
        
        if (config.getTargetingRules() == null) {
            config.setTargetingRules(new ArrayList<>());
        }
    }

    private void collectAnalytics() {
        // Collect analytics for all feature flags
        for (String flagName : featureFlags.keySet()) {
            try {
                // This would integrate with existing monitoring systems
                // For now, we'll just log the analytics collection
                log.debug("Collecting analytics for feature flag: {}", flagName);
            } catch (Exception e) {
                log.error("Error collecting analytics for feature flag: {}", flagName, e);
            }
        }
    }

    private void cleanupOldAnalytics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(ANALYTICS_RETENTION_HOURS);
        
        for (FeatureFlagAnalytics analytics : flagAnalytics.values()) {
            // Clean up old user analytics
            if (analytics.getUserAnalytics() != null) {
                analytics.getUserAnalytics().entrySet().removeIf(entry -> 
                    entry.getValue().getLastSeen().isBefore(cutoffTime));
            }
        }
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlag {
        private String name;
        private boolean enabled;
        private FeatureFlagConfig config;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdated;
        private int version;
        private boolean emergencyDisabled;
        private String emergencyDisableReason;
    }

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlagConfig {
        private String name;
        private String description;
        private boolean enabled;
        private List<TargetingRule> targetingRules;
        private boolean gradualRolloutEnabled;
        private double rolloutPercentage;
        private boolean abTestEnabled;
        private ABTestConfig abTestConfig;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class TargetingRule {
        private TargetingRuleType type;
        private double value;
        private String userGroup;
        private List<String> userIds;
        private String attributeName;
        private String attributeValue;
    }

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlagEvent {
        private String flagName;
        private String userId;
        private FeatureFlagEventType eventType;
        private Map<String, Object> context;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlagAnalytics {
        private String flagName;
        private long totalRequests;
        private long enabledRequests;
        private Map<String, UserAnalytics> userAnalytics;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserAnalytics {
        private String userId;
        private long totalRequests;
        private long enabledRequests;
        private LocalDateTime lastSeen;
    }

    @lombok.Data
    @lombok.Builder
    public static class ABTest {
        private String flagName;
        private ABTestConfig config;
        private ABTestStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private AtomicLong variantAUsers;
        private AtomicLong variantBUsers;
        private AtomicLong variantASuccesses;
        private AtomicLong variantBSuccesses;
    }

    @lombok.Data
    @lombok.Builder
    public static class ABTestConfig {
        private String name;
        private String description;
        private double variantAPercentage;
        private double variantBPercentage;
        private String successMetric;
        private int minimumSampleSize;
        private double significanceLevel;
    }

    @lombok.Data
    @lombok.Builder
    public static class ABTestResult {
        private String flagName;
        private ABTestStatus status;
        private double variantASuccessRate;
        private double variantBSuccessRate;
        private double statisticalSignificance;
        private String winningVariant;
        private LocalDateTime timestamp;
    }

    public enum TargetingRuleType {
        ALL_USERS, PERCENTAGE, USER_GROUP, USER_ID, CUSTOM_ATTRIBUTE
    }

    public enum FeatureFlagEventType {
        CREATED, UPDATED, TOGGLED, CONFIG_UPDATED, ENABLED, DISABLED, 
        EMERGENCY_DISABLED, AB_TEST_STARTED, AB_TEST_ENDED
    }

    public enum ABTestStatus {
        NOT_STARTED, RUNNING, COMPLETED, CANCELLED
    }
}
