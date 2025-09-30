package ai.youthmind.analytics.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonalityInsightGenerator {

    public Map<String, Object> generateInsights(List<Double> personalityVector) {
        Map<String, Object> out = new HashMap<>();
        double mean = personalityVector == null || personalityVector.isEmpty() ? 0.0 : personalityVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        out.put("readiness", mean);
        out.put("recommendation", mean > 0.6 ? "Encourage deeper engagement" : "Offer supportive content");
        return out;
    }
}


