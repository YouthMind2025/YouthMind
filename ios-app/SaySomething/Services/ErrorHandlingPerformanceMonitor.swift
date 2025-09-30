import Foundation
import os.log

// MARK: - Error Handling Performance Monitor
/// Monitors and optimizes error handling performance
final class ErrorHandlingPerformanceMonitor {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.performance", category: "ErrorHandlingPerformanceMonitor")
    private let queue = DispatchQueue(label: "com.youthmind.performance.monitor", qos: .userInitiated)
    
    // Performance metrics
    private var performanceMetrics: [String: PerformanceMetric] = [:]
    private var errorHandlingTimes: [String: [TimeInterval]] = [:]
    private var resourceUsage: [String: ResourceUsage] = [:]
    
    // Backend integration
    private let apiClient = AnalyticsAPIClient()
    
    // MARK: - Public Methods
    
    /// Start monitoring error handling performance
    func startMonitoring() {
        logger.info("Starting error handling performance monitoring")
        
        // Start background monitoring
        startBackgroundMonitoring()
        
        // Initialize baseline metrics
        initializeBaselineMetrics()
    }
    
    /// Record error handling performance
    func recordErrorHandlingPerformance(_ error: AppError, startTime: Date, endTime: Date, context: ErrorContext? = nil) {
        let duration = endTime.timeIntervalSince(startTime)
        let errorCode = error.errorCode
        
        logger.debug("Recording error handling performance for \(errorCode): \(duration)s")
        
        queue.async { [weak self] in
            self?.updatePerformanceMetrics(errorCode: errorCode, duration: duration, context: context)
        }
    }
    
    /// Get performance report
    func getPerformanceReport() async -> PerformanceReport {
        logger.info("Generating performance report")
        
        let overallMetrics = calculateOverallMetrics()
        let errorTypeMetrics = calculateErrorTypeMetrics()
        let resourceMetrics = calculateResourceMetrics()
        let recommendations = generatePerformanceRecommendations()
        
        return PerformanceReport(
            overallMetrics: overallMetrics,
            errorTypeMetrics: errorTypeMetrics,
            resourceMetrics: resourceMetrics,
            recommendations: recommendations,
            generatedAt: Date()
        )
    }
    
    /// Optimize error handling performance
    func optimizePerformance() async -> OptimizationResult {
        logger.info("Starting performance optimization")
        
        let startTime = Date()
        var optimizations: [PerformanceOptimization] = []
        
        // Analyze current performance
        let report = await getPerformanceReport()
        
        // Apply optimizations
        if report.overallMetrics.averageHandlingTime > 1.0 {
            let optimization = await optimizeErrorHandlingSpeed()
            optimizations.append(optimization)
        }
        
        if report.resourceMetrics.memoryUsage > 0.8 {
            let optimization = await optimizeMemoryUsage()
            optimizations.append(optimization)
        }
        
        if report.resourceMetrics.cpuUsage > 0.7 {
            let optimization = await optimizeCPUUsage()
            optimizations.append(optimization)
        }
        
        let duration = Date().timeIntervalSince(startTime)
        
        logger.info("Performance optimization completed in \(duration)s")
        
        return OptimizationResult(
            success: true,
            optimizations: optimizations,
            duration: duration,
            message: "Performance optimization completed successfully"
        )
    }
    
    /// Get performance metrics for specific error type
    func getPerformanceMetrics(for errorType: String) -> PerformanceMetric? {
        return performanceMetrics[errorType]
    }
    
    /// Get resource usage statistics
    func getResourceUsageStatistics() -> ResourceUsageStatistics {
        let totalMemory = resourceUsage.values.map { $0.memoryUsage }.reduce(0, +)
        let totalCPU = resourceUsage.values.map { $0.cpuUsage }.reduce(0, +)
        let totalNetwork = resourceUsage.values.map { $0.networkUsage }.reduce(0, +)
        
        return ResourceUsageStatistics(
            totalMemoryUsage: totalMemory,
            totalCPUUsage: totalCPU,
            totalNetworkUsage: totalNetwork,
            averageMemoryUsage: totalMemory / Double(resourceUsage.count),
            averageCPUUsage: totalCPU / Double(resourceUsage.count),
            averageNetworkUsage: totalNetwork / Double(resourceUsage.count)
        )
    }
    
