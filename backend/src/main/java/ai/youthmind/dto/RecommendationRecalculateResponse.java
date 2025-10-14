package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for recommendation recalculation requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRecalculateResponse {
    
    /**
     * Whether the recalculation was successful
     */
    private Boolean success;
    
    /**
     * Timestamp when recalculation started
     */
    private LocalDateTime startedAt;
    
    /**
     * Number of users processed
     */
    private Integer usersProcessed;
    
    /**
     * Number of users with updated R vectors
     */
    private Integer usersUpdated;
    
    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;
    
    /**
     * Summary of changes by emotion combination
     */
    private Map<String, Integer> changesByEmotion;
    
    /**
     * Any errors that occurred during processing
     */
    private String errorMessage;
}
