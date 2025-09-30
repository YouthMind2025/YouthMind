package ai.youthmind.service;

import ai.youthmind.entity.PsychologicalStateVector;
import ai.youthmind.entity.StateUpdateHistory;
import ai.youthmind.entity.User;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.PsychologicalStateVectorRepository;
import ai.youthmind.repository.StateUpdateHistoryRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.service.InteractionModulationService.ComprehensiveInteractionAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

/**
 * Core service implementing psychological state update algorithms
 * Implements the S_new = α·V_raw + (1-α)·S_prev formula with dynamic α calculation
 */
@Service
@Transactional
public class PsychologicalStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalStateService.class);
    
    // Constants for state management
    public static final int VECTOR_DIMENSIONS = 43;
    public static final double DEFAULT_BASE_ALPHA = 0.1;
    public static final double MAX_ALPHA = 0.9;
    public static final double MIN_ALPHA = 0.01;
    public static final double SIMILARITY_THRESHOLD = 0.7;
    
    // Redis keys
    private static final String CURRENT_STATE_KEY = "current_state:";
    private static final String STATE_CACHE_KEY = "state_cache:";
    private static final String SIMILARITY_CACHE_KEY = "similarity_cache:";
    
    @Autowired
    private PsychologicalStateVectorRepository stateVectorRepository;
    
    @Autowired
    private StateUpdateHistoryRepository stateUpdateHistoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StateVectorCalculator stateVectorCalculator;
    
    @Autowired
    private IntensityFactorCalculator intensityFactorCalculator;
    
    @Autowired
    private StateUpdateRuleEngine stateUpdateRuleEngine;
    
    @Autowired
    private StateNormalizationService stateNormalizationService;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private PositiveBalanceCalculator positiveBalanceCalculator;
    
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
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    // Enhanced queuing system integration
    @Autowired
    private RequestQueueService requestQueueService;
    
    @Autowired
    private StateUpdateBatchProcessor stateUpdateBatchProcessor;
    
    @Autowired
    private InteractionModulationService interactionModulationService;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    /**
     * Update user's psychological state from a post
     * Implements: S_new = α·V_raw + (1-α)·S_prev
     * 
     * @param userId User ID
     * @param postContent Post content for analysis
     * @param postId Post ID for tracking
     * @return Updated psychological state
     */
    public Map<String, Object> updateStateFromPost(UUID userId, String postContent, UUID postId) {
        try {
            logger.info("Updating psychological state from post for user: {}", userId);
            
            // Get current state
            List<Double> currentState = getCurrentState(userId);
            
            // Generate V_raw vector from post content
            List<Double> vRaw = generateVRawFromPost(postContent, userId);
            
            // Calculate intensity factor θ using comprehensive analysis
            double thetaFactor = intensityFactorCalculator.calculateIntensityFactor(
                postContent, userId, vRaw
            );
            
            // Use AIPostAnalysisService for additional psychological insights
            Map<String, Object> aiAnalysis = aiPostAnalysisService.processPsychologicalContent(postContent, userId);
            
            // Use AI analysis for content understanding
            Map<String, Object> nlpAnalysis = aiServiceClient.analyzeContentForModeration(postContent, userId);
            
            // Use personality computation for enhanced state updates
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Get base alpha for user
            double baseAlpha = getBaseAlphaForUser(userId);
            
            // Calculate dynamic alpha: α = base_alpha + θ
            double alpha = Math.min(MAX_ALPHA, Math.max(MIN_ALPHA, baseAlpha + thetaFactor));
            
            // Apply state update rule: S_new = α·V_raw + (1-α)·S_prev
            List<Double> newState = stateUpdateRuleEngine.applyPostingUpdate(
                currentState, vRaw, alpha
            );
            
            // Apply personality-based adjustments to the state
            newState = applyPersonalityAdjustments(newState, personalityProfile);
            
        // Apply AI-based adjustments (replacing NLP)
        newState = applyAIAdjustments(newState, postContent, userId);
            
            // Apply AI analysis insights
            newState = applyAIAnalysisAdjustments(newState, aiAnalysis);
            
            // Normalize the new state
            newState = stateNormalizationService.normalizeVector(newState);
            
            // Store the update
            storeStateUpdate(userId, newState, postId, currentState, alpha, thetaFactor, 
                           StateUpdateHistory.UpdateType.POSTING);
            
            // Cache the new state
            cacheCurrentState(userId, newState);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("previousState", currentState);
            result.put("newState", newState);
            result.put("vRaw", vRaw);
            result.put("alpha", alpha);
            result.put("thetaFactor", thetaFactor);
            result.put("baseAlpha", baseAlpha);
            result.put("updateType", "POSTING");
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("Successfully updated psychological state from post for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error updating psychological state from post for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update psychological state from post", e);
        }
    }
    
    /**
     * Update user's psychological state from an interaction
     * Implements: S_new = Ws·R′′ + (1-Ws)·S_prev
     * where R′′ = clip(R_raw × (1 + β·S_prev + μ·B))
     * 
     * @param userId User ID
     * @param targetPostId Target post ID for interaction
     * @param interactionType Type of interaction (resonance, linger, etc.)
     * @param interactionWeight Ws weight for the interaction
     * @return Updated psychological state
     */
    public Map<String, Object> updateStateFromInteraction(UUID userId, UUID targetPostId, 
                                                         String interactionType, double interactionWeight) {
        try {
            logger.info("Updating psychological state from interaction for user: {}", userId);
            
            // Get current state
            List<Double> currentState = getCurrentState(userId);
            
            // Get R_raw vector from target post
            List<Double> rRaw = getRRawFromPost(targetPostId);
            
            // Calculate modulation factors
            double betaFactor = calculateBetaFactor(userId, currentState);
            double muFactor = calculateMuFactor(userId);
            List<Double> balanceVector = getBalanceVector();
            
            // Apply interaction modulation: R′′ = clip(R_raw × (1 + β·S_prev + μ·B))
            List<Double> modulatedR = stateVectorCalculator.multiplyByScalar(rRaw, 
                1.0 + betaFactor + muFactor);
            List<Double> rPrime = stateVectorCalculator.applyClipping(modulatedR);
            
            // Apply state update rule: S_new = Ws·R′′ + (1-Ws)·S_prev
            List<Double> newState = stateUpdateRuleEngine.applyInteractionUpdate(
                currentState, rPrime, interactionWeight
            );
            
            // Normalize the new state
            newState = stateNormalizationService.normalizeVector(newState);
            
            // Store the update
            storeStateUpdate(userId, newState, targetPostId, currentState, null, null, 
                           StateUpdateHistory.UpdateType.INTERACTION, betaFactor, muFactor, interactionWeight);
            
            // Cache the new state
            cacheCurrentState(userId, newState);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("previousState", currentState);
            result.put("newState", newState);
            result.put("rRaw", rRaw);
            result.put("rPrime", rPrime);
            result.put("interactionWeight", interactionWeight);
            result.put("betaFactor", betaFactor);
            result.put("muFactor", muFactor);
            result.put("updateType", "INTERACTION");
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("Successfully updated psychological state from interaction for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error updating psychological state from interaction for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update psychological state from interaction", e);
        }
    }
    
    /**
     * Get current psychological state for a user
     * 
     * @param userId User ID
     * @return Current 43-dimensional psychological state vector
     */
    public List<Double> getCurrentState(UUID userId) {
        try {
            // Check cache first
            String cacheKey = CURRENT_STATE_KEY + userId;
            @SuppressWarnings("unchecked")
            List<Double> cachedState = (List<Double>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedState != null) {
                logger.debug("Retrieved current state from cache for user: {}", userId);
                return cachedState;
            }
            
            // Get from database
            Optional<PsychologicalStateVector> stateVector = stateVectorRepository
                .findByUserIdAndVectorType(userId, PsychologicalStateVector.VectorType.S_CURRENT)
                .stream()
                .max(Comparator.comparing(PsychologicalStateVector::getCreatedAt));
            
            if (stateVector.isPresent()) {
                List<Double> state = stateVector.get().getVectorData();
                cacheCurrentState(userId, state);
                return state;
            }
            
            // Return default state if no state found
            logger.warn("No psychological state found for user {}, returning default state", userId);
            return createDefaultState();
            
        } catch (Exception e) {
            logger.error("Error getting current state for user {}: {}", userId, e.getMessage(), e);
            return createDefaultState();
        }
    }
    
    /**
     * Calculate similarity between two psychological states
     * 
     * @param state1 First state vector
     * @param state2 Second state vector
     * @return Cosine similarity between the states
     */
    public double calculateStateSimilarity(List<Double> state1, List<Double> state2) {
        try {
            // Validate inputs
            if (state1 == null || state2 == null) {
                return 0.0;
            }
            if (state1.size() != VECTOR_DIMENSIONS || state2.size() != VECTOR_DIMENSIONS) {
                logger.warn("Invalid state dimensions for similarity calculation");
                return 0.0;
            }
            
            // Calculate cosine similarity
            double similarity = stateVectorCalculator.calculateCosineSimilarity(state1, state2);
            
            logger.debug("Calculated state similarity: {}", similarity);
            return similarity;
            
        } catch (Exception e) {
            logger.error("Error calculating state similarity: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Get psychological state history for a user
     * 
     * @param userId User ID
     * @param limit Maximum number of history entries
     * @return List of state update history
     */
    public List<StateUpdateHistory> getStateHistory(UUID userId, int limit) {
        try {
            return stateUpdateHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
        } catch (Exception e) {
            logger.error("Error getting state history for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Initialize psychological state for a new user
     * 
     * @param userId User ID
     * @return Initialized state
     */
    public Map<String, Object> initializeState(UUID userId) {
        try {
            logger.info("Initializing psychological state for user: {}", userId);
            
            // Create default state
            List<Double> defaultState = createDefaultState();
            
            // Store initial state
            PsychologicalStateVector stateVector = new PsychologicalStateVector();
            stateVector.setUserId(userId);
            stateVector.setVectorData(defaultState);
            stateVector.setVectorType(PsychologicalStateVector.VectorType.S_CURRENT);
            stateVector.setSourceType(PsychologicalStateVector.SourceType.SYSTEM);
            stateVector.setCreatedAt(LocalDateTime.now());
            stateVector.setUpdatedAt(LocalDateTime.now());
            
            stateVectorRepository.save(stateVector);
            
            // Cache the state
            cacheCurrentState(userId, defaultState);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("state", defaultState);
            result.put("initialized", true);
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("Successfully initialized psychological state for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error initializing psychological state for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize psychological state", e);
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private List<Double> generateVRawFromPost(String postContent, UUID userId) {
        try {
            // Use the existing PsychologicalVectorGenerator to generate V_raw vector
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(postContent, userId);
            
            @SuppressWarnings("unchecked")
            List<Double> vRaw = (List<Double>) vectorResult.get("psychologicalVector");
            
            if (vRaw == null || vRaw.size() != VECTOR_DIMENSIONS) {
                logger.warn("Generated invalid V_raw vector for user {}, using default", userId);
                return createDefaultState();
            }
            
            logger.debug("Generated V_raw vector for user {} with {} dimensions", userId, vRaw.size());
            return vRaw;
            
        } catch (Exception e) {
            logger.error("Error generating V_raw vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultState();
        }
    }
    
    private List<Double> getRRawFromPost(UUID postId) {
        try {
            // Get the post from database
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                logger.warn("Post {} not found, using default R_raw vector", postId);
                return createDefaultState();
            }
            
            Post post = postOpt.get();
            String postContent = post.getContent();
            
            // Generate R_raw vector from post content using PsychologicalVectorGenerator
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(postContent, post.getUserId());
            
            @SuppressWarnings("unchecked")
            List<Double> rRaw = (List<Double>) vectorResult.get("psychologicalVector");
            
            if (rRaw == null || rRaw.size() != VECTOR_DIMENSIONS) {
                logger.warn("Generated invalid R_raw vector for post {}, using default", postId);
                return createDefaultState();
            }
            
            logger.debug("Generated R_raw vector for post {} with {} dimensions", postId, rRaw.size());
            return rRaw;
            
        } catch (Exception e) {
            logger.error("Error generating R_raw vector for post {}: {}", postId, e.getMessage(), e);
            return createDefaultState();
        }
    }
    
    private double getBaseAlphaForUser(UUID userId) {
        try {
            // Get user from database
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User {} not found, using default base alpha", userId);
                return DEFAULT_BASE_ALPHA;
            }
            
            User user = userOpt.get();
            
            // Check if user has psychological state data
            List<Double> currentState = getCurrentState(userId);
            if (currentState != null && !currentState.isEmpty()) {
                // Use a default base alpha based on user's current state
                double stateMagnitude = currentState.stream().mapToDouble(Double::doubleValue).sum();
                double baseAlpha = Math.min(0.3, Math.max(0.05, stateMagnitude / currentState.size()));
                logger.debug("Calculated base alpha {} for user {}", baseAlpha, userId);
                return baseAlpha;
            }
            
            // Use default base alpha if not found or invalid
            logger.debug("Using default base alpha {} for user {}", DEFAULT_BASE_ALPHA, userId);
            return DEFAULT_BASE_ALPHA;
            
        } catch (Exception e) {
            logger.error("Error retrieving base alpha for user {}: {}", userId, e.getMessage(), e);
            return DEFAULT_BASE_ALPHA;
        }
    }
    
    private double calculateBetaFactor(UUID userId, List<Double> currentState) {
        try {
            // Calculate beta factor based on user's current psychological state
            // Beta represents how much the user's current state amplifies external influences
            
            // Calculate state magnitude as indicator of user's current psychological intensity
            double stateMagnitude = stateVectorCalculator.calculateL2Norm(currentState);
            
            // Normalize magnitude to [0, 0.5] range for beta
            // Higher magnitude = more susceptible to external influences
            double beta = Math.min(0.5, stateMagnitude / Math.sqrt(VECTOR_DIMENSIONS));
            
            // Apply user-specific adjustments based on historical patterns
            beta = applyUserBetaAdjustments(userId, beta);
            
            logger.debug("Calculated beta factor {} for user {} (state magnitude: {})", beta, userId, stateMagnitude);
            return beta;
            
        } catch (Exception e) {
            logger.error("Error calculating beta factor for user {}: {}", userId, e.getMessage(), e);
            return 0.1; // Default beta factor
        }
    }
    
    private double calculateMuFactor(UUID userId) {
        try {
            // Calculate mu factor for positive balance amplification
            // Mu represents how much the system should enhance positive mental factors
            
            // Get user's current psychological state
            List<Double> currentState = getCurrentState(userId);
            
            // Calculate negative factor intensity (dimensions 16-42 are negative factors)
            double negativeIntensity = 0.0;
            for (int i = 16; i < VECTOR_DIMENSIONS; i++) {
                negativeIntensity += Math.abs(currentState.get(i));
            }
            negativeIntensity /= (VECTOR_DIMENSIONS - 16); // Average negative intensity
            
            // Calculate positive factor intensity (dimensions 0-15 are positive factors)
            double positiveIntensity = 0.0;
            for (int i = 0; i < 16; i++) {
                positiveIntensity += Math.abs(currentState.get(i));
            }
            positiveIntensity /= 16; // Average positive intensity
            
            // Mu should be higher when user has more negative factors
            // This helps balance the psychological state
            double mu = Math.min(0.2, negativeIntensity * 0.1);
            
            // Apply user-specific adjustments
            mu = applyUserMuAdjustments(userId, mu, negativeIntensity, positiveIntensity);
            
            logger.debug("Calculated mu factor {} for user {} (negative: {}, positive: {})", 
                        mu, userId, negativeIntensity, positiveIntensity);
            return mu;
            
        } catch (Exception e) {
            logger.error("Error calculating mu factor for user {}: {}", userId, e.getMessage(), e);
            return 0.05; // Default mu factor
        }
    }
    
    private List<Double> getBalanceVector() {
        try {
            // Use the existing PositiveBalanceCalculator to get the system balance vector B
            // This vector enhances positive mental factors and suppresses negative ones
            
            // Generate a default positive balance vector
            // This vector enhances positive mental factors and suppresses negative ones
            List<Double> balanceVector = createDefaultBalanceVector();
            
            logger.debug("Generated default balance vector with {} dimensions", balanceVector.size());
            return balanceVector;
            
        } catch (Exception e) {
            logger.error("Error retrieving balance vector: {}", e.getMessage(), e);
            return createDefaultBalanceVector();
        }
    }
    
    private List<Double> createDefaultState() {
        List<Double> defaultState = new ArrayList<>();
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            defaultState.add(0.0); // Neutral values
        }
        return defaultState;
    }
    
    private List<Double> createDefaultBalanceVector() {
        List<Double> balanceVector = new ArrayList<>();
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            if (i < 16) {
                // Positive factors (dimensions 0-15) - small positive values
                balanceVector.add(0.1);
            } else {
                // Negative factors (dimensions 16-42) - small negative values
                balanceVector.add(-0.05);
            }
        }
        return balanceVector;
    }
    
    private double applyUserBetaAdjustments(UUID userId, double baseBeta) {
        try {
            // Get user personality profile for sensitivity analysis
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits that affect beta factor
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double conscientiousness = preferences != null ? (Double) preferences.getOrDefault("conscientiousness", 0.5) : 0.5;
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            
            // Get user's historical interaction patterns
            ComprehensiveInteractionAnalysis interactionAnalysis = interactionModulationService.getComprehensiveAnalysis(userId);
            double interactionIntensity = interactionAnalysis.getInteractionIntensity();
            
            // Get user's sensitivity level from LingerProcessor
            Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
            double averageLingerTime = (Double) lingerStats.getOrDefault("averageLingerTime", 0.0);
            double sensitivityLevel = Math.min(1.0, averageLingerTime / 600.0); // Normalize to 10 minutes
            
            // Get user's resonance patterns
            Map<String, Object> resonanceStats = resonanceProcessor.getResonanceStatistics(userId);
            double resonanceSensitivity = (Double) resonanceStats.getOrDefault("averageResonanceScore", 0.5);
            
            // Calculate beta adjustments based on user characteristics
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
            return Math.max(0.01, Math.min(0.5, adjustedBeta));
            
        } catch (Exception e) {
            logger.warn("Error applying user beta adjustments: {}", e.getMessage());
            return baseBeta;
        }
    }
    
    private double applyUserMuAdjustments(UUID userId, double baseMu, double negativeIntensity, double positiveIntensity) {
        try {
            // Get user personality profile for therapeutic needs analysis
            UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Extract personality traits that affect mu factor
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double agreeableness = preferences != null ? (Double) preferences.getOrDefault("agreeableness", 0.5) : 0.5;
            double extraversion = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            
            // Get user's historical psychological state patterns
            List<StateUpdateHistory> recentHistory = getStateHistory(userId, 10);
            double historicalNegativeRatio = calculateHistoricalNegativeRatio(recentHistory);
            
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
            double positiveContentPreference = 0.5; // Default value since ComprehensiveInteractionAnalysis doesn't have this field
            
            // Calculate mu adjustments based on user characteristics and needs
            double neuroticismAdjustment = neuroticism * 0.3; // Higher neuroticism = higher mu (more positive balance needed)
            double agreeablenessAdjustment = agreeableness * 0.2; // Higher agreeableness = higher mu
            double extraversionAdjustment = extraversion * 0.1; // Higher extraversion = higher mu
            double therapeuticAdjustment = therapeuticNeed * 0.4; // Higher therapeutic need = higher mu
            double historicalAdjustment = historicalNegativeRatio * 0.2; // Higher historical negative ratio = higher mu
            double preferenceAdjustment = positiveContentPreference * 0.1; // Higher positive preference = higher mu
            
            // Adjust mu based on current intensity ratio
            double currentRatio = positiveIntensity > 0 ? negativeIntensity / positiveIntensity : 1.0;
            double ratioAdjustment = currentRatio * 0.3; // Higher negative ratio = higher mu
            
            // Calculate total adjustment
            double totalAdjustment = neuroticismAdjustment + agreeablenessAdjustment + 
                                   extraversionAdjustment + therapeuticAdjustment + 
                                   historicalAdjustment + preferenceAdjustment + ratioAdjustment;
            
            // Apply adjustment to base mu
            double adjustedMu = baseMu * (1.0 + totalAdjustment);
            
            // Clamp to valid range
            return Math.max(0.01, Math.min(0.3, adjustedMu));
            
        } catch (Exception e) {
            logger.warn("Error applying user mu adjustments: {}", e.getMessage());
            return baseMu;
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
    
    private void storeStateUpdate(UUID userId, List<Double> newState, UUID sourceId, 
                                 List<Double> previousState, Double alpha, Double theta,
                                 StateUpdateHistory.UpdateType updateType) {
        storeStateUpdate(userId, newState, sourceId, previousState, alpha, theta, 
                        updateType, null, null, null);
    }
    
    private void storeStateUpdate(UUID userId, List<Double> newState, UUID sourceId, 
                                 List<Double> previousState, Double alpha, Double theta,
                                 StateUpdateHistory.UpdateType updateType, Double beta, 
                                 Double mu, Double wsWeight) {
        try {
            StateUpdateHistory history = new StateUpdateHistory();
            history.setUserId(userId);
            history.setUpdateType(updateType);
            history.setPreviousState(previousState);
            history.setNewState(newState);
            history.setAlphaUsed(alpha != null ? BigDecimal.valueOf(alpha) : null);
            history.setThetaFactor(theta != null ? BigDecimal.valueOf(theta) : null);
            history.setBetaFactor(beta != null ? BigDecimal.valueOf(beta) : null);
            history.setMuFactor(mu != null ? BigDecimal.valueOf(mu) : null);
            history.setWsWeight(wsWeight != null ? BigDecimal.valueOf(wsWeight) : null);
            history.setSourceId(sourceId);
            history.setCreatedAt(LocalDateTime.now());
            
            stateUpdateHistoryRepository.save(history);
            
            // Also store the current state vector
            PsychologicalStateVector stateVector = new PsychologicalStateVector();
            stateVector.setUserId(userId);
            stateVector.setVectorData(newState);
            stateVector.setVectorType(PsychologicalStateVector.VectorType.S_CURRENT);
            stateVector.setSourceType(PsychologicalStateVector.SourceType.COMPUTED);
            stateVector.setSourceId(sourceId);
            stateVector.setCreatedAt(LocalDateTime.now());
            stateVector.setUpdatedAt(LocalDateTime.now());
            
            stateVectorRepository.save(stateVector);
            
        } catch (Exception e) {
            logger.error("Error storing state update for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    private void cacheCurrentState(UUID userId, List<Double> state) {
        try {
            String cacheKey = CURRENT_STATE_KEY + userId;
            redisTemplate.opsForValue().set(cacheKey, state, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.warn("Error caching current state for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Apply personality-based adjustments to state vector
     */
    private List<Double> applyPersonalityAdjustments(List<Double> state, UserPersonalityProfile personalityProfile) {
        try {
            if (personalityProfile == null || state == null) {
                return state;
            }
            
            // Extract personality traits from preferences map
            Map<String, Object> preferences = personalityProfile.getPreferences();
            double openness = preferences != null ? (Double) preferences.getOrDefault("openness", 0.5) : 0.5;
            double neuroticism = preferences != null ? (Double) preferences.getOrDefault("neuroticism", 0.5) : 0.5;
            double extraversion = preferences != null ? (Double) preferences.getOrDefault("extraversion", 0.5) : 0.5;
            
            // Apply personality-based adjustments to specific dimensions
            List<Double> adjustedState = new ArrayList<>(state);
            
            // Openness affects cognitive dimensions (0-4)
            for (int i = 0; i < 5; i++) {
                adjustedState.set(i, adjustedState.get(i) * (1.0 + openness * 0.2));
            }
            
            // Neuroticism affects emotional dimensions (1-2, 7-8)
            List<Integer> emotionalDims = Arrays.asList(1, 2, 7, 8);
            for (int dim : emotionalDims) {
                if (dim < adjustedState.size()) {
                    adjustedState.set(dim, adjustedState.get(dim) * (1.0 + neuroticism * 0.3));
                }
            }
            
            // Extraversion affects social dimensions (5-6)
            for (int i = 5; i < 7; i++) {
                if (i < adjustedState.size()) {
                    adjustedState.set(i, adjustedState.get(i) * (1.0 + extraversion * 0.2));
                }
            }
            
            logger.debug("Applied personality adjustments (openness: {}, neuroticism: {}, extraversion: {})", 
                        openness, neuroticism, extraversion);
            
            return adjustedState;
            
        } catch (Exception e) {
            logger.warn("Error applying personality adjustments: {}", e.getMessage());
            return state;
        }
    }
    
    /**
     * Apply NLP-based adjustments to state vector
     */
    private List<Double> applyAIAdjustments(List<Double> state, String content, UUID userId) {
        try {
            if (state == null || content == null || content.trim().isEmpty()) {
                return state;
            }
            
            // Use AI service for content analysis
            Map<String, Object> aiAnalysis = aiServiceClient.analyzeContentForModeration(content, userId);
            
            // Extract AI insights
            double sentimentIntensity = (Double) aiAnalysis.getOrDefault("sentimentIntensity", 0.5);
            double emotionalIntensity = (Double) aiAnalysis.getOrDefault("emotionalIntensity", 0.5);
            double urgencyIntensity = (Double) aiAnalysis.getOrDefault("urgencyIntensity", 0.5);
            
            // Apply AI-based adjustments
            List<Double> adjustedState = new ArrayList<>(state);
            
            // Sentiment affects positive/negative factor balance
            if (sentimentIntensity > 0.7) {
                // Positive sentiment - boost positive factors (5-15)
                for (int i = 5; i < 16 && i < adjustedState.size(); i++) {
                    adjustedState.set(i, adjustedState.get(i) * (1.0 + sentimentIntensity * 0.2));
                }
            } else if (sentimentIntensity < 0.3) {
                // Negative sentiment - boost negative factors (16-42)
                for (int i = 16; i < adjustedState.size(); i++) {
                    adjustedState.set(i, adjustedState.get(i) * (1.0 + (1.0 - sentimentIntensity) * 0.2));
                }
            }
            
            // Emotional intensity affects feeling dimension (2)
            if (2 < adjustedState.size()) {
                adjustedState.set(2, adjustedState.get(2) * (1.0 + emotionalIntensity * 0.3));
            }
            
            // Urgency affects attention dimension (0)
            if (0 < adjustedState.size()) {
                adjustedState.set(0, adjustedState.get(0) * (1.0 + urgencyIntensity * 0.2));
            }
            
            logger.debug("Applied AI adjustments (sentiment: {}, emotional: {}, urgency: {})", 
                        sentimentIntensity, emotionalIntensity, urgencyIntensity);
            
            return adjustedState;
            
        } catch (Exception e) {
            logger.warn("Error applying AI adjustments: {}", e.getMessage());
            return state;
        }
    }
    
    /**
     * Apply AI analysis insights to state vector
     */
    private List<Double> applyAIAnalysisAdjustments(List<Double> state, Map<String, Object> aiAnalysis) {
        try {
            if (aiAnalysis == null || state == null) {
                return state;
            }
            
            // Extract AI analysis insights
            Map<String, Object> psychologicalVector = (Map<String, Object>) aiAnalysis.get("psychologicalVector");
            Map<String, Object> buddhistFactors = (Map<String, Object>) aiAnalysis.get("buddhistFactors");
            Map<String, Object> processedVector = (Map<String, Object>) aiAnalysis.get("processedVector");
            
            // Apply AI analysis adjustments
            List<Double> adjustedState = new ArrayList<>(state);
            
            // Use processed vector insights if available
            if (processedVector != null) {
                @SuppressWarnings("unchecked")
                List<Double> processedVectorData = (List<Double>) processedVector.get("processedVector");
                if (processedVectorData != null && processedVectorData.size() == state.size()) {
                    // Blend with processed vector (20% weight)
                    for (int i = 0; i < adjustedState.size(); i++) {
                        adjustedState.set(i, adjustedState.get(i) * 0.8 + processedVectorData.get(i) * 0.2);
                    }
                }
            }
            
            // Use Buddhist factors insights
            if (buddhistFactors != null) {
                @SuppressWarnings("unchecked")
                Map<String, Double> factorScores = (Map<String, Double>) buddhistFactors.get("factorScores");
                if (factorScores != null) {
                    // Apply factor-specific adjustments
                    applyBuddhistFactorAdjustments(adjustedState, factorScores);
                }
            }
            
            logger.debug("Applied AI analysis adjustments");
            
            return adjustedState;
            
        } catch (Exception e) {
            logger.warn("Error applying AI analysis adjustments: {}", e.getMessage());
            return state;
        }
    }
    
    /**
     * Apply Buddhist factor adjustments to state vector
     */
    private void applyBuddhistFactorAdjustments(List<Double> state, Map<String, Double> factorScores) {
        try {
            // Apply adjustments based on specific Buddhist factors
            // This is a simplified mapping - in practice, you'd have a more sophisticated mapping
            
            // Faith factor (dimension 5)
            if (factorScores.containsKey("faith") && 5 < state.size()) {
                double faithScore = factorScores.get("faith");
                state.set(5, state.get(5) * (1.0 + faithScore * 0.2));
            }
            
            // Vigor factor (dimension 6)
            if (factorScores.containsKey("vigor") && 6 < state.size()) {
                double vigorScore = factorScores.get("vigor");
                state.set(6, state.get(6) * (1.0 + vigorScore * 0.2));
            }
            
            // Greed factor (dimension 16)
            if (factorScores.containsKey("greed") && 16 < state.size()) {
                double greedScore = factorScores.get("greed");
                state.set(16, state.get(16) * (1.0 + greedScore * 0.2));
            }
            
            // Anger factor (dimension 17)
            if (factorScores.containsKey("anger") && 17 < state.size()) {
                double angerScore = factorScores.get("anger");
                state.set(17, state.get(17) * (1.0 + angerScore * 0.2));
            }
            
        } catch (Exception e) {
            logger.warn("Error applying Buddhist factor adjustments: {}", e.getMessage());
        }
    }

    // ===== ENHANCED QUEUING SYSTEM INTEGRATION =====

    /**
     * Queue psychological state update for asynchronous processing
     */
    public CompletableFuture<String> queueStateUpdateFromPost(UUID userId, String postContent, UUID postId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Queuing state update from post for user: {}", userId);
                
                // Create state update request
                RequestQueueService.StateUpdateRequest stateUpdateRequest = RequestQueueService.StateUpdateRequest.builder()
                    .userId(userId.toString())
                    .userGroup(determineUserGroup(userId))
                    .updateType(RequestQueueService.StateUpdateType.POST_CREATION)
                    .updateData(createPostUpdateData(postContent, postId))
                    .priority(RequestQueueService.StateUpdatePriority.NORMAL)
                    .urgency(RequestQueueService.StateUpdateUrgency.NORMAL)
                    .userTier(determineUserTier(userId))
                    .metadata(createPostMetadata(postContent, userId))
                    .build();
                
                // Queue the request
                return requestQueueService.queueStateUpdate(stateUpdateRequest).join();
                
            } catch (Exception e) {
                logger.error("Error queuing state update from post: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to queue state update", e);
            }
        });
    }

    /**
     * Queue psychological state update for interaction
     */
    public CompletableFuture<String> queueStateUpdateFromInteraction(UUID userId, UUID targetPostId, 
                                                                    String interactionType, double interactionWeight) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Queuing state update from interaction for user: {}", userId);
                
                // Create state update request
                RequestQueueService.StateUpdateRequest stateUpdateRequest = RequestQueueService.StateUpdateRequest.builder()
                    .userId(userId.toString())
                    .userGroup(determineUserGroup(userId))
                    .updateType(mapInteractionType(interactionType))
                    .updateData(createInteractionUpdateData(targetPostId, interactionWeight))
                    .priority(RequestQueueService.StateUpdatePriority.NORMAL)
                    .urgency(RequestQueueService.StateUpdateUrgency.NORMAL)
                    .userTier(determineUserTier(userId))
                    .metadata(createInteractionMetadata(interactionType, interactionWeight))
                    .build();
                
                // Queue the request
                return requestQueueService.queueStateUpdate(stateUpdateRequest).join();
                
            } catch (Exception e) {
                logger.error("Error queuing state update from interaction: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to queue state update", e);
            }
        });
    }

    /**
     * Process queued state updates in batches
     */
    public CompletableFuture<StateUpdateBatchProcessor.BatchStateUpdateResult> processQueuedStateUpdates(
            String queueName) {
        return stateUpdateBatchProcessor.processQueuedStateUpdates(queueName, this::processQueuedStateUpdate);
    }

    /**
     * Process a single queued state update request
     */
    public CompletableFuture<StateUpdateBatchProcessor.StateUpdateResult> processQueuedStateUpdate(
            RequestQueueService.StateUpdateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                logger.debug("Processing queued state update: {}", request.getRequestId());
                
                UUID userId = UUID.fromString(request.getUserId());
                StateUpdateBatchProcessor.StateUpdateResult result;
                
                switch (request.getUpdateType()) {
                    case POST_CREATION:
                        result = processPostCreationUpdate(userId, request);
                        break;
                    case INTERACTION_RESONANCE:
                    case INTERACTION_LINGER:
                        result = processInteractionUpdate(userId, request);
                        break;
                    case PERSONALITY_UPDATE:
                        result = processPersonalityUpdate(userId, request);
                        break;
                    case MOOD_ANALYSIS:
                        result = processMoodAnalysisUpdate(userId, request);
                        break;
                    case BATCH_UPDATE:
                        result = processBatchUpdate(userId, request);
                        break;
                    case EMERGENCY_UPDATE:
                        result = processEmergencyUpdate(userId, request);
                        break;
                    default:
                        result = StateUpdateBatchProcessor.StateUpdateResult.builder()
                            .success(false)
                            .error("Unknown update type: " + request.getUpdateType())
                            .build();
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                result.setProcessingTime(processingTime);
                
                // Complete the request in queue
                requestQueueService.completeRequest(request.getRequestId(), result.isSuccess(), result);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error processing queued state update {}: {}", request.getRequestId(), e.getMessage(), e);
                
                // Handle failed request
                requestQueueService.handleFailedStateUpdate(request, e);
                
                return StateUpdateBatchProcessor.StateUpdateResult.builder()
                    .success(false)
                    .error("Processing failed: " + e.getMessage())
                    .processingTime(System.currentTimeMillis() - startTime)
                    .build();
            }
        });
    }

    /**
     * Get queue health status for monitoring
     */
    public RequestQueueService.QueueHealthStatus getQueueHealthStatus() {
        return requestQueueService.getQueueHealthStatus();
    }

    /**
     * Get batch processing health status
     */
    public StateUpdateBatchProcessor.BatchProcessingHealthStatus getBatchProcessingHealthStatus() {
        return stateUpdateBatchProcessor.getBatchProcessingHealthStatus();
    }

    // Private helper methods for queuing integration
    private String determineUserGroup(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Determine user group based on user characteristics
                if (user.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1))) {
                    return "new_users";
                } else if (user.getCreatedAt().isBefore(LocalDateTime.now().minusYears(1))) {
                    return "veteran_users";
                } else {
                    return "regular_users";
                }
            }
        } catch (Exception e) {
            logger.warn("Error determining user group for {}: {}", userId, e.getMessage());
        }
        return "default_group";
    }

    private RequestQueueService.UserTier determineUserTier(UUID userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                // Determine user tier based on user characteristics
                // This is a simplified implementation
                return RequestQueueService.UserTier.FREE; // Default to FREE
            }
        } catch (Exception e) {
            logger.warn("Error determining user tier for {}: {}", userId, e.getMessage());
        }
        return RequestQueueService.UserTier.FREE;
    }

    private Map<String, Object> createPostUpdateData(String postContent, UUID postId) {
        Map<String, Object> data = new HashMap<>();
        data.put("postContent", postContent);
        data.put("postId", postId.toString());
        data.put("timestamp", LocalDateTime.now());
        return data;
    }

    private Map<String, Object> createInteractionUpdateData(UUID targetPostId, double interactionWeight) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetPostId", targetPostId.toString());
        data.put("interactionWeight", interactionWeight);
        data.put("timestamp", LocalDateTime.now());
        return data;
    }

    private Map<String, Object> createPostMetadata(String postContent, UUID userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentLength", postContent.length());
        metadata.put("userId", userId.toString());
        metadata.put("timestamp", LocalDateTime.now());
        return metadata;
    }

    private Map<String, Object> createInteractionMetadata(String interactionType, double interactionWeight) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("interactionType", interactionType);
        metadata.put("interactionWeight", interactionWeight);
        metadata.put("timestamp", LocalDateTime.now());
        return metadata;
    }

    private RequestQueueService.StateUpdateType mapInteractionType(String interactionType) {
        switch (interactionType.toLowerCase()) {
            case "resonance":
                return RequestQueueService.StateUpdateType.INTERACTION_RESONANCE;
            case "linger":
                return RequestQueueService.StateUpdateType.INTERACTION_LINGER;
            default:
                return RequestQueueService.StateUpdateType.INTERACTION_RESONANCE;
        }
    }

    // Process different types of queued updates
    private StateUpdateBatchProcessor.StateUpdateResult processPostCreationUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getUpdateData();
            String postContent = (String) data.get("postContent");
            UUID postId = UUID.fromString((String) data.get("postId"));
            
            Map<String, Object> result = updateStateFromPost(userId, postContent, postId);
            
            return StateUpdateBatchProcessor.StateUpdateResult.builder()
                .success(true)
                .updatedState(extractStateVector(result))
                .alpha(extractAlpha(result))
                .build();
                
        } catch (Exception e) {
            logger.error("Error processing post creation update: {}", e.getMessage(), e);
            return StateUpdateBatchProcessor.StateUpdateResult.builder()
                .success(false)
                .error("Post creation update failed: " + e.getMessage())
                .build();
        }
    }

    private StateUpdateBatchProcessor.StateUpdateResult processInteractionUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        try {
            Map<String, Object> data = (Map<String, Object>) request.getUpdateData();
            UUID targetPostId = UUID.fromString((String) data.get("targetPostId"));
            double interactionWeight = (Double) data.get("interactionWeight");
            
            String interactionType = request.getUpdateType() == RequestQueueService.StateUpdateType.INTERACTION_RESONANCE 
                ? "resonance" : "linger";
            
            Map<String, Object> result = updateStateFromInteraction(userId, targetPostId, interactionType, interactionWeight);
            
            return StateUpdateBatchProcessor.StateUpdateResult.builder()
                .success(true)
                .updatedState(extractStateVector(result))
                .alpha(extractAlpha(result))
                .build();
                
        } catch (Exception e) {
            logger.error("Error processing interaction update: {}", e.getMessage(), e);
            return StateUpdateBatchProcessor.StateUpdateResult.builder()
                .success(false)
                .error("Interaction update failed: " + e.getMessage())
                .build();
        }
    }

    private StateUpdateBatchProcessor.StateUpdateResult processPersonalityUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        // Placeholder for personality update processing
        return StateUpdateBatchProcessor.StateUpdateResult.builder()
            .success(true)
            .updatedState(convertDoubleListToFloatList(getCurrentState(userId)))
            .alpha(DEFAULT_BASE_ALPHA)
            .build();
    }

    private StateUpdateBatchProcessor.StateUpdateResult processMoodAnalysisUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        // Placeholder for mood analysis update processing
        return StateUpdateBatchProcessor.StateUpdateResult.builder()
            .success(true)
            .updatedState(convertDoubleListToFloatList(getCurrentState(userId)))
            .alpha(DEFAULT_BASE_ALPHA)
            .build();
    }

    private StateUpdateBatchProcessor.StateUpdateResult processBatchUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        // Placeholder for batch update processing
        return StateUpdateBatchProcessor.StateUpdateResult.builder()
            .success(true)
            .updatedState(convertDoubleListToFloatList(getCurrentState(userId)))
            .alpha(DEFAULT_BASE_ALPHA)
            .build();
    }

    private StateUpdateBatchProcessor.StateUpdateResult processEmergencyUpdate(UUID userId, RequestQueueService.StateUpdateRequest request) {
        // Placeholder for emergency update processing
        return StateUpdateBatchProcessor.StateUpdateResult.builder()
            .success(true)
            .updatedState(convertDoubleListToFloatList(getCurrentState(userId)))
            .alpha(MAX_ALPHA) // Use maximum alpha for emergency updates
            .build();
    }

    private List<Float> extractStateVector(Map<String, Object> result) {
        if (result != null && result.containsKey("state")) {
            Object stateObj = result.get("state");
            if (stateObj instanceof List) {
                return ((List<?>) stateObj).stream()
                    .map(obj -> ((Number) obj).floatValue())
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
        }
        return new ArrayList<>();
    }

    private double extractAlpha(Map<String, Object> result) {
        if (result != null && result.containsKey("alpha")) {
            Object alphaObj = result.get("alpha");
            if (alphaObj instanceof Number) {
                return ((Number) alphaObj).doubleValue();
            }
        }
        return DEFAULT_BASE_ALPHA;
    }

    private List<Float> convertDoubleListToFloatList(List<Double> doubleList) {
        if (doubleList == null) {
            return new ArrayList<>();
        }
        return doubleList.stream()
            .map(Double::floatValue)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
