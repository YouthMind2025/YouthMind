package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI Service Load Balancer with intelligent routing and failover mechanisms.
 * Features:
 * - Multiple load balancing algorithms (round-robin, weighted, least connections)
 * - Service discovery and registration
 * - Health-aware routing with automatic failover
 * - Sticky sessions for user-specific routing
 * - Load balancing metrics and performance tracking
 * - Dynamic service scaling and load distribution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceLoadBalancer {

    private final AIServiceConnectionHealthMonitor healthMonitor;

    // Service registry and routing
    private final Map<String, List<ServiceEndpoint>> serviceEndpoints = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AtomicLong>> serviceMetrics = new ConcurrentHashMap<>();
    private final Map<String, String> stickySessions = new ConcurrentHashMap<>();

    // Load balancing configuration
    private final Map<String, LoadBalancingConfig> serviceConfigs = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        initializeDefaultServiceEndpoints();
        initializeLoadBalancingConfigs();
        log.info("AI Service Load Balancer initialized with {} services", serviceEndpoints.size());
    }

    /**
     * Select service endpoint using configured load balancing algorithm
     */
    public ServiceEndpoint selectEndpoint(String serviceType, String userId, String operation) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints == null || endpoints.isEmpty()) {
            throw new RuntimeException("No endpoints available for service: " + serviceType);
        }

        // Filter healthy endpoints
        List<ServiceEndpoint> healthyEndpoints = filterHealthyEndpoints(endpoints);
        if (healthyEndpoints.isEmpty()) {
            log.warn("No healthy endpoints available for service: {}, using all endpoints", serviceType);
            healthyEndpoints = endpoints;
        }

        LoadBalancingConfig config = serviceConfigs.get(serviceType);
        ServiceEndpoint selectedEndpoint = selectEndpointByAlgorithm(healthyEndpoints, config, userId, operation);

        // Record selection metrics
        recordEndpointSelection(serviceType, selectedEndpoint);

        log.debug("Selected endpoint for service: {} user: {} operation: {} -> {}", 
                serviceType, userId, operation, selectedEndpoint.getEndpointId());

        return selectedEndpoint;
    }

    /**
     * Register service endpoint
     */
    public void registerEndpoint(String serviceType, ServiceEndpoint endpoint) {
        serviceEndpoints.computeIfAbsent(serviceType, k -> new ArrayList<>()).add(endpoint);
        log.info("Registered endpoint for service: {} -> {}", serviceType, endpoint.getEndpointId());
    }

    /**
     * Unregister service endpoint
     */
    public void unregisterEndpoint(String serviceType, String endpointId) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints != null) {
            endpoints.removeIf(endpoint -> endpoint.getEndpointId().equals(endpointId));
            log.info("Unregistered endpoint for service: {} -> {}", serviceType, endpointId);
        }
    }

    /**
     * Get load balancing metrics for service
     */
    public LoadBalancingMetrics getMetrics(String serviceType) {
        Map<String, AtomicLong> metrics = serviceMetrics.get(serviceType);
        if (metrics == null) {
            return LoadBalancingMetrics.builder().build();
        }

        long totalRequests = metrics.getOrDefault("totalRequests", new AtomicLong(0)).get();
        long successfulRequests = metrics.getOrDefault("successfulRequests", new AtomicLong(0)).get();
        long failedRequests = metrics.getOrDefault("failedRequests", new AtomicLong(0)).get();

        return LoadBalancingMetrics.builder()
            .serviceType(serviceType)
            .totalRequests(totalRequests)
            .successfulRequests(successfulRequests)
            .failedRequests(failedRequests)
            .successRate(totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0)
            .endpointCount(serviceEndpoints.getOrDefault(serviceType, Collections.emptyList()).size())
            .healthyEndpointCount(filterHealthyEndpoints(serviceEndpoints.getOrDefault(serviceType, Collections.emptyList())).size())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get all service endpoints
     */
    public Map<String, List<ServiceEndpoint>> getAllServiceEndpoints() {
        return new HashMap<>(serviceEndpoints);
    }

    /**
     * Auto-scale service endpoints based on load patterns
     */
    public void autoScaleService(String serviceType, int targetInstances) {
        List<ServiceEndpoint> currentEndpoints = serviceEndpoints.get(serviceType);
        if (currentEndpoints == null) {
            log.warn("Cannot auto-scale unknown service: {}", serviceType);
            return;
        }

        int currentInstances = currentEndpoints.size();
        
        if (targetInstances > currentInstances) {
            // Scale up - add new endpoints
            scaleUpService(serviceType, targetInstances - currentInstances);
        } else if (targetInstances < currentInstances) {
            // Scale down - remove least used endpoints
            scaleDownService(serviceType, currentInstances - targetInstances);
        }
        
        log.info("Auto-scaled service: {} from {} to {} instances", serviceType, currentInstances, targetInstances);
    }

    /**
     * Perform failover for failed endpoint
     */
    public ServiceEndpoint performFailover(String serviceType, String failedEndpointId) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints == null) {
            throw new RuntimeException("No endpoints available for failover: " + serviceType);
        }

        // Mark failed endpoint as unhealthy
        endpoints.stream()
            .filter(endpoint -> endpoint.getEndpointId().equals(failedEndpointId))
            .findFirst()
            .ifPresent(endpoint -> {
                endpoint.setHealthy(false);
                log.warn("Marked endpoint as unhealthy for failover: {}", failedEndpointId);
            });

        // Select new healthy endpoint
        List<ServiceEndpoint> healthyEndpoints = filterHealthyEndpoints(endpoints);
        if (healthyEndpoints.isEmpty()) {
            throw new RuntimeException("No healthy endpoints available for failover: " + serviceType);
        }

        LoadBalancingConfig config = serviceConfigs.get(serviceType);
        ServiceEndpoint newEndpoint = selectEndpointByAlgorithm(healthyEndpoints, config, "failover", "failover");
        
        log.info("Performed failover for service: {} from {} to {}", 
                serviceType, failedEndpointId, newEndpoint.getEndpointId());
        
        return newEndpoint;
    }

    /**
     * Get service discovery information
     */
    public ServiceDiscoveryInfo getServiceDiscoveryInfo(String serviceType) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints == null) {
            return ServiceDiscoveryInfo.builder()
                .serviceType(serviceType)
                .available(false)
                .endpointCount(0)
                .healthyEndpointCount(0)
                .build();
        }

        int healthyCount = filterHealthyEndpoints(endpoints).size();
        
        return ServiceDiscoveryInfo.builder()
            .serviceType(serviceType)
            .available(healthyCount > 0)
            .endpointCount(endpoints.size())
            .healthyEndpointCount(healthyCount)
            .endpoints(endpoints)
            .loadBalancingConfig(serviceConfigs.get(serviceType))
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Update load balancing configuration
     */
    public void updateLoadBalancingConfig(String serviceType, LoadBalancingConfig newConfig) {
        serviceConfigs.put(serviceType, newConfig);
        log.info("Updated load balancing configuration for service: {}", serviceType);
    }

    /**
     * Get comprehensive load balancing health status
     */
    public LoadBalancingHealthStatus getLoadBalancingHealthStatus() {
        Map<String, LoadBalancingMetrics> allMetrics = new HashMap<>();
        int totalServices = serviceEndpoints.size();
        int healthyServices = 0;
        int totalEndpoints = 0;
        int healthyEndpoints = 0;

        for (String serviceType : serviceEndpoints.keySet()) {
            LoadBalancingMetrics metrics = getMetrics(serviceType);
            allMetrics.put(serviceType, metrics);
            
            totalEndpoints += metrics.getEndpointCount();
            healthyEndpoints += metrics.getHealthyEndpointCount();
            
            if (metrics.getHealthyEndpointCount() > 0) {
                healthyServices++;
            }
        }

        double overallHealth = totalServices > 0 ? (double) healthyServices / totalServices : 0.0;
        double endpointHealth = totalEndpoints > 0 ? (double) healthyEndpoints / totalEndpoints : 0.0;

        return LoadBalancingHealthStatus.builder()
            .overallHealth(overallHealth)
            .endpointHealth(endpointHealth)
            .totalServices(totalServices)
            .healthyServices(healthyServices)
            .totalEndpoints(totalEndpoints)
            .healthyEndpoints(healthyEndpoints)
            .serviceMetrics(allMetrics)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Update endpoint health status
     */
    public void updateEndpointHealth(String serviceType, String endpointId, boolean isHealthy) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints != null) {
            endpoints.stream()
                .filter(endpoint -> endpoint.getEndpointId().equals(endpointId))
                .findFirst()
                .ifPresent(endpoint -> {
                    endpoint.setHealthy(isHealthy);
                    endpoint.setLastHealthCheck(LocalDateTime.now());
                });
        }
    }

    /**
     * Record request result for endpoint
     */
    public void recordRequestResult(String serviceType, String endpointId, boolean success, long responseTimeMs) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints != null) {
            endpoints.stream()
                .filter(endpoint -> endpoint.getEndpointId().equals(endpointId))
                .findFirst()
                .ifPresent(endpoint -> {
                    endpoint.recordRequest(success, responseTimeMs);
                });
        }

        // Update service metrics
        Map<String, AtomicLong> metrics = serviceMetrics.computeIfAbsent(serviceType, k -> new ConcurrentHashMap<>());
        metrics.computeIfAbsent("totalRequests", k -> new AtomicLong(0)).incrementAndGet();
        if (success) {
            metrics.computeIfAbsent("successfulRequests", k -> new AtomicLong(0)).incrementAndGet();
        } else {
            metrics.computeIfAbsent("failedRequests", k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    // Private helper methods

    private List<ServiceEndpoint> filterHealthyEndpoints(List<ServiceEndpoint> endpoints) {
        return endpoints.stream()
            .filter(ServiceEndpoint::isHealthy)
            .toList();
    }

    private ServiceEndpoint selectEndpointByAlgorithm(List<ServiceEndpoint> endpoints, LoadBalancingConfig config, String userId, String operation) {
        if (endpoints.isEmpty()) {
            throw new RuntimeException("No endpoints available for selection");
        }

        return switch (config.getAlgorithm()) {
            case ROUND_ROBIN -> selectRoundRobin(endpoints, config.getServiceType());
            case WEIGHTED -> selectWeighted(endpoints);
            case LEAST_CONNECTIONS -> selectLeastConnections(endpoints);
            case STICKY_SESSION -> selectStickySession(endpoints, userId);
            case HEALTH_AWARE -> selectHealthAware(endpoints);
            default -> selectRoundRobin(endpoints, config.getServiceType());
        };
    }

    private ServiceEndpoint selectRoundRobin(List<ServiceEndpoint> endpoints, String serviceType) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(serviceType, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % endpoints.size();
        return endpoints.get(index);
    }

    private ServiceEndpoint selectWeighted(List<ServiceEndpoint> endpoints) {
        int totalWeight = endpoints.stream().mapToInt(ServiceEndpoint::getWeight).sum();
        int randomWeight = new Random().nextInt(totalWeight);
        
        int currentWeight = 0;
        for (ServiceEndpoint endpoint : endpoints) {
            currentWeight += endpoint.getWeight();
            if (randomWeight < currentWeight) {
                return endpoint;
            }
        }
        
        return endpoints.get(0); // Fallback
    }

    private ServiceEndpoint selectLeastConnections(List<ServiceEndpoint> endpoints) {
        return endpoints.stream()
            .min(Comparator.comparingInt(ServiceEndpoint::getActiveConnections))
            .orElse(endpoints.get(0));
    }

    private ServiceEndpoint selectStickySession(List<ServiceEndpoint> endpoints, String userId) {
        String stickyEndpointId = stickySessions.get(userId);
        if (stickyEndpointId != null) {
            return endpoints.stream()
                .filter(endpoint -> endpoint.getEndpointId().equals(stickyEndpointId))
                .filter(ServiceEndpoint::isHealthy)
                .findFirst()
                .orElse(selectRoundRobin(endpoints, "default"));
        }
        
        // Select new endpoint and create sticky session
        ServiceEndpoint selectedEndpoint = selectRoundRobin(endpoints, "default");
        stickySessions.put(userId, selectedEndpoint.getEndpointId());
        return selectedEndpoint;
    }

    private ServiceEndpoint selectHealthAware(List<ServiceEndpoint> endpoints) {
        return endpoints.stream()
            .max(Comparator.comparingDouble(endpoint -> {
                double healthScore = healthMonitor.getServiceHealthScore(endpoint.getServiceType());
                return healthScore * endpoint.getSuccessRate();
            }))
            .orElse(endpoints.get(0));
    }

    private void recordEndpointSelection(String serviceType, ServiceEndpoint endpoint) {
        Map<String, AtomicLong> metrics = serviceMetrics.computeIfAbsent(serviceType, k -> new ConcurrentHashMap<>());
        metrics.computeIfAbsent("selections", k -> new AtomicLong(0)).incrementAndGet();
    }

    // Scaling helper methods
    private void scaleUpService(String serviceType, int instancesToAdd) {
        for (int i = 0; i < instancesToAdd; i++) {
            String endpointId = serviceType + "-scaled-" + System.currentTimeMillis() + "-" + i;
            String url = generateScaledEndpointUrl(serviceType, i);
            
            ServiceEndpoint newEndpoint = ServiceEndpoint.builder()
                .endpointId(endpointId)
                .serviceType(serviceType)
                .url(url)
                .weight(50) // Lower weight for scaled instances
                .healthy(true)
                .build();
                
            registerEndpoint(serviceType, newEndpoint);
            log.info("Scaled up service: {} with new endpoint: {}", serviceType, endpointId);
        }
    }

    private void scaleDownService(String serviceType, int instancesToRemove) {
        List<ServiceEndpoint> endpoints = serviceEndpoints.get(serviceType);
        if (endpoints == null || endpoints.size() <= instancesToRemove) {
            log.warn("Cannot scale down service: {} - insufficient endpoints", serviceType);
            return;
        }

        // Remove least used endpoints (those with lowest request count)
        endpoints.stream()
            .sorted(Comparator.comparingLong(ServiceEndpoint::getTotalRequests))
            .limit(instancesToRemove)
            .forEach(endpoint -> {
                unregisterEndpoint(serviceType, endpoint.getEndpointId());
                log.info("Scaled down service: {} by removing endpoint: {}", serviceType, endpoint.getEndpointId());
            });
    }

    private String generateScaledEndpointUrl(String serviceType, int instanceIndex) {
        // Generate URLs for scaled instances based on service type
        return switch (serviceType) {
            case "psychological_analysis" -> "http://localhost:" + (8001 + instanceIndex);
            case "state_update" -> "http://localhost:" + (8003 + instanceIndex);
            case "mood_analysis" -> "http://localhost:" + (8006 + instanceIndex);
            default -> "http://localhost:" + (9000 + instanceIndex);
        };
    }

    private void initializeDefaultServiceEndpoints() {
        // DeepSeek API endpoints
        registerEndpoint("deepseek", ServiceEndpoint.builder()
            .endpointId("deepseek-primary")
            .serviceType("deepseek")
            .url("https://api.deepseek.com/v1")
            .weight(100)
            .healthy(true)
            .build());

        // Moderation service endpoints
        registerEndpoint("moderation", ServiceEndpoint.builder()
            .endpointId("moderation-primary")
            .serviceType("moderation")
            .url("https://api.moderation.com/v1")
            .weight(100)
            .healthy(true)
            .build());

        // Vector search endpoints
        registerEndpoint("vector_search", ServiceEndpoint.builder()
            .endpointId("qdrant-primary")
            .serviceType("vector_search")
            .url("http://localhost:6333")
            .weight(100)
            .healthy(true)
            .build());

        // Psychological Analysis Service endpoints
        registerEndpoint("psychological_analysis", ServiceEndpoint.builder()
            .endpointId("psychological-analysis-primary")
            .serviceType("psychological_analysis")
            .url("http://localhost:8001")
            .weight(100)
            .healthy(true)
            .build());

        registerEndpoint("psychological_analysis", ServiceEndpoint.builder()
            .endpointId("psychological-analysis-secondary")
            .serviceType("psychological_analysis")
            .url("http://localhost:8002")
            .weight(80)
            .healthy(true)
            .build());

        // State Update Service endpoints
        registerEndpoint("state_update", ServiceEndpoint.builder()
            .endpointId("state-update-primary")
            .serviceType("state_update")
            .url("http://localhost:8003")
            .weight(100)
            .healthy(true)
            .build());

        registerEndpoint("state_update", ServiceEndpoint.builder()
            .endpointId("state-update-secondary")
            .serviceType("state_update")
            .url("http://localhost:8004")
            .weight(90)
            .healthy(true)
            .build());

        // Personality Analysis Service endpoints
        registerEndpoint("personality_analysis", ServiceEndpoint.builder()
            .endpointId("personality-analysis-primary")
            .serviceType("personality_analysis")
            .url("http://localhost:8005")
            .weight(100)
            .healthy(true)
            .build());

        // Mood Analysis Service endpoints
        registerEndpoint("mood_analysis", ServiceEndpoint.builder()
            .endpointId("mood-analysis-primary")
            .serviceType("mood_analysis")
            .url("http://localhost:8006")
            .weight(100)
            .healthy(true)
            .build());

        registerEndpoint("mood_analysis", ServiceEndpoint.builder()
            .endpointId("mood-analysis-secondary")
            .serviceType("mood_analysis")
            .url("http://localhost:8007")
            .weight(85)
            .healthy(true)
            .build());
    }

    private void initializeLoadBalancingConfigs() {
        serviceConfigs.put("deepseek", LoadBalancingConfig.builder()
            .serviceType("deepseek")
            .algorithm(LoadBalancingAlgorithm.ROUND_ROBIN)
            .enableStickySessions(true)
            .stickySessionTimeout(300000) // 5 minutes
            .healthCheckInterval(30000) // 30 seconds
            .build());

        serviceConfigs.put("moderation", LoadBalancingConfig.builder()
            .serviceType("moderation")
            .algorithm(LoadBalancingAlgorithm.LEAST_CONNECTIONS)
            .enableStickySessions(false)
            .healthCheckInterval(20000) // 20 seconds
            .build());

        serviceConfigs.put("vector_search", LoadBalancingConfig.builder()
            .serviceType("vector_search")
            .algorithm(LoadBalancingAlgorithm.HEALTH_AWARE)
            .enableStickySessions(false)
            .healthCheckInterval(15000) // 15 seconds
            .build());

        // Psychological Analysis Service configuration
        serviceConfigs.put("psychological_analysis", LoadBalancingConfig.builder()
            .serviceType("psychological_analysis")
            .algorithm(LoadBalancingAlgorithm.WEIGHTED)
            .enableStickySessions(true)
            .stickySessionTimeout(600000) // 10 minutes for user-specific analysis
            .healthCheckInterval(10000) // 10 seconds for critical service
            .build());

        // State Update Service configuration
        serviceConfigs.put("state_update", LoadBalancingConfig.builder()
            .serviceType("state_update")
            .algorithm(LoadBalancingAlgorithm.LEAST_CONNECTIONS)
            .enableStickySessions(false)
            .healthCheckInterval(15000) // 15 seconds
            .build());

        // Personality Analysis Service configuration
        serviceConfigs.put("personality_analysis", LoadBalancingConfig.builder()
            .serviceType("personality_analysis")
            .algorithm(LoadBalancingAlgorithm.HEALTH_AWARE)
            .enableStickySessions(true)
            .stickySessionTimeout(900000) // 15 minutes for personality consistency
            .healthCheckInterval(20000) // 20 seconds
            .build());

        // Mood Analysis Service configuration
        serviceConfigs.put("mood_analysis", LoadBalancingConfig.builder()
            .serviceType("mood_analysis")
            .algorithm(LoadBalancingAlgorithm.WEIGHTED)
            .enableStickySessions(false)
            .healthCheckInterval(12000) // 12 seconds
            .build());
    }

    // Inner classes for data structures

    @lombok.Data
    @lombok.Builder
    public static class ServiceEndpoint {
        private String endpointId;
        private String serviceType;
        private String url;
        private int weight;
        private boolean healthy;
        private int activeConnections;
        private long totalRequests;
        private long successfulRequests;
        private double averageResponseTime;
        private LocalDateTime lastHealthCheck;
        private LocalDateTime createdAt;

        public void recordRequest(boolean success, long responseTimeMs) {
            totalRequests++;
            if (success) {
                successfulRequests++;
            }
            
            // Update average response time
            averageResponseTime = (averageResponseTime * (totalRequests - 1) + responseTimeMs) / totalRequests;
        }

        public double getSuccessRate() {
            if (totalRequests == 0) return 1.0;
            return (double) successfulRequests / totalRequests;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class LoadBalancingConfig {
        private String serviceType;
        private LoadBalancingAlgorithm algorithm;
        private boolean enableStickySessions;
        private long stickySessionTimeout;
        private long healthCheckInterval;
    }

    @lombok.Data
    @lombok.Builder
    public static class LoadBalancingMetrics {
        private String serviceType;
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double successRate;
        private int endpointCount;
        private int healthyEndpointCount;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceDiscoveryInfo {
        private String serviceType;
        private boolean available;
        private int endpointCount;
        private int healthyEndpointCount;
        private List<ServiceEndpoint> endpoints;
        private LoadBalancingConfig loadBalancingConfig;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class LoadBalancingHealthStatus {
        private double overallHealth;
        private double endpointHealth;
        private int totalServices;
        private int healthyServices;
        private int totalEndpoints;
        private int healthyEndpoints;
        private Map<String, LoadBalancingMetrics> serviceMetrics;
        private LocalDateTime timestamp;
    }

    public enum LoadBalancingAlgorithm {
        ROUND_ROBIN, WEIGHTED, LEAST_CONNECTIONS, STICKY_SESSION, HEALTH_AWARE
    }
}