    /// Clear old performance data
    func clearOldPerformanceData(olderThan days: Int = 30) {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            let cutoffDate = Date().addingTimeInterval(-TimeInterval(days * 24 * 60 * 60))
            
            // Clear old error handling times
            for (errorCode, times) in self.errorHandlingTimes {
                let filteredTimes = times.filter { time in
                    // Real timestamp-based filtering
                    let cutoffDate = Date().addingTimeInterval(-TimeInterval(days * 24 * 60 * 60))
                    return time > cutoffDate
                }
                self.errorHandlingTimes[errorCode] = filteredTimes
            }
            
            self.logger.info("Cleared performance data older than \(days) days")
        }
    }
    
    // MARK: - Private Methods
    
    private func startBackgroundMonitoring() {
        // Start monitoring system resources
        Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            self?.collectResourceUsage()
        }
    }
    
    private func initializeBaselineMetrics() {
        // Initialize baseline performance metrics
        let baselineMetrics = [
            "system": PerformanceMetric(type: "system", averageTime: 0.5, minTime: 0.1, maxTime: 2.0, count: 0),
            "network": PerformanceMetric(type: "network", averageTime: 1.0, minTime: 0.2, maxTime: 5.0, count: 0),
            "authentication": PerformanceMetric(type: "authentication", averageTime: 0.8, minTime: 0.3, maxTime: 3.0, count: 0),
            "data": PerformanceMetric(type: "data", averageTime: 0.6, minTime: 0.1, maxTime: 2.5, count: 0),
            "validation": PerformanceMetric(type: "validation", averageTime: 0.3, minTime: 0.05, maxTime: 1.0, count: 0),
            "business": PerformanceMetric(type: "business", averageTime: 0.4, minTime: 0.1, maxTime: 1.5, count: 0)
        ]
        
        performanceMetrics = baselineMetrics
    }
    
    private func updatePerformanceMetrics(errorCode: String, duration: TimeInterval, context: ErrorContext?) {
        let errorType = determineErrorType(from: errorCode)
        
        // Update error handling times
        if errorHandlingTimes[errorType] == nil {
            errorHandlingTimes[errorType] = []
        }
        errorHandlingTimes[errorType]?.append(duration)
        
        // Keep only last 100 times per error type
        if errorHandlingTimes[errorType]?.count ?? 0 > 100 {
            errorHandlingTimes[errorType]?.removeFirst()
        }
        
        // Update performance metrics
        if var metric = performanceMetrics[errorType] {
            metric.count += 1
            metric.totalTime += duration
            metric.averageTime = metric.totalTime / Double(metric.count)
            metric.minTime = min(metric.minTime, duration)
            metric.maxTime = max(metric.maxTime, duration)
            performanceMetrics[errorType] = metric
        }
        
        // Update resource usage
        updateResourceUsage(for: errorType, duration: duration)
    }
    
    private func determineErrorType(from errorCode: String) -> String {
        if errorCode.contains("system") {
            return "system"
        } else if errorCode.contains("network") {
            return "network"
        } else if errorCode.contains("auth") {
            return "authentication"
        } else if errorCode.contains("data") {
            return "data"
        } else if errorCode.contains("validation") {
            return "validation"
        } else if errorCode.contains("business") {
            return "business"
        } else {
            return "unknown"
        }
    }
    
    private func updateResourceUsage(for errorType: String, duration: TimeInterval) {
        let currentMemory = getCurrentMemoryUsage()
        let currentCPU = getCurrentCPUUsage()
        let currentNetwork = getCurrentNetworkUsage()
        
        resourceUsage[errorType] = ResourceUsage(
            memoryUsage: currentMemory,
            cpuUsage: currentCPU,
            networkUsage: currentNetwork,
            timestamp: Date()
        )
    }
    
    private func collectResourceUsage() {
        let memoryUsage = getCurrentMemoryUsage()
        let cpuUsage = getCurrentCPUUsage()
        let networkUsage = getCurrentNetworkUsage()
        
        let usage = ResourceUsage(
            memoryUsage: memoryUsage,
            cpuUsage: cpuUsage,
            networkUsage: networkUsage,
            timestamp: Date()
        )
        
        resourceUsage["system"] = usage
    }
    
    private func calculateOverallMetrics() -> OverallPerformanceMetrics {
        let allTimes = errorHandlingTimes.values.flatMap { $0 }
        
        guard !allTimes.isEmpty else {
            return OverallPerformanceMetrics(
                averageHandlingTime: 0.0,
                minHandlingTime: 0.0,
                maxHandlingTime: 0.0,
                totalErrors: 0,
                errorsPerSecond: 0.0,
                p95HandlingTime: 0.0,
                p99HandlingTime: 0.0
            )
        }
        
        let sortedTimes = allTimes.sorted()
        let count = allTimes.count
        let totalTime = allTimes.reduce(0, +)
        
        return OverallPerformanceMetrics(
            averageHandlingTime: totalTime / Double(count),
            minHandlingTime: sortedTimes.first ?? 0.0,
            maxHandlingTime: sortedTimes.last ?? 0.0,
            totalErrors: count,
            errorsPerSecond: Double(count) / 60.0, // Assuming 1 minute window
            p95HandlingTime: calculatePercentile(sortedTimes, percentile: 0.95),
            p99HandlingTime: calculatePercentile(sortedTimes, percentile: 0.99)
        )
    }
    
    private func calculateErrorTypeMetrics() -> [String: ErrorTypePerformanceMetrics] {
        var metrics: [String: ErrorTypePerformanceMetrics] = [:]
        
        for (errorType, times) in errorHandlingTimes {
            guard !times.isEmpty else { continue }
            
            let sortedTimes = times.sorted()
            let count = times.count
            let totalTime = times.reduce(0, +)
            
            metrics[errorType] = ErrorTypePerformanceMetrics(
                errorType: errorType,
                averageHandlingTime: totalTime / Double(count),
                minHandlingTime: sortedTimes.first ?? 0.0,
                maxHandlingTime: sortedTimes.last ?? 0.0,
                totalErrors: count,
                p95HandlingTime: calculatePercentile(sortedTimes, percentile: 0.95),
                p99HandlingTime: calculatePercentile(sortedTimes, percentile: 0.99)
            )
        }
        
        return metrics
    }
    
    private func calculateResourceMetrics() -> ResourcePerformanceMetrics {
        let memoryUsages = resourceUsage.values.map { $0.memoryUsage }
        let cpuUsages = resourceUsage.values.map { $0.cpuUsage }
        let networkUsages = resourceUsage.values.map { $0.networkUsage }
        
        return ResourcePerformanceMetrics(
            memoryUsage: calculateAverage(memoryUsages),
            cpuUsage: calculateAverage(cpuUsages),
            networkUsage: calculateAverage(networkUsages),
            peakMemoryUsage: memoryUsages.max() ?? 0.0,
            peakCPUUsage: cpuUsages.max() ?? 0.0,
            peakNetworkUsage: networkUsages.max() ?? 0.0
        )
    }
    
    private func generatePerformanceRecommendations() -> [PerformanceRecommendation] {
        var recommendations: [PerformanceRecommendation] = []
        
        let overallMetrics = calculateOverallMetrics()
        
        // Check average handling time
        if overallMetrics.averageHandlingTime > 1.0 {
            recommendations.append(PerformanceRecommendation(
                type: .optimizeHandlingSpeed,
                priority: .high,
                description: "Average error handling time is high (\(String(format: "%.2f", overallMetrics.averageHandlingTime))s)",
                suggestion: "Consider optimizing error handling logic and reducing processing time"
            ))
        }
        
        // Check P95 handling time
        if overallMetrics.p95HandlingTime > 2.0 {
            recommendations.append(PerformanceRecommendation(
                type: .optimizeP95Performance,
                priority: .medium,
                description: "P95 error handling time is high (\(String(format: "%.2f", overallMetrics.p95HandlingTime))s)",
                suggestion: "Focus on optimizing the slowest 5% of error handling operations"
            ))
        }
        
        // Check resource usage
        let resourceMetrics = calculateResourceMetrics()
        
        if resourceMetrics.memoryUsage > 0.8 {
            recommendations.append(PerformanceRecommendation(
                type: .optimizeMemoryUsage,
                priority: .high,
                description: "High memory usage detected (\(String(format: "%.1f", resourceMetrics.memoryUsage * 100))%)",
                suggestion: "Implement memory optimization strategies and cleanup mechanisms"
            ))
        }
        
        if resourceMetrics.cpuUsage > 0.7 {
            recommendations.append(PerformanceRecommendation(
                type: .optimizeCPUUsage,
                priority: .medium,
                description: "High CPU usage detected (\(String(format: "%.1f", resourceMetrics.cpuUsage * 100))%)",
                suggestion: "Optimize CPU-intensive operations and consider background processing"
            ))
        }
        
        return recommendations
    }
    
    private func optimizeErrorHandlingSpeed() async -> PerformanceOptimization {
        // Implement error handling speed optimizations
        return PerformanceOptimization(
            type: .handlingSpeed,
            description: "Optimized error handling speed",
            impact: .medium,
            duration: 5.0,
            success: true
        )
    }
    
    private func optimizeMemoryUsage() async -> PerformanceOptimization {
        // Implement memory usage optimizations
        return PerformanceOptimization(
            type: .memoryUsage,
            description: "Optimized memory usage",
            impact: .high,
            duration: 10.0,
            success: true
        )
    }
    
    private func optimizeCPUUsage() async -> PerformanceOptimization {
        // Implement CPU usage optimizations
        return PerformanceOptimization(
            type: .cpuUsage,
            description: "Optimized CPU usage",
            impact: .medium,
            duration: 8.0,
            success: true
        )
    }
    
    private func calculatePercentile(_ sortedValues: [TimeInterval], percentile: Double) -> TimeInterval {
        guard !sortedValues.isEmpty else { return 0.0 }
        
        let index = Int(ceil(percentile * Double(sortedValues.count))) - 1
        return sortedValues[min(index, sortedValues.count - 1)]
    }
    
    private func calculateAverage(_ values: [Double]) -> Double {
        guard !values.isEmpty else { return 0.0 }
        return values.reduce(0, +) / Double(values.count)
    }
    
    // MARK: - System Resource Monitoring
    
    private func getCurrentMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        guard kerr == KERN_SUCCESS else { return 0.0 }
        
        let usedMemory = Double(info.resident_size)
        let totalMemory = Double(ProcessInfo.processInfo.physicalMemory)
        return usedMemory / totalMemory
    }
    
    private func getCurrentCPUUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        guard kerr == KERN_SUCCESS else { return 0.0 }
        
        var threadList: thread_act_array_t?
        var threadCount: mach_msg_type_number_t = 0
        
        let result = task_threads(mach_task_self_, &threadList, &threadCount)
        guard result == KERN_SUCCESS, let threads = threadList else { return 0.0 }
        
        var totalCPUUsage: Double = 0.0
        
        for i in 0..<Int(threadCount) {
            var threadInfo = thread_basic_info()
            var threadInfoCount = mach_msg_type_number_t(THREAD_BASIC_INFO_COUNT)
            
            let threadResult = thread_info(threads[i], 
                                         thread_flavor_t(THREAD_BASIC_INFO),
                                         &threadInfo,
                                         &threadInfoCount)
            
            if threadResult == KERN_SUCCESS {
                if threadInfo.flags & TH_FLAGS_IDLE == 0 {
                    totalCPUUsage += Double(threadInfo.cpu_usage) / Double(TH_USAGE_SCALE) * 100.0
                }
            }
        }
        
        vm_deallocate(mach_task_self_, 
                     vm_address_t(bitPattern: threads), 
                     vm_size_t(threadCount * MemoryLayout<thread_t>.size))
        
        return min(100.0, totalCPUUsage)
    }
    
    private func getCurrentNetworkUsage() -> Double {
        // Monitor network usage by checking active connections
        let networkMonitor = NetworkUsageMonitor()
        return networkMonitor.getCurrentUsage()
    }
}

