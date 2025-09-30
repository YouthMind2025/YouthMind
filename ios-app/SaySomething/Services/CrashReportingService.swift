import Foundation
import UIKit
import os.log

// MARK: - Crash Reporting Service
/// Comprehensive crash reporting service for data collection and backend integration
final class CrashReportingService {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.crash", category: "CrashReportingService")
    private let queue = DispatchQueue(label: "com.youthmind.crash.reporting", qos: .userInitiated)
    
    // Backend integration
    private let apiClient = AnalyticsAPIClient()
    
    // Crash data storage
    private var crashReports: [CrashReport] = []
    private var crashSessions: [String: CrashSession] = [:]
    
    // Configuration
    private let configuration = CrashReportingConfiguration()
    
    // MARK: - Initialization
    
    init() {
        setupCrashHandlers()
        loadPendingCrashReports()
    }
    
    // MARK: - Public Methods
    
    /// Report a crash with comprehensive data collection
    func reportCrash(_ error: AppError, context: ErrorContext? = nil, additionalData: [String: Any] = [:]) {
        logger.info("Reporting crash for error: \(error.errorCode)")
        
        let crashReport = createCrashReport(error, context: context, additionalData: additionalData)
        
        queue.async { [weak self] in
            self?.processCrashReport(crashReport)
        }
    }
    
    /// Report an uncaught exception
    func reportUncaughtException(_ exception: NSException, stackTrace: [String]) {
        logger.error("Reporting uncaught exception: \(exception.name.rawValue)")
        
        let crashReport = createExceptionCrashReport(exception, stackTrace: stackTrace)
        
        queue.async { [weak self] in
            self?.processCrashReport(crashReport)
        }
    }
    
    /// Report a memory warning crash
    func reportMemoryWarningCrash() {
        logger.warning("Reporting memory warning crash")
        
        let crashReport = createMemoryWarningCrashReport()
        
        queue.async { [weak self] in
            self?.processCrashReport(crashReport)
        }
    }
    
    /// Report a low battery crash
    func reportLowBatteryCrash() {
        logger.warning("Reporting low battery crash")
        
        let crashReport = createLowBatteryCrashReport()
        
        queue.async { [weak self] in
            self?.processCrashReport(crashReport)
        }
    }
    
    /// Get crash statistics
    func getCrashStatistics() -> CrashStatistics {
        return queue.sync {
            CrashStatistics(
                totalCrashes: crashReports.count,
                crashesLast24Hours: getCrashesInLast24Hours(),
                crashesLast7Days: getCrashesInLast7Days(),
                mostCommonError: getMostCommonError(),
                crashRate: calculateCrashRate(),
                averageCrashesPerSession: calculateAverageCrashesPerSession(),
                lastCrashTime: crashReports.last?.timestamp
            )
        }
    }
    
    /// Get crash reports for debugging
    func getCrashReports(limit: Int = 50) -> [CrashReport] {
        return queue.sync {
            Array(crashReports.suffix(limit))
        }
    }
    
