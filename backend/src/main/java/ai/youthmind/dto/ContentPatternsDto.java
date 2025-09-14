package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentPatternsDto {
    private List<String> postMostUsedWords;
    private List<String> emotionalTrends;
    private List<String> topicPreferences;
    private List<String> coveredTopics;
}
