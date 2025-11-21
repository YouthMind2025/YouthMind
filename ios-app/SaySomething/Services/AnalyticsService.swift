import Foundation
import Combine
import CryptoKit
import os.log

// MARK: - Analytics Service
/// Comprehensive analytics service that integrates with backend analytics APIs
/// Provides error tracking, user behavior analysis, performance metrics, and privacy compliance
final class AnalyticsService: ObservableObject {
    
    // MARK: - Singleton
    static let shared = AnalyticsService()
    
    // MARK: - Dependencies
    private let errorTracker: ErrorAnalyticsTracker
    private let userBehaviorTracker: UserBehaviorTracker
    private let errorFrequencyAnalyzer: ErrorFrequencyAnalyzer
    private let crashReporting: CrashReportingIntegration
    private let performanceMetrics: PerformanceMetricsCollector
    private let privacyManager: AnalyticsPrivacyManager
    private let encryptionService: AnalyticsEncryptionService
    private let dashboardConnector: AnalyticsDashboardConnector
    private let configurationManager: AnalyticsConfigurationManager
    
    // MARK: - Properties
    @Published var isEnabled: Bool = false
    @Published var userConsent: AnalyticsConsent = .notDetermined
    @Published var configuration: AnalyticsConfiguration
    
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsService")
    private var cancellables = Set<AnyCancellable>()
    private let queue = DispatchQueue(label: "com.youthmind.analytics.queue", qos: .background)
    
    // MARK: - Initialization
    private init() {
        self.configurationManager = AnalyticsConfigurationManager()
        self.configuration = configurationManager.loadConfiguration()
        
        // Initialize dependencies
        self.encryptionService = AnalyticsEncryptionService()
        self.privacyManager = AnalyticsPrivacyManager(encryptionService: encryptionService)
        self.errorTracker = ErrorAnalyticsTracker(privacyManager: privacyManager)
        self.userBehaviorTracker = UserBehaviorTracker(privacyManager: privacyManager)
        self.errorFrequencyAnalyzer = ErrorFrequencyAnalyzer()
        self.crashReporting = CrashReportingIntegration(privacyManager: privacyManager)
        self.performanceMetrics = PerformanceMetricsCollector(privacyManager: privacyManager)
        self.dashboardConnector = AnalyticsDashboardConnector(encryptionService: encryptionService)
        
        setupBindings()
        initializeService()
    }
    
    // MARK: - Public Methods
    
    /// Initialize the analytics service
    func initialize() async {
        await MainActor.run {
            isEnabled = configuration.isEnabled && userConsent == .granted
        }
        
        if isEnabled {
            await errorTracker.initialize()
            await userBehaviorTracker.initialize()
            await errorFrequencyAnalyzer.initialize()
            await crashReporting.initialize()
            await performanceMetrics.initialize()
            await dashboardConnector.initialize()
            
            logger.info("Analytics service initialized successfully")
        }
    }
    
    /// Track an error with comprehensive analytics
    func trackError(_ error: AppError, context: ErrorContext? = nil) {
        guard isEnabled else { return }
        
        Task {
            await errorTracker.trackError(error, context: context)
            await userBehaviorTracker.trackErrorBehavior(error, context: context)
            await errorFrequencyAnalyzer.recordError(error)
            await performanceMetrics.recordErrorPerformance(error)
        }
    }
    
    /// Track user behavior during error scenarios
    func trackUserBehavior(_ behavior: UserBehavior, context: BehaviorContext? = nil) {
        guard isEnabled else { return }
        
        Task {
            await userBehaviorTracker.trackBehavior(behavior, context: context)
        }
    }
    
    /// Track performance metrics
    func trackPerformance(_ metric: PerformanceMetric) {
        guard isEnabled else { return }
        
        Task {
            await performanceMetrics.recordMetric(metric)
        }
    }
    
    /// Report a crash
    func reportCrash(_ crash: CrashReport) {
        guard isEnabled else { return }
        
        Task {
            await crashReporting.reportCrash(crash)
        }
    }
    
