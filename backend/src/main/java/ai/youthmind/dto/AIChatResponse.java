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
public class AIChatResponse {
    
    private String messageId;
    private String response;
    private String personalityUsed;
    private String emotionDetected;
    private List<String> suggestedTopics;
    private Map<String, Object> metadata;
    private boolean safetyFiltered;
    private String safetyReason;
    private LocalDateTime timestamp;
    private int tokensUsed;
    private double confidence;
    private List<String> followUpQuestions;
    private String conversationContext;
    private boolean needsHumanReview;
    private String reviewReason;
}
