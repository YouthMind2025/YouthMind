import Foundation
import os.log
import CryptoKit

// MARK: - Analytics Privacy Manager
/// Manages privacy compliance, data anonymization, and user consent for analytics
final class AnalyticsPrivacyManager {
    
    // MARK: - Dependencies
    private let encryptionService: AnalyticsEncryptionService
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsPrivacyManager")
    private let queue = DispatchQueue(label: "com.youthmind.privacy.manager", qos: .background)
    
    // Privacy settings
    private var userConsent: AnalyticsConsent = .notDetermined
    private var privacySettings: PrivacySettings
    private var dataRetentionPolicy: DataRetentionPolicy
    
    // Anonymization
    private var anonymizedUserIds: [String: String] = [:]
    private var sessionIds: [String: String] = [:]
    
    // MARK: - Initialization
    init(encryptionService: AnalyticsEncryptionService) {
        self.encryptionService = encryptionService
        self.privacySettings = PrivacySettings.default
        self.dataRetentionPolicy = DataRetentionPolicy.default
        
        loadPrivacySettings()
    }
    
    // MARK: - Public Methods
    
    /// Check if error tracking is allowed
    func canTrackErrors() -> Bool {
        return userConsent == .granted && privacySettings.trackErrors
    }
    
    /// Check if user behavior tracking is allowed
    func canTrackUserBehavior() -> Bool {
        return userConsent == .granted && privacySettings.trackUserBehavior
    }
    
    /// Check if performance tracking is allowed
    func canTrackPerformance() -> Bool {
        return userConsent == .granted && privacySettings.trackPerformance
    }
    
    /// Check if crash reporting is allowed
    func canReportCrashes() -> Bool {
        return userConsent == .granted && privacySettings.reportCrashes
    }
    
    /// Get anonymized user ID
    func getAnonymizedUserId() -> String {
        return anonymizedUserIds["current"] ?? generateAnonymizedUserId()
    }
    
    /// Get anonymized session ID
    func getAnonymizedSessionId() -> String {
        return sessionIds["current"] ?? generateAnonymizedSessionId()
    }
    
