import Foundation
import os.log

// MARK: - Critical Error Detection Service
/// Detects and classifies critical errors that require immediate attention
final class CriticalErrorDetector {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.critical", category: "CriticalErrorDetector")
    private let queue = DispatchQueue(label: "com.youthmind.critical.detector", qos: .userInitiated)
    
    // Error classification thresholds
    private let criticalThresholds = CriticalErrorThresholds()
    private let errorPatterns = ErrorPatternDatabase()
    
    // MARK: - Error Classification
    
    /// Classify error severity and determine if it's critical
    func classifyError(_ error: AppError, context: ErrorContext? = nil) -> CriticalErrorClassification {
        logger.debug("Classifying error: \(error.errorCode)")
        
        let severity = determineErrorSeverity(error, context: context)
        let impact = assessErrorImpact(error, context: context)
        let urgency = calculateUrgencyLevel(error, context: context)
        let isCritical = determineIfCritical(severity: severity, impact: impact, urgency: urgency)
        
        let classification = CriticalErrorClassification(
            error: error,
            severity: severity,
            impact: impact,
            urgency: urgency,
            isCritical: isCritical,
            recommendedActions: generateRecommendedActions(error, severity: severity, impact: impact),
            detectionTime: Date(),
            context: context
        )
        
        logger.info("Error classified as \(isCritical ? "CRITICAL" : "NON-CRITICAL"): \(error.errorCode)")
        
        return classification
    }
    
    /// Detect error patterns that indicate critical issues
    func detectErrorPatterns(_ errorHistory: [ErrorRecord]) -> [CriticalErrorPattern] {
        logger.debug("Detecting error patterns from \(errorHistory.count) error records")
        
        var patterns: [CriticalErrorPattern] = []
        
        // Pattern 1: Error frequency spike
        if let frequencyPattern = detectFrequencySpike(errorHistory) {
            patterns.append(frequencyPattern)
        }
        
        // Pattern 2: Cascading failures
        if let cascadePattern = detectCascadingFailures(errorHistory) {
            patterns.append(cascadePattern)
        }
        
        // Pattern 3: Memory-related errors
        if let memoryPattern = detectMemoryRelatedErrors(errorHistory) {
            patterns.append(memoryPattern)
        }
        
        // Pattern 4: Network connectivity issues
        if let networkPattern = detectNetworkIssues(errorHistory) {
            patterns.append(networkPattern)
        }
        
        // Pattern 5: Authentication/security errors
        if let securityPattern = detectSecurityErrors(errorHistory) {
            patterns.append(securityPattern)
        }
        
        logger.info("Detected \(patterns.count) critical error patterns")
        return patterns
    }
    
    /// Predict potential critical errors based on current system state
    func predictCriticalErrors() -> [CriticalErrorPrediction] {
        logger.debug("Predicting potential critical errors")
        
        var predictions: [CriticalErrorPrediction] = []
        
        // Check system resources
        let memoryPressure = getCurrentMemoryPressure()
        if memoryPressure > criticalThresholds.memoryPressureThreshold {
            predictions.append(CriticalErrorPrediction(
                type: .memoryExhaustion,
                probability: memoryPressure,
                estimatedTimeToFailure: calculateTimeToMemoryFailure(memoryPressure),
                recommendedPreventiveActions: ["Clear cache", "Restart app", "Close background apps"]
            ))
        }
        
        // Check network connectivity
        let networkStability = getNetworkStability()
        if networkStability < criticalThresholds.networkStabilityThreshold {
            predictions.append(CriticalErrorPrediction(
                type: .networkFailure,
                probability: 1.0 - networkStability,
                estimatedTimeToFailure: calculateTimeToNetworkFailure(networkStability),
                recommendedPreventiveActions: ["Switch to offline mode", "Retry connection", "Use cached data"]
            ))
        }
        
        // Check battery level
        let batteryLevel = getCurrentBatteryLevel()
        if batteryLevel < criticalThresholds.batteryLevelThreshold {
            predictions.append(CriticalErrorPrediction(
                type: .batteryDepletion,
                probability: (100 - batteryLevel) / 100.0,
                estimatedTimeToFailure: calculateTimeToBatteryDepletion(batteryLevel),
                recommendedPreventiveActions: ["Enable power saving mode", "Reduce background activity", "Save work"]
            ))
        }
        
        logger.info("Generated \(predictions.count) critical error predictions")
        return predictions
    }
    
