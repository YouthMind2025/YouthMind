package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import ai.youthmind.service.InteractionModulationService.ComprehensiveInteractionAnalysis;
import ai.youthmind.dto.UserPersonalityProfile;

/**
 * Calculate intensity factor θ ∈ [0,1] using post content analysis,
 * emotion intensity, and user engagement patterns
 */
@Service
public class IntensityFactorCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(IntensityFactorCalculator.class);
    
    // Intensity calculation constants
    public static final double MIN_INTENSITY = 0.0;
    public static final double MAX_INTENSITY = 1.0;
    public static final double DEFAULT_INTENSITY = 0.5;
    
    // Content analysis weights
    public static final double EMOTION_WEIGHT = 0.4;
    public static final double LENGTH_WEIGHT = 0.2;
    public static final double ENGAGEMENT_WEIGHT = 0.2;
    public static final double KEYWORD_WEIGHT = 0.2;
    
    // Emotion intensity patterns
    private static final Map<String, Double> EMOTION_INTENSITY_MAP = new HashMap<String, Double>() {{
        // High intensity emotions
        put("ecstatic", 0.95); put("devastated", 0.95); put("furious", 0.9); put("terrified", 0.9);
        put("thrilled", 0.9); put("heartbroken", 0.9); put("enraged", 0.9); put("petrified", 0.9);
        
        // Medium-high intensity emotions
        put("excited", 0.8); put("angry", 0.8); put("scared", 0.8); put("happy", 0.8); put("sad", 0.8);
        put("worried", 0.8); put("frustrated", 0.8); put("anxious", 0.8); put("confident", 0.8);
        
        // Medium intensity emotions
        put("pleased", 0.6); put("concerned", 0.6); put("content", 0.6); put("uneasy", 0.6);
        put("satisfied", 0.6); put("nervous", 0.6); put("calm", 0.6); put("restless", 0.6);
        
        // Low intensity emotions
        put("neutral", 0.3); put("mild", 0.3); put("slight", 0.3); put("gentle", 0.3);
        put("subtle", 0.3); put("quiet", 0.3); put("soft", 0.3); put("peaceful", 0.3);
    }};
    
    // High intensity keywords
    private static final Set<String> HIGH_INTENSITY_KEYWORDS = Set.of(
        "amazing", "incredible", "unbelievable", "fantastic", "terrible", "awful",
        "horrible", "devastating", "shocking", "stunning", "outstanding", "brilliant",
        "disgusting", "revolting", "magnificent", "spectacular", "tremendous",
        "overwhelming", "intense", "extreme", "massive", "huge", "enormous"
    );
    
    // Medium intensity keywords
    private static final Set<String> MEDIUM_INTENSITY_KEYWORDS = Set.of(
        "good", "bad", "nice", "okay", "fine", "decent", "average", "normal",
        "typical", "standard", "regular", "usual", "common", "familiar",
        "comfortable", "uncomfortable", "pleasant", "unpleasant"
    );
    
    // Intensity punctuation patterns
    private static final Pattern EXCLAMATION_PATTERN = Pattern.compile("!+");
    private static final Pattern QUESTION_PATTERN = Pattern.compile("\\?+");
    private static final Pattern CAPS_PATTERN = Pattern.compile("[A-Z]{3,}");
    
    // Redis keys
    private static final String INTENSITY_CACHE_KEY = "intensity_cache:";
    private static final String USER_ENGAGEMENT_KEY = "user_engagement:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private InteractionModulationService interactionModulationService;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private IntensityPredictor intensityPredictor;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private AIPostAnalysisService aiPostAnalysisService;
    
    @Autowired
    private PsychologicalAnalysisBatchProcessor batchProcessor;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private PersonalityComputationService personalityComputationService;
    
    // Using existing services instead of missing ones
    
    /**
     * Calculate intensity factor θ for a post
     * 
     * @param postContent Post content to analyze
     * @param userId User ID for personalization
     * @param vRaw Generated V_raw vector (optional, for additional analysis)
     * @return Intensity factor θ ∈ [0,1]
     */
    public double calculateIntensityFactor(String postContent, UUID userId, List<Double> vRaw) {
        try {
            logger.debug("Calculating intensity factor for user {} with content length {}", 
                        userId, postContent.length());
            
            // Check cache first
            String cacheKey = INTENSITY_CACHE_KEY + userId + ":" + postContent.hashCode();
            Double cachedIntensity = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedIntensity != null) {
                logger.debug("Using cached intensity factor: {}", cachedIntensity);
                return cachedIntensity;
            }
            
            // Use IntensityPredictor for comprehensive analysis
            double predictorIntensity = calculateIntensityUsingPredictor(postContent, userId, vRaw);
            
            // Use AIPostAnalysisService for comprehensive psychological analysis
            double aiAnalysisIntensity = calculateAIAnalysisIntensity(postContent, userId);
            
            // Use NLP analysis for content understanding
            double nlpIntensity = calculateNLPIntensity(postContent, userId);
            
            // Use personality-based intensity analysis
            double personalityIntensity = calculatePersonalityIntensity(userId);
            
            // Calculate additional intensity components
            double emotionIntensity = calculateEmotionIntensity(postContent);
            double lengthIntensity = calculateLengthIntensity(postContent);
            double engagementIntensity = calculateEngagementIntensity(userId);
            double keywordIntensity = calculateKeywordIntensity(postContent);
            double punctuationIntensity = calculatePunctuationIntensity(postContent);
            double vectorIntensity = calculateVectorIntensity(vRaw);
            
            // Weighted combination with multiple service integrations
            double intensity = (predictorIntensity * 0.25) + // IntensityPredictor
                             (aiAnalysisIntensity * 0.20) + // AIPostAnalysisService
                             (nlpIntensity * 0.15) + // NLP analysis
                             (personalityIntensity * 0.10) + // Personality-based
                             (emotionIntensity * EMOTION_WEIGHT * 0.6) +
                             (lengthIntensity * LENGTH_WEIGHT * 0.6) +
                             (engagementIntensity * ENGAGEMENT_WEIGHT * 0.6) +
                             (keywordIntensity * KEYWORD_WEIGHT * 0.6) +
                             (punctuationIntensity * 0.05) +
                             (vectorIntensity * 0.15);
            
            // Apply user-specific adjustments
            intensity = applyUserAdjustments(intensity, userId);
            
            // Clamp to valid range
            intensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, intensity));
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, intensity, 1, TimeUnit.HOURS);
            
            logger.debug("Calculated intensity factor: {} (predictor: {}, emotion: {}, engagement: {})", 
                        intensity, predictorIntensity, emotionIntensity, engagementIntensity);
            return intensity;
            
        } catch (Exception e) {
            logger.error("Error calculating intensity factor for user {}: {}", userId, e.getMessage(), e);
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity factor for interaction
     * 
     * @param interactionType Type of interaction (resonance, linger, etc.)
     * @param interactionDuration Duration of interaction in seconds
     * @param userId User ID for personalization
     * @return Intensity factor θ ∈ [0,1]
     */
    public double calculateInteractionIntensity(String interactionType, long interactionDuration, UUID userId) {
        try {
            logger.debug("Calculating interaction intensity for user {} with type {} and duration {}", 
                        userId, interactionType, interactionDuration);
            
            // Use InteractionModulationService for comprehensive analysis
            double modulationIntensity = calculateModulationIntensity(interactionType, userId, interactionDuration);
            
            double baseIntensity = getBaseInteractionIntensity(interactionType);
            double durationMultiplier = calculateDurationMultiplier(interactionDuration);
            double userEngagementFactor = calculateEngagementIntensity(userId);
            
            // Combine with modulation service results
            double intensity = (modulationIntensity * 0.6) + (baseIntensity * durationMultiplier * userEngagementFactor * 0.4);
            
            // Clamp to valid range
            intensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, intensity));
            
            logger.debug("Calculated interaction intensity: {} (modulation: {}, base: {})", 
                        intensity, modulationIntensity, baseIntensity);
            return intensity;
            
        } catch (Exception e) {
            logger.error("Error calculating interaction intensity for user {}: {}", userId, e.getMessage(), e);
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity factor for psychological analysis
     * 
     * @param psychologicalVector 43-dimensional psychological vector
     * @param userId User ID for personalization
     * @return Intensity factor θ ∈ [0,1]
     */
    public double calculatePsychologicalIntensity(List<Double> psychologicalVector, UUID userId) {
        try {
            if (psychologicalVector == null || psychologicalVector.size() != 43) {
                return DEFAULT_INTENSITY;
            }
            
            // Use comprehensive vector intensity analysis
            double vectorIntensity = calculateVectorIntensity(psychologicalVector);
            
            // Apply user-specific adjustments
            double adjustedIntensity = applyUserAdjustments(vectorIntensity, userId);
            
            // Clamp to valid range
            adjustedIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, adjustedIntensity));
            
            logger.debug("Calculated psychological intensity: {} (vector intensity: {})", adjustedIntensity, vectorIntensity);
            return adjustedIntensity;
            
        } catch (Exception e) {
            logger.error("Error calculating psychological intensity for user {}: {}", userId, e.getMessage(), e);
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity factor specifically for 43-dimensional Buddhist mental factors
     * 
     * @param vRaw 43-dimensional raw mental factor vector
     * @param userId User ID for personalization
     * @return Intensity factor θ ∈ [0,1] based on Buddhist mental factors
     */
    public double calculateBuddhistFactorIntensity(List<Double> vRaw, UUID userId) {
        try {
            if (vRaw == null || vRaw.size() != 43) {
                return DEFAULT_INTENSITY;
            }
            
            // Calculate comprehensive Buddhist factor intensity
            double buddhistIntensity = calculateBuddhistFactorIntensity(vRaw);
            double specificIntensity = calculateSpecificFactorIntensity(vRaw);
            
            // Combine Buddhist-specific analyses
            double combinedIntensity = (buddhistIntensity * 0.7) + (specificIntensity * 0.3);
            
            // Apply user-specific adjustments
            double adjustedIntensity = applyUserAdjustments(combinedIntensity, userId);
            
            // Clamp to valid range
            adjustedIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, adjustedIntensity));
            
            logger.debug("Calculated Buddhist factor intensity: {} (buddhist: {}, specific: {})", 
                        adjustedIntensity, buddhistIntensity, specificIntensity);
            return adjustedIntensity;
            
        } catch (Exception e) {
            logger.error("Error calculating Buddhist factor intensity for user {}: {}", userId, e.getMessage(), e);
            return DEFAULT_INTENSITY;
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private double calculateEmotionIntensity(String content) {
        try {
            String lowerContent = content.toLowerCase();
            double maxEmotionIntensity = 0.0;
            
            // Check for emotion words
            for (Map.Entry<String, Double> entry : EMOTION_INTENSITY_MAP.entrySet()) {
                if (lowerContent.contains(entry.getKey())) {
                    maxEmotionIntensity = Math.max(maxEmotionIntensity, entry.getValue());
                }
            }
            
            // Use AI service for advanced emotion analysis
            double aiEmotionIntensity = analyzeEmotionWithAI(content);
            maxEmotionIntensity = Math.max(maxEmotionIntensity, aiEmotionIntensity);
            
            return maxEmotionIntensity;
            
        } catch (Exception e) {
            logger.warn("Error calculating emotion intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculateLengthIntensity(String content) {
        try {
            int length = content.length();
            
            // Normalize length to [0,1] range
            // Very short posts (0-50 chars) have low intensity
            // Medium posts (50-200 chars) have medium intensity
            // Long posts (200+ chars) have high intensity
            if (length <= 50) {
                return 0.2;
            } else if (length <= 200) {
                return 0.5;
            } else if (length <= 500) {
                return 0.8;
            } else {
                return 1.0;
            }
            
        } catch (Exception e) {
            logger.warn("Error calculating length intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculateEngagementIntensity(UUID userId) {
        try {
            // Check cache first
            String cacheKey = USER_ENGAGEMENT_KEY + userId;
            Double cachedEngagement = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedEngagement != null) {
                return cachedEngagement;
            }
            
            // Calculate engagement using existing services
            double engagement = calculateUserEngagementLevel(userId);
            redisTemplate.opsForValue().set(cacheKey, engagement, 30, TimeUnit.MINUTES);
            return engagement;
            
        } catch (Exception e) {
            logger.warn("Error calculating engagement intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculateKeywordIntensity(String content) {
        try {
            String lowerContent = content.toLowerCase();
            double intensity = 0.0;
            int keywordCount = 0;
            
            // Check for high intensity keywords
            for (String keyword : HIGH_INTENSITY_KEYWORDS) {
                if (lowerContent.contains(keyword)) {
                    intensity += 0.8;
                    keywordCount++;
                }
            }
            
            // Check for medium intensity keywords
            for (String keyword : MEDIUM_INTENSITY_KEYWORDS) {
                if (lowerContent.contains(keyword)) {
                    intensity += 0.4;
                    keywordCount++;
                }
            }
            
            // Normalize by keyword count
            if (keywordCount > 0) {
                intensity = Math.min(1.0, intensity / keywordCount);
            }
            
            return intensity;
            
        } catch (Exception e) {
            logger.warn("Error calculating keyword intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculatePunctuationIntensity(String content) {
        try {
            double intensity = 0.0;
            
            // Count exclamation marks
            int exclamationCount = (int) EXCLAMATION_PATTERN.matcher(content).results().count();
            intensity += Math.min(0.5, exclamationCount * 0.1);
            
            // Count question marks
            int questionCount = (int) QUESTION_PATTERN.matcher(content).results().count();
            intensity += Math.min(0.3, questionCount * 0.05);
            
            // Check for all caps
            if (CAPS_PATTERN.matcher(content).find()) {
                intensity += 0.2;
            }
            
            return Math.min(1.0, intensity);
            
        } catch (Exception e) {
            logger.warn("Error calculating punctuation intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculateVectorIntensity(List<Double> vRaw) {
        try {
            if (vRaw == null || vRaw.isEmpty()) {
                return DEFAULT_INTENSITY;
            }
            
            // Calculate comprehensive vector intensity using multiple methods
            double magnitudeIntensity = calculateMagnitudeIntensity(vRaw);
            double varianceIntensity = calculateVarianceIntensity(vRaw);
            double buddhistFactorIntensity = calculateBuddhistFactorIntensity(vRaw);
            double emotionalIntensity = calculateEmotionalDimensionIntensity(vRaw);
            double cognitiveIntensity = calculateCognitiveDimensionIntensity(vRaw);
            
            // Weighted combination of different intensity measures
            double combinedIntensity = (magnitudeIntensity * 0.25) +
                                     (varianceIntensity * 0.25) +
                                     (buddhistFactorIntensity * 0.25) +
                                     (emotionalIntensity * 0.15) +
                                     (cognitiveIntensity * 0.10);
            
            return Math.max(0.0, Math.min(1.0, combinedIntensity));
            
        } catch (Exception e) {
            logger.warn("Error calculating vector intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    private double calculateVectorMagnitude(List<Double> vector) {
        double sumSquared = 0.0;
        for (Double value : vector) {
            sumSquared += value * value;
        }
        return Math.sqrt(sumSquared);
    }
    
    private double getBaseInteractionIntensity(String interactionType) {
        return switch (interactionType.toLowerCase()) {
            case "resonance" -> 0.8;
            case "linger" -> 0.3;
            case "comment" -> 0.6;
            case "share" -> 0.7;
            case "like" -> 0.4;
            case "view" -> 0.2;
            default -> 0.5;
        };
    }
    
    private double calculateDurationMultiplier(long durationSeconds) {
        // Longer interactions indicate higher intensity
        if (durationSeconds <= 5) {
            return 0.5;
        } else if (durationSeconds <= 30) {
            return 0.8;
        } else if (durationSeconds <= 120) {
            return 1.0;
        } else {
            return 1.2;
        }
    }
    
    private double applyUserAdjustments(double baseIntensity, UUID userId) {
        try {
            // Apply user-specific adjustments based on:
            // - User's historical intensity patterns
            // - User's sensitivity levels
            // - User's engagement patterns
            
            // Get user's historical intensity patterns
            double historicalIntensity = getUserHistoricalIntensity(userId);
            
            // Get user's sensitivity level
            double sensitivityLevel = getUserSensitivityLevel(userId);
            
            // Apply adjustments
            double adjustedIntensity = baseIntensity * (1.0 + historicalIntensity * 0.2) * (1.0 + sensitivityLevel * 0.3);
            
            // Clamp to valid range
            return Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, adjustedIntensity));
            
        } catch (Exception e) {
            logger.warn("Error applying user adjustments: {}", e.getMessage());
            return baseIntensity;
        }
    }
    
    /**
     * Calculate modulation intensity using InteractionModulationService
     */
    private double calculateModulationIntensity(String interactionType, UUID userId, long duration) {
        try {
            // Get comprehensive interaction analysis
            ComprehensiveInteractionAnalysis analysis = interactionModulationService.getComprehensiveAnalysis(userId);
            
            // Extract relevant metrics based on interaction type
            double baseIntensity = 0.0;
            switch (interactionType.toLowerCase()) {
                case "resonance":
                    baseIntensity = analysis.getResonanceCount() / 10.0; // Normalize resonance count
                    break;
                case "linger":
                    Map<String, Object> lingerStats = analysis.getLingerStatistics();
                    double lingerTime = lingerStats != null ? (Double) lingerStats.getOrDefault("averageLingerTime", 0.0) : 0.0;
                    baseIntensity = Math.min(1.0, lingerTime / 300.0); // Normalize to 5 minutes
                    break;
                case "comment":
                case "share":
                case "like":
                    baseIntensity = analysis.getInteractionIntensity() / 3.0; // Normalize interaction intensity
                    break;
                default:
                    baseIntensity = 0.5;
            }
            
            // Apply duration multiplier
            double durationMultiplier = calculateDurationMultiplier(duration);
            double modulationIntensity = baseIntensity * durationMultiplier;
            
            logger.debug("Modulation intensity for {}: {} (base: {}, duration: {})", 
                        interactionType, modulationIntensity, baseIntensity, durationMultiplier);
            
            return Math.max(0.0, Math.min(1.0, modulationIntensity));
            
        } catch (Exception e) {
            logger.warn("Error calculating modulation intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity using AIPostAnalysisService
     */
    private double calculateAIAnalysisIntensity(String content, UUID userId) {
        try {
            // Use AIPostAnalysisService for comprehensive psychological analysis
            Map<String, Object> analysisResult = aiPostAnalysisService.processPsychologicalContent(content, userId);
            
            // Extract intensity from analysis result
            Map<String, Object> intensityFactor = (Map<String, Object>) analysisResult.get("intensityFactor");
            if (intensityFactor != null) {
                Double intensityScore = (Double) intensityFactor.get("intensityScore");
                if (intensityScore != null) {
                    logger.debug("AIPostAnalysisService intensity: {}", intensityScore);
                    return Math.max(0.0, Math.min(1.0, intensityScore));
                }
            }
            
            return DEFAULT_INTENSITY;
            
        } catch (Exception e) {
            logger.warn("Error using AIPostAnalysisService: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity using NLP analysis
     */
    private double calculateNLPIntensity(String content, UUID userId) {
        try {
            // Use NLPAnalyzer for content analysis
            Map<String, Object> nlpAnalysis = aiServiceClient.analyzeContentForModeration(content, userId);
            
            // Extract intensity indicators from NLP analysis
            double sentimentIntensity = (Double) nlpAnalysis.getOrDefault("sentimentIntensity", 0.5);
            double emotionalIntensity = (Double) nlpAnalysis.getOrDefault("emotionalIntensity", 0.5);
            double urgencyIntensity = (Double) nlpAnalysis.getOrDefault("urgencyIntensity", 0.5);
            
            // Combine NLP indicators
            double nlpIntensity = (sentimentIntensity + emotionalIntensity + urgencyIntensity) / 3.0;
            
            logger.debug("NLP intensity: {} (sentiment: {}, emotional: {}, urgency: {})", 
                        nlpIntensity, sentimentIntensity, emotionalIntensity, urgencyIntensity);
            
            return Math.max(0.0, Math.min(1.0, nlpIntensity));
            
        } catch (Exception e) {
            logger.warn("Error using NLP analysis: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity based on user personality
     */
    private double calculatePersonalityIntensity(UUID userId) {
        try {
            // Get user personality profile
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality intensity indicators from preferences
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double opennessIntensity = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            double neuroticismIntensity = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double extraversionIntensity = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            
            // Calculate personality-based intensity
            // Higher neuroticism and openness typically indicate higher intensity
            double personalityIntensity = (neuroticismIntensity * 0.5) + (opennessIntensity * 0.3) + (extraversionIntensity * 0.2);
            
            logger.debug("Personality intensity: {} (openness: {}, neuroticism: {}, extraversion: {})", 
                        personalityIntensity, opennessIntensity, neuroticismIntensity, extraversionIntensity);
            
            return Math.max(0.0, Math.min(1.0, personalityIntensity));
            
        } catch (Exception e) {
            logger.warn("Error calculating personality intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity using IntensityPredictor service
     */
    private double calculateIntensityUsingPredictor(String content, UUID userId, List<Double> vRaw) {
        try {
            // Generate Buddhist factors if not provided
            Map<String, Object> buddhistFactors = null;
            if (buddhistFactorAnalyzer != null) {
                buddhistFactors = buddhistFactorAnalyzer.analyzeFactors(content, userId);
            }
            
            // Use IntensityPredictor for comprehensive analysis
            Map<String, Object> intensityResult = intensityPredictor.predictIntensity(
                content, userId, vRaw, buddhistFactors);
            
            // Extract intensity score
            Double intensityScore = (Double) intensityResult.get("intensityScore");
            if (intensityScore != null) {
                logger.debug("IntensityPredictor result: {}", intensityScore);
                return Math.max(0.0, Math.min(1.0, intensityScore));
            }
            
            return DEFAULT_INTENSITY;
            
        } catch (Exception e) {
            logger.warn("Error using IntensityPredictor: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate user engagement level using existing services
     */
    private double calculateUserEngagementLevel(UUID userId) {
        try {
            // Get comprehensive interaction analysis
            ComprehensiveInteractionAnalysis analysis = interactionModulationService.getComprehensiveAnalysis(userId);
            
            // Calculate engagement based on multiple factors
            double resonanceEngagement = calculateResonanceEngagement(analysis);
            double lingerEngagement = calculateLingerEngagement(analysis);
            double interactionEngagement = calculateInteractionEngagement(analysis);
            
            // Weighted combination
            double totalEngagement = (resonanceEngagement * 0.4) + 
                                   (lingerEngagement * 0.3) + 
                                   (interactionEngagement * 0.3);
            
            return Math.max(0.0, Math.min(1.0, totalEngagement));
            
        } catch (Exception e) {
            logger.warn("Error calculating user engagement level: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Analyze emotion intensity using AI service
     */
    private double analyzeEmotionWithAI(String content) {
        try {
            // Create emotion analysis prompt
            String prompt = buildEmotionAnalysisPrompt(content);
            
            // Call AI service for emotion analysis
            String aiResponse = aiServiceClient.generateCompletion(prompt, "emotion_analysis");
            
            // Parse emotion intensity from AI response
            return parseEmotionIntensity(aiResponse);
            
        } catch (Exception e) {
            logger.warn("Error analyzing emotion with AI: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Get user's historical intensity patterns
     */
    private double getUserHistoricalIntensity(UUID userId) {
        try {
            // Get resonance patterns
            Map<String, Object> resonancePatterns = resonanceProcessor.trackResonancePatterns(userId);
            
            // Extract frequency and consistency
            double frequency = (Double) resonancePatterns.getOrDefault("frequency", 0.5);
            double consistency = (Double) resonancePatterns.getOrDefault("consistency", 0.5);
            
            // Calculate historical intensity
            return (frequency + consistency) / 2.0;
            
        } catch (Exception e) {
            logger.warn("Error getting user historical intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Get user's sensitivity level
     */
    private double getUserSensitivityLevel(UUID userId) {
        try {
            // Get linger statistics
            Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
            
            // Extract sensitivity indicators
            double averageLingerTime = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0);
            double lingerFrequency = (Double) lingerStats.getOrDefault("lingerFrequency", 0.0);
            
            // Calculate sensitivity level (higher linger time = higher sensitivity)
            double sensitivity = Math.min(1.0, (averageLingerTime / 60.0) * lingerFrequency);
            
            return sensitivity;
            
        } catch (Exception e) {
            logger.warn("Error getting user sensitivity level: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate resonance engagement
     */
    private double calculateResonanceEngagement(ComprehensiveInteractionAnalysis analysis) {
        try {
            int resonanceCount = analysis.getResonanceCount();
            Map<String, Object> resonancePatterns = analysis.getResonancePatterns();
            
            // Calculate engagement based on resonance count and patterns
            double frequency = (Double) resonancePatterns.getOrDefault("frequency", 0.0);
            double consistency = (Double) resonancePatterns.getOrDefault("consistency", 0.0);
            
            // Normalize resonance count (assuming 0-10 resonances per week is normal)
            double normalizedCount = Math.min(1.0, resonanceCount / 10.0);
            
            return (normalizedCount + frequency + consistency) / 3.0;
            
        } catch (Exception e) {
            logger.warn("Error calculating resonance engagement: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate linger engagement
     */
    private double calculateLingerEngagement(ComprehensiveInteractionAnalysis analysis) {
        try {
            Map<String, Object> lingerStats = analysis.getLingerStatistics();
            
            // Extract linger metrics
            double averageLingerTime = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0);
            double lingerFrequency = (Double) lingerStats.getOrDefault("lingerFrequency", 0.0);
            
            // Normalize linger time (assuming 0-300 seconds is normal)
            double normalizedTime = Math.min(1.0, averageLingerTime / 300.0);
            
            return (normalizedTime + lingerFrequency) / 2.0;
            
        } catch (Exception e) {
            logger.warn("Error calculating linger engagement: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate interaction engagement
     */
    private double calculateInteractionEngagement(ComprehensiveInteractionAnalysis analysis) {
        try {
            double interactionIntensity = analysis.getInteractionIntensity();
            
            // Normalize interaction intensity
            return Math.min(1.0, interactionIntensity / 3.0);
            
        } catch (Exception e) {
            logger.warn("Error calculating interaction engagement: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Build emotion analysis prompt for AI
     */
    private String buildEmotionAnalysisPrompt(String content) {
        return String.format("""
            Analyze the emotional intensity of this content on a scale from 0.0 to 1.0:
            
            Content: "%s"
            
            Consider:
            - Emotional language and expressions
            - Intensity of feelings expressed
            - Urgency and importance conveyed
            - Personal significance indicated
            
            Respond with only a number between 0.0 and 1.0 representing the emotional intensity.
            """, content);
    }
    
    /**
     * Parse emotion intensity from AI response
     */
    private double parseEmotionIntensity(String aiResponse) {
        try {
            // Extract number from AI response
            String numberStr = aiResponse.replaceAll("[^0-9.]", "");
            if (numberStr.isEmpty()) {
                return DEFAULT_INTENSITY;
            }
            
            double intensity = Double.parseDouble(numberStr);
            return Math.max(0.0, Math.min(1.0, intensity));
            
        } catch (Exception e) {
            logger.warn("Error parsing emotion intensity from AI response: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate magnitude-based intensity
     */
    private double calculateMagnitudeIntensity(List<Double> vector) {
        double magnitude = calculateVectorMagnitude(vector);
        // Normalize magnitude to [0,1] range
        return Math.min(1.0, magnitude / Math.sqrt(43));
    }
    
    /**
     * Calculate variance-based intensity
     */
    private double calculateVarianceIntensity(List<Double> vector) {
        double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = vector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        
        // Higher variance indicates higher intensity
        double stdDev = Math.sqrt(variance);
        return Math.min(1.0, stdDev);
    }
    
    /**
     * Calculate Buddhist factor-based intensity
     * Analyzes the 43-dimensional vector based on Buddhist mental factors
     */
    private double calculateBuddhistFactorIntensity(List<Double> vector) {
        try {
            // Core factors (dimensions 0-4) - universal mental factors
            double coreIntensity = calculateDimensionGroupIntensity(vector, 0, 4);
            
            // Positive factors (dimensions 5-15) - wholesome mental factors
            double positiveIntensity = calculateDimensionGroupIntensity(vector, 5, 15);
            
            // Negative factors (dimensions 16-42) - unwholesome mental factors
            double negativeIntensity = calculateDimensionGroupIntensity(vector, 16, 42);
            
            // Calculate intensity based on the balance and strength of factors
            // Higher negative factors and lower positive factors indicate higher intensity
            double factorIntensity = (coreIntensity * 0.2) + 
                                   (negativeIntensity * 0.6) - 
                                   (positiveIntensity * 0.2);
            
            return Math.max(0.0, Math.min(1.0, factorIntensity));
            
        } catch (Exception e) {
            logger.warn("Error calculating Buddhist factor intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate emotional dimension intensity
     * Focuses on emotional mental factors (feeling, contact, etc.)
     */
    private double calculateEmotionalDimensionIntensity(List<Double> vector) {
        try {
            // Emotional factors: feeling (2), contact (1), shame (7, 8)
            List<Integer> emotionalDimensions = Arrays.asList(1, 2, 7, 8);
            
            double emotionalIntensity = emotionalDimensions.stream()
                .mapToDouble(dim -> Math.abs(vector.get(dim)))
                .average().orElse(0.0);
            
            return Math.min(1.0, emotionalIntensity);
            
        } catch (Exception e) {
            logger.warn("Error calculating emotional dimension intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate cognitive dimension intensity
     * Focuses on cognitive mental factors (thinking, perception, etc.)
     */
    private double calculateCognitiveDimensionIntensity(List<Double> vector) {
        try {
            // Cognitive factors: attention (0), perception (3), thinking (4), deliberation (5)
            List<Integer> cognitiveDimensions = Arrays.asList(0, 3, 4, 5);
            
            double cognitiveIntensity = cognitiveDimensions.stream()
                .mapToDouble(dim -> Math.abs(vector.get(dim)))
                .average().orElse(0.0);
            
            return Math.min(1.0, cognitiveIntensity);
            
        } catch (Exception e) {
            logger.warn("Error calculating cognitive dimension intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity for a specific group of dimensions
     */
    private double calculateDimensionGroupIntensity(List<Double> vector, int startDim, int endDim) {
        try {
            double sum = 0.0;
            int count = 0;
            
            for (int i = startDim; i <= endDim && i < vector.size(); i++) {
                sum += Math.abs(vector.get(i));
                count++;
            }
            
            return count > 0 ? sum / count : 0.0;
            
        } catch (Exception e) {
            logger.warn("Error calculating dimension group intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }
    
    /**
     * Calculate intensity based on specific Buddhist mental factor patterns
     */
    private double calculateSpecificFactorIntensity(List<Double> vector) {
        try {
            // Analyze specific high-impact factors
            double greedIntensity = Math.abs(vector.get(16)); // Greed
            double angerIntensity = Math.abs(vector.get(17)); // Anger
            double arroganceIntensity = Math.abs(vector.get(18)); // Arrogance
            double doubtIntensity = Math.abs(vector.get(20)); // Doubt
            
            // Analyze positive factors that might indicate intensity
            double faithIntensity = Math.abs(vector.get(5)); // Faith
            double vigorIntensity = Math.abs(vector.get(6)); // Vigor
            
            // Calculate weighted intensity
            double negativeIntensity = (greedIntensity + angerIntensity + arroganceIntensity + doubtIntensity) / 4.0;
            double positiveIntensity = (faithIntensity + vigorIntensity) / 2.0;
            
            // Higher negative factors indicate higher intensity
            double specificIntensity = negativeIntensity * 0.8 + positiveIntensity * 0.2;
            
            return Math.min(1.0, specificIntensity);
            
        } catch (Exception e) {
            logger.warn("Error calculating specific factor intensity: {}", e.getMessage());
            return DEFAULT_INTENSITY;
        }
    }

}
