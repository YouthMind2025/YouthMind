package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for psychological analysis responses
 * Contains comprehensive psychological analysis results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologicalAnalysisResponse {
    
    // Response identification
    private String requestId;
    private UUID userId;
    private String sessionId;
    
    // Analysis results
    private PsychologicalVectorResult psychologicalVector;
    private BuddhistFactorsResult buddhistFactors;
    private IntensityFactorResult intensityFactor;
    private StateVectorResult stateVector;
    private ComprehensiveAnalysis comprehensiveAnalysis;
    
    // Analysis metadata
    private AnalysisMetadata metadata;
    
    // Recommendations and insights
    private List<Recommendation> recommendations;
    private List<Insight> insights;
    
    // Risk assessment
    private RiskAssessment riskAssessment;
    
    // Processing information
    private ProcessingInfo processingInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PsychologicalVectorResult {
        private List<Double> vector; // 43-dimensional vector
        private double confidenceScore;
        private boolean isValid;
        private Map<String, Object> optimizationDetails;
        private Map<String, Double> dimensionScores; // Individual dimension scores
        private String analysisMethod;
        private LocalDateTime generatedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddhistFactorsResult {
        private Map<String, Double> factorScores;
        private Map<String, List<String>> detectedKeywords;
        private Map<String, Object> classifiedFactors;
        private Map<String, Object> intensityAnalysis;
        private String dominantFactor;
        private double overallFactorScore;
        private LocalDateTime analyzedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntensityFactorResult {
        private double intensityScore;
        private double impactScore;
        private String intensityClassification; // "HIGH", "MODERATE", "LOW", "VERY_LOW"
        private boolean isValid;
        private Map<String, Object> predictionDetails;
        private double confidenceLevel;
        private LocalDateTime predictedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateVectorResult {
        private List<Double> processedVector;
        private Map<String, Object> processingDetails;
        private boolean isValid;
        private Map<String, Object> statistics;
        private String processingMethod;
        private LocalDateTime processedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComprehensiveAnalysis {
        private EmotionalAnalysis emotionalAnalysis;
        private CognitiveAnalysis cognitiveAnalysis;
        private SocialAnalysis socialAnalysis;
        private StressAnalysis stressAnalysis;
        private PersonalityAnalysis personalityAnalysis;
        private MentalHealthAnalysis mentalHealthAnalysis;
        private CopingAnalysis copingAnalysis;
        private ResilienceAnalysis resilienceAnalysis;
        private String overallAssessment;
        private double overallScore;
        private LocalDateTime analyzedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionalAnalysis {
        private String primaryEmotion;
        private List<String> secondaryEmotions;
        private double emotionalIntensity;
        private String emotionalStability;
        private Map<String, Double> emotionScores;
        private String emotionalTrend; // "improving", "stable", "declining"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CognitiveAnalysis {
        private String thinkingPattern;
        private double analyticalThinking;
        private double creativeExpression;
        private double problemSolving;
        private double memoryReflection;
        private double futureOrientation;
        private String cognitiveStyle;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialAnalysis {
        private double socialConnection;
        private double isolationLevel;
        private double empathyCompassion;
        private double conflictTension;
        private double trustIntimacy;
        private String socialStyle;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressAnalysis {
        private double workStress;
        private double academicStress;
        private double financialStress;
        private double healthStress;
        private double relationshipStress;
        private double overallStressLevel;
        private String stressPattern;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalityAnalysis {
        private double extraversion;
        private double introversion;
        private double openness;
        private double conscientiousness;
        private double agreeableness;
        private String dominantTrait;
        private String personalityType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MentalHealthAnalysis {
        private double depressionIndicators;
        private double anxietyIndicators;
        private double selfEsteem;
        private double sleepPatterns;
        private double appetiteEnergy;
        private String mentalHealthStatus;
        private List<String> riskFactors;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopingAnalysis {
        private double positiveCoping;
        private double avoidance;
        private double seekingSupport;
        private double selfCare;
        private double problemFocusedCoping;
        private String copingStyle;
        private List<String> effectiveStrategies;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResilienceAnalysis {
        private double resilience;
        private double growthMindset;
        private String resilienceLevel;
        private List<String> strengths;
        private List<String> areasForGrowth;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisMetadata {
        private String analysisId;
        private String modelVersion;
        private String analysisMethod;
        private LocalDateTime timestamp;
        private int processingTimeMs;
        private String status; // "SUCCESS", "PARTIAL", "ERROR"
        private Map<String, Object> technicalDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String type; // "emotional_support", "cognitive_technique", "social_connection", "stress_management"
        private String title;
        private String description;
        private String priority; // "high", "medium", "low"
        private List<String> actionSteps;
        private String rationale;
        private Map<String, Object> metadata;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String category; // "emotional", "cognitive", "social", "behavioral"
        private String title;
        private String description;
        private String significance;
        private String confidence; // "high", "medium", "low"
        private Map<String, Object> supportingData;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessment {
        private String overallRiskLevel; // "low", "medium", "high", "critical"
        private List<String> riskFactors;
        private List<String> protectiveFactors;
        private String riskCategory; // "self_harm", "substance_abuse", "violence", "isolation"
        private boolean requiresImmediateAttention;
        private String recommendedAction;
        private Map<String, Object> riskDetails;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingInfo {
        private String processingEngine;
        private String modelVersion;
        private int totalProcessingTimeMs;
        private Map<String, Integer> componentProcessingTimes;
        private String processingStatus;
        private List<String> warnings;
        private List<String> errors;
        private Map<String, Object> technicalMetrics;
    }
}
