package ai.youthmind.service.personality;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvancedPersonalityValidator {

    public boolean isValidVector(List<Double> vector) {
        if (vector == null || vector.size() != 43) return false;
        for (Double v : vector) {
            if (v == null || v.isNaN() || v.isInfinite() || v < 0.0 || v > 1.0) return false;
        }
        return true;
    }
}


