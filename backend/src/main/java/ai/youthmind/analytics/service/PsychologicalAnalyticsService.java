package ai.youthmind.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PsychologicalAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(PsychologicalAnalyticsService.class);

    public Map<String, Object> trackInsight(UUID userId, List<Double> stateVector) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", String.valueOf(userId));
        result.put("timestamp", LocalDateTime.now());
        result.put("meanState", stateVector == null || stateVector.isEmpty() ? 0.0 : stateVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        return result;
    }
}


