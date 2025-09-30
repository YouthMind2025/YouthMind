import Foundation
import UIKit
import os.log

// MARK: - Notification Security Validator
/// Validates notification content for security and safety
final class NotificationSecurityValidator {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.security", category: "NotificationSecurityValidator")
    private let queue = DispatchQueue(label: "com.youthmind.security.validator", qos: .userInitiated)
    
    // Security configuration
    private let configuration = SecurityConfiguration()
    private var securityRules: [SecurityRule] = []
    private var validationHistory: [ValidationEvent] = []
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Initialization
    
    init() {
        loadSecurityRules()
        setupSecurityMonitoring()
    }
    
    // MARK: - Public Methods
    
    /// Validate notification content
    func validateNotification(_ notification: ToastNotification) -> SecurityValidationResult {
        logger.debug("Validating notification security: \(notification.id)")
        
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Validate title
        let titleValidation = validateText(notification.title, type: .title)
        violations.append(contentsOf: titleValidation.violations)
        warnings.append(contentsOf: titleValidation.warnings)
        
        // Validate message
        let messageValidation = validateText(notification.message, type: .message)
        violations.append(contentsOf: messageValidation.violations)
        warnings.append(contentsOf: messageValidation.warnings)
        
        // Validate actions
        for action in notification.actions {
            let actionValidation = validateAction(action)
            violations.append(contentsOf: actionValidation.violations)
            warnings.append(contentsOf: actionValidation.warnings)
        }
        
        // Validate context
        if let context = notification.context {
            let contextValidation = validateContext(context)
            violations.append(contentsOf: contextValidation.violations)
            warnings.append(contentsOf: contextValidation.warnings)
        }
        
        let isValid = violations.isEmpty
        let score = calculateSecurityScore(violations: violations, warnings: warnings)
        
        let result = SecurityValidationResult(
            isValid: isValid,
            score: score,
            violations: violations,
            warnings: warnings,
            recommendations: generateRecommendations(violations: violations, warnings: warnings)
        )
        
        // Track validation
        analyticsTracker.trackSecurityValidation(notification, result: result)
        
        return result
    }
    
    /// Validate dialog content
    func validateDialog(_ dialog: AlertDialog) -> SecurityValidationResult {
        logger.debug("Validating dialog security: \(dialog.id)")
        
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Validate title
        if let title = dialog.title {
            let titleValidation = validateText(title, type: .title)
            violations.append(contentsOf: titleValidation.violations)
            warnings.append(contentsOf: titleValidation.warnings)
        }
        
        // Validate message
        if let message = dialog.message {
            let messageValidation = validateText(message, type: .message)
            violations.append(contentsOf: messageValidation.violations)
            warnings.append(contentsOf: messageValidation.warnings)
        }
        
        // Validate actions
        for action in dialog.actions {
            let actionValidation = validateAction(action)
            violations.append(contentsOf: actionValidation.violations)
            warnings.append(contentsOf: actionValidation.warnings)
        }
        
        let isValid = violations.isEmpty
        let score = calculateSecurityScore(violations: violations, warnings: warnings)
        
        let result = SecurityValidationResult(
            isValid: isValid,
            score: score,
            violations: violations,
            warnings: warnings,
            recommendations: generateRecommendations(violations: violations, warnings: warnings)
        )
        
        // Track validation
        analyticsTracker.trackSecurityValidation(dialog, result: result)
        
        return result
    }
    
    /// Sanitize notification content
    func sanitizeNotification(_ notification: inout ToastNotification) {
        logger.debug("Sanitizing notification: \(notification.id)")
        
        // Sanitize title
        let sanitizedTitle = sanitizeText(notification.title)
        
        // Sanitize message
        let sanitizedMessage = sanitizeText(notification.message)
        
        // Create sanitized notification
        notification = ToastNotification(
            id: notification.id,
            type: notification.type,
            title: sanitizedTitle,
            message: sanitizedMessage,
            duration: notification.duration,
            severity: notification.severity,
            context: notification.context,
            timestamp: notification.timestamp,
            retryable: notification.retryable,
            actions: notification.actions
        )
    }
    
    /// Check if content contains sensitive information
    func containsSensitiveInformation(_ text: String) -> Bool {
        logger.debug("Checking for sensitive information in text")
        
        // Check for common sensitive patterns
        let sensitivePatterns = [
            "password", "passwd", "pwd",
            "credit.card", "creditcard", "cc.number",
            "ssn", "social.security",
            "api.key", "apikey", "secret.key",
            "token", "auth.token", "access.token",
            "private.key", "privatekey",
            "email", "phone", "address"
        ]
        
        let lowercaseText = text.lowercased()
        
        for pattern in sensitivePatterns {
            if lowercaseText.contains(pattern) {
                return true
            }
        }
        
        return false
    }
    
