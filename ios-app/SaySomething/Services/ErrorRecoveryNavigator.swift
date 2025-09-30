import Foundation
import UIKit
import os.log

// MARK: - Error Recovery Navigator
/// Manages navigation and user flow during error recovery scenarios
final class ErrorRecoveryNavigator {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.navigation", category: "ErrorRecoveryNavigator")
    private let queue = DispatchQueue(label: "com.youthmind.navigation.recovery", qos: .userInitiated)
    
    // Navigation configuration
    private let configuration = NavigationConfiguration()
    private var navigationHistory: [NavigationEvent] = []
    private var currentRecoveryFlow: RecoveryFlow?
    
    // Analytics integration
    private let analyticsTracker = NotificationAnalyticsTracker()
    
    // MARK: - Public Methods
    
    /// Navigate to help section for specific error
    func navigateToHelp(for error: AppError, context: ErrorContext? = nil) {
        logger.info("Navigating to help for error: \(error.errorCode)")
        
        let helpFlow = createHelpFlow(for: error, context: context)
        executeNavigation(helpFlow)
    }
    
    /// Navigate to settings for error configuration
    func navigateToSettings(for error: AppError, context: ErrorContext? = nil) {
        logger.info("Navigating to settings for error: \(error.errorCode)")
        
        let settingsFlow = createSettingsFlow(for: error, context: context)
        executeNavigation(settingsFlow)
    }
    
    /// Navigate to support for error reporting
    func navigateToSupport(for error: AppError, context: ErrorContext? = nil) {
        logger.info("Navigating to support for error: \(error.errorCode)")
        
        let supportFlow = createSupportFlow(for: error, context: context)
        executeNavigation(supportFlow)
    }
    
    /// Navigate to recovery flow for error
    func navigateToRecoveryFlow(for error: AppError, context: ErrorContext? = nil) {
        logger.info("Navigating to recovery flow for error: \(error.errorCode)")
        
        let recoveryFlow = createRecoveryFlow(for: error, context: context)
        executeNavigation(recoveryFlow)
    }
    
    /// Navigate back in recovery flow
    func navigateBack() {
        logger.debug("Navigating back in recovery flow")
        
        guard let flow = currentRecoveryFlow else {
            logger.warning("No active recovery flow to navigate back from")
            return
        }
        
        flow.navigateBack()
    }
    
    /// Complete current recovery flow
    func completeRecoveryFlow() {
        logger.info("Completing recovery flow")
        
        guard let flow = currentRecoveryFlow else {
            logger.warning("No active recovery flow to complete")
            return
        }
        
        flow.complete()
        currentRecoveryFlow = nil
    }
    
    /// Get current recovery flow status
    func getCurrentFlowStatus() -> RecoveryFlowStatus? {
        return currentRecoveryFlow?.status
    }
    
    // MARK: - Private Methods
    
    private func executeNavigation(_ flow: RecoveryFlow) {
        queue.async { [weak self] in
            self?.processNavigation(flow)
        }
    }
    
    private func processNavigation(_ flow: RecoveryFlow) {
        // Track navigation start
        analyticsTracker.trackNavigationStart(flow)
        
        // Set current flow
        currentRecoveryFlow = flow
        
        // Execute navigation on main thread
        DispatchQueue.main.async { [weak self] in
            self?.performNavigation(flow)
        }
    }
    
    private func performNavigation(_ flow: RecoveryFlow) {
        logger.debug("Performing navigation: \(flow.type.rawValue)")
        
        // Record navigation event
        let event = NavigationEvent(
            id: UUID().uuidString,
            flow: flow,
            timestamp: Date(),
            action: .start
        )
        navigationHistory.append(event)
        
        // Execute navigation based on flow type
        switch flow.type {
        case .help:
            navigateToHelpScreen(flow)
        case .settings:
            navigateToSettingsScreen(flow)
        case .support:
            navigateToSupportScreen(flow)
        case .recovery:
            navigateToRecoveryScreen(flow)
        case .troubleshooting:
            navigateToTroubleshootingScreen(flow)
        case .feedback:
            navigateToFeedbackScreen(flow)
        }
    }
    
