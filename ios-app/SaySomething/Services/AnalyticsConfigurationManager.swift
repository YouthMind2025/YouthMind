import Foundation
import os.log

// MARK: - Analytics Configuration Manager
/// Manages analytics configuration and customization options
final class AnalyticsConfigurationManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsConfigurationManager")
    private let queue = DispatchQueue(label: "com.youthmind.config.manager", qos: .background)
    
    // Configuration storage
    private var configuration: AnalyticsConfiguration
    private let configurationKey = "AnalyticsConfiguration"
    
    // MARK: - Initialization
    init() {
        self.configuration = Self.loadDefaultConfiguration()
        loadConfiguration()
    }
    
    // MARK: - Public Methods
    
    /// Load configuration from storage
    func loadConfiguration() -> AnalyticsConfiguration {
        if let data = UserDefaults.standard.data(forKey: configurationKey),
           let config = try? JSONDecoder().decode(AnalyticsConfiguration.self, from: data) {
            configuration = config
            logger.info("Loaded analytics configuration from storage")
        } else {
            configuration = Self.loadDefaultConfiguration()
            saveConfiguration(configuration)
            logger.info("Using default analytics configuration")
        }
        
        return configuration
    }
    
    /// Save configuration to storage
    func saveConfiguration(_ config: AnalyticsConfiguration) {
        configuration = config
        
        do {
            let data = try JSONEncoder().encode(config)
            UserDefaults.standard.set(data, forKey: configurationKey)
            logger.info("Saved analytics configuration to storage")
        } catch {
            logger.error("Failed to save analytics configuration: \(error.localizedDescription)")
        }
    }
    
    /// Update configuration
    func updateConfiguration(_ updates: AnalyticsConfigurationUpdates) {
        var updatedConfig = configuration
        
        if let isEnabled = updates.isEnabled {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let dataRetentionDays = updates.dataRetentionDays {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let batchSize = updates.batchSize {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let flushInterval = updates.flushInterval {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let enableCrashReporting = updates.enableCrashReporting {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let enablePerformanceTracking = updates.enablePerformanceTracking {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let enableUserBehaviorTracking = updates.enableUserBehaviorTracking {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let enableErrorTracking = updates.enableErrorTracking {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let encryptionEnabled = updates.encryptionEnabled {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: encryptionEnabled,
                anonymizeData: updatedConfig.anonymizeData
            )
        }
        
        if let anonymizeData = updates.anonymizeData {
            updatedConfig = AnalyticsConfiguration(
                isEnabled: updatedConfig.isEnabled,
                dataRetentionDays: updatedConfig.dataRetentionDays,
                batchSize: updatedConfig.batchSize,
                flushInterval: updatedConfig.flushInterval,
                enableCrashReporting: updatedConfig.enableCrashReporting,
                enablePerformanceTracking: updatedConfig.enablePerformanceTracking,
                enableUserBehaviorTracking: updatedConfig.enableUserBehaviorTracking,
                enableErrorTracking: updatedConfig.enableErrorTracking,
                encryptionEnabled: updatedConfig.encryptionEnabled,
                anonymizeData: anonymizeData
            )
        }
        
        saveConfiguration(updatedConfig)
    }
    
    /// Reset to default configuration
    func resetToDefault() {
        configuration = Self.loadDefaultConfiguration()
        saveConfiguration(configuration)
        logger.info("Reset analytics configuration to default")
    }
    
    /// Get current configuration
    func getCurrentConfiguration() -> AnalyticsConfiguration {
        return configuration
    }
    
    /// Validate configuration
    func validateConfiguration(_ config: AnalyticsConfiguration) -> ConfigurationValidationResult {
        var errors: [String] = []
        var warnings: [String] = []
        
        // Validate data retention days
        if config.dataRetentionDays < 1 {
            errors.append("Data retention days must be at least 1")
        } else if config.dataRetentionDays > 365 {
            warnings.append("Data retention days is very high (\(config.dataRetentionDays) days)")
        }
        
        // Validate batch size
        if config.batchSize < 1 {
            errors.append("Batch size must be at least 1")
        } else if config.batchSize > 1000 {
            warnings.append("Batch size is very large (\(config.batchSize))")
        }
        
        // Validate flush interval
        if config.flushInterval < 1.0 {
            errors.append("Flush interval must be at least 1 second")
        } else if config.flushInterval > 3600.0 {
            warnings.append("Flush interval is very long (\(config.flushInterval) seconds)")
        }
        
        // Validate encryption settings
        if config.encryptionEnabled && !config.anonymizeData {
            warnings.append("Encryption is enabled but data anonymization is disabled")
        }
        
        return ConfigurationValidationResult(
            isValid: errors.isEmpty,
            errors: errors,
            warnings: warnings
        )
    }
    
    /// Get configuration recommendations
    func getConfigurationRecommendations() -> [ConfigurationRecommendation] {
        var recommendations: [ConfigurationRecommendation] = []
        
        // Data retention recommendation
        if configuration.dataRetentionDays > 90 {
            recommendations.append(ConfigurationRecommendation(
                type: .dataRetention,
                title: "Consider reducing data retention",
                description: "Current retention period is \(configuration.dataRetentionDays) days. Consider reducing to 30-60 days for better privacy.",
                priority: .medium,
                currentValue: "\(configuration.dataRetentionDays) days",
                recommendedValue: "30-60 days"
            ))
        }
        
        // Batch size recommendation
        if configuration.batchSize < 10 {
            recommendations.append(ConfigurationRecommendation(
                type: .batchSize,
                title: "Consider increasing batch size",
                description: "Current batch size is \(configuration.batchSize). Consider increasing to 50-100 for better performance.",
                priority: .low,
                currentValue: "\(configuration.batchSize)",
                recommendedValue: "50-100"
            ))
        }
        
        // Flush interval recommendation
        if configuration.flushInterval > 300 {
            recommendations.append(ConfigurationRecommendation(
                type: .flushInterval,
                title: "Consider reducing flush interval",
                description: "Current flush interval is \(configuration.flushInterval) seconds. Consider reducing to 60-120 seconds for more real-time analytics.",
                priority: .low,
                currentValue: "\(configuration.flushInterval) seconds",
                recommendedValue: "60-120 seconds"
            ))
        }
        
        // Encryption recommendation
        if !configuration.encryptionEnabled {
            recommendations.append(ConfigurationRecommendation(
                type: .encryption,
                title: "Enable data encryption",
                description: "Data encryption is disabled. Consider enabling for better security.",
                priority: .high,
                currentValue: "Disabled",
                recommendedValue: "Enabled"
            ))
        }
        
        // Anonymization recommendation
        if !configuration.anonymizeData {
            recommendations.append(ConfigurationRecommendation(
                type: .anonymization,
                title: "Enable data anonymization",
                description: "Data anonymization is disabled. Consider enabling for better privacy compliance.",
                priority: .high,
                currentValue: "Disabled",
                recommendedValue: "Enabled"
            ))
        }
        
        return recommendations.sorted { $0.priority.rawValue > $1.priority.rawValue }
    }
    
    // MARK: - Private Methods
    
    private static func loadDefaultConfiguration() -> AnalyticsConfiguration {
        return AnalyticsConfiguration.default
    }
}

// MARK: - Configuration Models

/// Analytics configuration updates
struct AnalyticsConfigurationUpdates {
    let isEnabled: Bool?
    let dataRetentionDays: Int?
    let batchSize: Int?
    let flushInterval: TimeInterval?
    let enableCrashReporting: Bool?
    let enablePerformanceTracking: Bool?
    let enableUserBehaviorTracking: Bool?
    let enableErrorTracking: Bool?
    let encryptionEnabled: Bool?
    let anonymizeData: Bool?
}

/// Configuration validation result
struct ConfigurationValidationResult {
    let isValid: Bool
    let errors: [String]
    let warnings: [String]
}

/// Configuration recommendation
struct ConfigurationRecommendation {
    let type: RecommendationType
    let title: String
    let description: String
    let priority: RecommendationPriority
    let currentValue: String
    let recommendedValue: String
}

/// Recommendation types
enum RecommendationType: String, Codable {
    case dataRetention = "data_retention"
    case batchSize = "batch_size"
    case flushInterval = "flush_interval"
    case encryption = "encryption"
    case anonymization = "anonymization"
    case performance = "performance"
    case privacy = "privacy"
}

/// Recommendation priorities
enum RecommendationPriority: String, Codable, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
    
    var displayName: String {
        switch self {
        case .low: return "Low"
        case .medium: return "Medium"
        case .high: return "High"
        case .critical: return "Critical"
        }
    }
}
