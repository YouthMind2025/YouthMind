package ai.youthmind.config;

import ai.youthmind.service.HikariCPConnectionPoolMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration for HikariCP Connection Pool Monitoring.
 * Initializes monitoring when the application is ready.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class HikariCPMonitoringConfig {

    private final HikariCPConnectionPoolMonitor connectionPoolMonitor;

    /**
     * Initialize monitoring when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMonitoring() {
        try {
            connectionPoolMonitor.initializeMonitoring();
            log.info("HikariCP connection pool monitoring started");
        } catch (Exception e) {
            log.error("Failed to initialize HikariCP monitoring: {}", e.getMessage(), e);
        }
    }
}
