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
public class AIChatSuggestionsResponse {
    
    private String messageId;
    private List<String> suggestions;
    private String suggestionType;
    private String personalityUsed;
    private String emotionDetected;
    private Map<String, Object> metadata;
    private boolean safetyFiltered;
    private String safetyReason;
    private LocalDateTime timestamp;
    private int tokensUsed;
    private double confidence;
    private String conversationContext;
    private boolean needsHumanReview;
    private String reviewReason;
}
