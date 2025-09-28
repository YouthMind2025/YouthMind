import Foundation
import UIKit
import os.log

// MARK: - Notification Localization Service
/// Provides comprehensive localization and multi-language support for notifications
final class NotificationLocalizationService {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.localization", category: "NotificationLocalizationService")
    private let queue = DispatchQueue(label: "com.youthmind.localization.service", qos: .userInitiated)
    
    // Localization configuration
    private let configuration = LocalizationConfiguration()
    private var supportedLanguages: [Language] = []
    private var currentLanguage: Language
    private var localizationCache: [String: LocalizedContent] = [:]
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Initialization
    
    init() {
        self.currentLanguage = detectCurrentLanguage()
        loadSupportedLanguages()
        loadLocalizationCache()
    }
    
    // MARK: - Public Methods
    
    /// Get localized content for notification
    func getLocalizedContent(for notification: ToastNotification) -> LocalizedContent {
        logger.debug("Getting localized content for notification: \(notification.id)")
        
        let cacheKey = generateCacheKey(for: notification)
        
        // Check cache first
        if let cachedContent = localizationCache[cacheKey] {
            return cachedContent
        }
        
        // Generate localized content
        let localizedContent = generateLocalizedContent(for: notification)
        
        // Cache the content
        localizationCache[cacheKey] = localizedContent
        
        return localizedContent
    }
    
    /// Get localized content for dialog
    func getLocalizedContent(for dialog: AlertDialog) -> LocalizedContent {
        logger.debug("Getting localized content for dialog: \(dialog.id)")
        
        let cacheKey = generateCacheKey(for: dialog)
        
        // Check cache first
        if let cachedContent = localizationCache[cacheKey] {
            return cachedContent
        }
        
        // Generate localized content
        let localizedContent = generateLocalizedContent(for: dialog)
        
        // Cache the content
        localizationCache[cacheKey] = localizedContent
        
        return localizedContent
    }
    
    /// Get localized error message
    func getLocalizedErrorMessage(for error: AppError) -> String {
        logger.debug("Getting localized error message for: \(error.errorCode)")
        
        let key = "error.\(error.errorCode)"
        return getLocalizedString(for: key, fallback: error.errorDescription ?? "An error occurred")
    }
    
    /// Get localized retry message
    func getLocalizedRetryMessage(for error: AppError) -> String {
        logger.debug("Getting localized retry message for: \(error.errorCode)")
        
        let key = "retry.\(error.errorCode)"
        return getLocalizedString(for: key, fallback: "Try again")
    }
    
    /// Get localized help message
    func getLocalizedHelpMessage(for error: AppError) -> String {
        logger.debug("Getting localized help message for: \(error.errorCode)")
        
        let key = "help.\(error.errorCode)"
        return getLocalizedString(for: key, fallback: "Get help with this error")
    }
    
    /// Set current language
    func setCurrentLanguage(_ language: Language) {
        logger.info("Setting current language to: \(language.code)")
        
        queue.async { [weak self] in
            self?.processLanguageChange(language)
        }
    }
    
    /// Get supported languages
    func getSupportedLanguages() -> [Language] {
        return supportedLanguages
    }
    
    /// Get current language
    func getCurrentLanguage() -> Language {
        return currentLanguage
    }
    
    /// Check if language is supported
    func isLanguageSupported(_ language: Language) -> Bool {
        return supportedLanguages.contains { $0.code == language.code }
    }
    
