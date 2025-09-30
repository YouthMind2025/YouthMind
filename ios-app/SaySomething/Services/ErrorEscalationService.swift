import Foundation
import os.log

// MARK: - Error Escalation Service
/// Handles error escalation and support ticket generation
final class ErrorEscalationService {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.escalation", category: "ErrorEscalationService")
    private let queue = DispatchQueue(label: "com.youthmind.escalation.service", qos: .userInitiated)
    
    // Backend integration
    private let apiClient = AnalyticsAPIClient()
    
    // Escalation configuration
    private let escalationConfig = EscalationConfiguration()
    
    // Support ticket storage
    private var supportTickets: [SupportTicket] = []
    private var escalationHistory: [EscalationRecord] = []
    
    // MARK: - Public Methods
    
    /// Escalate error to support team
    func escalateError(_ error: AppError, context: ErrorContext? = nil, priority: EscalationPriority = .medium) async -> EscalationResult {
        logger.info("Escalating error: \(error.errorCode) with priority: \(priority.rawValue)")
        
        let escalationRecord = createEscalationRecord(error, context: context, priority: priority)
        
        do {
            // Determine escalation level
            let escalationLevel = determineEscalationLevel(error, context: context, priority: priority)
            
            // Create support ticket
            let supportTicket = await createSupportTicket(escalationRecord, level: escalationLevel)
            
            // Send to backend
            try await sendEscalationToBackend(supportTicket)
            
            // Store locally
            await storeEscalationLocally(escalationRecord, ticket: supportTicket)
            
            logger.info("Error escalated successfully: \(supportTicket.id)")
            
            return EscalationResult(
                success: true,
                ticketId: supportTicket.id,
                escalationLevel: escalationLevel,
                estimatedResponseTime: calculateEstimatedResponseTime(escalationLevel),
                message: "Error escalated successfully"
            )
            
        } catch {
            logger.error("Failed to escalate error: \(error.localizedDescription)")
            
            return EscalationResult(
                success: false,
                ticketId: nil,
                escalationLevel: .none,
                estimatedResponseTime: 0,
                message: "Failed to escalate error: \(error.localizedDescription)"
            )
        }
    }
    
    /// Create support ticket for critical error
    func createSupportTicket(_ error: AppError, context: ErrorContext? = nil, userDescription: String? = nil) async -> SupportTicket {
        logger.info("Creating support ticket for error: \(error.errorCode)")
        
        let ticket = SupportTicket(
            id: UUID().uuidString,
            error: error,
            context: context,
            userDescription: userDescription,
            priority: determineTicketPriority(error, context: context),
            status: .open,
            createdAt: Date(),
            assignedTo: nil,
            estimatedResolutionTime: calculateEstimatedResolutionTime(error),
            tags: generateTags(for: error, context: context),
            attachments: await collectAttachments(for: error, context: context)
        )
        
        // Store ticket locally
        supportTickets.append(ticket)
        await cacheSupportTickets()
        
        logger.info("Support ticket created: \(ticket.id)")
        return ticket
    }
    
    /// Get support ticket status
    func getTicketStatus(_ ticketId: String) async -> TicketStatus? {
        return supportTickets.first { $0.id == ticketId }?.status
    }
    
    /// Update support ticket
    func updateTicket(_ ticketId: String, updates: TicketUpdate) async -> Bool {
        logger.info("Updating support ticket: \(ticketId)")
        
        guard let index = supportTickets.firstIndex(where: { $0.id == ticketId }) else {
            logger.warning("Support ticket not found: \(ticketId)")
            return false
        }
        
        var ticket = supportTickets[index]
        
        // Apply updates
        if let status = updates.status {
            ticket.status = status
        }
        if let assignedTo = updates.assignedTo {
            ticket.assignedTo = assignedTo
        }
        if let resolution = updates.resolution {
            ticket.resolution = resolution
        }
        if let comments = updates.comments {
            ticket.comments.append(contentsOf: comments)
        }
        
        ticket.updatedAt = Date()
        supportTickets[index] = ticket
        
        // Cache updated tickets
        await cacheSupportTickets()
        
        logger.info("Support ticket updated successfully")
        return true
    }
    
    /// Get escalation history
    func getEscalationHistory() -> [EscalationRecord] {
        return escalationHistory
    }
    
    /// Get support tickets
    func getSupportTickets(limit: Int = 50) -> [SupportTicket] {
        return Array(supportTickets.suffix(limit))
    }
    
    /// Close support ticket
    func closeTicket(_ ticketId: String, resolution: String) async -> Bool {
        logger.info("Closing support ticket: \(ticketId)")
        
        let update = TicketUpdate(
            status: .closed,
            resolution: resolution,
            comments: ["Ticket closed by system"]
        )
        
        return await updateTicket(ticketId, updates: update)
    }
    