    /// Clear old crash reports
    func clearOldCrashReports(olderThan days: Int = 30) {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            let cutoffDate = Date().addingTimeInterval(-TimeInterval(days * 24 * 60 * 60))
            self.crashReports.removeAll { $0.timestamp < cutoffDate }
            
            self.logger.info("Cleared crash reports older than \(days) days")
            self.cacheCrashReports()
        }
    }
    
    // MARK: - Private Methods
    
    private func setupCrashHandlers() {
        // Setup NSSetUncaughtExceptionHandler for Objective-C exceptions
        NSSetUncaughtExceptionHandler { exception in
            let crashService = CrashReportingService()
            crashService.reportUncaughtException(exception, stackTrace: exception.callStackSymbols)
        }
        
        // Setup signal handlers for C crashes
        setupSignalHandlers()
        
        // Setup memory warning observer
        NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.reportMemoryWarningCrash()
        }
        
        // Setup battery level monitoring
        UIDevice.current.isBatteryMonitoringEnabled = true
        NotificationCenter.default.addObserver(
            forName: UIDevice.batteryLevelDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            if UIDevice.current.batteryLevel < 0.05 { // 5%
                self?.reportLowBatteryCrash()
            }
        }
    }
    
    private func setupSignalHandlers() {
        // SIGABRT - Abort signal
        signal(SIGABRT) { signal in
            let crashService = CrashReportingService()
            crashService.reportSignalCrash(signal: signal, name: "SIGABRT")
        }
        
        // SIGBUS - Bus error
        signal(SIGBUS) { signal in
            let crashService = CrashReportingService()
            crashService.reportSignalCrash(signal: signal, name: "SIGBUS")
        }
        
        // SIGSEGV - Segmentation violation
        signal(SIGSEGV) { signal in
            let crashService = CrashReportingService()
            crashService.reportSignalCrash(signal: signal, name: "SIGSEGV")
        }
    }
    
    private func createCrashReport(_ error: AppError, context: ErrorContext?, additionalData: [String: Any]) -> CrashReport {
        let deviceInfo = collectDeviceInfo()
        let appInfo = collectAppInfo()
        let userInfo = collectUserInfo()
        let systemInfo = collectSystemInfo()
        
        return CrashReport(
            id: UUID().uuidString,
            error: error.errorDescription ?? "Unknown Error",
            errorCode: error.errorCode,
            stackTrace: Thread.callStackSymbols,
            deviceInfo: deviceInfo,
            appInfo: appInfo,
            userInfo: userInfo,
            systemInfo: systemInfo,
            context: context,
            additionalData: additionalData,
            timestamp: Date(),
            sessionId: getCurrentSessionId(),
            severity: determineCrashSeverity(error)
        )
    }
    
    private func createExceptionCrashReport(_ exception: NSException, stackTrace: [String]) -> CrashReport {
        let deviceInfo = collectDeviceInfo()
        let appInfo = collectAppInfo()
        let userInfo = collectUserInfo()
        let systemInfo = collectSystemInfo()
        
        return CrashReport(
            id: UUID().uuidString,
            error: "Uncaught Exception: \(exception.name.rawValue)",
            errorCode: "EXCEPTION_\(exception.name.rawValue)",
            stackTrace: stackTrace,
            deviceInfo: deviceInfo,
            appInfo: appInfo,
            userInfo: userInfo,
            systemInfo: systemInfo,
            context: nil,
            additionalData: [
                "exceptionName": exception.name.rawValue,
                "exceptionReason": exception.reason ?? "Unknown reason",
                "exceptionUserInfo": exception.userInfo?.description ?? "No user info"
            ],
            timestamp: Date(),
            sessionId: getCurrentSessionId(),
            severity: .critical
        )
    }
    
    private func createMemoryWarningCrashReport() -> CrashReport {
        let deviceInfo = collectDeviceInfo()
        let appInfo = collectAppInfo()
        let userInfo = collectUserInfo()
        let systemInfo = collectSystemInfo()
        
        return CrashReport(
            id: UUID().uuidString,
            error: "Memory Warning - App may crash due to low memory",
            errorCode: "MEMORY_WARNING",
            stackTrace: Thread.callStackSymbols,
            deviceInfo: deviceInfo,
            appInfo: appInfo,
            userInfo: userInfo,
            systemInfo: systemInfo,
            context: nil,
            additionalData: [
                "memoryWarning": true,
                "availableMemory": getAvailableMemory(),
                "memoryPressure": getMemoryPressure()
            ],
            timestamp: Date(),
            sessionId: getCurrentSessionId(),
            severity: .high
        )
    }
    
    private func createLowBatteryCrashReport() -> CrashReport {
        let deviceInfo = collectDeviceInfo()
        let appInfo = collectAppInfo()
        let userInfo = collectUserInfo()
        let systemInfo = collectSystemInfo()
        
        return CrashReport(
            id: UUID().uuidString,
            error: "Low Battery - App may crash due to insufficient power",
            errorCode: "LOW_BATTERY",
            stackTrace: Thread.callStackSymbols,
            deviceInfo: deviceInfo,
            appInfo: appInfo,
            userInfo: userInfo,
            systemInfo: systemInfo,
            context: nil,
            additionalData: [
                "batteryLevel": UIDevice.current.batteryLevel,
                "batteryState": UIDevice.current.batteryState.rawValue,
                "lowPowerMode": ProcessInfo.processInfo.isLowPowerModeEnabled
            ],
            timestamp: Date(),
            sessionId: getCurrentSessionId(),
            severity: .medium
        )
    }
    
    private func reportSignalCrash(signal: Int32, name: String) {
        let deviceInfo = collectDeviceInfo()
        let appInfo = collectAppInfo()
        let userInfo = collectUserInfo()
        let systemInfo = collectSystemInfo()
        
        let crashReport = CrashReport(
            id: UUID().uuidString,
            error: "Signal Crash: \(name)",
            errorCode: "SIGNAL_\(name)",
            stackTrace: Thread.callStackSymbols,
            deviceInfo: deviceInfo,
            appInfo: appInfo,
            userInfo: userInfo,
            systemInfo: systemInfo,
            context: nil,
            additionalData: [
                "signalNumber": signal,
                "signalName": name
            ],
            timestamp: Date(),
            sessionId: getCurrentSessionId(),
            severity: .critical
        )
        
        processCrashReport(crashReport)
    }
    
    private func processCrashReport(_ crashReport: CrashReport) {
        // Store crash report
        crashReports.append(crashReport)
        
        // Update crash session
        updateCrashSession(crashReport)
        
        // Cache crash reports
        cacheCrashReports()
        
        // Send to backend
        sendCrashToBackend(crashReport)
        
        logger.info("Processed crash report: \(crashReport.id)")
    }
    
    private func updateCrashSession(_ crashReport: CrashReport) {
        let sessionId = crashReport.sessionId
        
        if var session = crashSessions[sessionId] {
            session.crashes.append(crashReport)
            session.endTime = crashReport.timestamp
            crashSessions[sessionId] = session
        } else {
            let newSession = CrashSession(
                id: sessionId,
                startTime: crashReport.timestamp,
                endTime: crashReport.timestamp,
                crashes: [crashReport]
            )
            crashSessions[sessionId] = newSession
        }
    }
    
    private func sendCrashToBackend(_ crashReport: CrashReport) {
        Task {
            do {
                let request = CrashReportingRequest(
                    error: crashReport.error,
                    errorCode: crashReport.errorCode,
                    stackTrace: crashReport.stackTrace,
                    deviceInfo: crashReport.deviceInfo,
                    appInfo: crashReport.appInfo,
                    userInfo: crashReport.userInfo,
                    systemInfo: crashReport.systemInfo,
                    context: crashReport.context,
                    additionalData: crashReport.additionalData,
                    timestamp: crashReport.timestamp,
                    sessionId: crashReport.sessionId,
                    severity: crashReport.severity.rawValue
                )
                
                try await apiClient.reportCrash(request)
                logger.debug("Successfully sent crash report to backend: \(crashReport.id)")
                
            } catch {
                logger.error("Failed to send crash report to backend: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Data Collection Methods
    
    private func collectDeviceInfo() -> DeviceInfo {
        return DeviceInfo(
            model: UIDevice.current.model,
            systemName: UIDevice.current.systemName,
            systemVersion: UIDevice.current.systemVersion,
            identifierForVendor: UIDevice.current.identifierForVendor?.uuidString,
            batteryLevel: UIDevice.current.batteryLevel,
            batteryState: UIDevice.current.batteryState.rawValue,
            orientation: UIDevice.current.orientation.rawValue,
            isMultitaskingSupported: UIDevice.current.isMultitaskingSupported,
            userInterfaceIdiom: UIDevice.current.userInterfaceIdiom.rawValue
        )
    }
    
    private func collectAppInfo() -> AppInfo {
        let bundle = Bundle.main
        return AppInfo(
            bundleIdentifier: bundle.bundleIdentifier,
            version: bundle.infoDictionary?["CFBundleShortVersionString"] as? String,
            buildNumber: bundle.infoDictionary?["CFBundleVersion"] as? String,
            name: bundle.infoDictionary?["CFBundleName"] as? String,
            displayName: bundle.infoDictionary?["CFBundleDisplayName"] as? String,
            minimumOSVersion: bundle.infoDictionary?["MinimumOSVersion"] as? String,
            supportedPlatforms: bundle.infoDictionary?["CFBundleSupportedPlatforms"] as? [String],
            isLowPowerModeEnabled: ProcessInfo.processInfo.isLowPowerModeEnabled,
            isBackgroundRefreshEnabled: UIApplication.shared.backgroundRefreshStatus.rawValue
        )
    }
    
    private func collectUserInfo() -> UserInfo {
        return UserInfo(
            userId: getCurrentUserId(),
            sessionId: getCurrentSessionId(),
            isAnonymous: getCurrentUserId() == nil,
            locale: Locale.current.identifier,
            timeZone: TimeZone.current.identifier,
            language: Locale.current.languageCode,
            region: Locale.current.regionCode
        )
    }
    
    private func collectSystemInfo() -> SystemInfo {
        let processInfo = ProcessInfo.processInfo
        return SystemInfo(
            processIdentifier: processInfo.processIdentifier,
            processName: processInfo.processName,
            hostName: processInfo.hostName,
            operatingSystemVersion: processInfo.operatingSystemVersionString,
            processorCount: processInfo.processorCount,
            activeProcessorCount: processInfo.activeProcessorCount,
            physicalMemory: processInfo.physicalMemory,
            systemUptime: processInfo.systemUptime,
            thermalState: processInfo.thermalState.rawValue,
            isLowPowerModeEnabled: processInfo.isLowPowerModeEnabled
        )
    }
    
    // MARK: - Helper Methods
    
    private func getCurrentSessionId() -> String {
        // Get session ID from session manager or create new one
        if let sessionId = UserDefaults.standard.string(forKey: "CurrentSessionId") {
            return sessionId
        } else {
            // Create new session ID and store it
            let newSessionId = UUID().uuidString
            UserDefaults.standard.set(newSessionId, forKey: "CurrentSessionId")
            return newSessionId
        }
    }
    
    private func getCurrentUserId() -> String? {
        // Get user ID from authentication service or cached data
        if let userId = UserDefaults.standard.string(forKey: "CurrentUserId") {
            return userId
        } else {
            // Try to get from Keychain as fallback
            let keychain = KeychainManager()
            if let keychainUserId = try? keychain.getString(forKey: "CurrentUserId") {
                // Cache it in UserDefaults for faster access
                UserDefaults.standard.set(keychainUserId, forKey: "CurrentUserId")
                return keychainUserId
            }
            return nil
        }
    }
    
    private func determineCrashSeverity(_ error: AppError) -> CrashSeverity {
        switch error {
        case .system:
            return .critical
        case .authentication, .permission:
            return .high
        case .network, .data:
            return .medium
        case .validation, .business:
            return .low
        }
    }
    
    private func getAvailableMemory() -> UInt64 {
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
        
        guard kerr == KERN_SUCCESS else { return 0 }
        return info.resident_size
    }
    
    private func getMemoryPressure() -> Double {
        // Simplified memory pressure calculation
        let usedMemory = getAvailableMemory()
        let totalMemory = ProcessInfo.processInfo.physicalMemory
        return Double(usedMemory) / Double(totalMemory)
    }
    
    // MARK: - Statistics Methods
    
    private func getCrashesInLast24Hours() -> Int {
        let last24Hours = Date().addingTimeInterval(-24 * 60 * 60)
        return crashReports.filter { $0.timestamp > last24Hours }.count
    }
    
    private func getCrashesInLast7Days() -> Int {
        let last7Days = Date().addingTimeInterval(-7 * 24 * 60 * 60)
        return crashReports.filter { $0.timestamp > last7Days }.count
    }
    
    private func getMostCommonError() -> String? {
        let errorCounts = Dictionary(grouping: crashReports, by: { $0.errorCode })
            .mapValues { $0.count }
        return errorCounts.max(by: { $0.value < $1.value })?.key
    }
    
    private func calculateCrashRate() -> Double {
        let totalCrashes = crashReports.count
        let appLaunchCount = UserDefaults.standard.integer(forKey: "AppLaunchCount")
        
        if appLaunchCount == 0 {
            return 0.0
        }
        
        return Double(totalCrashes) / Double(appLaunchCount)
    }
    
    private func calculateAverageCrashesPerSession() -> Double {
        let totalCrashes = crashReports.count
        let sessionCount = crashSessions.count
        
        if sessionCount == 0 {
            return 0.0
        }
        
        return Double(totalCrashes) / Double(sessionCount)
    }
    
    // MARK: - Caching Methods
    
    private func loadPendingCrashReports() {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let crashDataURL = documentsDirectory.appendingPathComponent("crash_reports.json")
            
            guard FileManager.default.fileExists(atPath: crashDataURL.path) else {
                logger.info("No pending crash reports found")
                return
            }
            
            let data = try Data(contentsOf: crashDataURL)
            let cachedData = try JSONDecoder().decode(CachedCrashData.self, from: data)
            
            crashReports = cachedData.crashReports
            crashSessions = Dictionary(uniqueKeysWithValues: cachedData.crashSessions.map { ($0.id, $0) })
            
            logger.info("Loaded \(crashReports.count) pending crash reports")
        } catch {
            logger.error("Failed to load pending crash reports: \(error.localizedDescription)")
        }
    }
    
    private func cacheCrashReports() {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let crashDataURL = documentsDirectory.appendingPathComponent("crash_reports.json")
            
            let cachedData = CachedCrashData(
                crashReports: crashReports,
                crashSessions: Array(crashSessions.values),
                lastUpdated: Date()
            )
            
            let data = try JSONEncoder().encode(cachedData)
            try data.write(to: crashDataURL, options: [.atomic])
            
            logger.debug("Cached \(crashReports.count) crash reports")
        } catch {
            logger.error("Failed to cache crash reports: \(error.localizedDescription)")
        }
    }
}

// MARK: - Supporting Types

struct CrashReport: Codable {
    let id: String
    let error: String
    let errorCode: String
    let stackTrace: [String]
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
    let userInfo: UserInfo
    let systemInfo: SystemInfo
    let context: ErrorContext?
    let additionalData: [String: Any]
    let timestamp: Date
    let sessionId: String
    let severity: CrashSeverity
    
    enum CodingKeys: String, CodingKey {
        case id, error, errorCode, stackTrace, deviceInfo, appInfo, userInfo, systemInfo, context, timestamp, sessionId, severity
    }
    
    init(id: String, error: String, errorCode: String, stackTrace: [String], deviceInfo: DeviceInfo, appInfo: AppInfo, userInfo: UserInfo, systemInfo: SystemInfo, context: ErrorContext?, additionalData: [String: Any], timestamp: Date, sessionId: String, severity: CrashSeverity) {
        self.id = id
        self.error = error
        self.errorCode = errorCode
        self.stackTrace = stackTrace
        self.deviceInfo = deviceInfo
        self.appInfo = appInfo
        self.userInfo = userInfo
        self.systemInfo = systemInfo
        self.context = context
        self.additionalData = additionalData
        self.timestamp = timestamp
        self.sessionId = sessionId
        self.severity = severity
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(error, forKey: .error)
        try container.encode(errorCode, forKey: .errorCode)
        try container.encode(stackTrace, forKey: .stackTrace)
        try container.encode(deviceInfo, forKey: .deviceInfo)
        try container.encode(appInfo, forKey: .appInfo)
        try container.encode(userInfo, forKey: .userInfo)
        try container.encode(systemInfo, forKey: .systemInfo)
        try container.encodeIfPresent(context, forKey: .context)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(sessionId, forKey: .sessionId)
        try container.encode(severity, forKey: .severity)
        
        // Encode additionalData as JSON string
        if !additionalData.isEmpty {
            if let jsonData = try? JSONSerialization.data(withJSONObject: additionalData),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                try container.encode(jsonString, forKey: .additionalData)
            }
        }
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        error = try container.decode(String.self, forKey: .error)
        errorCode = try container.decode(String.self, forKey: .errorCode)
        stackTrace = try container.decode([String].self, forKey: .stackTrace)
        deviceInfo = try container.decode(DeviceInfo.self, forKey: .deviceInfo)
        appInfo = try container.decode(AppInfo.self, forKey: .appInfo)
        userInfo = try container.decode(UserInfo.self, forKey: .userInfo)
        systemInfo = try container.decode(SystemInfo.self, forKey: .systemInfo)
        context = try container.decodeIfPresent(ErrorContext.self, forKey: .context)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        sessionId = try container.decode(String.self, forKey: .sessionId)
        severity = try container.decode(CrashSeverity.self, forKey: .severity)
        
        // Decode additionalData from JSON string
        if let jsonString = try? container.decodeIfPresent(String.self, forKey: .additionalData),
           let jsonData = jsonString.data(using: .utf8),
           let decodedData = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {
            additionalData = decodedData
        } else {
            additionalData = [:]
        }
    }
}

