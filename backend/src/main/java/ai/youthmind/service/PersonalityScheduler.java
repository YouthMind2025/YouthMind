package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for scheduling personality computation
 * Handles weekly/monthly computation triggers and tracking
 */
@Service
@Transactional
public class PersonalityScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityScheduler.class);
    
    // Scheduling constants
    private static final long WEEKLY_COMPUTATION_INTERVAL_HOURS = 168; // 1 week
    private static final long MONTHLY_COMPUTATION_INTERVAL_HOURS = 720; // 1 month
    private static final long COMPUTATION_HISTORY_RETENTION_DAYS = 90; // 3 months
    private static final int MAX_CONCURRENT_COMPUTATIONS = 10; // Maximum concurrent computations
    
    // Redis keys
    private static final String SCHEDULED_COMPUTATIONS_KEY = "scheduled_computations:";
    private static final String COMPUTATION_HISTORY_KEY = "computation_history:";
    private static final String ACTIVE_COMPUTATIONS_KEY = "active_computations:";
    private static final String USER_LAST_COMPUTATION_KEY = "user_last_computation:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private PersonalityComputationService personalityComputationService;
    
    /**
     * Schedule personality computation for a user
     * @param userId User ID
     * @param triggerType Type of trigger (WEEKLY, MONTHLY, MANUAL, etc.)
     * @return Scheduling result
     */
    public Map<String, Object> scheduleComputation(UUID userId, String triggerType) {
        try {
            logger.info("Scheduling personality computation for user {} with trigger {}", userId, triggerType);
            
            // Check if computation is already scheduled
            String scheduledKey = SCHEDULED_COMPUTATIONS_KEY + userId;
            Map<String, Object> existingSchedule = (Map<String, Object>) redisTemplate.opsForValue().get(scheduledKey);
            
            if (existingSchedule != null) {
                LocalDateTime scheduledTime = (LocalDateTime) existingSchedule.get("scheduledTime");
                if (scheduledTime != null && scheduledTime.isAfter(LocalDateTime.now())) {
                    logger.debug("Computation already scheduled for user {} at {}", userId, scheduledTime);
                    return Map.of(
                        "status", "ALREADY_SCHEDULED",
                        "message", "Computation already scheduled",
                        "scheduledTime", scheduledTime,
                        "userId", userId.toString()
                    );
                }
            }
            
            // Determine computation interval based on trigger type
            long intervalHours = determineComputationInterval(triggerType);
            
            // Check if enough time has passed since last computation
            if (!shouldTriggerComputation(userId, intervalHours)) {
                logger.debug("Not enough time passed since last computation for user {}", userId);
                return Map.of(
                    "status", "SKIPPED",
                    "message", "Not enough time passed since last computation",
                    "userId", userId.toString()
                );
            }
            
            // Schedule the computation
            LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1); // Schedule for 1 hour from now
            
            Map<String, Object> schedule = new HashMap<>();
            schedule.put("userId", userId.toString());
            schedule.put("triggerType", triggerType);
            schedule.put("scheduledTime", scheduledTime);
            schedule.put("intervalHours", intervalHours);
            schedule.put("status", "SCHEDULED");
            schedule.put("createdAt", LocalDateTime.now());
            
            // Store schedule
            redisTemplate.opsForValue().set(scheduledKey, schedule, 2, TimeUnit.DAYS);
            
            logger.info("Personality computation scheduled for user {} at {}", userId, scheduledTime);
            return Map.of(
                "status", "SCHEDULED",
                "message", "Computation scheduled successfully",
                "scheduledTime", scheduledTime,
                "userId", userId.toString(),
                "triggerType", triggerType
            );
            
        } catch (Exception e) {
            logger.error("Error scheduling computation for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to schedule computation: " + e.getMessage(),
                "userId", userId.toString()
            );
        }
    }
    
    /**
     * Trigger personality computation for a user
     * @param userId User ID
     * @param triggerType Type of trigger
     * @return Computation result
     */
    @Async
    public CompletableFuture<Map<String, Object>> triggerComputation(UUID userId, String triggerType) {
        try {
            logger.info("Triggering personality computation for user {} with trigger {}", userId, triggerType);
            
            // Check if computation is already running
            if (isComputationRunning(userId)) {
                logger.debug("Computation already running for user {}", userId);
                return CompletableFuture.completedFuture(Map.of(
                    "status", "ALREADY_RUNNING",
                    "message", "Computation already running",
                    "userId", userId.toString()
                ));
            }
            
            // Mark computation as running
            markComputationAsRunning(userId);
            
            try {
                // Perform the computation
                Map<String, Object> result = personalityComputationService.triggerPersonalityComputation(userId, true);
                
                // Track computation history
                trackComputationHistory(userId, triggerType, result);
                
                // Update last computation time
                updateLastComputationTime(userId);
                
                logger.info("Personality computation completed for user {}", userId);
                return CompletableFuture.completedFuture(result);
                
            } finally {
                // Mark computation as completed
                markComputationAsCompleted(userId);
            }
            
        } catch (Exception e) {
            logger.error("Error triggering computation for user {}: {}", userId, e.getMessage(), e);
            markComputationAsCompleted(userId);
            return CompletableFuture.completedFuture(Map.of(
                "status", "ERROR",
                "message", "Failed to trigger computation: " + e.getMessage(),
                "userId", userId.toString()
            ));
        }
    }
    
    /**
     * Track computation history for analysis
     * @param userId User ID
     * @param triggerType Type of trigger
     * @param result Computation result
     */
    public void trackComputationHistory(UUID userId, String triggerType, Map<String, Object> result) {
        try {
            logger.debug("Tracking computation history for user {}", userId);
            
            String historyKey = COMPUTATION_HISTORY_KEY + userId;
            
            // Get existing history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            if (history == null) {
                history = new HashMap<>();
            }
            
            // Add new computation record
            List<Map<String, Object>> computations = (List<Map<String, Object>>) history.get("computations");
            if (computations == null) {
                computations = new ArrayList<>();
            }
            
            Map<String, Object> computation = new HashMap<>();
            computation.put("timestamp", LocalDateTime.now());
            computation.put("triggerType", triggerType);
            computation.put("status", result.get("status"));
            computation.put("result", result);
            
            computations.add(computation);
            
            // Keep only recent computations (last 100)
            if (computations.size() > 100) {
                computations = computations.subList(computations.size() - 100, computations.size());
            }
            
            // Calculate statistics
            Map<String, Object> statistics = calculateComputationStatistics(computations);
            
            history.put("computations", computations);
            history.put("statistics", statistics);
            history.put("lastUpdated", LocalDateTime.now());
            
            // Store updated history
            redisTemplate.opsForValue().set(historyKey, history, COMPUTATION_HISTORY_RETENTION_DAYS, TimeUnit.DAYS);
            
            logger.debug("Computation history tracked for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error tracking computation history for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get computation history for a user
     * @param userId User ID
     * @return Computation history
     */
    public Map<String, Object> getComputationHistory(UUID userId) {
        try {
            logger.debug("Getting computation history for user {}", userId);
            
            String historyKey = COMPUTATION_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            return history != null ? history : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting computation history for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Scheduled task to process pending computations
     * Runs every hour to check for scheduled computations
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void processScheduledComputations() {
        try {
            logger.debug("Processing scheduled computations");
            
            // Get all scheduled computations
            Set<String> scheduledKeys = redisTemplate.keys(SCHEDULED_COMPUTATIONS_KEY + "*");
            
            if (scheduledKeys == null || scheduledKeys.isEmpty()) {
                logger.debug("No scheduled computations found");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            int processedCount = 0;
            
            for (String key : scheduledKeys) {
                try {
                    Map<String, Object> schedule = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (schedule == null) continue;
                    
                    LocalDateTime scheduledTime = (LocalDateTime) schedule.get("scheduledTime");
                    String userIdStr = (String) schedule.get("userId");
                    String triggerType = (String) schedule.get("triggerType");
                    
                    if (scheduledTime != null && scheduledTime.isBefore(now) && userIdStr != null) {
                        UUID userId = UUID.fromString(userIdStr);
                        
                        // Trigger computation asynchronously
                        triggerComputation(userId, triggerType);
                        
                        // Remove from scheduled computations
                        redisTemplate.delete(key);
                        
                        processedCount++;
                        
                        logger.debug("Processed scheduled computation for user {}", userId);
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing scheduled computation for key {}: {}", key, e.getMessage(), e);
                }
            }
            
            logger.debug("Processed {} scheduled computations", processedCount);
            
        } catch (Exception e) {
            logger.error("Error processing scheduled computations: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task for weekly personality computations
     * Runs every Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void scheduleWeeklyComputations() {
        try {
            logger.info("Scheduling weekly personality computations");
            
            // This would typically get all active users from the database
            // For now, we'll use a placeholder
            List<UUID> activeUsers = getActiveUsers();
            
            int scheduledCount = 0;
            for (UUID userId : activeUsers) {
                Map<String, Object> result = scheduleComputation(userId, "WEEKLY");
                if ("SCHEDULED".equals(result.get("status"))) {
                    scheduledCount++;
                }
            }
            
            logger.info("Scheduled {} weekly personality computations", scheduledCount);
            
        } catch (Exception e) {
            logger.error("Error scheduling weekly computations: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task for monthly personality computations
     * Runs on the 1st of every month at 3 AM
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void scheduleMonthlyComputations() {
        try {
            logger.info("Scheduling monthly personality computations");
            
            // This would typically get all active users from the database
            // For now, we'll use a placeholder
            List<UUID> activeUsers = getActiveUsers();
            
            int scheduledCount = 0;
            for (UUID userId : activeUsers) {
                Map<String, Object> result = scheduleComputation(userId, "MONTHLY");
                if ("SCHEDULED".equals(result.get("status"))) {
                    scheduledCount++;
                }
            }
            
            logger.info("Scheduled {} monthly personality computations", scheduledCount);
            
        } catch (Exception e) {
            logger.error("Error scheduling monthly computations: {}", e.getMessage(), e);
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private long determineComputationInterval(String triggerType) {
        switch (triggerType.toUpperCase()) {
            case "WEEKLY":
                return WEEKLY_COMPUTATION_INTERVAL_HOURS;
            case "MONTHLY":
                return MONTHLY_COMPUTATION_INTERVAL_HOURS;
            case "MANUAL":
            case "MANUAL_TRIGGER":
                return 1; // Allow manual triggers anytime
            default:
                return WEEKLY_COMPUTATION_INTERVAL_HOURS; // Default to weekly
        }
    }
    
    private boolean shouldTriggerComputation(UUID userId, long intervalHours) {
        try {
            String lastComputationKey = USER_LAST_COMPUTATION_KEY + userId;
            LocalDateTime lastComputation = (LocalDateTime) redisTemplate.opsForValue().get(lastComputationKey);
            
            if (lastComputation == null) {
                return true; // No previous computation, allow it
            }
            
            long hoursSinceLastComputation = ChronoUnit.HOURS.between(lastComputation, LocalDateTime.now());
            return hoursSinceLastComputation >= intervalHours;
            
        } catch (Exception e) {
            logger.warn("Error checking computation interval for user {}: {}", userId, e.getMessage());
            return true; // Allow computation on error
        }
    }
    
    private boolean isComputationRunning(UUID userId) {
        try {
            String activeKey = ACTIVE_COMPUTATIONS_KEY + userId;
            return redisTemplate.hasKey(activeKey);
        } catch (Exception e) {
            logger.warn("Error checking if computation is running for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    private void markComputationAsRunning(UUID userId) {
        try {
            String activeKey = ACTIVE_COMPUTATIONS_KEY + userId;
            Map<String, Object> activeComputation = new HashMap<>();
            activeComputation.put("userId", userId.toString());
            activeComputation.put("startedAt", LocalDateTime.now());
            activeComputation.put("status", "RUNNING");
            
            redisTemplate.opsForValue().set(activeKey, activeComputation, 2, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.warn("Error marking computation as running for user {}: {}", userId, e.getMessage());
        }
    }
    
    private void markComputationAsCompleted(UUID userId) {
        try {
            String activeKey = ACTIVE_COMPUTATIONS_KEY + userId;
            redisTemplate.delete(activeKey);
        } catch (Exception e) {
            logger.warn("Error marking computation as completed for user {}: {}", userId, e.getMessage());
        }
    }
    
    private void updateLastComputationTime(UUID userId) {
        try {
            String lastComputationKey = USER_LAST_COMPUTATION_KEY + userId;
            redisTemplate.opsForValue().set(lastComputationKey, LocalDateTime.now(), 30, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.warn("Error updating last computation time for user {}: {}", userId, e.getMessage());
        }
    }
    
    private Map<String, Object> calculateComputationStatistics(List<Map<String, Object>> computations) {
        Map<String, Object> statistics = new HashMap<>();
        
        if (computations.isEmpty()) {
            statistics.put("totalComputations", 0);
            statistics.put("successfulComputations", 0);
            statistics.put("failedComputations", 0);
            statistics.put("successRate", 0.0);
            return statistics;
        }
        
        int totalComputations = computations.size();
        int successfulComputations = 0;
        int failedComputations = 0;
        
        for (Map<String, Object> computation : computations) {
            String status = (String) computation.get("status");
            if ("COMPLETED".equals(status)) {
                successfulComputations++;
            } else if ("ERROR".equals(status)) {
                failedComputations++;
            }
        }
        
        double successRate = totalComputations > 0 ? (double) successfulComputations / totalComputations : 0.0;
        
        statistics.put("totalComputations", totalComputations);
        statistics.put("successfulComputations", successfulComputations);
        statistics.put("failedComputations", failedComputations);
        statistics.put("successRate", successRate);
        
        // Calculate average time between computations
        if (computations.size() >= 2) {
            Map<String, Object> firstComputation = computations.get(0);
            Map<String, Object> lastComputation = computations.get(computations.size() - 1);
            
            LocalDateTime firstTime = (LocalDateTime) firstComputation.get("timestamp");
            LocalDateTime lastTime = (LocalDateTime) lastComputation.get("timestamp");
            
            if (firstTime != null && lastTime != null) {
                long totalHours = ChronoUnit.HOURS.between(firstTime, lastTime);
                double avgHoursBetweenComputations = totalHours / (double) (computations.size() - 1);
                statistics.put("avgHoursBetweenComputations", avgHoursBetweenComputations);
            }
        }
        
        return statistics;
    }
    
    private List<UUID> getActiveUsers() {
        // This is a placeholder implementation
        // In a real implementation, this would query the database for active users
        // For now, return an empty list
        return new ArrayList<>();
    }
}
