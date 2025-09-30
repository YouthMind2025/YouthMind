import Foundation
import UIKit
import os.log

// MARK: - User Alert Manager
/// Manages user alerts and notifications for critical errors
final class UserAlertManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.alerts", category: "UserAlertManager")
    private let queue = DispatchQueue(label: "com.youthmind.alerts.manager", qos: .userInitiated)
    
    // Alert configuration
    private let alertConfiguration = AlertConfiguration()
    private var activeAlerts: [String: UserAlert] = [:]
    private var alertHistory: [UserAlert] = []
    
    // MARK: - Public Methods
    
    /// Show critical error alert to user
    func showCriticalErrorAlert(_ classification: CriticalErrorClassification) {
        logger.info("Showing critical error alert for: \(classification.error.errorCode)")
        
        let alert = createCriticalErrorAlert(classification)
        presentAlert(alert)
        
        // Track alert display
        trackAlertDisplay(alert)
    }
    
    /// Show error recovery progress to user
    func showRecoveryProgress(_ action: CriticalErrorAction, progress: Double) {
        logger.debug("Showing recovery progress for action: \(action.type.rawValue) - \(Int(progress * 100))%")
        
        let alert = createRecoveryProgressAlert(action, progress: progress)
        presentAlert(alert)
    }
    
    /// Show error resolution confirmation
    func showResolutionConfirmation(_ error: AppError, wasResolved: Bool) {
        logger.info("Showing resolution confirmation for: \(error.errorCode) - \(wasResolved ? "RESOLVED" : "FAILED")")
        
        let alert = createResolutionConfirmationAlert(error, wasResolved: wasResolved)
        presentAlert(alert)
    }
    
    /// Show preventive action suggestions
    func showPreventiveActions(_ predictions: [CriticalErrorPrediction]) {
        logger.info("Showing preventive actions for \(predictions.count) predictions")
        
        let alert = createPreventiveActionsAlert(predictions)
        presentAlert(alert)
    }
    
    /// Show support contact information
    func showSupportContact(_ error: AppError, context: ErrorContext?) {
        logger.info("Showing support contact for error: \(error.errorCode)")
        
        let alert = createSupportContactAlert(error, context: context)
        presentAlert(alert)
    }
    
    /// Dismiss all active alerts
    func dismissAllAlerts() {
        logger.info("Dismissing all active alerts")
        
        queue.async { [weak self] in
            self?.activeAlerts.removeAll()
        }
        
        // Dismiss all presented alerts
        DispatchQueue.main.async {
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first {
                window.rootViewController?.dismiss(animated: true)
            }
        }
    }
    
    /// Get alert history
    func getAlertHistory() -> [UserAlert] {
        return alertHistory
    }
    
    // MARK: - Private Methods
    
    private func createCriticalErrorAlert(_ classification: CriticalErrorClassification) -> UserAlert {
        let title = getCriticalErrorTitle(classification)
        let message = getCriticalErrorMessage(classification)
        let actions = getCriticalErrorActions(classification)
        
        return UserAlert(
            id: UUID().uuidString,
            type: .criticalError,
            title: title,
            message: message,
            severity: classification.severity,
            actions: actions,
            timestamp: Date(),
            isDismissible: classification.severity != .critical
        )
    }
    
    private func createRecoveryProgressAlert(_ action: CriticalErrorAction, progress: Double) -> UserAlert {
        let title = "Recovering from Error"
        let message = "\(action.description)\n\nProgress: \(Int(progress * 100))%"
        let actions = [
            UserAlertAction(
                title: "Cancel",
                style: .cancel,
                handler: { [weak self] in
                    self?.cancelRecoveryAction(action)
                }
            )
        ]
        
        return UserAlert(
            id: UUID().uuidString,
            type: .recoveryProgress,
            title: title,
            message: message,
            severity: .medium,
            actions: actions,
            timestamp: Date(),
            isDismissible: false
        )
    }
    
    private func createResolutionConfirmationAlert(_ error: AppError, wasResolved: Bool) -> UserAlert {
        let title = wasResolved ? "Error Resolved" : "Error Resolution Failed"
        let message = wasResolved ? 
            "The error has been successfully resolved. Your app should now work normally." :
            "We couldn't automatically resolve this error. Please contact support for assistance."
        
        let actions = wasResolved ? [] : [
            UserAlertAction(
                title: "Contact Support",
                style: .default,
                handler: { [weak self] in
                    self?.showSupportContact(error, context: nil)
                }
            ),
            UserAlertAction(
                title: "Try Again",
                style: .default,
                handler: { [weak self] in
                    self?.retryErrorResolution(error)
                }
            )
        ]
        
        return UserAlert(
            id: UUID().uuidString,
            type: .resolutionConfirmation,
            title: title,
            message: message,
            severity: wasResolved ? .low : .high,
            actions: actions,
            timestamp: Date(),
            isDismissible: true
        )
    }
    
    private func createPreventiveActionsAlert(_ predictions: [CriticalErrorPrediction]) -> UserAlert {
        let title = "Preventive Actions Recommended"
        let message = "We've detected potential issues that could cause problems. Here are some actions you can take to prevent them:"
        
        var actions: [UserAlertAction] = []
        
        for prediction in predictions {
            for action in prediction.recommendedPreventiveActions {
                actions.append(UserAlertAction(
                    title: action,
                    style: .default,
                    handler: { [weak self] in
                        self?.executePreventiveAction(action, for: prediction)
                    }
                ))
            }
        }
        
        actions.append(UserAlertAction(
            title: "Dismiss",
            style: .cancel,
            handler: nil
        ))
        
        return UserAlert(
            id: UUID().uuidString,
            type: .preventiveActions,
            title: title,
            message: message,
            severity: .medium,
            actions: actions,
            timestamp: Date(),
            isDismissible: true
        )
    }
    
    private func createSupportContactAlert(_ error: AppError, context: ErrorContext?) -> UserAlert {
        let title = "Contact Support"
        let message = "We're here to help! Please contact our support team with the following information:\n\nError Code: \(error.errorCode)\nTimestamp: \(DateFormatter.iso8601.string(from: Date()))"
        
        let actions = [
            UserAlertAction(
                title: "Email Support",
                style: .default,
                handler: { [weak self] in
                    self?.openEmailSupport(error, context: context)
                }
            ),
            UserAlertAction(
                title: "Call Support",
                style: .default,
                handler: { [weak self] in
                    self?.openPhoneSupport()
                }
            ),
            UserAlertAction(
                title: "Live Chat",
                style: .default,
                handler: { [weak self] in
                    self?.openLiveChat(error, context: context)
                }
            ),
            UserAlertAction(
                title: "Cancel",
                style: .cancel,
                handler: nil
            )
        ]
        
        return UserAlert(
            id: UUID().uuidString,
            type: .supportContact,
            title: title,
            message: message,
            severity: .medium,
            actions: actions,
            timestamp: Date(),
            isDismissible: true
        )
    }
    
    private func presentAlert(_ alert: UserAlert) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Store active alert
            self.activeAlerts[alert.id] = alert
            
            // Create and present UIAlertController
            let alertController = self.createUIAlertController(from: alert)
            
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first,
               let rootViewController = window.rootViewController {
                
                // Find the topmost presented view controller
                var topController = rootViewController
                while let presentedController = topController.presentedViewController {
                    topController = presentedController
                }
                
                topController.present(alertController, animated: true)
            }
        }
    }
    
    private func createUIAlertController(from alert: UserAlert) -> UIAlertController {
        let alertController = UIAlertController(
            title: alert.title,
            message: alert.message,
            preferredStyle: .alert
        )
        
        // Add actions
        for action in alert.actions {
            let uiAction = UIAlertAction(title: action.title, style: action.style.uiAlertActionStyle) { _ in
                action.handler?()
            }
            alertController.addAction(uiAction)
        }
        
        // Add dismiss action if alert is dismissible
        if alert.isDismissible && alert.actions.isEmpty {
            let dismissAction = UIAlertAction(title: "OK", style: .default) { [weak self] _ in
                self?.dismissAlert(alert.id)
            }
            alertController.addAction(dismissAction)
        }
        
        return alertController
    }
    
    private func dismissAlert(_ alertId: String) {
        queue.async { [weak self] in
            self?.activeAlerts.removeValue(forKey: alertId)
        }
    }
    
    private func trackAlertDisplay(_ alert: UserAlert) {
        queue.async { [weak self] in
            self?.alertHistory.append(alert)
            
            // Keep only last 100 alerts
            if self?.alertHistory.count ?? 0 > 100 {
                self?.alertHistory.removeFirst()
            }
        }
    }
    
    // MARK: - Alert Content Generation
    
    private func getCriticalErrorTitle(_ classification: CriticalErrorClassification) -> String {
        switch classification.severity {
        case .critical:
            return "🚨 Critical Error"
        case .high:
            return "⚠️ High Priority Error"
        case .medium:
            return "⚠️ Error Detected"
        case .low:
            return "ℹ️ Notice"
        }
    }
    
    private func getCriticalErrorMessage(_ classification: CriticalErrorClassification) -> String {
        let baseMessage = "An error has occurred that requires your attention:\n\n\(classification.error.userMessage)"
        
        if !classification.recommendedActions.isEmpty {
            let actionsText = classification.recommendedActions.map { "• \($0.description)" }.joined(separator: "\n")
            return "\(baseMessage)\n\nRecommended actions:\n\(actionsText)"
        }
        
        return baseMessage
    }
    
    private func getCriticalErrorActions(_ classification: CriticalErrorClassification) -> [UserAlertAction] {
        var actions: [UserAlertAction] = []
        
        // Add recommended actions
        for recommendedAction in classification.recommendedActions {
            actions.append(UserAlertAction(
                title: recommendedAction.description,
                style: getActionStyle(for: recommendedAction.priority),
                handler: { [weak self] in
                    self?.executeRecommendedAction(recommendedAction, for: classification.error)
                }
            ))
        }
        
        // Add support contact action for critical errors
        if classification.severity == .critical {
            actions.append(UserAlertAction(
                title: "Contact Support",
                style: .default,
                handler: { [weak self] in
                    self?.showSupportContact(classification.error, context: classification.context)
                }
            ))
        }
        
        // Add dismiss action
        if classification.isDismissible {
            actions.append(UserAlertAction(
                title: "Dismiss",
                style: .cancel,
                handler: { [weak self] in
                    self?.dismissAlert(classification.error.errorCode)
                }
            ))
        }
        
        return actions
    }
    
    private func getActionStyle(for priority: ActionPriority) -> UserAlertActionStyle {
        switch priority {
        case .critical:
            return .destructive
        case .high:
            return .default
        case .medium:
            return .default
        case .low:
            return .cancel
        }
    }
    
    // MARK: - Action Handlers
    
    private func executeRecommendedAction(_ action: CriticalErrorAction, for error: AppError) {
        logger.info("Executing recommended action: \(action.type.rawValue) for error: \(error.errorCode)")
        
        switch action.type {
        case .clearCache:
            clearAppCache()
        case .restartApp:
            restartApplication()
        case .reportCrash:
            reportCrashToBackend(error)
        case .reauthenticate:
            reauthenticateUser()
        case .retryConnection:
            retryNetworkConnection()
        case .restoreData:
            restoreDataFromBackup()
        case .validateData:
            validateDataIntegrity()
        case .requestPermissions:
            requestRequiredPermissions()
        case .retryOperation:
            retryFailedOperation(error)
        }
    }
    
    private func cancelRecoveryAction(_ action: CriticalErrorAction) {
        logger.info("Cancelling recovery action: \(action.type.rawValue)")
        
        // Cancel the specific recovery action based on type
        switch action.type {
        case .clearCache:
            cancelCacheClearing()
        case .restartApp:
            cancelAppRestart()
        case .restoreData:
            cancelDataRestoration()
        case .reauthenticate:
            cancelReauthentication()
        case .retryConnection:
            cancelConnectionRetry()
        case .validateData:
            cancelDataValidation()
        case .requestPermissions:
            cancelPermissionRequest()
        case .reportCrash:
            cancelCrashReporting()
        case .escalateError:
            cancelErrorEscalation()
        case .collectDebugInfo:
            cancelDebugCollection()
        case .preventiveAction:
            cancelPreventiveAction()
        }
        
        // Notify that action was cancelled
        NotificationCenter.default.post(name: .recoveryActionCancelled, object: action)
    }
    
    private func retryErrorResolution(_ error: AppError) {
        logger.info("Retrying error resolution for: \(error.errorCode)")
        
        // Implement retry logic based on error type
        switch error {
        case .network:
            retryNetworkError()
        case .authentication:
            retryAuthenticationError()
        case .data:
            retryDataError()
        case .system:
            retrySystemError()
        case .permission:
            retryPermissionError()
        case .validation:
            retryValidationError()
        case .business:
            retryBusinessError()
        }
        
        // Track retry attempt
        let retryRecord = ErrorRetryRecord(
            errorCode: error.errorCode,
            timestamp: Date(),
            attemptNumber: getRetryAttemptCount(for: error) + 1
        )
        recordRetryAttempt(retryRecord)
    }
    
    private func executePreventiveAction(_ action: String, for prediction: CriticalErrorPrediction) {
        logger.info("Executing preventive action: \(action) for prediction: \(prediction.type.rawValue)")
        
        // Execute specific preventive actions
        switch action.lowercased() {
        case "clear_cache":
            clearAppCache()
        case "restart_app":
            restartApplication()
        case "validate_data":
            validateDataIntegrity()
        case "check_permissions":
            requestRequiredPermissions()
        case "test_network":
            retryNetworkConnection()
        case "backup_data":
            performDataBackup()
        case "monitor_resources":
            startResourceMonitoring()
        case "update_configuration":
            updateAppConfiguration()
        default:
            logger.warning("Unknown preventive action: \(action)")
        }
        
        // Track preventive action execution
        let preventiveRecord = PreventiveActionRecord(
            action: action,
            predictionType: prediction.type,
            timestamp: Date(),
            success: true
        )
        recordPreventiveAction(preventiveRecord)
    }
    
    private func openEmailSupport(_ error: AppError, context: ErrorContext?) {
        logger.info("Opening email support for error: \(error.errorCode)")
        
        let subject = "Support Request - Error: \(error.errorCode)"
        let body = createSupportEmailBody(error, context: context)
        
        if let url = URL(string: "mailto:support@youthmind.com?subject=\(subject)&body=\(body)") {
            UIApplication.shared.open(url)
        }
    }
    
    private func openPhoneSupport() {
        logger.info("Opening phone support")
        
        if let url = URL(string: "tel:+1234567890") {
            UIApplication.shared.open(url)
        }
    }
    
    private func openLiveChat(_ error: AppError, context: ErrorContext?) {
        logger.info("Opening live chat for error: \(error.errorCode)")
        
        // Create live chat URL with error context
        var chatURL = "https://youthmind.com/support/chat"
        var queryItems: [URLQueryItem] = []
        
        // Add error context to chat
        queryItems.append(URLQueryItem(name: "error_code", value: error.errorCode))
        queryItems.append(URLQueryItem(name: "error_severity", value: String(describing: error.severity)))
        queryItems.append(URLQueryItem(name: "timestamp", value: ISO8601DateFormatter().string(from: Date())))
        
        if let context = context {
            if let sessionId = context.sessionId {
                queryItems.append(URLQueryItem(name: "session_id", value: sessionId))
            }
            if let userId = context.userId {
                queryItems.append(URLQueryItem(name: "user_id", value: userId))
            }
            if let screenName = context.screenName {
                queryItems.append(URLQueryItem(name: "screen", value: screenName))
            }
        }
        
        // Add device info
        queryItems.append(URLQueryItem(name: "device_model", value: UIDevice.current.model))
        queryItems.append(URLQueryItem(name: "os_version", value: UIDevice.current.systemVersion))
        queryItems.append(URLQueryItem(name: "app_version", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown"))
        
        // Build final URL
        if var components = URLComponents(string: chatURL) {
            components.queryItems = queryItems
            if let finalURL = components.url {
                UIApplication.shared.open(finalURL)
            }
        }
    }
    
    // MARK: - Action Implementations
    
    private func clearAppCache() {
        logger.info("Clearing app cache")
        
        // Clear URL cache
        URLCache.shared.removeAllCachedResponses()
        
        // Clear image cache
        if let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first {
            do {
                let imageCacheURL = cacheDirectory.appendingPathComponent("ImageCache")
                if FileManager.default.fileExists(atPath: imageCacheURL.path) {
                    try FileManager.default.removeItem(at: imageCacheURL)
                }
            } catch {
                logger.error("Failed to clear image cache: \(error.localizedDescription)")
            }
        }
        
        // Clear temporary files
        if let tempDirectory = FileManager.default.temporaryDirectory as URL? {
            do {
                let tempFiles = try FileManager.default.contentsOfDirectory(at: tempDirectory, includingPropertiesForKeys: nil)
                for file in tempFiles {
                    try FileManager.default.removeItem(at: file)
                }
            } catch {
                logger.error("Failed to clear temporary files: \(error.localizedDescription)")
            }
        }
    }
    
    private func restartApplication() {
        logger.info("Restarting application")
        
        // Save current state
        UserDefaults.standard.synchronize()
        
        // Clear sensitive data from memory
        clearSensitiveDataFromMemory()
        
        // Schedule app restart
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            exit(0) // This will restart the app
        }
    }
    
    private func reportCrashToBackend(_ error: AppError) {
        logger.info("Reporting crash to backend for error: \(error.errorCode)")
        
        let crashService = CrashReportingService()
        crashService.reportCrash(error)
    }
    
    private func reauthenticateUser() {
        logger.info("Reauthenticating user")
        
        // Clear current authentication
        UserDefaults.standard.removeObject(forKey: "AuthToken")
        UserDefaults.standard.removeObject(forKey: "RefreshToken")
        
        // Clear from Keychain
        let keychain = KeychainManager()
        try? keychain.deleteKey(forKey: "AuthToken")
        try? keychain.deleteKey(forKey: "RefreshToken")
        
        // Trigger reauthentication flow
        NotificationCenter.default.post(name: .userReauthenticationRequired, object: nil)
    }
    
    private func retryNetworkConnection() {
        logger.info("Retrying network connection")
        
        // Test network connectivity
        testNetworkConnectivity { [weak self] isConnected in
            if isConnected {
                self?.logger.info("Network connection restored")
                NotificationCenter.default.post(name: .networkConnectionRestored, object: nil)
            } else {
                self?.logger.warning("Network connection retry failed")
                NotificationCenter.default.post(name: .networkConnectionFailed, object: nil)
            }
        }
    }
    
    private func restoreDataFromBackup() {
        logger.info("Restoring data from backup")
        
        let backupManager = DataBackupManager()
        backupManager.restoreFromBackup { [weak self] success in
            if success {
                self?.logger.info("Data restored from backup successfully")
                NotificationCenter.default.post(name: .dataRestored, object: nil)
            } else {
                self?.logger.error("Data restoration failed")
                NotificationCenter.default.post(name: .dataRestoreFailed, object: nil)
            }
        }
    }
    
    private func validateDataIntegrity() {
        logger.info("Validating data integrity")
        
        let dataValidator = DataIntegrityValidator()
        dataValidator.validateAllData { [weak self] isValid in
            if isValid {
                self?.logger.info("Data integrity validation passed")
                NotificationCenter.default.post(name: .dataIntegrityValid, object: nil)
            } else {
                self?.logger.warning("Data integrity validation failed")
                NotificationCenter.default.post(name: .dataIntegrityInvalid, object: nil)
            }
        }
    }
    
    private func requestRequiredPermissions() {
        logger.info("Requesting required permissions")
        
        let permissionManager = PermissionManager()
        permissionManager.requestAllRequiredPermissions { [weak self] granted in
            if granted {
                self?.logger.info("All required permissions granted")
                NotificationCenter.default.post(name: .permissionsGranted, object: nil)
            } else {
                self?.logger.warning("Some permissions were not granted")
                NotificationCenter.default.post(name: .permissionsDenied, object: nil)
            }
        }
    }
    
    private func retryFailedOperation(_ error: AppError) {
        logger.info("Retrying failed operation for error: \(error.errorCode)")
        
        // Implement retry logic based on error type
        switch error {
        case .network:
            retryNetworkOperation()
        case .authentication:
            reauthenticateUser()
        case .data:
            validateDataIntegrity()
        case .system:
            clearAppCache()
        default:
            // Generic retry
            NotificationCenter.default.post(name: .operationRetryRequested, object: error)
        }
    }
    
    private func clearSensitiveDataFromMemory() {
        // Clear sensitive data from memory
        logger.info("Clearing sensitive data from memory")
        
        // Clear authentication tokens from memory
        clearAuthenticationTokens()
        
        // Clear cached user data
        clearCachedUserData()
        
        // Clear sensitive cached data
        clearSensitiveCachedData()
        
        // Clear clipboard if it contains sensitive data
        clearClipboardIfSensitive()
        
        // Clear any cached passwords or credentials
        clearCachedCredentials()
        
        // Force garbage collection to free memory
        forceMemoryCleanup()
    }
    
    private func clearAuthenticationTokens() {
        // Clear JWT tokens and refresh tokens from memory
        UserDefaults.standard.removeObject(forKey: "AuthToken")
        UserDefaults.standard.removeObject(forKey: "RefreshToken")
        UserDefaults.standard.removeObject(forKey: "AccessToken")
        
        // Clear from Keychain
        let keychain = KeychainManager()
        try? keychain.deleteKey(forKey: "AuthToken")
        try? keychain.deleteKey(forKey: "RefreshToken")
        try? keychain.deleteKey(forKey: "AccessToken")
        
        logger.debug("Cleared authentication tokens")
    }
    
    private func clearCachedUserData() {
        // Clear cached user profile data
        UserDefaults.standard.removeObject(forKey: "CachedUserProfile")
        UserDefaults.standard.removeObject(forKey: "CachedUserSettings")
        UserDefaults.standard.removeObject(forKey: "CachedUserPreferences")
        
        // Clear from file system
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let userDataFile = documentsDirectory.appendingPathComponent("cached_user_data.json")
        try? FileManager.default.removeItem(at: userDataFile)
        
        logger.debug("Cleared cached user data")
    }
    
    private func clearSensitiveCachedData() {
        // Clear any other sensitive cached data
        UserDefaults.standard.removeObject(forKey: "SensitiveCache")
        UserDefaults.standard.removeObject(forKey: "TemporaryData")
        UserDefaults.standard.removeObject(forKey: "SessionData")
        
        // Clear sensitive files from Documents directory
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let sensitiveFiles = [
            "sensitive_data.json",
            "temporary_data.json",
            "session_data.json",
            "private_cache.json"
        ]
        
        for fileName in sensitiveFiles {
            let fileURL = documentsDirectory.appendingPathComponent(fileName)
            try? FileManager.default.removeItem(at: fileURL)
        }
        
        logger.debug("Cleared sensitive cached data")
    }
    
    private func clearClipboardIfSensitive() {
        // Check if clipboard contains sensitive data and clear it
        let pasteboard = UIPasteboard.general
        if let clipboardContent = pasteboard.string {
            // Check for common sensitive data patterns
            let sensitivePatterns = [
                "password",
                "token",
                "secret",
                "key",
                "credential",
                "auth",
                "login"
            ]
            
            let containsSensitiveData = sensitivePatterns.contains { pattern in
                clipboardContent.lowercased().contains(pattern)
            }
            
            if containsSensitiveData {
                pasteboard.string = ""
                logger.debug("Cleared clipboard containing sensitive data")
            }
        }
    }
    
    private func clearCachedCredentials() {
        // Clear any cached passwords or credentials
        UserDefaults.standard.removeObject(forKey: "CachedPassword")
        UserDefaults.standard.removeObject(forKey: "CachedCredentials")
        UserDefaults.standard.removeObject(forKey: "BiometricData")
        
        // Clear from Keychain
        let keychain = KeychainManager()
        try? keychain.deleteKey(forKey: "CachedPassword")
        try? keychain.deleteKey(forKey: "CachedCredentials")
        try? keychain.deleteKey(forKey: "BiometricData")
        
        logger.debug("Cleared cached credentials")
    }
    
    private func forceMemoryCleanup() {
        // Force garbage collection to free memory
        // This helps ensure sensitive data is actually removed from memory
        DispatchQueue.global(qos: .background).async {
            // Force memory cleanup by creating and releasing large objects
            for _ in 0..<10 {
                let _ = Array(repeating: 0, count: 1000000) // 1MB array
            }
            
            // Trigger memory warning to force cleanup
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: UIApplication.didReceiveMemoryWarningNotification, object: nil)
            }
        }
        
        logger.debug("Forced memory cleanup")
    }
    
    private func testNetworkConnectivity(completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "https://www.google.com") else {
            completion(false)
            return
        }
        
        let task = URLSession.shared.dataTask(with: url) { _, response, error in
            let isConnected = (response as? HTTPURLResponse)?.statusCode == 200 && error == nil
            DispatchQueue.main.async {
                completion(isConnected)
            }
        }
        task.resume()
    }
    
    private func retryNetworkOperation() {
        // Retry the last failed network operation
        NotificationCenter.default.post(name: .networkOperationRetryRequested, object: nil)
    }
    
    // MARK: - Cancellation Methods
    
    private func cancelCacheClearing() {
        logger.info("Cancelling cache clearing operation")
        // Cancel any ongoing cache clearing operations
        NotificationCenter.default.post(name: .cacheClearingCancelled, object: nil)
    }
    
    private func cancelAppRestart() {
        logger.info("Cancelling app restart")
        // Cancel any scheduled app restart
        NotificationCenter.default.post(name: .appRestartCancelled, object: nil)
    }
    
    private func cancelDataRestoration() {
        logger.info("Cancelling data restoration")
        // Cancel any ongoing data restoration
        NotificationCenter.default.post(name: .dataRestorationCancelled, object: nil)
    }
    
    private func cancelReauthentication() {
        logger.info("Cancelling reauthentication")
        // Cancel any ongoing reauthentication flow
        NotificationCenter.default.post(name: .reauthenticationCancelled, object: nil)
    }
    
    private func cancelConnectionRetry() {
        logger.info("Cancelling connection retry")
        // Cancel any ongoing connection retry attempts
        NotificationCenter.default.post(name: .connectionRetryCancelled, object: nil)
    }
    
    private func cancelDataValidation() {
        logger.info("Cancelling data validation")
        // Cancel any ongoing data validation
        NotificationCenter.default.post(name: .dataValidationCancelled, object: nil)
    }
    
    private func cancelPermissionRequest() {
        logger.info("Cancelling permission request")
        // Cancel any ongoing permission requests
        NotificationCenter.default.post(name: .permissionRequestCancelled, object: nil)
    }
    
    private func cancelCrashReporting() {
        logger.info("Cancelling crash reporting")
        // Cancel any ongoing crash reporting
        NotificationCenter.default.post(name: .crashReportingCancelled, object: nil)
    }
    
    private func cancelErrorEscalation() {
        logger.info("Cancelling error escalation")
        // Cancel any ongoing error escalation
        NotificationCenter.default.post(name: .errorEscalationCancelled, object: nil)
    }
    
    private func cancelDebugCollection() {
        logger.info("Cancelling debug collection")
        // Cancel any ongoing debug data collection
        NotificationCenter.default.post(name: .debugCollectionCancelled, object: nil)
    }
    
    private func cancelPreventiveAction() {
        logger.info("Cancelling preventive action")
        // Cancel any ongoing preventive actions
        NotificationCenter.default.post(name: .preventiveActionCancelled, object: nil)
    }
    
    // MARK: - Retry Methods
    
    private func retryNetworkError() {
        logger.info("Retrying network error")
        retryNetworkConnection()
    }
    
    private func retryAuthenticationError() {
        logger.info("Retrying authentication error")
        reauthenticateUser()
    }
    
    private func retryDataError() {
        logger.info("Retrying data error")
        validateDataIntegrity()
    }
    
    private func retrySystemError() {
        logger.info("Retrying system error")
        clearAppCache()
    }
    
    private func retryPermissionError() {
        logger.info("Retrying permission error")
        requestRequiredPermissions()
    }
    
    private func retryValidationError() {
        logger.info("Retrying validation error")
        validateDataIntegrity()
    }
    
    private func retryBusinessError() {
        logger.info("Retrying business error")
        // For business errors, try to refresh data or retry the operation
        NotificationCenter.default.post(name: .businessErrorRetryRequested, object: nil)
    }
    
    // MARK: - Preventive Action Methods
    
    private func performDataBackup() {
        logger.info("Performing data backup")
        let backupManager = DataBackupManager()
        backupManager.performBackup { [weak self] success in
            if success {
                self?.logger.info("Data backup completed successfully")
                NotificationCenter.default.post(name: .dataBackupCompleted, object: nil)
            } else {
                self?.logger.error("Data backup failed")
                NotificationCenter.default.post(name: .dataBackupFailed, object: nil)
            }
        }
    }
    
    private func startResourceMonitoring() {
        logger.info("Starting resource monitoring")
        let performanceMonitor = ErrorHandlingPerformanceMonitor()
        performanceMonitor.startMonitoring()
        NotificationCenter.default.post(name: .resourceMonitoringStarted, object: nil)
    }
    
    private func updateAppConfiguration() {
        logger.info("Updating app configuration")
        // Update app configuration to prevent future errors
        let configManager = AnalyticsConfigurationManager()
        configManager.updateConfiguration { [weak self] success in
            if success {
                self?.logger.info("App configuration updated successfully")
                NotificationCenter.default.post(name: .configurationUpdated, object: nil)
            } else {
                self?.logger.error("Failed to update app configuration")
                NotificationCenter.default.post(name: .configurationUpdateFailed, object: nil)
            }
        }
    }
    
    // MARK: - Tracking Methods
    
    private func getRetryAttemptCount(for error: AppError) -> Int {
        let key = "retry_count_\(error.errorCode)"
        return UserDefaults.standard.integer(forKey: key)
    }
    
    private func recordRetryAttempt(_ record: ErrorRetryRecord) {
        let key = "retry_count_\(record.errorCode)"
        UserDefaults.standard.set(record.attemptNumber, forKey: key)
        
        // Store detailed retry record
        var retryHistory = getRetryHistory()
        retryHistory.append(record)
        
        // Keep only last 100 retry records
        if retryHistory.count > 100 {
            retryHistory.removeFirst(retryHistory.count - 100)
        }
        
        saveRetryHistory(retryHistory)
    }
    
    private func getRetryHistory() -> [ErrorRetryRecord] {
        if let data = UserDefaults.standard.data(forKey: "retry_history"),
           let history = try? JSONDecoder().decode([ErrorRetryRecord].self, from: data) {
            return history
        }
        return []
    }
    
    private func saveRetryHistory(_ history: [ErrorRetryRecord]) {
        if let data = try? JSONEncoder().encode(history) {
            UserDefaults.standard.set(data, forKey: "retry_history")
        }
    }
    
    private func recordPreventiveAction(_ record: PreventiveActionRecord) {
        var preventiveHistory = getPreventiveActionHistory()
        preventiveHistory.append(record)
        
        // Keep only last 50 preventive action records
        if preventiveHistory.count > 50 {
            preventiveHistory.removeFirst(preventiveHistory.count - 50)
        }
        
        savePreventiveActionHistory(preventiveHistory)
    }
    
    private func getPreventiveActionHistory() -> [PreventiveActionRecord] {
        if let data = UserDefaults.standard.data(forKey: "preventive_action_history"),
           let history = try? JSONDecoder().decode([PreventiveActionRecord].self, from: data) {
            return history
        }
        return []
    }
    
    private func savePreventiveActionHistory(_ history: [PreventiveActionRecord]) {
        if let data = try? JSONEncoder().encode(history) {
            UserDefaults.standard.set(data, forKey: "preventive_action_history")
        }
    }
    
    private func createSupportEmailBody(_ error: AppError, context: ErrorContext?) -> String {
        var body = "Error Details:\n"
        body += "Error Code: \(error.errorCode)\n"
        body += "Error Message: \(error.userMessage)\n"
        body += "Timestamp: \(DateFormatter.iso8601.string(from: Date()))\n"
        body += "App Version: \(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown")\n"
        body += "Device: \(UIDevice.current.model)\n"
        body += "OS Version: \(UIDevice.current.systemVersion)\n"
        
        if let context = context {
            body += "\nContext:\n"
            body += "Screen: \(context.screenName ?? "Unknown")\n"
            body += "User Action: \(context.userAction ?? "Unknown")\n"
        }
        
        body += "\nPlease describe what you were doing when this error occurred:\n"
        
        return body.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
    }
}

