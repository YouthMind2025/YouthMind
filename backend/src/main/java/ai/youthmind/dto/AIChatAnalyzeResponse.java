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
public class AIChatAnalyzeResponse {
    
    private String messageId;
    private String analysisResult;
    private String analysisType;
    private String personalityUsed;
    private String emotionDetected;
    private String sentimentScore;
    private String intentDetected;
    private List<String> keyTopics;
    private List<String> recommendations;
    private Map<String, Object> riskAssessment;
    private Map<String, Object> metadata;
    private boolean safetyFiltered;
    private String safetyReason;
    private LocalDateTime timestamp;
    private int tokensUsed;
    private double confidence;
    private String conversationContext;
    private boolean needsHumanReview;
    private String reviewReason;
    private Map<String, Object> emotionalInsights;
    private List<String> suggestedActions;
}
