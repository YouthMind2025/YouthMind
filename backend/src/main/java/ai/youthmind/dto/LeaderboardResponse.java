package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private List<LeaderboardPostDto> posts;
    private String metric;
    private String timeframe;
    private String emotion;
    private int limit;
}