    // MARK: - Flow Creation Methods
    
    private func createHelpFlow(for error: AppError, context: ErrorContext?) -> RecoveryFlow {
        let steps = createHelpSteps(for: error, context: context)
        
        return RecoveryFlow(
            id: UUID().uuidString,
            type: .help,
            title: "Get Help",
            description: "Find solutions for your error",
            steps: steps,
            context: context,
            timestamp: Date()
        )
    }
    
    private func createSettingsFlow(for error: AppError, context: ErrorContext?) -> RecoveryFlow {
        let steps = createSettingsSteps(for: error, context: context)
        
        return RecoveryFlow(
            id: UUID().uuidString,
            type: .settings,
            title: "Settings",
            description: "Configure app settings",
            steps: steps,
            context: context,
            timestamp: Date()
        )
    }
    
    private func createSupportFlow(for error: AppError, context: ErrorContext?) -> RecoveryFlow {
        let steps = createSupportSteps(for: error, context: context)
        
        return RecoveryFlow(
            id: UUID().uuidString,
            type: .support,
            title: "Contact Support",
            description: "Get help from our support team",
            steps: steps,
            context: context,
            timestamp: Date()
        )
    }
    
    private func createRecoveryFlow(for error: AppError, context: ErrorContext?) -> RecoveryFlow {
        let steps = createRecoverySteps(for: error, context: context)
        
        return RecoveryFlow(
            id: UUID().uuidString,
            type: .recovery,
            title: "Error Recovery",
            description: "Follow these steps to resolve the error",
            steps: steps,
            context: context,
            timestamp: Date()
        )
    }
    
    // MARK: - Step Creation Methods
    
    private func createHelpSteps(for error: AppError, context: ErrorContext?) -> [RecoveryStep] {
        var steps: [RecoveryStep] = []
        
        // Add error-specific help steps
        switch error {
        case .network:
            steps.append(RecoveryStep(
                id: "network_help_1",
                title: "Check Internet Connection",
                description: "Ensure you have a stable internet connection",
                type: .instruction,
                action: .checkConnection
            ))
            steps.append(RecoveryStep(
                id: "network_help_2",
                title: "Try Different Network",
                description: "Switch to a different Wi-Fi network or use cellular data",
                type: .instruction,
                action: .switchNetwork
            ))
            
        case .authentication:
            steps.append(RecoveryStep(
                id: "auth_help_1",
                title: "Verify Credentials",
                description: "Check your username and password",
                type: .instruction,
                action: .verifyCredentials
            ))
            steps.append(RecoveryStep(
                id: "auth_help_2",
                title: "Reset Password",
                description: "Reset your password if you've forgotten it",
                type: .instruction,
                action: .resetPassword
            ))
            
        case .dataValidation:
            steps.append(RecoveryStep(
                id: "validation_help_1",
                title: "Check Input Format",
                description: "Ensure your input matches the required format",
                type: .instruction,
                action: .checkInputFormat
            ))
            steps.append(RecoveryStep(
                id: "validation_help_2",
                title: "Review Requirements",
                description: "Review the field requirements and try again",
                type: .instruction,
                action: .reviewRequirements
            ))
            
        default:
            steps.append(RecoveryStep(
                id: "general_help_1",
                title: "General Troubleshooting",
                description: "Try these general troubleshooting steps",
                type: .instruction,
                action: .generalTroubleshooting
            ))
        }
        
        // Add general help steps
        steps.append(RecoveryStep(
            id: "help_contact_support",
            title: "Contact Support",
            description: "If the issue persists, contact our support team",
            type: .action,
            action: .contactSupport
        ))
        
        return steps
    }
    
