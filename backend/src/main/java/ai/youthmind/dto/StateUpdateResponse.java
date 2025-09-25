package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for state update responses
 * Response after updating user psychological state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateUpdateResponse {
    
    private Boolean success;
    private Map<String, Object> newState; // Updated psychological state
    private Map<String, Object> changes; // What changed in the state
    private Map<String, Object> insights; // Insights from the update
    private Map<String, Object> metadata; // Additional metadata
    
    // Response metadata
    private UUID userId;
    private LocalDateTime updatedAt;
    private String updateType;
    private Double confidenceScore;
}