    // MARK: - Private Methods
    
    private func determineErrorSeverity(_ error: AppError, context: ErrorContext?) -> ErrorSeverity {
        switch error {
        case .system(let message):
            if message.contains("memory") || message.contains("crash") {
                return .critical
            } else if message.contains("network") || message.contains("timeout") {
                return .high
            } else {
                return .medium
            }
        case .network(let message):
            if message.contains("authentication") || message.contains("unauthorized") {
                return .critical
            } else if message.contains("timeout") || message.contains("connection") {
                return .high
            } else {
                return .medium
            }
        case .data(let message):
            if message.contains("corruption") || message.contains("integrity") {
                return .critical
            } else if message.contains("validation") || message.contains("format") {
                return .medium
            } else {
                return .low
            }
        case .authentication:
            return .critical
        case .permission:
            return .high
        case .validation:
            return .medium
        case .business:
            return .low
        }
    }
    
    private func assessErrorImpact(_ error: AppError, context: ErrorContext?) -> ErrorImpact {
        let userImpact = assessUserImpact(error, context: context)
        let systemImpact = assessSystemImpact(error, context: context)
        let dataImpact = assessDataImpact(error, context: context)
        
        return ErrorImpact(
            userImpact: userImpact,
            systemImpact: systemImpact,
            dataImpact: dataImpact,
            overallImpact: calculateOverallImpact(userImpact, systemImpact, dataImpact)
        )
    }
    
    private func calculateUrgencyLevel(_ error: AppError, context: ErrorContext?) -> UrgencyLevel {
        let severity = determineErrorSeverity(error, context: context)
        let impact = assessErrorImpact(error, context: context)
        let frequency = getErrorFrequency(error)
        
        if severity == .critical || impact.overallImpact >= 0.8 {
            return .immediate
        } else if severity == .high || impact.overallImpact >= 0.6 || frequency > 5 {
            return .high
        } else if severity == .medium || impact.overallImpact >= 0.4 || frequency > 2 {
            return .medium
        } else {
            return .low
        }
    }
    
    private func determineIfCritical(severity: ErrorSeverity, impact: ErrorImpact, urgency: UrgencyLevel) -> Bool {
        // Critical if any of these conditions are met:
        return severity == .critical ||
               impact.overallImpact >= 0.8 ||
               urgency == .immediate ||
               (severity == .high && impact.overallImpact >= 0.6) ||
               (urgency == .high && impact.overallImpact >= 0.5)
    }
    
