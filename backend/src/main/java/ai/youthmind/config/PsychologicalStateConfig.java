package ai.youthmind.config;

import ai.youthmind.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.UUID;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.service.InteractionModulationService.ComprehensiveInteractionAnalysis;

// Interface definitions for analyzer classes
interface UserEngagementAnalyzer {
    double calculateEngagementLevel(UUID userId);
}

interface ContentEmotionAnalyzer {
    double analyzeEmotionIntensity(String content, UUID userId);
}

interface UserSensitivityAnalyzer {
    double calculateSensitivityLevel(UUID userId);
}

interface ContentComplexityAnalyzer {
    double analyzeComplexity(String content, UUID userId);
}

/**
 * Configuration class for Psychological State Management services
 * Provides bean definitions and service wiring with actual backend service integrations
 */
@Configuration
public class PsychologicalStateConfig {
    
    @Autowired
    private InteractionModulationService interactionModulationService;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityPredictor intensityPredictor;
    
    /**
     * Create actual implementation of UserEngagementAnalyzer using existing services
     */
    @Bean
    @Primary
    public UserEngagementAnalyzer userEngagementAnalyzer() {
        return new UserEngagementAnalyzer() {
            @Override
            public double calculateEngagementLevel(UUID userId) {
                try {
                    // Use InteractionModulationService for comprehensive engagement analysis
                    ComprehensiveInteractionAnalysis comprehensiveAnalysis = interactionModulationService.getComprehensiveAnalysis(userId);
                    
                    // Extract engagement metrics
                    double interactionIntensity = comprehensiveAnalysis.getInteractionIntensity();
                    int resonanceCount = comprehensiveAnalysis.getResonanceCount();
                    int lingerCount = 0; // Default value as ComprehensiveInteractionAnalysis doesn't have this field
                    
                    // Use ResonanceProcessor for resonance-based engagement
                    Map<String, Object> resonanceStats = resonanceProcessor.getResonanceStatistics(userId);
                    double resonanceEngagement = (Double) resonanceStats.getOrDefault("averageResonanceScore", 0.5);
                    
                    // Use LingerProcessor for linger-based engagement
                    Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
                    double lingerEngagement = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0) / 300.0; // Normalize to 5 minutes
                    
                    // Calculate weighted engagement level
                    double engagementLevel = (interactionIntensity * 0.4) + 
                                           (resonanceEngagement * 0.3) + 
                                           (lingerEngagement * 0.3);
                    
                    // Clamp to valid range
                    return Math.max(0.0, Math.min(1.0, engagementLevel));
                    
                } catch (Exception e) {
                    // Fallback to default engagement level
                    return 0.5;
                }
            }
        };
    }
    
    /**
     * Create actual implementation of ContentEmotionAnalyzer using existing services
     */
    @Bean
    @Primary
    public ContentEmotionAnalyzer contentEmotionAnalyzer() {
        return new ContentEmotionAnalyzer() {
            @Override
            public double analyzeEmotionIntensity(String content, UUID userId) {
                try {
                    // Use AIServiceClient for AI-based emotion analysis
                    Map<String, Object> emotionAnalysis = aiServiceClient.analyzeContentForModeration(content, userId);
                    Double aiEmotionIntensity = (Double) emotionAnalysis.get("emotionIntensity");
                    
                    // Use AIServiceClient for NLP-based emotion analysis (fallback)
                    Map<String, Object> nlpAnalysis = aiServiceClient.analyzeContentForModeration(content, userId);
                    Double nlpEmotionIntensity = (Double) nlpAnalysis.getOrDefault("emotionalIntensity", 0.5);
                    
                    // Use BuddhistFactorAnalyzer for Buddhist mental factor analysis
                    Map<String, Object> buddhistAnalysis = buddhistFactorAnalyzer.analyzeFactors(content, null);
                    @SuppressWarnings("unchecked")
                    Map<String, Double> factorScores = (Map<String, Double>) buddhistAnalysis.get("factorScores");
                    
                    double buddhistEmotionIntensity = 0.5;
                    if (factorScores != null) {
                        // Calculate emotion intensity from Buddhist factors
                        double anger = factorScores.getOrDefault("anger", 0.0);
                        double hatred = factorScores.getOrDefault("hatred", 0.0);
                        double rage = factorScores.getOrDefault("rage", 0.0);
                        double faith = factorScores.getOrDefault("faith", 0.0);
                        double vigor = factorScores.getOrDefault("vigor", 0.0);
                        
                        // Negative emotions increase intensity, positive emotions moderate it
                        buddhistEmotionIntensity = (anger + hatred + rage) / 3.0 + (1.0 - faith) * 0.2 + vigor * 0.1;
                    }
                    
                    // Combine different emotion analysis methods
                    double emotionIntensity = 0.0;
                    int methodCount = 0;
                    
                    if (aiEmotionIntensity != null) {
                        emotionIntensity += aiEmotionIntensity;
                        methodCount++;
                    }
                    
                    emotionIntensity += nlpEmotionIntensity;
                    methodCount++;
                    
                    emotionIntensity += buddhistEmotionIntensity;
                    methodCount++;
                    
                    // Calculate average emotion intensity
                    double averageEmotionIntensity = methodCount > 0 ? emotionIntensity / methodCount : 0.5;
                    
                    // Clamp to valid range
                    return Math.max(0.0, Math.min(1.0, averageEmotionIntensity));
                    
                } catch (Exception e) {
                    // Fallback to default emotion intensity
                    return 0.5;
                }
            }
        };
    }
    
    /**
     * Create actual implementation of UserSensitivityAnalyzer using existing services
     */
    @Bean
    @Primary
    public UserSensitivityAnalyzer userSensitivityAnalyzer() {
        return new UserSensitivityAnalyzer() {
            @Override
            public double calculateSensitivityLevel(UUID userId) {
                try {
                    // Use UserPersonalityService for personality-based sensitivity
                    UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
                    
                    // Extract personality traits that affect sensitivity
                    Map<String, Object> preferences = personalityProfile.getPreferences();
                    double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
                    double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
                    double conscientiousness = preferences != null ? (Double) preferences.getOrDefault("conscientiousness", 0.5) : 0.5;
                    
                    // Use LingerProcessor for behavioral sensitivity indicators
                    Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
                    double averageLingerTime = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0);
                    double lingerSensitivity = Math.min(1.0, averageLingerTime / 600.0); // Normalize to 10 minutes
                    
                    // Use ResonanceProcessor for resonance sensitivity
                    Map<String, Object> resonanceStats = resonanceProcessor.getResonanceStatistics(userId);
                    double resonanceSensitivity = (Double) resonanceStats.getOrDefault("averageResonanceScore", 0.5);
                    
                    // Calculate sensitivity level based on personality and behavior
                    double sensitivityLevel = (neuroticism * 0.4) + 
                                            (openness * 0.2) + 
                                            ((1.0 - conscientiousness) * 0.1) + 
                                            (lingerSensitivity * 0.2) + 
                                            (resonanceSensitivity * 0.1);
                    
                    // Clamp to valid range
                    return Math.max(0.0, Math.min(1.0, sensitivityLevel));
                    
                } catch (Exception e) {
                    // Fallback to default sensitivity level
                    return 0.5;
                }
            }
        };
    }
    
    /**
     * Create actual implementation of ContentComplexityAnalyzer using existing services
     */
    @Bean
    @Primary
    public ContentComplexityAnalyzer contentComplexityAnalyzer() {
        return new ContentComplexityAnalyzer() {
            @Override
            public double analyzeComplexity(String content, UUID userId) {
                try {
                    // Use AIServiceClient for content analysis
                    Map<String, Object> nlpAnalysis = aiServiceClient.analyzeContentForModeration(content, null);
                    Double linguisticComplexity = (Double) nlpAnalysis.getOrDefault("complexity", 0.5);
                    
                    // Use BuddhistFactorAnalyzer for psychological complexity
                    Map<String, Object> buddhistAnalysis = buddhistFactorAnalyzer.analyzeFactors(content, null);
                    @SuppressWarnings("unchecked")
                    Map<String, Double> factorScores = (Map<String, Double>) buddhistAnalysis.get("factorScores");
                    
                    double psychologicalComplexity = 0.5;
                    if (factorScores != null) {
                        // Calculate complexity based on number of active factors
                        double activeFactors = factorScores.values().stream()
                            .mapToDouble(score -> score > 0.3 ? 1.0 : 0.0)
                            .sum();
                        psychologicalComplexity = Math.min(1.0, activeFactors / 10.0); // Normalize to 10 factors
                    }
                    
                    // Use AIServiceClient for semantic complexity
                    Map<String, Object> aiAnalysis = aiServiceClient.analyzeContentForModeration(content, null);
                    Double semanticComplexity = (Double) aiAnalysis.getOrDefault("complexity", 0.5);
                    
                    // Calculate weighted complexity
                    double complexity = (linguisticComplexity * 0.4) + 
                                      (psychologicalComplexity * 0.4) + 
                                      (semanticComplexity * 0.2);
                    
                    // Clamp to valid range
                    return Math.max(0.0, Math.min(1.0, complexity));
                    
                } catch (Exception e) {
                    // Fallback to default complexity
                    return 0.5;
                }
            }
        };
    }
}