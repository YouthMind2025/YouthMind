import Foundation
import UIKit
import os.log

// MARK: - Notification Accessibility Service
/// Provides comprehensive accessibility support for notifications
final class NotificationAccessibilityService {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.accessibility", category: "NotificationAccessibilityService")
    private let queue = DispatchQueue(label: "com.youthmind.accessibility.service", qos: .userInitiated)
    
    // Accessibility configuration
    private let configuration = AccessibilityConfiguration()
    private var accessibilitySettings: AccessibilitySettings
    private var accessibilityHistory: [AccessibilityEvent] = []
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Initialization
    
    init() {
        self.accessibilitySettings = loadAccessibilitySettings()
        setupAccessibilityObservers()
    }
    
    // MARK: - Public Methods
    
    /// Get current accessibility settings
    func getAccessibilitySettings() -> AccessibilitySettings {
        return accessibilitySettings
    }
    
    /// Update accessibility settings
    func updateAccessibilitySettings(_ settings: AccessibilitySettings) {
        logger.info("Updating accessibility settings")
        
        queue.async { [weak self] in
            self?.processAccessibilityUpdate(settings)
        }
    }
    
    /// Make notification accessible
    func makeNotificationAccessible(_ notification: inout ToastNotification) -> ToastNotification {
        logger.debug("Making notification accessible: \(notification.id)")
        
        // Apply accessibility enhancements
        let accessibleNotification = applyAccessibilityEnhancements(notification)
        
        // Track accessibility enhancement
        analyticsTracker.trackAccessibilityEnhancement(notification)
        
        return accessibleNotification
    }
    
    /// Announce notification to screen reader
    func announceNotification(_ notification: ToastNotification) {
        logger.debug("Announcing notification to screen reader: \(notification.id)")
        
        guard accessibilitySettings.voiceOverEnabled else { return }
        
        let announcement = createAccessibilityAnnouncement(notification)
        UIAccessibility.post(notification: .announcement, argument: announcement)
        
        // Track announcement
        analyticsTracker.trackAccessibilityAnnouncement(notification)
    }
    
    /// Create accessible view for notification
    func createAccessibleView(for notification: ToastNotification) -> UIView {
        logger.debug("Creating accessible view for notification: \(notification.id)")
        
        let view = createBaseAccessibleView(for: notification)
        configureAccessibilityTraits(view, for: notification)
        configureAccessibilityLabels(view, for: notification)
        configureAccessibilityActions(view, for: notification)
        
        return view
    }
    
    /// Check if accessibility features are enabled
    func isAccessibilityEnabled() -> Bool {
        return UIAccessibility.isVoiceOverRunning || 
               UIAccessibility.isSwitchControlRunning || 
               UIAccessibility.isAssistiveTouchRunning
    }
    
    /// Get accessibility requirements for notification type
    func getAccessibilityRequirements(for type: NotificationType) -> [AccessibilityRequirement] {
        logger.debug("Getting accessibility requirements for: \(type.rawValue)")
        
        return createAccessibilityRequirements(for: type)
    }
    
    /// Validate notification accessibility
    func validateNotificationAccessibility(_ notification: ToastNotification) -> AccessibilityValidationResult {
        logger.debug("Validating notification accessibility: \(notification.id)")
        
        let requirements = getAccessibilityRequirements(for: .toast)
        var violations: [AccessibilityViolation] = []
        var warnings: [AccessibilityWarning] = []
        
        // Check each requirement
        for requirement in requirements {
            let result = validateRequirement(requirement, for: notification)
            violations.append(contentsOf: result.violations)
            warnings.append(contentsOf: result.warnings)
        }
        
        let isValid = violations.isEmpty
        let score = calculateAccessibilityScore(violations: violations, warnings: warnings)
        
        return AccessibilityValidationResult(
            isValid: isValid,
            score: score,
            violations: violations,
            warnings: warnings,
            recommendations: generateRecommendations(violations: violations, warnings: warnings)
        )
    }
    
    /// Get accessibility help for notification
    func getAccessibilityHelp(for notification: ToastNotification) -> AccessibilityHelp {
        logger.debug("Getting accessibility help for notification: \(notification.id)")
        
        return createAccessibilityHelp(for: notification)
    }
    
    // MARK: - Private Methods
    
