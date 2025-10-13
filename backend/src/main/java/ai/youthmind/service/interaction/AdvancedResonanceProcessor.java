package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Advanced resonance processing with temporal decay and signal smoothing.
 * Returns a score in [0, 1].
 */
@Service
public class AdvancedResonanceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedResonanceProcessor.class);

    // Tunable parameters
    private static final double BASE_WEIGHT = 0.6;
    private static final double RECENCY_WEIGHT = 0.3;
    private static final double STATE_ALIGNMENT_WEIGHT = 0.1;
    private static final long HALF_LIFE_HOURS = 24;

    public double computeResonanceScore(UUID userId,
                                        UUID postId,
                                        List<Double> currentStateVector,
                                        double recentResonanceSignal,
                                        LocalDateTime lastInteractionAt) {
        try {
            double boundedRecent = clamp(recentResonanceSignal, 0.0, 1.0);
            double recencyBoost = computeRecencyBoost(lastInteractionAt);

            // Lightweight alignment proxy: favor mid-range stable states
            double alignment = 0.0;
            if (currentStateVector != null && !currentStateVector.isEmpty()) {
                double mean = currentStateVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                alignment = 1.0 - Math.min(1.0, Math.abs(mean - 0.5));
            }

            double score = (BASE_WEIGHT * boundedRecent)
                + (RECENCY_WEIGHT * recencyBoost)
                + (STATE_ALIGNMENT_WEIGHT * alignment);

            return clamp(score, 0.0, 1.0);
        } catch (Exception e) {
            logger.error("Failed to compute resonance score for user {} post {}: {}", userId, postId, e.getMessage(), e);
            return 0.0;
        }
    }

    private double computeRecencyBoost(LocalDateTime lastInteractionAt) {
        if (lastInteractionAt == null) {
            return 0.0;
        }
        long hours = Math.max(0, Duration.between(lastInteractionAt, LocalDateTime.now()).toHours());
        double lambda = Math.log(2) / HALF_LIFE_HOURS; // exponential half-life
        double decay = Math.exp(-lambda * hours);
        return clamp(decay, 0.0, 1.0);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}


