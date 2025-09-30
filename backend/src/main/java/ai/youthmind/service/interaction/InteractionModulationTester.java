package ai.youthmind.service.interaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight validation harness to sanity-check scores.
 */
@Service
public class InteractionModulationTester {

    private static final Logger logger = LoggerFactory.getLogger(InteractionModulationTester.class);

    public void sanityCheck(UUID userId, UUID postId, List<Double> stateVector, double resonance, double linger, double balance) {
        if (Double.isNaN(resonance) || Double.isNaN(linger) || Double.isNaN(balance)) {
            logger.warn("Sanity check failed for user {} post {}: NaN values detected", userId, postId);
        }
        if (resonance < 0 || resonance > 1 || linger < 0 || linger > 1 || balance < 0 || balance > 1) {
            logger.warn("Sanity check bounds violation for user {} post {}", userId, postId);
        }
    }
}


