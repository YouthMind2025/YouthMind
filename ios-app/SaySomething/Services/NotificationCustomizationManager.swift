import Foundation
import UIKit
import os.log
import AudioToolbox

// MARK: - Notification Customization Manager
/// Manages notification customization and personalization options
final class NotificationCustomizationManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.customization", category: "NotificationCustomizationManager")
    private let queue = DispatchQueue(label: "com.youthmind.customization.manager", qos: .userInitiated)
    
    // Customization configuration
    private let configuration = CustomizationConfiguration()
    private var userPreferences: NotificationPreferences
    private var customizationHistory: [CustomizationEvent] = []
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Initialization
    
    init() {
        self.userPreferences = loadUserPreferences()
        setupDefaultPreferences()
    }
    
    // MARK: - Public Methods
    
    /// Get current notification preferences
    func getNotificationPreferences() -> NotificationPreferences {
        return userPreferences
    }
    
    /// Update notification preferences
    func updateNotificationPreferences(_ preferences: NotificationPreferences) {
        logger.info("Updating notification preferences")
        
        queue.async { [weak self] in
            self?.processPreferenceUpdate(preferences)
        }
    }
    
    /// Get customization options for notification type
    func getCustomizationOptions(for type: NotificationType) -> [CustomizationOption] {
        logger.debug("Getting customization options for: \(type.rawValue)")
        
        return createCustomizationOptions(for: type)
    }
    
    /// Apply customization to notification
    func applyCustomization(_ customization: NotificationCustomization, to notification: inout ToastNotification) {
        logger.debug("Applying customization to notification: \(notification.id)")
        
        // Apply visual customization
        applyVisualCustomization(customization.visual, to: &notification)
        
        // Apply behavior customization
        applyBehaviorCustomization(customization.behavior, to: &notification)
        
        // Apply content customization
        applyContentCustomization(customization.content, to: &notification)
    }
    
    /// Get personalized notification content
    func getPersonalizedContent(for error: AppError, context: ErrorContext?) -> PersonalizedContent {
        logger.debug("Getting personalized content for error: \(error.errorCode)")
        
        return createPersonalizedContent(for: error, context: context)
    }
    
    /// Save customization preset
    func saveCustomizationPreset(_ preset: CustomizationPreset) {
        logger.info("Saving customization preset: \(preset.name)")
        
        queue.async { [weak self] in
            self?.processPresetSave(preset)
        }
    }
    
    /// Load customization preset
    func loadCustomizationPreset(_ presetName: String) -> CustomizationPreset? {
        logger.debug("Loading customization preset: \(presetName)")
        
        return loadPreset(named: presetName)
    }
    
    /// Get available presets
    func getAvailablePresets() -> [CustomizationPreset] {
        return loadAllPresets()
    }
    
    /// Reset to default preferences
    func resetToDefaults() {
        logger.info("Resetting notification preferences to defaults")
        
        queue.async { [weak self] in
            self?.processResetToDefaults()
        }
    }
    
    // MARK: - Private Methods
    
    private func processPreferenceUpdate(_ preferences: NotificationPreferences) {
        let oldPreferences = userPreferences
        userPreferences = preferences
        
        // Save preferences
        saveUserPreferences(preferences)
        
        // Track preference change
        analyticsTracker.trackPreferenceChange(oldPreferences, newPreferences: preferences)
        
        // Record customization event
        let event = CustomizationEvent(
            id: UUID().uuidString,
            type: .preferenceUpdate,
            timestamp: Date(),
            details: ["preferences": preferences]
        )
        customizationHistory.append(event)
        
        // Notify preference change
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .notificationPreferencesChanged,
                object: nil,
                userInfo: ["preferences": preferences]
            )
        }
    }
    
    private func processPresetSave(_ preset: CustomizationPreset) {
        // Save preset to storage
        savePreset(preset)
        
        // Track preset save
        analyticsTracker.trackPresetSave(preset)
        
        // Record customization event
        let event = CustomizationEvent(
            id: UUID().uuidString,
            type: .presetSave,
            timestamp: Date(),
            details: ["preset": preset.name]
        )
        customizationHistory.append(event)
    }
    
    private func processResetToDefaults() {
        let oldPreferences = userPreferences
        userPreferences = createDefaultPreferences()
        
        // Save default preferences
        saveUserPreferences(userPreferences)
        
        // Track reset
        analyticsTracker.trackPreferenceReset(oldPreferences)
        
        // Record customization event
        let event = CustomizationEvent(
            id: UUID().uuidString,
            type: .resetToDefaults,
            timestamp: Date(),
            details: [:]
        )
        customizationHistory.append(event)
        
        // Notify reset
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .notificationPreferencesReset,
                object: nil
            )
        }
    }
    
    // MARK: - Customization Creation Methods
    
    private func createCustomizationOptions(for type: NotificationType) -> [CustomizationOption] {
        var options: [CustomizationOption] = []
        
        switch type {
        case .toast:
            options.append(contentsOf: createToastCustomizationOptions())
        case .alert:
            options.append(contentsOf: createAlertCustomizationOptions())
        case .banner:
            options.append(contentsOf: createBannerCustomizationOptions())
        case .badge:
            options.append(contentsOf: createBadgeCustomizationOptions())
        }
        
        return options
    }
    
    private func createToastCustomizationOptions() -> [CustomizationOption] {
        return [
            CustomizationOption(
                id: "toast_position",
                title: "Position",
                description: "Choose where toasts appear on screen",
                type: .selection,
                values: ["top", "bottom", "center"],
                defaultValue: "top"
            ),
            CustomizationOption(
                id: "toast_duration",
                title: "Duration",
                description: "How long toasts stay visible",
                type: .slider,
                values: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"],
                defaultValue: "3"
            ),
            CustomizationOption(
                id: "toast_animation",
                title: "Animation",
                description: "Choose toast animation style",
                type: .selection,
                values: ["slide", "fade", "bounce", "none"],
                defaultValue: "slide"
            ),
            CustomizationOption(
                id: "toast_sound",
                title: "Sound",
                description: "Play sound when toast appears",
                type: .toggle,
                values: ["true", "false"],
                defaultValue: "true"
            ),
            CustomizationOption(
                id: "toast_haptic",
                title: "Haptic Feedback",
                description: "Vibrate when toast appears",
                type: .toggle,
                values: ["true", "false"],
                defaultValue: "true"
            )
        ]
    }
    
    private func createAlertCustomizationOptions() -> [CustomizationOption] {
        return [
            CustomizationOption(
                id: "alert_style",
                title: "Alert Style",
                description: "Choose alert dialog style",
                type: .selection,
                values: ["alert", "actionSheet"],
                defaultValue: "alert"
            ),
            CustomizationOption(
                id: "alert_buttons",
                title: "Button Style",
                description: "Choose button appearance",
                type: .selection,
                values: ["default", "filled", "outlined"],
                defaultValue: "default"
            ),
            CustomizationOption(
                id: "alert_auto_dismiss",
                title: "Auto Dismiss",
                description: "Automatically dismiss alerts after timeout",
                type: .toggle,
                values: ["true", "false"],
                defaultValue: "false"
            ),
            CustomizationOption(
                id: "alert_timeout",
                title: "Timeout",
                description: "Auto dismiss timeout in seconds",
                type: .slider,
                values: ["5", "10", "15", "20", "30", "60"],
                defaultValue: "30"
            )
        ]
    }
    
    private func createBannerCustomizationOptions() -> [CustomizationOption] {
        return [
            CustomizationOption(
                id: "banner_size",
                title: "Size",
                description: "Choose banner size",
                type: .selection,
                values: ["small", "medium", "large"],
                defaultValue: "medium"
            ),
            CustomizationOption(
                id: "banner_color",
                title: "Color Theme",
                description: "Choose banner color theme",
                type: .selection,
                values: ["system", "light", "dark", "custom"],
                defaultValue: "system"
            ),
            CustomizationOption(
                id: "banner_icon",
                title: "Show Icon",
                description: "Display icon in banner",
                type: .toggle,
                values: ["true", "false"],
                defaultValue: "true"
            )
        ]
    }
    
    private func createBadgeCustomizationOptions() -> [CustomizationOption] {
        return [
            CustomizationOption(
                id: "badge_color",
                title: "Badge Color",
                description: "Choose badge color",
                type: .selection,
                values: ["red", "orange", "yellow", "green", "blue", "purple"],
                defaultValue: "red"
            ),
            CustomizationOption(
                id: "badge_position",
                title: "Position",
                description: "Choose badge position",
                type: .selection,
                values: ["topRight", "topLeft", "bottomRight", "bottomLeft"],
                defaultValue: "topRight"
            ),
            CustomizationOption(
                id: "badge_animation",
                title: "Animation",
                description: "Choose badge animation",
                type: .selection,
                values: ["pulse", "bounce", "shake", "none"],
                defaultValue: "pulse"
            )
        ]
    }
    
    // MARK: - Customization Application Methods
    
    private func applyVisualCustomization(_ visual: VisualCustomization, to notification: inout ToastNotification) {
        // Apply position customization
        if let position = visual.position {
            // Position will be applied when displaying the notification
        }
        
        // Apply color customization
        if let color = visual.color {
            // Color will be applied when creating the toast view
        }
        
        // Apply size customization
        if let size = visual.size {
            // Size will be applied when creating the toast view
        }
        
        // Apply animation customization
        if let animation = visual.animation {
            // Animation will be applied when displaying the notification
        }
    }
    
    private func applyBehaviorCustomization(_ behavior: BehaviorCustomization, to notification: inout ToastNotification) {
        // Apply duration customization
        if let duration = behavior.duration {
            notification = ToastNotification(
                id: notification.id,
                type: notification.type,
                title: notification.title,
                message: notification.message,
                duration: duration,
                severity: notification.severity,
                context: notification.context,
                timestamp: notification.timestamp,
                retryable: notification.retryable,
                actions: notification.actions
            )
        }
        
        // Apply auto-dismiss customization
        if behavior.autoDismiss {
            // Set auto-dismiss flag in UserDefaults
            UserDefaults.standard.set(true, forKey: "AutoDismissEnabled")
        }
        
        // Apply sound customization
        if behavior.playSound {
            // Play system sound for notification
            AudioServicesPlaySystemSound(1007) // Default notification sound
        }
        
        // Apply haptic customization
        if behavior.hapticFeedback {
            // Trigger haptic feedback
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
        }
    }
    
    private func applyContentCustomization(_ content: ContentCustomization, to notification: inout ToastNotification) {
        // Apply title customization
        if let title = content.title {
            notification = ToastNotification(
                id: notification.id,
                type: notification.type,
                title: title,
                message: notification.message,
                duration: notification.duration,
                severity: notification.severity,
                context: notification.context,
                timestamp: notification.timestamp,
                retryable: notification.retryable,
                actions: notification.actions
            )
        }
        
        // Apply message customization
        if let message = content.message {
            notification = ToastNotification(
                id: notification.id,
                type: notification.type,
                title: notification.title,
                message: message,
                duration: notification.duration,
                severity: notification.severity,
                context: notification.context,
                timestamp: notification.timestamp,
                retryable: notification.retryable,
                actions: notification.actions
            )
        }
        
        // Apply language customization
        if let language = content.language {
            // Language will be applied when localizing the content
        }
    }
    
    // MARK: - Personalized Content Creation
    
    private func createPersonalizedContent(for error: AppError, context: ErrorContext?) -> PersonalizedContent {
        let userLevel = getUserExperienceLevel()
        let errorHistory = getErrorHistory(for: error)
        let userPreferences = getNotificationPreferences()
        
        return PersonalizedContent(
            title: createPersonalizedTitle(for: error, userLevel: userLevel),
            message: createPersonalizedMessage(for: error, userLevel: userLevel, errorHistory: errorHistory),
            suggestions: createPersonalizedSuggestions(for: error, userLevel: userLevel),
            tone: getPersonalizedTone(for: error, userLevel: userLevel),
            complexity: getPersonalizedComplexity(for: error, userLevel: userLevel),
            language: userPreferences.language,
            context: context
        )
    }
    
    private func createPersonalizedTitle(for error: AppError, userLevel: UserExperienceLevel) -> String {
        switch userLevel {
        case .beginner:
            return "Oops! Something went wrong"
        case .intermediate:
            return "Error: \(error.errorCode)"
        case .advanced:
            return "\(error.errorCode): \(error.errorDescription ?? "Unknown error")"
        case .expert:
            return "\(error.errorCode) - \(error.failureReason ?? "System error")"
        }
    }
    
    private func createPersonalizedMessage(for error: AppError, userLevel: UserExperienceLevel, errorHistory: [ErrorRecord]) -> String {
        let baseMessage = error.errorDescription ?? "An unexpected error occurred"
        
        switch userLevel {
        case .beginner:
            return "Don't worry! This happens sometimes. \(baseMessage)"
        case .intermediate:
            return baseMessage
        case .advanced:
            let context = errorHistory.count > 0 ? " (occurred \(errorHistory.count) times)" : ""
            return "\(baseMessage)\(context)"
        case .expert:
            let technicalDetails = getTechnicalDetails(for: error)
            return "\(baseMessage)\n\nTechnical details: \(technicalDetails)"
        }
    }
    
    private func createPersonalizedSuggestions(for error: AppError, userLevel: UserExperienceLevel) -> [String] {
        let baseSuggestions = getBaseSuggestions(for: error)
        
        switch userLevel {
        case .beginner:
            return baseSuggestions.map { "• \($0)" }
        case .intermediate:
            return baseSuggestions
        case .advanced:
            return baseSuggestions + getAdvancedSuggestions(for: error)
        case .expert:
            return getAdvancedSuggestions(for: error) + getExpertSuggestions(for: error)
        }
    }
    
    private func getPersonalizedTone(for error: AppError, userLevel: UserExperienceLevel) -> NotificationTone {
        switch userLevel {
        case .beginner:
            return .friendly
        case .intermediate:
            return .professional
        case .advanced:
            return .technical
        case .expert:
            return .detailed
        }
    }
    
    private func getPersonalizedComplexity(for error: AppError, userLevel: UserExperienceLevel) -> NotificationComplexity {
        switch userLevel {
        case .beginner:
            return .simple
        case .intermediate:
            return .moderate
        case .advanced:
            return .detailed
        case .expert:
            return .comprehensive
        }
    }
    
    // MARK: - Helper Methods
    
    private func loadUserPreferences() -> NotificationPreferences {
        // Load from UserDefaults
        if let data = UserDefaults.standard.data(forKey: "NotificationPreferences"),
           let preferences = try? JSONDecoder().decode(NotificationPreferences.self, from: data) {
            return preferences
        }
        
        return createDefaultPreferences()
    }
    
    private func saveUserPreferences(_ preferences: NotificationPreferences) {
        if let data = try? JSONEncoder().encode(preferences) {
            UserDefaults.standard.set(data, forKey: "NotificationPreferences")
        }
    }
    
    private func createDefaultPreferences() -> NotificationPreferences {
        return NotificationPreferences(
            showToasts: true,
            showAlerts: true,
            showBanners: false,
            showBadges: true,
            language: Locale.current.languageCode ?? "en",
            tone: .professional,
            complexity: .moderate,
            position: .top,
            duration: 3.0,
            sound: true,
            hapticFeedback: true,
            autoDismiss: false,
            theme: .system
        )
    }
    
    private func setupDefaultPreferences() {
        // Set up default preferences if not already set
        if UserDefaults.standard.object(forKey: "NotificationPreferences") == nil {
            saveUserPreferences(userPreferences)
        }
    }
    
    private func getUserExperienceLevel() -> UserExperienceLevel {
        // Determine user experience level based on app usage
        let daysSinceFirstLaunch = UserDefaults.standard.double(forKey: "DaysSinceFirstLaunch")
        let errorCount = UserDefaults.standard.integer(forKey: "TotalErrorCount")
        let featureUsage = UserDefaults.standard.integer(forKey: "FeatureUsageCount")
        
        if daysSinceFirstLaunch < 7 || errorCount < 5 {
            return .beginner
        } else if daysSinceFirstLaunch < 30 || errorCount < 20 {
            return .intermediate
        } else if daysSinceFirstLaunch < 90 || errorCount < 50 {
            return .advanced
        } else {
            return .expert
        }
    }
    
    private func getErrorHistory(for error: AppError) -> [ErrorRecord] {
        // Load error history from storage
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let errorHistoryFile = documentsDirectory.appendingPathComponent("error_history.json")
        
        do {
            if FileManager.default.fileExists(atPath: errorHistoryFile.path) {
                let data = try Data(contentsOf: errorHistoryFile)
                let errorHistory = try JSONDecoder().decode([ErrorRecord].self, from: data)
                return errorHistory.filter { $0.errorCode == error.errorCode }
            }
        } catch {
            logger.error("Failed to load error history: \(error.localizedDescription)")
        }
        
        return []
    }
    
    private func getBaseSuggestions(for error: AppError) -> [String] {
        switch error {
        case .network:
            return ["Check your internet connection", "Try again in a moment"]
        case .authentication:
            return ["Check your login details", "Try logging in again"]
        case .dataValidation:
            return ["Check your input", "Make sure all fields are filled"]
        default:
            return ["Try again", "Contact support if the problem persists"]
        }
    }
    
    private func getAdvancedSuggestions(for error: AppError) -> [String] {
        switch error {
        case .network:
            return ["Check network configuration", "Try different network", "Check firewall settings"]
        case .authentication:
            return ["Clear authentication cache", "Reset password", "Check account status"]
        case .dataValidation:
            return ["Review validation rules", "Check data format", "Verify field requirements"]
        default:
            return ["Check system logs", "Verify configuration", "Test with different data"]
        }
    }
    
    private func getExpertSuggestions(for error: AppError) -> [String] {
        switch error {
        case .network:
            return ["Check DNS settings", "Verify proxy configuration", "Test network connectivity", "Review network logs"]
        case .authentication:
            return ["Check token expiration", "Verify OAuth flow", "Review authentication logs", "Test with different credentials"]
        case .dataValidation:
            return ["Review validation schema", "Check data transformation", "Verify business rules", "Test edge cases"]
        default:
            return ["Review system architecture", "Check component dependencies", "Verify error handling", "Test failure scenarios"]
        }
    }
    
    private func getTechnicalDetails(for error: AppError) -> String {
        switch error {
        case .network(let apiError):
            return "API Error: \(apiError.localizedDescription)"
        case .business(let code, let message, let details):
            return "Business Error \(code): \(message ?? "No message")"
        default:
            return "Error Code: \(error.errorCode)"
        }
    }
    
    private func savePreset(_ preset: CustomizationPreset) {
        // Save preset to storage
        let key = "CustomizationPreset_\(preset.name)"
        if let data = try? JSONEncoder().encode(preset) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
    
    private func loadPreset(named name: String) -> CustomizationPreset? {
        let key = "CustomizationPreset_\(name)"
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(CustomizationPreset.self, from: data)
    }
    
    private func loadAllPresets() -> [CustomizationPreset] {
        var presets: [CustomizationPreset] = []
        
        // Load default presets
        presets.append(createDefaultPreset())
        presets.append(createMinimalPreset())
        presets.append(createDetailedPreset())
        
        // Load user-created presets
        let userDefaults = UserDefaults.standard
        for key in userDefaults.dictionaryRepresentation().keys {
            if key.hasPrefix("CustomizationPreset_") {
                let presetName = String(key.dropFirst("CustomizationPreset_".count))
                if let preset = loadPreset(named: presetName) {
                    presets.append(preset)
                }
            }
        }
        
        return presets
    }
    
    private func createDefaultPreset() -> CustomizationPreset {
        return CustomizationPreset(
            name: "Default",
            description: "Standard notification settings",
            preferences: createDefaultPreferences(),
            timestamp: Date()
        )
    }
    
    private func createMinimalPreset() -> CustomizationPreset {
        var preferences = createDefaultPreferences()
        preferences.showToasts = true
        preferences.showAlerts = false
        preferences.showBanners = false
        preferences.showBadges = false
        preferences.duration = 2.0
        preferences.sound = false
        preferences.hapticFeedback = false
        
        return CustomizationPreset(
            name: "Minimal",
            description: "Minimal notifications with reduced distractions",
            preferences: preferences,
            timestamp: Date()
        )
    }
    
    private func createDetailedPreset() -> CustomizationPreset {
        var preferences = createDefaultPreferences()
        preferences.showToasts = true
        preferences.showAlerts = true
        preferences.showBanners = true
        preferences.showBadges = true
        preferences.duration = 5.0
        preferences.complexity = .comprehensive
        preferences.tone = .detailed
        
        return CustomizationPreset(
            name: "Detailed",
            description: "Comprehensive notifications with full details",
            preferences: preferences,
            timestamp: Date()
        )
    }
}

