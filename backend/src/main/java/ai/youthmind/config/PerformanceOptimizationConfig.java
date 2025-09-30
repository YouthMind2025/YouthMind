package ai.youthmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for performance optimization features
 */
@Configuration
@EnableScheduling
public class PerformanceOptimizationConfig {

    /**
     * Thread pool task scheduler for background tasks
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("performance-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Performance optimization properties
     */
    @Bean
    @ConfigurationProperties(prefix = "youthmind.performance")
    public PerformanceProperties performanceProperties() {
        return new PerformanceProperties();
    }

    /**
     * Performance optimization properties
     */
    public static class PerformanceProperties {
        private boolean asyncModerationEnabled = true;
        private boolean batchProcessingEnabled = true;
        private boolean vectorSearchEnabled = true;
        private boolean autoScalingEnabled = true;
        private boolean metricsEnabled = true;
        private int maxConcurrentUsers = 10000;
        private int vectorSearchTimeoutMs = 30;
        private int batchSize = 100;
        private int queueCapacity = 5000;
        private double cpuThresholdHigh = 80.0;
        private double cpuThresholdLow = 20.0;
        private double memoryThresholdHigh = 85.0;
        private double memoryThresholdLow = 30.0;

        // Getters and setters
        public boolean isAsyncModerationEnabled() { return asyncModerationEnabled; }
        public void setAsyncModerationEnabled(boolean asyncModerationEnabled) { this.asyncModerationEnabled = asyncModerationEnabled; }

        public boolean isBatchProcessingEnabled() { return batchProcessingEnabled; }
        public void setBatchProcessingEnabled(boolean batchProcessingEnabled) { this.batchProcessingEnabled = batchProcessingEnabled; }

        public boolean isVectorSearchEnabled() { return vectorSearchEnabled; }
        public void setVectorSearchEnabled(boolean vectorSearchEnabled) { this.vectorSearchEnabled = vectorSearchEnabled; }

        public boolean isAutoScalingEnabled() { return autoScalingEnabled; }
        public void setAutoScalingEnabled(boolean autoScalingEnabled) { this.autoScalingEnabled = autoScalingEnabled; }

        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }

        public int getMaxConcurrentUsers() { return maxConcurrentUsers; }
        public void setMaxConcurrentUsers(int maxConcurrentUsers) { this.maxConcurrentUsers = maxConcurrentUsers; }

        public int getVectorSearchTimeoutMs() { return vectorSearchTimeoutMs; }
        public void setVectorSearchTimeoutMs(int vectorSearchTimeoutMs) { this.vectorSearchTimeoutMs = vectorSearchTimeoutMs; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

        public double getCpuThresholdHigh() { return cpuThresholdHigh; }
        public void setCpuThresholdHigh(double cpuThresholdHigh) { this.cpuThresholdHigh = cpuThresholdHigh; }

        public double getCpuThresholdLow() { return cpuThresholdLow; }
        public void setCpuThresholdLow(double cpuThresholdLow) { this.cpuThresholdLow = cpuThresholdLow; }

        public double getMemoryThresholdHigh() { return memoryThresholdHigh; }
        public void setMemoryThresholdHigh(double memoryThresholdHigh) { this.memoryThresholdHigh = memoryThresholdHigh; }

        public double getMemoryThresholdLow() { return memoryThresholdLow; }
        public void setMemoryThresholdLow(double memoryThresholdLow) { this.memoryThresholdLow = memoryThresholdLow; }
    }
}
