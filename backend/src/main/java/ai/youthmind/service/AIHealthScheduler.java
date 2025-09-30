package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for periodic AI health monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIHealthScheduler {

    private final AIHealthMonitoringService healthMonitoringService;

    /**
     * Perform health check every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void performPeriodicHealthCheck() {
        try {
            log.debug("Performing scheduled AI service health check");
            healthMonitoringService.performHealthCheck();
            log.debug("Scheduled AI service health check completed successfully");
        } catch (Exception e) {
            log.error("Scheduled AI service health check failed: {}", e.getMessage());
        }
    }

    /**
     * Log health status every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void logHealthStatus() {
        try {
            boolean isHealthy = healthMonitoringService.isServiceHealthy();
            String status = isHealthy ? "HEALTHY" : "UNHEALTHY";
            log.info("AI Service Status: {} (Last check: {})", 
                status, 
                healthMonitoringService.getLastHealthCheck());
            
            if (!isHealthy) {
                log.warn("AI Service is currently unhealthy. Last error: {}", 
                    healthMonitoringService.getLastHealthError());
            }
        } catch (Exception e) {
            log.error("Failed to log AI service health status: {}", e.getMessage());
        }
    }
}