    /// Check if content contains malicious patterns
    func containsMaliciousContent(_ text: String) -> Bool {
        logger.debug("Checking for malicious content in text")
        
        // Check for script injection patterns
        let maliciousPatterns = [
            "<script", "javascript:", "vbscript:",
            "onload=", "onerror=", "onclick=",
            "eval(", "document.cookie",
            "window.location", "document.location"
        ]
        
        let lowercaseText = text.lowercased()
        
        for pattern in maliciousPatterns {
            if lowercaseText.contains(pattern) {
                return true
            }
        }
        
        return false
    }
    
    /// Get security recommendations
    func getSecurityRecommendations() -> [SecurityRecommendation] {
        logger.debug("Getting security recommendations")
        
        var recommendations: [SecurityRecommendation] = []
        
        // Check for common security issues
        if !isContentSanitizationEnabled() {
            recommendations.append(SecurityRecommendation(
                type: .contentSanitization,
                priority: .high,
                title: "Enable Content Sanitization",
                description: "Content sanitization should be enabled to prevent XSS attacks",
                action: "Enable automatic content sanitization"
            ))
        }
        
        if !isInputValidationEnabled() {
            recommendations.append(SecurityRecommendation(
                type: .inputValidation,
                priority: .high,
                title: "Enable Input Validation",
                description: "Input validation should be enabled to prevent malicious input",
                action: "Enable strict input validation"
            ))
        }
        
        if !isRateLimitingEnabled() {
            recommendations.append(SecurityRecommendation(
                type: .rateLimiting,
                priority: .medium,
                title: "Enable Rate Limiting",
                description: "Rate limiting should be enabled to prevent spam and DoS attacks",
                action: "Enable notification rate limiting"
            ))
        }
        
        return recommendations
    }
    
    // MARK: - Private Methods
    
    private func validateText(_ text: String, type: ContentType) -> ValidationResult {
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Check for sensitive information
        if containsSensitiveInformation(text) {
            violations.append(SecurityViolation(
                type: .sensitiveInformation,
                severity: .high,
                message: "Text contains sensitive information",
                content: text,
                position: 0
            ))
        }
        
        // Check for malicious content
        if containsMaliciousContent(text) {
            violations.append(SecurityViolation(
                type: .maliciousContent,
                severity: .critical,
                message: "Text contains potentially malicious content",
                content: text,
                position: 0
            ))
        }
        
        // Check for excessive length
        if text.count > configuration.maxContentLength {
            warnings.append(SecurityWarning(
                type: .excessiveLength,
                message: "Text exceeds maximum recommended length",
                content: text,
                position: 0
            ))
        }
        
        // Check for special characters
        if containsExcessiveSpecialCharacters(text) {
            warnings.append(SecurityWarning(
                type: .excessiveSpecialCharacters,
                message: "Text contains excessive special characters",
                content: text,
                position: 0
            ))
        }
        
        return ValidationResult(violations: violations, warnings: warnings)
    }
    
    private func validateAction(_ action: ToastAction) -> ValidationResult {
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Validate action title
        let titleValidation = validateText(action.title, type: .actionTitle)
        violations.append(contentsOf: titleValidation.violations)
        warnings.append(contentsOf: titleValidation.warnings)
        
        return ValidationResult(violations: violations, warnings: warnings)
    }
    
    private func validateAction(_ action: AlertAction) -> ValidationResult {
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Validate action title
        let titleValidation = validateText(action.title, type: .actionTitle)
        violations.append(contentsOf: titleValidation.violations)
        warnings.append(contentsOf: titleValidation.warnings)
        
        return ValidationResult(violations: violations, warnings: warnings)
    }
    