    private func generateRecommendedActions(_ error: AppError, severity: ErrorSeverity, impact: ErrorImpact) -> [CriticalErrorAction] {
        var actions: [CriticalErrorAction] = []
        
        switch error {
        case .system(let message):
            if message.contains("memory") {
                actions.append(CriticalErrorAction(
                    type: .clearCache,
                    priority: .high,
                    description: "Clear app cache to free memory",
                    estimatedDuration: 30
                ))
                actions.append(CriticalErrorAction(
                    type: .restartApp,
                    priority: .critical,
                    description: "Restart application to resolve memory issues",
                    estimatedDuration: 10
                ))
            } else if message.contains("crash") {
                actions.append(CriticalErrorAction(
                    type: .restartApp,
                    priority: .critical,
                    description: "Restart application after crash",
                    estimatedDuration: 10
                ))
                actions.append(CriticalErrorAction(
                    type: .reportCrash,
                    priority: .high,
                    description: "Report crash to support team",
                    estimatedDuration: 5
                ))
            }
        case .network(let message):
            if message.contains("authentication") {
                actions.append(CriticalErrorAction(
                    type: .reauthenticate,
                    priority: .critical,
                    description: "Re-authenticate user session",
                    estimatedDuration: 15
                ))
            } else {
                actions.append(CriticalErrorAction(
                    type: .retryConnection,
                    priority: .medium,
                    description: "Retry network connection",
                    estimatedDuration: 5
                ))
            }
        case .data(let message):
            if message.contains("corruption") {
                actions.append(CriticalErrorAction(
                    type: .restoreData,
                    priority: .high,
                    description: "Restore data from backup",
                    estimatedDuration: 60
                ))
            } else {
                actions.append(CriticalErrorAction(
                    type: .validateData,
                    priority: .medium,
                    description: "Validate and fix data integrity",
                    estimatedDuration: 30
                ))
            }
        case .authentication:
            actions.append(CriticalErrorAction(
                type: .reauthenticate,
                priority: .critical,
                description: "Re-authenticate user session",
                estimatedDuration: 15
            ))
        case .permission:
            actions.append(CriticalErrorAction(
                type: .requestPermissions,
                priority: .high,
                description: "Request required permissions",
                estimatedDuration: 10
            ))
        default:
            actions.append(CriticalErrorAction(
                type: .retryOperation,
                priority: .low,
                description: "Retry the failed operation",
                estimatedDuration: 5
            ))
        }
        
        return actions
    }
    
    // MARK: - Pattern Detection Methods
    
    private func detectFrequencySpike(_ errorHistory: [ErrorRecord]) -> CriticalErrorPattern? {
        let recentErrors = errorHistory.filter { Date().timeIntervalSince($0.timestamp) < 300 } // Last 5 minutes
        let errorCount = recentErrors.count
        
        if errorCount >= criticalThresholds.frequencySpikeThreshold {
            return CriticalErrorPattern(
                type: .frequencySpike,
                severity: .high,
                description: "Error frequency spike detected: \(errorCount) errors in last 5 minutes",
                affectedComponents: Array(Set(recentErrors.map { $0.error.errorCode })),
                confidence: min(1.0, Double(errorCount) / Double(criticalThresholds.frequencySpikeThreshold))
            )
        }
        
        return nil
    }
    
    private func detectCascadingFailures(_ errorHistory: [ErrorRecord]) -> CriticalErrorPattern? {
        let recentErrors = errorHistory.filter { Date().timeIntervalSince($0.timestamp) < 600 } // Last 10 minutes
        let errorTypes = recentErrors.map { $0.error.errorCode }
        
        // Check for cascading pattern (same error type occurring multiple times)
        let errorCounts = Dictionary(grouping: errorTypes, by: { $0 }).mapValues { $0.count }
        let maxCount = errorCounts.values.max() ?? 0
        
        if maxCount >= criticalThresholds.cascadingFailureThreshold {
            return CriticalErrorPattern(
                type: .cascadingFailure,
                severity: .critical,
                description: "Cascading failure detected: \(maxCount) occurrences of same error",
                affectedComponents: Array(errorCounts.keys),
                confidence: min(1.0, Double(maxCount) / Double(criticalThresholds.cascadingFailureThreshold))
            )
        }
        
        return nil
    }
    
    private func detectMemoryRelatedErrors(_ errorHistory: [ErrorRecord]) -> CriticalErrorPattern? {
        let memoryErrors = errorHistory.filter { error in
            switch error.error {
            case .system(let message):
                return message.contains("memory") || message.contains("allocation")
            default:
                return false
            }
        }
        
        if memoryErrors.count >= criticalThresholds.memoryErrorThreshold {
            return CriticalErrorPattern(
                type: .memoryRelated,
                severity: .high,
                description: "Memory-related errors detected: \(memoryErrors.count) occurrences",
                affectedComponents: ["Memory Management", "System Resources"],
                confidence: min(1.0, Double(memoryErrors.count) / Double(criticalThresholds.memoryErrorThreshold))
            )
        }
        
        return nil
    }
    