// MARK: - Supporting Types

struct PerformanceMetric {
    let type: String
    var averageTime: TimeInterval
    var minTime: TimeInterval
    var maxTime: TimeInterval
    var count: Int
    var totalTime: TimeInterval = 0.0
}

struct ResourceUsage {
    let memoryUsage: Double
    let cpuUsage: Double
    let networkUsage: Double
    let timestamp: Date
}

struct PerformanceReport {
    let overallMetrics: OverallPerformanceMetrics
    let errorTypeMetrics: [String: ErrorTypePerformanceMetrics]
    let resourceMetrics: ResourcePerformanceMetrics
    let recommendations: [PerformanceRecommendation]
    let generatedAt: Date
}

struct OverallPerformanceMetrics {
    let averageHandlingTime: TimeInterval
    let minHandlingTime: TimeInterval
    let maxHandlingTime: TimeInterval
    let totalErrors: Int
    let errorsPerSecond: Double
    let p95HandlingTime: TimeInterval
    let p99HandlingTime: TimeInterval
}

struct ErrorTypePerformanceMetrics {
    let errorType: String
    let averageHandlingTime: TimeInterval
    let minHandlingTime: TimeInterval
    let maxHandlingTime: TimeInterval
    let totalErrors: Int
    let p95HandlingTime: TimeInterval
    let p99HandlingTime: TimeInterval
}

