import Foundation
import os.log

// MARK: - App-wide Error Model
enum AppError: Error, LocalizedError {
    
    // MARK: - Error Cases
    
    // Network related errors (wrapping existing APIError)
    case network(APIError)
    
    // Business logic errors from backend
    case business(code: String, message: String?, details: [String: Any]?)
    
    // Authentication and authorization errors
    case authentication(message: String)
    case authorization(message: String)
    
    // Data validation and processing errors
    case dataValidation(field: String, message: String)
    case dataNotFound(resource: String)
    case dataCorrupted(message: String)
    
    // User input and interaction errors
    case userInput(field: String, message: String)
    case invalidOperation(message: String)
    
    // System and infrastructure errors
    case system(message: String)
    case configuration(message: String)
    
    // MARK: - Error Severity
    enum ErrorSeverity {
        case low
        case medium
        case high
        case critical
    }
    
    // MARK: - LocalizedError Conformance
    
    var errorDescription: String? {
        switch self {
        case .network(let apiError):
            return apiError.errorDescription
            
        case .business(_, let message, _):
            return message ?? "Business logic error occurred"
            
        case .authentication(let message):
            return "Authentication error: \(message)"
            
        case .authorization(let message):
            return "Authorization error: \(message)"
            
        case .dataValidation(let field, let message):
            return "Validation error for \(field): \(message)"
            
        case .dataNotFound(let resource):
            return "\(resource) not found"
            
        case .dataCorrupted(let message):
            return "Data corruption: \(message)"
            
        case .userInput(let field, let message):
            return "Invalid input for \(field): \(message)"
            
        case .invalidOperation(let message):
            return "Invalid operation: \(message)"
            
        case .system(let message):
            return "System error: \(message)"
            
        case .configuration(let message):
            return "Configuration error: \(message)"
        }
    }
    
    var failureReason: String? {
        switch self {
        case .network(let apiError):
            return "Network request failed: \(apiError.localizedDescription)"
            
        case .business(let code, _, _):
            return "Business error with code: \(code)"
            
        case .authentication:
            return "User authentication failed"
            
        case .authorization:
            return "User authorization failed"
            
        case .dataValidation:
            return "Data validation failed"
            
        case .dataNotFound:
            return "Requested data not available"
            
        case .dataCorrupted:
            return "Data integrity compromised"
            
        case .userInput:
            return "Invalid user input provided"
            
        case .invalidOperation:
            return "Operation not allowed in current state"
            
        case .system:
            return "Internal system error"
            
        case .configuration:
            return "Application configuration error"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .network(let apiError):
            if apiError.isRetryable {
                return "Please check your connection and try again"
            } else {
                return "Please contact support if the problem persists"
            }
            
        case .business(_, _, _):
            return "Please try again or contact support"
            
        case .authentication:
            return "Please log in again"
            
        case .authorization:
            return "You don't have permission to perform this action"
            
        case .dataValidation(let field, _):
            return "Please check the \(field) and try again"
            
        case .dataNotFound:
            return "The requested content may have been removed"
            
        case .dataCorrupted:
            return "Please refresh the app or contact support"
            
        case .userInput(let field, _):
            return "Please correct the \(field) and try again"
            
        case .invalidOperation:
            return "This action is not available right now"
            
        case .system:
            return "Please restart the app or contact support"
            
        case .configuration:
            return "Please contact support to resolve this issue"
        }
    }
    
    // MARK: - Error Properties
    
    var severity: ErrorSeverity {
        switch self {
        case .network(let apiError):
            switch apiError {
            case .timeout, .noInternetConnection, .connectionFailed, .connectionLost:
                return .medium
            case .unauthorized, .forbidden:
                return .high
            case .serverError:
                return .critical
            default:
                return .medium
            }
            
        case .business:
            return .medium
            
        case .authentication, .authorization:
            return .high
            
        case .dataValidation, .userInput:
            return .low
            
        case .dataNotFound:
            return .medium
            
        case .dataCorrupted:
            return .high
            
        case .invalidOperation:
            return .medium
            
        case .system, .configuration:
            return .critical
        }
    }
    
    var isRetryable: Bool {
        switch self {
        case .network(let apiError):
            return apiError.isRetryable
            
        case .business:
            return true
            
        case .authentication:
            return true
            
        case .authorization:
            return false
            
        case .dataValidation, .userInput:
            return true
            
        case .dataNotFound:
            return false
            
        case .dataCorrupted:
            return false
            
        case .invalidOperation:
            return false
            
        case .system, .configuration:
            return false
        }
    }
    
