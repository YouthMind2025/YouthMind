package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AISafetyFilterResult {
    
    private boolean isSafe;
    private String reason;
    private List<String> flaggedTopics;
    private Map<String, Double> riskScores; // Topic -> risk score
    private String suggestedAlternative;
    private boolean needsHumanReview;
    private String reviewReason;
    private List<String> safetyRecommendations;
    private String originalMessage;
    private String filteredMessage;
    private int severityLevel; // 1-5, where 5 is most severe
    private String actionTaken; // "approved", "filtered", "blocked", "flagged"
    
    /**
     * Create a safe result
     */
    public static AISafetyFilterResult safe(String reason) {
        AISafetyFilterResult result = new AISafetyFilterResult();
        result.setSafe(true);
        result.setReason(reason);
        result.setSeverityLevel(0);
        result.setActionTaken("approved");
        result.setNeedsHumanReview(false);
        result.setSafetyRecommendations(java.util.Arrays.asList("Continue conversation normally"));
        return result;
    }
    
    /**
     * Create an unsafe result
     */
    public static AISafetyFilterResult unsafe(String reason, int severityLevel, String actionTaken, 
                                            List<String> flaggedTopics, List<String> safetyRecommendations, 
                                            double riskScore, boolean needsHumanReview) {
        AISafetyFilterResult result = new AISafetyFilterResult();
        result.setSafe(false);
        result.setReason(reason);
        result.setSeverityLevel(severityLevel);
        result.setActionTaken(actionTaken);
        result.setFlaggedTopics(flaggedTopics);
        result.setSafetyRecommendations(safetyRecommendations);
        result.setNeedsHumanReview(needsHumanReview);
        
        // Set risk scores
        Map<String, Double> riskScores = new java.util.HashMap<>();
        riskScores.put("overall", riskScore);
        result.setRiskScores(riskScores);
        
        return result;
    }
}
