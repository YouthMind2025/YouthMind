package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendingPostDto {
    private UUID id;
    private String title;
    private String content;
    private String emotion;
    private int resonance;
    private int views;
    private int collects;
    private double trendingScore;
    private LocalDateTime createdAt;
}
