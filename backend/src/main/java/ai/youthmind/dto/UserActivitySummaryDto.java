package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivitySummaryDto {
    private ContentPatternsDto contentPatternsDto;
    private EngagementPatternsDto engagementPatternsDto;
    private ActivityPatternsDto activityPatternsDto;
}