// MARK: - Supporting Types

struct NotificationPreferences: Codable {
    let showToasts: Bool
    let showAlerts: Bool
    let showBanners: Bool
    let showBadges: Bool
    let language: String
    let tone: NotificationTone
    let complexity: NotificationComplexity
    let position: NotificationPosition
    let duration: TimeInterval
    let sound: Bool
    let hapticFeedback: Bool
    let autoDismiss: Bool
    let theme: NotificationTheme
}

enum NotificationType: String, CaseIterable {
    case toast = "toast"
    case alert = "alert"
    case banner = "banner"
    case badge = "badge"
}

struct CustomizationOption {
    let id: String
    let title: String
    let description: String
    let type: OptionType
    let values: [String]
    let defaultValue: String
}

enum OptionType {
    case toggle
    case slider
    case selection
    case text
    case color
}

struct NotificationCustomization {
    let visual: VisualCustomization
    let behavior: BehaviorCustomization
    let content: ContentCustomization
}

struct VisualCustomization {
    let position: NotificationPosition?
    let color: UIColor?
    let size: NotificationSize?
    let animation: NotificationAnimation?
}

struct BehaviorCustomization {
    let duration: TimeInterval?
    let autoDismiss: Bool
    let playSound: Bool
    let hapticFeedback: Bool
}