    // MARK: - Error Code for Analytics
    var errorCode: String {
        switch self {
        case .network(let apiError):
            return "NETWORK_\(String(describing: apiError))"
            
        case .business(let code, _, _):
            return "BUSINESS_\(code)"
            
        case .authentication:
            return "AUTH_ERROR"
            
        case .authorization:
            return "AUTHZ_ERROR"
            
        case .dataValidation:
            return "VALIDATION_ERROR"
            
        case .dataNotFound:
            return "NOT_FOUND"
            
        case .dataCorrupted:
            return "DATA_CORRUPTED"
            
        case .userInput:
            return "USER_INPUT_ERROR"
            
        case .invalidOperation:
            return "INVALID_OPERATION"
            
        case .system:
            return "SYSTEM_ERROR"
            
        case .configuration:
            return "CONFIG_ERROR"
        }
    }
}

// MARK: - AppError Extensions

extension AppError {
    
    // MARK: - Convenience Initializers
    
    /// Create AppError from APIError
    static func from(_ apiError: APIError) -> AppError {
        return .network(apiError)
    }
    
    /// Create business error from backend response
    static func createBusinessError(code: String, message: String? = nil, details: [String: Any]? = nil) -> AppError {
        return .business(code: code, message: message, details: details)
    }
    
    /// Create authentication error
    static func authentication(_ message: String) -> AppError {
        return .authentication(message: message)
    }
    
    /// Create authorization error
    static func authorization(_ message: String) -> AppError {
        return .authorization(message: message)
    }
    
    /// Create data validation error
    static func validation(field: String, message: String) -> AppError {
        return .dataValidation(field: field, message: message)
    }
    
    /// Create user input error
    static func createUserInputError(field: String, message: String) -> AppError {
        return .userInput(field: field, message: message)
    }
    
    // MARK: - Error Handling Utilities
    
    /// Check if error is critical and requires immediate attention
    var isCritical: Bool {
        return severity == .critical
    }
    
    /// Get user-friendly error message for display
    var userMessage: String {
        return errorDescription ?? "An unexpected error occurred"
    }
    
    /// Get detailed error information for debugging
    var debugInfo: [String: Any] {
        var info: [String: Any] = [
            "code": errorCode,
            "severity": String(describing: severity),
            "retryable": isRetryable,
            "description": errorDescription ?? "",
            "recovery": recoverySuggestion ?? ""
        ]
        
        switch self {
        case .business(_, _, let details):
            if let details = details {
                info["details"] = details
            }
        case .dataValidation(let field, _):
            info["field"] = field
        case .userInput(let field, _):
            info["field"] = field
        default:
            break
        }
        
        return info
    }
}

// MARK: - Error Logging Extension

extension AppError {
    
    /// Log error for analytics and debugging
    func log() {
        #if DEBUG
        print("🔴 AppError: \(errorCode) - \(userMessage)")
        print("📊 Debug Info: \(debugInfo)")
        #endif
        
        // Integrate with analytics service
        Task {
            await AnalyticsService.shared.trackError(self)
        }
    }
    
    /// Handle critical error with comprehensive error handling
    func handleCriticalError(context: ErrorContext? = nil) async {
        let logger = Logger(subsystem: "com.youthmind.critical", category: "AppError")
        logger.info("Handling critical error: \(errorCode)")
        
        let startTime = Date()
        
        // 1. Detect and classify error
        let detector = CriticalErrorDetector()
        let classification = detector.classifyError(self, context: context)
        
        // 2. Secure error data
        let securityManager = ErrorHandlingSecurityManager()
        let securedData = await securityManager.secureErrorData(self, context: context)
        
        // 3. Validate security
        let validationResult = await securityManager.validateSecurity(securedData)
        
        // 4. Apply data protection
        let protectionResult = await securityManager.applyDataProtection(securedData)
        
        // 5. Show user alert
        let alertManager = UserAlertManager()
        alertManager.showCriticalErrorAlert(classification)
        
        // 6. Report crash if needed
        if classification.isCritical {
            let crashService = CrashReportingService()
            crashService.reportCrash(self, context: context)
        }
        
        // 7. Execute recovery strategies
        let recoveryStrategies = ErrorRecoveryStrategies()
        let strategy = recoveryStrategies.getRecoveryStrategy(for: self, context: context)
        let recoveryResult = await recoveryStrategies.executeRecoveryStrategy(strategy, for: self)
        
        // 8. Escalate if needed
        if classification.severity == .critical || !recoveryResult.success {
            let escalationService = ErrorEscalationService()
            await escalationService.escalateError(self, context: context, priority: .critical)
        }
        
        // 9. Start debugging session
        let debuggingCollector = ErrorDebuggingCollector()
        let sessionId = debuggingCollector.startDebugSession(for: self, context: context)
        
        // 10. Monitor performance
        let performanceMonitor = ErrorHandlingPerformanceMonitor()
        performanceMonitor.recordErrorHandlingPerformance(self, startTime: startTime, endTime: Date(), context: context)
        
        // 11. Apply prevention strategies
        let preventionStrategies = ErrorPreventionStrategies()
        let preventionSuggestions = await preventionStrategies.analyzeAndSuggestPreventions(for: [])
        
        // 12. End debugging session
        await debuggingCollector.endDebugSession(sessionId)
        
        let duration = Date().timeIntervalSince(startTime)
        logger.info("Critical error handling completed in \(duration)s")
    }
    
