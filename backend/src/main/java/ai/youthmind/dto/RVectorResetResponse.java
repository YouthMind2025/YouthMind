package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for R vector reset requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RVectorResetResponse {
    
    /**
     * User ID whose R vector was reset
     */
    private UUID userId;
    
    /**
     * Whether the reset was successful
     */
    private Boolean success;
    
    /**
     * Timestamp when the reset occurred
     */
    private LocalDateTime resetAt;
    
    /**
     * Reason for the reset
     */
    private String reason;
    
    /**
     * New default R vector values
     */
    private java.util.List<Double> newRVector;
}
