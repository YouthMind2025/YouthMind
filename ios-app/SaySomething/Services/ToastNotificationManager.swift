import Foundation
import UIKit
import os.log

// MARK: - Toast Notification Manager
/// Manages toast notifications for error messages with intelligent display logic
final class ToastNotificationManager {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.notifications", category: "ToastNotificationManager")
    private let queue = DispatchQueue(label: "com.youthmind.toast.manager", qos: .userInitiated)
    
    // Notification configuration
    private let configuration = ToastConfiguration()
    private var activeToasts: [String: ToastView] = [:]
    private var toastQueue: [ToastNotification] = []
    private var isProcessingQueue = false
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Public Methods
    
    /// Show toast notification for error
    func showErrorToast(_ error: AppError, context: ErrorContext? = nil) {
        logger.info("Showing error toast for: \(error.errorCode)")
        
        let notification = createErrorToast(error, context: context)
        queue.async { [weak self] in
            self?.processToastNotification(notification)
        }
    }
    
    /// Show success toast notification
    func showSuccessToast(_ message: String, duration: TimeInterval = 3.0) {
        logger.debug("Showing success toast: \(message)")
        
        let notification = createSuccessToast(message, duration: duration)
        queue.async { [weak self] in
            self?.processToastNotification(notification)
        }
    }
    
    /// Show warning toast notification
    func showWarningToast(_ message: String, duration: TimeInterval = 4.0) {
        logger.debug("Showing warning toast: \(message)")
        
        let notification = createWarningToast(message, duration: duration)
        queue.async { [weak self] in
            self?.processToastNotification(notification)
        }
    }
    
    /// Show info toast notification
    func showInfoToast(_ message: String, duration: TimeInterval = 3.0) {
        logger.debug("Showing info toast: \(message)")
        
        let notification = createInfoToast(message, duration: duration)
        queue.async { [weak self] in
            self?.processToastNotification(notification)
        }
    }
    
    /// Dismiss specific toast
    func dismissToast(_ toastId: String) {
        logger.debug("Dismissing toast: \(toastId)")
        
        queue.async { [weak self] in
            self?.dismissToastImmediately(toastId)
        }
    }
    
    /// Dismiss all active toasts
    func dismissAllToasts() {
        logger.info("Dismissing all active toasts")
        
        queue.async { [weak self] in
            self?.dismissAllToastsImmediately()
        }
    }
    
    // MARK: - Private Methods
    
    private func processToastNotification(_ notification: ToastNotification) {
        // Check if we should show this notification based on user preferences
        guard shouldShowNotification(notification) else {
            logger.debug("Notification suppressed by user preferences: \(notification.id)")
            return
        }
        
        // Add to queue if processing
        if isProcessingQueue {
            toastQueue.append(notification)
            return
        }
        
        // Process immediately
        showToastImmediately(notification)
    }
    
