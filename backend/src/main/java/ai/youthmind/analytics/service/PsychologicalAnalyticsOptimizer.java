package ai.youthmind.analytics.service;

import org.springframework.stereotype.Service;

@Service
public class PsychologicalAnalyticsOptimizer {
    public double denoise(double x) { return Math.max(0.0, Math.min(1.0, 0.9 * x + 0.05)); }
}


