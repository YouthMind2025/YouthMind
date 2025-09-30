package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptive Learning Rate Service for dynamic α calculation.
 * Implements sophisticated algorithms for adjusting learning rates
 * based on user behavior patterns and psychological state transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveLearningRateService {

    // Learning rate parameters
    private static final double DEFAULT_BASE_ALPHA = 0.1;
    private static final double MIN_ALPHA = 0.01;
    private static final double MAX_ALPHA = 0.5;
    private static final double ADAPTATION_DECAY = 0.95;
    private static final double CONVERGENCE_THRESHOLD = 0.001;

    // User behavior tracking
    private final Map<String, LearningRateProfile> learningRateProfiles = new ConcurrentHashMap<>();

    /**
     * Calculate dynamic learning rate α = base_alpha + θ
     */
    public CompletableFuture<Double> calculateDynamicAlpha(
            UserBehaviorProfile behaviorProfile,
            String observationType,
            Map<String, Object> context) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = behaviorProfile.getUserId();
                LearningRateProfile profile = getLearningRateProfile(userId);
                
                // Base alpha from behavior profile
                double baseAlpha = behaviorProfile.getBaseAlpha();
                
                // Calculate adaptation factor θ based on multiple factors
                double theta = calculateAdaptationFactor(profile, observationType, context);
                
                // Apply dynamic formula: α = base_alpha + θ
                double dynamicAlpha = baseAlpha + theta;
                
                // Apply bounds and constraints
                double finalAlpha = applyConstraints(dynamicAlpha, profile);
                
                // Update learning rate profile
                updateLearningRateProfile(profile, observationType, finalAlpha);
                
                log.debug("Calculated dynamic alpha for user {}: base={}, theta={}, final={}", 
                        userId, baseAlpha, theta, finalAlpha);
                
                return finalAlpha;
                
            } catch (Exception e) {
                log.error("Error calculating dynamic alpha: {}", e.getMessage(), e);
                return DEFAULT_BASE_ALPHA;
            }
        });
    }

    /**
     * Calculate adaptation factor θ based on multiple behavioral factors
     */
    private double calculateAdaptationFactor(
            LearningRateProfile profile,
            String observationType,
            Map<String, Object> context) {
        
        double theta = 0.0;
        
        // Factor 1: Observation frequency (more frequent = lower alpha)
        double frequencyFactor = calculateFrequencyFactor(profile);
        theta += frequencyFactor * 0.3;
        
        // Factor 2: Observation type adaptation
        double typeFactor = calculateTypeFactor(profile, observationType);
        theta += typeFactor * 0.2;
        
        // Factor 3: Temporal patterns
        double temporalFactor = calculateTemporalFactor(profile);
        theta += temporalFactor * 0.2;
        
        // Factor 4: Context-based adaptation
        double contextFactor = calculateContextFactor(context);
        theta += contextFactor * 0.2;
        
        // Factor 5: Convergence history
        double convergenceFactor = calculateConvergenceFactor(profile);
        theta += convergenceFactor * 0.1;
        
        return theta;
    }

    /**
     * Calculate frequency-based adaptation factor
     */
    private double calculateFrequencyFactor(LearningRateProfile profile) {
        int recentObservations = profile.getRecentObservationCount();
        
        if (recentObservations < 5) {
            return 0.1; // Higher learning rate for new users
        } else if (recentObservations < 20) {
            return 0.0; // Normal learning rate
        } else {
            return -0.1; // Lower learning rate for experienced users
        }
    }

    /**
     * Calculate observation type-based adaptation factor
     */
    private double calculateTypeFactor(LearningRateProfile profile, String observationType) {
        Map<String, Integer> typeCounts = profile.getObservationTypeCounts();
        int typeCount = typeCounts.getOrDefault(observationType, 0);
        
        if (typeCount == 0) {
            return 0.05; // Slightly higher for new observation types
        } else if (typeCount < 10) {
            return 0.0; // Normal for familiar types
        } else {
            return -0.05; // Slightly lower for very familiar types
        }
    }

    /**
     * Calculate temporal pattern-based adaptation factor
     */
    private double calculateTemporalFactor(LearningRateProfile profile) {
        long timeSinceLastUpdate = System.currentTimeMillis() - profile.getLastUpdateTime();
        long timeSinceLastObservation = System.currentTimeMillis() - profile.getLastObservationTime();
        
        // If user hasn't been active recently, increase learning rate
        if (timeSinceLastUpdate > 24 * 60 * 60 * 1000) { // 24 hours
            return 0.1;
        } else if (timeSinceLastObservation > 60 * 60 * 1000) { // 1 hour
            return 0.05;
        } else {
            return 0.0;
        }
    }

    /**
     * Calculate context-based adaptation factor
     */
    private double calculateContextFactor(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return 0.0;
        }
        
        // Check for high-stress or emotional context
        String emotionalState = (String) context.get("emotional_state");
        if ("high_stress".equals(emotionalState) || "crisis".equals(emotionalState)) {
            return 0.15; // Higher learning rate for critical situations
        }
        
        // Check for confidence level
        Double confidence = (Double) context.get("confidence");
        if (confidence != null && confidence < 0.5) {
            return 0.1; // Higher learning rate for low confidence
        }
        
        return 0.0;
    }

    /**
     * Calculate convergence-based adaptation factor
     */
    private double calculateConvergenceFactor(LearningRateProfile profile) {
        List<Double> recentConvergenceScores = profile.getRecentConvergenceScores();
        
        if (recentConvergenceScores.isEmpty()) {
            return 0.0;
        }
        
        double avgConvergence = recentConvergenceScores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        if (avgConvergence > CONVERGENCE_THRESHOLD) {
            return 0.1; // Higher learning rate if not converging well
        } else {
            return -0.05; // Lower learning rate if converging well
        }
    }

    /**
     * Apply constraints to learning rate
     */
    private double applyConstraints(double alpha, LearningRateProfile profile) {
        // Apply bounds
        alpha = Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
        
        // Apply decay based on usage
        double decayFactor = Math.pow(ADAPTATION_DECAY, profile.getTotalObservations() / 100.0);
        alpha *= decayFactor;
        
        // Ensure minimum viable learning rate
        return Math.max(MIN_ALPHA, alpha);
    }

    /**
     * Get or create learning rate profile for user
     */
    private LearningRateProfile getLearningRateProfile(String userId) {
        return learningRateProfiles.computeIfAbsent(userId, k -> 
            LearningRateProfile.builder()
                .userId(userId)
                .totalObservations(0)
                .recentObservationCount(0)
                .lastUpdateTime(System.currentTimeMillis())
                .lastObservationTime(System.currentTimeMillis())
                .observationTypeCounts(new HashMap<>())
                .recentConvergenceScores(new ArrayList<>())
                .build()
        );
    }

    /**
     * Update learning rate profile with new observation
     */
    private void updateLearningRateProfile(LearningRateProfile profile, String observationType, double alpha) {
        profile.setTotalObservations(profile.getTotalObservations() + 1);
        profile.setRecentObservationCount(profile.getRecentObservationCount() + 1);
        profile.setLastUpdateTime(System.currentTimeMillis());
        profile.setLastObservationTime(System.currentTimeMillis());
        
        // Update observation type counts
        Map<String, Integer> typeCounts = profile.getObservationTypeCounts();
        typeCounts.put(observationType, typeCounts.getOrDefault(observationType, 0) + 1);
        
        // Keep only recent convergence scores (last 50)
        List<Double> convergenceScores = profile.getRecentConvergenceScores();
        if (convergenceScores.size() >= 50) {
            convergenceScores.remove(0);
        }
    }

    /**
     * Update convergence score for learning rate adaptation
     */
    public void updateConvergenceScore(String userId, double convergenceScore) {
        LearningRateProfile profile = learningRateProfiles.get(userId);
        if (profile != null) {
            List<Double> scores = profile.getRecentConvergenceScores();
            scores.add(convergenceScore);
            
            // Keep only recent scores
            if (scores.size() > 50) {
                scores.remove(0);
            }
        }
    }

    /**
     * Get learning rate profile for user (public method)
     */
    public LearningRateProfile getUserLearningRateProfile(String userId) {
        return learningRateProfiles.get(userId);
    }

    /**
     * Reset learning rate profile for user
     */
    public void resetLearningRateProfile(String userId) {
        learningRateProfiles.remove(userId);
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class UserBehaviorProfile {
        private String userId;
        private double baseAlpha;
        private double adaptationFactor;
        private double smoothingFactor;
        private int observationCount;
        private long lastUpdateTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class LearningRateProfile {
        private String userId;
        private int totalObservations;
        private int recentObservationCount;
        private long lastUpdateTime;
        private long lastObservationTime;
        private Map<String, Integer> observationTypeCounts;
        private List<Double> recentConvergenceScores;
    }
}
