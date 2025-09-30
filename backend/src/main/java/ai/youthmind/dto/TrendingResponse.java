package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingResponse {
    private List<TrendingPostDto> posts;
    private String timeframe;
    private int limit;
}
