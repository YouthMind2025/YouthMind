import Foundation
import UIKit
import os.log

// MARK: - Notification Analytics Tracker
/// Tracks notification analytics and user interaction patterns
final class NotificationAnalyticsTracker {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "NotificationAnalyticsTracker")
    private let queue = DispatchQueue(label: "com.youthmind.analytics.notifications", qos: .background)
    
    // Analytics configuration
    private let configuration = AnalyticsConfiguration()
    private var analyticsData: [AnalyticsEvent] = []
    private var userInteractions: [UserInteraction] = []
    private var notificationMetrics: NotificationMetrics
    
    // Backend integration
    private let apiClient = AnalyticsAPIClient()
    
    // MARK: - Initialization
    
    init() {
        self.notificationMetrics = NotificationMetrics()
        loadCachedAnalyticsData()
        startAnalyticsProcessing()
    }
    
    // MARK: - Public Methods
    
    /// Track notification display
    func trackNotificationDisplay(_ notification: ToastNotification) {
        logger.debug("Tracking notification display: \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .notificationDisplay,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "notificationType": notification.type.rawValue,
                "severity": notification.severity.rawValue,
                "duration": notification.duration,
                "retryable": notification.retryable
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track notification action
    func trackNotificationAction(_ notification: ToastNotification, action: ToastAction) {
        logger.debug("Tracking notification action: \(action.type.rawValue) for \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .notificationAction,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "actionType": action.type.rawValue,
                "actionTitle": action.title,
                "actionStyle": action.style.rawValue
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track dialog display
    func trackDialogDisplay(_ dialog: AlertDialog) {
        logger.debug("Tracking dialog display: \(dialog.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .dialogDisplay,
            timestamp: Date(),
            data: [
                "dialogId": dialog.id,
                "dialogType": dialog.type.rawValue,
                "title": dialog.title ?? "",
                "message": dialog.message ?? "",
                "actionCount": dialog.actions.count
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track dialog action
    func trackDialogAction(_ dialog: AlertDialog, action: AlertAction) {
        logger.debug("Tracking dialog action: \(action.title) for \(dialog.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .dialogAction,
            timestamp: Date(),
            data: [
                "dialogId": dialog.id,
                "actionTitle": action.title,
                "actionStyle": action.style.rawValue
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track retry attempt
    func trackRetryAttempt(_ error: AppError, context: ErrorContext?) {
        logger.debug("Tracking retry attempt for error: \(error.errorCode)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .retryAttempt,
            timestamp: Date(),
            data: [
                "errorCode": error.errorCode,
                "errorSeverity": error.severity.rawValue,
                "retryable": error.isRetryable,
                "context": context?.toDictionary() ?? [:]
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track retry success
    func trackRetrySuccess(_ retryKey: String, context: ErrorContext?) {
        logger.debug("Tracking retry success: \(retryKey)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .retrySuccess,
            timestamp: Date(),
            data: [
                "retryKey": retryKey,
                "context": context?.toDictionary() ?? [:]
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track retry failure
    func trackRetryFailure(_ retryKey: String, error: Error?, reason: String?, context: ErrorContext?) {
        logger.debug("Tracking retry failure: \(retryKey)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .retryFailure,
            timestamp: Date(),
            data: [
                "retryKey": retryKey,
                "error": error?.localizedDescription ?? "",
                "reason": reason ?? "",
                "context": context?.toDictionary() ?? [:]
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track navigation start
    func trackNavigationStart(_ flow: RecoveryFlow) {
        logger.debug("Tracking navigation start: \(flow.type.rawValue)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .navigationStart,
            timestamp: Date(),
            data: [
                "flowId": flow.id,
                "flowType": flow.type.rawValue,
                "title": flow.title,
                "stepCount": flow.steps.count
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track navigation complete
    func trackNavigationComplete(_ flow: RecoveryFlow) {
        logger.debug("Tracking navigation complete: \(flow.type.rawValue)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .navigationComplete,
            timestamp: Date(),
            data: [
                "flowId": flow.id,
                "flowType": flow.type.rawValue,
                "completedSteps": flow.currentStepIndex,
                "totalSteps": flow.steps.count
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track preference change
    func trackPreferenceChange(_ oldPreferences: NotificationPreferences, newPreferences: NotificationPreferences) {
        logger.debug("Tracking preference change")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .preferenceChange,
            timestamp: Date(),
            data: [
                "oldPreferences": oldPreferences.toDictionary(),
                "newPreferences": newPreferences.toDictionary()
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track accessibility enhancement
    func trackAccessibilityEnhancement(_ notification: ToastNotification) {
        logger.debug("Tracking accessibility enhancement: \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .accessibilityEnhancement,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "notificationType": notification.type.rawValue,
                "severity": notification.severity.rawValue
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track accessibility announcement
    func trackAccessibilityAnnouncement(_ notification: ToastNotification) {
        logger.debug("Tracking accessibility announcement: \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .accessibilityAnnouncement,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "notificationType": notification.type.rawValue
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track accessibility action
    func trackAccessibilityAction(_ actionName: String) {
        logger.debug("Tracking accessibility action: \(actionName)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .accessibilityAction,
            timestamp: Date(),
            data: [
                "actionName": actionName
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track performance optimization
    func trackPerformanceOptimization(_ notification: ToastNotification) {
        logger.debug("Tracking performance optimization: \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .performanceOptimization,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "notificationType": notification.type.rawValue,
                "duration": notification.duration
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track queue optimization
    func trackQueueOptimization(_ originalCount: Int, optimizedCount: Int) {
        logger.debug("Tracking queue optimization: \(originalCount) -> \(optimizedCount)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .queueOptimization,
            timestamp: Date(),
            data: [
                "originalCount": originalCount,
                "optimizedCount": optimizedCount,
                "reduction": originalCount - optimizedCount
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track recommendation application
    func trackRecommendationApplication(_ recommendation: PerformanceRecommendation) {
        logger.debug("Tracking recommendation application: \(recommendation.title)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .recommendationApplication,
            timestamp: Date(),
            data: [
                "recommendationType": recommendation.type.rawValue,
                "priority": recommendation.priority.rawValue,
                "title": recommendation.title
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track preset save
    func trackPresetSave(_ preset: CustomizationPreset) {
        logger.debug("Tracking preset save: \(preset.name)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .presetSave,
            timestamp: Date(),
            data: [
                "presetName": preset.name,
                "presetDescription": preset.description
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track preset load
    func trackPresetLoad(_ preset: CustomizationPreset) {
        logger.debug("Tracking preset load: \(preset.name)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .presetLoad,
            timestamp: Date(),
            data: [
                "presetName": preset.name,
                "presetDescription": preset.description
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track accessibility settings change
    func trackAccessibilitySettingsChange(_ oldSettings: AccessibilitySettings, newSettings: AccessibilitySettings) {
        logger.debug("Tracking accessibility settings change")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .accessibilitySettingsChange,
            timestamp: Date(),
            data: [
                "oldSettings": oldSettings.toDictionary(),
                "newSettings": newSettings.toDictionary()
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track dialog dismissal
    func trackDialogDismissal(_ error: AppError, context: ErrorContext?) {
        logger.debug("Tracking dialog dismissal for error: \(error.errorCode)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .dialogDismissal,
            timestamp: Date(),
            data: [
                "errorCode": error.errorCode,
                "errorSeverity": error.severity.rawValue,
                "context": context?.toDictionary() ?? [:]
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Track max retry attempts reached
    func trackMaxRetryAttemptsReached(_ retryKey: String, context: ErrorContext?) {
        logger.debug("Tracking max retry attempts reached: \(retryKey)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .maxRetryAttemptsReached,
            timestamp: Date(),
            data: [
                "retryKey": retryKey,
                "context": context?.toDictionary() ?? [:]
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    /// Get analytics summary
    func getAnalyticsSummary() -> AnalyticsSummary {
        logger.debug("Getting analytics summary")
        
        let summary = AnalyticsSummary(
            totalEvents: analyticsData.count,
            notificationDisplays: getEventCount(.notificationDisplay),
            notificationActions: getEventCount(.notificationAction),
            dialogDisplays: getEventCount(.dialogDisplay),
            dialogActions: getEventCount(.dialogAction),
            retryAttempts: getEventCount(.retryAttempt),
            retrySuccesses: getEventCount(.retrySuccess),
            retryFailures: getEventCount(.retryFailure),
            navigationStarts: getEventCount(.navigationStart),
            navigationCompletes: getEventCount(.navigationComplete),
            preferenceChanges: getEventCount(.preferenceChange),
            accessibilityEnhancements: getEventCount(.accessibilityEnhancement),
            performanceOptimizations: getEventCount(.performanceOptimization),
            timestamp: Date()
        )
        
        return summary
    }
    
    // MARK: - Private Methods
    
    private func processAnalyticsEvent(_ event: AnalyticsEvent) {
        // Add to analytics data
        analyticsData.append(event)
        
        // Update metrics
        updateNotificationMetrics(event)
        
        // Cache data
        cacheAnalyticsData()
        
        // Send to backend if needed
        if shouldSendToBackend(event) {
            sendToBackend(event)
        }
        
        // Clean up old data
        cleanupOldData()
    }
    
    private func updateNotificationMetrics(_ event: AnalyticsEvent) {
        switch event.type {
        case .notificationDisplay:
            notificationMetrics.totalDisplays += 1
        case .notificationAction:
            notificationMetrics.totalActions += 1
        case .dialogDisplay:
            notificationMetrics.totalDialogDisplays += 1
        case .dialogAction:
            notificationMetrics.totalDialogActions += 1
        case .retryAttempt:
            notificationMetrics.totalRetryAttempts += 1
        case .retrySuccess:
            notificationMetrics.totalRetrySuccesses += 1
        case .retryFailure:
            notificationMetrics.totalRetryFailures += 1
        default:
            break
        }
    }
    
    private func getEventCount(_ type: AnalyticsEventType) -> Int {
        return analyticsData.filter { $0.type == type }.count
    }
    
    private func shouldSendToBackend(_ event: AnalyticsEvent) -> Bool {
        // Send high priority events immediately
        let highPriorityTypes: [AnalyticsEventType] = [
            .retryFailure,
            .maxRetryAttemptsReached,
            .performanceOptimization
        ]
        
        if highPriorityTypes.contains(event.type) {
            return true
        }
        
        // Send batch of events periodically
        return analyticsData.count % configuration.batchSize == 0
    }
    
    private func sendToBackend(_ event: AnalyticsEvent) {
        // Send event to backend
        Task {
            do {
                try await apiClient.trackNotificationAnalytics(event)
                logger.debug("Successfully sent analytics event to backend: \(event.id)")
            } catch {
                logger.error("Failed to send analytics event to backend: \(error.localizedDescription)")
            }
        }
    }
    
    private func loadCachedAnalyticsData() {
        // Load cached analytics data from storage
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let analyticsFile = documentsDirectory.appendingPathComponent("notification_analytics.json")
        
        do {
            if FileManager.default.fileExists(atPath: analyticsFile.path) {
                let data = try Data(contentsOf: analyticsFile)
                let cachedData = try JSONDecoder().decode([AnalyticsEvent].self, from: data)
                analyticsData = cachedData
                logger.info("Loaded \(cachedData.count) cached analytics events")
            }
        } catch {
            logger.error("Failed to load cached analytics data: \(error.localizedDescription)")
        }
    }
    
    private func cacheAnalyticsData() {
        // Cache analytics data to storage
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let analyticsFile = documentsDirectory.appendingPathComponent("notification_analytics.json")
        
        do {
            let data = try JSONEncoder().encode(analyticsData)
            try data.write(to: analyticsFile, options: .atomic)
        } catch {
            logger.error("Failed to cache analytics data: \(error.localizedDescription)")
        }
    }
    
    private func cleanupOldData() {
        // Remove old analytics data
        let cutoffDate = Date().addingTimeInterval(-configuration.dataRetentionPeriod)
        analyticsData.removeAll { $0.timestamp < cutoffDate }
    }
    
    private func startAnalyticsProcessing() {
        // Start periodic processing of analytics data
        Timer.scheduledTimer(withTimeInterval: 60.0, repeats: true) { [weak self] _ in
            self?.processAnalyticsData()
        }
    }
    
    private func processAnalyticsData() {
        // Process analytics data for insights
        let recentEvents = analyticsData.filter { $0.timestamp > Date().addingTimeInterval(-3600) } // Last hour
        
        // Calculate metrics
        let displayRate = calculateDisplayRate(recentEvents)
        let actionRate = calculateActionRate(recentEvents)
        let retrySuccessRate = calculateRetrySuccessRate(recentEvents)
        
        // Update metrics
        notificationMetrics.displayRate = displayRate
        notificationMetrics.actionRate = actionRate
        notificationMetrics.retrySuccessRate = retrySuccessRate
        
        logger.debug("Processed analytics data - Display rate: \(displayRate), Action rate: \(actionRate), Retry success rate: \(retrySuccessRate)")
    }
    
    private func calculateDisplayRate(_ events: [AnalyticsEvent]) -> Double {
        let displays = events.filter { $0.type == .notificationDisplay }.count
        let total = events.count
        return total > 0 ? Double(displays) / Double(total) : 0.0
    }
    
    private func calculateActionRate(_ events: [AnalyticsEvent]) -> Double {
        let displays = events.filter { $0.type == .notificationDisplay }.count
        let actions = events.filter { $0.type == .notificationAction }.count
        return displays > 0 ? Double(actions) / Double(displays) : 0.0
    }
    
    private func calculateRetrySuccessRate(_ events: [AnalyticsEvent]) -> Double {
        let retryAttempts = events.filter { $0.type == .retryAttempt }.count
        let retrySuccesses = events.filter { $0.type == .retrySuccess }.count
        return retryAttempts > 0 ? Double(retrySuccesses) / Double(retryAttempts) : 0.0
    }
}

// MARK: - Supporting Types

struct AnalyticsEvent: Codable {
    let id: String
    let type: AnalyticsEventType
    let timestamp: Date
    let data: [String: Any]
    
    enum CodingKeys: String, CodingKey {
        case id, type, timestamp, data
    }
    
    init(id: String, type: AnalyticsEventType, timestamp: Date, data: [String: Any]) {
        self.id = id
        self.type = type
        self.timestamp = timestamp
        self.data = data
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        type = try container.decode(AnalyticsEventType.self, forKey: .type)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        
        // Decode data as [String: String] and convert to [String: Any]
        let stringData = try container.decode([String: String].self, forKey: .data)
        data = stringData
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(type, forKey: .type)
        try container.encode(timestamp, forKey: .timestamp)
        
        // Convert [String: Any] to [String: String] for encoding
        let stringData = data.mapValues { "\($0)" }
        try container.encode(stringData, forKey: .data)
    }
}

enum AnalyticsEventType: String, Codable, CaseIterable {
    case notificationDisplay = "notification_display"
    case notificationAction = "notification_action"
    case dialogDisplay = "dialog_display"
    case dialogAction = "dialog_action"
    case retryAttempt = "retry_attempt"
    case retrySuccess = "retry_success"
    case retryFailure = "retry_failure"
    case navigationStart = "navigation_start"
    case navigationComplete = "navigation_complete"
    case preferenceChange = "preference_change"
    case accessibilityEnhancement = "accessibility_enhancement"
    case accessibilityAnnouncement = "accessibility_announcement"
    case accessibilityAction = "accessibility_action"
    case performanceOptimization = "performance_optimization"
    case queueOptimization = "queue_optimization"
    case recommendationApplication = "recommendation_application"
    case presetSave = "preset_save"
    case presetLoad = "preset_load"
    case accessibilitySettingsChange = "accessibility_settings_change"
    case dialogDismissal = "dialog_dismissal"
    case maxRetryAttemptsReached = "max_retry_attempts_reached"
}

struct NotificationMetrics {
    var totalDisplays: Int = 0
    var totalActions: Int = 0
    var totalDialogDisplays: Int = 0
    var totalDialogActions: Int = 0
    var totalRetryAttempts: Int = 0
    var totalRetrySuccesses: Int = 0
    var totalRetryFailures: Int = 0
    var displayRate: Double = 0.0
    var actionRate: Double = 0.0
    var retrySuccessRate: Double = 0.0
}

struct AnalyticsSummary {
    let totalEvents: Int
    let notificationDisplays: Int
    let notificationActions: Int
    let dialogDisplays: Int
    let dialogActions: Int
    let retryAttempts: Int
    let retrySuccesses: Int
    let retryFailures: Int
    let navigationStarts: Int
    let navigationCompletes: Int
    let preferenceChanges: Int
    let accessibilityEnhancements: Int
    let performanceOptimizations: Int
    let timestamp: Date
}

struct UserInteraction {
    let id: String
    let type: InteractionType
    let timestamp: Date
    let duration: TimeInterval
    let success: Bool
}

enum InteractionType {
    case tap
    case swipe
    case longPress
    case voiceOver
    case switchControl
}

struct AnalyticsConfiguration {
    let batchSize = 10
    let dataRetentionPeriod: TimeInterval = 7 * 24 * 60 * 60 // 7 days
    let processingInterval: TimeInterval = 60.0 // 1 minute
}

// MARK: - Extensions for Dictionary Conversion

extension NotificationPreferences {
    func toDictionary() -> [String: Any] {
        return [
            "showToasts": showToasts,
            "showAlerts": showAlerts,
            "showBanners": showBanners,
            "showBadges": showBadges,
            "language": language,
            "tone": tone.rawValue,
            "complexity": complexity.rawValue,
            "position": position.rawValue,
            "duration": duration,
            "sound": sound,
            "hapticFeedback": hapticFeedback,
            "autoDismiss": autoDismiss,
            "theme": theme.rawValue
        ]
    }
}

extension AccessibilitySettings {
    func toDictionary() -> [String: Any] {
        return [
            "voiceOverEnabled": voiceOverEnabled,
            "switchControlEnabled": switchControlEnabled,
            "assistiveTouchEnabled": assistiveTouchEnabled,
            "announceSeverity": announceSeverity,
            "announcePriority": announcePriority,
            "provideContext": provideContext,
            "provideActionInstructions": provideActionInstructions,
            "increaseDuration": increaseDuration,
            "highContrast": highContrast,
            "reduceMotion": reduceMotion,
            "largeText": largeText
        ]
    }
}

extension ErrorContext {
    func toDictionary() -> [String: Any] {
        var dict: [String: Any] = [:]
        
        if let sessionId = sessionId {
            dict["sessionId"] = sessionId
        }
        if let userId = userId {
            dict["userId"] = userId
        }
        if let screenName = screenName {
            dict["screenName"] = screenName
        }
        if let error = error {
            dict["error"] = error.errorCode
        }
        
        return dict
    }
}