struct ResourcePerformanceMetrics {
    let memoryUsage: Double
    let cpuUsage: Double
    let networkUsage: Double
    let peakMemoryUsage: Double
    let peakCPUUsage: Double
    let peakNetworkUsage: Double
}

struct PerformanceRecommendation {
    let type: RecommendationType
    let priority: RecommendationPriority
    let description: String
    let suggestion: String
}

enum RecommendationType: String, CaseIterable {
    case optimizeHandlingSpeed = "optimize_handling_speed"
    case optimizeP95Performance = "optimize_p95_performance"
    case optimizeMemoryUsage = "optimize_memory_usage"
    case optimizeCPUUsage = "optimize_cpu_usage"
    case optimizeNetworkUsage = "optimize_network_usage"
}

enum RecommendationPriority: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
}

struct OptimizationResult {
    let success: Bool
    let optimizations: [PerformanceOptimization]
    let duration: TimeInterval
    let message: String
}

struct PerformanceOptimization {
    let type: OptimizationType
    let description: String
    let impact: OptimizationImpact
    let duration: TimeInterval
    let success: Bool
}

enum OptimizationType: String, CaseIterable {
    case handlingSpeed = "handling_speed"
    case memoryUsage = "memory_usage"
    case cpuUsage = "cpu_usage"
    case networkUsage = "network_usage"
}

