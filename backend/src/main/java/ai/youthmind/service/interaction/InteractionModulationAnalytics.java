package ai.youthmind.service.interaction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Micrometer-based analytics for interaction modulation.
 */
@Service
public class InteractionModulationAnalytics {

    private final Timer modulationTimer;

    @Autowired
    public InteractionModulationAnalytics(MeterRegistry registry) {
        this.modulationTimer = registry.timer("interaction.modulation.timer");
    }

    public <T> T timeModulation(java.util.concurrent.Callable<T> callable) {
        // Wrap Callable into Supplier to avoid checked exception signature
        return modulationTimer.record(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Interaction modulation timing failed", e);
            }
        });
    }
}