struct CrashSession: Codable {
    let id: String
    let startTime: Date
    var endTime: Date
    var crashes: [CrashReport]
}

struct CrashStatistics {
    let totalCrashes: Int
    let crashesLast24Hours: Int
    let crashesLast7Days: Int
    let mostCommonError: String?
    let crashRate: Double
    let averageCrashesPerSession: Double
    let lastCrashTime: Date?
}

struct DeviceInfo: Codable {
    let model: String
    let systemName: String
    let systemVersion: String
    let identifierForVendor: String?
    let batteryLevel: Float
    let batteryState: Int
    let orientation: Int
    let isMultitaskingSupported: Bool
    let userInterfaceIdiom: Int
}

struct AppInfo: Codable {
    let bundleIdentifier: String?
    let version: String?
    let buildNumber: String?
    let name: String?
    let displayName: String?
    let minimumOSVersion: String?
    let supportedPlatforms: [String]?
    let isLowPowerModeEnabled: Bool
    let isBackgroundRefreshEnabled: Int
}

struct UserInfo: Codable {
    let userId: String?
    let sessionId: String
    let isAnonymous: Bool
    let locale: String
    let timeZone: String
    let language: String?
    let region: String?
}

struct SystemInfo: Codable {
    let processIdentifier: Int32
    let processName: String
    let hostName: String
    let operatingSystemVersion: String
    let processorCount: Int
    let activeProcessorCount: Int
    let physicalMemory: UInt64
    let systemUptime: TimeInterval
    let thermalState: Int
    let isLowPowerModeEnabled: Bool
}

enum CrashSeverity: String, Codable, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

struct CrashReportingConfiguration {
    let maxCrashReports = 1000
    let crashRetentionDays = 30
    let batchSize = 10
    let retryAttempts = 3
}

struct CachedCrashData: Codable {
    let crashReports: [CrashReport]
    let crashSessions: [CrashSession]
    let lastUpdated: Date
}