    /// Get escalation statistics
    func getEscalationStatistics() -> EscalationStatistics {
        let totalEscalations = escalationHistory.count
        let openTickets = supportTickets.filter { $0.status == .open }.count
        let closedTickets = supportTickets.filter { $0.status == .closed }.count
        let averageResolutionTime = calculateAverageResolutionTime()
        
        return EscalationStatistics(
            totalEscalations: totalEscalations,
            openTickets: openTickets,
            closedTickets: closedTickets,
            averageResolutionTime: averageResolutionTime,
            escalationRate: calculateEscalationRate(),
            mostCommonError: getMostCommonEscalatedError()
        )
    }
    
    // MARK: - Private Methods
    
    private func createEscalationRecord(_ error: AppError, context: ErrorContext?, priority: EscalationPriority) -> EscalationRecord {
        return EscalationRecord(
            id: UUID().uuidString,
            error: error,
            context: context,
            priority: priority,
            timestamp: Date(),
            userId: getCurrentUserId(),
            sessionId: getCurrentSessionId(),
            deviceInfo: collectDeviceInfo(),
            appInfo: collectAppInfo()
        )
    }
    
    private func determineEscalationLevel(_ error: AppError, context: ErrorContext?, priority: EscalationPriority) -> EscalationLevel {
        // Determine escalation level based on error severity and priority
        let severity = determineErrorSeverity(error)
        
        switch (severity, priority) {
        case (.critical, _), (_, .critical):
            return .immediate
        case (.high, .high), (.high, .medium):
            return .high
        case (.medium, .high), (.medium, .medium), (.high, .low):
            return .medium
        case (.low, _), (.medium, .low):
            return .low
        default:
            return .low
        }
    }
    
    private func determineTicketPriority(_ error: AppError, context: ErrorContext?) -> TicketPriority {
        let severity = determineErrorSeverity(error)
        
        switch severity {
        case .critical:
            return .critical
        case .high:
            return .high
        case .medium:
            return .medium
        case .low:
            return .low
        }
    }
    
    private func determineErrorSeverity(_ error: AppError) -> ErrorSeverity {
        switch error {
        case .system:
            return .critical
        case .authentication, .permission:
            return .high
        case .network, .data:
            return .medium
        case .validation, .business:
            return .low
        }
    }
    
    private func calculateEstimatedResponseTime(_ level: EscalationLevel) -> TimeInterval {
        switch level {
        case .immediate:
            return 300 // 5 minutes
        case .high:
            return 1800 // 30 minutes
        case .medium:
            return 3600 // 1 hour
        case .low:
            return 7200 // 2 hours
        case .none:
            return 0
        }
    }
    
    private func calculateEstimatedResolutionTime(_ error: AppError) -> TimeInterval {
        let severity = determineErrorSeverity(error)
        
        switch severity {
        case .critical:
            return 3600 // 1 hour
        case .high:
            return 7200 // 2 hours
        case .medium:
            return 14400 // 4 hours
        case .low:
            return 28800 // 8 hours
        }
    }
    
    private func generateTags(for error: AppError, context: ErrorContext?) -> [String] {
        var tags: [String] = []
        
        // Add error type tags
        switch error {
        case .system:
            tags.append("system-error")
        case .network:
            tags.append("network-error")
        case .authentication:
            tags.append("auth-error")
        case .data:
            tags.append("data-error")
        case .permission:
            tags.append("permission-error")
        case .validation:
            tags.append("validation-error")
        case .business:
            tags.append("business-error")
        }
        
        // Add context tags
        if let context = context {
            if let screenName = context.screenName {
                tags.append("screen-\(screenName)")
            }
            if let userAction = context.userAction {
                tags.append("action-\(userAction)")
            }
        }
        
        // Add severity tag
        let severity = determineErrorSeverity(error)
        tags.append("severity-\(severity.rawValue)")
        
        // Add timestamp tag
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        tags.append("date-\(formatter.string(from: Date()))")
        
        return tags
    }
    
    private func collectAttachments(for error: AppError, context: ErrorContext?) async -> [Attachment] {
        var attachments: [Attachment] = []
        
        // Collect error logs
        if let errorLogs = await collectErrorLogs(for: error) {
            attachments.append(Attachment(
                id: UUID().uuidString,
                name: "error_logs.txt",
                type: .text,
                data: errorLogs,
                size: errorLogs.count
            ))
        }
        
        // Collect system information
        let systemInfo = collectSystemInfo()
        if let systemInfoData = try? JSONEncoder().encode(systemInfo) {
            attachments.append(Attachment(
                id: UUID().uuidString,
                name: "system_info.json",
                type: .json,
                data: systemInfoData,
                size: systemInfoData.count
            ))
        }
        
        // Collect crash report if available
        if let crashReport = await collectCrashReport(for: error) {
            attachments.append(Attachment(
                id: UUID().uuidString,
                name: "crash_report.json",
                type: .json,
                data: crashReport,
                size: crashReport.count
            ))
        }
        
        return attachments
    }
    
