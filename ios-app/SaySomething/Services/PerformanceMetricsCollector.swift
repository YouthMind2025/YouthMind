import Foundation
import os.log
import UIKit

// MARK: - Performance Metrics Collector
/// Collects and analyzes performance metrics during error handling and recovery
final class PerformanceMetricsCollector {
    
    // MARK: - Dependencies
    private let privacyManager: AnalyticsPrivacyManager
    private let apiClient: AnalyticsAPIClient
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "PerformanceMetricsCollector")
    private let queue = DispatchQueue(label: "com.youthmind.performance.metrics", qos: .background)
    
    // Performance tracking
    private var performanceMetrics: [PerformanceMetric] = []
    private var performanceSessions: [String: PerformanceSession] = [:]
    private let maxMetrics = 5000
    
    // System monitoring
    private var systemMonitor: SystemMonitor?
    private var performanceTimer: Timer?
    
    // MARK: - Initialization
    init(privacyManager: AnalyticsPrivacyManager) {
        self.privacyManager = privacyManager
        self.apiClient = AnalyticsAPIClient()
    }
    
    // MARK: - Public Methods
    
    /// Initialize performance metrics collection
    func initialize() async {
        logger.info("Initializing Performance Metrics Collector")
        
        // Setup system monitoring
        setupSystemMonitoring()
        
        // Start performance tracking
        startPerformanceTracking()
        
        // Load cached metrics
        await loadCachedMetrics()
    }
    
    /// Record a performance metric
    func recordMetric(_ metric: PerformanceMetric) async {
        guard privacyManager.canTrackPerformance() else { return }
        
        await queue.async { [weak self] in
            self?.processPerformanceMetric(metric)
        }
    }
    
    /// Record error performance specifically
    func recordErrorPerformance(_ error: AppError) async {
        let startTime = Date()
        
        // Simulate error handling time measurement
        let errorHandlingTime = Date().timeIntervalSince(startTime)
        
        let metric = PerformanceMetric(
            type: .errorHandlingTime,
            value: errorHandlingTime,
            unit: "seconds",
            context: [
                "errorCode": error.errorCode,
                "severity": String(describing: error.severity),
                "retryable": String(error.isRetryable)
            ]
        )
        
        await recordMetric(metric)
    }
    
    /// Get performance analytics
    func getPerformanceAnalytics() async -> PerformanceAnalytics {
        return await queue.sync {
            let now = Date()
            let last24Hours = performanceMetrics.filter { $0.timestamp > now.addingTimeInterval(-24 * 3600) }
            let last7Days = performanceMetrics.filter { $0.timestamp > now.addingTimeInterval(-7 * 24 * 3600) }
            
            let metricsByType = Dictionary(grouping: performanceMetrics, by: { $0.type.rawValue })
                .mapValues { metrics in
                    PerformanceTypeAnalytics(
                        type: metrics.first?.type ?? .errorHandlingTime,
                        count: metrics.count,
                        averageValue: metrics.map { $0.value }.reduce(0, +) / Double(metrics.count),
                        minValue: metrics.map { $0.value }.min() ?? 0.0,
                        maxValue: metrics.map { $0.value }.max() ?? 0.0,
                        medianValue: calculateMedian(metrics.map { $0.value })
                    )
                }
            
            return PerformanceAnalytics(
                totalMetrics: performanceMetrics.count,
                last24Hours: last24Hours.count,
                last7Days: last7Days.count,
                metricsByType: metricsByType,
                averageErrorHandlingTime: calculateAverageErrorHandlingTime(),
                averageRetryLatency: calculateAverageRetryLatency(),
                averageRecoveryTime: calculateAverageRecoveryTime(),
                systemPerformance: getSystemPerformance(),
                timestamp: now
            )
        }
    }
    
    /// Get performance trends
    func getPerformanceTrends() async -> [PerformanceTrend] {
        return await queue.sync {
            let calendar = Calendar.current
            let now = Date()
            let last30Days = performanceMetrics.filter { $0.timestamp > now.addingTimeInterval(-30 * 24 * 3600) }
            
            let groupedByDay = Dictionary(grouping: last30Days) { metric in
                calendar.startOfDay(for: metric.timestamp)
            }
            
            return groupedByDay.map { date, metrics in
                let errorHandlingMetrics = metrics.filter { $0.type == .errorHandlingTime }
                let averageErrorHandlingTime = errorHandlingMetrics.isEmpty ? 0.0 : 
                    errorHandlingMetrics.map { $0.value }.reduce(0, +) / Double(errorHandlingMetrics.count)
                
                return PerformanceTrend(
                    date: date,
                    averageErrorHandlingTime: averageErrorHandlingTime,
                    totalMetrics: metrics.count,
                    performanceScore: calculatePerformanceScore(metrics)
                )
            }.sorted { $0.date < $1.date }
        }
    }
    
    /// Get system performance metrics
    func getSystemPerformance() -> SystemPerformance {
        return SystemPerformance(
            memoryUsage: getCurrentMemoryUsage(),
            cpuUsage: getCurrentCPUUsage(),
            diskUsage: getCurrentDiskUsage(),
            networkLatency: getCurrentNetworkLatency(),
            batteryLevel: getCurrentBatteryLevel(),
            timestamp: Date()
        )
    }
    
    // MARK: - Private Methods
    
    private func processPerformanceMetric(_ metric: PerformanceMetric) {
        // Add to metrics
        performanceMetrics.append(metric)
        
        // Maintain max metrics
        if performanceMetrics.count > maxMetrics {
            performanceMetrics.removeFirst(performanceMetrics.count - maxMetrics)
        }
        
        // Update performance session
        updatePerformanceSession(metric)
        
        // Send to backend
        Task {
            await sendMetricToBackend(metric)
        }
        
        // Cache locally
        cacheMetric(metric)
        
        logger.debug("Recorded performance metric: \(metric.type.rawValue) = \(metric.value) \(metric.unit)")
    }
    
    private func updatePerformanceSession(_ metric: PerformanceMetric) {
        let sessionId = metric.context["sessionId"] ?? generateSessionId()
        
        var session = performanceSessions[sessionId] ?? PerformanceSession(
            id: sessionId,
            startTime: metric.timestamp,
            endTime: metric.timestamp,
            metrics: []
        )
        
        session.metrics.append(metric)
        session.endTime = metric.timestamp
        performanceSessions[sessionId] = session
    }
    
    private func calculateAverageErrorHandlingTime() -> TimeInterval {
        let errorHandlingMetrics = performanceMetrics.filter { $0.type == .errorHandlingTime }
        guard !errorHandlingMetrics.isEmpty else { return 0.0 }
        
        let totalTime = errorHandlingMetrics.map { $0.value }.reduce(0, +)
        return totalTime / Double(errorHandlingMetrics.count)
    }
    
    private func calculateAverageRetryLatency() -> TimeInterval {
        let retryMetrics = performanceMetrics.filter { $0.type == .retryLatency }
        guard !retryMetrics.isEmpty else { return 0.0 }
        
        let totalLatency = retryMetrics.map { $0.value }.reduce(0, +)
        return totalLatency / Double(retryMetrics.count)
    }
    
    private func calculateAverageRecoveryTime() -> TimeInterval {
        let recoveryMetrics = performanceMetrics.filter { $0.type == .recoveryTime }
        guard !recoveryMetrics.isEmpty else { return 0.0 }
        
        let totalTime = recoveryMetrics.map { $0.value }.reduce(0, +)
        return totalTime / Double(recoveryMetrics.count)
    }
    
    private func calculateMedian(_ values: [Double]) -> Double {
        let sortedValues = values.sorted()
        let count = sortedValues.count
        
        if count % 2 == 0 {
            return (sortedValues[count / 2 - 1] + sortedValues[count / 2]) / 2.0
        } else {
            return sortedValues[count / 2]
        }
    }
    
    private func calculatePerformanceScore(_ metrics: [PerformanceMetric]) -> Double {
        // Calculate performance score based on various metrics
        var score = 1.0
        
        // Error handling time score (lower is better)
        let errorHandlingMetrics = metrics.filter { $0.type == .errorHandlingTime }
        if !errorHandlingMetrics.isEmpty {
            let averageTime = errorHandlingMetrics.map { $0.value }.reduce(0, +) / Double(errorHandlingMetrics.count)
            if averageTime > 1.0 { // More than 1 second is poor
                score -= 0.2
            }
        }
        
        // Memory usage score
        let memoryMetrics = metrics.filter { $0.type == .memoryUsage }
        if !memoryMetrics.isEmpty {
            let averageMemory = memoryMetrics.map { $0.value }.reduce(0, +) / Double(memoryMetrics.count)
            if averageMemory > 100.0 { // More than 100MB is poor
                score -= 0.1
            }
        }
        
        // CPU usage score
        let cpuMetrics = metrics.filter { $0.type == .cpuUsage }
        if !cpuMetrics.isEmpty {
            let averageCPU = cpuMetrics.map { $0.value }.reduce(0, +) / Double(cpuMetrics.count)
            if averageCPU > 80.0 { // More than 80% is poor
                score -= 0.1
            }
        }
        
        return max(0.0, min(1.0, score))
    }
    
    private func setupSystemMonitoring() {
        systemMonitor = SystemMonitor()
        systemMonitor?.delegate = self
    }
    
    private func startPerformanceTracking() {
        // Track system performance every 30 seconds
        performanceTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            Task {
                await self?.recordSystemMetrics()
            }
        }
    }
    
    private func recordSystemMetrics() async {
        let memoryUsage = getCurrentMemoryUsage()
        let cpuUsage = getCurrentCPUUsage()
        let diskUsage = getCurrentDiskUsage()
        let networkLatency = getCurrentNetworkLatency()
        let batteryLevel = getCurrentBatteryLevel()
        
        // Record memory usage
        if memoryUsage > 0 {
            let memoryMetric = PerformanceMetric(
                type: .memoryUsage,
                value: memoryUsage,
                unit: "MB",
                context: ["source": "system_monitoring"]
            )
            await recordMetric(memoryMetric)
        }
        
        // Record CPU usage
        if cpuUsage > 0 {
            let cpuMetric = PerformanceMetric(
                type: .cpuUsage,
                value: cpuUsage,
                unit: "percent",
                context: ["source": "system_monitoring"]
            )
            await recordMetric(cpuMetric)
        }
    }
    
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
        
        if kerr == KERN_SUCCESS {
            return Double(info.resident_size) / 1024.0 / 1024.0 // Convert to MB
        }
        return 0.0
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
        
        // Get CPU usage from thread info
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
        
        // Clean up thread list
        vm_deallocate(mach_task_self_, 
                     vm_address_t(bitPattern: threads), 
                     vm_size_t(threadCount * MemoryLayout<thread_t>.size))
        
        return min(100.0, totalCPUUsage)
    }
    
    private func getCurrentDiskUsage() -> Double {
        let fileManager = FileManager.default
        do {
            let attributes = try fileManager.attributesOfFileSystem(forPath: NSHomeDirectory())
            if let totalSize = attributes[.systemSize] as? NSNumber,
               let freeSize = attributes[.systemFreeSize] as? NSNumber {
                let usedSize = totalSize.doubleValue - freeSize.doubleValue
                return usedSize / 1024.0 / 1024.0 / 1024.0 // Convert to GB
            }
        } catch {
            logger.error("Failed to get disk usage: \(error.localizedDescription)")
        }
        return 0.0
    }
    
    private func getCurrentNetworkLatency() -> Double {
        // Ping a reliable server to measure network latency
        let host = "8.8.8.8" // Google DNS
        let port: UInt16 = 53 // DNS port
        
        let startTime = CFAbsoluteTimeGetCurrent()
        
        // Create a socket to test connectivity
        let socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        guard socket != -1 else { return -1.0 }
        
        defer { close(socket) }
        
        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = port.bigEndian
        addr.sin_addr.s_addr = inet_addr(host)
        
        let result = withUnsafePointer(to: &addr) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                connect(socket, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }
        
        let endTime = CFAbsoluteTimeGetCurrent()
        let latency = (endTime - startTime) * 1000.0 // Convert to milliseconds
        
        if result == 0 {
            return latency
        } else {
            return -1.0 // Connection failed
        }
    }
    
    private func getCurrentBatteryLevel() -> Double {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let batteryLevel = UIDevice.current.batteryLevel
        return batteryLevel >= 0 ? Double(batteryLevel) * 100 : -1.0
    }
    
    private func sendMetricToBackend(_ metric: PerformanceMetric) async {
        do {
            let request = PerformanceMetricRequest(
                type: metric.type.rawValue,
                value: metric.value,
                unit: metric.unit,
                context: metric.context,
                timestamp: metric.timestamp
            )
            
            try await apiClient.recordPerformanceMetric(request)
            logger.debug("Successfully sent performance metric to backend: \(metric.type.rawValue)")
            
        } catch {
            logger.error("Failed to send performance metric to backend: \(error.localizedDescription)")
        }
    }
    
    private func loadCachedMetrics() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let metricsDataURL = documentsDirectory.appendingPathComponent("analytics_performance_data.json")
            
            guard FileManager.default.fileExists(atPath: metricsDataURL.path) else {
                logger.info("No cached performance metrics found")
                return
            }
            
            let data = try Data(contentsOf: metricsDataURL)
            let cachedData = try JSONDecoder().decode(CachedPerformanceData.self, from: data)
            
            await queue.async { [weak self] in
                self?.performanceMetrics = cachedData.performanceMetrics
                self?.performanceSessions = Dictionary(uniqueKeysWithValues: cachedData.performanceSessions.map { ($0.id, $0) })
            }
            
            logger.info("Loaded \(cachedData.performanceMetrics.count) cached performance metrics")
        } catch {
            logger.error("Failed to load cached performance metrics: \(error.localizedDescription)")
        }
    }
    
    private func cacheMetric(_ metric: PerformanceMetric) {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let metricsDataURL = documentsDirectory.appendingPathComponent("analytics_performance_data.json")
                
                let cachedData = CachedPerformanceData(
                    performanceMetrics: self.performanceMetrics,
                    performanceSessions: Array(self.performanceSessions.values),
                    lastUpdated: Date()
                )
                
                let data = try JSONEncoder().encode(cachedData)
                try data.write(to: metricsDataURL, options: [.atomic])
                
                self.logger.debug("Cached \(self.performanceMetrics.count) performance metrics")
            } catch {
                self.logger.error("Failed to cache performance metric: \(error.localizedDescription)")
            }
        }
    }
    
    private func generateSessionId() -> String {
        return UUID().uuidString
    }
}