    private func createSettingsSteps(for error: AppError, context: ErrorContext?) -> [RecoveryStep] {
        var steps: [RecoveryStep] = []
        
        // Add error-specific settings steps
        switch error {
        case .network:
            steps.append(RecoveryStep(
                id: "network_settings_1",
                title: "Network Settings",
                description: "Configure network-related settings",
                type: .navigation,
                action: .openNetworkSettings
            ))
            
        case .configuration:
            steps.append(RecoveryStep(
                id: "config_settings_1",
                title: "App Configuration",
                description: "Reset app configuration to defaults",
                type: .action,
                action: .resetConfiguration
            ))
            
        default:
            steps.append(RecoveryStep(
                id: "general_settings_1",
                title: "General Settings",
                description: "Review and update app settings",
                type: .navigation,
                action: .openGeneralSettings
            ))
        }
        
        return steps
    }
    
    private func createSupportSteps(for error: AppError, context: ErrorContext?) -> [RecoveryStep] {
        var steps: [RecoveryStep] = []
        
        // Add error context to support request
        steps.append(RecoveryStep(
            id: "support_context",
            title: "Error Context",
            description: "Include error details in your support request",
            type: .instruction,
            action: .includeErrorContext
        ))
        
        // Add support contact options
        steps.append(RecoveryStep(
            id: "support_email",
            title: "Email Support",
            description: "Send an email to our support team",
            type: .action,
            action: .emailSupport
        ))
        
        steps.append(RecoveryStep(
            id: "support_chat",
            title: "Live Chat",
            description: "Start a live chat with our support team",
            type: .action,
            action: .startLiveChat
        ))
        
        steps.append(RecoveryStep(
            id: "support_phone",
            title: "Phone Support",
            description: "Call our support team",
            type: .action,
            action: .callSupport
        ))
        
        return steps
    }
    
    private func createRecoverySteps(for error: AppError, context: ErrorContext?) -> [RecoveryStep] {
        var steps: [RecoveryStep] = []
        
        // Add error-specific recovery steps
        switch error {
        case .network:
            steps.append(RecoveryStep(
                id: "network_recovery_1",
                title: "Check Connection",
                description: "Verify your internet connection",
                type: .action,
                action: .checkConnection
            ))
            steps.append(RecoveryStep(
                id: "network_recovery_2",
                title: "Retry Request",
                description: "Try the request again",
                type: .action,
                action: .retryRequest
            ))
            
        case .authentication:
            steps.append(RecoveryStep(
                id: "auth_recovery_1",
                title: "Re-authenticate",
                description: "Log in again with your credentials",
                type: .action,
                action: .reauthenticate
            ))
            
        case .dataCorrupted:
            steps.append(RecoveryStep(
                id: "data_recovery_1",
                title: "Clear Cache",
                description: "Clear app cache to resolve data issues",
                type: .action,
                action: .clearCache
            ))
            steps.append(RecoveryStep(
                id: "data_recovery_2",
                title: "Restore Data",
                description: "Restore data from backup",
                type: .action,
                action: .restoreData
            ))
            
        default:
            steps.append(RecoveryStep(
                id: "general_recovery_1",
                title: "Restart App",
                description: "Close and restart the application",
                type: .action,
                action: .restartApp
            ))
        }
        
        return steps
    }
    
    // MARK: - Navigation Implementation Methods
    
    private func navigateToHelpScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to help screen")
        