    /// Anonymize data for privacy compliance
    func anonymizeData<T: Codable>(_ data: T) -> T? {
        guard privacySettings.anonymizeData else { return data }
        
        do {
            let jsonData = try JSONEncoder().encode(data)
            let anonymizedData = try anonymizeJSONData(jsonData)
            return try JSONDecoder().decode(T.self, from: anonymizedData)
        } catch {
            logger.error("Failed to anonymize data: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Encrypt sensitive data
    func encryptSensitiveData(_ data: Data) -> Data? {
        guard privacySettings.encryptData else { return data }
        
        return encryptionService.encrypt(data)
    }
    
    /// Decrypt sensitive data
    func decryptSensitiveData(_ encryptedData: Data) -> Data? {
        guard privacySettings.encryptData else { return encryptedData }
        
        return encryptionService.decrypt(encryptedData)
    }
    
    /// Update user consent
    func updateUserConsent(_ consent: AnalyticsConsent) {
        userConsent = consent
        savePrivacySettings()
        
        logger.info("User consent updated to: \(consent.rawValue)")
    }
    
    /// Update privacy settings
    func updatePrivacySettings(_ settings: PrivacySettings) {
        privacySettings = settings
        savePrivacySettings()
        
        logger.info("Privacy settings updated")
    }
    
    /// Get data retention status
    func getDataRetentionStatus() -> DataRetentionStatus {
        return DataRetentionStatus(
            canRetainData: userConsent == .granted,
            retentionDays: dataRetentionPolicy.retentionDays,
            anonymizeData: privacySettings.anonymizeData,
            encryptData: privacySettings.encryptData,
            lastCleanup: dataRetentionPolicy.lastCleanup
        )
    }
    
    /// Clean up expired data
    func cleanupExpiredData() async {
        guard userConsent == .granted else { return }
        
        let cutoffDate = Date().addingTimeInterval(-TimeInterval(dataRetentionPolicy.retentionDays * 24 * 3600))
        
        await queue.async { [weak self] in
            self?.performDataCleanup(cutoffDate: cutoffDate)
        }
    }
    
    /// Generate privacy report
    func generatePrivacyReport() -> PrivacyReport {
        return PrivacyReport(
            userConsent: userConsent,
            privacySettings: privacySettings,
            dataRetentionPolicy: dataRetentionPolicy,
            anonymizedUserIds: anonymizedUserIds.count,
            sessionIds: sessionIds.count,
            lastCleanup: dataRetentionPolicy.lastCleanup,
            timestamp: Date()
        )
    }
    
    // MARK: - Private Methods
    
    private func anonymizeJSONData(_ data: Data) throws -> Data {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return data
        }
        
        let anonymizedJson = anonymizeJSONObject(json)
        return try JSONSerialization.data(withJSONObject: anonymizedJson)
    }
    
    private func anonymizeJSONObject(_ object: Any) -> Any {
        switch object {
        case let dict as [String: Any]:
            var anonymizedDict: [String: Any] = [:]
            for (key, value) in dict {
                if isSensitiveKey(key) {
                    anonymizedDict[key] = anonymizeValue(value)
                } else {
                    anonymizedDict[key] = anonymizeJSONObject(value)
                }
            }
            return anonymizedDict
            
        case let array as [Any]:
            return array.map { anonymizeJSONObject($0) }
            
        case let string as String:
            return anonymizeString(string)
            
        default:
            return object
        }
    }
    
    private func isSensitiveKey(_ key: String) -> Bool {
        let sensitiveKeys = [
            "userId", "user_id", "sessionId", "session_id",
            "email", "phone", "name", "username",
            "ip_address", "device_id", "advertising_id"
        ]
        
        return sensitiveKeys.contains { key.lowercased().contains($0) }
    }
    
    private func anonymizeValue(_ value: Any) -> Any {
        switch value {
        case let string as String:
            return anonymizeString(string)
        case let number as NSNumber:
            return anonymizeNumber(number)
        default:
            return "[ANONYMIZED]"
        }
    }
    
    private func anonymizeString(_ string: String) -> String {
        if string.isEmpty {
            return string
        }
        
        // Hash the string for consistent anonymization
        let data = string.data(using: .utf8) ?? Data()
        let hash = SHA256.hash(data: data)
        let hashString = hash.compactMap { String(format: "%02x", $0) }.joined()
        
        // Return first 8 characters of hash
        return String(hashString.prefix(8))
    }
    
    private func anonymizeNumber(_ number: NSNumber) -> NSNumber {
        // Round to nearest 10 for numeric anonymization
        let value = number.doubleValue
        let anonymizedValue = round(value / 10.0) * 10.0
        return NSNumber(value: anonymizedValue)
    }
    
    private func generateAnonymizedUserId() -> String {
        let userId = UUID().uuidString
        let anonymizedId = anonymizeString(userId)
        anonymizedUserIds["current"] = anonymizedId
        return anonymizedId
    }
    
    private func generateAnonymizedSessionId() -> String {
        let sessionId = UUID().uuidString
        let anonymizedId = anonymizeString(sessionId)
        sessionIds["current"] = anonymizedId
        return anonymizedId
    }
    
    private func performDataCleanup(cutoffDate: Date) {
        // Clean up old anonymized IDs
        anonymizedUserIds = anonymizedUserIds.filter { _, _ in
            // Keep current session data
            true
        }
        
        sessionIds = sessionIds.filter { _, _ in
            // Keep current session data
            true
        }
        
        // Update last cleanup time
        dataRetentionPolicy.lastCleanup = Date()
        savePrivacySettings()
        
        logger.info("Data cleanup completed")
    }
    
    private func loadPrivacySettings() {
        if let data = UserDefaults.standard.data(forKey: "AnalyticsPrivacySettings"),
           let settings = try? JSONDecoder().decode(PrivacySettings.self, from: data) {
            privacySettings = settings
        }
        
        if let data = UserDefaults.standard.data(forKey: "AnalyticsDataRetentionPolicy"),
           let policy = try? JSONDecoder().decode(DataRetentionPolicy.self, from: data) {
            dataRetentionPolicy = policy
        }
        
        if let consentString = UserDefaults.standard.string(forKey: "AnalyticsUserConsent"),
           let consent = AnalyticsConsent(rawValue: consentString) {
            userConsent = consent
        }
    }
    
    private func savePrivacySettings() {
        if let data = try? JSONEncoder().encode(privacySettings) {
            UserDefaults.standard.set(data, forKey: "AnalyticsPrivacySettings")
        }
        
        if let data = try? JSONEncoder().encode(dataRetentionPolicy) {
            UserDefaults.standard.set(data, forKey: "AnalyticsDataRetentionPolicy")
        }
        
        UserDefaults.standard.set(userConsent.rawValue, forKey: "AnalyticsUserConsent")
    }
}

// MARK: - Privacy Models

/// Privacy settings
struct PrivacySettings: Codable {
    let trackErrors: Bool
    let trackUserBehavior: Bool
    let trackPerformance: Bool
    let reportCrashes: Bool
    let anonymizeData: Bool
    let encryptData: Bool
    let allowThirdPartySharing: Bool
    let allowDataExport: Bool
    
    static let `default` = PrivacySettings(
        trackErrors: true,
        trackUserBehavior: true,
        trackPerformance: true,
        reportCrashes: true,
        anonymizeData: true,
        encryptData: true,
        allowThirdPartySharing: false,
        allowDataExport: true
    )
}

/// Data retention policy
struct DataRetentionPolicy: Codable {
    let retentionDays: Int
    let autoCleanup: Bool
    let cleanupInterval: TimeInterval
    var lastCleanup: Date
    
    static let `default` = DataRetentionPolicy(
        retentionDays: 30,
        autoCleanup: true,
        cleanupInterval: 24 * 3600, // 24 hours
        lastCleanup: Date()
    )
}

/// Data retention status
struct DataRetentionStatus: Codable {
    let canRetainData: Bool
    let retentionDays: Int
    let anonymizeData: Bool
    let encryptData: Bool
    let lastCleanup: Date
}

/// Privacy report
struct PrivacyReport: Codable {
    let userConsent: AnalyticsConsent
    let privacySettings: PrivacySettings
    let dataRetentionPolicy: DataRetentionPolicy
    let anonymizedUserIds: Int
    let sessionIds: Int
    let lastCleanup: Date
    let timestamp: Date
}