    private func validateContext(_ context: ErrorContext) -> ValidationResult {
        var violations: [SecurityViolation] = []
        var warnings: [SecurityWarning] = []
        
        // Validate session ID
        if let sessionId = context.sessionId {
            if !isValidSessionId(sessionId) {
                violations.append(SecurityViolation(
                    type: .invalidSessionId,
                    severity: .medium,
                    message: "Invalid session ID format",
                    content: sessionId,
                    position: 0
                ))
            }
        }
        
        // Validate user ID
        if let userId = context.userId {
            if !isValidUserId(userId) {
                violations.append(SecurityViolation(
                    type: .invalidUserId,
                    severity: .medium,
                    message: "Invalid user ID format",
                    content: userId,
                    position: 0
                ))
            }
        }
        
        // Validate screen name
        if let screenName = context.screenName {
            if !isValidScreenName(screenName) {
                warnings.append(SecurityWarning(
                    type: .invalidScreenName,
                    message: "Invalid screen name format",
                    content: screenName,
                    position: 0
                ))
            }
        }
        
        return ValidationResult(violations: violations, warnings: warnings)
    }
    
    private func sanitizeText(_ text: String) -> String {
        var sanitizedText = text
        
        // Remove HTML tags
        sanitizedText = sanitizedText.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        
        // Remove script tags
        sanitizedText = sanitizedText.replacingOccurrences(of: "<script[^>]*>.*?</script>", with: "", options: [.regularExpression, .caseInsensitive])
        
        // Remove event handlers
        sanitizedText = sanitizedText.replacingOccurrences(of: "on\\w+\\s*=", with: "", options: .regularExpression)
        
        // Remove javascript: and vbscript: protocols
        sanitizedText = sanitizedText.replacingOccurrences(of: "javascript:", with: "", options: .caseInsensitive)
        sanitizedText = sanitizedText.replacingOccurrences(of: "vbscript:", with: "", options: .caseInsensitive)
        
        // Remove null bytes
        sanitizedText = sanitizedText.replacingOccurrences(of: "\0", with: "")
        
        // Trim whitespace
        sanitizedText = sanitizedText.trimmingCharacters(in: .whitespacesAndNewlines)
        
        return sanitizedText
    }
    
    private func containsExcessiveSpecialCharacters(_ text: String) -> Bool {
        let specialCharacterCount = text.filter { !$0.isLetter && !$0.isNumber && !$0.isWhitespace }.count
        let totalCharacterCount = text.count
        
        if totalCharacterCount == 0 {
            return false
        }
        
        let specialCharacterRatio = Double(specialCharacterCount) / Double(totalCharacterCount)
        return specialCharacterRatio > 0.3 // More than 30% special characters
    }
    
    private func isValidSessionId(_ sessionId: String) -> Bool {
        // Session ID should be UUID format
        return UUID(uuidString: sessionId) != nil
    }
    
    private func isValidUserId(_ userId: String) -> Bool {
        // User ID should be alphanumeric and reasonable length
        return userId.count >= 3 && userId.count <= 50 && userId.allSatisfy { $0.isLetter || $0.isNumber }
    }
    
    private func isValidScreenName(_ screenName: String) -> Bool {
        // Screen name should be alphanumeric with underscores and reasonable length
        return screenName.count >= 3 && screenName.count <= 50 && screenName.allSatisfy { $0.isLetter || $0.isNumber || $0 == "_" }
    }
    
    private func calculateSecurityScore(violations: [SecurityViolation], warnings: [SecurityWarning]) -> Double {
        let baseScore = 10.0
        let violationPenalty = Double(violations.count) * 2.0
        let warningPenalty = Double(warnings.count) * 0.5
        
        let score = max(0.0, baseScore - violationPenalty - warningPenalty)
        return min(10.0, score)
    }
    
    private func generateRecommendations(violations: [SecurityViolation], warnings: [SecurityWarning]) -> [String] {
        var recommendations: [String] = []
        
        for violation in violations {
            switch violation.type {
            case .sensitiveInformation:
                recommendations.append("Remove or mask sensitive information")
            case .maliciousContent:
                recommendations.append("Remove potentially malicious content")
            case .invalidSessionId:
                recommendations.append("Use valid UUID format for session ID")
            case .invalidUserId:
                recommendations.append("Use valid alphanumeric format for user ID")
            }
        }
        
        for warning in warnings {
            switch warning.type {
            case .excessiveLength:
                recommendations.append("Consider shortening the content")
            case .excessiveSpecialCharacters:
                recommendations.append("Reduce special character usage")
            case .invalidScreenName:
                recommendations.append("Use valid format for screen name")
            }
        }
        
        return recommendations
    }
    
    private func loadSecurityRules() {
        // Load security rules from configuration
        securityRules = [
            SecurityRule(
                id: "sensitive_info",
                name: "Sensitive Information",
                description: "Detect sensitive information in content",
                type: .content,
                severity: .high,
                enabled: true
            ),
            SecurityRule(
                id: "malicious_content",
                name: "Malicious Content",
                description: "Detect potentially malicious content",
                type: .content,
                severity: .critical,
                enabled: true
            ),
            SecurityRule(
                id: "excessive_length",
                name: "Excessive Length",
                description: "Detect excessively long content",
                type: .content,
                severity: .low,
                enabled: true
            )
        ]
    }
    