        // Create help view controller
        let helpVC = createHelpViewController(flow)
        presentViewController(helpVC, flow: flow)
    }
    
    private func navigateToSettingsScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to settings screen")
        
        // Navigate to app settings
        if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(settingsURL)
        }
        
        // Track navigation
        analyticsTracker.trackNavigationComplete(flow)
    }
    
    private func navigateToSupportScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to support screen")
        
        // Create support view controller
        let supportVC = createSupportViewController(flow)
        presentViewController(supportVC, flow: flow)
    }
    
    private func navigateToRecoveryScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to recovery screen")
        
        // Create recovery view controller
        let recoveryVC = createRecoveryViewController(flow)
        presentViewController(recoveryVC, flow: flow)
    }
    
    private func navigateToTroubleshootingScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to troubleshooting screen")
        
        // Create troubleshooting view controller
        let troubleshootingVC = createTroubleshootingViewController(flow)
        presentViewController(troubleshootingVC, flow: flow)
    }
    
    private func navigateToFeedbackScreen(_ flow: RecoveryFlow) {
        logger.debug("Navigating to feedback screen")
        
        // Create feedback view controller
        let feedbackVC = createFeedbackViewController(flow)
        presentViewController(feedbackVC, flow: flow)
    }
    
    // MARK: - View Controller Creation Methods
    
    private func createHelpViewController(_ flow: RecoveryFlow) -> UIViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let helpVC = storyboard.instantiateViewController(withIdentifier: "HelpViewController") as! HelpViewController
        helpVC.configure(with: flow)
        return helpVC
    }
    
    private func createSupportViewController(_ flow: RecoveryFlow) -> UIViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let supportVC = storyboard.instantiateViewController(withIdentifier: "SupportViewController") as! SupportViewController
        supportVC.configure(with: flow)
        return supportVC
    }
    
    private func createRecoveryViewController(_ flow: RecoveryFlow) -> UIViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let recoveryVC = storyboard.instantiateViewController(withIdentifier: "RecoveryViewController") as! RecoveryViewController
        recoveryVC.configure(with: flow)
        return recoveryVC
    }
    
    private func createTroubleshootingViewController(_ flow: RecoveryFlow) -> UIViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let troubleshootingVC = storyboard.instantiateViewController(withIdentifier: "TroubleshootingViewController") as! TroubleshootingViewController
        troubleshootingVC.configure(with: flow)
        return troubleshootingVC
    }
    
    private func createFeedbackViewController(_ flow: RecoveryFlow) -> UIViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let feedbackVC = storyboard.instantiateViewController(withIdentifier: "FeedbackViewController") as! FeedbackViewController
        feedbackVC.configure(with: flow)
        return feedbackVC
    }
    
    private func presentViewController(_ viewController: UIViewController, flow: RecoveryFlow) {
        guard let window = UIApplication.shared.windows.first,
              let rootViewController = window.rootViewController else {
            logger.error("No root view controller available to present view controller")
            return
        }
        
        // Find the topmost presented view controller
        var topController = rootViewController
        while let presentedController = topController.presentedViewController {
            topController = presentedController
        }
        
        // Present view controller
        topController.present(viewController, animated: true) {
            self.logger.debug("View controller presented successfully: \(flow.type.rawValue)")
            self.analyticsTracker.trackNavigationComplete(flow)
        }
    }
}

// MARK: - Supporting Types

struct RecoveryFlow {
    let id: String
    let type: FlowType
    let title: String
    let description: String
    let steps: [RecoveryStep]
    let context: ErrorContext?
    let timestamp: Date
    
    var status: RecoveryFlowStatus = .active
    var currentStepIndex: Int = 0
    
    mutating func navigateBack() {
        if currentStepIndex > 0 {
            currentStepIndex -= 1
        }
    }
    
    mutating func navigateNext() {
        if currentStepIndex < steps.count - 1 {
            currentStepIndex += 1
        }
    }
    
    mutating func complete() {
        status = .completed
    }
    
    var currentStep: RecoveryStep? {
        guard currentStepIndex < steps.count else { return nil }
        return steps[currentStepIndex]
    }
    
    var isComplete: Bool {
        return currentStepIndex >= steps.count - 1
    }
}

enum FlowType: String, CaseIterable {
    case help = "help"
    case settings = "settings"
    case support = "support"
    case recovery = "recovery"
    case troubleshooting = "troubleshooting"
    case feedback = "feedback"
}

struct RecoveryStep {
    let id: String
    let title: String
    let description: String
    let type: StepType
    let action: StepAction
    var isCompleted: Bool = false
    var timestamp: Date?
}

enum StepType {
    case instruction
    case action
    case navigation
    case confirmation
}

