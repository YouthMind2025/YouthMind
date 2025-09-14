package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmotionalAnalysisResponse {
    private List<String> emotionalTrends;
    private Map<String, Integer> postEmotionDistribution;
    private LocalDateTime start;
    private LocalDateTime end;
}
