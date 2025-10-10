package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for feed block requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedBlockResponse {
    
    /**
     * List of posts in this block
     */
    private List<PostDto> posts;
    
    /**
     * Block metadata
     */
    private BlockMetadata blockMetadata;
    
    /**
     * Whether there are more blocks available
     */
    private Boolean hasMore;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockMetadata {
        /**
         * Block number
         */
        private Integer blockNumber;
        
        /**
         * User ID this block belongs to
         */
        private UUID userId;
        
        /**
         * When this block was generated
         */
        private LocalDateTime generatedAt;
        
        /**
         * R vector version used for this block
         */
        private String rVectorVersion;
        
        /**
         * Total number of posts in this block
         */
        private Integer totalPosts;
        
        /**
         * Emotion distribution in this block
         */
        private java.util.Map<String, Integer> emotionDistribution;
    }
}