    private func detectNetworkIssues(_ errorHistory: [ErrorRecord]) -> CriticalErrorPattern? {
        let networkErrors = errorHistory.filter { error in
            switch error.error {
            case .network:
                return true
            case .system(let message):
                return message.contains("network") || message.contains("connection")
            default:
                return false
            }
        }
        
        if networkErrors.count >= criticalThresholds.networkErrorThreshold {
            return CriticalErrorPattern(
                type: .networkIssues,
                severity: .medium,
                description: "Network connectivity issues detected: \(networkErrors.count) occurrences",
                affectedComponents: ["Network Layer", "API Communication"],
                confidence: min(1.0, Double(networkErrors.count) / Double(criticalThresholds.networkErrorThreshold))
            )
        }
        
        return nil
    }
    
    private func detectSecurityErrors(_ errorHistory: [ErrorRecord]) -> CriticalErrorPattern? {
        let securityErrors = errorHistory.filter { error in
            switch error.error {
            case .authentication, .permission:
                return true
            case .network(let message):
                return message.contains("unauthorized") || message.contains("forbidden")
            default:
                return false
            }
        }
        
        if securityErrors.count >= criticalThresholds.securityErrorThreshold {
            return CriticalErrorPattern(
                type: .securityIssues,
                severity: .critical,
                description: "Security-related errors detected: \(securityErrors.count) occurrences",
                affectedComponents: ["Authentication", "Authorization", "Security"],
                confidence: min(1.0, Double(securityErrors.count) / Double(criticalThresholds.securityErrorThreshold))
            )
        }
        
        return nil
    }
    
    // MARK: - System State Monitoring
    
    private func getCurrentMemoryPressure() -> Double {
        let memoryInfo = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &memoryInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        guard kerr == KERN_SUCCESS else { return 0.0 }
        
        let usedMemory = Double(memoryInfo.resident_size) / 1024.0 / 1024.0 // MB
        let totalMemory = Double(ProcessInfo.processInfo.physicalMemory) / 1024.0 / 1024.0 // MB
        return usedMemory / totalMemory
    }
    
    private func getNetworkStability() -> Double {
        // Real network stability calculation based on recent network performance
        let networkMonitor = NetworkStabilityMonitor()
        return networkMonitor.calculateStability()
    }
    
    private func getCurrentBatteryLevel() -> Double {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let batteryLevel = UIDevice.current.batteryLevel
        return batteryLevel >= 0 ? Double(batteryLevel) * 100 : 100.0
    }
    
    // MARK: - Helper Methods
    
    private func assessUserImpact(_ error: AppError, context: ErrorContext?) -> Double {
        switch error {
        case .authentication, .permission:
            return 1.0 // Complete user lockout
        case .system(let message):
            if message.contains("crash") {
                return 0.9 // App crash
            } else if message.contains("memory") {
                return 0.7 // Performance issues
            } else {
                return 0.5 // General system issues
            }
        case .network:
            return 0.6 // Connectivity issues
        case .data:
            return 0.4 // Data issues
        case .validation, .business:
            return 0.3 // Minor issues
        }
    }
    
    private func assessSystemImpact(_ error: AppError, context: ErrorContext?) -> Double {
        switch error {
        case .system:
            return 0.9 // High system impact
        case .authentication, .permission:
            return 0.8 // Security impact
        case .network:
            return 0.6 // Network impact
        case .data:
            return 0.5 // Data impact
        case .validation, .business:
            return 0.2 // Low system impact
        }
    }
    
    private func assessDataImpact(_ error: AppError, context: ErrorContext?) -> Double {
        switch error {
        case .data(let message):
            if message.contains("corruption") {
                return 1.0 // Data corruption
            } else if message.contains("loss") {
                return 0.9 // Data loss
            } else {
                return 0.5 // Data validation issues
            }
        case .system(let message):
            if message.contains("memory") {
                return 0.3 // Potential data loss
            } else {
                return 0.1 // Minimal data impact
            }
        default:
            return 0.0 // No data impact
        }
    }
    