    /// Get error analytics dashboard data
    func getErrorAnalytics() async -> ErrorAnalyticsDashboard? {
        guard isEnabled else { return nil }
        
        return await dashboardConnector.getErrorAnalytics()
    }
    
    /// Get user behavior analytics
    func getUserBehaviorAnalytics() async -> UserBehaviorAnalytics? {
        guard isEnabled else { return nil }
        
        return await dashboardConnector.getUserBehaviorAnalytics()
    }
    
    /// Update user consent
    func updateUserConsent(_ consent: AnalyticsConsent) {
        userConsent = consent
        Task {
            await initialize()
        }
    }
    
    /// Update configuration
    func updateConfiguration(_ config: AnalyticsConfiguration) {
        configuration = config
        configurationManager.saveConfiguration(config)
        Task {
            await initialize()
        }
    }
    
    // MARK: - Private Methods
    
    private func setupBindings() {
        // Monitor configuration changes
        $configuration
            .sink { [weak self] config in
                self?.configurationManager.saveConfiguration(config)
            }
            .store(in: &cancellables)
    }
    
    private func initializeService() {
        Task {
            await initialize()
        }
    }
}

// MARK: - Analytics Models

/// Analytics consent levels
enum AnalyticsConsent: String, CaseIterable, Codable {
    case notDetermined = "not_determined"
    case granted = "granted"
    case denied = "denied"
    
    var displayName: String {
        switch self {
        case .notDetermined: return "Not Determined"
        case .granted: return "Granted"
        case .denied: return "Denied"
        }
    }
}

/// Analytics configuration
struct AnalyticsConfiguration: Codable {
    let isEnabled: Bool
    let dataRetentionDays: Int
    let batchSize: Int
    let flushInterval: TimeInterval
    let enableCrashReporting: Bool
    let enablePerformanceTracking: Bool
    let enableUserBehaviorTracking: Bool
    let enableErrorTracking: Bool
    let encryptionEnabled: Bool
    let anonymizeData: Bool
    
    static let `default` = AnalyticsConfiguration(
        isEnabled: true,
        dataRetentionDays: 30,
        batchSize: 50,
        flushInterval: 60.0,
        enableCrashReporting: true,
        enablePerformanceTracking: true,
        enableUserBehaviorTracking: true,
        enableErrorTracking: true,
        encryptionEnabled: true,
        anonymizeData: true
    )
}

/// Error context for analytics
struct ErrorContext: Codable {
    let userId: String?
    let sessionId: String?
    let screenName: String?
    let userAction: String?
    let timestamp: Date
    let additionalData: [String: String]
    
    init(userId: String? = nil, sessionId: String? = nil, screenName: String? = nil, userAction: String? = nil, additionalData: [String: String] = [:]) {
        self.userId = userId
        self.sessionId = sessionId
        self.screenName = screenName
        self.userAction = userAction
        self.timestamp = Date()
        self.additionalData = additionalData
    }
}

/// User behavior types
enum UserBehavior: String, CaseIterable, Codable {
    case errorEncountered = "error_encountered"
    case retryAttempted = "retry_attempted"
    case errorDismissed = "error_dismissed"
    case helpRequested = "help_requested"
    case feedbackProvided = "feedback_provided"
    case appRestarted = "app_restarted"
    case cacheCleared = "cache_cleared"
    case settingsChanged = "settings_changed"
    
    var displayName: String {
        switch self {
        case .errorEncountered: return "Error Encountered"
        case .retryAttempted: return "Retry Attempted"
        case .errorDismissed: return "Error Dismissed"
        case .helpRequested: return "Help Requested"
        case .feedbackProvided: return "Feedback Provided"
        case .appRestarted: return "App Restarted"
        case .cacheCleared: return "Cache Cleared"
        case .settingsChanged: return "Settings Changed"
        }
    }
}

/// Behavior context
struct BehaviorContext: Codable {
    let errorCode: String?
    let screenName: String?
    let timestamp: Date
    let additionalData: [String: String]
    
    init(errorCode: String? = nil, screenName: String? = nil, additionalData: [String: String] = [:]) {
        self.errorCode = errorCode
        self.screenName = screenName
        self.timestamp = Date()
        self.additionalData = additionalData
    }
}

