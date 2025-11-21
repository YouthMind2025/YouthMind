package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing user's current R vector")
public class RVectorResponse {
    
    @Schema(description = "Current R vector version", example = "5")
    private Integer version;
    
    @Schema(description = "Emotion weights as map")
    private Map<String, BigDecimal> weights;
    
    @Schema(description = "Top 3 emotions by weight", example = "[\"happy\", \"surprised\", \"sad\"]")
    private List<String> topEmotions;
    
    @Schema(description = "Timestamp of last update")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Total weight sum", example = "0.6000")
    private BigDecimal totalWeight;
    
    @Schema(description = "User ID this vector belongs to")
    private java.util.UUID userId;
}

