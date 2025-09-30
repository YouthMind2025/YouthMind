package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Advanced positive balance with weighted factors and normalization.
 */
@Service
public class AdvancedPositiveBalanceCalculator {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedPositiveBalanceCalculator.class);

    // Tunable weights
    private static final double BASE_BALANCE_WEIGHT = 0.5;
    private static final double SOCIAL_POSITIVITY_WEIGHT = 0.3;
    private static final double COGNITIVE_CLARITY_WEIGHT = 0.2;

    /**
     * Compute positive balance given decomposed factor scores.
     * All inputs expected in [0,1].
     */
    public double computePositiveBalance(double baseBalance,
                                         double socialPositivity,
                                         double cognitiveClarity) {
        try {
            double score = BASE_BALANCE_WEIGHT * clamp(baseBalance, 0, 1)
                + SOCIAL_POSITIVITY_WEIGHT * clamp(socialPositivity, 0, 1)
                + COGNITIVE_CLARITY_WEIGHT * clamp(cognitiveClarity, 0, 1);
            return clamp(score, 0.0, 1.0);
        } catch (Exception e) {
            logger.error("Failed to compute positive balance: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Convenience: derive balance from a 43-dim vector by simple pooling.
     */
    public double computeFromVector(List<Double> vector43) {
        if (vector43 == null || vector43.isEmpty()) return 0.0;
        double mean = vector43.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return clamp(mean, 0.0, 1.0);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}