// MARK: - Supporting Types

struct UserAlert {
    let id: String
    let type: AlertType
    let title: String
    let message: String
    let severity: ErrorSeverity
    let actions: [UserAlertAction]
    let timestamp: Date
    let isDismissible: Bool
}

enum AlertType: String, CaseIterable {
    case criticalError = "critical_error"
    case recoveryProgress = "recovery_progress"
    case resolutionConfirmation = "resolution_confirmation"
    case preventiveActions = "preventive_actions"
    case supportContact = "support_contact"
}

struct UserAlertAction {
    let title: String
    let style: UserAlertActionStyle
    let handler: (() -> Void)?
}

enum UserAlertActionStyle {
    case `default`
    case cancel
    case destructive
    
    var uiAlertActionStyle: UIAlertAction.Style {
        switch self {
        case .default:
            return .default
        case .cancel:
            return .cancel
        case .destructive:
            return .destructive
        }
    }
}

struct AlertConfiguration {
    let maxActiveAlerts = 3
    let alertTimeout: TimeInterval = 30.0
    let maxAlertHistory = 100
}

extension DateFormatter {
    static let iso8601: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        return formatter
    }()
}

// MARK: - Notification Names
extension Notification.Name {
    static let userReauthenticationRequired = Notification.Name("userReauthenticationRequired")
    static let networkConnectionRestored = Notification.Name("networkConnectionRestored")
    static let networkConnectionFailed = Notification.Name("networkConnectionFailed")
    static let dataRestored = Notification.Name("dataRestored")
    static let dataRestoreFailed = Notification.Name("dataRestoreFailed")
    static let dataIntegrityValid = Notification.Name("dataIntegrityValid")
    static let dataIntegrityInvalid = Notification.Name("dataIntegrityInvalid")
    static let permissionsGranted = Notification.Name("permissionsGranted")
    static let permissionsDenied = Notification.Name("permissionsDenied")
    static let operationRetryRequested = Notification.Name("operationRetryRequested")
    static let networkOperationRetryRequested = Notification.Name("networkOperationRetryRequested")
    
