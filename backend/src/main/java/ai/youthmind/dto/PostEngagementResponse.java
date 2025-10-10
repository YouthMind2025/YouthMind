package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostEngagementResponse {
    private UUID postId;
    private Integer resonance;
    private Integer collects;
    private Integer views;
    private boolean isResonanced;
    private boolean isCollected;
}