enum StepAction {
    case checkConnection
    case switchNetwork
    case verifyCredentials
    case resetPassword
    case checkInputFormat
    case reviewRequirements
    case generalTroubleshooting
    case contactSupport
    case openNetworkSettings
    case resetConfiguration
    case openGeneralSettings
    case includeErrorContext
    case emailSupport
    case startLiveChat
    case callSupport
    case retryRequest
    case reauthenticate
    case clearCache
    case restoreData
    case restartApp
}

enum RecoveryFlowStatus {
    case active
    case paused
    case completed
    case cancelled
}

struct NavigationEvent {
    let id: String
    let flow: RecoveryFlow
    let timestamp: Date
    let action: NavigationAction
}

enum NavigationAction {
    case start
    case back
    case next
    case complete
    case cancel
}

struct NavigationConfiguration {
    let maxNavigationHistory = 50
    let defaultFlowTimeout: TimeInterval = 300.0 // 5 minutes
    let stepTimeout: TimeInterval = 60.0 // 1 minute
}

// MARK: - View Controller Protocols
protocol FlowConfigurable {
    func configure(with flow: RecoveryFlow)
}

// MARK: - Real View Controller Implementations
class HelpViewController: UIViewController, FlowConfigurable {
    
    // MARK: - Properties
    private var flow: RecoveryFlow?
    private let scrollView = UIScrollView()
    private let contentView = UIView()
    private let titleLabel = UILabel()
    private let descriptionLabel = UILabel()
    private let stepsStackView = UIStackView()
    private let actionButton = UIButton(type: .system)
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    // MARK: - FlowConfigurable
    func configure(with flow: RecoveryFlow) {
        self.flow = flow
        updateUI()
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "Get Help"
        
        // Setup scroll view
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(descriptionLabel)
        contentView.addSubview(stepsStackView)
        contentView.addSubview(actionButton)
        
        setupConstraints()
        setupAppearance()
    }
    
    private func setupConstraints() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        contentView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descriptionLabel.translatesAutoresizingMaskIntoConstraints = false
        stepsStackView.translatesAutoresizingMaskIntoConstraints = false
        actionButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // Scroll view
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // Content view
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            // Title label
            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 20),
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            
            // Description label
            descriptionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10),
            descriptionLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            descriptionLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            
            // Steps stack view
            stepsStackView.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 20),
            stepsStackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            stepsStackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            
            // Action button
            actionButton.topAnchor.constraint(equalTo: stepsStackView.bottomAnchor, constant: 30),
            actionButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            actionButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
            actionButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -20),
            actionButton.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    private func setupAppearance() {
        // Title label
        titleLabel.font = .boldSystemFont(ofSize: 24)
        titleLabel.textColor = .label
        titleLabel.numberOfLines = 0
        
        // Description label
        descriptionLabel.font = .systemFont(ofSize: 16)
        descriptionLabel.textColor = .secondaryLabel
        descriptionLabel.numberOfLines = 0
        
        // Steps stack view
        stepsStackView.axis = .vertical
        stepsStackView.spacing = 15
        stepsStackView.distribution = .fillEqually
        
        // Action button
        actionButton.backgroundColor = .systemBlue
        actionButton.setTitleColor(.white, for: .normal)
        actionButton.titleLabel?.font = .boldSystemFont(ofSize: 18)
        actionButton.layer.cornerRadius = 8
        actionButton.addTarget(self, action: #selector(actionButtonTapped), for: .touchUpInside)
    }
    
    private func updateUI() {
        guard let flow = flow else { return }
        
        titleLabel.text = flow.title
        descriptionLabel.text = flow.description
        actionButton.setTitle("Continue", for: .normal)
        
        // Clear existing steps
        stepsStackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        
        // Add steps
        for (index, step) in flow.steps.enumerated() {
            let stepView = createStepView(step: step, index: index + 1)
            stepsStackView.addArrangedSubview(stepView)
        }
    }
    
    private func createStepView(step: RecoveryStep, index: Int) -> UIView {
        let containerView = UIView()
        containerView.backgroundColor = .secondarySystemBackground
        containerView.layer.cornerRadius = 8
        
        let numberLabel = UILabel()
        numberLabel.text = "\(index)"
        numberLabel.font = .boldSystemFont(ofSize: 18)
        numberLabel.textColor = .white
        numberLabel.backgroundColor = .systemBlue
        numberLabel.textAlignment = .center
        numberLabel.layer.cornerRadius = 15
        numberLabel.clipsToBounds = true
        
        let titleLabel = UILabel()
        titleLabel.text = step.title
        titleLabel.font = .boldSystemFont(ofSize: 16)
        titleLabel.textColor = .label
        titleLabel.numberOfLines = 0
        
        let descriptionLabel = UILabel()
        descriptionLabel.text = step.description
        descriptionLabel.font = .systemFont(ofSize: 14)
        descriptionLabel.textColor = .secondaryLabel
        descriptionLabel.numberOfLines = 0
        
        containerView.addSubview(numberLabel)
        containerView.addSubview(titleLabel)
        containerView.addSubview(descriptionLabel)
        
        numberLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        descriptionLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // Number label
            numberLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 15),
            numberLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 15),
            numberLabel.widthAnchor.constraint(equalToConstant: 30),
            numberLabel.heightAnchor.constraint(equalToConstant: 30),
            
            // Title label
            titleLabel.leadingAnchor.constraint(equalTo: numberLabel.trailingAnchor, constant: 15),
            titleLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -15),
            titleLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 15),
            
            // Description label
            descriptionLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            descriptionLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
            descriptionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 5),
            descriptionLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -15)
        ])
        
        return containerView
    }
    
    @objc private func actionButtonTapped() {
        // Navigate to next step or complete flow
        if let flow = flow {
            if flow.isComplete {
                dismiss(animated: true)
            } else {
                flow.navigateNext()
                updateUI()
            }
        }
    }
}