/// Performance metric types
enum PerformanceMetricType: String, CaseIterable, Codable {
    case errorHandlingTime = "error_handling_time"
    case retryLatency = "retry_latency"
    case recoveryTime = "recovery_time"
    case memoryUsage = "memory_usage"
    case cpuUsage = "cpu_usage"
    case networkLatency = "network_latency"
    
    var displayName: String {
        switch self {
        case .errorHandlingTime: return "Error Handling Time"
        case .retryLatency: return "Retry Latency"
        case .recoveryTime: return "Recovery Time"
        case .memoryUsage: return "Memory Usage"
        case .cpuUsage: return "CPU Usage"
        case .networkLatency: return "Network Latency"
        }
    }
}

/// Performance metric
struct PerformanceMetric: Codable {
    let type: PerformanceMetricType
    let value: Double
    let unit: String
    let timestamp: Date
    let context: [String: String]
    
    init(type: PerformanceMetricType, value: Double, unit: String, context: [String: String] = [:]) {
        self.type = type
        self.value = value
        self.unit = unit
        self.timestamp = Date()
        self.context = context
    }
}

/// Crash report
struct CrashReport: Codable {
    let id: UUID
    let timestamp: Date
    let error: String
    let stackTrace: [String]
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
    let userInfo: UserInfo?
    
    init(error: String, stackTrace: [String], userInfo: UserInfo? = nil) {
        self.id = UUID()
        self.timestamp = Date()
        self.error = error
        self.stackTrace = stackTrace
        self.deviceInfo = DeviceInfo.current
        self.appInfo = AppInfo.current
        self.userInfo = userInfo
    }
}

/// Device information
struct DeviceInfo: Codable {
    let model: String
    let systemVersion: String
    let systemName: String
    let identifierForVendor: String?
    let totalMemory: UInt64
    let availableMemory: UInt64
    
    static var current: DeviceInfo {
        let processInfo = ProcessInfo.processInfo
        return DeviceInfo(
            model: UIDevice.current.model,
            systemVersion: UIDevice.current.systemVersion,
            systemName: UIDevice.current.systemName,
            identifierForVendor: UIDevice.current.identifierForVendor?.uuidString,
            totalMemory: processInfo.physicalMemory,
            availableMemory: processInfo.physicalMemory - processInfo.physicalMemory / 2 // Approximation
        )
    }
}

/// App information
struct AppInfo: Codable {
    let version: String
    let buildNumber: String
    let bundleIdentifier: String
    
    static var current: AppInfo {
        let bundle = Bundle.main
        return AppInfo(
            version: bundle.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown",
            buildNumber: bundle.infoDictionary?["CFBundleVersion"] as? String ?? "Unknown",
            bundleIdentifier: bundle.bundleIdentifier ?? "Unknown"
        )
    }
}

/// User information (anonymized)
struct UserInfo: Codable {
    let userId: String
    let sessionId: String
    let isAnonymous: Bool
}

// MARK: - Analytics Dashboard Models

/// Error analytics dashboard data
struct ErrorAnalyticsDashboard: Codable {
    let totalErrors: Int
    let errorsByType: [String: Int]
    let errorsBySeverity: [String: Int]
    let errorFrequency: [String: Int]
    let topErrorSources: [ErrorSource]
    let errorTrends: [ErrorTrend]
    let averageResolutionTime: TimeInterval
    let criticalErrorCount: Int
    let timestamp: Date
}

/// Error source information
struct ErrorSource: Codable, Identifiable {
    let id = UUID()
    let source: String
    let count: Int
    let percentage: Double
}

/// Error trend data
struct ErrorTrend: Codable, Identifiable {
    let id = UUID()
    let date: Date
    let errorCount: Int
    let severity: String
}

/// User behavior analytics
struct UserBehaviorAnalytics: Codable {
    let totalBehaviors: Int
    let behaviorsByType: [String: Int]
    let errorRecoveryRate: Double
    let averageRecoveryTime: TimeInterval
    let userSatisfactionScore: Double
    let timestamp: Date
}
