package ai.youthmind.service.interaction;

import org.springframework.stereotype.Service;

/**
 * Basic validator for modulation inputs/outputs.
 */
@Service
public class InteractionModulationValidator {

    public boolean isValidScore(double v) {
        return v >= 0.0 && v <= 1.0 && !Double.isNaN(v) && !Double.isInfinite(v);
    }
}


