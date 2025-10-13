package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Adaptive interaction weight (Ws) based on behavior and quality signals.
 */
@Service
public class AdaptiveInteractionWeightCalculator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveInteractionWeightCalculator.class);

    // Base Ws and bounds
    private static final double BASE_WS = 0.2;
    private static final double MIN_WS = 0.05;
    private static final double MAX_WS = 0.6;

    /**
     * Compute Ws in [MIN_WS, MAX_WS].
     */
    public double computeWs(double resonanceScore,
                            double lingerScore,
                            double positiveBalance,
                            boolean contentQualityHigh) {
        try {
            double behavior = 0.5 * clamp(resonanceScore, 0, 1) + 0.5 * clamp(lingerScore, 0, 1);
            double quality = contentQualityHigh ? 0.05 : -0.02;
            double balanceAdj = 0.1 * (clamp(positiveBalance, 0, 1) - 0.5);
            double ws = BASE_WS + 0.2 * behavior + quality + balanceAdj;
            return clamp(ws, MIN_WS, MAX_WS);
        } catch (Exception e) {
            logger.error("Failed to compute adaptive Ws: {}", e.getMessage(), e);
            return BASE_WS;
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}


