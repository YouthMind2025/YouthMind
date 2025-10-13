package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Advanced linger modeling using time-on-content and recency decay.
 */
@Service
public class AdvancedLingerProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedLingerProcessor.class);

    private static final long HALF_LIFE_HOURS = 72;
    private static final double MAX_LINGER_SECONDS = 120.0; // cap to reduce outliers

    public double computeLingerScore(double secondsOnContent, LocalDateTime viewedAt) {
        try {
            double bounded = clamp(secondsOnContent / MAX_LINGER_SECONDS, 0.0, 1.0);
            double recency = computeRecency(viewedAt);
            return clamp(0.7 * bounded + 0.3 * recency, 0.0, 1.0);
        } catch (Exception e) {
            logger.error("Failed to compute linger score: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    private double computeRecency(LocalDateTime viewedAt) {
        if (viewedAt == null) return 0.0;
        long hours = Math.max(0, Duration.between(viewedAt, LocalDateTime.now()).toHours());
        double lambda = Math.log(2) / HALF_LIFE_HOURS;
        return clamp(Math.exp(-lambda * hours), 0.0, 1.0);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}


