import Foundation
import os.log

// MARK: - User Behavior Tracker
/// Tracks user behavior during error scenarios and provides behavioral analytics
final class UserBehaviorTracker {
    
    // MARK: - Dependencies
    private let privacyManager: AnalyticsPrivacyManager
    private let apiClient: AnalyticsAPIClient
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "UserBehaviorTracker")
    private let queue = DispatchQueue(label: "com.youthmind.behavior.tracker", qos: .background)
    
    // Local storage for offline tracking
    private var behaviorHistory: [BehaviorRecord] = []
    private var userSessions: [String: UserSession] = [:]
    private let maxHistorySize = 2000
    
    // MARK: - Initialization
    init(privacyManager: AnalyticsPrivacyManager) {
        self.privacyManager = privacyManager
        self.apiClient = AnalyticsAPIClient()
    }
    
    // MARK: - Public Methods
    
    /// Initialize the behavior tracker
    func initialize() async {
        logger.info("Initializing User Behavior Tracker")
        
        // Load cached behavior data
        await loadCachedBehaviorData()
        
        // Start background processing
        startBackgroundProcessing()
    }
    
    /// Track user behavior
    func trackBehavior(_ behavior: UserBehavior, context: BehaviorContext? = nil) async {
        guard privacyManager.canTrackUserBehavior() else { return }
        
        let behaviorRecord = BehaviorRecord(
            id: UUID(),
            behavior: behavior,
            context: context ?? BehaviorContext(),
            timestamp: Date(),
            sessionId: context?.errorCode != nil ? generateSessionId() : getCurrentSessionId(),
            userId: privacyManager.getAnonymizedUserId()
        )
        
        await queue.async { [weak self] in
            self?.processBehaviorRecord(behaviorRecord)
        }
    }
    
    /// Track error behavior specifically
    func trackErrorBehavior(_ error: AppError, context: ErrorContext? = nil) async {
        let behaviorContext = BehaviorContext(
            errorCode: error.errorCode,
            screenName: context?.screenName,
            additionalData: context?.additionalData ?? [:]
        )
        
        await trackBehavior(.errorEncountered, context: behaviorContext)
    }
    
    /// Get user behavior analytics
    func getUserBehaviorAnalytics() async -> UserBehaviorAnalytics {
        return await queue.sync {
            let now = Date()
            let last24Hours = behaviorHistory.filter { $0.timestamp > now.addingTimeInterval(-24 * 3600) }
            let last7Days = behaviorHistory.filter { $0.timestamp > now.addingTimeInterval(-7 * 24 * 3600) }
            
            let behaviorsByType = Dictionary(grouping: behaviorHistory, by: { $0.behavior.rawValue })
                .mapValues { $0.count }
            
            let errorRecoveryRate = calculateErrorRecoveryRate()
            let averageRecoveryTime = calculateAverageRecoveryTime()
            let userSatisfactionScore = calculateUserSatisfactionScore()
            
            return UserBehaviorAnalytics(
                totalBehaviors: behaviorHistory.count,
                behaviorsByType: behaviorsByType,
                errorRecoveryRate: errorRecoveryRate,
                averageRecoveryTime: averageRecoveryTime,
                userSatisfactionScore: userSatisfactionScore,
                timestamp: now
            )
        }
    }
    
    /// Get behavior patterns for a specific user session
    func getBehaviorPatterns(for sessionId: String) async -> [BehaviorPattern] {
        return await queue.sync {
            let sessionBehaviors = behaviorHistory.filter { $0.sessionId == sessionId }
            return analyzeBehaviorPatterns(sessionBehaviors)
        }
    }
    
    /// Get error recovery metrics
    func getErrorRecoveryMetrics() async -> ErrorRecoveryMetrics {
        return await queue.sync {
            let errorBehaviors = behaviorHistory.filter { $0.behavior == .errorEncountered }
            let retryBehaviors = behaviorHistory.filter { $0.behavior == .retryAttempted }
            let dismissBehaviors = behaviorHistory.filter { $0.behavior == .errorDismissed }
            let helpBehaviors = behaviorHistory.filter { $0.behavior == .helpRequested }
            
            let totalErrors = errorBehaviors.count
            let totalRetries = retryBehaviors.count
            let totalDismissals = dismissBehaviors.count
            let totalHelpRequests = helpBehaviors.count
            
            return ErrorRecoveryMetrics(
                totalErrors: totalErrors,
                retryRate: totalErrors > 0 ? Double(totalRetries) / Double(totalErrors) : 0.0,
                dismissalRate: totalErrors > 0 ? Double(totalDismissals) / Double(totalErrors) : 0.0,
                helpRequestRate: totalErrors > 0 ? Double(totalHelpRequests) / Double(totalErrors) : 0.0,
                averageRecoveryTime: calculateAverageRecoveryTime(),
                successfulRecoveries: calculateSuccessfulRecoveries()
            )
        }
    }
    
    // MARK: - Private Methods
    
    private func processBehaviorRecord(_ record: BehaviorRecord) {
        // Add to history
        behaviorHistory.append(record)
        
        // Maintain history size limit
        if behaviorHistory.count > maxHistorySize {
            behaviorHistory.removeFirst(behaviorHistory.count - maxHistorySize)
        }
        
        // Update user session
        updateUserSession(record)
        
        // Send to backend if online
        Task {
            await sendBehaviorToBackend(record)
        }
        
        // Cache locally
        cacheBehaviorData()
        
        logger.debug("Tracked behavior: \(record.behavior.rawValue) for session: \(record.sessionId)")
    }
    
    private func updateUserSession(_ record: BehaviorRecord) {
        let sessionId = record.sessionId
        var session = userSessions[sessionId] ?? UserSession(
            id: sessionId,
            userId: record.userId,
            startTime: record.timestamp,
            lastActivity: record.timestamp,
            behaviors: []
        )
        
        session.behaviors.append(record)
        session.lastActivity = record.timestamp
        userSessions[sessionId] = session
    }
    
    private func calculateErrorRecoveryRate() -> Double {
        let errorBehaviors = behaviorHistory.filter { $0.behavior == .errorEncountered }
        let retryBehaviors = behaviorHistory.filter { $0.behavior == .retryAttempted }
        
        guard !errorBehaviors.isEmpty else { return 0.0 }
        
        // Calculate recovery rate based on retry attempts after errors
        let errorSessions = Set(errorBehaviors.map { $0.sessionId })
        let retrySessions = Set(retryBehaviors.map { $0.sessionId })
        
        let recoveredSessions = errorSessions.intersection(retrySessions)
        return Double(recoveredSessions.count) / Double(errorSessions.count)
    }
    
    private func calculateAverageRecoveryTime() -> TimeInterval {
        let errorBehaviors = behaviorHistory.filter { $0.behavior == .errorEncountered }
        let retryBehaviors = behaviorHistory.filter { $0.behavior == .retryAttempted }
        
        var recoveryTimes: [TimeInterval] = []
        
        for errorBehavior in errorBehaviors {
            let sessionRetries = retryBehaviors.filter { $0.sessionId == errorBehavior.sessionId }
            if let firstRetry = sessionRetries.first {
                let recoveryTime = firstRetry.timestamp.timeIntervalSince(errorBehavior.timestamp)
                if recoveryTime > 0 {
                    recoveryTimes.append(recoveryTime)
                }
            }
        }
        
        return recoveryTimes.isEmpty ? 0.0 : recoveryTimes.reduce(0, +) / Double(recoveryTimes.count)
    }
    
    private func calculateUserSatisfactionScore() -> Double {
        // Calculate satisfaction based on behavior patterns
        let helpRequests = behaviorHistory.filter { $0.behavior == .helpRequested }.count
        let feedbackProvided = behaviorHistory.filter { $0.behavior == .feedbackProvided }.count
        let appRestarts = behaviorHistory.filter { $0.behavior == .appRestarted }.count
        
        let totalBehaviors = behaviorHistory.count
        guard totalBehaviors > 0 else { return 0.0 }
        
        // Higher help requests and app restarts indicate lower satisfaction
        let negativeScore = Double(helpRequests + appRestarts) / Double(totalBehaviors)
        let positiveScore = Double(feedbackProvided) / Double(totalBehaviors)
        
        // Return score between 0 and 1 (higher is better)
        return max(0.0, min(1.0, positiveScore - negativeScore + 0.5))
    }
    
    private func calculateSuccessfulRecoveries() -> Int {
        let errorSessions = Set(behaviorHistory.filter { $0.behavior == .errorEncountered }.map { $0.sessionId })
        let retrySessions = Set(behaviorHistory.filter { $0.behavior == .retryAttempted }.map { $0.sessionId })
        
        return errorSessions.intersection(retrySessions).count
    }
    
    private func analyzeBehaviorPatterns(_ behaviors: [BehaviorRecord]) -> [BehaviorPattern] {
        let groupedByType = Dictionary(grouping: behaviors, by: { $0.behavior })
        
        return groupedByType.map { behavior, records in
            BehaviorPattern(
                behavior: behavior,
                count: records.count,
                firstOccurrence: records.min { $0.timestamp < $1.timestamp }?.timestamp ?? Date(),
                lastOccurrence: records.max { $0.timestamp < $1.timestamp }?.timestamp ?? Date(),
                averageInterval: calculateAverageInterval(records),
                commonContexts: records.map { $0.context }
            )
        }
    }
    
    private func calculateAverageInterval(_ records: [BehaviorRecord]) -> TimeInterval {
        guard records.count > 1 else { return 0.0 }
        
        let sortedRecords = records.sorted { $0.timestamp < $1.timestamp }
        var intervals: [TimeInterval] = []
        
        for i in 1..<sortedRecords.count {
            let interval = sortedRecords[i].timestamp.timeIntervalSince(sortedRecords[i-1].timestamp)
            intervals.append(interval)
        }
        
        return intervals.reduce(0, +) / Double(intervals.count)
    }
    
    private func sendBehaviorToBackend(_ record: BehaviorRecord) async {
        do {
            let request = BehaviorTrackingRequest(
                behavior: record.behavior.rawValue,
                context: record.context,
                timestamp: record.timestamp,
                sessionId: record.sessionId,
                userId: record.userId
            )
            
            try await apiClient.trackBehavior(request)
            logger.debug("Successfully sent behavior to backend: \(record.behavior.rawValue)")
            
        } catch {
            logger.error("Failed to send behavior to backend: \(error.localizedDescription)")
        }
    }
    
    private func loadCachedBehaviorData() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let behaviorDataURL = documentsDirectory.appendingPathComponent("analytics_behavior_data.json")
            
            guard FileManager.default.fileExists(atPath: behaviorDataURL.path) else {
                logger.info("No cached behavior data found")
                return
            }
            
            let data = try Data(contentsOf: behaviorDataURL)
            let cachedData = try JSONDecoder().decode(CachedBehaviorData.self, from: data)
            
            await queue.async { [weak self] in
                self?.behaviorHistory = cachedData.behaviorHistory
                self?.userSessions = Dictionary(uniqueKeysWithValues: cachedData.userSessions.map { ($0.id, $0) })
            }
            
            logger.info("Loaded \(cachedData.behaviorHistory.count) cached behavior records")
        } catch {
            logger.error("Failed to load cached behavior data: \(error.localizedDescription)")
        }
    }
    
    private func cacheBehaviorData() {
        queue.async { [weak self] in
            guard let self = self else { return }
            
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let behaviorDataURL = documentsDirectory.appendingPathComponent("analytics_behavior_data.json")
                
                let cachedData = CachedBehaviorData(
                    behaviorHistory: self.behaviorHistory,
                    userSessions: Array(self.userSessions.values),
                    lastUpdated: Date()
                )
                
                let data = try JSONEncoder().encode(cachedData)
                try data.write(to: behaviorDataURL, options: [.atomic])
                
                self.logger.debug("Cached \(self.behaviorHistory.count) behavior records")
            } catch {
                self.logger.error("Failed to cache behavior data: \(error.localizedDescription)")
            }
        }
    }
    
    private func startBackgroundProcessing() {
        // Process behavior patterns every 10 minutes
        Timer.scheduledTimer(withTimeInterval: 600, repeats: true) { [weak self] _ in
            Task {
                await self?.processBehaviorPatterns()
            }
        }
    }
    
    private func processBehaviorPatterns() async {
        // Analyze patterns and detect anomalies
        let sessions = await queue.sync { Array(userSessions.values) }
        
        for session in sessions {
            let patterns = analyzeBehaviorPatterns(session.behaviors)
            
            // Detect unusual behavior patterns
            for pattern in patterns {
                if pattern.count > 5 && pattern.behavior == .errorEncountered {
                    logger.warning("High error frequency in session: \(session.id) - \(pattern.count) errors")
                }
            }
        }
    }
    
    private func getCurrentSessionId() -> String {
        // Return current session ID or generate new one
        return generateSessionId()
    }
    
    private func generateSessionId() -> String {
        return UUID().uuidString
    }
}

