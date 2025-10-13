package ai.youthmind.service.personality;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class PersonalityComputationAnalytics {

    private final Timer timer;

    public PersonalityComputationAnalytics(MeterRegistry registry) {
        this.timer = registry.timer("personality.computation.timer");
    }

    public <T> T time(java.util.concurrent.Callable<T> callable) {
        return timer.record(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Personality computation timing failed", e);
            }
        });
    }
}