    private func processAccessibilityUpdate(_ settings: AccessibilitySettings) {
        let oldSettings = accessibilitySettings
        accessibilitySettings = settings
        
        // Save settings
        saveAccessibilitySettings(settings)
        
        // Track settings change
        analyticsTracker.trackAccessibilitySettingsChange(oldSettings, newSettings: settings)
        
        // Record accessibility event
        let event = AccessibilityEvent(
            id: UUID().uuidString,
            type: .settingsUpdate,
            timestamp: Date(),
            details: ["settings": settings]
        )
        accessibilityHistory.append(event)
        
        // Notify settings change
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .accessibilitySettingsChanged,
                object: nil,
                userInfo: ["settings": settings]
            )
        }
    }
    
    private func applyAccessibilityEnhancements(_ notification: ToastNotification) -> ToastNotification {
        // Create enhanced notification with accessibility features
        let enhancedNotification = ToastNotification(
            id: notification.id,
            type: notification.type,
            title: enhanceTitleForAccessibility(notification.title),
            message: enhanceMessageForAccessibility(notification.message),
            duration: adjustDurationForAccessibility(notification.duration),
            severity: notification.severity,
            context: notification.context,
            timestamp: notification.timestamp,
            retryable: notification.retryable,
            actions: enhanceActionsForAccessibility(notification.actions)
        )
        
        return enhancedNotification
    }
    
    private func enhanceTitleForAccessibility(_ title: String) -> String {
        var enhancedTitle = title
        
        // Add severity indicator for screen readers
        if accessibilitySettings.announceSeverity {
            enhancedTitle = "\(title) - Error notification"
        }
        
        // Add priority indicator
        if accessibilitySettings.announcePriority {
            enhancedTitle = "High priority: \(enhancedTitle)"
        }
        
        return enhancedTitle
    }
    
    private func enhanceMessageForAccessibility(_ message: String) -> String {
        var enhancedMessage = message
        
        // Add context for screen readers
        if accessibilitySettings.provideContext {
            enhancedMessage = "\(enhancedMessage). This is an error message that requires your attention."
        }
        
        // Add action instructions
        if accessibilitySettings.provideActionInstructions {
            enhancedMessage = "\(enhancedMessage). Swipe right to access actions or double-tap to dismiss."
        }
        
        return enhancedMessage
    }
    
    private func adjustDurationForAccessibility(_ duration: TimeInterval) -> TimeInterval {
        // Increase duration for users who need more time
        if accessibilitySettings.increaseDuration {
            return duration * 1.5
        }
        
        // Further increase for voice over users
        if UIAccessibility.isVoiceOverRunning {
            return duration * 2.0
        }
        
        return duration
    }
    
    private func enhanceActionsForAccessibility(_ actions: [ToastAction]) -> [ToastAction] {
        return actions.map { action in
            ToastAction(
                title: enhanceActionTitleForAccessibility(action.title, type: action.type),
                type: action.type,
                style: action.style
            )
        }
    }
    
    private func enhanceActionTitleForAccessibility(_ title: String, type: ToastActionType) -> String {
        var enhancedTitle = title
        
        // Add action type context
        switch type {
        case .retry:
            enhancedTitle = "\(enhancedTitle) - Double tap to retry the failed operation"
        case .report:
            enhancedTitle = "\(enhancedTitle) - Double tap to report this error"
        case .dismiss:
            enhancedTitle = "\(enhancedTitle) - Double tap to dismiss this notification"
        }
        
        return enhancedTitle
    }
    
    private func createAccessibilityAnnouncement(_ notification: ToastNotification) -> String {
        var announcement = ""
        
        // Add title
        announcement += notification.title
        
        // Add message
        if !notification.message.isEmpty {
            announcement += ". \(notification.message)"
        }
        
        // Add severity
        if accessibilitySettings.announceSeverity {
            announcement += ". Severity: \(notification.severity.rawValue)"
        }
        
        // Add actions
        if !notification.actions.isEmpty {
            let actionTitles = notification.actions.map { $0.title }
            announcement += ". Available actions: \(actionTitles.joined(separator: ", "))"
        }
        
        return announcement
    }
    
    private func createBaseAccessibleView(for notification: ToastNotification) -> UIView {
        let view = UIView()
        view.isAccessibilityElement = true
        view.accessibilityTraits = [.staticText, .updatesFrequently]
        view.accessibilityLabel = createAccessibilityLabel(for: notification)
        view.accessibilityHint = createAccessibilityHint(for: notification)
        
        return view
    }
    
    private func configureAccessibilityTraits(_ view: UIView, for notification: ToastNotification) {
        var traits: UIAccessibilityTraits = [.staticText]
        
        // Add appropriate traits based on notification type
        switch notification.type {
        case .error:
            traits.insert(.updatesFrequently)
        case .success:
            traits.insert(.playsSound)
        case .warning:
            traits.insert(.updatesFrequently)
        case .info:
            traits.insert(.staticText)
        }
        
        // Add retryable trait
        if notification.retryable {
            traits.insert(.button)
        }
        
        view.accessibilityTraits = traits
    }
    
    private func configureAccessibilityLabels(_ view: UIView, for notification: ToastNotification) {
        view.accessibilityLabel = createAccessibilityLabel(for: notification)
        view.accessibilityValue = createAccessibilityValue(for: notification)
        view.accessibilityHint = createAccessibilityHint(for: notification)
    }
    
    private func configureAccessibilityActions(_ view: UIView, for notification: ToastNotification) {
        var actions: [UIAccessibilityCustomAction] = []
        
        for action in notification.actions {
            let customAction = UIAccessibilityCustomAction(
                name: action.title,
                target: self,
                selector: #selector(handleAccessibilityAction(_:))
            )
            actions.append(customAction)
        }
        
        view.accessibilityCustomActions = actions
    }
    
    @objc private func handleAccessibilityAction(_ action: UIAccessibilityCustomAction) -> Bool {
        logger.debug("Handling accessibility action: \(action.name)")
        
        // Track accessibility action
        analyticsTracker.trackAccessibilityAction(action.name)
        
        // Handle action based on name
        switch action.name {
        case "Retry":
            // Handle retry action
            return true
        case "Report":
            // Handle report action
            return true
        case "Dismiss":
            // Handle dismiss action
            return true
        default:
            return false
        }
    }
    
    private func createAccessibilityLabel(for notification: ToastNotification) -> String {
        var label = notification.title
        
        if !notification.message.isEmpty {
            label += ". \(notification.message)"
        }
        
        return label
    }
    
    private func createAccessibilityValue(for notification: ToastNotification) -> String {
        var value = ""
        
        // Add severity
        value += "Severity: \(notification.severity.rawValue)"
        
        // Add timestamp
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        value += ". Time: \(formatter.string(from: notification.timestamp))"
        
        return value
    }
    
    private func createAccessibilityHint(for notification: ToastNotification) -> String {
        var hint = ""
        
        if notification.retryable {
            hint += "This error can be retried. "
        }
        
        if !notification.actions.isEmpty {
            hint += "Swipe right to access actions or double-tap to dismiss."
        } else {
            hint += "Double-tap to dismiss."
        }
        
        return hint
    }
    
    // MARK: - Accessibility Requirements and Validation
    
    private func createAccessibilityRequirements(for type: NotificationType) -> [AccessibilityRequirement] {
        var requirements: [AccessibilityRequirement] = []
        
        // Common requirements
        requirements.append(AccessibilityRequirement(
            id: "label_required",
            title: "Accessibility Label",
            description: "Notification must have an accessibility label",
            type: .required,
            category: .labeling
        ))
        
        requirements.append(AccessibilityRequirement(
            id: "hint_required",
            title: "Accessibility Hint",
            description: "Notification must have an accessibility hint",
            type: .required,
            category: .labeling
        ))
        
        requirements.append(AccessibilityRequirement(
            id: "traits_required",
            title: "Accessibility Traits",
            description: "Notification must have appropriate accessibility traits",
            type: .required,
            category: .behavior
        ))
        
        // Type-specific requirements
        switch type {
        case .toast:
            requirements.append(AccessibilityRequirement(
                id: "toast_announcement",
                title: "Toast Announcement",
                description: "Toast notifications must be announced to screen readers",
                type: .required,
                category: .announcement
            ))
            
        case .alert:
            requirements.append(AccessibilityRequirement(
                id: "alert_focus",
                title: "Alert Focus",
                description: "Alert dialogs must receive focus when presented",
                type: .required,
                category: .focus
            ))
            
        case .banner:
            requirements.append(AccessibilityRequirement(
                id: "banner_contrast",
                title: "Banner Contrast",
                description: "Banner notifications must meet contrast requirements",
                type: .required,
                category: .visual
            ))
            
        case .badge:
            requirements.append(AccessibilityRequirement(
                id: "badge_announcement",
                title: "Badge Announcement",
                description: "Badge updates must be announced to screen readers",
                type: .required,
                category: .announcement
            ))
        }
        
        return requirements
    }
    
    private func validateRequirement(_ requirement: AccessibilityRequirement, for notification: ToastNotification) -> AccessibilityValidationResult {
        var violations: [AccessibilityViolation] = []
        var warnings: [AccessibilityWarning] = []
        
        switch requirement.id {
        case "label_required":
            if notification.title.isEmpty {
                violations.append(AccessibilityViolation(
                    requirement: requirement,
                    severity: .error,
                    message: "Notification title is empty"
                ))
            }
            
        case "hint_required":
            if notification.actions.isEmpty {
                warnings.append(AccessibilityWarning(
                    requirement: requirement,
                    message: "No accessibility hint provided for actions"
                ))
            }
            
        case "traits_required":
            // This would be validated when creating the view
            break
            
        case "toast_announcement":
            if !accessibilitySettings.voiceOverEnabled {
                warnings.append(AccessibilityWarning(
                    requirement: requirement,
                    message: "VoiceOver is not enabled, announcement may not work"
                ))
            }
            
        default:
            break
        }
        
        return AccessibilityValidationResult(
            isValid: violations.isEmpty,
            score: calculateAccessibilityScore(violations: violations, warnings: warnings),
            violations: violations,
            warnings: warnings,
            recommendations: []
        )
    }
    
    private func calculateAccessibilityScore(violations: [AccessibilityViolation], warnings: [AccessibilityWarning]) -> Double {
        let totalRequirements = 10.0 // Base score
        let violationPenalty = Double(violations.count) * 2.0
        let warningPenalty = Double(warnings.count) * 0.5
        
        let score = max(0.0, totalRequirements - violationPenalty - warningPenalty)
        return min(10.0, score)
    }
    
    private func generateRecommendations(violations: [AccessibilityViolation], warnings: [AccessibilityWarning]) -> [String] {
        var recommendations: [String] = []
        
        for violation in violations {
            switch violation.requirement.category {
            case .labeling:
                recommendations.append("Add clear, descriptive accessibility labels")
            case .behavior:
                recommendations.append("Ensure proper accessibility traits are set")
            case .announcement:
                recommendations.append("Implement proper screen reader announcements")
            case .focus:
                recommendations.append("Ensure proper focus management")
            case .visual:
                recommendations.append("Improve visual accessibility and contrast")
            }
        }
        
        return recommendations
    }
    
    private func createAccessibilityHelp(for notification: ToastNotification) -> AccessibilityHelp {
        return AccessibilityHelp(
            title: "Accessibility Help",
            description: "How to use this notification with accessibility features",
            instructions: createAccessibilityInstructions(for: notification),
            shortcuts: createAccessibilityShortcuts(for: notification),
            tips: createAccessibilityTips(for: notification)
        )
    }
    
    private func createAccessibilityInstructions(for notification: ToastNotification) -> [String] {
        var instructions: [String] = []
        
        if UIAccessibility.isVoiceOverRunning {
            instructions.append("VoiceOver is enabled. Swipe right or left to navigate through the notification.")
            instructions.append("Double-tap to activate buttons or dismiss the notification.")
        }
        
        if UIAccessibility.isSwitchControlRunning {
            instructions.append("Switch Control is enabled. Use your switch to navigate and activate elements.")
        }
        
        if UIAccessibility.isAssistiveTouchRunning {
            instructions.append("AssistiveTouch is enabled. Use the AssistiveTouch menu to interact with the notification.")
        }
        
        return instructions
    }
    
    private func createAccessibilityShortcuts(for notification: ToastNotification) -> [String] {
        return [
            "Double-tap: Activate primary action",
            "Swipe right: Next element",
            "Swipe left: Previous element",
            "Swipe up: Next notification",
            "Swipe down: Previous notification"
        ]
    }
    
    private func createAccessibilityTips(for notification: ToastNotification) -> [String] {
        return [
            "Enable VoiceOver for spoken feedback",
            "Use Switch Control for hands-free navigation",
            "Adjust text size in Settings > Display & Brightness",
            "Enable High Contrast for better visibility",
            "Use Reduce Motion to minimize animations"
        ]
    }
    
    // MARK: - Settings Management
    
    private func loadAccessibilitySettings() -> AccessibilitySettings {
        if let data = UserDefaults.standard.data(forKey: "AccessibilitySettings"),
           let settings = try? JSONDecoder().decode(AccessibilitySettings.self, from: data) {
            return settings
        }
        
        return createDefaultAccessibilitySettings()
    }
    
    private func saveAccessibilitySettings(_ settings: AccessibilitySettings) {
        if let data = try? JSONEncoder().encode(settings) {
            UserDefaults.standard.set(data, forKey: "AccessibilitySettings")
        }
    }
    
    private func createDefaultAccessibilitySettings() -> AccessibilitySettings {
        return AccessibilitySettings(
            voiceOverEnabled: UIAccessibility.isVoiceOverRunning,
            switchControlEnabled: UIAccessibility.isSwitchControlRunning,
            assistiveTouchEnabled: UIAccessibility.isAssistiveTouchRunning,
            announceSeverity: true,
            announcePriority: true,
            provideContext: true,
            provideActionInstructions: true,
            increaseDuration: false,
            highContrast: false,
            reduceMotion: false,
            largeText: false
        )
    }
    
    private func setupAccessibilityObservers() {
        // Observe accessibility changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityStatusChanged),
            name: UIAccessibility.voiceOverStatusDidChangeNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(accessibilityStatusChanged),
            name: UIAccessibility.switchControlStatusDidChangeNotification,
            object: nil
        )
    }
    
    @objc private func accessibilityStatusChanged() {
        logger.info("Accessibility status changed")
        
        // Update settings based on current accessibility status
        var updatedSettings = accessibilitySettings
        updatedSettings.voiceOverEnabled = UIAccessibility.isVoiceOverRunning
        updatedSettings.switchControlEnabled = UIAccessibility.isSwitchControlRunning
        updatedSettings.assistiveTouchEnabled = UIAccessibility.isAssistiveTouchRunning
        
        updateAccessibilitySettings(updatedSettings)
    }
}