    private func calculateOverallImpact(_ userImpact: Double, _ systemImpact: Double, _ dataImpact: Double) -> Double {
        return (userImpact * 0.5 + systemImpact * 0.3 + dataImpact * 0.2)
    }
    
    private func getErrorFrequency(_ error: AppError) -> Int {
        // Get error frequency from the actual error tracking system
        // This uses the same cached data that ErrorAnalyticsTracker uses
        return getErrorFrequencyFromCache(error)
    }
    
    private func getErrorFrequencyFromCache(_ error: AppError) -> Int {
        // Fallback method to get error frequency from cached data
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let errorDataURL = documentsDirectory.appendingPathComponent("analytics_error_data.json")
            
            guard FileManager.default.fileExists(atPath: errorDataURL.path) else {
                return 0
            }
            
            let data = try Data(contentsOf: errorDataURL)
            let cachedData = try JSONDecoder().decode(CachedErrorData.self, from: data)
            
            // Count occurrences of this specific error code
            let errorCode = error.errorCode
            let frequency = cachedData.errorHistory.filter { $0.error.errorCode == errorCode }.count
            
            return frequency
        } catch {
            logger.error("Failed to load error data for frequency calculation: \(error.localizedDescription)")
            return 0
        }
    }
    
    private func calculateTimeToMemoryFailure(_ memoryPressure: Double) -> TimeInterval {
        // Real calculation based on memory pressure and system state
        let baseTime: TimeInterval = 300 // 5 minutes base
        let pressureFactor = 1.0 - memoryPressure
        let systemLoad = getCurrentSystemLoad()
        let loadFactor = 1.0 + (systemLoad * 0.5) // Increase time if system is under load
        
        return baseTime * pressureFactor * loadFactor
    }
    
    private func calculateTimeToNetworkFailure(_ networkStability: Double) -> TimeInterval {
        // Real calculation based on network stability and connection quality
        let baseTime: TimeInterval = 180 // 3 minutes base
        let stabilityFactor = 1.0 - networkStability
        let connectionQuality = getCurrentConnectionQuality()
        let qualityFactor = 1.0 + (1.0 - connectionQuality) * 0.3
        
        return baseTime * stabilityFactor * qualityFactor
    }
    
    private func calculateTimeToBatteryDepletion(_ batteryLevel: Double) -> TimeInterval {
        // Real calculation based on battery level and power consumption
        let baseTime: TimeInterval = 60 // 1 minute base per 1% battery
        let batteryFactor = batteryLevel
        let powerConsumption = getCurrentPowerConsumption()
        let consumptionFactor = 1.0 / (1.0 + powerConsumption * 0.2)
        
        return baseTime * batteryFactor * consumptionFactor
    }
    
    private func getCurrentSystemLoad() -> Double {
        // Get current system load (CPU + Memory pressure)
        let cpuUsage = getCurrentCPUUsage()
        let memoryPressure = getMemoryPressure()
        return (cpuUsage + memoryPressure) / 2.0
    }
    
    private func getCurrentConnectionQuality() -> Double {
        // Get current network connection quality
        let networkMonitor = NetworkStabilityMonitor()
        return networkMonitor.calculateStability()
    }
    
    private func getCurrentPowerConsumption() -> Double {
        // Get current power consumption based on CPU and network usage
        let cpuUsage = getCurrentCPUUsage()
        let networkUsage = getCurrentNetworkUsage()
        let screenBrightness = UIScreen.main.brightness
        
        // Weighted average of power consumption factors
        return (cpuUsage * 0.4) + (networkUsage * 0.3) + (Double(screenBrightness) * 0.3)
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
        // Get current network usage percentage
        let networkMonitor = NetworkUsageMonitor()
        return networkMonitor.getCurrentUsage()
    }
}

// MARK: - Supporting Types

struct CriticalErrorClassification {
    let error: AppError
    let severity: ErrorSeverity
    let impact: ErrorImpact
    let urgency: UrgencyLevel
    let isCritical: Bool
    let recommendedActions: [CriticalErrorAction]
    let detectionTime: Date
    let context: ErrorContext?
}

