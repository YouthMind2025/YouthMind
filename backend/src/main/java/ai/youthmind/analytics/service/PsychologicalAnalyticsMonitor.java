package ai.youthmind.analytics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class PsychologicalAnalyticsMonitor {

    private final Counter insightsCounter;

    public PsychologicalAnalyticsMonitor(MeterRegistry registry) {
        this.insightsCounter = registry.counter("psych.analytics.insights");
    }

    public void markInsightProduced() { insightsCounter.increment(); }
}


