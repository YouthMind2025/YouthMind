package ai.youthmind.analytics.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StateTrendAnalyzer {

    public List<Double> movingAverage(List<Double> series, int window) {
        List<Double> out = new ArrayList<>();
        if (series == null || series.isEmpty() || window <= 0) return out;
        for (int i = 0; i < series.size(); i++) {
            int start = Math.max(0, i - window + 1);
            double sum = 0.0;
            int count = 0;
            for (int j = start; j <= i; j++) { sum += series.get(j); count++; }
            out.add(sum / count);
        }
        return out;
    }
}