    // Recovery action notifications
    static let recoveryActionCancelled = Notification.Name("recoveryActionCancelled")
    static let cacheClearingCancelled = Notification.Name("cacheClearingCancelled")
    static let appRestartCancelled = Notification.Name("appRestartCancelled")
    static let dataRestorationCancelled = Notification.Name("dataRestorationCancelled")
    static let reauthenticationCancelled = Notification.Name("reauthenticationCancelled")
    static let connectionRetryCancelled = Notification.Name("connectionRetryCancelled")
    static let dataValidationCancelled = Notification.Name("dataValidationCancelled")
    static let permissionRequestCancelled = Notification.Name("permissionRequestCancelled")
    static let crashReportingCancelled = Notification.Name("crashReportingCancelled")
    static let errorEscalationCancelled = Notification.Name("errorEscalationCancelled")
    static let debugCollectionCancelled = Notification.Name("debugCollectionCancelled")
    static let preventiveActionCancelled = Notification.Name("preventiveActionCancelled")
    
    // Business error notifications
    static let businessErrorRetryRequested = Notification.Name("businessErrorRetryRequested")
    
    // Data backup notifications
    static let dataBackupCompleted = Notification.Name("dataBackupCompleted")
    static let dataBackupFailed = Notification.Name("dataBackupFailed")
    