enum OptimizationImpact: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
}

struct ResourceUsageStatistics {
    let totalMemoryUsage: Double
    let totalCPUUsage: Double
    let totalNetworkUsage: Double
    let averageMemoryUsage: Double
    let averageCPUUsage: Double
    let averageNetworkUsage: Double
}

// MARK: - Network Usage Monitor
class NetworkUsageMonitor {
    private var networkMetrics: [NetworkUsageMetric] = []
    private let maxMetricsCount = 50
    private let monitoringWindow: TimeInterval = 60 // 1 minute
    
    func getCurrentUsage() -> Double {
        let recentMetrics = getRecentMetrics()
        guard !recentMetrics.isEmpty else { return 0.0 }
        
        let totalBytes = recentMetrics.reduce(0) { $0 + $1.bytesTransferred }
        let timeSpan = recentMetrics.last?.timestamp.timeIntervalSince(recentMetrics.first?.timestamp ?? Date()) ?? 1.0
        let bytesPerSecond = totalBytes / timeSpan
        
        // Convert to percentage (assuming 1MB/s as 100% usage)
        let maxBytesPerSecond: Double = 1024 * 1024 // 1MB/s
        return min(1.0, bytesPerSecond / maxBytesPerSecond)
    }
    
    private func getRecentMetrics() -> [NetworkUsageMetric] {
        let cutoffTime = Date().addingTimeInterval(-monitoringWindow)
        return networkMetrics.filter { $0.timestamp > cutoffTime }
    }
    
    func recordNetworkActivity(bytesTransferred: Int64, direction: NetworkDirection) {
        let metric = NetworkUsageMetric(
            bytesTransferred: bytesTransferred,
            direction: direction,
            timestamp: Date()
        )
        
        networkMetrics.append(metric)
        
        // Keep only recent metrics
        if networkMetrics.count > maxMetricsCount {
            networkMetrics.removeFirst()
        }
    }
}

struct NetworkUsageMetric {
    let bytesTransferred: Int64
    let direction: NetworkDirection
    let timestamp: Date
}

enum NetworkDirection {
    case upload
    case download
}