class SupportViewController: UIViewController, FlowConfigurable {
    
    // MARK: - Properties
    private var flow: RecoveryFlow?
    private let tableView = UITableView()
    private let contactOptions = [
        ("Email Support", "support@youthmind.com"),
        ("Live Chat", "Start a live chat session"),
        ("Phone Support", "+1-800-YOUTH-MIND"),
        ("Help Center", "Browse our help articles")
    ]
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    // MARK: - FlowConfigurable
    func configure(with flow: RecoveryFlow) {
        self.flow = flow
        updateUI()
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "Contact Support"
        
        view.addSubview(tableView)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(SupportOptionCell.self, forCellReuseIdentifier: "SupportOptionCell")
        
        setupConstraints()
    }
    
    private func setupConstraints() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func updateUI() {
        tableView.reloadData()
    }
}

extension SupportViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return contactOptions.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "SupportOptionCell", for: indexPath) as! SupportOptionCell
        let option = contactOptions[indexPath.row]
        cell.configure(title: option.0, subtitle: option.1)
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let option = contactOptions[indexPath.row]
        handleSupportOption(option.0)
    }
    
    private func handleSupportOption(_ option: String) {
        switch option {
        case "Email Support":
            openEmailSupport()
        case "Live Chat":
            startLiveChat()
        case "Phone Support":
            makePhoneCall()
        case "Help Center":
            openHelpCenter()
        default:
            break
        }
    }
    
    private func openEmailSupport() {
        if let url = URL(string: "mailto:support@youthmind.com") {
            UIApplication.shared.open(url)
        }
    }
    
    private func startLiveChat() {
        // Implement live chat functionality
        let alert = UIAlertController(title: "Live Chat", message: "Live chat feature coming soon!", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
    
    private func makePhoneCall() {
        if let url = URL(string: "tel:+18009688464") {
            UIApplication.shared.open(url)
        }
    }
    
    private func openHelpCenter() {
        if let url = URL(string: "https://help.youthmind.com") {
            UIApplication.shared.open(url)
        }
    }
}

class SupportOptionCell: UITableViewCell {
    
    private let titleLabel = UILabel()
    private let subtitleLabel = UILabel()
    private let iconImageView = UIImageView()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupUI()
    }
    
    private func setupUI() {
        contentView.addSubview(iconImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(subtitleLabel)
        
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            iconImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 15),
            iconImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            iconImageView.widthAnchor.constraint(equalToConstant: 30),
            iconImageView.heightAnchor.constraint(equalToConstant: 30),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: 15),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -15),
            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 10),
            
            subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
            subtitleLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 5),
            subtitleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -10)
        ])
        
        titleLabel.font = .boldSystemFont(ofSize: 16)
        titleLabel.textColor = .label
        
        subtitleLabel.font = .systemFont(ofSize: 14)
        subtitleLabel.textColor = .secondaryLabel
        
        iconImageView.image = UIImage(systemName: "questionmark.circle")
        iconImageView.tintColor = .systemBlue
    }
    
    func configure(title: String, subtitle: String) {
        titleLabel.text = title
        subtitleLabel.text = subtitle
    }
}

