package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for feed scroll requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedScrollResponse {
    
    /**
     * Whether the next block is ready for the user
     */
    private Boolean nextBlockReady;
    
    /**
     * Estimated time until next block is ready (in seconds)
     */
    private Integer estimatedWaitTime;
    
    /**
     * Current block number the user is on
     */
    private Integer currentBlock;
    
    /**
     * Whether the system is currently generating blocks
     */
    private Boolean isGenerating;
}