    private func showToastImmediately(_ notification: ToastNotification) {
        isProcessingQueue = true
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Create and configure toast view
            let toastView = self.createToastView(for: notification)
            
            // Position toast
            self.positionToast(toastView, notification: notification)
            
            // Add to view hierarchy
            self.addToastToViewHierarchy(toastView)
            
            // Store reference
            self.activeToasts[notification.id] = toastView
            
            // Animate in
            self.animateToastIn(toastView) {
                // Start auto-dismiss timer
                self.startAutoDismissTimer(for: notification)
            }
            
            // Track notification display
            self.analyticsTracker.trackNotificationDisplay(notification)
            
            // Process next in queue
            self.processNextToastInQueue()
        }
    }
    
    private func createErrorToast(_ error: AppError, context: ErrorContext?) -> ToastNotification {
        let severity = error.severity
        let message = error.errorDescription ?? "An error occurred"
        let duration = getDurationForSeverity(severity)
        
        return ToastNotification(
            id: UUID().uuidString,
            type: .error,
            title: getTitleForError(error),
            message: message,
            duration: duration,
            severity: severity,
            context: context,
            timestamp: Date(),
            retryable: error.isRetryable,
            actions: getActionsForError(error)
        )
    }
    
    private func createSuccessToast(_ message: String, duration: TimeInterval) -> ToastNotification {
        return ToastNotification(
            id: UUID().uuidString,
            type: .success,
            title: "Success",
            message: message,
            duration: duration,
            severity: .low,
            context: nil,
            timestamp: Date(),
            retryable: false,
            actions: []
        )
    }
    
    private func createWarningToast(_ message: String, duration: TimeInterval) -> ToastNotification {
        return ToastNotification(
            id: UUID().uuidString,
            type: .warning,
            title: "Warning",
            message: message,
            duration: duration,
            severity: .medium,
            context: nil,
            timestamp: Date(),
            retryable: false,
            actions: []
        )
    }
    
    private func createInfoToast(_ message: String, duration: TimeInterval) -> ToastNotification {
        return ToastNotification(
            id: UUID().uuidString,
            type: .info,
            title: "Information",
            message: message,
            duration: duration,
            severity: .low,
            context: nil,
            timestamp: Date(),
            retryable: false,
            actions: []
        )
    }
    
    private func createToastView(for notification: ToastNotification) -> ToastView {
        let toastView = ToastView()
        toastView.configure(with: notification)
        toastView.delegate = self
        return toastView
    }
    
    private func positionToast(_ toastView: ToastView, notification: ToastNotification) {
        guard let window = UIApplication.shared.windows.first else { return }
        
        let safeAreaInsets = window.safeAreaInsets
        let screenWidth = window.bounds.width
        let toastWidth = min(screenWidth - 32, 400)
        
        // Calculate position based on notification type and existing toasts
        let yPosition = calculateYPosition(for: notification)
        
        toastView.frame = CGRect(
            x: (screenWidth - toastWidth) / 2,
            y: yPosition,
            width: toastWidth,
            height: 0 // Will be calculated by auto layout
        )
        
        // Set up auto layout constraints
        toastView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            toastView.centerXAnchor.constraint(equalTo: window.centerXAnchor),
            toastView.topAnchor.constraint(equalTo: window.topAnchor, constant: yPosition),
            toastView.widthAnchor.constraint(lessThanOrEqualToConstant: toastWidth)
        ])
    }
    
    private func calculateYPosition(for notification: ToastNotification) -> CGFloat {
        let safeAreaTop = UIApplication.shared.windows.first?.safeAreaInsets.top ?? 0
        let baseY = safeAreaTop + 20
        
        // Stack toasts vertically
        let existingToastCount = activeToasts.count
        let toastHeight = 60.0
        let spacing = 8.0
        
        return baseY + CGFloat(existingToastCount) * (toastHeight + spacing)
    }
    
    private func addToastToViewHierarchy(_ toastView: ToastView) {
        guard let window = UIApplication.shared.windows.first else { return }
        window.addSubview(toastView)
    }
    
    private func animateToastIn(_ toastView: ToastView, completion: @escaping () -> Void) {
        toastView.alpha = 0
        toastView.transform = CGAffineTransform(translationX: 0, y: -20)
        
        UIView.animate(withDuration: 0.3, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0.5, options: [.curveEaseOut], animations: {
            toastView.alpha = 1
            toastView.transform = .identity
        }, completion: { _ in
            completion()
        })
    }
    
    private func animateToastOut(_ toastView: ToastView, completion: @escaping () -> Void) {
        UIView.animate(withDuration: 0.2, delay: 0, options: [.curveEaseIn], animations: {
            toastView.alpha = 0
            toastView.transform = CGAffineTransform(translationX: 0, y: -20)
        }, completion: { _ in
            toastView.removeFromSuperview()
            completion()
        })
    }
    
    private func startAutoDismissTimer(for notification: ToastNotification) {
        guard notification.duration > 0 else { return }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + notification.duration) { [weak self] in
            self?.dismissToast(notification.id)
        }
    }
    
    private func dismissToastImmediately(_ toastId: String) {
        guard let toastView = activeToasts[toastId] else { return }
        
        activeToasts.removeValue(forKey: toastId)
        
        DispatchQueue.main.async { [weak self] in
            self?.animateToastOut(toastView) {
                self?.processNextToastInQueue()
            }
        }
    }
    
    private func dismissAllToastsImmediately() {
        let allToastIds = Array(activeToasts.keys)
        
        for toastId in allToastIds {
            dismissToastImmediately(toastId)
        }
        
        toastQueue.removeAll()
    }
    
    private func processNextToastInQueue() {
        isProcessingQueue = false
        
        guard !toastQueue.isEmpty else { return }
        
        let nextNotification = toastQueue.removeFirst()
        showToastImmediately(nextNotification)
    }
    
    private func shouldShowNotification(_ notification: ToastNotification) -> Bool {
        // Check user preferences
        let showToasts = UserDefaults.standard.bool(forKey: "ShowToastNotifications")
        guard showToasts else { return false }
        
        // Check severity threshold
        let minSeverity = AppError.ErrorSeverity(rawValue: UserDefaults.standard.string(forKey: "MinToastSeverity") ?? "low") ?? .low
        guard notification.severity.rawValue >= minSeverity.rawValue else { return false }
        
        // Check rate limiting
        return !isRateLimited(notification)
    }
    
    private func isRateLimited(_ notification: ToastNotification) -> Bool {
        let maxToastsPerMinute = UserDefaults.standard.integer(forKey: "MaxToastsPerMinute")
        guard maxToastsPerMinute > 0 else { return false }
        
        let oneMinuteAgo = Date().addingTimeInterval(-60)
        let recentToasts = activeToasts.values.filter { $0.notification.timestamp > oneMinuteAgo }
        
        return recentToasts.count >= maxToastsPerMinute
    }
    
    private func getDurationForSeverity(_ severity: AppError.ErrorSeverity) -> TimeInterval {
        switch severity {
        case .low: return 3.0
        case .medium: return 4.0
        case .high: return 5.0
        case .critical: return 6.0
        }
    }
    
    private func getTitleForError(_ error: AppError) -> String {
        switch error {
        case .network: return "Network Error"
        case .authentication: return "Authentication Error"
        case .authorization: return "Authorization Error"
        case .dataValidation: return "Validation Error"
        case .dataNotFound: return "Data Not Found"
        case .dataCorrupted: return "Data Error"
        case .userInput: return "Input Error"
        case .invalidOperation: return "Operation Error"
        case .system: return "System Error"
        case .configuration: return "Configuration Error"
        case .business: return "Business Error"
        }
    }
    
    private func getActionsForError(_ error: AppError) -> [ToastAction] {
        var actions: [ToastAction] = []
        
        if error.isRetryable {
            actions.append(ToastAction(title: "Retry", type: .retry, style: .primary))
        }
        
        if error.severity == .critical {
            actions.append(ToastAction(title: "Report", type: .report, style: .secondary))
        }
        
        actions.append(ToastAction(title: "Dismiss", type: .dismiss, style: .tertiary))
        
        return actions
    }
}

