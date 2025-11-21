package ai.youthmind.analytics.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PsychologicalAnalyticsVisualizer {

    public String sparkline(List<Double> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (double v : values) sb.append((char) ('' + (int) Math.round(8 * Math.max(0, Math.min(1, v)))));
        return sb.toString();
    }
}