class RecoveryViewController: UIViewController, FlowConfigurable {
    
    // MARK: - Properties
    private var flow: RecoveryFlow?
    private let progressView = UIProgressView()
    private let statusLabel = UILabel()
    private let actionButton = UIButton(type: .system)
    private var currentStepIndex = 0
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    // MARK: - FlowConfigurable
    func configure(with flow: RecoveryFlow) {
        self.flow = flow
        updateUI()
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "Error Recovery"
        
        view.addSubview(progressView)
        view.addSubview(statusLabel)
        view.addSubview(actionButton)
        
        setupConstraints()
        setupAppearance()
    }
    
    private func setupConstraints() {
        progressView.translatesAutoresizingMaskIntoConstraints = false
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        actionButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            progressView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            progressView.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -50),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 50),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -50),
            progressView.heightAnchor.constraint(equalToConstant: 8),
            
            statusLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            statusLabel.topAnchor.constraint(equalTo: progressView.bottomAnchor, constant: 20),
            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            statusLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            
            actionButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            actionButton.topAnchor.constraint(equalTo: statusLabel.bottomAnchor, constant: 30),
            actionButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 50),
            actionButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -50),
            actionButton.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    private func setupAppearance() {
        progressView.progressTintColor = .systemBlue
        progressView.trackTintColor = .systemGray5
        
        statusLabel.font = .systemFont(ofSize: 16)
        statusLabel.textColor = .label
        statusLabel.textAlignment = .center
        statusLabel.numberOfLines = 0
        
        actionButton.backgroundColor = .systemBlue
        actionButton.setTitleColor(.white, for: .normal)
        actionButton.titleLabel?.font = .boldSystemFont(ofSize: 18)
        actionButton.layer.cornerRadius = 8
        actionButton.addTarget(self, action: #selector(actionButtonTapped), for: .touchUpInside)
    }
    
    private func updateUI() {
        guard let flow = flow else { return }
        
        let progress = Float(currentStepIndex) / Float(flow.steps.count)
        progressView.setProgress(progress, animated: true)
        
        if currentStepIndex < flow.steps.count {
            let step = flow.steps[currentStepIndex]
            statusLabel.text = "Step \(currentStepIndex + 1) of \(flow.steps.count): \(step.title)"
            actionButton.setTitle("Execute Step", for: .normal)
        } else {
            statusLabel.text = "Recovery completed successfully!"
            actionButton.setTitle("Finish", for: .normal)
        }
    }
    
    @objc private func actionButtonTapped() {
        guard let flow = flow else { return }
        
        if currentStepIndex < flow.steps.count {
            executeCurrentStep()
        } else {
            dismiss(animated: true)
        }
    }
    
    private func executeCurrentStep() {
        guard let flow = flow else { return }
        let step = flow.steps[currentStepIndex]
        
        // Simulate step execution
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.currentStepIndex += 1
            self.updateUI()
        }
    }
}

class TroubleshootingViewController: UIViewController, FlowConfigurable {
    
    // MARK: - Properties
    private var flow: RecoveryFlow?
    private let collectionView: UICollectionView
    
