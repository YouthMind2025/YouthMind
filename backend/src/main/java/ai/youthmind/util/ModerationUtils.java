package ai.youthmind.util;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.WarningHistory;

/**
 * Utility class for moderation-related operations.
 * Centralizes common moderation logic to avoid code duplication.
 */
public class ModerationUtils {
    
    /**
     * Get the primary category from detected categories array.
     * 
     * @param result The moderation result
     * @return The primary detected category or "OTHER" if none
     */
    public static String getPrimaryCategory(ModerationResult result) {
        if (result.getDetectedCategories() == null || result.getDetectedCategories().length == 0) {
            return "OTHER";
        }
        return result.getDetectedCategories()[0]; // Return first detected category
    }
    
    /**
     * Map moderation category to warning type for logging and warnings.
     * 
     * @param category The detected category from AI moderation
     * @return The corresponding warning type
     */
    public static WarningHistory.WarningType mapCategoryToWarningType(String category) {
        if (category == null) {
            return WarningHistory.WarningType.OTHER;
        }
        
        switch (category.toUpperCase()) {
            case "HARASSMENT":
            case "BULLYING":
                return WarningHistory.WarningType.HARASSMENT;
            case "HATE_SPEECH":
            case "DISCRIMINATION":
                return WarningHistory.WarningType.HATE_SPEECH;
            case "VIOLENCE":
            case "THREATS":
                return WarningHistory.WarningType.VIOLENCE;
            case "SPAM":
                return WarningHistory.WarningType.SPAM;
            case "INAPPROPRIATE":
            case "EXPLICIT":
                return WarningHistory.WarningType.INAPPROPRIATE_CONTENT;
            default:
                return WarningHistory.WarningType.OTHER;
        }
    }
    
    /**
     * Map confidence score to severity level.
     * 
     * @param confidence The AI confidence score (0.0 to 1.0)
     * @return The corresponding severity level
     */
    public static WarningHistory.Severity mapConfidenceToSeverity(Double confidence) {
        if (confidence == null) {
            return WarningHistory.Severity.MEDIUM;
        }
        
        if (confidence >= 0.9) {
            return WarningHistory.Severity.CRITICAL;
        } else if (confidence >= 0.8) {
            return WarningHistory.Severity.HIGH;
        } else if (confidence >= 0.6) {
            return WarningHistory.Severity.MEDIUM;
        } else {
            return WarningHistory.Severity.LOW;
        }
    }
    
    /**
     * Map moderation category to warning type string for logging.
     * 
     * @param category The detected category from AI moderation
     * @return The corresponding warning type string
     */
    public static String mapCategoryToWarningTypeString(String category) {
        if (category == null) {
            return "OTHER";
        }
        
        switch (category.toUpperCase()) {
            case "HARASSMENT":
            case "BULLYING":
                return "HARASSMENT";
            case "HATE_SPEECH":
            case "DISCRIMINATION":
                return "HATE_SPEECH";
            case "VIOLENCE":
            case "THREATS":
                return "VIOLENCE";
            case "SPAM":
                return "SPAM";
            case "INAPPROPRIATE":
            case "EXPLICIT":
                return "INAPPROPRIATE_CONTENT";
            default:
                return "OTHER";
        }
    }
    
    /**
     * Check if a warning should be issued based on moderation result.
     * 
     * @param result The moderation result
     * @return true if a warning should be issued
     */
    public static boolean shouldIssueWarning(ModerationResult result) {
        return result.getConfidence() >= 0.7 && 
               (result.getSeverity() == ModerationResult.SeverityLevel.HIGH || 
                result.getSeverity() == ModerationResult.SeverityLevel.CRITICAL);
    }
    
    /**
     * Get severity level as string for logging.
     * 
     * @param result The moderation result
     * @return The severity level as string
     */
    public static String getSeverityAsString(ModerationResult result) {
        return result.getSeverity() != null ? result.getSeverity().toString() : "UNKNOWN";
    }
}