struct ContentCustomization {
    let title: String?
    let message: String?
    let language: String?
}

struct PersonalizedContent {
    let title: String
    let message: String
    let suggestions: [String]
    let tone: NotificationTone
    let complexity: NotificationComplexity
    let language: String
    let context: ErrorContext?
}

enum UserExperienceLevel: Int, CaseIterable {
    case beginner = 0
    case intermediate = 1
    case advanced = 2
    case expert = 3
}

enum NotificationTone: String, CaseIterable, Codable {
    case friendly = "friendly"
    case professional = "professional"
    case technical = "technical"
    case detailed = "detailed"
}

enum NotificationComplexity: String, CaseIterable, Codable {
    case simple = "simple"
    case moderate = "moderate"
    case detailed = "detailed"
    case comprehensive = "comprehensive"
}

enum NotificationPosition: String, CaseIterable, Codable {
    case top = "top"
    case bottom = "bottom"
    case center = "center"
}

enum NotificationSize: String, CaseIterable {
    case small = "small"
    case medium = "medium"
    case large = "large"
}

enum NotificationAnimation: String, CaseIterable {
    case slide = "slide"
    case fade = "fade"
    case bounce = "bounce"
    case none = "none"
}

enum NotificationTheme: String, CaseIterable, Codable {
    case system = "system"
    case light = "light"
    case dark = "dark"
    case custom = "custom"
}

