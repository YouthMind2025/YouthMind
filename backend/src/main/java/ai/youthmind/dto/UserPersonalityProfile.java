package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPersonalityProfile {
    
    private String userId;
    private String preferredPersonalityType; // "supportive", "professional", "casual", "empathetic"
    private String communicationStyle; // "formal", "informal", "direct", "gentle"
    private List<String> interests;
    private List<String> triggers; // Topics to be careful with
    private Map<String, Object> preferences;
    private String emotionalState; // Current emotional state
    private String preferredLanguage;
    private int conversationCount;
    private LocalDateTime lastUpdated;
    private Map<String, Integer> topicEngagement; // Topic -> engagement score
    private List<String> preferredResponseLength; // "short", "medium", "long"
    private boolean enableHumor;
    private boolean enableEmpathy;
    private String timezone;
    private String culturalContext;
}