    private func setupSecurityMonitoring() {
        // Set up security monitoring
        Timer.scheduledTimer(withTimeInterval: 60.0, repeats: true) { [weak self] _ in
            self?.performSecurityScan()
        }
    }
    
    private func performSecurityScan() {
        // Perform periodic security scan
        logger.debug("Performing security scan")
        
        // Check for security issues
        let recommendations = getSecurityRecommendations()
        
        if !recommendations.isEmpty {
            logger.warning("Security scan found \(recommendations.count) recommendations")
        }
    }
    
    private func isContentSanitizationEnabled() -> Bool {
        return UserDefaults.standard.bool(forKey: "ContentSanitizationEnabled")
    }
    
    private func isInputValidationEnabled() -> Bool {
        return UserDefaults.standard.bool(forKey: "InputValidationEnabled")
    }
    
    private func isRateLimitingEnabled() -> Bool {
        return UserDefaults.standard.bool(forKey: "RateLimitingEnabled")
    }
}

// MARK: - Supporting Types

struct SecurityValidationResult {
    let isValid: Bool
    let score: Double
    let violations: [SecurityViolation]
    let warnings: [SecurityWarning]
    let recommendations: [String]
}

struct SecurityViolation {
    let type: ViolationType
    let severity: ViolationSeverity
    let message: String
    let content: String
    let position: Int
}

struct SecurityWarning {
    let type: WarningType
    let message: String
    let content: String
    let position: Int
}

enum ViolationType {
    case sensitiveInformation
    case maliciousContent
    case invalidSessionId
    case invalidUserId
}

enum WarningType {
    case excessiveLength
    case excessiveSpecialCharacters
    case invalidScreenName
}

enum ViolationSeverity {
    case low
    case medium
    case high
    case critical
}

enum ContentType {
    case title
    case message
    case actionTitle
}

struct ValidationResult {
    let violations: [SecurityViolation]
    let warnings: [SecurityWarning]
}

struct SecurityRule {
    let id: String
    let name: String
    let description: String
    let type: RuleType
    let severity: ViolationSeverity
    let enabled: Bool
}

enum RuleType {
    case content
    case action
    case context
}

struct SecurityRecommendation {
    let type: RecommendationType
    let priority: RecommendationPriority
    let title: String
    let description: String
    let action: String
}

enum RecommendationType {
    case contentSanitization
    case inputValidation
    case rateLimiting
    case encryption
}

enum RecommendationPriority {
    case low
    case medium
    case high
    case critical
}

struct ValidationEvent {
    let id: String
    let type: ValidationEventType
    let timestamp: Date
    let result: SecurityValidationResult
}

enum ValidationEventType {
    case notificationValidation
    case dialogValidation
    case contentSanitization
    case securityScan
}

struct SecurityConfiguration {
    let maxContentLength = 1000
    let maxSpecialCharacterRatio = 0.3
    let enableContentSanitization = true
    let enableInputValidation = true
    let enableRateLimiting = true
}

// MARK: - Analytics Extension
extension NotificationAnalyticsTracker {
    func trackSecurityValidation(_ notification: ToastNotification, result: SecurityValidationResult) {
        logger.debug("Tracking security validation for notification: \(notification.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .securityValidation,
            timestamp: Date(),
            data: [
                "notificationId": notification.id,
                "isValid": result.isValid,
                "score": result.score,
                "violationCount": result.violations.count,
                "warningCount": result.warnings.count
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
    
    func trackSecurityValidation(_ dialog: AlertDialog, result: SecurityValidationResult) {
        logger.debug("Tracking security validation for dialog: \(dialog.id)")
        
        let event = AnalyticsEvent(
            id: UUID().uuidString,
            type: .securityValidation,
            timestamp: Date(),
            data: [
                "dialogId": dialog.id,
                "isValid": result.isValid,
                "score": result.score,
                "violationCount": result.violations.count,
                "warningCount": result.warnings.count
            ]
        )
        
        queue.async { [weak self] in
            self?.processAnalyticsEvent(event)
        }
    }
}

// MARK: - Additional Analytics Event Type
extension AnalyticsEventType {
    static let securityValidation = AnalyticsEventType(rawValue: "security_validation")!
}
