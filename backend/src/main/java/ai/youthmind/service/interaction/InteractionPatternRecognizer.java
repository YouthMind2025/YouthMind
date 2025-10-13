package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lightweight interaction pattern recognition (rule-based + heuristics).
 */
@Service
public class InteractionPatternRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(InteractionPatternRecognizer.class);

    public enum PatternType { HIGH_RESONANCE, LONG_LINGER, BALANCED, VOLATILE, UNKNOWN }

    public PatternType classify(List<Double> recentResonances,
                                List<Double> recentLingers,
                                List<Double> recentBalances) {
        try {
            double r = avg(recentResonances);
            double l = avg(recentLingers);
            double b = avg(recentBalances);

            if (r > 0.7 && b > 0.5) return PatternType.HIGH_RESONANCE;
            if (l > 0.7 && b >= 0.4) return PatternType.LONG_LINGER;
            if (Math.abs(b - 0.5) < 0.1) return PatternType.BALANCED;
            if (variance(recentResonances) > 0.08 || variance(recentLingers) > 0.08) return PatternType.VOLATILE;
            return PatternType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Pattern classification failed: {}", e.getMessage(), e);
            return PatternType.UNKNOWN;
        }
    }

    private double avg(List<Double> xs) {
        if (xs == null || xs.isEmpty()) return 0.0;
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double variance(List<Double> xs) {
        if (xs == null || xs.size() < 2) return 0.0;
        double m = avg(xs);
        double s2 = 0.0;
        for (double x : xs) s2 += (x - m) * (x - m);
        return s2 / (xs.size() - 1);
    }
}