    /// Get localized date format
    func getLocalizedDateFormat() -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: currentLanguage.code)
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }
    
    /// Get localized number format
    func getLocalizedNumberFormat() -> NumberFormatter {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: currentLanguage.code)
        formatter.numberStyle = .decimal
        return formatter
    }
    
    /// Get localized currency format
    func getLocalizedCurrencyFormat() -> NumberFormatter {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: currentLanguage.code)
        formatter.numberStyle = .currency
        return formatter
    }
    
    /// Get localized duration format
    func getLocalizedDurationFormat(_ duration: TimeInterval) -> String {
        let formatter = DateComponentsFormatter()
        formatter.locale = Locale(identifier: currentLanguage.code)
        formatter.allowedUnits = [.hour, .minute, .second]
        formatter.unitsStyle = .abbreviated
        return formatter.string(from: duration) ?? "\(Int(duration))s"
    }
    
    /// Get localized severity text
    func getLocalizedSeverityText(_ severity: AppError.ErrorSeverity) -> String {
        let key = "severity.\(severity.rawValue)"
        return getLocalizedString(for: key, fallback: severity.rawValue.capitalized)
    }
    
    /// Get localized action text
    func getLocalizedActionText(_ action: ToastActionType) -> String {
        let key = "action.\(action.rawValue)"
        return getLocalizedString(for: key, fallback: action.rawValue.capitalized)
    }
    
    /// Get localized flow text
    func getLocalizedFlowText(_ flow: FlowType) -> String {
        let key = "flow.\(flow.rawValue)"
        return getLocalizedString(for: key, fallback: flow.rawValue.capitalized)
    }
    
    /// Get localized step text
    func getLocalizedStepText(_ step: RecoveryStep) -> String {
        let key = "step.\(step.id)"
        return getLocalizedString(for: key, fallback: step.title)
    }
    
    /// Clear localization cache
    func clearLocalizationCache() {
        logger.info("Clearing localization cache")
        
        queue.async { [weak self] in
            self?.localizationCache.removeAll()
        }
    }
    
    // MARK: - Private Methods
    
    private func processLanguageChange(_ language: Language) {
        let oldLanguage = currentLanguage
        currentLanguage = language
        
        // Clear cache for new language
        localizationCache.removeAll()
        
        // Track language change
        analyticsTracker.trackLanguageChange(oldLanguage, newLanguage: language)
        
        // Notify language change
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .languageChanged,
                object: nil,
                userInfo: ["language": language]
            )
        }
    }
    
    private func generateLocalizedContent(for notification: ToastNotification) -> LocalizedContent {
        let title = getLocalizedString(for: "notification.\(notification.type.rawValue).title", fallback: notification.title)
        let message = getLocalizedString(for: "notification.\(notification.type.rawValue).message", fallback: notification.message)
        
        let localizedActions = notification.actions.map { action in
            ToastAction(
                title: getLocalizedActionText(action.type),
                type: action.type,
                style: action.style
            )
        }
        
        return LocalizedContent(
            title: title,
            message: message,
            actions: localizedActions,
            language: currentLanguage,
            timestamp: Date()
        )
    }
    
    private func generateLocalizedContent(for dialog: AlertDialog) -> LocalizedContent {
        let title = getLocalizedString(for: "dialog.\(dialog.type.rawValue).title", fallback: dialog.title ?? "")
        let message = getLocalizedString(for: "dialog.\(dialog.type.rawValue).message", fallback: dialog.message ?? "")
        
        let localizedActions = dialog.actions.map { action in
            AlertAction(
                title: getLocalizedString(for: "dialog.action.\(action.title)", fallback: action.title),
                style: action.style,
                handler: action.handler
            )
        }
        
        return LocalizedContent(
            title: title,
            message: message,
            actions: localizedActions,
            language: currentLanguage,
            timestamp: Date()
        )
    }
    
    private func getLocalizedString(for key: String, fallback: String) -> String {
        // Try to get from localized strings
        if let localizedString = getLocalizedStringFromBundle(key) {
            return localizedString
        }
        
        // Try to get from custom localization
        if let customString = getCustomLocalizedString(key) {
            return customString
        }
        
        // Return fallback
        return fallback
    }
    
    private func getLocalizedStringFromBundle(_ key: String) -> String? {
        let bundle = Bundle.main
        let localizedString = bundle.localizedString(forKey: key, value: nil, table: "Notifications")
        
        // Check if we got a different value (meaning it was found)
        if localizedString != key {
            return localizedString
        }
        
        return nil
    }
    
    private func getCustomLocalizedString(_ key: String) -> String? {
        // Load custom localization from storage
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let localizationFile = documentsDirectory.appendingPathComponent("custom_localization_\(currentLanguage.code).json")
        
        do {
            if FileManager.default.fileExists(atPath: localizationFile.path) {
                let data = try Data(contentsOf: localizationFile)
                let customLocalization = try JSONDecoder().decode([String: String].self, from: data)
                return customLocalization[key]
            }
        } catch {
            logger.error("Failed to load custom localization: \(error.localizedDescription)")
        }
        
        return nil
    }
    
    private func generateCacheKey(for notification: ToastNotification) -> String {
        return "notification_\(notification.id)_\(currentLanguage.code)"
    }
    
    private func generateCacheKey(for dialog: AlertDialog) -> String {
        return "dialog_\(dialog.id)_\(currentLanguage.code)"
    }
    
    private func detectCurrentLanguage() -> Language {
        // Get current system language
        let systemLanguage = Locale.current.languageCode ?? "en"
        
        // Check if system language is supported
        if let supportedLanguage = supportedLanguages.first(where: { $0.code == systemLanguage }) {
            return supportedLanguage
        }
        
        // Fall back to English
        return Language(code: "en", name: "English", nativeName: "English")
    }
    
    private func loadSupportedLanguages() {
        supportedLanguages = [
            Language(code: "en", name: "English", nativeName: "English"),
            Language(code: "es", name: "Spanish", nativeName: "Español"),
            Language(code: "fr", name: "French", nativeName: "Français"),
            Language(code: "de", name: "German", nativeName: "Deutsch"),
            Language(code: "it", name: "Italian", nativeName: "Italiano"),
            Language(code: "pt", name: "Portuguese", nativeName: "Português"),
            Language(code: "ru", name: "Russian", nativeName: "Русский"),
            Language(code: "ja", name: "Japanese", nativeName: "日本語"),
            Language(code: "ko", name: "Korean", nativeName: "한국어"),
            Language(code: "zh", name: "Chinese", nativeName: "中文"),
            Language(code: "ar", name: "Arabic", nativeName: "العربية"),
            Language(code: "hi", name: "Hindi", nativeName: "हिन्दी"),
            Language(code: "th", name: "Thai", nativeName: "ไทย"),
            Language(code: "vi", name: "Vietnamese", nativeName: "Tiếng Việt"),
            Language(code: "tr", name: "Turkish", nativeName: "Türkçe"),
            Language(code: "pl", name: "Polish", nativeName: "Polski"),
            Language(code: "nl", name: "Dutch", nativeName: "Nederlands"),
            Language(code: "sv", name: "Swedish", nativeName: "Svenska"),
            Language(code: "da", name: "Danish", nativeName: "Dansk"),
            Language(code: "no", name: "Norwegian", nativeName: "Norsk")
        ]
    }
    
    private func loadLocalizationCache() {
        // Load cached localization data
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let cacheFile = documentsDirectory.appendingPathComponent("localization_cache_\(currentLanguage.code).json")
        
        do {
            if FileManager.default.fileExists(atPath: cacheFile.path) {
                let data = try Data(contentsOf: cacheFile)
                let cachedData = try JSONDecoder().decode([String: LocalizedContent].self, from: data)
                localizationCache = cachedData
                logger.info("Loaded \(cachedData.count) cached localization entries")
            }
        } catch {
            logger.error("Failed to load localization cache: \(error.localizedDescription)")
        }
    }
    
    private func saveLocalizationCache() {
        // Save localization cache
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let cacheFile = documentsDirectory.appendingPathComponent("localization_cache_\(currentLanguage.code).json")
        
        do {
            let data = try JSONEncoder().encode(localizationCache)
            try data.write(to: cacheFile, options: .atomic)
        } catch {
            logger.error("Failed to save localization cache: \(error.localizedDescription)")
        }
    }
}

