package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.AiApiUsage;
import ai.youthmind.repository.AiApiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIHealthMonitoringService {

    private final AIServiceClient aiServiceClient;
    private final DeepSeekConfig deepSeekConfig;
    private final AiApiUsageRepository aiApiUsageRepository;

    private volatile boolean isServiceHealthy = true;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private volatile String lastHealthError = null;

    public Map<String, Object> performHealthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        
        try {
            // Test API connectivity with a simple moderation request
            CompletableFuture<ModerationResult> healthCheckFuture = CompletableFuture.supplyAsync(() -> {
                return aiServiceClient.moderateContent("test", "text");
            });

            ModerationResult result = healthCheckFuture.get(10, TimeUnit.SECONDS);
            
            healthStatus.put("status", "UP");
            healthStatus.put("apiKeyConfigured", deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isEmpty());
            healthStatus.put("lastCheck", LocalDateTime.now());
            healthStatus.put("responseTime", "OK");
            healthStatus.put("error", null);
            
            isServiceHealthy = true;
            lastHealthCheck = LocalDateTime.now();
            lastHealthError = null;
            
        } catch (Exception e) {
            healthStatus.put("status", "DOWN");
            healthStatus.put("apiKeyConfigured", deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isEmpty());
            healthStatus.put("lastCheck", LocalDateTime.now());
            healthStatus.put("responseTime", "TIMEOUT");
            healthStatus.put("error", e.getMessage());
            
            isServiceHealthy = false;
            lastHealthCheck = LocalDateTime.now();
            lastHealthError = e.getMessage();
            
            log.error("AI service health check failed: {}", e.getMessage());
        }
        
        return healthStatus;
    }

    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic service status
        status.put("healthy", isServiceHealthy);
        status.put("lastHealthCheck", lastHealthCheck);
        status.put("lastError", lastHealthError);
        
        // Configuration status
        status.put("apiKeyConfigured", deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isEmpty());
        status.put("baseUrl", deepSeekConfig.getBaseUrl());
        status.put("timeout", deepSeekConfig.getTimeout());
        status.put("maxRetries", deepSeekConfig.getMaxRetries());
        status.put("model", deepSeekConfig.getModel());
        
        // Recent usage statistics (last 24 hours)
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        long totalRequests = aiApiUsageRepository.countByCreatedAtAfter(twentyFourHoursAgo);
        long successfulRequests = aiApiUsageRepository.countByCreatedAtAfterAndSuccess(twentyFourHoursAgo, true);
        long failedRequests = aiApiUsageRepository.countByCreatedAtAfterAndSuccess(twentyFourHoursAgo, false);
        
        status.put("requestsLast24h", totalRequests);
        status.put("successfulRequestsLast24h", successfulRequests);
        status.put("failedRequestsLast24h", failedRequests);
        status.put("successRate", totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0);
        
        return status;
    }

    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Recent performance data (last hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
        
        // Average response time
        Double avgResponseTime = aiApiUsageRepository.findAverageResponseTimeByCreatedAtAfter(oneHourAgo);
        metrics.put("averageResponseTimeMs", avgResponseTime != null ? avgResponseTime : 0.0);
        
        // Total cost in last hour
        Double totalCost = aiApiUsageRepository.findTotalCostByCreatedAtAfter(oneHourAgo);
        metrics.put("totalCostLastHour", totalCost != null ? totalCost : 0.0);
        
        // Token usage
        Long totalTokens = aiApiUsageRepository.findTotalTokensByCreatedAtAfter(oneHourAgo);
        metrics.put("totalTokensLastHour", totalTokens != null ? totalTokens : 0L);
        
        // Error rate
        long totalRequests = aiApiUsageRepository.countByCreatedAtAfter(oneHourAgo);
        long failedRequests = aiApiUsageRepository.countByCreatedAtAfterAndSuccess(oneHourAgo, false);
        metrics.put("errorRate", totalRequests > 0 ? (double) failedRequests / totalRequests : 0.0);
        
        // Peak usage times (requests per hour for last 24 hours)
        Map<String, Long> hourlyUsage = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            LocalDateTime hourStart = LocalDateTime.now().minus(i, ChronoUnit.HOURS);
            LocalDateTime hourEnd = hourStart.plus(1, ChronoUnit.HOURS);
            long hourRequests = aiApiUsageRepository.countByCreatedAtBetween(hourStart, hourEnd);
            hourlyUsage.put(hourStart.getHour() + ":00", hourRequests);
        }
        metrics.put("hourlyUsage", hourlyUsage);
        
        return metrics;
    }

    public boolean isServiceHealthy() {
        return isServiceHealthy;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public String getLastHealthError() {
        return lastHealthError;
    }
}
