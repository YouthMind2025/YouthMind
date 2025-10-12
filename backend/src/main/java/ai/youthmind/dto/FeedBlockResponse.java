package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing feed block posts")
public class FeedBlockResponse {
    
    @Schema(description = "Block number", example = "1")
    private Integer blockNumber;
    
    @Schema(description = "List of posts in this block")
    private List<PostDto> posts;
    
    @Schema(description = "Emotion distribution used for this block")
    private Map<String, Integer> emotionDistribution;
    
    @Schema(description = "R vector version used", example = "3")
    private Integer rVectorVersion;
    
    @Schema(description = "Block generation/retrieval timestamp")
    private LocalDateTime timestamp;
    
    @Schema(description = "Whether this block was from cache or newly generated", example = "true")
    private Boolean fromCache;
    
    @Schema(description = "When this block expires")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Total number of posts in block", example = "20")
    private Integer totalPosts;
}

