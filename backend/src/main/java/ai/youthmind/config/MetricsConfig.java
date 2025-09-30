package ai.youthmind.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Micrometer metrics and monitoring
 */
@Configuration
public class MetricsConfig {

    /**
     * Prometheus meter registry for metrics collection
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    /**
     * Customize meter registry with common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                .commonTags("application", "youthmind-backend")
                .commonTags("environment", getEnvironment())
                .commonTags("version", getVersion());
        };
    }

    /**
     * Configure distribution statistics for timers
     */
    @Bean
    public MeterFilter configureDistributionStatistics() {
        return MeterFilter.accept();
    }

    /**
     * Configure histogram buckets for response times
     */
    @Bean
    public MeterFilter configureHistogramBuckets() {
        return MeterFilter.accept();
    }

    private String getEnvironment() {
        String env = System.getProperty("spring.profiles.active");
        return env != null ? env : "dev";
    }

    private String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version != null ? version : "1.0.0";
    }
}
