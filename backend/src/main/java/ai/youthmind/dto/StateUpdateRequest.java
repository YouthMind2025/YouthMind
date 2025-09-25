package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for state update requests
 * Used when updating user psychological state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateUpdateRequest {
    
    private UUID userId;
    private String updateType; // "content_analysis", "manual_update", "interaction_update"
    private String content; // Content that triggered the update
    private Map<String, Object> parameters; // Additional parameters for the update
    private Map<String, Object> metadata; // Additional metadata
    
    // Update context
    private String source; // "post", "comment", "message", "interaction"
    private LocalDateTime timestamp;
    private String sessionId;
}