// MARK: - System Monitor Delegate
extension PerformanceMetricsCollector: SystemMonitorDelegate {
    func systemMonitor(_ monitor: SystemMonitor, didUpdateMemoryUsage memoryUsage: Double) {
        Task {
            let metric = PerformanceMetric(
                type: .memoryUsage,
                value: memoryUsage,
                unit: "MB",
                context: ["source": "system_monitor"]
            )
            await recordMetric(metric)
        }
    }
    
    func systemMonitor(_ monitor: SystemMonitor, didUpdateCPUUsage cpuUsage: Double) {
        Task {
            let metric = PerformanceMetric(
                type: .cpuUsage,
                value: cpuUsage,
                unit: "percent",
                context: ["source": "system_monitor"]
            )
            await recordMetric(metric)
        }
    }
}

// MARK: - Performance Models

/// Performance session tracking
struct PerformanceSession: Codable {
    let id: String
    let startTime: Date
    var endTime: Date
    var metrics: [PerformanceMetric]
}

/// Performance analytics
struct PerformanceAnalytics: Codable {
    let totalMetrics: Int
    let last24Hours: Int
    let last7Days: Int
    let metricsByType: [String: PerformanceTypeAnalytics]
    let averageErrorHandlingTime: TimeInterval
    let averageRetryLatency: TimeInterval
    let averageRecoveryTime: TimeInterval
    let systemPerformance: SystemPerformance
    let timestamp: Date
}

