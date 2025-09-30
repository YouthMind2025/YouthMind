package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalityTraitsResponse {
    private List<String> postMostUsedWords;
    private List<String> topicPreferences;
    private List<String> coveredTopics;
    private List<String> mostEngagedTopic;
    private LocalDateTime start;
    private LocalDateTime end;
}