    /// Handle error with app recovery
    func handleWithRecovery(context: ErrorContext? = nil) async -> RecoveryResult {
        let logger = Logger(subsystem: "com.youthmind.recovery", category: "AppError")
        logger.info("Handling error with recovery: \(errorCode)")
        
        let recoveryManager = AppRecoveryManager()
        
        // Get recommended actions
        let detector = CriticalErrorDetector()
        let classification = detector.classifyError(self, context: context)
        
        if classification.recommendedActions.isEmpty {
            return RecoveryResult(success: false, message: "No recovery actions available", duration: 0)
        }
        
        // Execute recovery actions
        let results = await recoveryManager.executeRecoverySequence(classification.recommendedActions, for: self)
        
        return RecoveryResult(
            success: results.success,
            message: results.message,
            duration: results.duration
        )
    }
}

// MARK: - Error Handling Protocol

protocol AppErrorHandling {
    func handle(_ error: AppError)
    func handle(_ error: AppError, retryAction: (() -> Void)?)
}

// MARK: - Supporting Types for Critical Error Handling

struct ErrorContext {
    let screenName: String?
    let userAction: String?
    let additionalData: [String: Any]?
}

struct RecoveryResult {
    let success: Bool
    let message: String
    let duration: TimeInterval
}

// MARK: - Default Error Handler

class AppErrorHandler: AppErrorHandling {
    
    static let shared = AppErrorHandler()
    
    private init() {}
    
    func handle(_ error: AppError) {
        handle(error, retryAction: nil)
    }
    
    func handle(_ error: AppError, retryAction: (() -> Void)?) {
        // Log the error
        error.log()
        
        // Track user behavior during error handling
        Task {
            await AnalyticsService.shared.trackUserBehavior(.errorEncountered, context: BehaviorContext(
                errorCode: error.errorCode,
                screenName: getCurrentScreenName(),
                additionalData: ["severity": String(describing: error.severity)]
            ))
        }
        
        // Handle critical errors immediately
        if error.isCritical {
            handleCriticalError(error)
            return
        }
        
        // Show user-friendly error message
        showErrorToUser(error, retryAction: retryAction)
    }
    
    private func handleCriticalError(_ error: AppError) {
        // Track critical error behavior
        Task {
            await AnalyticsService.shared.trackUserBehavior(.errorEncountered, context: BehaviorContext(
                errorCode: error.errorCode,
                screenName: getCurrentScreenName(),
                additionalData: ["severity": "critical", "type": "critical_error"]
            ))
        }
        
        // Report as crash if it's a system error
        if case .system = error {
            let crashReport = CrashReport(
                error: error.errorDescription ?? "System Error",
                stackTrace: Thread.callStackSymbols,
                userInfo: UserInfo(
                    userId: "unknown",
                    sessionId: UUID().uuidString,
                    isAnonymous: true
                )
            )
            
            Task {
                await AnalyticsService.shared.reportCrash(crashReport)
            }
        }
        
        // TODO: Implement critical error handling
        // - Show alert to user
        // - Log to crash reporting service
        // - Potentially restart app or clear cache
    }
    
    private func showErrorToUser(_ error: AppError, retryAction: (() -> Void)?) {
        // Track error display to user
        Task {
            await AnalyticsService.shared.trackUserBehavior(.errorEncountered, context: BehaviorContext(
                errorCode: error.errorCode,
                screenName: getCurrentScreenName(),
                additionalData: ["action": "error_displayed", "retryable": String(error.isRetryable)]
            ))
        }
        
        // TODO: Implement user notification
        // - Show toast or alert
        // - Provide retry button if applicable
        // - Navigate to appropriate screen if needed
    }
    
    private func getCurrentScreenName() -> String? {
        // Get current screen name for analytics
        // This should be implemented based on your navigation system
        return "unknown"
    }
} 


// example of using
//class SomeViewModel: ObservableObject {
//    @Published var errorMessage: String?
//    
//    func handleError(_ error: AppError) {
//        AppErrorHandler.shared.handle(error) { [weak self] in
//            // retry operation
//            self?.retryOperation()
//        }
//    }
//    
//    func makeAPICall() {
//        // API fetch
//        PostAPI.fetchPosts()
//            .mapError { AppError.from($0) }
//            .sink(
//                receiveCompletion: { completion in
//                    if case .failure(let error) = completion {
//                        self.handleError(error)
//                    }
//                },
//                receiveValue: { response in
//                    // handle success response
//                }
//            )
//            .store(in: &cancellables)
//    }
//}
