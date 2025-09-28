package ai.youthmind.service.personality;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class PersonalityComputationMonitor {

    private final Counter successCounter;
    private final Counter errorCounter;

    public PersonalityComputationMonitor(MeterRegistry registry) {
        this.successCounter = registry.counter("personality.computation.success");
        this.errorCounter = registry.counter("personality.computation.error");
    }

    public void markSuccess() { successCounter.increment(); }
    public void markError() { errorCounter.increment(); }
}