struct CustomizationPreset: Codable {
    let name: String
    let description: String
    let preferences: NotificationPreferences
    let timestamp: Date
}

struct CustomizationEvent {
    let id: String
    let type: CustomizationEventType
    let timestamp: Date
    let details: [String: Any]
}

enum CustomizationEventType {
    case preferenceUpdate
    case presetSave
    case presetLoad
    case resetToDefaults
}

struct CustomizationConfiguration {
    let maxCustomizationHistory = 100
    let maxPresets = 20
    let defaultTimeout: TimeInterval = 30.0
}

// MARK: - Supporting Types for Error History
struct ErrorRecord: Codable {
    let id: String
    let errorCode: String
    let timestamp: Date
    let context: [String: Any]
    let resolved: Bool
    
    enum CodingKeys: String, CodingKey {
        case id, errorCode, timestamp, context, resolved
    }
    
    init(id: String, errorCode: String, timestamp: Date, context: [String: Any], resolved: Bool) {
        self.id = id
        self.errorCode = errorCode
        self.timestamp = timestamp
        self.context = context
        self.resolved = resolved
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        errorCode = try container.decode(String.self, forKey: .errorCode)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        resolved = try container.decode(Bool.self, forKey: .resolved)
        
        // Decode context as [String: String] and convert to [String: Any]
        let stringContext = try container.decode([String: String].self, forKey: .context)
        context = stringContext
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(errorCode, forKey: .errorCode)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(resolved, forKey: .resolved)
        
        // Convert [String: Any] to [String: String] for encoding
        let stringContext = context.mapValues { "\($0)" }
        try container.encode(stringContext, forKey: .context)
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let notificationPreferencesChanged = Notification.Name("notificationPreferencesChanged")
    static let notificationPreferencesReset = Notification.Name("notificationPreferencesReset")
}