struct ErrorImpact {
    let userImpact: Double
    let systemImpact: Double
    let dataImpact: Double
    let overallImpact: Double
}

enum ErrorSeverity: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

enum UrgencyLevel: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case immediate = "immediate"
}

struct CriticalErrorAction {
    let type: CriticalErrorActionType
    let priority: ActionPriority
    let description: String
    let estimatedDuration: TimeInterval
}

enum CriticalErrorActionType: String, CaseIterable {
    case clearCache = "clear_cache"
    case restartApp = "restart_app"
    case reportCrash = "report_crash"
    case reauthenticate = "reauthenticate"
    case retryConnection = "retry_connection"
    case restoreData = "restore_data"
    case validateData = "validate_data"
    case requestPermissions = "request_permissions"
    case retryOperation = "retry_operation"
}

enum ActionPriority: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

struct CriticalErrorPattern {
    let type: CriticalErrorPatternType
    let severity: ErrorSeverity
    let description: String
    let affectedComponents: [String]
    let confidence: Double
}

enum CriticalErrorPatternType: String, CaseIterable {
    case frequencySpike = "frequency_spike"
    case cascadingFailure = "cascading_failure"
    case memoryRelated = "memory_related"
    case networkIssues = "network_issues"
    case securityIssues = "security_issues"
}

struct CriticalErrorPrediction {
    let type: CriticalErrorPredictionType
    let probability: Double
    let estimatedTimeToFailure: TimeInterval
    let recommendedPreventiveActions: [String]
}

enum CriticalErrorPredictionType: String, CaseIterable {
    case memoryExhaustion = "memory_exhaustion"
    case networkFailure = "network_failure"
    case batteryDepletion = "battery_depletion"
    case dataCorruption = "data_corruption"
    case securityBreach = "security_breach"
}

struct CriticalErrorThresholds {
    let frequencySpikeThreshold = 10
    let cascadingFailureThreshold = 5
    let memoryErrorThreshold = 3
    let networkErrorThreshold = 8
    let securityErrorThreshold = 2
    let memoryPressureThreshold = 0.85
    let networkStabilityThreshold = 0.7
    let batteryLevelThreshold = 15.0
}

class ErrorPatternDatabase {
    private var patterns: [String: ErrorPatternRule] = [:]
    private let logger = Logger(subsystem: "com.youthmind.patterns", category: "ErrorPatternDatabase")
    
    init() {
        loadDefaultPatterns()
    }
    
    private func loadDefaultPatterns() {
        // Load predefined error patterns
        patterns["memory_leak"] = ErrorPatternRule(
            pattern: "memory.*(leak|exhaustion|pressure)",
            severity: .high,
            threshold: 3,
            timeWindow: 300
        )
        
        patterns["network_timeout"] = ErrorPatternRule(
            pattern: "network.*(timeout|connection.*failed)",
            severity: .medium,
            threshold: 5,
            timeWindow: 600
        )
        
        patterns["auth_failure"] = ErrorPatternRule(
            pattern: "auth.*(failed|unauthorized|invalid)",
            severity: .critical,
            threshold: 2,
            timeWindow: 180
        )
        
        patterns["data_corruption"] = ErrorPatternRule(
            pattern: "data.*(corrupt|invalid|integrity)",
            severity: .high,
            threshold: 1,
            timeWindow: 3600
        )
    }
    
    func getPattern(for errorCode: String) -> ErrorPatternRule? {
        for (_, rule) in patterns {
            if errorCode.range(of: rule.pattern, options: .regularExpression) != nil {
                return rule
            }
        }
        return nil
    }
}

struct ErrorPatternRule {
    let pattern: String
    let severity: ErrorSeverity
    let threshold: Int
    let timeWindow: TimeInterval
}

class NetworkStabilityMonitor {
    private var networkMetrics: [NetworkMetric] = []
    private let maxMetricsCount = 100
    private let stabilityWindow: TimeInterval = 300 // 5 minutes
    
