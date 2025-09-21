package ai.youthmind.service;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics service with Micrometer integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> activeTimers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> customCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> customGauges = new ConcurrentHashMap<>();

    /**
     * Start timing an operation
     */
    public void startTimer(String operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        activeTimers.put(operation, sample);
    }

    /**
     * End timing an operation and record metrics
     */
    public void endTimer(String operation) {
        Timer.Sample sample = activeTimers.remove(operation);
        if (sample != null) {
            Timer timer = Timer.builder("operation.duration")
                .tag("operation", operation)
                .description("Duration of " + operation + " operation")
                .register(meterRegistry);
            sample.stop(timer);
        }
    }

    /**
     * Record operation duration with tags
     */
    public void recordDuration(String operation, long durationMs, String... tags) {
        Timer.Builder builder = Timer.builder("operation.duration")
            .tag("operation", operation);
        
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        
        builder.register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Increment a counter
     */
    public void incrementCounter(String counter) {
        Counter.builder("operation.counter")
            .tag("counter", counter)
            .register(meterRegistry)
            .increment();
        
        customCounters.computeIfAbsent(counter, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increment counter with tags
     */
    public void incrementCounter(String counter, String... tags) {
        Counter.Builder builder = Counter.builder("operation.counter")
            .tag("counter", counter);
        
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        
        builder.register(meterRegistry).increment();
    }

    /**
     * Set a gauge value
     */
    public void setGauge(String gauge, long value) {
        Gauge.builder("operation.gauge", () -> value)
            .tag("gauge", gauge)
            .register(meterRegistry);
        
        customGauges.put(gauge, new AtomicLong(value));
    }

    /**
     * Set gauge with tags
     */
    public void setGauge(String gauge, long value, String... tags) {
        var builder = Gauge.builder("operation.gauge", () -> (double) value)
            .tag("gauge", gauge);
        
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        
        builder.register(meterRegistry);
    }

    /**
     * Record a distribution summary
     */
    public void recordDistribution(String metricName, double value, String... tags) {
        DistributionSummary.Builder builder = DistributionSummary.builder(metricName);
        
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        
        builder.register(meterRegistry).record(value);
    }

    /**
     * Record API metrics
     */
    public void recordAPIMetrics(String endpoint, String method, int statusCode, long durationMs) {
        Timer.builder("api.request.duration")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        Counter.builder("api.request.total")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record database metrics
     */
    public void recordDatabaseMetrics(String operation, String table, long durationMs, boolean success) {
        Timer.builder("database.operation.duration")
            .tag("operation", operation)
            .tag("table", table)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        Counter.builder("database.operation.total")
            .tag("operation", operation)
            .tag("table", table)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record AI service metrics
     */
    public void recordAIMetrics(String service, String operation, long durationMs, boolean success) {
        Timer.builder("ai.service.duration")
            .tag("service", service)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        Counter.builder("ai.service.total")
            .tag("service", service)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record vector search metrics
     */
    public void recordVectorSearchMetrics(String operation, long durationMs, int resultCount) {
        Timer.builder("vector.search.duration")
            .tag("operation", operation)
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        DistributionSummary.builder("vector.search.results")
            .tag("operation", operation)
            .register(meterRegistry)
            .record(resultCount);
    }

    /**
     * Record concurrency metrics
     */
    public void recordConcurrencyMetrics(int activeConnections, int activeRequests, int queueSize) {
        setGauge("concurrency.active_connections", activeConnections);
        setGauge("concurrency.active_requests", activeRequests);
        setGauge("concurrency.queue_size", queueSize);
    }

    /**
     * Record error metrics
     */
    public void recordError(String operation, String errorType, String errorMessage) {
        Counter.builder("error.total")
            .tag("operation", operation)
            .tag("error_type", errorType)
            .register(meterRegistry)
            .increment();
        
        log.error("Error recorded: operation={}, type={}, message={}", operation, errorType, errorMessage);
    }

    /**
     * Get all metrics as a map
     */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get all meters
        List<Meter> meters = meterRegistry.getMeters();
        Map<String, Object> micrometerMetrics = new HashMap<>();
        
        for (Meter meter : meters) {
            String meterName = meter.getId().getName();
            Object meterValue = getMeterValue(meter);
            micrometerMetrics.put(meterName, meterValue);
        }
        
        metrics.put("micrometer_metrics", micrometerMetrics);
        metrics.put("custom_counters", new HashMap<>(customCounters));
        metrics.put("custom_gauges", new HashMap<>(customGauges));
        
        return metrics;
    }

    /**
     * Get system metrics
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        systemMetrics.put("jvm_memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        systemMetrics.put("jvm_memory_total_mb", runtime.totalMemory() / 1024 / 1024);
        systemMetrics.put("jvm_memory_max_mb", runtime.maxMemory() / 1024 / 1024);
        systemMetrics.put("jvm_available_processors", runtime.availableProcessors());
        
        // Thread metrics
        ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();
        systemMetrics.put("thread_count", threadBean.getThreadCount());
        systemMetrics.put("thread_daemon_count", threadBean.getDaemonThreadCount());
        systemMetrics.put("thread_peak_count", threadBean.getPeakThreadCount());
        
        // GC metrics
        List<GarbageCollectorMXBean> gcBeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcMetrics = new HashMap<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            gcMetrics.put(gcBean.getName() + "_count", gcBean.getCollectionCount());
            gcMetrics.put(gcBean.getName() + "_time_ms", gcBean.getCollectionTime());
        }
        systemMetrics.put("gc_metrics", gcMetrics);
        
        return systemMetrics;
    }

    /**
     * Get application metrics
     */
    public Map<String, Object> getApplicationMetrics() {
        Map<String, Object> appMetrics = new HashMap<>();
        
        // Get metrics from meters
        List<Meter> meters = meterRegistry.getMeters();
        
        for (Meter meter : meters) {
            String meterName = meter.getId().getName();
            if (meterName.startsWith("api.")) {
                appMetrics.put(meterName, getMeterValue(meter));
            } else if (meterName.startsWith("database.")) {
                appMetrics.put(meterName, getMeterValue(meter));
            } else if (meterName.startsWith("ai.")) {
                appMetrics.put(meterName, getMeterValue(meter));
            } else if (meterName.startsWith("vector.")) {
                appMetrics.put(meterName, getMeterValue(meter));
            }
        }
        
        return appMetrics;
    }

    /**
     * Get performance summary
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Calculate averages from meters
        List<Meter> meters = meterRegistry.getMeters();
        Map<String, List<Double>> operationTimes = new HashMap<>();
        
        for (Meter meter : meters) {
            if (meter instanceof Timer) {
                Timer timer = (Timer) meter;
                String operation = timer.getId().getTag("operation");
                if (operation != null) {
                    operationTimes.computeIfAbsent(operation, k -> new ArrayList<>())
                        .add(timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                }
            }
        }
        
        // Calculate performance metrics
        Map<String, Object> performanceMetrics = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : operationTimes.entrySet()) {
            List<Double> times = entry.getValue();
            double avgTime = times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double maxTime = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double minTime = times.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            
            performanceMetrics.put(entry.getKey(), Map.of(
                "avg_time_ms", avgTime,
                "max_time_ms", maxTime,
                "min_time_ms", minTime,
                "sample_count", times.size()
            ));
        }
        
        summary.put("performance_metrics", performanceMetrics);
        summary.put("system_metrics", getSystemMetrics());
        summary.put("timestamp", System.currentTimeMillis());
        
        return summary;
    }

    // Helper methods
    private Object getMeterValue(Meter meter) {
        if (meter instanceof Counter) {
            return ((Counter) meter).count();
        } else if (meter instanceof Timer) {
            Timer timer = (Timer) meter;
            return Map.of(
                "count", timer.count(),
                "total_time_ms", timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
                "mean_time_ms", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                "max_time_ms", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS)
            );
        } else if (meter instanceof Gauge) {
            return ((Gauge) meter).value();
        } else if (meter instanceof DistributionSummary) {
            DistributionSummary summary = (DistributionSummary) meter;
            return Map.of(
                "count", summary.count(),
                "total", summary.totalAmount(),
                "mean", summary.mean(),
                "max", summary.max()
            );
        }
        return "unknown";
    }
}
