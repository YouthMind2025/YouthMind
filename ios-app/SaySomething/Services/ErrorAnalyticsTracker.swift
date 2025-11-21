import Foundation
import os.log

// MARK: - Error Analytics Tracker
/// Tracks error patterns, frequency, and provides comprehensive error analysis
final class ErrorAnalyticsTracker {
    
    // MARK: - Dependencies
    private let privacyManager: AnalyticsPrivacyManager
    private let apiClient: AnalyticsAPIClient
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "ErrorAnalyticsTracker")
    private let queue = DispatchQueue(label: "com.youthmind.error.tracker", qos: .background)
    
    // Local storage for offline tracking
    private var errorHistory: [ErrorRecord] = []
    private var errorPatterns: [String: ErrorPattern] = [:]
    private let maxHistorySize = 1000
    
    // MARK: - Initialization
    init(privacyManager: AnalyticsPrivacyManager) {
        self.privacyManager = privacyManager
        self.apiClient = AnalyticsAPIClient()
    }
    
    // MARK: - Public Methods
    
    /// Initialize the error tracker
    func initialize() async {
        logger.info("Initializing Error Analytics Tracker")
        
        // Load cached error data
        await loadCachedErrorData()
        
        // Start background processing
        startBackgroundProcessing()
    }
    
    /// Track an error with comprehensive analytics
    func trackError(_ error: AppError, context: ErrorContext? = nil) async {
        guard privacyManager.canTrackErrors() else { return }
        
        let errorRecord = ErrorRecord(
            id: UUID(),
            error: error,
            context: context ?? ErrorContext(),
            timestamp: Date(),
            sessionId: context?.sessionId ?? generateSessionId(),
            userId: context?.userId
        )
        
        await queue.async { [weak self] in
            self?.processErrorRecord(errorRecord)
        }
    }
    
    /// Get error patterns for analysis
    func getErrorPatterns() async -> [ErrorPattern] {
        return await queue.sync {
            return Array(errorPatterns.values)
        }
    }
    
    /// Get error frequency analysis
    func getErrorFrequencyAnalysis() async -> ErrorFrequencyAnalysis {
        return await queue.sync {
            let now = Date()
            let last24Hours = errorHistory.filter { $0.timestamp > now.addingTimeInterval(-24 * 3600) }
            let last7Days = errorHistory.filter { $0.timestamp > now.addingTimeInterval(-7 * 24 * 3600) }
            let last30Days = errorHistory.filter { $0.timestamp > now.addingTimeInterval(-30 * 24 * 3600) }
            
            return ErrorFrequencyAnalysis(
                last24Hours: last24Hours.count,
                last7Days: last7Days.count,
                last30Days: last30Days.count,
                totalErrors: errorHistory.count,
                errorsByType: Dictionary(grouping: errorHistory, by: { $0.error.errorCode })
                    .mapValues { $0.count },
                errorsBySeverity: Dictionary(grouping: errorHistory, by: { String(describing: $0.error.severity) })
                    .mapValues { $0.count },
                topErrorSources: getTopErrorSources(),
                averageErrorsPerDay: Double(last30Days.count) / 30.0
            )
        }
    }
    
    /// Get error trends over time
    func getErrorTrends() async -> [ErrorTrend] {
        return await queue.sync {
            let calendar = Calendar.current
            let now = Date()
            let last30Days = errorHistory.filter { $0.timestamp > now.addingTimeInterval(-30 * 24 * 3600) }
            
            let groupedByDay = Dictionary(grouping: last30Days) { error in
                calendar.startOfDay(for: error.timestamp)
            }
            
            return groupedByDay.map { date, errors in
                let severityCounts = Dictionary(grouping: errors, by: { String(describing: $0.error.severity) })
                    .mapValues { $0.count }
                
                return ErrorTrend(
                    date: date,
                    errorCount: errors.count,
                    severity: severityCounts.max { $0.value < $1.value }?.key ?? "unknown"
                )
            }.sorted { $0.date < $1.date }
        }
    }
    
    // MARK: - Private Methods
    
    private func processErrorRecord(_ record: ErrorRecord) {
        // Add to history
        errorHistory.append(record)
        
        // Maintain history size limit
        if errorHistory.count > maxHistorySize {
            errorHistory.removeFirst(errorHistory.count - maxHistorySize)
        }
        
        // Update error patterns
        updateErrorPatterns(for: record)
        
        // Send to backend if online
        Task {
            await sendErrorToBackend(record)
        }
        
        // Cache locally
        cacheErrorData()
        
        logger.info("Tracked error: \(record.error.errorCode) with severity: \(record.error.severity)")
    }
    
    private func updateErrorPatterns(for record: ErrorRecord) {
        let errorCode = record.error.errorCode
        let pattern = errorPatterns[errorCode] ?? ErrorPattern(
            errorCode: errorCode,
            count: 0,
            firstOccurrence: record.timestamp,
            lastOccurrence: record.timestamp,
            severity: record.error.severity,
            isRetryable: record.error.isRetryable,
            commonContexts: []
        )
        
        pattern.count += 1
        pattern.lastOccurrence = record.timestamp
        
        // Update common contexts
        if let context = record.context {
            pattern.commonContexts.append(context)
            if pattern.commonContexts.count > 10 {
                pattern.commonContexts.removeFirst()
            }
        }
        
        errorPatterns[errorCode] = pattern
    }
    
    private func getTopErrorSources() -> [ErrorSource] {
        let sourceCounts = Dictionary(grouping: errorHistory) { record in
            record.context.screenName ?? "unknown"
        }.mapValues { $0.count }
        
        let total = sourceCounts.values.reduce(0, +)
        
        return sourceCounts.map { source, count in
            ErrorSource(
                source: source,
                count: count,
                percentage: total > 0 ? Double(count) / Double(total) * 100.0 : 0.0
            )
        }.sorted { $0.count > $1.count }.prefix(10).map { $0 }
    }
    
    private func sendErrorToBackend(_ record: ErrorRecord) async {
        do {
            let request = ErrorTrackingRequest(
                errorCode: record.error.errorCode,
                severity: String(describing: record.error.severity),
                message: record.error.errorDescription ?? "",
                context: record.context,
                timestamp: record.timestamp,
                sessionId: record.sessionId,
                userId: record.userId
            )
            
            try await apiClient.trackError(request)
            logger.debug("Successfully sent error to backend: \(record.error.errorCode)")
            
        } catch {
            logger.error("Failed to send error to backend: \(error.localizedDescription)")
        }
    }
    
    private func loadCachedErrorData() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let errorDataURL = documentsDirectory.appendingPathComponent("analytics_error_data.json")
            
            guard FileManager.default.fileExists(atPath: errorDataURL.path) else {
                logger.info("No cached error data found")
                return
            }
            
            let data = try Data(contentsOf: errorDataURL)
            let cachedData = try JSONDecoder().decode(CachedErrorData.self, from: data)
            
            await queue.async { [weak self] in
                self?.errorHistory = cachedData.errorHistory
                self?.errorPatterns = Dictionary(uniqueKeysWithValues: cachedData.errorPatterns.map { ($0.errorCode, $0) })
            }
            
            logger.info("Loaded \(cachedData.errorHistory.count) cached error records")
        } catch {
            logger.error("Failed to load cached error data: \(error.localizedDescription)")
        }
    }
    
    private func cacheErrorData() {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let errorDataURL = documentsDirectory.appendingPathComponent("analytics_error_data.json")
                
                let cachedData = CachedErrorData(
                    errorHistory: self.errorHistory,
                    errorPatterns: Array(self.errorPatterns.values),
                    lastUpdated: Date()
                )
                
                let data = try JSONEncoder().encode(cachedData)
                try data.write(to: errorDataURL, options: [.atomic])
                
                self.logger.debug("Cached \(self.errorHistory.count) error records")
            } catch {
                self.logger.error("Failed to cache error data: \(error.localizedDescription)")
            }
        }
    }
    
    private func startBackgroundProcessing() {
        // Process error patterns every 5 minutes
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { [weak self] _ in
            Task {
                await self?.processErrorPatterns()
            }
        }
    }
    
    private func processErrorPatterns() async {
        // Analyze patterns and detect anomalies
        let patterns = await getErrorPatterns()
        
        for pattern in patterns {
            if pattern.count > 10 && pattern.isRetryable {
                logger.warning("High frequency retryable error detected: \(pattern.errorCode) - \(pattern.count) occurrences")
            }
        }
    }
    
    private func generateSessionId() -> String {
        return UUID().uuidString
    }
}

// MARK: - Error Tracking Models

/// Error record for tracking
struct ErrorRecord: Codable {
    let id: UUID
    let error: AppError
    let context: ErrorContext
    let timestamp: Date
    let sessionId: String
    let userId: String?
}

/// Error pattern analysis
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

/// Error frequency analysis
struct ErrorFrequencyAnalysis: Codable {
    let last24Hours: Int
    let last7Days: Int
    let last30Days: Int
    let totalErrors: Int
    let errorsByType: [String: Int]
    let errorsBySeverity: [String: Int]
    let topErrorSources: [ErrorSource]
    let averageErrorsPerDay: Double
}

/// Error tracking request for backend
struct ErrorTrackingRequest: Codable {
    let errorCode: String
    let severity: String
    let message: String
    let context: ErrorContext
    let timestamp: Date
    let sessionId: String
    let userId: String?
}

/// Cached error data for persistence
struct CachedErrorData: Codable {
    let errorHistory: [ErrorRecord]
    let errorPatterns: [ErrorPattern]
    let lastUpdated: Date
}