    private func collectErrorLogs(for error: AppError) async -> Data? {
        // Collect relevant error logs
        let logEntry = """
        Error: \(error.errorCode)
        Message: \(error.userMessage)
        Timestamp: \(Date())
        Stack Trace: \(Thread.callStackSymbols.joined(separator: "\n"))
        """
        
        return logEntry.data(using: .utf8)
    }
    
    private func collectCrashReport(for error: AppError) async -> Data? {
        // Collect crash report data
        let crashReport = CrashReportData(
            error: error.errorDescription ?? "Unknown Error",
            errorCode: error.errorCode,
            stackTrace: Thread.callStackSymbols,
            timestamp: Date(),
            deviceInfo: collectDeviceInfo(),
            appInfo: collectAppInfo()
        )
        
        return try? JSONEncoder().encode(crashReport)
    }
    
    private func sendEscalationToBackend(_ ticket: SupportTicket) async throws {
        let request = SupportTicketRequest(
            id: ticket.id,
            errorCode: ticket.error.errorCode,
            errorMessage: ticket.error.userMessage,
            context: ticket.context,
            userDescription: ticket.userDescription,
            priority: ticket.priority.rawValue,
            status: ticket.status.rawValue,
            tags: ticket.tags,
            attachments: ticket.attachments.map { attachment in
                AttachmentRequest(
                    id: attachment.id,
                    name: attachment.name,
                    type: attachment.type.rawValue,
                    data: attachment.data.base64EncodedString(),
                    size: attachment.size
                )
            },
            createdAt: ticket.createdAt,
            estimatedResolutionTime: ticket.estimatedResolutionTime
        )
        
        try await apiClient.createSupportTicket(request)
    }
    
    private func storeEscalationLocally(_ record: EscalationRecord, ticket: SupportTicket) async {
        queue.async { [weak self] in
            self?.escalationHistory.append(record)
            
            // Keep only last 100 escalation records
            if self?.escalationHistory.count ?? 0 > 100 {
                self?.escalationHistory.removeFirst()
            }
        }
    }
    
    private func cacheSupportTickets() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let ticketsURL = documentsDirectory.appendingPathComponent("support_tickets.json")
            
            let cachedData = CachedSupportTickets(
                tickets: supportTickets,
                lastUpdated: Date()
            )
            
            let data = try JSONEncoder().encode(cachedData)
            try data.write(to: ticketsURL, options: [.atomic])
            
            logger.debug("Cached \(supportTickets.count) support tickets")
        } catch {
            logger.error("Failed to cache support tickets: \(error.localizedDescription)")
        }
    }
    
    private func calculateAverageResolutionTime() -> TimeInterval {
        let closedTickets = supportTickets.filter { $0.status == .closed }
        
        guard !closedTickets.isEmpty else { return 0 }
        
        let totalTime = closedTickets.compactMap { ticket in
            ticket.resolvedAt?.timeIntervalSince(ticket.createdAt)
        }.reduce(0, +)
        
        return totalTime / Double(closedTickets.count)
    }
    
    private func calculateEscalationRate() -> Double {
        let totalErrors = UserDefaults.standard.integer(forKey: "TotalErrorCount")
        let escalatedErrors = escalationHistory.count
        
        guard totalErrors > 0 else { return 0.0 }
        
        return Double(escalatedErrors) / Double(totalErrors)
    }
    
    private func getMostCommonEscalatedError() -> String? {
        let errorCounts = Dictionary(grouping: escalationHistory, by: { $0.error.errorCode })
            .mapValues { $0.count }
        
        return errorCounts.max(by: { $0.value < $1.value })?.key
    }
    
    // MARK: - Helper Methods
    
    private func getCurrentUserId() -> String? {
        return UserDefaults.standard.string(forKey: "CurrentUserId")
    }
    
    private func getCurrentSessionId() -> String {
        return UserDefaults.standard.string(forKey: "CurrentSessionId") ?? UUID().uuidString
    }
    
    private func collectDeviceInfo() -> DeviceInfo {
        return DeviceInfo(
            model: UIDevice.current.model,
            systemName: UIDevice.current.systemName,
            systemVersion: UIDevice.current.systemVersion,
            identifierForVendor: UIDevice.current.identifierForVendor?.uuidString,
            batteryLevel: UIDevice.current.batteryLevel,
            batteryState: UIDevice.current.batteryState.rawValue,
            orientation: UIDevice.current.orientation.rawValue,
            isMultitaskingSupported: UIDevice.current.isMultitaskingSupported,
            userInterfaceIdiom: UIDevice.current.userInterfaceIdiom.rawValue
        )
    }
    
    private func collectAppInfo() -> AppInfo {
        let bundle = Bundle.main
        return AppInfo(
            bundleIdentifier: bundle.bundleIdentifier,
            version: bundle.infoDictionary?["CFBundleShortVersionString"] as? String,
            buildNumber: bundle.infoDictionary?["CFBundleVersion"] as? String,
            name: bundle.infoDictionary?["CFBundleName"] as? String,
            displayName: bundle.infoDictionary?["CFBundleDisplayName"] as? String,
            minimumOSVersion: bundle.infoDictionary?["MinimumOSVersion"] as? String,
            supportedPlatforms: bundle.infoDictionary?["CFBundleSupportedPlatforms"] as? [String],
            isLowPowerModeEnabled: ProcessInfo.processInfo.isLowPowerModeEnabled,
            isBackgroundRefreshEnabled: UIApplication.shared.backgroundRefreshStatus.rawValue
        )
    }
    
    private func collectSystemInfo() -> SystemInfo {
        let processInfo = ProcessInfo.processInfo
        return SystemInfo(
            processIdentifier: processInfo.processIdentifier,
            processName: processInfo.processName,
            hostName: processInfo.hostName,
            operatingSystemVersion: processInfo.operatingSystemVersionString,
            processorCount: processInfo.processorCount,
            activeProcessorCount: processInfo.activeProcessorCount,
            physicalMemory: processInfo.physicalMemory,
            systemUptime: processInfo.systemUptime,
            thermalState: processInfo.thermalState.rawValue,
            isLowPowerModeEnabled: processInfo.isLowPowerModeEnabled
        )
    }
}