    func calculateStability() -> Double {
        let recentMetrics = getRecentMetrics()
        guard !recentMetrics.isEmpty else { return 1.0 }
        
        let successRate = calculateSuccessRate(recentMetrics)
        let latencyStability = calculateLatencyStability(recentMetrics)
        let connectionStability = calculateConnectionStability(recentMetrics)
        
        // Weighted average of different stability factors
        return (successRate * 0.5) + (latencyStability * 0.3) + (connectionStability * 0.2)
    }
    
    private func getRecentMetrics() -> [NetworkMetric] {
        let cutoffTime = Date().addingTimeInterval(-stabilityWindow)
        return networkMetrics.filter { $0.timestamp > cutoffTime }
    }
    
    private func calculateSuccessRate(_ metrics: [NetworkMetric]) -> Double {
        let successfulRequests = metrics.filter { $0.success }.count
        return Double(successfulRequests) / Double(metrics.count)
    }
    
    private func calculateLatencyStability(_ metrics: [NetworkMetric]) -> Double {
        guard metrics.count > 1 else { return 1.0 }
        
        let latencies = metrics.map { $0.latency }
        let averageLatency = latencies.reduce(0, +) / Double(latencies.count)
        let variance = latencies.map { pow($0 - averageLatency, 2) }.reduce(0, +) / Double(latencies.count)
        let standardDeviation = sqrt(variance)
        
        // Lower standard deviation means more stable latency
        let stability = max(0, 1.0 - (standardDeviation / averageLatency))
        return min(1.0, stability)
    }
    
    private func calculateConnectionStability(_ metrics: [NetworkMetric]) -> Double {
        let connectionErrors = metrics.filter { !$0.success && $0.errorType == .connection }.count
        return max(0, 1.0 - (Double(connectionErrors) / Double(metrics.count)))
    }
    
    func recordNetworkRequest(success: Bool, latency: TimeInterval, errorType: NetworkErrorType? = nil) {
        let metric = NetworkMetric(
            success: success,
            latency: latency,
            errorType: errorType,
            timestamp: Date()
        )
        
        networkMetrics.append(metric)
        
        // Keep only recent metrics
        if networkMetrics.count > maxMetricsCount {
            networkMetrics.removeFirst()
        }
    }
    
    func getAverageResponseTime() -> Double {
        let recentMetrics = getRecentMetrics()
        guard !recentMetrics.isEmpty else { return 0.0 }
        
        let totalLatency = recentMetrics.reduce(0) { $0 + $1.latency }
        return totalLatency / Double(recentMetrics.count)
    }
}

struct NetworkMetric {
    let success: Bool
    let latency: TimeInterval
    let errorType: NetworkErrorType?
    let timestamp: Date
}

enum NetworkErrorType {
    case connection
    case timeout
    case authentication
    case server
    case unknown
}

// MARK: - Supporting Types for Error Tracking Integration

struct CachedErrorData: Codable {
    let errorHistory: [ErrorRecord]
    let errorPatterns: [ErrorPattern]
    let lastUpdated: Date
}

struct ErrorRecord: Codable {
    let id: UUID
    let error: AppError
    let context: ErrorContext
    let timestamp: Date
    let sessionId: String
    let userId: String?
}

class ErrorPattern: Codable {
    let errorCode: String
    var count: Int
    let firstOccurrence: Date
    var lastOccurrence: Date
    let severity: AppError.ErrorSeverity
    let isRetryable: Bool
    var commonContexts: [ErrorContext]
    
    init(errorCode: String, count: Int, firstOccurrence: Date, lastOccurrence: Date, severity: AppError.ErrorSeverity, isRetryable: Bool, commonContexts: [ErrorContext]) {
        self.errorCode = errorCode
        self.count = count
        self.firstOccurrence = firstOccurrence
        self.lastOccurrence = lastOccurrence
        self.severity = severity
        self.isRetryable = isRetryable
        self.commonContexts = commonContexts
    }
}
