import Foundation
import os.log

// MARK: - Crash Reporting Integration
/// Integrates with crash reporting services and provides comprehensive crash analytics
final class CrashReportingIntegration {
    
    // MARK: - Dependencies
    private let privacyManager: AnalyticsPrivacyManager
    private let apiClient: AnalyticsAPIClient
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "CrashReportingIntegration")
    private let queue = DispatchQueue(label: "com.youthmind.crash.reporting", qos: .background)
    
    // Crash tracking
    private var crashReports: [CrashReport] = []
    private var crashSessions: [String: CrashSession] = [:]
    private let maxCrashReports = 100
    
    // MARK: - Initialization
    init(privacyManager: AnalyticsPrivacyManager) {
        self.privacyManager = privacyManager
        self.apiClient = AnalyticsAPIClient()
    }
    
    // MARK: - Public Methods
    
    /// Initialize crash reporting
    func initialize() async {
        logger.info("Initializing Crash Reporting Integration")
        
        // Setup crash handlers
        setupCrashHandlers()
        
        // Load pending crash reports
        await loadPendingCrashReports()
        
        // Start background processing
        startBackgroundProcessing()
    }
    
    /// Report a crash
    func reportCrash(_ crash: CrashReport) async {
        guard privacyManager.canReportCrashes() else { return }
        
        await queue.async { [weak self] in
            self?.processCrashReport(crash)
        }
    }
    
    /// Get crash analytics
    func getCrashAnalytics() async -> CrashAnalytics {
        return await queue.sync {
            let now = Date()
            let last24Hours = crashReports.filter { $0.timestamp > now.addingTimeInterval(-24 * 3600) }
            let last7Days = crashReports.filter { $0.timestamp > now.addingTimeInterval(-7 * 24 * 3600) }
            let last30Days = crashReports.filter { $0.timestamp > now.addingTimeInterval(-30 * 24 * 3600) }
            
            let crashesByType = Dictionary(grouping: crashReports, by: { $0.error })
                .mapValues { $0.count }
            
            let crashesByDevice = Dictionary(grouping: crashReports, by: { $0.deviceInfo.model })
                .mapValues { $0.count }
            
            let crashesByAppVersion = Dictionary(grouping: crashReports, by: { $0.appInfo.version })
                .mapValues { $0.count }
            
            return CrashAnalytics(
                totalCrashes: crashReports.count,
                last24Hours: last24Hours.count,
                last7Days: last7Days.count,
                last30Days: last30Days.count,
                crashesByType: crashesByType,
                crashesByDevice: crashesByDevice,
                crashesByAppVersion: crashesByAppVersion,
                averageCrashesPerDay: Double(last30Days.count) / 30.0,
                crashRate: calculateCrashRate(),
                timestamp: now
            )
        }
    }
    
    /// Get crash trends
    func getCrashTrends() async -> [CrashTrend] {
        return await queue.sync {
            let calendar = Calendar.current
            let now = Date()
            let last30Days = crashReports.filter { $0.timestamp > now.addingTimeInterval(-30 * 24 * 3600) }
            
            let groupedByDay = Dictionary(grouping: last30Days) { crash in
                calendar.startOfDay(for: crash.timestamp)
            }
            
            return groupedByDay.map { date, crashes in
                let crashesByType = Dictionary(grouping: crashes, by: { $0.error })
                    .mapValues { $0.count }
                
                return CrashTrend(
                    date: date,
                    crashCount: crashes.count,
                    topCrashType: crashesByType.max { $0.value < $1.value }?.key ?? "unknown"
                )
            }.sorted { $0.date < $1.date }
        }
    }
    
    /// Get crash patterns
    func getCrashPatterns() async -> [CrashPattern] {
        return await queue.sync {
            let patterns = Dictionary(grouping: crashReports, by: { $0.error })
                .map { (error, crashes) in
                    CrashPattern(
                        error: error,
                        count: crashes.count,
                        firstOccurrence: crashes.min { $0.timestamp < $1.timestamp }?.timestamp ?? Date(),
                        lastOccurrence: crashes.max { $0.timestamp < $1.timestamp }?.timestamp ?? Date(),
                        affectedDevices: Set(crashes.map { $0.deviceInfo.model }).count,
                        affectedVersions: Set(crashes.map { $0.appInfo.version }).count,
                        stackTraces: crashes.map { $0.stackTrace }
                    )
                }
            
            return Array(patterns.values).sorted { $0.count > $1.count }
        }
    }
    
    /// Get crash session analytics
    func getCrashSessionAnalytics() async -> [CrashSessionAnalytics] {
        return await queue.sync {
            return crashSessions.values.map { session in
                CrashSessionAnalytics(
                    sessionId: session.id,
                    startTime: session.startTime,
                    endTime: session.endTime,
                    crashCount: session.crashes.count,
                    totalDuration: session.endTime.timeIntervalSince(session.startTime),
                    deviceInfo: session.deviceInfo,
                    appInfo: session.appInfo
                )
            }
        }
    }
    
    // MARK: - Private Methods
    
    private func processCrashReport(_ crash: CrashReport) {
        // Add to crash reports
        crashReports.append(crash)
        
        // Maintain max crash reports
        if crashReports.count > maxCrashReports {
            crashReports.removeFirst(crashReports.count - maxCrashReports)
        }
        
        // Update crash session
        updateCrashSession(crash)
        
        // Send to backend
        Task {
            await sendCrashToBackend(crash)
        }
        
        // Cache locally
        cacheCrashReport(crash)
        
        logger.warning("Processed crash report: \(crash.error)")
    }
    
    private func updateCrashSession(_ crash: CrashReport) {
        let sessionId = crash.userInfo?.sessionId ?? generateSessionId()
        
        var session = crashSessions[sessionId] ?? CrashSession(
            id: sessionId,
            startTime: crash.timestamp,
            endTime: crash.timestamp,
            crashes: [],
            deviceInfo: crash.deviceInfo,
            appInfo: crash.appInfo
        )
        
        session.crashes.append(crash)
        session.endTime = crash.timestamp
        crashSessions[sessionId] = session
    }
    
    private func calculateCrashRate() -> Double {
        // Calculate crash rate based on actual app launches vs crashes
        let totalCrashes = crashReports.count
        
        // Get app launch count from UserDefaults (tracked by app lifecycle)
        let appLaunchCount = UserDefaults.standard.integer(forKey: "AppLaunchCount")
        
        // If no launch count is tracked, estimate based on crash frequency
        if appLaunchCount == 0 {
            // Estimate based on crash frequency and typical crash rates
            let estimatedLaunches = max(1, totalCrashes * 50) // Assume 2% crash rate for estimation
            return totalCrashes > 0 ? Double(totalCrashes) / Double(estimatedLaunches) : 0.0
        }
        
        return appLaunchCount > 0 ? Double(totalCrashes) / Double(appLaunchCount) : 0.0
    }
    
    private func sendCrashToBackend(_ crash: CrashReport) async {
        do {
            let request = CrashReportingRequest(
                error: crash.error,
                stackTrace: crash.stackTrace,
                deviceInfo: crash.deviceInfo,
                appInfo: crash.appInfo,
                userInfo: crash.userInfo,
                timestamp: crash.timestamp
            )
            
            try await apiClient.reportCrash(request)
            logger.debug("Successfully sent crash to backend: \(crash.error)")
            
        } catch {
            logger.error("Failed to send crash to backend: \(error.localizedDescription)")
        }
    }
    
    private func setupCrashHandlers() {
        // Setup NSSetUncaughtExceptionHandler for Objective-C exceptions
        NSSetUncaughtExceptionHandler { exception in
            let crashReport = CrashReport(
                error: "Uncaught Exception: \(exception.name.rawValue)",
                stackTrace: exception.callStackSymbols,
                userInfo: UserInfo(
                    userId: "unknown",
                    sessionId: UUID().uuidString,
                    isAnonymous: true
                )
            )
            
            Task {
                await CrashReportingIntegration.shared.reportCrash(crashReport)
            }
        }
        
        // Setup signal handlers for C++ exceptions and other signals
        setupSignalHandlers()
    }
    
    private func setupSignalHandlers() {
        // SIGABRT - Abort signal
        signal(SIGABRT) { signal in
            let crashReport = CrashReport(
                error: "SIGABRT - Abort signal",
                stackTrace: Thread.callStackSymbols,
                userInfo: UserInfo(
                    userId: "unknown",
                    sessionId: UUID().uuidString,
                    isAnonymous: true
                )
            )
            
            Task {
                await CrashReportingIntegration.shared.reportCrash(crashReport)
            }
        }
        
        // SIGSEGV - Segmentation violation
        signal(SIGSEGV) { signal in
            let crashReport = CrashReport(
                error: "SIGSEGV - Segmentation violation",
                stackTrace: Thread.callStackSymbols,
                userInfo: UserInfo(
                    userId: "unknown",
                    sessionId: UUID().uuidString,
                    isAnonymous: true
                )
            )
            
            Task {
                await CrashReportingIntegration.shared.reportCrash(crashReport)
            }
        }
    }
    
    private func loadPendingCrashReports() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let crashDataURL = documentsDirectory.appendingPathComponent("analytics_crash_data.json")
            
            guard FileManager.default.fileExists(atPath: crashDataURL.path) else {
                logger.info("No pending crash reports found")
                return
            }
            
            let data = try Data(contentsOf: crashDataURL)
            let cachedData = try JSONDecoder().decode(CachedCrashData.self, from: data)
            
            await queue.async { [weak self] in
                self?.crashReports = cachedData.crashReports
                self?.crashSessions = Dictionary(uniqueKeysWithValues: cachedData.crashSessions.map { ($0.id, $0) })
            }
            
            logger.info("Loaded \(cachedData.crashReports.count) pending crash reports")
        } catch {
            logger.error("Failed to load pending crash reports: \(error.localizedDescription)")
        }
    }
    
    private func cacheCrashReport(_ crash: CrashReport) {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let crashDataURL = documentsDirectory.appendingPathComponent("analytics_crash_data.json")
                
                let cachedData = CachedCrashData(
                    crashReports: self.crashReports,
                    crashSessions: Array(self.crashSessions.values),
                    lastUpdated: Date()
                )
                
                let data = try JSONEncoder().encode(cachedData)
                try data.write(to: crashDataURL, options: [.atomic])
                
                self.logger.debug("Cached \(self.crashReports.count) crash reports")
            } catch {
                self.logger.error("Failed to cache crash report: \(error.localizedDescription)")
            }
        }
    }
    
    private func startBackgroundProcessing() {
        // Process crash reports every 10 minutes
        Timer.scheduledTimer(withTimeInterval: 600, repeats: true) { [weak self] _ in
            Task {
                await self?.processPendingCrashReports()
            }
        }
    }
    
    private func processPendingCrashReports() async {
        // Process any pending crash reports
        let pendingReports = await queue.sync { crashReports.filter { $0.timestamp > Date().addingTimeInterval(-24 * 3600) } }
        
        for report in pendingReports {
            await sendCrashToBackend(report)
        }
    }
    
    private func generateSessionId() -> String {
        return UUID().uuidString
    }
}

