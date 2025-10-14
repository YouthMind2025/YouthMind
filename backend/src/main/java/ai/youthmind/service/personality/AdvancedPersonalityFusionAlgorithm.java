package ai.youthmind.service.personality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sophisticated fusion algorithm utilities to complement PersonalityFusionService.
 */
@Service
public class AdvancedPersonalityFusionAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedPersonalityFusionAlgorithm.class);

    /**
     * Weighted fusion with confidence-aware normalization.
     */
    public Map<String, Object> fuse(List<Double> active, List<Double> shadow, double lambdaActive, double lambdaShadow,
                                    double activeConfidence, double shadowConfidence) {
        Map<String, Object> out = new HashMap<>();
        try {
            if (active == null || shadow == null || active.size() != shadow.size()) {
                out.put("personalityVector", new ArrayList<>());
                out.put("confidence", 0.0);
                return out;
            }
            double confNorm = Math.max(0.0001, activeConfidence + shadowConfidence);
            double la = (lambdaActive * activeConfidence) / confNorm;
            double ls = (lambdaShadow * shadowConfidence) / confNorm;

            List<Double> fused = new ArrayList<>(active.size());
            for (int i = 0; i < active.size(); i++) {
                double v = la * active.get(i) + ls * shadow.get(i);
                fused.add(Math.max(0.0, Math.min(1.0, v)));
            }
            out.put("personalityVector", fused);
            out.put("confidence", Math.min(1.0, (activeConfidence + shadowConfidence) / 2.0));
            return out;
        } catch (Exception e) {
            logger.error("Advanced fusion failed: {}", e.getMessage(), e);
            out.put("personalityVector", new ArrayList<>());
            out.put("confidence", 0.0);
            return out;
        }
    }
}


