package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import ai.youthmind.entity.StateUpdateHistory;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.service.InteractionModulationService.ComprehensiveInteractionAnalysis;

import static net.logstash.logback.argument.StructuredArguments.*;
import static ai.youthmind.util.StructuredLogger.*;

/**
 * Implement state update rules with dynamic α calculation
 * Implements S_new = α·V_raw + (1-α)·S_prev for posting updates
 * Implements S_new = Ws·R′′ + (1-Ws)·S_prev for interaction updates
 */
@Service
public class StateUpdateRuleEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(StateUpdateRuleEngine.class);
    
    // State update constants
    public static final double MIN_ALPHA = 0.01;
    public static final double MAX_ALPHA = 0.9;
    public static final double DEFAULT_ALPHA = 0.1;
    public static final int VECTOR_DIMENSIONS = 43;
    
    @Autowired
    private StateVectorCalculator stateVectorCalculator;
    
    @Autowired
    private IntensityFactorCalculator intensityFactorCalculator;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private AIPostAnalysisService aiPostAnalysisService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private InteractionModulationService interactionModulationService;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    // Redis keys
    private static final String UPDATE_CACHE_KEY = "state_update:";
    private static final String ALPHA_CACHE_KEY = "alpha_calculation:";
    
    /**
     * Apply posting update rule: S_new = α·V_raw + (1-α)·S_prev
     * 
     * @param currentState S_prev - current psychological state
     * @param vRaw V_raw - raw vector from post content
     * @param alpha α - learning rate (base_alpha + theta)
     * @return Updated state S_new
     */
    public List<Double> applyPostingUpdate(List<Double> currentState, List<Double> vRaw, double alpha) {
        try {
            // Log state update with before values
            Map<String, Double> beforeState = extractDominantDimensions(currentState);
            
            logger.info("Applying posting update",
                kv("beforeState", beforeState),
                kv("alpha", alpha),
                kv("updateType", "posting")
            );
            
            // Validate inputs
            validateStateUpdateInputs(currentState, vRaw);
            
            // Clamp alpha to valid range
            alpha = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
            
            // Calculate α·V_raw
            List<Double> alphaVRaw = stateVectorCalculator.multiplyByScalar(vRaw, alpha);
            
            // Calculate (1-α)·S_prev
            double oneMinusAlpha = 1.0 - alpha;
            List<Double> oneMinusAlphaSPrev = stateVectorCalculator.multiplyByScalar(currentState, oneMinusAlpha);
            
            // Calculate S_new = α·V_raw + (1-α)·S_prev
            List<Double> newState = stateVectorCalculator.addVectors(alphaVRaw, oneMinusAlphaSPrev);
            
            Map<String, Double> afterState = extractDominantDimensions(newState);
            
            logger.info("Posting update applied",
                kv("beforeState", beforeState),
                kv("afterState", afterState),
                kv("alpha", alpha),
                kv("dimensionsChanged", calculateChangedDimensions(beforeState, afterState))
            );
            
            return newState;
            
        } catch (Exception e) {
            logger.error("Posting update failed",
                kv("error", e.getMessage()),
                kv("alpha", alpha),
                kv("errorType", e.getClass().getSimpleName())
            );
            throw new RuntimeException("Failed to apply posting update", e);
        }
    }
    
    /**
     * Apply interaction update rule: S_new = Ws·R′′ + (1-Ws)·S_prev
     * 
     * @param currentState S_prev - current psychological state
     * @param rPrime R′′ - modulated interaction vector
     * @param wsWeight Ws - interaction weight
     * @return Updated state S_new
     */
    public List<Double> applyInteractionUpdate(List<Double> currentState, List<Double> rPrime, double wsWeight) {
        try {
            logger.debug("Applying interaction update with Ws weight: {}", wsWeight);
            
            // Validate inputs
            validateStateUpdateInputs(currentState, rPrime);
            
            // Clamp Ws weight to valid range [0, 1]
            wsWeight = Math.max(0.0, Math.min(1.0, wsWeight));
            
            // Calculate Ws·R′′
            List<Double> wsRPrime = stateVectorCalculator.multiplyByScalar(rPrime, wsWeight);
            
            // Calculate (1-Ws)·S_prev
            double oneMinusWs = 1.0 - wsWeight;
            List<Double> oneMinusWsSPrev = stateVectorCalculator.multiplyByScalar(currentState, oneMinusWs);
            
            // Calculate S_new = Ws·R′′ + (1-Ws)·S_prev
            List<Double> newState = stateVectorCalculator.addVectors(wsRPrime, oneMinusWsSPrev);
            
            logger.debug("Successfully applied interaction update");
            return newState;
            
        } catch (Exception e) {
            logger.error("Error applying interaction update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply interaction update", e);
        }
    }
    
    /**
     * Apply modulated interaction update with full modulation formula:
     * R′′ = clip(R_raw × (1 + β·S_prev + μ·B))
     * S_new = Ws·R′′ + (1-Ws)·S_prev
     * 
     * @param currentState S_prev - current psychological state
     * @param rRaw R_raw - raw interaction vector
     * @param betaFactor β - user state amplification factor
     * @param muFactor μ - positive balance amplification factor
     * @param balanceVector B - system balance vector
     * @param wsWeight Ws - interaction weight
     * @return Updated state S_new
     */
    public List<Double> applyModulatedInteractionUpdate(List<Double> currentState, List<Double> rRaw,
                                                       double betaFactor, double muFactor,
                                                       List<Double> balanceVector, double wsWeight) {
        try {
            logger.debug("Applying modulated interaction update with β: {}, μ: {}, Ws: {}", 
                        betaFactor, muFactor, wsWeight);
            
            // Validate inputs
            validateStateUpdateInputs(currentState, rRaw);
            validateVector(balanceVector);
            
            // Calculate modulation factor: (1 + β·S_prev + μ·B)
            List<Double> betaSPrev = stateVectorCalculator.multiplyByScalar(currentState, betaFactor);
            List<Double> muB = stateVectorCalculator.multiplyByScalar(balanceVector, muFactor);
            List<Double> modulationFactor = stateVectorCalculator.addVectors(betaSPrev, muB);
            
            // Add 1.0 to each element
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                modulationFactor.set(i, modulationFactor.get(i) + 1.0);
            }
            
            // Calculate R_raw × (1 + β·S_prev + μ·B)
            List<Double> modulatedR = new ArrayList<>();
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                modulatedR.add(rRaw.get(i) * modulationFactor.get(i));
            }
            
            // Apply clipping: R′′ = clip(R_raw × (1 + β·S_prev + μ·B))
            List<Double> rPrime = stateVectorCalculator.applyClipping(modulatedR);
            
            // Apply interaction update: S_new = Ws·R′′ + (1-Ws)·S_prev
            List<Double> newState = applyInteractionUpdate(currentState, rPrime, wsWeight);
            
            logger.debug("Successfully applied modulated interaction update");
            return newState;
            
        } catch (Exception e) {
            logger.error("Error applying modulated interaction update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to apply modulated interaction update", e);
        }
    }
    
    /**
     * Calculate dynamic alpha based on intensity and base alpha
     * α = base_alpha + θ (with clamping)
     * 
     * @param baseAlpha Base alpha value
     * @param thetaFactor Theta factor from intensity calculation
     * @return Dynamic alpha value
     */
    public double calculateDynamicAlpha(double baseAlpha, double thetaFactor) {
        try {
            // α = base_alpha + θ
            double alpha = baseAlpha + thetaFactor;
            
            // Clamp to valid range
            alpha = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
            
            logger.debug("Calculated dynamic alpha: {} (base: {}, theta: {})", alpha, baseAlpha, thetaFactor);
            return alpha;
            
        } catch (Exception e) {
            logger.error("Error calculating dynamic alpha: {}", e.getMessage(), e);
            return DEFAULT_ALPHA;
        }
    }
    
    /**
     * Calculate interaction weight based on interaction type
     * 
     * @param interactionType Type of interaction
     * @return Interaction weight Ws
     */
    public double calculateInteractionWeight(String interactionType) {
        return switch (interactionType.toLowerCase()) {
            case "resonance" -> 0.2;
            case "linger" -> 0.01;
            case "comment" -> 0.15;
            case "share" -> 0.18;
            case "like" -> 0.05;
            case "view" -> 0.02;
            case "bookmark" -> 0.08;
            case "report" -> 0.12;
            default -> 0.1;
        };
    }
    
    /**
     * Calculate beta factor for user state amplification
     * 
     * @param userId User ID
     * @param currentState Current psychological state
     * @return Beta factor
     */
    public double calculateBetaFactor(UUID userId, List<Double> currentState) {
        try {
            // Get user personality profile for sensitivity analysis
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits that affect beta factor
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double conscientiousness = preferences != null ? (Double) preferences.getOrDefault("conscientiousness", 0.5) : 0.5;
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            
            // Get user's interaction patterns
            ComprehensiveInteractionAnalysis interactionAnalysis = interactionModulationService.getComprehensiveAnalysis(userId);
            double interactionIntensity = interactionAnalysis.getInteractionIntensity();
            
            // Get user's sensitivity level from LingerProcessor
            Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
            double averageLingerTime = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0);
            double sensitivityLevel = Math.min(1.0, averageLingerTime / 600.0); // Normalize to 10 minutes
            
            // Get user's resonance patterns
            Map<String, Object> resonanceStats = resonanceProcessor.getResonanceStatistics(userId);
            double resonanceSensitivity = (Double) resonanceStats.getOrDefault("averageResonanceScore", 0.5);
            
            // Calculate state magnitude as base indicator
            double stateMagnitude = stateVectorCalculator.calculateL2Norm(currentState);
            double baseBeta = Math.min(0.5, stateMagnitude / Math.sqrt(VECTOR_DIMENSIONS));
            
            // Apply personality-based adjustments
            double neuroticismAdjustment = neuroticism * 0.2; // Higher neuroticism = higher beta
            double conscientiousnessAdjustment = conscientiousness * 0.1; // Higher conscientiousness = higher beta
            double opennessAdjustment = (1.0 - openness) * 0.1; // Lower openness = higher beta
            double sensitivityAdjustment = sensitivityLevel * 0.3; // Higher sensitivity = higher beta
            double interactionAdjustment = interactionIntensity * 0.2; // Higher interaction = higher beta
            double resonanceAdjustment = resonanceSensitivity * 0.1; // Higher resonance = higher beta
            
            // Calculate total adjustment
            double totalAdjustment = neuroticismAdjustment + conscientiousnessAdjustment + 
                                   opennessAdjustment + sensitivityAdjustment + 
                                   interactionAdjustment + resonanceAdjustment;
            
            // Apply adjustment to base beta
            double adjustedBeta = baseBeta * (1.0 + totalAdjustment);
            
            // Clamp to valid range
            double finalBeta = Math.max(0.01, Math.min(0.5, adjustedBeta));
            
            logger.debug("Calculated beta factor: {} (base: {}, adjustment: {}) for user: {}", 
                        finalBeta, baseBeta, totalAdjustment, userId);
            return finalBeta;
            
        } catch (Exception e) {
            logger.error("Error calculating beta factor for user {}: {}", userId, e.getMessage(), e);
            return 0.1; // Default beta factor
        }
    }
    
    /**
     * Calculate mu factor for positive balance amplification
     * 
     * @param userId User ID
     * @return Mu factor
     */
    public double calculateMuFactor(UUID userId) {
        try {
            // Get user personality profile for therapeutic needs analysis
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits that affect mu factor
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double agreeableness = preferences != null ? (Double) preferences.getOrDefault("agreeableness", 0.5) : 0.5;
            double extraversion = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            
            // Get user's Buddhist factor analysis for therapeutic needs
            Map<String, Object> buddhistAnalysis = buddhistFactorAnalyzer.analyzeFactors("", userId);
            @SuppressWarnings("unchecked")
            Map<String, Double> factorScores = (Map<String, Double>) buddhistAnalysis.get("factorScores");
            
            double therapeuticNeed = 0.5;
            if (factorScores != null) {
                // Calculate therapeutic need based on negative factors
                double anger = factorScores.getOrDefault("anger", 0.0);
                double hatred = factorScores.getOrDefault("hatred", 0.0);
                double greed = factorScores.getOrDefault("greed", 0.0);
                double doubt = factorScores.getOrDefault("doubt", 0.0);
                double faith = factorScores.getOrDefault("faith", 0.0);
                double vigor = factorScores.getOrDefault("vigor", 0.0);
                
                // Higher negative factors and lower positive factors = higher therapeutic need
                double negativeFactorSum = anger + hatred + greed + doubt;
                double positiveFactorSum = faith + vigor;
                therapeuticNeed = (negativeFactorSum / 4.0) + (1.0 - positiveFactorSum / 2.0);
                therapeuticNeed = Math.max(0.0, Math.min(1.0, therapeuticNeed));
            }
            
            // Get user's interaction patterns for content preference analysis
            ComprehensiveInteractionAnalysis interactionAnalysis = interactionModulationService.getComprehensiveAnalysis(userId);
            double positiveContentPreference = 0.5; // Default value as ComprehensiveInteractionAnalysis doesn't have this field
            
            // Get user's historical psychological state patterns
            List<StateUpdateHistory> recentHistory = getStateHistory(userId, 10);
            double historicalNegativeRatio = calculateHistoricalNegativeRatio(recentHistory);
            
            // Calculate mu based on user characteristics and needs
            double baseMu = 0.05; // Base mu factor
            
            // Apply personality-based adjustments
            double neuroticismAdjustment = neuroticism * 0.3; // Higher neuroticism = higher mu
            double agreeablenessAdjustment = agreeableness * 0.2; // Higher agreeableness = higher mu
            double extraversionAdjustment = extraversion * 0.1; // Higher extraversion = higher mu
            double therapeuticAdjustment = therapeuticNeed * 0.4; // Higher therapeutic need = higher mu
            double historicalAdjustment = historicalNegativeRatio * 0.2; // Higher historical negative ratio = higher mu
            double preferenceAdjustment = positiveContentPreference * 0.1; // Higher positive preference = higher mu
            
            // Calculate total adjustment
            double totalAdjustment = neuroticismAdjustment + agreeablenessAdjustment + 
                                   extraversionAdjustment + therapeuticAdjustment + 
                                   historicalAdjustment + preferenceAdjustment;
            
            // Apply adjustment to base mu
            double adjustedMu = baseMu * (1.0 + totalAdjustment);
            
            // Clamp to valid range
            double finalMu = Math.max(0.01, Math.min(0.3, adjustedMu));
            
            logger.debug("Calculated mu factor: {} (base: {}, adjustment: {}) for user: {}", 
                        finalMu, baseMu, totalAdjustment, userId);
            return finalMu;
            
        } catch (Exception e) {
            logger.error("Error calculating mu factor for user {}: {}", userId, e.getMessage(), e);
            return 0.05; // Default mu factor
        }
    }
    
    /**
     * Calculate historical negative ratio from state update history
     */
    private double calculateHistoricalNegativeRatio(List<StateUpdateHistory> history) {
        try {
            if (history == null || history.isEmpty()) {
                return 0.5; // Default ratio
            }
            
            int negativeUpdates = 0;
            int totalUpdates = history.size();
            
            for (StateUpdateHistory update : history) {
                if (update.getNewState() != null && update.getNewState().size() >= 43) {
                    // Check if negative factors (dimensions 16-42) are dominant
                    double negativeSum = 0.0;
                    double positiveSum = 0.0;
                    
                    for (int i = 16; i < 43 && i < update.getNewState().size(); i++) {
                        negativeSum += Math.abs(update.getNewState().get(i));
                    }
                    
                    for (int i = 0; i < 16 && i < update.getNewState().size(); i++) {
                        positiveSum += Math.abs(update.getNewState().get(i));
                    }
                    
                    if (negativeSum > positiveSum) {
                        negativeUpdates++;
                    }
                }
            }
            
            return totalUpdates > 0 ? (double) negativeUpdates / totalUpdates : 0.5;
            
        } catch (Exception e) {
            logger.warn("Error calculating historical negative ratio: {}", e.getMessage());
            return 0.5;
        }
    }
    
    /**
     * Get state history for user
     */
    private List<StateUpdateHistory> getStateHistory(UUID userId, int limit) {
        try {
            // This would typically use a repository to get state history
            // For now, return empty list as we don't have direct access to the repository
            return new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Error getting state history for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Validate state update inputs
     * 
     * @param currentState Current state vector
     * @param inputVector Input vector (V_raw or R_raw)
     */
    private void validateStateUpdateInputs(List<Double> currentState, List<Double> inputVector) {
        if (currentState == null || currentState.size() != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Current state must have " + VECTOR_DIMENSIONS + " dimensions");
        }
        if (inputVector == null || inputVector.size() != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Input vector must have " + VECTOR_DIMENSIONS + " dimensions");
        }
    }
    
    /**
     * Validate vector dimensions
     * 
     * @param vector Vector to validate
     */
    private void validateVector(List<Double> vector) {
        if (vector == null || vector.size() != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("Vector must have " + VECTOR_DIMENSIONS + " dimensions");
        }
    }
    
    /**
     * Apply enhanced posting update with AI analysis
     */
    public List<Double> applyEnhancedPostingUpdate(List<Double> currentState, String postContent, UUID userId) {
        try {
            logger.debug("Applying enhanced posting update for user {}", userId);
            
            // Check cache first
            String cacheKey = UPDATE_CACHE_KEY + userId + ":" + postContent.hashCode();
            @SuppressWarnings("unchecked")
            List<Double> cachedResult = (List<Double>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                logger.debug("Using cached enhanced posting update");
                return cachedResult;
            }
            
            // Generate V_raw using PsychologicalVectorGenerator
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(postContent, userId);
            @SuppressWarnings("unchecked")
            List<Double> vRaw = (List<Double>) vectorResult.get("psychologicalVector");
            
            if (vRaw == null || vRaw.size() != VECTOR_DIMENSIONS) {
                logger.warn("Invalid V_raw generated, using default");
                vRaw = createDefaultVector();
            }
            
            // Calculate enhanced alpha using multiple services
            double enhancedAlpha = calculateEnhancedAlpha(postContent, userId, vRaw);
            
            // Apply posting update rule
            List<Double> newState = applyPostingUpdate(currentState, vRaw, enhancedAlpha);
            
            // Apply AI analysis adjustments
            newState = applyAIAnalysisAdjustments(newState, postContent, userId);
            
            // Apply personality-based adjustments
            newState = applyPersonalityAdjustments(newState, userId);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, newState, 1, TimeUnit.HOURS);
            
            logger.debug("Applied enhanced posting update with alpha: {}", enhancedAlpha);
            return newState;
            
        } catch (Exception e) {
            logger.error("Error applying enhanced posting update: {}", e.getMessage(), e);
            return currentState;
        }
    }
    
    /**
     * Calculate enhanced alpha using multiple services
     */
    private double calculateEnhancedAlpha(String postContent, UUID userId, List<Double> vRaw) {
        try {
            // Check cache first
            String cacheKey = ALPHA_CACHE_KEY + userId + ":" + postContent.hashCode();
            Double cachedAlpha = (Double) redisTemplate.opsForValue().get(cacheKey);
            if (cachedAlpha != null) {
                return cachedAlpha;
            }
            
            // Get base alpha from user personality
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double baseAlpha = preferences != null ? (Double) preferences.getOrDefault("base_alpha", DEFAULT_ALPHA) : DEFAULT_ALPHA;
            
            // Calculate intensity factor using IntensityFactorCalculator
            double intensityFactor = intensityFactorCalculator.calculateIntensityFactor(postContent, userId, vRaw);
            
            // Use AI analysis for additional insights
            Map<String, Object> aiAnalysis = aiPostAnalysisService.processPsychologicalContent(postContent, userId);
            double aiIntensity = extractAIIntensity(aiAnalysis);
            
            // Calculate enhanced alpha
            double enhancedAlpha = baseAlpha + (intensityFactor * 0.6) + (aiIntensity * 0.4);
            enhancedAlpha = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, enhancedAlpha));
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, enhancedAlpha, 30, TimeUnit.MINUTES);
            
            logger.debug("Calculated enhanced alpha: {} (base: {}, intensity: {}, ai: {})", 
                        enhancedAlpha, baseAlpha, intensityFactor, aiIntensity);
            
            return enhancedAlpha;
            
        } catch (Exception e) {
            logger.warn("Error calculating enhanced alpha: {}", e.getMessage());
            return DEFAULT_ALPHA;
        }
    }
    
    /**
     * Apply AI analysis adjustments to state
     */
    private List<Double> applyAIAnalysisAdjustments(List<Double> state, String postContent, UUID userId) {
        try {
            // Get AI analysis
            Map<String, Object> aiAnalysis = aiPostAnalysisService.processPsychologicalContent(postContent, userId);
            
            // Extract processed vector if available
            Map<String, Object> processedVector = (Map<String, Object>) aiAnalysis.get("processedVector");
            if (processedVector != null) {
                @SuppressWarnings("unchecked")
                List<Double> processedVectorData = (List<Double>) processedVector.get("processedVector");
                if (processedVectorData != null && processedVectorData.size() == state.size()) {
                    // Blend with processed vector (15% weight)
                    for (int i = 0; i < state.size(); i++) {
                        state.set(i, state.get(i) * 0.85 + processedVectorData.get(i) * 0.15);
                    }
                }
            }
            
            return state;
            
        } catch (Exception e) {
            logger.warn("Error applying AI analysis adjustments: {}", e.getMessage());
            return state;
        }
    }
    
    /**
     * Apply personality-based adjustments to state
     */
    private List<Double> applyPersonalityAdjustments(List<Double> state, UUID userId) {
        try {
            // Get user personality profile
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double extraversion = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            
            // Apply personality-based adjustments
            List<Double> adjustedState = new ArrayList<>(state);
            
            // Openness affects cognitive dimensions (0-4)
            for (int i = 0; i < 5; i++) {
                adjustedState.set(i, adjustedState.get(i) * (1.0 + openness * 0.1));
            }
            
            // Neuroticism affects emotional dimensions (1-2, 7-8)
            List<Integer> emotionalDims = List.of(1, 2, 7, 8);
            for (int dim : emotionalDims) {
                if (dim < adjustedState.size()) {
                    adjustedState.set(dim, adjustedState.get(dim) * (1.0 + neuroticism * 0.15));
                }
            }
            
            // Extraversion affects social dimensions (5-6)
            for (int i = 5; i < 7; i++) {
                if (i < adjustedState.size()) {
                    adjustedState.set(i, adjustedState.get(i) * (1.0 + extraversion * 0.1));
                }
            }
            
            return adjustedState;
            
        } catch (Exception e) {
            logger.warn("Error applying personality adjustments: {}", e.getMessage());
            return state;
        }
    }
    
    /**
     * Extract AI intensity from analysis result
     */
    private double extractAIIntensity(Map<String, Object> aiAnalysis) {
        try {
            Map<String, Object> intensityFactor = (Map<String, Object>) aiAnalysis.get("intensityFactor");
            if (intensityFactor != null) {
                Double intensityScore = (Double) intensityFactor.get("intensityScore");
                if (intensityScore != null) {
                    return intensityScore;
                }
            }
            return 0.5; // Default intensity
        } catch (Exception e) {
            logger.warn("Error extracting AI intensity: {}", e.getMessage());
            return 0.5;
        }
    }
    
    /**
     * Create default vector
     */
    private List<Double> createDefaultVector() {
        List<Double> defaultVector = new ArrayList<>();
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            defaultVector.add(0.0);
        }
        return defaultVector;
    }
    
    /**
     * Extract dominant dimensions from state vector for logging
     * Maps 43-dimensional Buddhist mental factors to meaningful psychological dimensions
     */
    private Map<String, Double> extractDominantDimensions(List<Double> state) {
        Map<String, Double> dominant = new LinkedHashMap<>();
        try {
            if (state == null || state.size() < VECTOR_DIMENSIONS) {
                logger.warn("Invalid state vector: null or insufficient dimensions (expected {}, got {})", 
                           VECTOR_DIMENSIONS, state != null ? state.size() : 0);
                return dominant;
            }
            
            // Map key psychological dimensions to meaningful names
            // Based on Buddhist mental factors from PsychologicalVectorGenerator
            Map<Integer, String> keyDimensions = new HashMap<>();
            keyDimensions.put(0, "attention");           // mindful awareness and focus
            keyDimensions.put(1, "contact");             // sensory and mental contact  
            keyDimensions.put(2, "feeling");             // emotional sensations
            keyDimensions.put(3, "thinking");            // cognitive processes
            keyDimensions.put(4, "deliberation");        // careful consideration
            keyDimensions.put(16, "greed");              // desire and craving
            keyDimensions.put(17, "anger");              // rage and hostility
            keyDimensions.put(18, "arrogance");          // pride and conceit
            keyDimensions.put(19, "foolishness");        // ignorance and unwisdom
            keyDimensions.put(20, "doubt");              // uncertainty and confusion
            keyDimensions.put(30, "jealousy");           // envy and resentment
            keyDimensions.put(37, "torpor");             // dullness and lethargy
            keyDimensions.put(38, "restlessness");       // agitation and anxiety
            keyDimensions.put(39, "distraction");        // scatteredness and confusion
            keyDimensions.put(42, "regret");             // remorse and sorrow
            
            // Extract values for key dimensions and sort by magnitude
            Map<String, Double> dimensionValues = new HashMap<>();
            for (Map.Entry<Integer, String> entry : keyDimensions.entrySet()) {
                int index = entry.getKey();
                String name = entry.getValue();
                if (index < state.size()) {
                    dimensionValues.put(name, state.get(index));
                }
            }
            
            // Sort by absolute value (descending) and take top 5
            dimensionValues.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(Math.abs(e2.getValue()), Math.abs(e1.getValue())))
                .limit(5)
                .forEach(entry -> dominant.put(entry.getKey(), entry.getValue()));
            
            logger.debug("Extracted dominant psychological dimensions: {}", dominant);
            
        } catch (Exception e) {
            logger.warn("Error extracting dominant dimensions: {}", e.getMessage());
        }
        return dominant;
    }
    
    /**
     * Calculate the number of changed dimensions between before and after states
     */
    private int calculateChangedDimensions(Map<String, Double> before, Map<String, Double> after) {
        if (before == null || after == null) {
            return 0;
        }
        
        int changed = 0;
        for (Map.Entry<String, Double> entry : before.entrySet()) {
            String key = entry.getKey();
            Double beforeValue = entry.getValue();
            Double afterValue = after.get(key);
            
            if (afterValue == null || Math.abs(beforeValue - afterValue) > 0.001) {
                changed++;
            }
        }
        
        return changed;
    }
}
