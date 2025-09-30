package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for AI-powered content moderation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIModerationResponse {
    
    /**
     * Whether the content is safe to publish
     */
    private boolean isSafe;
    
    /**
     * Confidence score (0.0 to 1.0) for the moderation decision
     */
    private double confidence;
    
    /**
     * Reason for the moderation decision
     */
    private String reason;
    
    /**
     * Detected content categories
     */
    private String[] detectedCategories;
    
    /**
     * Severity level
     */
    private String severity;
    
    /**
     * Recommended action
     */
    private String recommendedAction;
    
    /**
     * Timestamp when moderation was performed
     */
    private LocalDateTime moderatedAt;
    
    /**
     * Cost of the API call (in USD)
     */
    private double cost;
    
    /**
     * Number of tokens used
     */
    private int tokensUsed;
    
    /**
     * Service status
     */
    private String serviceStatus;
    
    /**
     * Create response from ModerationResult
     */
    public static AIModerationResponse fromModerationResult(ModerationResult result, String serviceStatus) {
        AIModerationResponse response = new AIModerationResponse();
        response.setSafe(result.isSafe());
        response.setConfidence(result.getConfidence());
        response.setReason(result.getReason());
        response.setDetectedCategories(result.getDetectedCategories());
        response.setSeverity(result.getSeverity() != null ? result.getSeverity().toString() : "UNKNOWN");
        response.setRecommendedAction(determineRecommendedAction(result));
        response.setModeratedAt(result.getModeratedAt());
        response.setCost(result.getCost());
        response.setTokensUsed(result.getTokensUsed());
        response.setServiceStatus(serviceStatus);
        return response;
    }
    
    /**
     * Determine recommended action based on moderation result
     */
    private static String determineRecommendedAction(ModerationResult result) {
        if (result.isSafe()) {
            return "APPROVE";
        }
        
        switch (result.getSeverity()) {
            case CRITICAL:
                return "BLOCK_IMMEDIATELY";
            case HIGH:
                return "BLOCK_AND_REVIEW";
            case MEDIUM:
                return "FLAG_FOR_REVIEW";
            case LOW:
                return "MONITOR";
            default:
                return "REVIEW";
        }
    }
}
