import Foundation
import UIKit
import os.log

// MARK: - Alert Dialog Manager
/// Manages alert dialogs for critical error notifications with detailed information and actions
final class AlertDialogManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.alerts", category: "AlertDialogManager")
    private let queue = DispatchQueue(label: "com.youthmind.alert.dialog", qos: .userInitiated)
    
    // Dialog configuration
    private let configuration = AlertDialogConfiguration()
    private var activeDialogs: [String: UIAlertController] = [:]
    private var dialogQueue: [AlertDialog] = []
    private var isProcessingQueue = false
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Public Methods
    
    /// Show critical error alert dialog
    func showCriticalErrorDialog(_ error: AppError, context: ErrorContext? = nil) {
        logger.info("Showing critical error dialog for: \(error.errorCode)")
        
        let dialog = createCriticalErrorDialog(error, context: context)
        queue.async { [weak self] in
            self?.processDialog(dialog)
        }
    }
    
    /// Show confirmation dialog
    func showConfirmationDialog(
        title: String,
        message: String,
        confirmTitle: String = "Confirm",
        cancelTitle: String = "Cancel",
        style: UIAlertController.Style = .alert,
        completion: @escaping (Bool) -> Void
    ) {
        logger.debug("Showing confirmation dialog: \(title)")
        
        let dialog = createConfirmationDialog(
            title: title,
            message: message,
            confirmTitle: confirmTitle,
            cancelTitle: cancelTitle,
            style: style,
            completion: completion
        )
        
        queue.async { [weak self] in
            self?.processDialog(dialog)
        }
    }
    
    /// Show action sheet with multiple options
    func showActionSheet(
        title: String?,
        message: String?,
        actions: [AlertAction],
        sourceView: UIView? = nil,
        sourceRect: CGRect? = nil
    ) {
        logger.debug("Showing action sheet: \(title ?? "Untitled")")
        
        let dialog = createActionSheet(
            title: title,
            message: message,
            actions: actions,
            sourceView: sourceView,
            sourceRect: sourceRect
        )
        
        queue.async { [weak self] in
            self?.processDialog(dialog)
        }
    }
    
    /// Show input dialog for user input
    func showInputDialog(
        title: String,
        message: String,
        placeholder: String? = nil,
        keyboardType: UIKeyboardType = .default,
        completion: @escaping (String?) -> Void
    ) {
        logger.debug("Showing input dialog: \(title)")
        
        let dialog = createInputDialog(
            title: title,
            message: message,
            placeholder: placeholder,
            keyboardType: keyboardType,
            completion: completion
        )
        
        queue.async { [weak self] in
            self?.processDialog(dialog)
        }
    }
    
    /// Dismiss specific dialog
    func dismissDialog(_ dialogId: String) {
        logger.debug("Dismissing dialog: \(dialogId)")
        
        queue.async { [weak self] in
            self?.dismissDialogImmediately(dialogId)
        }
    }
    
    /// Dismiss all active dialogs
    func dismissAllDialogs() {
        logger.info("Dismissing all active dialogs")
        
        queue.async { [weak self] in
            self?.dismissAllDialogsImmediately()
        }
    }
    
    // MARK: - Private Methods
    
    private func processDialog(_ dialog: AlertDialog) {
        // Check if we should show this dialog based on user preferences
        guard shouldShowDialog(dialog) else {
            logger.debug("Dialog suppressed by user preferences: \(dialog.id)")
            return
        }
        
        // Add to queue if processing
        if isProcessingQueue {
            dialogQueue.append(dialog)
            return
        }
        
        // Process immediately
        showDialogImmediately(dialog)
    }
    
    private func showDialogImmediately(_ dialog: AlertDialog) {
        isProcessingQueue = true
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Create alert controller
            let alertController = self.createAlertController(for: dialog)
            
            // Present alert
            self.presentAlertController(alertController, dialog: dialog)
            
            // Store reference
            self.activeDialogs[dialog.id] = alertController
            
            // Track dialog display
            self.analyticsTracker.trackDialogDisplay(dialog)
            
            // Process next in queue
            self.processNextDialogInQueue()
        }
    }
    
    private func createCriticalErrorDialog(_ error: AppError, context: ErrorContext?) -> AlertDialog {
        let severity = error.severity
        let title = getTitleForCriticalError(error)
        let message = getDetailedMessageForError(error, context: context)
        
        var actions: [AlertAction] = []
        
        // Add retry action if retryable
        if error.isRetryable {
            actions.append(AlertAction(
                title: "Retry",
                style: .default,
                handler: { [weak self] in
                    self?.handleRetryAction(for: error, context: context)
                }
            ))
        }
        
        // Add report action for critical errors
        if severity == .critical {
            actions.append(AlertAction(
                title: "Report Issue",
                style: .default,
                handler: { [weak self] in
                    self?.handleReportAction(for: error, context: context)
                }
            ))
        }
        
        // Add help action
        actions.append(AlertAction(
            title: "Get Help",
            style: .default,
            handler: { [weak self] in
                self?.handleHelpAction(for: error, context: context)
            }
        ))
        
        // Add dismiss action
        actions.append(AlertAction(
            title: "Dismiss",
            style: .cancel,
            handler: { [weak self] in
                self?.handleDismissAction(for: error, context: context)
            }
        ))
        
        return AlertDialog(
            id: UUID().uuidString,
            type: .criticalError,
            title: title,
            message: message,
            style: .alert,
            actions: actions,
            context: context,
            timestamp: Date(),
            priority: getPriorityForSeverity(severity)
        )
    }
    
    private func createConfirmationDialog(
        title: String,
        message: String,
        confirmTitle: String,
        cancelTitle: String,
        style: UIAlertController.Style,
        completion: @escaping (Bool) -> Void
    ) -> AlertDialog {
        
        let actions = [
            AlertAction(
                title: confirmTitle,
                style: .default,
                handler: { _ in completion(true) }
            ),
            AlertAction(
                title: cancelTitle,
                style: .cancel,
                handler: { _ in completion(false) }
            )
        ]
        
        return AlertDialog(
            id: UUID().uuidString,
            type: .confirmation,
            title: title,
            message: message,
            style: style,
            actions: actions,
            context: nil,
            timestamp: Date(),
            priority: .medium
        )
    }
    
    private func createActionSheet(
        title: String?,
        message: String?,
        actions: [AlertAction],
        sourceView: UIView?,
        sourceRect: CGRect?
    ) -> AlertDialog {
        
        return AlertDialog(
            id: UUID().uuidString,
            type: .actionSheet,
            title: title,
            message: message,
            style: .actionSheet,
            actions: actions,
            context: nil,
            timestamp: Date(),
            priority: .medium,
            sourceView: sourceView,
            sourceRect: sourceRect
        )
    }
    
    private func createInputDialog(
        title: String,
        message: String,
        placeholder: String?,
        keyboardType: UIKeyboardType,
        completion: @escaping (String?) -> Void
    ) -> AlertDialog {
        
        let actions = [
            AlertAction(
                title: "OK",
                style: .default,
                handler: { _ in
                    // Input will be handled by the text field
                    completion(nil) // This will be updated when text field is configured
                }
            ),
            AlertAction(
                title: "Cancel",
                style: .cancel,
                handler: { _ in completion(nil) }
            )
        ]
        
        return AlertDialog(
            id: UUID().uuidString,
            type: .input,
            title: title,
            message: message,
            style: .alert,
            actions: actions,
            context: nil,
            timestamp: Date(),
            priority: .medium,
            inputConfiguration: InputConfiguration(
                placeholder: placeholder,
                keyboardType: keyboardType,
                completion: completion
            )
        )
    }
    
    private func createAlertController(for dialog: AlertDialog) -> UIAlertController {
        let alertController = UIAlertController(
            title: dialog.title,
            message: dialog.message,
            preferredStyle: dialog.style
        )
        
        // Configure input if needed
        if let inputConfig = dialog.inputConfiguration {
            alertController.addTextField { textField in
                textField.placeholder = inputConfig.placeholder
                textField.keyboardType = inputConfig.keyboardType
            }
        }
        
        // Add actions
        for action in dialog.actions {
            let alertAction = UIAlertAction(title: action.title, style: action.style) { _ in
                action.handler(action)
                
                // Handle input if present
                if let inputConfig = dialog.inputConfiguration,
                   let textField = alertController.textFields?.first {
                    inputConfig.completion(textField.text)
                }
                
                // Track action
                self.analyticsTracker.trackDialogAction(dialog, action: action)
            }
            alertController.addAction(alertAction)
        }
        
        // Configure for iPad
        if let popover = alertController.popoverPresentationController {
            if let sourceView = dialog.sourceView {
                popover.sourceView = sourceView
                popover.sourceRect = dialog.sourceRect ?? sourceView.bounds
            } else {
                popover.sourceView = UIApplication.shared.windows.first
                popover.sourceRect = CGRect(x: 0, y: 0, width: 1, height: 1)
            }
        }
        
        return alertController
    }
    
    private func presentAlertController(_ alertController: UIAlertController, dialog: AlertDialog) {
        guard let window = UIApplication.shared.windows.first,
              let rootViewController = window.rootViewController else {
            logger.error("No root view controller available to present dialog")
            return
        }
        
        // Find the topmost presented view controller
        var topController = rootViewController
        while let presentedController = topController.presentedViewController {
            topController = presentedController
        }
        
        topController.present(alertController, animated: true) {
            self.logger.debug("Dialog presented successfully: \(dialog.id)")
        }
    }
    
    private func dismissDialogImmediately(_ dialogId: String) {
        guard let alertController = activeDialogs[dialogId] else { return }
        
        activeDialogs.removeValue(forKey: dialogId)
        
        DispatchQueue.main.async {
            alertController.dismiss(animated: true) {
                self.processNextDialogInQueue()
            }
        }
    }
    
    private func dismissAllDialogsImmediately() {
        let allDialogIds = Array(activeDialogs.keys)
        
        for dialogId in allDialogIds {
            dismissDialogImmediately(dialogId)
        }
        
        dialogQueue.removeAll()
    }
    
    private func processNextDialogInQueue() {
        isProcessingQueue = false
        
        guard !dialogQueue.isEmpty else { return }
        
        let nextDialog = dialogQueue.removeFirst()
        showDialogImmediately(nextDialog)
    }
    
    private func shouldShowDialog(_ dialog: AlertDialog) -> Bool {
        // Check user preferences
        let showDialogs = UserDefaults.standard.bool(forKey: "ShowAlertDialogs")
        guard showDialogs else { return false }
        
        // Check priority threshold
        let minPriority = DialogPriority(rawValue: UserDefaults.standard.string(forKey: "MinDialogPriority") ?? "medium") ?? .medium
        guard dialog.priority.rawValue >= minPriority.rawValue else { return false }
        
        // Check rate limiting
        return !isRateLimited(dialog)
    }
    
    private func isRateLimited(_ dialog: AlertDialog) -> Bool {
        let maxDialogsPerMinute = UserDefaults.standard.integer(forKey: "MaxDialogsPerMinute")
        guard maxDialogsPerMinute > 0 else { return false }
        
        let oneMinuteAgo = Date().addingTimeInterval(-60)
        let recentDialogs = activeDialogs.values.filter { $0.title != nil } // Simple check for recent dialogs
        
        return recentDialogs.count >= maxDialogsPerMinute
    }
    
    // MARK: - Action Handlers
    
    private func handleRetryAction(for error: AppError, context: ErrorContext?) {
        logger.info("Handling retry action for error: \(error.errorCode)")
        
        let retryHandler = RetryButtonHandler()
        retryHandler.handleRetry(for: error, context: context)
    }
    
    private func handleReportAction(for error: AppError, context: ErrorContext?) {
        logger.info("Handling report action for error: \(error.errorCode)")
        
        let escalationService = ErrorEscalationService()
        escalationService.escalateError(error)
    }
    
    private func handleHelpAction(for error: AppError, context: ErrorContext?) {
        logger.info("Handling help action for error: \(error.errorCode)")
        
        // Navigate to help section
        let navigator = ErrorRecoveryNavigator()
        navigator.navigateToHelp(for: error, context: context)
    }
    
    private func handleDismissAction(for error: AppError, context: ErrorContext?) {
        logger.info("Handling dismiss action for error: \(error.errorCode)")
        
        // Track dismissal
        analyticsTracker.trackDialogDismissal(error, context: context)
    }
    
    // MARK: - Helper Methods
    
    private func getTitleForCriticalError(_ error: AppError) -> String {
        switch error {
        case .network: return "Network Connection Error"
        case .authentication: return "Authentication Failed"
        case .authorization: return "Access Denied"
        case .dataValidation: return "Data Validation Error"
        case .dataNotFound: return "Data Not Found"
        case .dataCorrupted: return "Data Corruption Detected"
        case .userInput: return "Invalid Input"
        case .invalidOperation: return "Operation Failed"
        case .system: return "System Error"
        case .configuration: return "Configuration Error"
        case .business: return "Application Error"
        }
    }
    
    private func getDetailedMessageForError(_ error: AppError, context: ErrorContext?) -> String {
        var message = error.errorDescription ?? "An unexpected error occurred"
        
        // Add context information if available
        if let context = context {
            if let screenName = context.screenName {
                message += "\n\nScreen: \(screenName)"
            }
            if let userId = context.userId {
                message += "\nUser ID: \(userId)"
            }
        }
        
        // Add suggestions based on error type
        let suggestions = getSuggestionsForError(error)
        if !suggestions.isEmpty {
            message += "\n\nSuggestions:\n• " + suggestions.joined(separator: "\n• ")
        }
        
        return message
    }
    
    private func getSuggestionsForError(_ error: AppError) -> [String] {
        switch error {
        case .network:
            return [
                "Check your internet connection",
                "Try again in a few moments",
                "Contact support if the problem persists"
            ]
        case .authentication:
            return [
                "Verify your login credentials",
                "Try logging in again",
                "Reset your password if needed"
            ]
        case .authorization:
            return [
                "Check your account permissions",
                "Contact your administrator",
                "Verify your subscription status"
            ]
        case .dataValidation:
            return [
                "Check your input format",
                "Ensure all required fields are filled",
                "Verify data types and ranges"
            ]
        case .dataNotFound:
            return [
                "Refresh the page",
                "Check if the resource still exists",
                "Try searching again"
            ]
        case .dataCorrupted:
            return [
                "Try refreshing the data",
                "Clear app cache",
                "Contact support for data recovery"
            ]
        case .userInput:
            return [
                "Check your input format",
                "Ensure all required fields are filled",
                "Follow the input guidelines"
            ]
        case .invalidOperation:
            return [
                "Check if the operation is allowed",
                "Verify prerequisites are met",
                "Try a different approach"
            ]
        case .system:
            return [
                "Restart the application",
                "Check system resources",
                "Contact support if the problem persists"
            ]
        case .configuration:
            return [
                "Check app settings",
                "Reset to default configuration",
                "Contact support for configuration help"
            ]
        case .business:
            return [
                "Check your account status",
                "Verify business rules",
                "Contact support for assistance"
            ]
        }
    }
    
    private func getPriorityForSeverity(_ severity: AppError.ErrorSeverity) -> DialogPriority {
        switch severity {
        case .low: return .low
        case .medium: return .medium
        case .high: return .high
        case .critical: return .critical
        }
    }
}