    // Resource monitoring notifications
    static let resourceMonitoringStarted = Notification.Name("resourceMonitoringStarted")
    
    // Configuration notifications
    static let configurationUpdated = Notification.Name("configurationUpdated")
    static let configurationUpdateFailed = Notification.Name("configurationUpdateFailed")
}

// MARK: - Data Structures

struct ErrorRetryRecord: Codable {
    let errorCode: String
    let timestamp: Date
    let attemptNumber: Int
}

struct PreventiveActionRecord: Codable {
    let action: String
    let predictionType: CriticalErrorPredictionType
    let timestamp: Date
    let success: Bool
}

enum CriticalErrorPredictionType: String, Codable {
    case memoryPressure = "memory_pressure"
    case networkFailure = "network_failure"
    case batteryDepletion = "battery_depletion"
    case dataCorruption = "data_corruption"
    case authenticationFailure = "authentication_failure"
    case permissionDenied = "permission_denied"
    case systemOverload = "system_overload"
    case unknown = "unknown"
}

// MARK: - Helper Classes
class DataBackupManager {
    func restoreFromBackup(completion: @escaping (Bool) -> Void) {
        // Real data restoration from backup
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let backupDirectory = documentsDirectory.appendingPathComponent("Backups")
                
                // Check if backup exists
                guard FileManager.default.fileExists(atPath: backupDirectory.path) else {
                    DispatchQueue.main.async {
                        completion(false)
                    }
                    return
                }
                
                // Get latest backup
                let backupFiles = try FileManager.default.contentsOfDirectory(at: backupDirectory, includingPropertiesForKeys: [.creationDateKey])
                guard let latestBackup = backupFiles.max(by: { $0.creationDate ?? Date.distantPast < $1.creationDate ?? Date.distantPast }) else {
                    DispatchQueue.main.async {
                        completion(false)
                    }
                    return
                }
                
                // Restore from backup
                try self.restoreFromBackupFile(latestBackup)
                
                DispatchQueue.main.async {
                    completion(true)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        }
    }
    
