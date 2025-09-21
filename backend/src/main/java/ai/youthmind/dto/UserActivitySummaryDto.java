package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivitySummaryDto {
    private ContentPatternsDto contentPatternsDto;
    private EngagementPatternsDto engagementPatternsDto;
    private ActivityPatternsDto activityPatternsDto;
    private Pair<LocalDateTime, LocalDateTime> timeRange;
}
