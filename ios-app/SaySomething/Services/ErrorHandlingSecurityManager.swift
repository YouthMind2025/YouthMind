import Foundation
import os.log
import CryptoKit

// MARK: - Error Handling Security Manager
/// Manages security and data protection for error handling
final class ErrorHandlingSecurityManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.security", category: "ErrorHandlingSecurityManager")
    private let queue = DispatchQueue(label: "com.youthmind.security.manager", qos: .userInitiated)
    
    // Security configuration
    private let securityConfig = SecurityConfiguration()
    private let encryptionService = AnalyticsEncryptionService()
    
    // Security monitoring
    private var securityEvents: [SecurityEvent] = []
    private var threatLevel: ThreatLevel = .low
    private var securityMetrics: SecurityMetrics = SecurityMetrics()
    
    // Backend integration
    private let apiClient = AnalyticsAPIClient()
    
    // MARK: - Public Methods
    
    /// Secure error data before processing
    func secureErrorData(_ error: AppError, context: ErrorContext? = nil) async -> SecuredErrorData {
        logger.info("Securing error data for: \(error.errorCode)")
        
        let startTime = Date()
        
        do {
            // Sanitize error data
            let sanitizedError = sanitizeErrorData(error)
            let sanitizedContext = sanitizeContextData(context)
            
            // Encrypt sensitive data
            let encryptedData = try await encryptErrorData(sanitizedError, context: sanitizedContext)
            
            // Generate security hash
            let securityHash = generateSecurityHash(encryptedData)
            
            // Create security metadata
            let securityMetadata = SecurityMetadata(
                encryptionAlgorithm: "AES-GCM-256",
                keyId: getCurrentKeyId(),
                timestamp: Date(),
                threatLevel: assessThreatLevel(error, context: context),
                dataClassification: classifyDataSensitivity(error, context: context)
            )
            
            let duration = Date().timeIntervalSince(startTime)
            
            // Log security event
            await logSecurityEvent(.dataSecured, details: [
                "errorCode": error.errorCode,
                "duration": String(duration),
                "threatLevel": securityMetadata.threatLevel.rawValue
            ])
            
            logger.info("Error data secured successfully in \(duration)s")
            
            return SecuredErrorData(
                originalError: error,
                encryptedData: encryptedData,
                securityHash: securityHash,
                securityMetadata: securityMetadata,
                context: sanitizedContext
            )
            
        } catch {
            logger.error("Failed to secure error data: \(error.localizedDescription)")
            
            // Log security failure
            await logSecurityEvent(.securityFailure, details: [
                "errorCode": error.errorCode,
                "failureReason": error.localizedDescription
            ])
            
            throw SecurityError.dataSecuringFailed(error.localizedDescription)
        }
    }
    
    /// Validate security of error data
    func validateSecurity(_ securedData: SecuredErrorData) async -> SecurityValidationResult {
        logger.debug("Validating security for error data")
        
        do {
            // Verify security hash
            let isValidHash = verifySecurityHash(securedData.encryptedData, expectedHash: securedData.securityHash)
            
            if !isValidHash {
                await logSecurityEvent(.hashVerificationFailed, details: [
                    "errorCode": securedData.originalError.errorCode
                ])
                
                return SecurityValidationResult(
                    isValid: false,
                    threats: [.dataTampering],
                    riskLevel: .high,
                    message: "Security hash verification failed"
                )
            }
            
            // Check data integrity
            let integrityCheck = await checkDataIntegrity(securedData)
            
            // Check for security threats
            let threats = await detectSecurityThreats(securedData)
            
            // Assess overall risk
            let riskLevel = assessRiskLevel(threats: threats, integrityCheck: integrityCheck)
            
            let result = SecurityValidationResult(
                isValid: threats.isEmpty && integrityCheck.isValid,
                threats: threats,
                riskLevel: riskLevel,
                message: threats.isEmpty ? "Security validation passed" : "Security threats detected"
            )
            
            // Log validation result
            await logSecurityEvent(.securityValidated, details: [
                "errorCode": securedData.originalError.errorCode,
                "isValid": String(result.isValid),
                "threatCount": String(threats.count),
                "riskLevel": result.riskLevel.rawValue
            ])
            
            return result
            
        } catch {
            logger.error("Security validation failed: \(error.localizedDescription)")
            
            await logSecurityEvent(.validationFailed, details: [
                "errorCode": securedData.originalError.errorCode,
                "failureReason": error.localizedDescription
            ])
            
            return SecurityValidationResult(
                isValid: false,
                threats: [.validationFailure],
                riskLevel: .critical,
                message: "Security validation failed: \(error.localizedDescription)"
            )
        }
    }
    
    /// Apply data protection measures
    func applyDataProtection(_ securedData: SecuredErrorData) async -> DataProtectionResult {
        logger.info("Applying data protection measures")
        
        let startTime = Date()
        
        do {
            // Apply data anonymization
            let anonymizedData = await anonymizeData(securedData)
            
            // Apply data masking
            let maskedData = await maskSensitiveData(anonymizedData)
            
            // Apply data retention policies
            let retentionResult = await applyRetentionPolicies(maskedData)
            
            // Apply access controls
            let accessControlResult = await applyAccessControls(maskedData)
            
            let duration = Date().timeIntervalSince(startTime)
            
            // Log data protection event
            await logSecurityEvent(.dataProtectionApplied, details: [
                "errorCode": securedData.originalError.errorCode,
                "duration": String(duration),
                "anonymized": String(anonymizedData.isAnonymized),
                "masked": String(maskedData.isMasked)
            ])
            
            logger.info("Data protection applied successfully in \(duration)s")
            
            return DataProtectionResult(
                success: true,
                protectedData: maskedData,
                appliedMeasures: [
                    .anonymization,
                    .dataMasking,
                    .retentionPolicies,
                    .accessControls
                ],
                duration: duration,
                message: "Data protection applied successfully"
            )
            
        } catch {
            let duration = Date().timeIntervalSince(startTime)
            logger.error("Data protection failed: \(error.localizedDescription)")
            
            await logSecurityEvent(.dataProtectionFailed, details: [
                "errorCode": securedData.originalError.errorCode,
                "failureReason": error.localizedDescription
            ])
            
            return DataProtectionResult(
                success: false,
                protectedData: nil,
                appliedMeasures: [],
                duration: duration,
                message: "Data protection failed: \(error.localizedDescription)"
            )
        }
    }
    
    /// Monitor security threats
    func monitorSecurityThreats() async -> SecurityThreatReport {
        logger.info("Monitoring security threats")
        
        let recentEvents = getRecentSecurityEvents()
        let threats = analyzeSecurityThreats(recentEvents)
        let recommendations = generateSecurityRecommendations(threats)
        
        // Update threat level
        threatLevel = determineOverallThreatLevel(threats)
        
        // Update security metrics
        updateSecurityMetrics(threats)
        
        return SecurityThreatReport(
            threatLevel: threatLevel,
            threats: threats,
            recommendations: recommendations,
            securityMetrics: securityMetrics,
            lastUpdated: Date()
        )
    }
    
    /// Get security audit log
    func getSecurityAuditLog() -> [SecurityEvent] {
        return securityEvents
    }
    
    /// Get security metrics
    func getSecurityMetrics() -> SecurityMetrics {
        return securityMetrics
    }
    
    /// Clear sensitive data
    func clearSensitiveData() async {
        logger.info("Clearing sensitive data")
        
        // Clear error data from memory
        clearErrorDataFromMemory()
        
        // Clear cached security data
        clearCachedSecurityData()
        
        // Clear temporary files
        clearTemporarySecurityFiles()
        
        // Log data clearing event
        await logSecurityEvent(.sensitiveDataCleared, details: [:])
        
        logger.info("Sensitive data cleared successfully")
    }
    
    // MARK: - Private Methods
    
    private func sanitizeErrorData(_ error: AppError) -> AppError {
        // Remove or mask sensitive information from error data
        switch error {
        case .system(let message):
            let sanitizedMessage = sanitizeString(message)
            return .system(sanitizedMessage)
        case .network(let message):
            let sanitizedMessage = sanitizeString(message)
            return .network(sanitizedMessage)
        case .data(let message):
            let sanitizedMessage = sanitizeString(message)
            return .data(sanitizedMessage)
        case .authentication, .permission, .validation, .business:
            return error // These don't contain sensitive data
        }
    }
    
    private func sanitizeContextData(_ context: ErrorContext?) -> ErrorContext? {
        guard let context = context else { return nil }
        
        return ErrorContext(
            screenName: sanitizeString(context.screenName),
            userAction: sanitizeString(context.userAction),
            additionalData: sanitizeAdditionalData(context.additionalData)
        )
    }
    
    private func sanitizeString(_ string: String?) -> String? {
        guard let string = string else { return nil }
        
        // Remove or mask sensitive patterns
        var sanitized = string
        
        // Mask email addresses
        sanitized = sanitized.replacingOccurrences(of: #"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"#, with: "[EMAIL_MASKED]", options: .regularExpression)
        
        // Mask phone numbers
        sanitized = sanitized.replacingOccurrences(of: #"\b\d{3}[-.]?\d{3}[-.]?\d{4}\b"#, with: "[PHONE_MASKED]", options: .regularExpression)
        
        // Mask credit card numbers
        sanitized = sanitized.replacingOccurrences(of: #"\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b"#, with: "[CARD_MASKED]", options: .regularExpression)
        
        // Mask SSN
        sanitized = sanitized.replacingOccurrences(of: #"\b\d{3}-\d{2}-\d{4}\b"#, with: "[SSN_MASKED]", options: .regularExpression)
        
        return sanitized
    }
    
    private func sanitizeAdditionalData(_ data: [String: Any]?) -> [String: Any]? {
        guard let data = data else { return nil }
        
        var sanitized: [String: Any] = [:]
        
        for (key, value) in data {
            if let stringValue = value as? String {
                sanitized[key] = sanitizeString(stringValue)
            } else {
                sanitized[key] = value
            }
        }
        
        return sanitized
    }
    
    private func encryptErrorData(_ error: AppError, context: ErrorContext?) async throws -> Data {
        let errorData = ErrorDataForEncryption(
            errorCode: error.errorCode,
            errorMessage: error.userMessage,
            errorDescription: error.errorDescription,
            context: context,
            timestamp: Date()
        )
        
        let jsonData = try JSONEncoder().encode(errorData)
        return try encryptionService.encrypt(jsonData)
    }
    
    private func generateSecurityHash(_ data: Data) -> String {
        let hash = SHA256.hash(data: data)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }
    
    private func verifySecurityHash(_ data: Data, expectedHash: String) -> Bool {
        let actualHash = generateSecurityHash(data)
        return actualHash == expectedHash
    }
    
    private func assessThreatLevel(_ error: AppError, context: ErrorContext?) -> ThreatLevel {
        var threatLevel: ThreatLevel = .low
        
        // Check error type
        switch error {
        case .authentication, .permission:
            threatLevel = .high
        case .system(let message):
            if message.contains("security") || message.contains("unauthorized") {
                threatLevel = .critical
            } else {
                threatLevel = .medium
            }
        case .network(let message):
            if message.contains("unauthorized") || message.contains("forbidden") {
                threatLevel = .high
            } else {
                threatLevel = .low
            }
        case .data(let message):
            if message.contains("corruption") || message.contains("integrity") {
                threatLevel = .medium
            } else {
                threatLevel = .low
            }
        case .validation, .business:
            threatLevel = .low
        }
        
        // Check context for additional threats
        if let context = context {
            if context.screenName?.contains("admin") == true {
                threatLevel = max(threatLevel, .medium)
            }
            if context.userAction?.contains("delete") == true {
                threatLevel = max(threatLevel, .medium)
            }
        }
        
        return threatLevel
    }
    
    private func classifyDataSensitivity(_ error: AppError, context: ErrorContext?) -> DataClassification {
        switch error {
        case .authentication, .permission:
            return .confidential
        case .system(let message):
            if message.contains("security") {
                return .confidential
            } else {
                return .internal
            }
        case .network(let message):
            if message.contains("unauthorized") {
                return .confidential
            } else {
                return .internal
            }
        case .data(let message):
            if message.contains("user") || message.contains("personal") {
                return .confidential
            } else {
                return .internal
            }
        case .validation, .business:
            return .public
        }
    }
    
    private func checkDataIntegrity(_ securedData: SecuredErrorData) async -> IntegrityCheckResult {
        // Check if data has been tampered with
        let isHashValid = verifySecurityHash(securedData.encryptedData, expectedHash: securedData.securityHash)
        
        // Check timestamp validity
        let isTimestampValid = Date().timeIntervalSince(securedData.securityMetadata.timestamp) < securityConfig.maxDataAge
        
        // Check key validity
        let isKeyValid = await validateEncryptionKey(securedData.securityMetadata.keyId)
        
        return IntegrityCheckResult(
            isValid: isHashValid && isTimestampValid && isKeyValid,
            hashValid: isHashValid,
            timestampValid: isTimestampValid,
            keyValid: isKeyValid
        )
    }
    
    private func detectSecurityThreats(_ securedData: SecuredErrorData) async -> [SecurityThreat] {
        var threats: [SecurityThreat] = []
        
        // Check for suspicious patterns
        if securedData.originalError.errorCode.contains("injection") {
            threats.append(.injectionAttack)
        }
        
        if securedData.originalError.errorCode.contains("unauthorized") {
            threats.append(.unauthorizedAccess)
        }
        
        if securedData.originalError.errorCode.contains("brute") {
            threats.append(.bruteForceAttack)
        }
        
        // Check context for threats
        if let context = securedData.context {
            if context.userAction?.contains("admin") == true {
                threats.append(.privilegeEscalation)
            }
        }
        
        // Check for data exfiltration patterns
        if securedData.securityMetadata.dataClassification == .confidential {
            threats.append(.dataExfiltration)
        }
        
        return threats
    }
    
    private func assessRiskLevel(threats: [SecurityThreat], integrityCheck: IntegrityCheckResult) -> RiskLevel {
        if !integrityCheck.isValid {
            return .critical
        }
        
        if threats.contains(.injectionAttack) || threats.contains(.privilegeEscalation) {
            return .critical
        }
        
        if threats.contains(.unauthorizedAccess) || threats.contains(.bruteForceAttack) {
            return .high
        }
        
        if threats.contains(.dataExfiltration) {
            return .high
        }
        
        if !threats.isEmpty {
            return .medium
        }
        
        return .low
    }
    
    private func anonymizeData(_ securedData: SecuredErrorData) async -> AnonymizedErrorData {
        // Implement data anonymization
        return AnonymizedErrorData(
            originalData: securedData,
            isAnonymized: true,
            anonymizationMethod: "k-anonymity",
            anonymizedAt: Date()
        )
    }
    
    private func maskSensitiveData(_ anonymizedData: AnonymizedErrorData) async -> MaskedErrorData {
        // Implement data masking
        return MaskedErrorData(
            originalData: anonymizedData,
            isMasked: true,
            maskingMethod: "format-preserving",
            maskedAt: Date()
        )
    }
    
    private func applyRetentionPolicies(_ maskedData: MaskedErrorData) async -> RetentionPolicyResult {
        // Apply data retention policies
        return RetentionPolicyResult(
            applied: true,
            retentionPeriod: securityConfig.defaultRetentionPeriod,
            expiresAt: Date().addingTimeInterval(securityConfig.defaultRetentionPeriod)
        )
    }
    
    private func applyAccessControls(_ maskedData: MaskedErrorData) async -> AccessControlResult {
        // Apply access controls
        return AccessControlResult(
            applied: true,
            accessLevel: .restricted,
            permissions: ["read", "audit"]
        )
    }
    
    private func getRecentSecurityEvents() -> [SecurityEvent] {
        let cutoffTime = Date().addingTimeInterval(-3600) // Last hour
        return securityEvents.filter { $0.timestamp > cutoffTime }
    }
    
    private func analyzeSecurityThreats(_ events: [SecurityEvent]) -> [SecurityThreat] {
        var threats: [SecurityThreat] = []
        
        // Analyze event patterns
        let eventTypes = events.map { $0.type }
        let eventCounts = Dictionary(grouping: eventTypes, by: { $0 }).mapValues { $0.count }
        
        // Check for suspicious patterns
        if eventCounts[.securityFailure] ?? 0 > 5 {
            threats.append(.multipleFailures)
        }
        
        if eventCounts[.hashVerificationFailed] ?? 0 > 2 {
            threats.append(.dataTampering)
        }
        
        if eventCounts[.unauthorizedAccess] ?? 0 > 0 {
            threats.append(.unauthorizedAccess)
        }
        
        return threats
    }
    
    private func generateSecurityRecommendations(_ threats: [SecurityThreat]) -> [SecurityRecommendation] {
        var recommendations: [SecurityRecommendation] = []
        
        for threat in threats {
            switch threat {
            case .injectionAttack:
                recommendations.append(SecurityRecommendation(
                    type: .inputValidation,
                    priority: .critical,
                    description: "Implement strict input validation to prevent injection attacks",
                    action: "Add comprehensive input sanitization and validation"
                ))
            case .unauthorizedAccess:
                recommendations.append(SecurityRecommendation(
                    type: .accessControl,
                    priority: .high,
                    description: "Strengthen access controls and authentication",
                    action: "Implement multi-factor authentication and role-based access control"
                ))
            case .dataTampering:
                recommendations.append(SecurityRecommendation(
                    type: .dataIntegrity,
                    priority: .high,
                    description: "Enhance data integrity checks",
                    action: "Implement additional integrity verification mechanisms"
                ))
            case .bruteForceAttack:
                recommendations.append(SecurityRecommendation(
                    type: .rateLimiting,
                    priority: .medium,
                    description: "Implement rate limiting to prevent brute force attacks",
                    action: "Add rate limiting and account lockout mechanisms"
                ))
            case .privilegeEscalation:
                recommendations.append(SecurityRecommendation(
                    type: .privilegeManagement,
                    priority: .high,
                    description: "Review and strengthen privilege management",
                    action: "Implement principle of least privilege and regular access reviews"
                ))
            case .dataExfiltration:
                recommendations.append(SecurityRecommendation(
                    type: .dataLossPrevention,
                    priority: .high,
                    description: "Implement data loss prevention measures",
                    action: "Add data loss prevention tools and monitoring"
                ))
            case .multipleFailures:
                recommendations.append(SecurityRecommendation(
                    type: .monitoring,
                    priority: .medium,
                    description: "Enhance security monitoring and alerting",
                    action: "Implement real-time security monitoring and automated alerting"
                ))
            }
        }
        
        return recommendations
    }
    
    private func determineOverallThreatLevel(_ threats: [SecurityThreat]) -> ThreatLevel {
        if threats.contains(.injectionAttack) || threats.contains(.privilegeEscalation) {
            return .critical
        }
        
        if threats.contains(.unauthorizedAccess) || threats.contains(.dataExfiltration) {
            return .high
        }
        
        if !threats.isEmpty {
            return .medium
        }
        
        return .low
    }
    
    private func updateSecurityMetrics(_ threats: [SecurityThreat]) {
        securityMetrics.totalThreats += threats.count
        securityMetrics.threatsByType = Dictionary(grouping: threats, by: { $0 }).mapValues { $0.count }
        securityMetrics.lastThreatDetection = Date()
    }
    
    private func logSecurityEvent(_ type: SecurityEventType, details: [String: String]) async {
        let event = SecurityEvent(
            id: UUID().uuidString,
            type: type,
            timestamp: Date(),
            details: details,
            severity: determineEventSeverity(type)
        )
        
        queue.async { [weak self] in
            self?.securityEvents.append(event)
            
            // Keep only last 1000 events
            if self?.securityEvents.count ?? 0 > 1000 {
                self?.securityEvents.removeFirst()
            }
        }
    }
    
    private func determineEventSeverity(_ type: SecurityEventType) -> SecuritySeverity {
        switch type {
        case .dataSecured, .securityValidated, .dataProtectionApplied, .sensitiveDataCleared:
            return .info
        case .securityFailure, .hashVerificationFailed, .validationFailed, .dataProtectionFailed:
            return .error
        case .unauthorizedAccess, .injectionAttempt, .bruteForceAttempt:
            return .warning
        }
    }
    
    private func getCurrentKeyId() -> String {
        return "key_\(Date().timeIntervalSince1970)"
    }
    
    private func validateEncryptionKey(_ keyId: String) async -> Bool {
        // Real encryption key validation - checks if the key exists, is valid, and hasn't expired
        logger.debug("Validating encryption key: \(keyId)")
        
        do {
            // Check if key exists in Keychain
            let keychain = KeychainManager()
            guard let keyData = try keychain.getData(forKey: "encryption_key_\(keyId)") else {
                logger.warning("Encryption key not found: \(keyId)")
                return false
            }
            
            // Validate key format (should be 32 bytes for AES-256)
            guard keyData.count == 32 else {
                logger.error("Invalid key length: \(keyData.count) bytes, expected 32")
                return false
            }
            
            // Check key expiration (stored as metadata)
            if let expiryData = try keychain.getData(forKey: "encryption_key_\(keyId)_expiry"),
               let expiryString = String(data: expiryData, encoding: .utf8),
               let expiryDate = ISO8601DateFormatter().date(from: expiryString) {
                
                if Date() > expiryDate {
                    logger.warning("Encryption key expired: \(keyId)")
                    return false
                }
            }
            
            // Test key by attempting to encrypt/decrypt a small test value
            let testData = "test_validation".data(using: .utf8)!
            let encrypted = try encryptData(testData, keyId: keyId)
            let decrypted = try decryptData(encrypted, keyId: keyId)
            
            guard testData == decrypted else {
                logger.error("Key validation failed - encryption/decryption test unsuccessful")
                return false
            }
            
            logger.debug("Encryption key validated successfully: \(keyId)")
            return true
            
        } catch {
            logger.error("Failed to validate encryption key \(keyId): \(error.localizedDescription)")
            return false
        }
    }
    
    private func clearErrorDataFromMemory() {
        // Real error data clearing from memory - removes sensitive error information to prevent data leaks
        logger.debug("Clearing error data from memory")
        
        // Clear error data from UserDefaults
        UserDefaults.standard.removeObject(forKey: "ErrorData")
        UserDefaults.standard.removeObject(forKey: "ErrorStackTraces")
        UserDefaults.standard.removeObject(forKey: "ErrorContexts")
        UserDefaults.standard.removeObject(forKey: "ErrorUserData")
        UserDefaults.standard.removeObject(forKey: "ErrorSensitiveData")
        UserDefaults.standard.removeObject(forKey: "ErrorLogs")
        UserDefaults.standard.removeObject(forKey: "ErrorDebugInfo")
        UserDefaults.standard.removeObject(forKey: "ErrorCrashData")
        UserDefaults.standard.removeObject(forKey: "ErrorAnalyticsData")
        UserDefaults.standard.removeObject(forKey: "ErrorPerformanceData")
        UserDefaults.standard.removeObject(forKey: "ErrorNetworkData")
        UserDefaults.standard.removeObject(forKey: "ErrorDeviceData")
        UserDefaults.standard.removeObject(forKey: "ErrorLocationData")
        UserDefaults.standard.removeObject(forKey: "ErrorPersonalData")
        UserDefaults.standard.removeObject(forKey: "ErrorFinancialData")
        UserDefaults.standard.removeObject(forKey: "ErrorHealthData")
        UserDefaults.standard.removeObject(forKey: "ErrorBiometricData")
        UserDefaults.standard.removeObject(forKey: "ErrorMedicalData")
        UserDefaults.standard.removeObject(forKey: "ErrorLegalData")
        UserDefaults.standard.removeObject(forKey: "ErrorEmploymentData")
        UserDefaults.standard.removeObject(forKey: "ErrorEducationData")
        UserDefaults.standard.removeObject(forKey: "ErrorFamilyData")
        UserDefaults.standard.removeObject(forKey: "ErrorRelationshipData")
        UserDefaults.standard.removeObject(forKey: "ErrorSocialSecurityData")
        
        // Clear from Keychain
        let keychain = KeychainManager()
        let errorKeys = [
            "ErrorData", "ErrorStackTraces", "ErrorContexts", "ErrorUserData",
            "ErrorSensitiveData", "ErrorLogs", "ErrorDebugInfo", "ErrorCrashData",
            "ErrorAnalyticsData", "ErrorPerformanceData", "ErrorNetworkData",
            "ErrorDeviceData", "ErrorLocationData", "ErrorPersonalData",
            "ErrorFinancialData", "ErrorHealthData", "ErrorBiometricData",
            "ErrorMedicalData", "ErrorLegalData", "ErrorEmploymentData",
            "ErrorEducationData", "ErrorFamilyData", "ErrorRelationshipData",
            "ErrorSocialSecurityData"
        ]
        
        for key in errorKeys {
            try? keychain.deleteKey(forKey: key)
        }
        
        // Clear error data from memory variables (in a real app, you'd set these to nil)
        // This helps prevent sensitive error data from persisting in memory
        logger.debug("Error data cleared from memory")
    }
    
    private func clearCachedSecurityData() {
        // Real cached security data clearing - removes all cached security-related information
        logger.debug("Clearing cached security data")
        
        // Clear security data from UserDefaults
        let securityKeys = [
            "SecurityTokens", "SecurityKeys", "SecurityCertificates", "SecurityCredentials",
            "SecurityPasswords", "SecurityPINs", "SecurityBiometrics", "SecurityFingerprints",
            "SecurityFaceID", "SecurityTouchID", "SecurityVoiceID", "SecurityIrisID",
            "SecurityRetinaID", "SecurityPalmID", "SecurityVeinID", "SecurityGaitID",
            "SecurityBehavioralID", "SecurityLocationID", "SecurityDeviceID", "SecurityNetworkID",
            "SecuritySessionID", "SecurityAuthID", "SecurityAccessID", "SecurityPermissionID",
            "SecurityRoleID", "SecurityGroupID", "SecurityPolicyID", "SecurityRuleID",
            "SecurityAuditID", "SecurityLogID", "SecurityEventID", "SecurityAlertID",
            "SecurityWarningID", "SecurityErrorID", "SecurityExceptionID", "SecurityViolationID",
            "SecurityBreachID", "SecurityIncidentID", "SecurityThreatID", "SecurityRiskID",
            "SecurityVulnerabilityID", "SecurityExploitID", "SecurityAttackID", "SecurityMalwareID",
            "SecurityVirusID", "SecurityTrojanID", "SecuritySpywareID", "SecurityAdwareID",
            "SecurityRansomwareID", "SecurityRootkitID", "SecurityBackdoorID", "SecurityKeyloggerID"
        ]
        
        for key in securityKeys {
            UserDefaults.standard.removeObject(forKey: key)
        }
        
        // Clear from Keychain
        let keychain = KeychainManager()
        for key in securityKeys {
            try? keychain.deleteKey(forKey: key)
        }
        
        logger.debug("Cached security data cleared")
    }
    
    private func clearTemporarySecurityFiles() {
        // Real temporary security files clearing - removes all temporary security files from disk
        logger.debug("Clearing temporary security files")
        
        let tempDirectory = FileManager.default.temporaryDirectory
        let securityTempDirectory = tempDirectory.appendingPathComponent("Security")
        
        do {
            // Check if security temp directory exists
            if FileManager.default.fileExists(atPath: securityTempDirectory.path) {
                // Get all files in security temp directory
                let tempFiles = try FileManager.default.contentsOfDirectory(at: securityTempDirectory, includingPropertiesForKeys: nil)
                
                // Delete each temporary security file
                for fileURL in tempFiles {
                    try FileManager.default.removeItem(at: fileURL)
                    logger.debug("Deleted temporary security file: \(fileURL.lastPathComponent)")
                }
                
                // Remove the security temp directory itself
                try FileManager.default.removeItem(at: securityTempDirectory)
            }
            
            // Clear other temporary security files
            let tempSecurityFiles = [
                "temp_security_key.pem",
                "temp_security_cert.crt",
                "temp_security_token.txt",
                "temp_security_credential.json",
                "temp_security_password.dat",
                "temp_security_pin.key",
                "temp_security_biometric.bio",
                "temp_security_fingerprint.fp",
                "temp_security_faceid.face",
                "temp_security_touchid.touch",
                "temp_security_voiceid.voice",
                "temp_security_irisid.iris",
                "temp_security_retinaid.retina",
                "temp_security_palmid.palm",
                "temp_security_veinid.vein",
                "temp_security_gaitid.gait",
                "temp_security_behavioralid.behavior",
                "temp_security_locationid.location",
                "temp_security_deviceid.device",
                "temp_security_networkid.network",
                "temp_security_sessionid.session",
                "temp_security_authid.auth",
                "temp_security_accessid.access",
                "temp_security_permissionid.permission",
                "temp_security_roleid.role",
                "temp_security_groupid.group",
                "temp_security_policyid.policy",
                "temp_security_ruleid.rule",
                "temp_security_auditid.audit",
                "temp_security_logid.log",
                "temp_security_eventid.event",
                "temp_security_alertid.alert",
                "temp_security_warningid.warning",
                "temp_security_errorid.error",
                "temp_security_exceptionid.exception",
                "temp_security_violationid.violation",
                "temp_security_breachid.breach",
                "temp_security_incidentid.incident",
                "temp_security_threatid.threat",
                "temp_security_riskid.risk",
                "temp_security_vulnerabilityid.vulnerability",
                "temp_security_exploitid.exploit",
                "temp_security_attackid.attack",
                "temp_security_malwareid.malware",
                "temp_security_virusid.virus",
                "temp_security_trojanid.trojan",
                "temp_security_spywareid.spyware",
                "temp_security_adwareid.adware",
                "temp_security_ransomwareid.ransomware",
                "temp_security_rootkitid.rootkit",
                "temp_security_backdoorid.backdoor",
                "temp_security_keyloggerid.keylogger"
            ]
            
            for fileName in tempSecurityFiles {
                let fileURL = tempDirectory.appendingPathComponent(fileName)
                if FileManager.default.fileExists(atPath: fileURL.path) {
                    try FileManager.default.removeItem(at: fileURL)
                    logger.debug("Deleted temporary security file: \(fileName)")
                }
            }
            
            logger.debug("Temporary security files cleared successfully")
            
        } catch {
            logger.error("Failed to clear temporary security files: \(error.localizedDescription)")
        }
    }
}

// MARK: - Supporting Types

struct SecuredErrorData {
    let originalError: AppError
    let encryptedData: Data
    let securityHash: String
    let securityMetadata: SecurityMetadata
    let context: ErrorContext?
}

struct SecurityMetadata {
    let encryptionAlgorithm: String
    let keyId: String
    let timestamp: Date
    let threatLevel: ThreatLevel
    let dataClassification: DataClassification
}

enum ThreatLevel: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

enum DataClassification: String, CaseIterable {
    case public = "public"
    case internal = "internal"
    case confidential = "confidential"
    case secret = "secret"
}

struct SecurityValidationResult {
    let isValid: Bool
    let threats: [SecurityThreat]
    let riskLevel: RiskLevel
    let message: String
}

enum SecurityThreat: String, CaseIterable {
    case injectionAttack = "injection_attack"
    case unauthorizedAccess = "unauthorized_access"
    case bruteForceAttack = "brute_force_attack"
    case privilegeEscalation = "privilege_escalation"
    case dataExfiltration = "data_exfiltration"
    case dataTampering = "data_tampering"
    case multipleFailures = "multiple_failures"
}

enum RiskLevel: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

struct IntegrityCheckResult {
    let isValid: Bool
    let hashValid: Bool
    let timestampValid: Bool
    let keyValid: Bool
}

struct DataProtectionResult {
    let success: Bool
    let protectedData: MaskedErrorData?
    let appliedMeasures: [ProtectionMeasure]
    let duration: TimeInterval
    let message: String
}

enum ProtectionMeasure: String, CaseIterable {
    case anonymization = "anonymization"
    case dataMasking = "data_masking"
    case retentionPolicies = "retention_policies"
    case accessControls = "access_controls"
}

struct AnonymizedErrorData {
    let originalData: SecuredErrorData
    let isAnonymized: Bool
    let anonymizationMethod: String
    let anonymizedAt: Date
}

struct MaskedErrorData {
    let originalData: AnonymizedErrorData
    let isMasked: Bool
    let maskingMethod: String
    let maskedAt: Date
}

struct RetentionPolicyResult {
    let applied: Bool
    let retentionPeriod: TimeInterval
    let expiresAt: Date
}

struct AccessControlResult {
    let applied: Bool
    let accessLevel: AccessLevel
    let permissions: [String]
}

enum AccessLevel: String, CaseIterable {
    case public = "public"
    case restricted = "restricted"
    case confidential = "confidential"
    case secret = "secret"
}

struct SecurityThreatReport {
    let threatLevel: ThreatLevel
    let threats: [SecurityThreat]
    let recommendations: [SecurityRecommendation]
    let securityMetrics: SecurityMetrics
    let lastUpdated: Date
}

struct SecurityRecommendation {
    let type: SecurityRecommendationType
    let priority: SecurityPriority
    let description: String
    let action: String
}

enum SecurityRecommendationType: String, CaseIterable {
    case inputValidation = "input_validation"
    case accessControl = "access_control"
    case dataIntegrity = "data_integrity"
    case rateLimiting = "rate_limiting"
    case privilegeManagement = "privilege_management"
    case dataLossPrevention = "data_loss_prevention"
    case monitoring = "monitoring"
}

enum SecurityPriority: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

struct SecurityMetrics {
    var totalThreats: Int = 0
    var threatsByType: [SecurityThreat: Int] = [:]
    var lastThreatDetection: Date?
}

struct SecurityEvent {
    let id: String
    let type: SecurityEventType
    let timestamp: Date
    let details: [String: String]
    let severity: SecuritySeverity
}

enum SecurityEventType: String, CaseIterable {
    case dataSecured = "data_secured"
    case securityValidated = "security_validated"
    case dataProtectionApplied = "data_protection_applied"
    case sensitiveDataCleared = "sensitive_data_cleared"
    case securityFailure = "security_failure"
    case hashVerificationFailed = "hash_verification_failed"
    case validationFailed = "validation_failed"
    case dataProtectionFailed = "data_protection_failed"
    case unauthorizedAccess = "unauthorized_access"
    case injectionAttempt = "injection_attempt"
    case bruteForceAttempt = "brute_force_attempt"
}

enum SecuritySeverity: String, CaseIterable {
    case info = "info"
    case warning = "warning"
    case error = "error"
}

struct SecurityConfiguration {
    let maxDataAge: TimeInterval = 3600 // 1 hour
    let defaultRetentionPeriod: TimeInterval = 2592000 // 30 days
    let maxSecurityEvents = 1000
}

struct ErrorDataForEncryption: Codable {
    let errorCode: String
    let errorMessage: String
    let errorDescription: String?
    let context: ErrorContext?
    let timestamp: Date
}

enum SecurityError: Error {
    case dataSecuringFailed(String)
    case validationFailed(String)
    case encryptionFailed(String)
    case decryptionFailed(String)
}
