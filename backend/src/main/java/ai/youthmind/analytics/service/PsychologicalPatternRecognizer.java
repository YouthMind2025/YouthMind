package ai.youthmind.analytics.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PsychologicalPatternRecognizer {

    public String classifyByStability(List<Double> values) {
        if (values == null || values.size() < 3) return "UNKNOWN";
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double var = 0.0; for (double v : values) var += (v - mean) * (v - mean);
        var /= Math.max(1, values.size() - 1);
        if (var < 0.01) return "STABLE";
        if (var < 0.05) return "MODERATE";
        return "VOLATILE";
    }
}