// MARK: - Supporting Types

struct AlertDialog {
    let id: String
    let type: DialogType
    let title: String?
    let message: String?
    let style: UIAlertController.Style
    let actions: [AlertAction]
    let context: ErrorContext?
    let timestamp: Date
    let priority: DialogPriority
    let sourceView: UIView?
    let sourceRect: CGRect?
    let inputConfiguration: InputConfiguration?
    
    init(
        id: String,
        type: DialogType,
        title: String?,
        message: String?,
        style: UIAlertController.Style,
        actions: [AlertAction],
        context: ErrorContext?,
        timestamp: Date,
        priority: DialogPriority,
        sourceView: UIView? = nil,
        sourceRect: CGRect? = nil,
        inputConfiguration: InputConfiguration? = nil
    ) {
        self.id = id
        self.type = type
        self.title = title
        self.message = message
        self.style = style
        self.actions = actions
        self.context = context
        self.timestamp = timestamp
        self.priority = priority
        self.sourceView = sourceView
        self.sourceRect = sourceRect
        self.inputConfiguration = inputConfiguration
    }
}

enum DialogType {
    case criticalError
    case confirmation
    case actionSheet
    case input
}

struct AlertAction {
    let title: String
    let style: UIAlertAction.Style
    let handler: (AlertAction) -> Void
}

enum DialogPriority: Int, CaseIterable {
    case low = 0
    case medium = 1
    case high = 2
    case critical = 3
}

struct InputConfiguration {
    let placeholder: String?
    let keyboardType: UIKeyboardType
    let completion: (String?) -> Void
}

struct AlertDialogConfiguration {
    let maxConcurrentDialogs = 1
    let defaultTimeout: TimeInterval = 30.0
    let animationDuration: TimeInterval = 0.3
}
