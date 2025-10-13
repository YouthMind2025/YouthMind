package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for user-specific recommendation analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendationAnalyticsResponse {
    
    /**
     * User ID
     */
    private UUID userId;
    
    /**
     * R vector progression over time
     */
    private List<RVectorProgression> rVectorProgression;
    
    /**
     * Blocks consumed by user
     */
    private Integer blocksConsumed;
    
    /**
     * Engagement patterns
     */
    private EngagementPatterns engagementPatterns;
    
    /**
     * Emotion distribution in user's interactions
     */
    private Map<String, Integer> emotionDistribution;
    
    /**
     * User activity summary
     */
    private UserActivitySummary userActivitySummary;
    
    /**
     * When this data was generated
     */
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RVectorProgression {
        /**
         * R vector values at this point
         */
        private List<Double> rVector;
        
        /**
         * Top emotions at this point
         */
        private List<String> topEmotions;
        
        /**
         * Timestamp
         */
        private LocalDateTime timestamp;
        
        /**
         * Trigger event
         */
        private String triggerEvent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngagementPatterns {
        /**
         * Average time spent per block (seconds)
         */
        private Double avgTimePerBlock;
        
        /**
         * Most engaged time of day
         */
        private String peakEngagementHour;
        
        /**
         * Scroll patterns
         */
        private Map<String, Integer> scrollPatterns;
        
        /**
         * Like rate percentage
         */
        private Double likeRate;
        
        /**
         * View completion rate percentage
         */
        private Double viewCompletionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivitySummary {
        /**
         * Total interactions
         */
        private Integer totalInteractions;
        
        /**
         * Days active
         */
        private Integer daysActive;
        
        /**
         * Last activity timestamp
         */
        private LocalDateTime lastActivity;
        
        /**
         * Activity streak (days)
         */
        private Integer activityStreak;
    }
}
