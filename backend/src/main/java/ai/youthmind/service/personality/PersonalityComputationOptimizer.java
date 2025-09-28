package ai.youthmind.service.personality;

import org.springframework.stereotype.Service;

@Service
public class PersonalityComputationOptimizer {
    public double smoothConfidence(double c) {
        return Math.max(0.0, Math.min(1.0, 0.85 * c + 0.075));
    }
}