    // MARK: - Initialization
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        let layout = UICollectionViewFlowLayout()
        layout.itemSize = CGSize(width: 150, height: 100)
        layout.minimumInteritemSpacing = 10
        layout.minimumLineSpacing = 10
        self.collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    // MARK: - FlowConfigurable
    func configure(with flow: RecoveryFlow) {
        self.flow = flow
        updateUI()
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "Troubleshooting"
        
        view.addSubview(collectionView)
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.register(TroubleshootingCell.self, forCellWithReuseIdentifier: "TroubleshootingCell")
        collectionView.backgroundColor = .systemBackground
        
        setupConstraints()
    }
    
    private func setupConstraints() {
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            collectionView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            collectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            collectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            collectionView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func updateUI() {
        collectionView.reloadData()
    }
}

extension TroubleshootingViewController: UICollectionViewDataSource, UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return flow?.steps.count ?? 0
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "TroubleshootingCell", for: indexPath) as! TroubleshootingCell
        if let step = flow?.steps[indexPath.item] {
            cell.configure(step: step)
        }
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        collectionView.deselectItem(at: indexPath, animated: true)
        // Handle troubleshooting step selection
    }
}

class TroubleshootingCell: UICollectionViewCell {
    
    private let titleLabel = UILabel()
    private let iconImageView = UIImageView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupUI()
    }
    
    private func setupUI() {
        contentView.backgroundColor = .secondarySystemBackground
        contentView.layer.cornerRadius = 8
        
        contentView.addSubview(iconImageView)
        contentView.addSubview(titleLabel)
        
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            iconImageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            iconImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 20),
            iconImageView.widthAnchor.constraint(equalToConstant: 30),
            iconImageView.heightAnchor.constraint(equalToConstant: 30),
            
            titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            titleLabel.topAnchor.constraint(equalTo: iconImageView.bottomAnchor, constant: 10),
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 10),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -10)
        ])
        
        titleLabel.font = .systemFont(ofSize: 14)
        titleLabel.textColor = .label
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 2
        
        iconImageView.image = UIImage(systemName: "wrench")
        iconImageView.tintColor = .systemBlue
    }
    
    func configure(step: RecoveryStep) {
        titleLabel.text = step.title
    }
}

class FeedbackViewController: UIViewController, FlowConfigurable {
    
    // MARK: - Properties
    private var flow: RecoveryFlow?
    private let textView = UITextView()
    private let submitButton = UIButton(type: .system)
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    // MARK: - FlowConfigurable
    func configure(with flow: RecoveryFlow) {
        self.flow = flow
        updateUI()
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "Send Feedback"
        
        view.addSubview(textView)
        view.addSubview(submitButton)
        
        setupConstraints()
        setupAppearance()
    }
    
    private func setupConstraints() {
        textView.translatesAutoresizingMaskIntoConstraints = false
        submitButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            textView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            textView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            textView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            textView.heightAnchor.constraint(equalToConstant: 200),
            
            submitButton.topAnchor.constraint(equalTo: textView.bottomAnchor, constant: 20),
            submitButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 50),
            submitButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -50),
            submitButton.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    private func setupAppearance() {
        textView.font = .systemFont(ofSize: 16)
        textView.layer.borderColor = UIColor.systemGray4.cgColor
        textView.layer.borderWidth = 1
        textView.layer.cornerRadius = 8
        textView.text = "Please describe your experience and any suggestions for improvement..."
        
        submitButton.backgroundColor = .systemBlue
        submitButton.setTitleColor(.white, for: .normal)
        submitButton.setTitle("Submit Feedback", for: .normal)
        submitButton.titleLabel?.font = .boldSystemFont(ofSize: 18)
        submitButton.layer.cornerRadius = 8
        submitButton.addTarget(self, action: #selector(submitButtonTapped), for: .touchUpInside)
    }
    
    private func updateUI() {
        // Update UI based on flow
    }
    
    @objc private func submitButtonTapped() {
        // Submit feedback
        let alert = UIAlertController(title: "Feedback Submitted", message: "Thank you for your feedback!", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            self.dismiss(animated: true)
        })
        present(alert, animated: true)
    }
}