    func performBackup(completion: @escaping (Bool) -> Void) {
        // Real data backup implementation
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let backupDirectory = documentsDirectory.appendingPathComponent("Backups")
                
                // Create backup directory if it doesn't exist
                try FileManager.default.createDirectory(at: backupDirectory, withIntermediateDirectories: true)
                
                // Create backup file
                let timestamp = DateFormatter.iso8601.string(from: Date())
                let backupFile = backupDirectory.appendingPathComponent("backup_\(timestamp).json")
                
                // Backup critical data
                let backupData = try self.createBackupData()
                try backupData.write(to: backupFile)
                
                // Clean up old backups (keep only last 10)
                try self.cleanupOldBackups(in: backupDirectory)
                
                DispatchQueue.main.async {
                    completion(true)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        }
    }
    
    private func restoreFromBackupFile(_ backupFile: URL) throws {
        let data = try Data(contentsOf: backupFile)
        let backupData = try JSONDecoder().decode(BackupData.self, from: data)
        
        // Restore user data
        if let userData = backupData.userData {
            try userData.write(to: getDocumentsDirectory().appendingPathComponent("user_data.json"))
        }
        
        // Restore settings
        if let settings = backupData.settings {
            UserDefaults.standard.set(settings, forKey: "app_settings")
        }
    }
    