// MARK: - Behavior Tracking Models

/// Behavior record for tracking
struct BehaviorRecord: Codable {
    let id: UUID
    let behavior: UserBehavior
    let context: BehaviorContext
    let timestamp: Date
    let sessionId: String
    let userId: String?
}

/// User session tracking
struct UserSession: Codable {
    let id: String
    let userId: String?
    let startTime: Date
    var lastActivity: Date
    var behaviors: [BehaviorRecord]
}

/// Behavior pattern analysis
struct BehaviorPattern: Codable {
    let behavior: UserBehavior
    let count: Int
    let firstOccurrence: Date
    let lastOccurrence: Date
    let averageInterval: TimeInterval
    let commonContexts: [BehaviorContext]
}

/// Error recovery metrics
struct ErrorRecoveryMetrics: Codable {
    let totalErrors: Int
    let retryRate: Double
    let dismissalRate: Double
    let helpRequestRate: Double
    let averageRecoveryTime: TimeInterval
    let successfulRecoveries: Int
}

/// Behavior tracking request for backend
struct BehaviorTrackingRequest: Codable {
    let behavior: String
    let context: BehaviorContext
    let timestamp: Date
    let sessionId: String
    let userId: String?
}

/// Cached behavior data for persistence
struct CachedBehaviorData: Codable {
    let behaviorHistory: [BehaviorRecord]
    let userSessions: [UserSession]
    let lastUpdated: Date
}
