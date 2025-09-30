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
 * Feature Flag Analytics Service.
 * Collects and analyzes feature flag usage and performance metrics.
 * Features:
 * - Real-time analytics collection
 * - Performance impact monitoring
 * - A/B test statistical analysis
 * - User behavior tracking
 * - Conversion rate analysis
 * - Statistical significance calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagAnalyticsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IntelligentCacheManager cacheManager;

    // Analytics storage
    private final Map<String, FeatureFlagMetrics> flagMetrics = new ConcurrentHashMap<>();
    private final Map<String, ABTestMetrics> abTestMetrics = new ConcurrentHashMap<>();
    private final Map<String, UserBehaviorMetrics> userBehaviorMetrics = new ConcurrentHashMap<>();
    
    // Background analytics processing
    private final ScheduledExecutorService analyticsScheduler = Executors.newScheduledThreadPool(3);
    
    // Configuration
    private static final String ANALYTICS_PREFIX = "feature_analytics:";
    private static final String AB_TEST_PREFIX = "ab_test_analytics:";
    private static final int METRICS_RETENTION_HOURS = 168; // 7 days

    /**
     * Initialize analytics service
     */
    public void initialize() {
        try {
            // Load existing analytics from Redis
            loadAnalyticsFromRedis();
            
            // Start background analytics processing
            startAnalyticsProcessing();
            
            log.info("Feature Flag Analytics Service initialized");
            
        } catch (Exception e) {
            log.error("Error initializing Feature Flag Analytics Service", e);
        }
    }

    /**
     * Record feature flag usage
     */
    public CompletableFuture<Void> recordFeatureFlagUsage(String flagName, String userId, boolean enabled, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Update flag metrics
                updateFlagMetrics(flagName, userId, enabled, context);
                
                // Update user behavior metrics
                updateUserBehaviorMetrics(userId, flagName, enabled, context);
                
                // Update A/B test metrics if applicable
                updateABTestMetrics(flagName, userId, enabled, context);
                
            } catch (Exception e) {
                log.error("Error recording feature flag usage for flag: {} user: {}", flagName, userId, e);
            }
        });
    }

    /**
     * Record conversion event
     */
    public CompletableFuture<Void> recordConversion(String flagName, String userId, String conversionType, double value) {
        return CompletableFuture.runAsync(() -> {
            try {
                FeatureFlagMetrics metrics = flagMetrics.get(flagName);
                if (metrics != null) {
                    metrics.setTotalConversions(metrics.getTotalConversions() + 1);
                    metrics.setTotalConversionValue(metrics.getTotalConversionValue() + value);
                    
                    // Update conversion rate
                    double conversionRate = (double) metrics.getTotalConversions() / metrics.getTotalRequests();
                    // Note: conversionRate is calculated dynamically, not stored
                    
                    // Update user-specific conversion metrics
                    Map<String, UserConversionMetrics> userConversions = metrics.getUserConversions();
                    if (userConversions == null) {
                        userConversions = new ConcurrentHashMap<>();
                        metrics.setUserConversions(userConversions);
                    }
                    
                    UserConversionMetrics userConversion = userConversions.computeIfAbsent(userId, 
                        k -> UserConversionMetrics.builder()
                            .userId(userId)
                            .totalConversions(0)
                            .totalConversionValue(0.0)
                            .build());
                    
                    userConversion.setTotalConversions(userConversion.getTotalConversions() + 1);
                    userConversion.setTotalConversionValue(userConversion.getTotalConversionValue() + value);
                }
                
            } catch (Exception e) {
                log.error("Error recording conversion for flag: {} user: {}", flagName, userId, e);
            }
        });
    }

    /**
     * Get feature flag analytics
     */
    public FeatureFlagAnalytics getFeatureFlagAnalytics(String flagName) {
        try {
            FeatureFlagMetrics metrics = flagMetrics.get(flagName);
            if (metrics == null) {
                return FeatureFlagAnalytics.builder()
                    .flagName(flagName)
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Calculate additional metrics
            double enableRate = metrics.getTotalRequests() > 0 ? 
                (double) metrics.getEnabledRequests() / metrics.getTotalRequests() : 0.0;
            
            double conversionRate = metrics.getTotalRequests() > 0 ? 
                (double) metrics.getTotalConversions() / metrics.getTotalRequests() : 0.0;
            
            return FeatureFlagAnalytics.builder()
                .flagName(flagName)
                .totalRequests(metrics.getTotalRequests())
                .enabledRequests(metrics.getEnabledRequests())
                .enableRate(enableRate)
                .totalConversions(metrics.getTotalConversions())
                .conversionRate(conversionRate)
                .totalConversionValue(metrics.getTotalConversionValue())
                .averageConversionValue(metrics.getTotalConversions() > 0 ? 
                    metrics.getTotalConversionValue() / metrics.getTotalConversions() : 0.0)
                .uniqueUsers(metrics.getUniqueUsers().size())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting feature flag analytics for: {}", flagName, e);
            return FeatureFlagAnalytics.builder()
                .flagName(flagName)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Get A/B test analytics
     */
    public ABTestAnalytics getABTestAnalytics(String flagName) {
        try {
            ABTestMetrics metrics = abTestMetrics.get(flagName);
            if (metrics == null) {
                return ABTestAnalytics.builder()
                    .flagName(flagName)
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Calculate statistical significance
            double significance = calculateStatisticalSignificance(
                metrics.getVariantAUsers().get(),
                metrics.getVariantASuccesses().get(),
                metrics.getVariantBUsers().get(),
                metrics.getVariantBSuccesses().get()
            );
            
            // Determine winning variant
            String winningVariant = determineWinningVariant(
                metrics.getVariantASuccesses().get(),
                metrics.getVariantAUsers().get(),
                metrics.getVariantBSuccesses().get(),
                metrics.getVariantBUsers().get()
            );
            
            return ABTestAnalytics.builder()
                .flagName(flagName)
                .variantAUsers(metrics.getVariantAUsers().get())
                .variantBUsers(metrics.getVariantBUsers().get())
                .variantASuccesses(metrics.getVariantASuccesses().get())
                .variantBSuccesses(metrics.getVariantBSuccesses().get())
                .variantASuccessRate(metrics.getVariantAUsers().get() > 0 ? 
                    (double) metrics.getVariantASuccesses().get() / metrics.getVariantAUsers().get() : 0.0)
                .variantBSuccessRate(metrics.getVariantBUsers().get() > 0 ? 
                    (double) metrics.getVariantBSuccesses().get() / metrics.getVariantBUsers().get() : 0.0)
                .statisticalSignificance(significance)
                .winningVariant(winningVariant)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting A/B test analytics for: {}", flagName, e);
            return ABTestAnalytics.builder()
                .flagName(flagName)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Get user behavior analytics
     */
    public UserBehaviorAnalytics getUserBehaviorAnalytics(String userId) {
        try {
            UserBehaviorMetrics metrics = userBehaviorMetrics.get(userId);
            if (metrics == null) {
                return UserBehaviorAnalytics.builder()
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            return UserBehaviorAnalytics.builder()
                .userId(userId)
                .totalFeatureFlagsUsed(metrics.getFeatureFlagsUsed().size())
                .enabledFeatureFlags(metrics.getEnabledFeatureFlags().size())
                .totalRequests(metrics.getTotalRequests())
                .totalConversions(metrics.getTotalConversions())
                .conversionRate(metrics.getTotalRequests() > 0 ? 
                    (double) metrics.getTotalConversions() / metrics.getTotalRequests() : 0.0)
                .lastSeen(metrics.getLastSeen())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting user behavior analytics for: {}", userId, e);
            return UserBehaviorAnalytics.builder()
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Get comprehensive analytics dashboard
     */
    public AnalyticsDashboard getAnalyticsDashboard() {
        try {
            Map<String, FeatureFlagAnalytics> flagAnalytics = new HashMap<>();
            Map<String, ABTestAnalytics> abTestAnalytics = new HashMap<>();
            
            // Collect analytics for all flags
            for (String flagName : flagMetrics.keySet()) {
                flagAnalytics.put(flagName, getFeatureFlagAnalytics(flagName));
            }
            
            // Collect A/B test analytics
            for (String flagName : abTestMetrics.keySet()) {
                abTestAnalytics.put(flagName, getABTestAnalytics(flagName));
            }
            
            // Calculate overall metrics
            long totalRequests = flagMetrics.values().stream()
                .mapToLong(FeatureFlagMetrics::getTotalRequests)
                .sum();
            
            long totalConversions = flagMetrics.values().stream()
                .mapToLong(FeatureFlagMetrics::getTotalConversions)
                .sum();
            
            double overallConversionRate = totalRequests > 0 ? 
                (double) totalConversions / totalRequests : 0.0;
            
            return AnalyticsDashboard.builder()
                .totalFeatureFlags(flagMetrics.size())
                .totalRequests(totalRequests)
                .totalConversions(totalConversions)
                .overallConversionRate(overallConversionRate)
                .flagAnalytics(flagAnalytics)
                .abTestAnalytics(abTestAnalytics)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting analytics dashboard", e);
            return AnalyticsDashboard.builder()
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    // Private helper methods

    private void loadAnalyticsFromRedis() {
        try {
            // Load flag metrics
            Set<String> flagKeys = redisTemplate.keys(ANALYTICS_PREFIX + "*");
            if (flagKeys != null) {
                for (String key : flagKeys) {
                    String flagName = key.substring(ANALYTICS_PREFIX.length());
                    FeatureFlagMetrics metrics = (FeatureFlagMetrics) redisTemplate.opsForValue().get(key);
                    if (metrics != null) {
                        flagMetrics.put(flagName, metrics);
                    }
                }
            }
            
            // Load A/B test metrics
            Set<String> abTestKeys = redisTemplate.keys(AB_TEST_PREFIX + "*");
            if (abTestKeys != null) {
                for (String key : abTestKeys) {
                    String flagName = key.substring(AB_TEST_PREFIX.length());
                    ABTestMetrics metrics = (ABTestMetrics) redisTemplate.opsForValue().get(key);
                    if (metrics != null) {
                        abTestMetrics.put(flagName, metrics);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error loading analytics from Redis", e);
        }
    }

    private void startAnalyticsProcessing() {
        // Process analytics every 5 minutes
        analyticsScheduler.scheduleAtFixedRate(() -> {
            try {
                processAnalytics();
            } catch (Exception e) {
                log.error("Error processing analytics", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        // Persist analytics to Redis every 10 minutes
        analyticsScheduler.scheduleAtFixedRate(() -> {
            try {
                persistAnalyticsToRedis();
            } catch (Exception e) {
                log.error("Error persisting analytics to Redis", e);
            }
        }, 0, 10, TimeUnit.MINUTES);
        
        // Clean up old analytics every hour
        analyticsScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupOldAnalytics();
            } catch (Exception e) {
                log.error("Error cleaning up old analytics", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private void updateFlagMetrics(String flagName, String userId, boolean enabled, Map<String, Object> context) {
        FeatureFlagMetrics metrics = flagMetrics.computeIfAbsent(flagName, 
            k -> FeatureFlagMetrics.builder()
                .flagName(flagName)
                .totalRequests(0)
                .enabledRequests(0)
                .totalConversions(0)
                .totalConversionValue(0.0)
                .uniqueUsers(new HashSet<>())
                .timestamp(LocalDateTime.now())
                .build());
        
        metrics.setTotalRequests(metrics.getTotalRequests() + 1);
        if (enabled) {
            metrics.setEnabledRequests(metrics.getEnabledRequests() + 1);
        }
        
        metrics.getUniqueUsers().add(userId);
        metrics.setLastUpdated(LocalDateTime.now());
    }

    private void updateUserBehaviorMetrics(String userId, String flagName, boolean enabled, Map<String, Object> context) {
        UserBehaviorMetrics metrics = userBehaviorMetrics.computeIfAbsent(userId, 
            k -> UserBehaviorMetrics.builder()
                .userId(userId)
                .totalRequests(0)
                .totalConversions(0)
                .featureFlagsUsed(new HashSet<>())
                .enabledFeatureFlags(new HashSet<>())
                .lastSeen(LocalDateTime.now())
                .build());
        
        metrics.setTotalRequests(metrics.getTotalRequests() + 1);
        metrics.getFeatureFlagsUsed().add(flagName);
        
        if (enabled) {
            metrics.getEnabledFeatureFlags().add(flagName);
        }
        
        metrics.setLastSeen(LocalDateTime.now());
    }

    private void updateABTestMetrics(String flagName, String userId, boolean enabled, Map<String, Object> context) {
        ABTestMetrics metrics = abTestMetrics.get(flagName);
        if (metrics != null) {
            // Determine variant based on user ID hash
            int userHash = Math.abs(userId.hashCode()) % 100;
            boolean isVariantA = userHash < 50;
            
            if (isVariantA) {
                metrics.getVariantAUsers().incrementAndGet();
                if (enabled) {
                    metrics.getVariantASuccesses().incrementAndGet();
                }
            } else {
                metrics.getVariantBUsers().incrementAndGet();
                if (enabled) {
                    metrics.getVariantBSuccesses().incrementAndGet();
                }
            }
        }
    }

    private double calculateStatisticalSignificance(long variantAUsers, long variantASuccesses, 
                                                   long variantBUsers, long variantBSuccesses) {
        if (variantAUsers == 0 || variantBUsers == 0) {
            return 0.0;
        }
        
        double rateA = (double) variantASuccesses / variantAUsers;
        double rateB = (double) variantBSuccesses / variantBUsers;
        
        // Simplified statistical significance calculation
        // In production, you would use proper statistical tests like chi-square or t-test
        double difference = Math.abs(rateA - rateB);
        double pooledRate = (double) (variantASuccesses + variantBSuccesses) / (variantAUsers + variantBUsers);
        double standardError = Math.sqrt(pooledRate * (1 - pooledRate) * (1.0/variantAUsers + 1.0/variantBUsers));
        
        if (standardError == 0) {
            return 0.0;
        }
        
        double zScore = difference / standardError;
        
        // Convert z-score to p-value (simplified)
        if (zScore > 2.58) return 0.99; // 99% confidence
        if (zScore > 1.96) return 0.95; // 95% confidence
        if (zScore > 1.65) return 0.90; // 90% confidence
        return 0.0;
    }

    private String determineWinningVariant(long variantASuccesses, long variantAUsers, 
                                         long variantBSuccesses, long variantBUsers) {
        if (variantAUsers == 0 || variantBUsers == 0) {
            return "insufficient_data";
        }
        
        double rateA = (double) variantASuccesses / variantAUsers;
        double rateB = (double) variantBSuccesses / variantBUsers;
        
        if (rateA > rateB) {
            return "variant_a";
        } else if (rateB > rateA) {
            return "variant_b";
        } else {
            return "tie";
        }
    }

    private void processAnalytics() {
        // Process and aggregate analytics data
        log.debug("Processing feature flag analytics");
    }

    private void persistAnalyticsToRedis() {
        try {
            // Persist flag metrics
            for (Map.Entry<String, FeatureFlagMetrics> entry : flagMetrics.entrySet()) {
                String key = ANALYTICS_PREFIX + entry.getKey();
                redisTemplate.opsForValue().set(key, entry.getValue());
            }
            
            // Persist A/B test metrics
            for (Map.Entry<String, ABTestMetrics> entry : abTestMetrics.entrySet()) {
                String key = AB_TEST_PREFIX + entry.getKey();
                redisTemplate.opsForValue().set(key, entry.getValue());
            }
            
        } catch (Exception e) {
            log.error("Error persisting analytics to Redis", e);
        }
    }

    private void cleanupOldAnalytics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(METRICS_RETENTION_HOURS);
        
        // Clean up old user behavior metrics
        userBehaviorMetrics.entrySet().removeIf(entry -> 
            entry.getValue().getLastSeen().isBefore(cutoffTime));
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlagMetrics {
        private String flagName;
        private long totalRequests;
        private long enabledRequests;
        private long totalConversions;
        private double totalConversionValue;
        private Set<String> uniqueUsers;
        private Map<String, UserConversionMetrics> userConversions;
        private LocalDateTime timestamp;
        private LocalDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserConversionMetrics {
        private String userId;
        private long totalConversions;
        private double totalConversionValue;
        private LocalDateTime lastConversion;
    }

    @lombok.Data
    @lombok.Builder
    public static class ABTestMetrics {
        private String flagName;
        private AtomicLong variantAUsers;
        private AtomicLong variantBUsers;
        private AtomicLong variantASuccesses;
        private AtomicLong variantBSuccesses;
        private LocalDateTime startedAt;
        private LocalDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserBehaviorMetrics {
        private String userId;
        private long totalRequests;
        private long totalConversions;
        private Set<String> featureFlagsUsed;
        private Set<String> enabledFeatureFlags;
        private LocalDateTime lastSeen;
    }

    @lombok.Data
    @lombok.Builder
    public static class FeatureFlagAnalytics {
        private String flagName;
        private long totalRequests;
        private long enabledRequests;
        private double enableRate;
        private long totalConversions;
        private double conversionRate;
        private double totalConversionValue;
        private double averageConversionValue;
        private int uniqueUsers;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class ABTestAnalytics {
        private String flagName;
        private long variantAUsers;
        private long variantBUsers;
        private long variantASuccesses;
        private long variantBSuccesses;
        private double variantASuccessRate;
        private double variantBSuccessRate;
        private double statisticalSignificance;
        private String winningVariant;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserBehaviorAnalytics {
        private String userId;
        private int totalFeatureFlagsUsed;
        private int enabledFeatureFlags;
        private long totalRequests;
        private long totalConversions;
        private double conversionRate;
        private LocalDateTime lastSeen;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsDashboard {
        private int totalFeatureFlags;
        private long totalRequests;
        private long totalConversions;
        private double overallConversionRate;
        private Map<String, FeatureFlagAnalytics> flagAnalytics;
        private Map<String, ABTestAnalytics> abTestAnalytics;
        private LocalDateTime timestamp;
    }
}
