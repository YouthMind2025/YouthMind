package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user R vector after interaction")
public class RVectorUpdateRequest {
    
    @NotNull(message = "Interaction type is required")
    @Schema(description = "Type of interaction: POST_CREATED, POST_VIEWED, POST_LIKED, POST_COLLECTED", 
            example = "POST_VIEWED",
            allowableValues = {"POST_CREATED", "POST_VIEWED", "POST_LIKED", "POST_COLLECTED"})
    private String interactionType;
    
    @NotNull(message = "Emotion is required")
    @Schema(description = "Emotion of the post involved in the interaction", 
            example = "happy",
            allowableValues = {"sad", "fear", "angry", "happy", "conflicted", "surprised"})
    private String emotion;
    
    @Schema(description = "Additional metadata (optional)")
    private Map<String, Object> metadata;
}

