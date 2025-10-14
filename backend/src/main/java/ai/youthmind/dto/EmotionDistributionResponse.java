package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for platform-wide emotion distribution analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionDistributionResponse {
    
    /**
     * Distribution of user R vectors across all emotions
     */
    private Map<String, Double> overallEmotionDistribution;
    
    /**
     * Most common top-3 emotion combinations
     */
    private List<EmotionCombination> topEmotionCombinations;
    
    /**
     * Emotion trends over time
     */
    private List<EmotionTrend> emotionTrends;
    
    /**
     * User segments by dominant emotion
     */
    private Map<String, Integer> userSegments;
    
    /**
     * When this data was generated
     */
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionCombination {
        /**
         * Emotion combination (e.g., "joy,anger,sadness")
         */
        private String combination;
        
        /**
         * Number of users with this combination
         */
        private Integer userCount;
        
        /**
         * Percentage of total users
         */
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionTrend {
        /**
         * Emotion name
         */
        private String emotion;
        
        /**
         * Average value across all users
         */
        private Double averageValue;
        
        /**
         * Trend direction ("increasing", "decreasing", "stable")
         */
        private String trend;
        
        /**
         * Timestamp
         */
        private LocalDateTime timestamp;
    }
}