// MARK: - ToastNotificationManager + ToastViewDelegate
extension ToastNotificationManager: ToastViewDelegate {
    func toastView(_ toastView: ToastView, didSelectAction action: ToastAction) {
        logger.debug("Toast action selected: \(action.type.rawValue)")
        
        // Track action
        analyticsTracker.trackNotificationAction(toastView.notification, action: action)
        
        // Handle action
        switch action.type {
        case .retry:
            handleRetryAction(toastView.notification)
        case .report:
            handleReportAction(toastView.notification)
        case .dismiss:
            dismissToast(toastView.notification.id)
        }
    }
    
    private func handleRetryAction(_ notification: ToastNotification) {
        // Delegate to retry button handler
        let retryHandler = RetryButtonHandler()
        retryHandler.handleRetry(for: notification)
    }
    
    private func handleReportAction(_ notification: ToastNotification) {
        // Delegate to error escalation service
        let escalationService = ErrorEscalationService()
        escalationService.escalateError(notification.context?.error ?? AppError.system(message: "Unknown error"))
    }
}

// MARK: - Supporting Types

struct ToastNotification: Codable {
    let id: String
    let type: ToastType
    let title: String
    let message: String
    let duration: TimeInterval
    let severity: AppError.ErrorSeverity
    let context: ErrorContext?
    let timestamp: Date
    let retryable: Bool
    let actions: [ToastAction]
}

enum ToastType: String, Codable {
    case error
    case success
    case warning
    case info
}

struct ToastAction: Codable {
    let title: String
    let type: ToastActionType
    let style: ToastActionStyle
}

enum ToastActionType: String, Codable {
    case retry
    case report
    case dismiss
}

enum ToastActionStyle: String, Codable {
    case primary
    case secondary
    case tertiary
}

struct ToastConfiguration {
    let maxConcurrentToasts = 3
    let defaultDuration: TimeInterval = 3.0
    let animationDuration: TimeInterval = 0.3
    let spacing: CGFloat = 8.0
}

// MARK: - ToastView Protocol
protocol ToastViewDelegate: AnyObject {
    func toastView(_ toastView: ToastView, didSelectAction action: ToastAction)
}

// MARK: - ToastView
class ToastView: UIView {
    
    // MARK: - Properties
    weak var delegate: ToastViewDelegate?
    var notification: ToastNotification!
    
    private let titleLabel = UILabel()
    private let messageLabel = UILabel()
    private let actionStackView = UIStackView()
    private let iconImageView = UIImageView()
    
