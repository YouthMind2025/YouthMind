package ai.youthmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for the recommendation engine.
 * Maps to youthmind.recommendation.* properties in application.yml
 */
@Data
@Validated
@ConfigurationProperties(prefix = "youthmind.recommendation")
public class RecommendationEngineProperties {

    /**
     * Whether the recommendation engine is enabled
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Default size of feed blocks
     */
    @Positive
    private Integer defaultBlockSize = 20;

    /**
     * How long blocks expire in hours
     */
    @Positive
    private Integer blockExpiryHours = 1;

    /**
     * R vector cache TTL in minutes
     */
    @Positive
    private Integer rvectorCacheTtlMinutes = 30;

    /**
     * Maximum depth of event queue
     */
    @Positive
    private Integer eventQueueMaxDepth = 10000;

    /**
     * Whether block pre-calculation is enabled
     */
    @NotNull
    private Boolean blockPrecalcEnabled = true;

    /**
     * Whether to fallback to chronological feed
     */
    @NotNull
    private Boolean fallbackToChronological = true;

    /**
     * Minimum posts required per emotion
     */
    @Min(1)
    private Integer minPostsPerEmotion = 3;

    /**
     * Redis Streams configuration
     */
    @NestedConfigurationProperty
    private Streams streams = new Streams();

    /**
     * Rate limiting configuration
     */
    @NestedConfigurationProperty
    private RateLimiting rateLimiting = new RateLimiting();

    /**
     * Circuit breaker configuration
     */
    @NestedConfigurationProperty
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Performance targets for monitoring
     */
    @NestedConfigurationProperty
    private PerformanceTargets performanceTargets = new PerformanceTargets();

    /**
     * Cache configuration
     */
    @NestedConfigurationProperty
    private Cache cache = new Cache();

    @Data
    public static class Streams {
        /**
         * Name of the R vector updates stream
         */
        @NotNull
        private String rvectorUpdatesStream = "recommendation:rvector-updates";

        /**
         * Name of the block pre-calculation stream
         */
        @NotNull
        private String blockPrecalcStream = "recommendation:block-precalc";

        /**
         * Consumer group name
         */
        @NotNull
        private String consumerGroup = "recommendation-engine-workers";

        /**
         * Maximum retries for stream operations
         */
        @Positive
        private Integer maxRetries = 3;

        /**
         * Retry delay in milliseconds
         */
        @Positive
        private Long retryDelayMs = 1000L;
    }

    @Data
    public static class RateLimiting {
        /**
         * Maximum requests per minute
         */
        @Positive
        private Integer maxRequestsPerMinute = 100;

        /**
         * Burst capacity for rate limiting
         */
        @Positive
        private Integer burstCapacity = 10;
    }

    @Data
    public static class CircuitBreaker {
        /**
         * Failure rate threshold percentage
         */
        @Min(1)
        @Max(100)
        private Integer failureRateThreshold = 50;

        /**
         * Wait duration in seconds before retrying
         */
        @Positive
        private Integer waitDurationSeconds = 30;

        /**
         * Sliding window size for failure calculation
         */
        @Positive
        private Integer slidingWindowSize = 10;

        /**
         * Minimum number of calls before calculating failure rate
         */
        @Positive
        private Integer minimumCalls = 5;
    }

    @Data
    public static class PerformanceTargets {
        /**
         * Target R vector update time in milliseconds
         */
        @Positive
        private Integer rVectorUpdateMs = 5;

        /**
         * Target top emotion selection time in milliseconds
         */
        @Positive
        private Integer topEmotionSelectMs = 1;

        /**
         * Target single emotion selection time in milliseconds
         */
        @Positive
        private Integer singleEmotionSelectMs = 50;

        /**
         * Target full block generation time in milliseconds
         */
        @Positive
        private Integer fullBlockGenerateMs = 200;

        /**
         * Target event publishing time in milliseconds
         */
        @Positive
        private Integer eventPublishMs = 10;
    }

    @Data
    public static class Cache {
        /**
         * TTL for mapping rules cache in hours
         */
        @Positive
        private Integer mappingRulesTtlHours = 1;

        /**
         * TTL for R vector cache in minutes
         */
        @Positive
        private Integer rvectorTtlMinutes = 30;

        /**
         * TTL for feed blocks cache in hours
         */
        @Positive
        private Integer feedBlocksTtlHours = 1;

        /**
         * TTL for shown posts cache in days
         */
        @Positive
        private Integer shownPostsTtlDays = 7;

        /**
         * Maximum size of shown posts cache
         */
        @Positive
        private Integer shownPostsMaxSize = 500;
    }
}