// MARK: - Crash Reporting Models

/// Crash session tracking
struct CrashSession: Codable {
    let id: String
    let startTime: Date
    var endTime: Date
    var crashes: [CrashReport]
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
}

/// Crash analytics
struct CrashAnalytics: Codable {
    let totalCrashes: Int
    let last24Hours: Int
    let last7Days: Int
    let last30Days: Int
    let crashesByType: [String: Int]
    let crashesByDevice: [String: Int]
    let crashesByAppVersion: [String: Int]
    let averageCrashesPerDay: Double
    let crashRate: Double
    let timestamp: Date
}

/// Crash trend
struct CrashTrend: Codable {
    let date: Date
    let crashCount: Int
    let topCrashType: String
}

/// Crash pattern
struct CrashPattern: Codable {
    let error: String
    let count: Int
    let firstOccurrence: Date
    let lastOccurrence: Date
    let affectedDevices: Int
    let affectedVersions: Int
    let stackTraces: [[String]]
}

/// Crash session analytics
struct CrashSessionAnalytics: Codable {
    let sessionId: String
    let startTime: Date
    let endTime: Date
    let crashCount: Int
    let totalDuration: TimeInterval
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
}

/// Crash reporting request for backend
struct CrashReportingRequest: Codable {
    let error: String
    let stackTrace: [String]
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
    let userInfo: UserInfo?
    let timestamp: Date
}

/// Cached crash data for persistence
struct CachedCrashData: Codable {
    let crashReports: [CrashReport]
    let crashSessions: [CrashSession]
    let lastUpdated: Date
}