// MARK: - Supporting Types

struct AccessibilitySettings: Codable {
    let voiceOverEnabled: Bool
    let switchControlEnabled: Bool
    let assistiveTouchEnabled: Bool
    let announceSeverity: Bool
    let announcePriority: Bool
    let provideContext: Bool
    let provideActionInstructions: Bool
    let increaseDuration: Bool
    let highContrast: Bool
    let reduceMotion: Bool
    let largeText: Bool
}

struct AccessibilityRequirement {
    let id: String
    let title: String
    let description: String
    let type: RequirementType
    let category: RequirementCategory
}

enum RequirementType {
    case required
    case recommended
    case optional
}

enum RequirementCategory {
    case labeling
    case behavior
    case announcement
    case focus
    case visual
}

struct AccessibilityViolation {
    let requirement: AccessibilityRequirement
    let severity: ViolationSeverity
    let message: String
}

enum ViolationSeverity {
    case error
    case warning
    case info
}

struct AccessibilityWarning {
    let requirement: AccessibilityRequirement
    let message: String
}

struct AccessibilityValidationResult {
    let isValid: Bool
    let score: Double
    let violations: [AccessibilityViolation]
    let warnings: [AccessibilityWarning]
    let recommendations: [String]
}

struct AccessibilityHelp {
    let title: String
    let description: String
    let instructions: [String]
    let shortcuts: [String]
    let tips: [String]
}

struct AccessibilityEvent {
    let id: String
    let type: AccessibilityEventType
    let timestamp: Date
    let details: [String: Any]
}

enum AccessibilityEventType {
    case settingsUpdate
    case announcement
    case action
    case validation
}

struct AccessibilityConfiguration {
    let maxAccessibilityHistory = 100
    let defaultTimeout: TimeInterval = 30.0
    let announcementDelay: TimeInterval = 0.5
}

// MARK: - Notification Names
extension Notification.Name {
    static let accessibilitySettingsChanged = Notification.Name("accessibilitySettingsChanged")
}
