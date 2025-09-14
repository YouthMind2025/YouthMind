package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModerationStatsResponse {
    private Long totalPending;
    private Long totalReviewed;
    private Long totalApproved;
    private Long totalRejected;
    private Long totalEscalated;
    private ContentTypeStats postStats;
    private ContentTypeStats commentStats;
    private ContentTypeStats userStats;
    private String timeframe;
    private LocalDateTime generatedAt;
}
