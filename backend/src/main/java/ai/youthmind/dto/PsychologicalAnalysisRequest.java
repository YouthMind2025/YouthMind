package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for psychological analysis requests
 * Contains all necessary data for comprehensive psychological analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologicalAnalysisRequest {
    
    // Request identification
    private String requestId;
    private UUID userId;
    private String sessionId;
    
    // Content to analyze
    private String content;
    private String contentType; // "post", "comment", "message", "journal_entry"
    private String language;
    
    // Context information
    private Map<String, Object> context;
    private List<String> tags;
    private String emotion; // Current emotion if known
    private String mood; // Current mood if known
    
    // Analysis configuration
    private AnalysisConfig analysisConfig;
    
    // User profile information
    private UserProfile userProfile;
    
    // Request metadata
    private LocalDateTime timestamp;
    private String source; // "mobile_app", "web_app", "api"
    private String version; // API version
    
    // Analysis preferences
    private AnalysisPreferences preferences;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisConfig {
        private boolean generatePsychologicalVector;
        private boolean analyzeBuddhistFactors;
        private boolean calculateIntensityFactor;
        private boolean processStateVector;
        private boolean includeDetailedAnalysis;
        private boolean includeRecommendations;
        private int maxAnalysisDepth; // 1-5 scale
        private String analysisMode; // "quick", "standard", "comprehensive"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private String ageGroup; // "teen", "young_adult", "adult"
        private String gender;
        private String culturalBackground;
        private String timezone;
        private List<String> knownConditions; // Mental health conditions
        private List<String> interests;
        private String preferredLanguage;
        private Map<String, Object> personalityTraits;
        private String riskLevel; // "low", "medium", "high"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisPreferences {
        private boolean includeEmotionalAnalysis;
        private boolean includeCognitiveAnalysis;
        private boolean includeSocialAnalysis;
        private boolean includeStressIndicators;
        private boolean includePersonalityTraits;
        private boolean includeMentalHealthIndicators;
        private boolean includeCopingMechanisms;
        private boolean includeResilienceFactors;
        private String outputFormat; // "json", "detailed", "summary"
        private String detailLevel; // "basic", "intermediate", "advanced"
    }
}