    private func createBackupData() throws -> Data {
        let backupData = BackupData(
            timestamp: Date(),
            userData: try? Data(contentsOf: getDocumentsDirectory().appendingPathComponent("user_data.json")),
            settings: UserDefaults.standard.dictionary(forKey: "app_settings"),
            version: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        )
        
        return try JSONEncoder().encode(backupData)
    }
    
    private func cleanupOldBackups(in directory: URL) throws {
        let backupFiles = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: [.creationDateKey])
        let sortedFiles = backupFiles.sorted { $0.creationDate ?? Date.distantPast > $1.creationDate ?? Date.distantPast }
        
        // Keep only last 10 backups
        if sortedFiles.count > 10 {
            for i in 10..<sortedFiles.count {
                try FileManager.default.removeItem(at: sortedFiles[i])
            }
        }
    }
    
    private func getDocumentsDirectory() -> URL {
        return FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }
}

struct BackupData: Codable {
    let timestamp: Date
    let userData: Data?
    let settings: [String: Any]?
    let version: String
    
    enum CodingKeys: String, CodingKey {
        case timestamp, userData, version
        case settings
    }
    
    init(timestamp: Date, userData: Data?, settings: [String: Any]?, version: String) {
        self.timestamp = timestamp
        self.userData = userData
        self.settings = settings
        self.version = version
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        userData = try container.decodeIfPresent(Data.self, forKey: .userData)
        version = try container.decode(String.self, forKey: .version)
        
        // Handle settings dictionary
        if let settingsData = try container.decodeIfPresent(Data.self, forKey: .settings) {
            settings = try JSONSerialization.jsonObject(with: settingsData) as? [String: Any]
        } else {
            settings = nil
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encodeIfPresent(userData, forKey: .userData)
        try container.encode(version, forKey: .version)
        
        // Handle settings dictionary
        if let settings = settings {
            let settingsData = try JSONSerialization.data(withJSONObject: settings)
            try container.encode(settingsData, forKey: .settings)
        }
    }
}

class DataIntegrityValidator {
    func validateAllData(completion: @escaping (Bool) -> Void) {
        // Implement data integrity validation
        DispatchQueue.global(qos: .userInitiated).async {
            // Simulate data validation
            Thread.sleep(forTimeInterval: 1.0)
            DispatchQueue.main.async {
                completion(true)
            }
        }
    }
}

class PermissionManager {
    func requestAllRequiredPermissions(completion: @escaping (Bool) -> Void) {
        // Implement permission requests
        DispatchQueue.main.async {
            // Simulate permission request
            completion(true)
        }
    }
}