// MARK: - Supporting Types

struct EscalationResult {
    let success: Bool
    let ticketId: String?
    let escalationLevel: EscalationLevel
    let estimatedResponseTime: TimeInterval
    let message: String
}

struct SupportTicket {
    let id: String
    let error: AppError
    let context: ErrorContext?
    let userDescription: String?
    let priority: TicketPriority
    var status: TicketStatus
    let createdAt: Date
    var updatedAt: Date?
    var assignedTo: String?
    var resolvedAt: Date?
    var resolution: String?
    var comments: [String]
    let estimatedResolutionTime: TimeInterval
    let tags: [String]
    let attachments: [Attachment]
}

enum EscalationLevel: String, CaseIterable {
    case none = "none"
    case low = "low"
    case medium = "medium"
    case high = "high"
    case immediate = "immediate"
}

enum EscalationPriority: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

enum TicketPriority: String, CaseIterable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
}

enum TicketStatus: String, CaseIterable {
    case open = "open"
    case inProgress = "in_progress"
    case pending = "pending"
    case resolved = "resolved"
    case closed = "closed"
}

struct EscalationRecord {
    let id: String
    let error: AppError
    let context: ErrorContext?
    let priority: EscalationPriority
    let timestamp: Date
    let userId: String?
    let sessionId: String
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
}

struct TicketUpdate {
    let status: TicketStatus?
    let assignedTo: String?
    let resolution: String?
    let comments: [String]?
}

struct Attachment {
    let id: String
    let name: String
    let type: AttachmentType
    let data: Data
    let size: Int
}

enum AttachmentType: String, CaseIterable {
    case text = "text"
    case json = "json"
    case image = "image"
    case video = "video"
    case audio = "audio"
    case other = "other"
}

struct EscalationStatistics {
    let totalEscalations: Int
    let openTickets: Int
    let closedTickets: Int
    let averageResolutionTime: TimeInterval
    let escalationRate: Double
    let mostCommonError: String?
}

struct EscalationConfiguration {
    let maxEscalationHistory = 100
    let maxSupportTickets = 1000
    let escalationRetentionDays = 90
}

struct CachedSupportTickets: Codable {
    let tickets: [SupportTicket]
    let lastUpdated: Date
}

struct CrashReportData: Codable {
    let error: String
    let errorCode: String
    let stackTrace: [String]
    let timestamp: Date
    let deviceInfo: DeviceInfo
    let appInfo: AppInfo
}

// MARK: - Backend Request Types

struct SupportTicketRequest: Codable {
    let id: String
    let errorCode: String
    let errorMessage: String
    let context: ErrorContext?
    let userDescription: String?
    let priority: String
    let status: String
    let tags: [String]
    let attachments: [AttachmentRequest]
    let createdAt: Date
    let estimatedResolutionTime: TimeInterval
}

struct AttachmentRequest: Codable {
    let id: String
    let name: String
    let type: String
    let data: String // Base64 encoded
    let size: Int
}