/// Performance type analytics
struct PerformanceTypeAnalytics: Codable {
    let type: PerformanceMetricType
    let count: Int
    let averageValue: Double
    let minValue: Double
    let maxValue: Double
    let medianValue: Double
}

/// Performance trend
struct PerformanceTrend: Codable {
    let date: Date
    let averageErrorHandlingTime: TimeInterval
    let totalMetrics: Int
    let performanceScore: Double
}

/// System performance metrics
struct SystemPerformance: Codable {
    let memoryUsage: Double
    let cpuUsage: Double
    let diskUsage: Double
    let networkLatency: Double
    let batteryLevel: Double
    let timestamp: Date
}

/// Performance metric request for backend
struct PerformanceMetricRequest: Codable {
    let type: String
    let value: Double
    let unit: String
    let context: [String: String]
    let timestamp: Date
}

/// Cached performance data for persistence
struct CachedPerformanceData: Codable {
    let performanceMetrics: [PerformanceMetric]
    let performanceSessions: [PerformanceSession]
    let lastUpdated: Date
}

// MARK: - System Monitor
protocol SystemMonitorDelegate: AnyObject {
    func systemMonitor(_ monitor: SystemMonitor, didUpdateMemoryUsage memoryUsage: Double)
    func systemMonitor(_ monitor: SystemMonitor, didUpdateCPUUsage cpuUsage: Double)
}

class SystemMonitor {
    weak var delegate: SystemMonitorDelegate?
    private var monitoringTimer: Timer?
    
    func startMonitoring() {
        monitoringTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.updateSystemMetrics()
        }
    }
    
    func stopMonitoring() {
        monitoringTimer?.invalidate()
        monitoringTimer = nil
    }
    
    private func updateSystemMetrics() {
        // Update memory usage
        let memoryUsage = getCurrentMemoryUsage()
        delegate?.systemMonitor(self, didUpdateMemoryUsage: memoryUsage)
        
        // Update CPU usage
        let cpuUsage = getCurrentCPUUsage()
        delegate?.systemMonitor(self, didUpdateCPUUsage: cpuUsage)
    }
    
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
        
        if kerr == KERN_SUCCESS {
            return Double(info.resident_size) / 1024.0 / 1024.0 // Convert to MB
        }
        return 0.0
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
        
        // Get CPU usage from thread info
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
        
        // Clean up thread list
        vm_deallocate(mach_task_self_, 
                     vm_address_t(bitPattern: threads), 
                     vm_size_t(threadCount * MemoryLayout<thread_t>.size))
        
        return min(100.0, totalCPUUsage)
    }
}