// MARK: - Supporting Types

struct Language: Codable, Equatable {
    let code: String
    let name: String
    let nativeName: String
}

struct LocalizedContent: Codable {
    let title: String
    let message: String
    let actions: [LocalizedAction]
    let language: Language
    let timestamp: Date
}

struct LocalizedAction: Codable {
    let title: String
    let type: String
    let style: String
}

struct LocalizationConfiguration {
    let supportedLanguages = 20
    let cacheSize = 1000
    let cacheExpiration: TimeInterval = 24 * 60 * 60 // 24 hours
    let fallbackLanguage = "en"
}

// MARK: - Extensions for Localization

extension ToastAction {
    init(title: String, type: ToastActionType, style: ToastActionStyle) {
        self.title = title
        self.type = type
        self.style = style
    }
}

extension AlertAction {
    init(title: String, style: UIAlertAction.Style, handler: @escaping (AlertAction) -> Void) {
        self.title = title
        self.style = style
        self.handler = handler
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let languageChanged = Notification.Name("languageChanged")
}

// MARK: - Analytics Extension
extension NotificationAnalyticsTracker {
    func trackLanguageChange(_ oldLanguage: Language, newLanguage: Language) {
        logger.debug("Tracking language change: \(oldLanguage.code) -> \(newLanguage.code)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .languageChange,
            timestamp: Date(),
            data: [
                "oldLanguage": oldLanguage.code,
                "newLanguage": newLanguage.code,
                "oldLanguageName": oldLanguage.name,
                "newLanguageName": newLanguage.name
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
}

// MARK: - Additional Analytics Event Type
extension AnalyticsEventType {
    static let languageChange = AnalyticsEventType(rawValue: "language_change")!
}
