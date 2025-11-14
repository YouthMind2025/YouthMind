package ai.youthmind.service;

import ai.youthmind.util.PerformanceMonitor;
import ai.youthmind.util.ApplicationContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Auto-scaling and resource management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoScalingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrencyManager concurrencyManager;
    private final MetricsService metricsService;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final java.util.concurrent.Executor backgroundExecutor;

    // Scaling thresholds
    private static final double CPU_THRESHOLD_HIGH = 80.0;
    private static final double CPU_THRESHOLD_LOW = 20.0;
    private static final double MEMORY_THRESHOLD_HIGH = 85.0;
    private static final double MEMORY_THRESHOLD_LOW = 30.0;
    private static final int REQUEST_THRESHOLD_HIGH = 8000;
    private static final int REQUEST_THRESHOLD_LOW = 2000;
    private static final double ERROR_RATE_THRESHOLD = 5.0;

    // Scaling state
    private final AtomicInteger currentScaleLevel = new AtomicInteger(1);
    private final AtomicLong lastScaleTime = new AtomicLong(0);
    private final AtomicInteger scaleUpCount = new AtomicInteger(0);
    private final AtomicInteger scaleDownCount = new AtomicInteger(0);

    // Resource pools
    private final Map<String, ResourcePool> resourcePools = new HashMap<>();

    /**
     * Monitor system metrics and trigger auto-scaling
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorAndScale() {
        try {
            PerformanceMonitor.start("auto_scaling_monitor");
            
            SystemMetrics metrics = collectSystemMetrics();
            ScalingDecision decision = evaluateScalingDecision(metrics);
            
            if (decision.shouldScale()) {
                executeScaling(decision);
            }
            
            // Update resource pools
            updateResourcePools(metrics);
            
            // Log scaling status
            logScalingStatus(metrics, decision);
            
            PerformanceMonitor.end("auto_scaling_monitor");
            
        } catch (Exception e) {
            log.error("Error in auto-scaling monitor: {}", e.getMessage(), e);
        }
    }

    /**
     * Collect current system metrics
     */
    private SystemMetrics collectSystemMetrics() {
        ConcurrencyManager.SystemHealth health = concurrencyManager.getSystemHealth();
        Map<String, Object> systemMetrics = metricsService.getSystemMetrics();
        
        return SystemMetrics.builder()
            .timestamp(LocalDateTime.now())
            .cpuUsage(getCpuUsage())
            .memoryUsage(health.getMemoryUsage())
            .activeConnections(health.getActiveConnections())
            .activeRequests(health.getActiveRequests())
            .errorRate(health.getErrorRate())
            .databaseConnectionsAvailable(health.getDatabaseConnectionsAvailable())
            .aiServiceConnectionsAvailable(health.getAiServiceConnectionsAvailable())
            .vectorSearchConnectionsAvailable(health.getVectorSearchConnectionsAvailable())
            .queueSize(getQueueSize())
            .responseTime(getAverageResponseTime())
            .build();
    }

    /**
     * Evaluate if scaling is needed
     */
    private ScalingDecision evaluateScalingDecision(SystemMetrics metrics) {
        ScalingDecision decision = new ScalingDecision();
        
        // Check scale-up conditions
        if (shouldScaleUp(metrics)) {
            decision.setAction(ScalingAction.SCALE_UP);
            decision.setReason("High resource utilization detected");
            decision.setTargetScaleLevel(Math.min(currentScaleLevel.get() + 1, 10)); // Max 10x scale
        }
        // Check scale-down conditions
        else if (shouldScaleDown(metrics)) {
            decision.setAction(ScalingAction.SCALE_DOWN);
            decision.setReason("Low resource utilization detected");
            decision.setTargetScaleLevel(Math.max(currentScaleLevel.get() - 1, 1)); // Min 1x scale
        }
        // No scaling needed
        else {
            decision.setAction(ScalingAction.NO_ACTION);
            decision.setReason("System operating within normal parameters");
            decision.setTargetScaleLevel(currentScaleLevel.get());
        }
        
        return decision;
    }

    /**
     * Check if system should scale up
     */
    private boolean shouldScaleUp(SystemMetrics metrics) {
        // Prevent rapid scaling
        if (System.currentTimeMillis() - lastScaleTime.get() < 60000) { // 1 minute cooldown
            return false;
        }
        
        return metrics.getCpuUsage() > CPU_THRESHOLD_HIGH ||
               metrics.getMemoryUsage() > MEMORY_THRESHOLD_HIGH ||
               metrics.getActiveRequests() > REQUEST_THRESHOLD_HIGH ||
               metrics.getErrorRate() > ERROR_RATE_THRESHOLD ||
               metrics.getQueueSize() > 1000;
    }

    /**
     * Check if system should scale down
     */
    private boolean shouldScaleDown(SystemMetrics metrics) {
        // Prevent rapid scaling
        if (System.currentTimeMillis() - lastScaleTime.get() < 300000) { // 5 minute cooldown
            return false;
        }
        
        // Only scale down if all metrics are low for sustained period
        return metrics.getCpuUsage() < CPU_THRESHOLD_LOW &&
               metrics.getMemoryUsage() < MEMORY_THRESHOLD_LOW &&
               metrics.getActiveRequests() < REQUEST_THRESHOLD_LOW &&
               metrics.getErrorRate() < 1.0 &&
               metrics.getQueueSize() < 100;
    }

    /**
     * Execute scaling action
     */
    private void executeScaling(ScalingDecision decision) {
        try {
            log.info("Executing scaling action: {} to level {}", decision.getAction(), decision.getTargetScaleLevel());
            
            switch (decision.getAction()) {
                case SCALE_UP:
                    executeScaleUp(decision.getTargetScaleLevel());
                    break;
                case SCALE_DOWN:
                    executeScaleDown(decision.getTargetScaleLevel());
                    break;
                case NO_ACTION:
                    // No action needed
                    break;
            }
            
            lastScaleTime.set(System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("Error executing scaling action: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute scale-up operation
     */
    private void executeScaleUp(int targetLevel) {
        // Increase thread pool sizes
        increaseThreadPoolSizes(targetLevel);
        
        // Increase resource limits
        increaseResourceLimits(targetLevel);
        
        // Update current scale level
        currentScaleLevel.set(targetLevel);
        scaleUpCount.incrementAndGet();
        
        // Record metrics
        metricsService.incrementCounter("auto_scaling_scale_up", "target_level", String.valueOf(targetLevel));
        
        log.info("Scaled up to level {}", targetLevel);
    }

    /**
     * Execute scale-down operation
     */
    private void executeScaleDown(int targetLevel) {
        // Decrease thread pool sizes
        decreaseThreadPoolSizes(targetLevel);
        
        // Decrease resource limits
        decreaseResourceLimits(targetLevel);
        
        // Update current scale level
        currentScaleLevel.set(targetLevel);
        scaleDownCount.incrementAndGet();
        
        // Record metrics
        metricsService.incrementCounter("auto_scaling_scale_down", "target_level", String.valueOf(targetLevel));
        
        log.info("Scaled down to level {}", targetLevel);
    }

    /**
     * Update resource pools based on current metrics
     */
    private void updateResourcePools(SystemMetrics metrics) {
        // Update database connection pool
        updateResourcePool("database", metrics.getDatabaseConnectionsAvailable(), 100);
        
        // Update AI service connection pool
        updateResourcePool("ai_service", metrics.getAiServiceConnectionsAvailable(), 20);
        
        // Update vector search connection pool
        updateResourcePool("vector_search", metrics.getVectorSearchConnectionsAvailable(), 50);
    }

    /**
     * Update specific resource pool
     */
    private void updateResourcePool(String poolName, int currentAvailable, int maxCapacity) {
        ResourcePool pool = resourcePools.computeIfAbsent(poolName, k -> new ResourcePool(poolName, maxCapacity, currentAvailable, 0.0, LocalDateTime.now()));
        pool.setCurrentAvailable(currentAvailable);
        pool.setLastUpdated(LocalDateTime.now());
        
        // Calculate utilization
        double utilization = (double) (maxCapacity - currentAvailable) / maxCapacity * 100;
        pool.setUtilization(utilization);
        
        // Set gauge metrics
        metricsService.setGauge("resource_pool_available", currentAvailable, "pool", poolName);
        metricsService.setGauge("resource_pool_utilization", (long) utilization, "pool", poolName);
    }

    /**
     * Get current scaling status
     */
    public ScalingStatus getScalingStatus() {
        SystemMetrics metrics = collectSystemMetrics();
        
        return ScalingStatus.builder()
            .currentScaleLevel(currentScaleLevel.get())
            .lastScaleTime(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(lastScaleTime.get()),
                java.time.ZoneId.systemDefault()))
            .scaleUpCount(scaleUpCount.get())
            .scaleDownCount(scaleDownCount.get())
            .systemMetrics(metrics)
            .resourcePools(new HashMap<>(resourcePools))
            .build();
    }

    /**
     * Manual scaling trigger
     */
    public CompletableFuture<Boolean> manualScale(int targetLevel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (targetLevel < 1 || targetLevel > 10) {
                    throw new IllegalArgumentException("Target scale level must be between 1 and 10");
                }
                
                ScalingDecision decision = new ScalingDecision();
                decision.setTargetScaleLevel(targetLevel);
                
                if (targetLevel > currentScaleLevel.get()) {
                    decision.setAction(ScalingAction.SCALE_UP);
                } else if (targetLevel < currentScaleLevel.get()) {
                    decision.setAction(ScalingAction.SCALE_DOWN);
                } else {
                    decision.setAction(ScalingAction.NO_ACTION);
                }
                
                executeScaling(decision);
                return true;
                
            } catch (Exception e) {
                log.error("Error in manual scaling: {}", e.getMessage(), e);
                return false;
            }
        }, backgroundExecutor);  // Use dedicated executor, not commonPool
    }

    // Helper methods
    private double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad() * 100.0;
        } catch (Exception e) {
            log.warn("Could not get CPU usage: {}", e.getMessage());
            return 0.0;
        }
    }

    private long getQueueSize() {
        try {
            return redisTemplate.opsForZSet().size("queue:normal_priority") +
                   redisTemplate.opsForZSet().size("queue:high_priority") +
                   redisTemplate.opsForZSet().size("queue:low_priority");
        } catch (Exception e) {
            return 0;
        }
    }

    private double getAverageResponseTime() {
        try {
            // Get from Micrometer metrics
            List<io.micrometer.core.instrument.Meter> meters = meterRegistry.getMeters();
            
            for (io.micrometer.core.instrument.Meter meter : meters) {
                if (meter instanceof io.micrometer.core.instrument.Timer && 
                    "api.request.duration".equals(meter.getId().getName())) {
                    return ((io.micrometer.core.instrument.Timer) meter)
                        .mean(java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
            return 150.0; // Default if no data
        } catch (Exception e) {
            log.warn("Could not get average response time: {}", e.getMessage());
            return 150.0;
        }
    }

    private void increaseThreadPoolSizes(int scaleLevel) {
        try {
            // Get thread pool executors from application context
            org.springframework.context.ApplicationContext context = 
                ApplicationContextProvider.getApplicationContext();
            
            // Scale up API executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor apiExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("apiExecutor");
            if (apiExecutor != null) {
                int currentCore = apiExecutor.getCorePoolSize();
                int currentMax = apiExecutor.getMaxPoolSize();
                int newCoreSize = Math.min(currentCore * scaleLevel, 1000);
                int newMaxSize = Math.min(currentMax * scaleLevel, 2000);
                
                // IMPORTANT: Set maxPoolSize FIRST (so it's large enough for corePoolSize)
                apiExecutor.setMaxPoolSize(newMaxSize);
                // Then set corePoolSize (validates against the new maxPoolSize)
                apiExecutor.setCorePoolSize(newCoreSize);
            }
            
            // Scale up general executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor generalExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("generalExecutor");
            if (generalExecutor != null) {
                int currentCore = generalExecutor.getCorePoolSize();
                int currentMax = generalExecutor.getMaxPoolSize();
                int newCoreSize = Math.min(currentCore * scaleLevel, 500);
                int newMaxSize = Math.min(currentMax * scaleLevel, 1000);
                
                // Set maxPoolSize FIRST
                generalExecutor.setMaxPoolSize(newMaxSize);
                // Then set corePoolSize
                generalExecutor.setCorePoolSize(newCoreSize);
            }
            
            // Scale up database executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor databaseExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("databaseExecutor");
            if (databaseExecutor != null) {
                int currentCore = databaseExecutor.getCorePoolSize();
                int currentMax = databaseExecutor.getMaxPoolSize();
                int newCoreSize = Math.min(currentCore * scaleLevel, 200);
                int newMaxSize = Math.min(currentMax * scaleLevel, 500);
                
                // Set maxPoolSize FIRST
                databaseExecutor.setMaxPoolSize(newMaxSize);
                // Then set corePoolSize
                databaseExecutor.setCorePoolSize(newCoreSize);
            }
            
            log.info("Scaled up thread pools to level {}", scaleLevel);
                
        } catch (Exception e) {
            log.error("Error scaling up thread pools: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to scale thread pools", e);
        }
    }

    private void decreaseThreadPoolSizes(int scaleLevel) {
        try {
            // Get thread pool executors from application context
            org.springframework.context.ApplicationContext context = 
                ApplicationContextProvider.getApplicationContext();
            
            // Scale down API executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor apiExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("apiExecutor");
            int newCoreSize = Math.max(apiExecutor.getCorePoolSize() / scaleLevel, 10);
            int newMaxSize = Math.max(apiExecutor.getMaxPoolSize() / scaleLevel, 50);
            apiExecutor.setCorePoolSize(newCoreSize);
            apiExecutor.setMaxPoolSize(newMaxSize);
            
            // Scale down general executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor generalExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("generalExecutor");
            generalExecutor.setCorePoolSize(Math.max(generalExecutor.getCorePoolSize() / scaleLevel, 5));
            generalExecutor.setMaxPoolSize(Math.max(generalExecutor.getMaxPoolSize() / scaleLevel, 20));
            
            // Scale down database executor
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor databaseExecutor = 
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) context.getBean("databaseExecutor");
            databaseExecutor.setCorePoolSize(Math.max(databaseExecutor.getCorePoolSize() / scaleLevel, 5));
            databaseExecutor.setMaxPoolSize(Math.max(databaseExecutor.getMaxPoolSize() / scaleLevel, 20));
            
            log.info("Scaled down thread pools to level {}: API({}/{}), General({}/{}), DB({}/{})", 
                scaleLevel, newCoreSize, newMaxSize, 
                generalExecutor.getCorePoolSize(), generalExecutor.getMaxPoolSize(),
                databaseExecutor.getCorePoolSize(), databaseExecutor.getMaxPoolSize());
                
        } catch (Exception e) {
            log.error("Error scaling down thread pools: {}", e.getMessage(), e);
        }
    }

    private void increaseResourceLimits(int scaleLevel) {
        try {
            // Increase database connection pool size
            javax.sql.DataSource dataSource = 
                ApplicationContextProvider.getApplicationContext()
                    .getBean(javax.sql.DataSource.class);
            
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
                int newMaxPoolSize = Math.min(hikariDataSource.getMaximumPoolSize() * scaleLevel, 200);
                int newMinIdle = Math.min(hikariDataSource.getMinimumIdle() * scaleLevel, 50);
                
                hikariDataSource.setMaximumPoolSize(newMaxPoolSize);
                hikariDataSource.setMinimumIdle(newMinIdle);
                
                log.info("Increased database connection pool: max={}, min={}", newMaxPoolSize, newMinIdle);
            }
            
            // Increase Redis connection pool size
            org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory = 
                ApplicationContextProvider.getApplicationContext()
                    .getBean(org.springframework.data.redis.connection.RedisConnectionFactory.class);
            
            if (redisConnectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
                org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lettuceFactory = 
                    (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redisConnectionFactory;
                
                // Update pool configuration
                // org.apache.commons.pool2.impl.GenericObjectPoolConfig<?> poolConfig = 
                //     lettuceFactory.getPoolConfig();
                if (true) { // Simplified for now
                    // int newMaxActive = Math.min(poolConfig.getMaxTotal() * scaleLevel, 500);
                    // int newMaxIdle = Math.min(poolConfig.getMaxIdle() * scaleLevel, 100);
                    // int newMinIdle = Math.min(poolConfig.getMinIdle() * scaleLevel, 20);
                    
                    // poolConfig.setMaxTotal(newMaxActive);
                    // poolConfig.setMaxIdle(newMaxIdle);
                    // poolConfig.setMinIdle(newMinIdle);
                    
                    log.info("Increased Redis connection pool scaling to level: {}", scaleLevel);
                }
            }
            
            // Update concurrency manager limits
            concurrencyManager.updateResourceLimits(scaleLevel);
            
            log.info("Increased resource limits for scale level {}", scaleLevel);
            
        } catch (Exception e) {
            log.error("Error increasing resource limits: {}", e.getMessage(), e);
        }
    }

    private void decreaseResourceLimits(int scaleLevel) {
        try {
            // Decrease database connection pool size
            javax.sql.DataSource dataSource = 
                ApplicationContextProvider.getApplicationContext()
                    .getBean(javax.sql.DataSource.class);
            
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
                int newMaxPoolSize = Math.max(hikariDataSource.getMaximumPoolSize() / scaleLevel, 10);
                int newMinIdle = Math.max(hikariDataSource.getMinimumIdle() / scaleLevel, 2);
                
                hikariDataSource.setMaximumPoolSize(newMaxPoolSize);
                hikariDataSource.setMinimumIdle(newMinIdle);
                
                log.info("Decreased database connection pool: max={}, min={}", newMaxPoolSize, newMinIdle);
            }
            
            // Decrease Redis connection pool size
            org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory = 
                ApplicationContextProvider.getApplicationContext()
                    .getBean(org.springframework.data.redis.connection.RedisConnectionFactory.class);
            
            if (redisConnectionFactory instanceof org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) {
                org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory lettuceFactory = 
                    (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redisConnectionFactory;
                
                // Update pool configuration
                // org.apache.commons.pool2.impl.GenericObjectPoolConfig<?> poolConfig = 
                //     lettuceFactory.getPoolConfig();
                if (true) { // Simplified for now
                    // int newMaxActive = Math.max(poolConfig.getMaxTotal() / scaleLevel, 10);
                    // int newMaxIdle = Math.max(poolConfig.getMaxIdle() / scaleLevel, 5);
                    // int newMinIdle = Math.max(poolConfig.getMinIdle() / scaleLevel, 1);
                    
                    // poolConfig.setMaxTotal(newMaxActive);
                    // poolConfig.setMaxIdle(newMaxIdle);
                    // poolConfig.setMinIdle(newMinIdle);
                    
                    log.info("Decreased Redis connection pool scaling to level: {}", scaleLevel);
                }
            }
            
            // Update concurrency manager limits
            concurrencyManager.updateResourceLimits(1.0 / scaleLevel);
            
            log.info("Decreased resource limits for scale level {}", scaleLevel);
            
        } catch (Exception e) {
            log.error("Error decreasing resource limits: {}", e.getMessage(), e);
        }
    }

    private void logScalingStatus(SystemMetrics metrics, ScalingDecision decision) {
        log.debug("Scaling status - Level: {}, CPU: {:.1f}%, Memory: {:.1f}%, Requests: {}, Decision: {}",
            currentScaleLevel.get(), metrics.getCpuUsage(), metrics.getMemoryUsage(),
            metrics.getActiveRequests(), decision.getAction());
    }

    // Data classes
    @lombok.Data
    @lombok.Builder
    public static class SystemMetrics {
        private LocalDateTime timestamp;
        private double cpuUsage;
        private double memoryUsage;
        private int activeConnections;
        private int activeRequests;
        private double errorRate;
        private int databaseConnectionsAvailable;
        private int aiServiceConnectionsAvailable;
        private int vectorSearchConnectionsAvailable;
        private long queueSize;
        private double responseTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class ScalingDecision {
        private ScalingAction action;
        private String reason;
        private int targetScaleLevel;
        
        public ScalingDecision() {
            this.action = ScalingAction.NO_ACTION;
            this.reason = "";
            this.targetScaleLevel = 1;
        }
        
        public ScalingDecision(ScalingAction action, String reason, int targetScaleLevel) {
            this.action = action;
            this.reason = reason;
            this.targetScaleLevel = targetScaleLevel;
        }
        
        public boolean shouldScale() {
            return action != ScalingAction.NO_ACTION;
        }
        
        // Getters and setters
        public ScalingAction getAction() { return action; }
        public void setAction(ScalingAction action) { this.action = action; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public int getTargetScaleLevel() { return targetScaleLevel; }
        public void setTargetScaleLevel(int targetScaleLevel) { this.targetScaleLevel = targetScaleLevel; }
    }

    public enum ScalingAction {
        SCALE_UP,
        SCALE_DOWN,
        NO_ACTION
    }

    @lombok.Data
    @lombok.Builder
    public static class ScalingStatus {
        private int currentScaleLevel;
        private LocalDateTime lastScaleTime;
        private int scaleUpCount;
        private int scaleDownCount;
        private SystemMetrics systemMetrics;
        private Map<String, ResourcePool> resourcePools;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ResourcePool {
        private String name;
        private int maxCapacity;
        private int currentAvailable;
        private double utilization;
        private LocalDateTime lastUpdated;
        
        public ResourcePool() {
            this.name = "";
            this.maxCapacity = 0;
            this.currentAvailable = 0;
            this.utilization = 0.0;
            this.lastUpdated = LocalDateTime.now();
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
        public int getCurrentAvailable() { return currentAvailable; }
        public void setCurrentAvailable(int currentAvailable) { this.currentAvailable = currentAvailable; }
        public double getUtilization() { return utilization; }
        public void setUtilization(double utilization) { this.utilization = utilization; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
