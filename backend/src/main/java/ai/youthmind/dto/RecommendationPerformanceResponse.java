package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for recommendation performance analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationPerformanceResponse {
    
    /**
     * Average block generation time in milliseconds
     */
    private Double avgBlockGenerationTimeMs;
    
    /**
     * Cache hit rate percentage (0-100)
     */
    private Double cacheHitRate;
    
    /**
     * Event processing rate (events per minute)
     */
    private Double eventProcessingRate;
    
    /**
     * R vector update success rate percentage (0-100)
     */
    private Double rVectorUpdateSuccessRate;
    
    /**
     * Consumer health status
     */
    private Map<String, Boolean> consumerHealth;
    
    /**
     * Stream metrics
     */
    private StreamMetrics streamMetrics;
    
    /**
     * Performance targets comparison
     */
    private Map<String, Boolean> performanceTargets;
    
    /**
     * When this data was generated
     */
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamMetrics {
        /**
         * R vector updates stream length
         */
        private Long rvectorUpdatesStreamLength;
        
        /**
         * Block precalc stream length
         */
        private Long blockPrecalcStreamLength;
        
        /**
         * Consumer lag in milliseconds
         */
        private Long consumerLagMs;
        
        /**
         * Dead letter queue size
         */
        private Long dlqSize;
    }
}
