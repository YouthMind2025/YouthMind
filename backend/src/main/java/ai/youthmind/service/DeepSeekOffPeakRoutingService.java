package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing off-peak window routing to optimize DeepSeek API usage.
 * Routes requests to different models or configurations based on time windows.
 */
@Service
public class DeepSeekOffPeakRoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekOffPeakRoutingService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    // Performance tracking for routing decisions
    private final Map<String, RoutingMetrics> routingMetrics = new ConcurrentHashMap<>();
    
    /**
     * Determine optimal routing configuration based on current time
     * 
     * @param requestType Type of request (moderation, chat, analysis)
     * @param userId User ID for personalized routing
     * @return RoutingDecision with optimal configuration
     */
    public RoutingDecision determineOptimalRouting(String requestType, UUID userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isOffPeak = isOffPeakTime(now);
            
            // Get base configuration
            RoutingDecision decision = new RoutingDecision();
            decision.setTimestamp(now);
            decision.setOffPeak(isOffPeak);
            decision.setRequestType(requestType);
            decision.setUserId(userId);
            
            if (isOffPeak) {
                // Off-peak configuration - use more powerful models
                configureOffPeakRouting(decision, requestType);
            } else {
                // Peak configuration - use standard models
                configurePeakRouting(decision, requestType);
            }
            
            // Update routing metrics
            updateRoutingMetrics(decision);
            
            logger.debug("Routing decision for {} at {}: {} (off-peak: {})", 
                requestType, now.format(DateTimeFormatter.ISO_LOCAL_TIME), 
                decision.getSelectedModel(), isOffPeak);
            
            return decision;
            
        } catch (Exception e) {
            logger.error("Error determining optimal routing: {}", e.getMessage(), e);
            // Return fallback configuration
            return createFallbackRouting(requestType, userId);
        }
    }
    
    /**
     * Check if current time is within off-peak window
     */
    private boolean isOffPeakTime(LocalDateTime now) {
        if (!deepSeekConfig.isEnableOffPeakRouting()) {
            return false;
        }
        
        try {
            // Parse off-peak times
            LocalTime offPeakStart = LocalTime.parse(deepSeekConfig.getOffPeakStartTime());
            LocalTime offPeakEnd = LocalTime.parse(deepSeekConfig.getOffPeakEndTime());
            LocalTime currentTime = now.toLocalTime();
            
            // Handle timezone conversion if needed
            ZoneId timezone = ZoneId.of(deepSeekConfig.getOffPeakTimezone());
            LocalDateTime utcNow = now.atZone(timezone).toLocalDateTime();
            LocalTime utcCurrentTime = utcNow.toLocalTime();
            
            // Check if current time is within off-peak window
            if (offPeakStart.isBefore(offPeakEnd)) {
                // Normal case: off-peak window doesn't cross midnight
                return utcCurrentTime.isAfter(offPeakStart) && utcCurrentTime.isBefore(offPeakEnd);
            } else {
                // Edge case: off-peak window crosses midnight (e.g., 22:00 to 06:00)
                return utcCurrentTime.isAfter(offPeakStart) || utcCurrentTime.isBefore(offPeakEnd);
            }
            
        } catch (Exception e) {
            logger.error("Error checking off-peak time: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Configure routing for off-peak hours
     */
    private void configureOffPeakRouting(RoutingDecision decision, String requestType) {
        // Off-peak: Use more powerful models, higher quality settings
        switch (requestType.toLowerCase()) {
            case "moderation" -> {
                decision.setSelectedModel("deepseek-reasoner"); // More powerful for complex moderation
                decision.setTemperature(0.1); // Lower temperature for consistency
                decision.setMaxTokens(200); // Allow more detailed responses
                decision.setPriority("high"); // Higher priority during off-peak
            }
            case "chat" -> {
                decision.setSelectedModel("deepseek-chat"); // Standard chat model
                decision.setTemperature(0.7); // Balanced creativity
                decision.setMaxTokens(500); // Standard response length
                decision.setPriority("normal");
            }
            case "analysis" -> {
                decision.setSelectedModel("deepseek-reasoner"); // Best for analysis
                decision.setTemperature(0.3); // Lower temperature for analysis
                decision.setMaxTokens(800); // Longer responses for analysis
                decision.setPriority("high");
            }
            default -> {
                decision.setSelectedModel(deepSeekConfig.getDefaultModel());
                decision.setTemperature(0.5);
                decision.setMaxTokens(300);
                decision.setPriority("normal");
            }
        }
        
        decision.setReason("Off-peak routing: Using enhanced model for better quality");
        decision.setCostMultiplier(1.0); // No cost penalty during off-peak
    }
    
    /**
     * Configure routing for peak hours
     */
    private void configurePeakRouting(RoutingDecision decision, String requestType) {
        // Peak: Use standard models, optimized for speed and cost
        switch (requestType.toLowerCase()) {
            case "moderation" -> {
                decision.setSelectedModel("deepseek-chat"); // Faster, cheaper model
                decision.setTemperature(0.1); // Low temperature for consistency
                decision.setMaxTokens(150); // Shorter responses for speed
                decision.setPriority("normal");
            }
            case "chat" -> {
                decision.setSelectedModel("deepseek-chat"); // Standard model
                decision.setTemperature(0.7); // Balanced creativity
                decision.setMaxTokens(400); // Standard response length
                decision.setPriority("normal");
            }
            case "analysis" -> {
                decision.setSelectedModel("deepseek-chat"); // Use chat model for speed
                decision.setTemperature(0.4); // Slightly lower for analysis
                decision.setMaxTokens(600); // Moderate response length
                decision.setPriority("normal");
            }
            default -> {
                decision.setSelectedModel(deepSeekConfig.getDefaultModel());
                decision.setTemperature(0.5);
                decision.setMaxTokens(300);
                decision.setPriority("normal");
            }
        }
        
        decision.setReason("Peak routing: Using standard model for optimal performance");
        decision.setCostMultiplier(1.0); // Standard cost
    }
    
    /**
     * Create fallback routing configuration
     */
    private RoutingDecision createFallbackRouting(String requestType, UUID userId) {
        RoutingDecision decision = new RoutingDecision();
        decision.setTimestamp(LocalDateTime.now());
        decision.setOffPeak(false);
        decision.setRequestType(requestType);
        decision.setUserId(userId);
        decision.setSelectedModel(deepSeekConfig.getFallbackModel());
        decision.setTemperature(0.5);
        decision.setMaxTokens(300);
        decision.setPriority("normal");
        decision.setReason("Fallback routing: Using default configuration");
        decision.setCostMultiplier(1.0);
        
        return decision;
    }
    
    /**
     * Update routing metrics for analytics
     */
    private void updateRoutingMetrics(RoutingDecision decision) {
        try {
            String key = decision.getRequestType() + ":" + (decision.isOffPeak() ? "offpeak" : "peak");
            RoutingMetrics metrics = routingMetrics.computeIfAbsent(key, k -> new RoutingMetrics());
            
            metrics.incrementRequestCount();
            metrics.setLastUsed(decision.getTimestamp());
            metrics.setLastModel(decision.getSelectedModel());
            
        } catch (Exception e) {
            logger.error("Error updating routing metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get routing statistics
     * 
     * @return Map of routing metrics by type and time window
     */
    public Map<String, RoutingMetrics> getRoutingStatistics() {
        return Map.copyOf(routingMetrics);
    }
    
    /**
     * Get optimal model for specific request characteristics
     * 
     * @param requestType Type of request
     * @param complexity Complexity level (low, medium, high)
     * @param urgency Urgency level (low, medium, high)
     * @return Optimal model name
     */
    public String getOptimalModel(String requestType, String complexity, String urgency) {
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isOffPeak = isOffPeakTime(now);
            
            // High complexity or high urgency during off-peak -> use reasoner
            if (isOffPeak && ("high".equalsIgnoreCase(complexity) || "high".equalsIgnoreCase(urgency))) {
                return "deepseek-reasoner";
            }
            
            // Analysis requests -> prefer reasoner
            if ("analysis".equalsIgnoreCase(requestType)) {
                return isOffPeak ? "deepseek-reasoner" : "deepseek-chat";
            }
            
            // Default routing
            return isOffPeak ? "deepseek-reasoner" : "deepseek-chat";
            
        } catch (Exception e) {
            logger.error("Error getting optimal model: {}", e.getMessage(), e);
            return deepSeekConfig.getDefaultModel();
        }
    }
    
    /**
     * Check if off-peak routing is currently active
     */
    public boolean isOffPeakActive() {
        return isOffPeakTime(LocalDateTime.now());
    }
    
    /**
     * Get off-peak window information
     */
    public OffPeakWindowInfo getOffPeakWindowInfo() {
        try {
            LocalTime offPeakStart = LocalTime.parse(deepSeekConfig.getOffPeakStartTime());
            LocalTime offPeakEnd = LocalTime.parse(deepSeekConfig.getOffPeakEndTime());
            String timezone = deepSeekConfig.getOffPeakTimezone();
            boolean isActive = isOffPeakActive();
            
            return new OffPeakWindowInfo(offPeakStart, offPeakEnd, timezone, isActive);
            
        } catch (Exception e) {
            logger.error("Error getting off-peak window info: {}", e.getMessage(), e);
            return new OffPeakWindowInfo(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, "UTC", false);
        }
    }
    
    /**
     * Routing decision result
     */
    public static class RoutingDecision {
        private LocalDateTime timestamp;
        private boolean offPeak;
        private String requestType;
        private UUID userId;
        private String selectedModel;
        private double temperature;
        private int maxTokens;
        private String priority;
        private String reason;
        private double costMultiplier;
        
        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isOffPeak() { return offPeak; }
        public void setOffPeak(boolean offPeak) { this.offPeak = offPeak; }
        
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getSelectedModel() { return selectedModel; }
        public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
        
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public double getCostMultiplier() { return costMultiplier; }
        public void setCostMultiplier(double costMultiplier) { this.costMultiplier = costMultiplier; }
    }
    
    /**
     * Routing metrics for analytics
     */
    public static class RoutingMetrics {
        private int requestCount = 0;
        private LocalDateTime lastUsed;
        private String lastModel;
        
        public void incrementRequestCount() { requestCount++; }
        
        // Getters and setters
        public int getRequestCount() { return requestCount; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
        public String getLastModel() { return lastModel; }
        public void setLastModel(String lastModel) { this.lastModel = lastModel; }
    }
    
    /**
     * Off-peak window information
     */
    public static class OffPeakWindowInfo {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final String timezone;
        private final boolean isActive;
        
        public OffPeakWindowInfo(LocalTime startTime, LocalTime endTime, String timezone, boolean isActive) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.timezone = timezone;
            this.isActive = isActive;
        }
        
        // Getters
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public String getTimezone() { return timezone; }
        public boolean isActive() { return isActive; }
    }
}