    // MARK: - Initialization
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupUI()
    }
    
    // MARK: - Configuration
    func configure(with notification: ToastNotification) {
        self.notification = notification
        
        titleLabel.text = notification.title
        messageLabel.text = notification.message
        
        // Configure appearance based on type
        configureAppearance(for: notification.type)
        
        // Configure actions
        configureActions(notification.actions)
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        backgroundColor = UIColor.systemBackground
        layer.cornerRadius = 12
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOffset = CGSize(width: 0, height: 2)
        layer.shadowRadius = 8
        layer.shadowOpacity = 0.1
        
        setupSubviews()
        setupConstraints()
    }
    
    private func setupSubviews() {
        addSubview(iconImageView)
        addSubview(titleLabel)
        addSubview(messageLabel)
        addSubview(actionStackView)
        
        // Configure title label
        titleLabel.font = UIFont.boldSystemFont(ofSize: 16)
        titleLabel.numberOfLines = 1
        
        // Configure message label
        messageLabel.font = UIFont.systemFont(ofSize: 14)
        messageLabel.numberOfLines = 0
        
        // Configure action stack view
        actionStackView.axis = .horizontal
        actionStackView.spacing = 8
        actionStackView.distribution = .fillEqually
        
        // Configure icon
        iconImageView.contentMode = .scaleAspectFit
        iconImageView.tintColor = UIColor.systemBlue
    }
    
    private func setupConstraints() {
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        actionStackView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // Icon
            iconImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            iconImageView.topAnchor.constraint(equalTo: topAnchor, constant: 16),
            iconImageView.widthAnchor.constraint(equalToConstant: 24),
            iconImageView.heightAnchor.constraint(equalToConstant: 24),
            
            // Title
            titleLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: 12),
            titleLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: 16),
            
            // Message
            messageLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            messageLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
            messageLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            
            // Actions
            actionStackView.leadingAnchor.constraint(equalTo: messageLabel.leadingAnchor),
            actionStackView.trailingAnchor.constraint(equalTo: messageLabel.trailingAnchor),
            actionStackView.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 12),
            actionStackView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -16)
        ])
    }
    
    private func configureAppearance(for type: ToastType) {
        switch type {
        case .error:
            backgroundColor = UIColor.systemRed.withAlphaComponent(0.1)
            titleLabel.textColor = UIColor.systemRed
            messageLabel.textColor = UIColor.systemRed
            iconImageView.image = UIImage(systemName: "exclamationmark.triangle.fill")
            iconImageView.tintColor = UIColor.systemRed
            
        case .success:
            backgroundColor = UIColor.systemGreen.withAlphaComponent(0.1)
            titleLabel.textColor = UIColor.systemGreen
            messageLabel.textColor = UIColor.systemGreen
            iconImageView.image = UIImage(systemName: "checkmark.circle.fill")
            iconImageView.tintColor = UIColor.systemGreen
            
        case .warning:
            backgroundColor = UIColor.systemOrange.withAlphaComponent(0.1)
            titleLabel.textColor = UIColor.systemOrange
            messageLabel.textColor = UIColor.systemOrange
            iconImageView.image = UIImage(systemName: "exclamationmark.triangle.fill")
            iconImageView.tintColor = UIColor.systemOrange
            
        case .info:
            backgroundColor = UIColor.systemBlue.withAlphaComponent(0.1)
            titleLabel.textColor = UIColor.systemBlue
            messageLabel.textColor = UIColor.systemBlue
            iconImageView.image = UIImage(systemName: "info.circle.fill")
            iconImageView.tintColor = UIColor.systemBlue
        }
    }
    
    private func configureActions(_ actions: [ToastAction]) {
        // Clear existing actions
        actionStackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        
        for action in actions {
            let button = createActionButton(for: action)
            actionStackView.addArrangedSubview(button)
        }
    }
    
    private func createActionButton(for action: ToastAction) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(action.title, for: .normal)
        button.addTarget(self, action: #selector(actionButtonTapped(_:)), for: .touchUpInside)
        
        // Configure style
        switch action.style {
        case .primary:
            button.backgroundColor = UIColor.systemBlue
            button.setTitleColor(.white, for: .normal)
            button.layer.cornerRadius = 8
            
        case .secondary:
            button.backgroundColor = UIColor.systemGray5
            button.setTitleColor(.label, for: .normal)
            button.layer.cornerRadius = 8
            
        case .tertiary:
            button.backgroundColor = .clear
            button.setTitleColor(.systemBlue, for: .normal)
        }
        
        // Store action type
        button.tag = action.type.hashValue
        
        return button
    }
    
    @objc private func actionButtonTapped(_ sender: UIButton) {
        guard let actionType = ToastActionType.allCases.first(where: { $0.hashValue == sender.tag }) else { return }
        
        let action = ToastAction(title: sender.title(for: .normal) ?? "", type: actionType, style: .primary)
        delegate?.toastView(self, didSelectAction: action)
    }
}

// MARK: - ToastActionType Extension
extension ToastActionType: CaseIterable {
    static var allCases: [ToastActionType] {
        return [.retry, .report, .dismiss]
    }
}
