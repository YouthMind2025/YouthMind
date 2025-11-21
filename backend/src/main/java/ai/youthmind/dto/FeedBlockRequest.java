package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for feed block generation")
public class FeedBlockRequest {
    
    @Min(value = 1, message = "Block number must be at least 1")
    @Schema(description = "Block number to request (default: next available)", example = "1")
    private Integer blockNumber;
    
    @Min(value = 5, message = "Posts per block must be at least 5")
    @Max(value = 50, message = "Posts per block cannot exceed 50")
    @Schema(description = "Number of posts per block (default: 20)", example = "20")
    private Integer postsPerBlock = 20;
    
    @Schema(description = "Force regeneration even if cached block exists", example = "false")
    private Boolean forceRegenerate = false;
    
    @Schema(description = "Use recommendation engine (default: true)", example = "true")
    private Boolean useRecommendation = true;
}

