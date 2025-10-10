package ai.youthmind.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring utility for tracking method execution times and performance metrics.
 * 
 * This utility provides:
 * - Method execution time tracking
 * - Performance statistics aggregation
 * - Memory usage monitoring
 * - Performance threshold alerts
 * 
 * Usage:
 * ```java
 * PerformanceMonitor.start("methodName");
 * // ... method execution ...
 * PerformanceMonitor.end("methodName");
 * ```
 */
@Component
public class PerformanceMonitor {
    
    private static final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Instant> activeTimers = new ConcurrentHashMap<>();
    
    /**
     * Start timing a method execution
     * 
     * @param methodName The name of the method being timed
     */
    public static void start(String methodName) {
        activeTimers.put(methodName, Instant.now());
    }
    
    /**
     * End timing a method execution and record statistics
     * 
     * @param methodName The name of the method being timed
     */
    public static void end(String methodName) {
        Instant startTime = activeTimers.remove(methodName);
        if (startTime != null) {
            Duration duration = Duration.between(startTime, Instant.now());
            recordExecutionTime(methodName, duration.toNanos());
        }
    }
    
    /**
     * Record execution time for a method
     * 
     * @param methodName The name of the method
     * @param executionTimeNanos Execution time in nanoseconds
     */
    private static void recordExecutionTime(String methodName, long executionTimeNanos) {
        methodStats.compute(methodName, (key, existingStats) -> {
            if (existingStats == null) {
                return new MethodStats(methodName, executionTimeNanos);
            } else {
                existingStats.recordExecution(executionTimeNanos);
                return existingStats;
            }
        });
    }
    
    /**
     * Get performance statistics for a specific method
     * 
     * @param methodName The name of the method
     * @return MethodStats object containing performance data
     */
    public static MethodStats getStats(String methodName) {
        return methodStats.get(methodName);
    }
    
    /**
     * Get all performance statistics
     * 
     * @return ConcurrentHashMap containing all method statistics
     */
    public static ConcurrentHashMap<String, MethodStats> getAllStats() {
        return new ConcurrentHashMap<>(methodStats);
    }
    
    /**
     * Reset all performance statistics
     */
    public static void resetStats() {
        methodStats.clear();
        activeTimers.clear();
    }
    
    /**
     * Check if a method is performing below acceptable thresholds
     * 
     * @param methodName The name of the method
     * @param thresholdNanos Threshold in nanoseconds
     * @return true if method is performing below threshold
     */
    public static boolean isBelowThreshold(String methodName, long thresholdNanos) {
        MethodStats stats = getStats(methodName);
        if (stats == null) return false;
        
        return stats.getAverageExecutionTime() > thresholdNanos;
    }
    
    /**
     * Get memory usage statistics
     * 
     * @return MemoryInfo object containing memory statistics
     */
    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return new MemoryInfo(totalMemory, freeMemory, usedMemory, maxMemory);
    }
    
    /**
     * Inner class for storing method performance statistics
     */
    public static class MethodStats {
        private final String methodName;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        
        public MethodStats(String methodName, long executionTime) {
            this.methodName = methodName;
            recordExecution(executionTime);
        }
        
        public void recordExecution(long executionTime) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            
            // Update min execution time
            long currentMin;
            do {
                currentMin = minExecutionTime.get();
                if (executionTime >= currentMin) break;
            } while (!minExecutionTime.compareAndSet(currentMin, executionTime));
            
            // Update max execution time
            long currentMax;
            do {
                currentMax = maxExecutionTime.get();
                if (executionTime <= currentMax) break;
            } while (!maxExecutionTime.compareAndSet(currentMax, executionTime));
        }
        
        // Getters
        public String getMethodName() { return methodName; }
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
        public long getMinExecutionTime() { return minExecutionTime.get(); }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MethodStats{methodName='%s', totalExecutions=%d, avgTime=%.2fns, minTime=%dns, maxTime=%dns}",
                methodName, getTotalExecutions(), getAverageExecutionTime(), getMinExecutionTime(), getMaxExecutionTime()
            );
        }
    }
    
    /**
     * Inner class for storing memory usage statistics
     */
    public static class MemoryInfo {
        private final long totalMemory;
        private final long freeMemory;
        private final long usedMemory;
        private final long maxMemory;
        
        public MemoryInfo(long totalMemory, long freeMemory, long usedMemory, long maxMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
        }
        
        // Getters
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        
        public double getMemoryUsagePercentage() {
            return maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MemoryInfo{used=%dMB, free=%dMB, total=%dMB, max=%dMB, usage=%.1f%%}",
                usedMemory / 1024 / 1024, freeMemory / 1024 / 1024,
                totalMemory / 1024 / 1024, maxMemory / 1024 / 1024,
                getMemoryUsagePercentage()
            );
        }
    }
} 