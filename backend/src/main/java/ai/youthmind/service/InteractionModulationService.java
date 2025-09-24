package ai.youthmind.service;

import ai.youthmind.entity.PostResonance;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for processing interaction modulation algorithms
 * Implements sophisticated interaction processing with resonance, linger, and balance calculations
 */
@Service
@Transactional
public class InteractionModulationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InteractionModulationService.class);
    
    // Algorithm constants
    public static final double RESONANCE_WEIGHT = 0.2; // Ws = 0.2 for resonance
    public static final double LINGER_WEIGHT = 0.01;   // Ws = 0.01 for linger
    public static final double POSITIVE_BALANCE_THRESHOLD = 0.7;
    private static final double INTERACTION_INTENSITY_BASE = 1.0;
    
    // Time decay constants
    private static final long RESONANCE_DECAY_HOURS = 24;
    private static final long LINGER_DECAY_HOURS = 72;
    private static final long BALANCE_TRACKING_DAYS = 30;
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ResonanceProcessor resonanceProcessor;
    
    @Autowired
    private LingerProcessor lingerProcessor;
    
    @Autowired
    private PositiveBalanceCalculator positiveBalanceCalculator;
    
    @Autowired
    private InteractionWeightService interactionWeightService;
    
    /**
     * Process resonance-based interaction modulation
     * @param userId User ID
     * @param postId Post ID
     * @return InteractionModulationResult with resonance impact
     */
    public InteractionModulationResult processResonance(UUID userId, UUID postId) {
        try {
            logger.debug("Processing resonance for user {} and post {}", userId, postId);
            
            // Check if resonance already exists
            boolean hasResonanced = postResonanceRepository.existsByUserIdAndPostId(userId, postId);
            
            if (hasResonanced) {
                return processResonanceRemoval(userId, postId);
            } else {
                return processResonanceAddition(userId, postId);
            }
            
        } catch (Exception e) {
            logger.error("Error processing resonance for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return createErrorResult("Failed to process resonance");
        }
    }
    
    /**
     * Process linger-based interaction modulation
     * @param userId User ID
     * @param postId Post ID
     * @param lingerDuration Duration in seconds
     * @return InteractionModulationResult with linger impact
     */
    public InteractionModulationResult processLinger(UUID userId, UUID postId, long lingerDuration) {
        try {
            logger.debug("Processing linger for user {} and post {} with duration {}", userId, postId, lingerDuration);
            
            // Calculate linger impact
            double lingerImpact = lingerProcessor.calculateLingerImpact(lingerDuration);
            
            // Update state from linger
            Map<String, Object> stateUpdate = lingerProcessor.updateStateFromLinger(userId, postId, lingerDuration);
            
            // Track linger duration
            lingerProcessor.trackLingerDuration(userId, postId, lingerDuration);
            
            // Calculate modulation
            double modulation = calculateModulation(RESONANCE_WEIGHT, lingerImpact, 0.0);
            
            // Apply positive balance
            double positiveBalance = positiveBalanceCalculator.calculatePositiveBalance(userId, postId);
            
            InteractionModulationResult result = new InteractionModulationResult();
            result.setUserId(userId);
            result.setPostId(postId);
            result.setModulationType("LINGER");
            result.setModulationValue(modulation);
            result.setLingerImpact(lingerImpact);
            result.setLingerDuration(lingerDuration);
            result.setStateUpdate(stateUpdate);
            result.setPositiveBalance(positiveBalance);
            result.setTimestamp(LocalDateTime.now());
            result.setSuccess(true);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing linger for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return createErrorResult("Failed to process linger");
        }
    }
    
    /**
     * Calculate interaction modulation using weighted components
     * @param resonanceWeight Weight for resonance component
     * @param lingerWeight Weight for linger component
     * @param balanceWeight Weight for balance component
     * @return Calculated modulation value
     */
    public double calculateModulation(double resonanceWeight, double lingerWeight, double balanceWeight) {
        try {
            // Base modulation calculation
            double baseModulation = (resonanceWeight * RESONANCE_WEIGHT) + 
                                  (lingerWeight * LINGER_WEIGHT) + 
                                  (balanceWeight * POSITIVE_BALANCE_THRESHOLD);
            
            // Apply interaction intensity factor
            double intensityFactor = interactionWeightService.calculateBeta();
            double modulatedValue = baseModulation * intensityFactor;
            
            // Normalize to [0, 1] range
            return Math.max(0.0, Math.min(1.0, modulatedValue));
            
        } catch (Exception e) {
            logger.error("Error calculating modulation: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Apply positive balance vector to interaction
     * @param userId User ID
     * @param postId Post ID
     * @return Positive balance value
     */
    public double applyPositiveBalance(UUID userId, UUID postId) {
        try {
            logger.debug("Applying positive balance for user {} and post {}", userId, postId);
            
            // Calculate positive balance
            double balance = positiveBalanceCalculator.calculatePositiveBalance(userId, postId);
            
            // Update balance vector
            positiveBalanceCalculator.updateBalanceVector(userId, postId, balance);
            
            // Track balance trends
            positiveBalanceCalculator.trackBalanceTrends(userId, balance);
            
            return balance;
            
        } catch (Exception e) {
            logger.error("Error applying positive balance for user {} and post {}: {}", userId, postId, e.getMessage(), e);
            return 0.0;
        }
    }
    
    /**
     * Get comprehensive interaction analysis for a user
     * @param userId User ID
     * @return Comprehensive interaction analysis
     */
    public ComprehensiveInteractionAnalysis getComprehensiveAnalysis(UUID userId) {
        try {
            logger.debug("Getting comprehensive interaction analysis for user {}", userId);
            
            // Get recent resonances
            List<PostResonance> recentResonances = postResonanceRepository
                .findByUserIdAndResonancedAtAfter(userId, LocalDateTime.now().minusDays(7));
            
            // Calculate resonance patterns
            Map<String, Object> resonancePatterns = resonanceProcessor.trackResonancePatterns(userId);
            
            // Get linger statistics
            Map<String, Object> lingerStats = lingerProcessor.getLingerStatistics(userId);
            
            // Get balance trends
            Map<String, Object> balanceTrends = positiveBalanceCalculator.getBalanceTrends(userId);
            
            // Calculate interaction intensity
            double interactionIntensity = interactionWeightService.calculateBeta();
            
            ComprehensiveInteractionAnalysis analysis = new ComprehensiveInteractionAnalysis();
            analysis.setUserId(userId);
            analysis.setResonanceCount(recentResonances.size());
            analysis.setResonancePatterns(resonancePatterns);
            analysis.setLingerStatistics(lingerStats);
            analysis.setBalanceTrends(balanceTrends);
            analysis.setInteractionIntensity(interactionIntensity);
            analysis.setAnalysisTimestamp(LocalDateTime.now());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive analysis for user {}: {}", userId, e.getMessage(), e);
            return new ComprehensiveInteractionAnalysis();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private InteractionModulationResult processResonanceAddition(UUID userId, UUID postId) {
        // Create new resonance
        PostResonance resonance = new PostResonance(userId, postId);
        postResonanceRepository.save(resonance);
        
        // Calculate resonance impact
        double resonanceImpact = resonanceProcessor.calculateResonanceImpact(userId, postId);
        
        // Update state from resonance
        Map<String, Object> stateUpdate = resonanceProcessor.updateStateFromResonance(userId, postId);
        
        // Track resonance patterns
        resonanceProcessor.trackResonancePatterns(userId);
        
        // Calculate modulation
        double modulation = calculateModulation(resonanceImpact, 0.0, 0.0);
        
        // Apply positive balance
        double positiveBalance = applyPositiveBalance(userId, postId);
        
        InteractionModulationResult result = new InteractionModulationResult();
        result.setUserId(userId);
        result.setPostId(postId);
        result.setModulationType("RESONANCE_ADD");
        result.setModulationValue(modulation);
        result.setResonanceImpact(resonanceImpact);
        result.setStateUpdate(stateUpdate);
        result.setPositiveBalance(positiveBalance);
        result.setTimestamp(LocalDateTime.now());
        result.setSuccess(true);
        
        return result;
    }
    
    private InteractionModulationResult processResonanceRemoval(UUID userId, UUID postId) {
        // Remove existing resonance
        postResonanceRepository.deleteByUserIdAndPostId(userId, postId);
        
        // Calculate negative resonance impact
        double resonanceImpact = -resonanceProcessor.calculateResonanceImpact(userId, postId);
        
        // Update state from resonance removal
        Map<String, Object> stateUpdate = resonanceProcessor.updateStateFromResonance(userId, postId);
        
        // Track resonance patterns
        resonanceProcessor.trackResonancePatterns(userId);
        
        // Calculate modulation
        double modulation = calculateModulation(resonanceImpact, 0.0, 0.0);
        
        // Apply positive balance
        double positiveBalance = applyPositiveBalance(userId, postId);
        
        InteractionModulationResult result = new InteractionModulationResult();
        result.setUserId(userId);
        result.setPostId(postId);
        result.setModulationType("RESONANCE_REMOVE");
        result.setModulationValue(modulation);
        result.setResonanceImpact(resonanceImpact);
        result.setStateUpdate(stateUpdate);
        result.setPositiveBalance(positiveBalance);
        result.setTimestamp(LocalDateTime.now());
        result.setSuccess(true);
        
        return result;
    }
    
    private InteractionModulationResult createErrorResult(String errorMessage) {
        InteractionModulationResult result = new InteractionModulationResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Result of interaction modulation processing
     */
    public static class InteractionModulationResult {
        private UUID userId;
        private UUID postId;
        private String modulationType;
        private double modulationValue;
        private double resonanceImpact;
        private double lingerImpact;
        private long lingerDuration;
        private Map<String, Object> stateUpdate;
        private double positiveBalance;
        private LocalDateTime timestamp;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public UUID getPostId() { return postId; }
        public void setPostId(UUID postId) { this.postId = postId; }
        
        public String getModulationType() { return modulationType; }
        public void setModulationType(String modulationType) { this.modulationType = modulationType; }
        
        public double getModulationValue() { return modulationValue; }
        public void setModulationValue(double modulationValue) { this.modulationValue = modulationValue; }
        
        public double getResonanceImpact() { return resonanceImpact; }
        public void setResonanceImpact(double resonanceImpact) { this.resonanceImpact = resonanceImpact; }
        
        public double getLingerImpact() { return lingerImpact; }
        public void setLingerImpact(double lingerImpact) { this.lingerImpact = lingerImpact; }
        
        public long getLingerDuration() { return lingerDuration; }
        public void setLingerDuration(long lingerDuration) { this.lingerDuration = lingerDuration; }
        
        public Map<String, Object> getStateUpdate() { return stateUpdate; }
        public void setStateUpdate(Map<String, Object> stateUpdate) { this.stateUpdate = stateUpdate; }
        
        public double getPositiveBalance() { return positiveBalance; }
        public void setPositiveBalance(double positiveBalance) { this.positiveBalance = positiveBalance; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * Comprehensive interaction analysis result
     */
    public static class ComprehensiveInteractionAnalysis {
        private UUID userId;
        private int resonanceCount;
        private Map<String, Object> resonancePatterns;
        private Map<String, Object> lingerStatistics;
        private Map<String, Object> balanceTrends;
        private double interactionIntensity;
        private LocalDateTime analysisTimestamp;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public int getResonanceCount() { return resonanceCount; }
        public void setResonanceCount(int resonanceCount) { this.resonanceCount = resonanceCount; }
        
        public Map<String, Object> getResonancePatterns() { return resonancePatterns; }
        public void setResonancePatterns(Map<String, Object> resonancePatterns) { this.resonancePatterns = resonancePatterns; }
        
        public Map<String, Object> getLingerStatistics() { return lingerStatistics; }
        public void setLingerStatistics(Map<String, Object> lingerStatistics) { this.lingerStatistics = lingerStatistics; }
        
        public Map<String, Object> getBalanceTrends() { return balanceTrends; }
        public void setBalanceTrends(Map<String, Object> balanceTrends) { this.balanceTrends = balanceTrends; }
        
        public double getInteractionIntensity() { return interactionIntensity; }
        public void setInteractionIntensity(double interactionIntensity) { this.interactionIntensity = interactionIntensity; }
        
        public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
        public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
    }
}
