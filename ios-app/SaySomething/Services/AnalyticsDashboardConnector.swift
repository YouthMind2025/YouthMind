import Foundation
import os.log

// MARK: - Analytics Dashboard Connector
/// Connects to backend analytics dashboard and provides insights
final class AnalyticsDashboardConnector {
    
    // MARK: - Dependencies
    private let encryptionService: AnalyticsEncryptionService
    private let apiClient: AnalyticsAPIClient
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsDashboardConnector")
    private let queue = DispatchQueue(label: "com.youthmind.dashboard.connector", qos: .background)
    
    // Dashboard data
    private var cachedDashboardData: DashboardData?
    private var lastUpdateTime: Date?
    private let cacheExpirationInterval: TimeInterval = 300 // 5 minutes
    
    // MARK: - Initialization
    init(encryptionService: AnalyticsEncryptionService) {
        self.encryptionService = encryptionService
        self.apiClient = AnalyticsAPIClient()
    }
    
    // MARK: - Public Methods
    
    /// Initialize dashboard connector
    func initialize() async {
        logger.info("Initializing Analytics Dashboard Connector")
        
        // Load cached dashboard data
        await loadCachedDashboardData()
        
        // Start background updates
        startBackgroundUpdates()
    }
    
    /// Get error analytics dashboard
    func getErrorAnalytics() async -> ErrorAnalyticsDashboard? {
        // Check cache first
        if let cachedData = cachedDashboardData,
           let lastUpdate = lastUpdateTime,
           Date().timeIntervalSince(lastUpdate) < cacheExpirationInterval {
            return cachedData.errorAnalytics
        }
        
        // Fetch from backend
        do {
            let dashboardData = try await fetchDashboardData()
            cachedDashboardData = dashboardData
            lastUpdateTime = Date()
            
            // Cache the data
            await cacheDashboardData(dashboardData)
            
            return dashboardData.errorAnalytics
        } catch {
            logger.error("Failed to fetch error analytics: \(error.localizedDescription)")
            return cachedDashboardData?.errorAnalytics
        }
    }
    
    /// Get user behavior analytics
    func getUserBehaviorAnalytics() async -> UserBehaviorAnalytics? {
        // Check cache first
        if let cachedData = cachedDashboardData,
           let lastUpdate = lastUpdateTime,
           Date().timeIntervalSince(lastUpdate) < cacheExpirationInterval {
            return cachedData.userBehaviorAnalytics
        }
        
        // Fetch from backend
        do {
            let dashboardData = try await fetchDashboardData()
            cachedDashboardData = dashboardData
            lastUpdateTime = Date()
            
            // Cache the data
            await cacheDashboardData(dashboardData)
            
            return dashboardData.userBehaviorAnalytics
        } catch {
            logger.error("Failed to fetch user behavior analytics: \(error.localizedDescription)")
            return cachedDashboardData?.userBehaviorAnalytics
        }
    }
    
    /// Get performance analytics
    func getPerformanceAnalytics() async -> PerformanceAnalytics? {
        // Check cache first
        if let cachedData = cachedDashboardData,
           let lastUpdate = lastUpdateTime,
           Date().timeIntervalSince(lastUpdate) < cacheExpirationInterval {
            return cachedData.performanceAnalytics
        }
        
        // Fetch from backend
        do {
            let dashboardData = try await fetchDashboardData()
            cachedDashboardData = dashboardData
            lastUpdateTime = Date()
            
            // Cache the data
            await cacheDashboardData(dashboardData)
            
            return dashboardData.performanceAnalytics
        } catch {
            logger.error("Failed to fetch performance analytics: \(error.localizedDescription)")
            return cachedDashboardData?.performanceAnalytics
        }
    }
    
    /// Get comprehensive dashboard data
    func getDashboardData() async -> DashboardData? {
        // Check cache first
        if let cachedData = cachedDashboardData,
           let lastUpdate = lastUpdateTime,
           Date().timeIntervalSince(lastUpdate) < cacheExpirationInterval {
            return cachedData
        }
        
        // Fetch from backend
        do {
            let dashboardData = try await fetchDashboardData()
            cachedDashboardData = dashboardData
            lastUpdateTime = Date()
            
            // Cache the data
            await cacheDashboardData(dashboardData)
            
            return dashboardData
        } catch {
            logger.error("Failed to fetch dashboard data: \(error.localizedDescription)")
            return cachedDashboardData
        }
    }
    
    /// Get analytics insights
    func getAnalyticsInsights() async -> [AnalyticsInsight] {
        guard let dashboardData = await getDashboardData() else {
            return []
        }
        
        return generateInsights(from: dashboardData)
    }
    
    /// Get analytics trends
    func getAnalyticsTrends() async -> AnalyticsTrends {
        guard let dashboardData = await getDashboardData() else {
            return AnalyticsTrends(
                errorTrends: [],
                behaviorTrends: [],
                performanceTrends: [],
                timestamp: Date()
            )
        }
        
        return AnalyticsTrends(
            errorTrends: dashboardData.errorTrends,
            behaviorTrends: dashboardData.behaviorTrends,
            performanceTrends: dashboardData.performanceTrends,
            timestamp: Date()
        )
    }
    
    /// Refresh dashboard data
    func refreshDashboardData() async {
        do {
            let dashboardData = try await fetchDashboardData()
            cachedDashboardData = dashboardData
            lastUpdateTime = Date()
            
            // Cache the data
            await cacheDashboardData(dashboardData)
            
            logger.info("Dashboard data refreshed successfully")
        } catch {
            logger.error("Failed to refresh dashboard data: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Private Methods
    
    private func fetchDashboardData() async throws -> DashboardData {
        let request = DashboardDataRequest(
            includeErrorAnalytics: true,
            includeUserBehaviorAnalytics: true,
            includePerformanceAnalytics: true,
            includeTrends: true,
            timeRange: .last30Days
        )
        
        return try await apiClient.fetchDashboardData(request)
    }
    
    private func generateInsights(from data: DashboardData) -> [AnalyticsInsight] {
        var insights: [AnalyticsInsight] = []
        
        // Error insights
        if let errorAnalytics = data.errorAnalytics {
            insights.append(contentsOf: generateErrorInsights(from: errorAnalytics))
        }
        
        // User behavior insights
        if let behaviorAnalytics = data.userBehaviorAnalytics {
            insights.append(contentsOf: generateBehaviorInsights(from: behaviorAnalytics))
        }
        
        // Performance insights
        if let performanceAnalytics = data.performanceAnalytics {
            insights.append(contentsOf: generatePerformanceInsights(from: performanceAnalytics))
        }
        
        return insights.sorted { $0.priority.rawValue > $1.priority.rawValue }
    }
    
    private func generateErrorInsights(from analytics: ErrorAnalyticsDashboard) -> [AnalyticsInsight] {
        var insights: [AnalyticsInsight] = []
        
        // High error frequency insight
        if analytics.totalErrors > 100 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .error,
                title: "High Error Frequency",
                description: "Total errors: \(analytics.totalErrors). Consider investigating error patterns.",
                priority: .high,
                timestamp: Date(),
                actionable: true,
                actionTitle: "View Error Details",
                actionData: ["errorCount": analytics.totalErrors]
            ))
        }
        
        // Critical errors insight
        if analytics.criticalErrorCount > 0 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .error,
                title: "Critical Errors Detected",
                description: "\(analytics.criticalErrorCount) critical errors require immediate attention.",
                priority: .critical,
                timestamp: Date(),
                actionable: true,
                actionTitle: "Review Critical Errors",
                actionData: ["criticalErrorCount": analytics.criticalErrorCount]
            ))
        }
        
        // Error trend insight
        if let topError = analytics.topErrorSources.first {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .error,
                title: "Top Error Source",
                description: "\(topError.source) is the most common error source with \(topError.count) occurrences.",
                priority: .medium,
                timestamp: Date(),
                actionable: true,
                actionTitle: "Investigate Source",
                actionData: ["source": topError.source, "count": topError.count]
            ))
        }
        
        return insights
    }
    
    private func generateBehaviorInsights(from analytics: UserBehaviorAnalytics) -> [AnalyticsInsight] {
        var insights: [AnalyticsInsight] = []
        
        // Low recovery rate insight
        if analytics.errorRecoveryRate < 0.5 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .behavior,
                title: "Low Error Recovery Rate",
                description: "Error recovery rate is \(String(format: "%.1f", analytics.errorRecoveryRate * 100))%. Consider improving error handling UX.",
                priority: .medium,
                timestamp: Date(),
                actionable: true,
                actionTitle: "Improve Error Handling",
                actionData: ["recoveryRate": analytics.errorRecoveryRate]
            ))
        }
        
        // High satisfaction insight
        if analytics.userSatisfactionScore > 0.8 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .behavior,
                title: "High User Satisfaction",
                description: "User satisfaction score is \(String(format: "%.1f", analytics.userSatisfactionScore * 100))%. Great job!",
                priority: .low,
                timestamp: Date(),
                actionable: false,
                actionTitle: nil,
                actionData: ["satisfactionScore": analytics.userSatisfactionScore]
            ))
        }
        
        return insights
    }
    
    private func generatePerformanceInsights(from analytics: PerformanceAnalytics) -> [AnalyticsInsight] {
        var insights: [AnalyticsInsight] = []
        
        // Slow error handling insight
        if analytics.averageErrorHandlingTime > 2.0 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .performance,
                title: "Slow Error Handling",
                description: "Average error handling time is \(String(format: "%.1f", analytics.averageErrorHandlingTime))s. Consider optimizing error handling logic.",
                priority: .medium,
                timestamp: Date(),
                actionable: true,
                actionTitle: "Optimize Error Handling",
                actionData: ["averageTime": analytics.averageErrorHandlingTime]
            ))
        }
        
        // High memory usage insight
        if let memoryUsage = analytics.systemPerformance.memoryUsage, memoryUsage > 200.0 {
            insights.append(AnalyticsInsight(
                id: UUID().uuidString,
                type: .performance,
                title: "High Memory Usage",
                description: "Memory usage is \(String(format: "%.1f", memoryUsage))MB. Consider memory optimization.",
                priority: .medium,
                timestamp: Date(),
                actionable: true,
                actionTitle: "Optimize Memory",
                actionData: ["memoryUsage": memoryUsage]
            ))
        }
        
        return insights
    }
    
    private func loadCachedDashboardData() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let dashboardDataURL = documentsDirectory.appendingPathComponent("analytics_dashboard_data.json")
            
            guard FileManager.default.fileExists(atPath: dashboardDataURL.path) else {
                logger.info("No cached dashboard data found")
                return
            }
            
            let data = try Data(contentsOf: dashboardDataURL)
            let cachedData = try JSONDecoder().decode(CachedDashboardData.self, from: data)
            
            // Check if cache is still valid (within 5 minutes)
            if Date().timeIntervalSince(cachedData.lastUpdated) < cacheExpirationInterval {
                cachedDashboardData = cachedData.dashboardData
                lastUpdateTime = cachedData.lastUpdated
                logger.info("Loaded cached dashboard data from \(cachedData.lastUpdated)")
            } else {
                logger.info("Cached dashboard data expired")
            }
        } catch {
            logger.error("Failed to load cached dashboard data: \(error.localizedDescription)")
        }
    }
    
    private func cacheDashboardData(_ data: DashboardData) async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let dashboardDataURL = documentsDirectory.appendingPathComponent("analytics_dashboard_data.json")
            
            let cachedData = CachedDashboardData(
                dashboardData: data,
                lastUpdated: Date()
            )
            
            let jsonData = try JSONEncoder().encode(cachedData)
            try jsonData.write(to: dashboardDataURL, options: [.atomic])
            
            logger.debug("Cached dashboard data")
        } catch {
            logger.error("Failed to cache dashboard data: \(error.localizedDescription)")
        }
    }
    
    private func startBackgroundUpdates() {
        // Update dashboard data every 5 minutes
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { [weak self] _ in
            Task {
                await self?.refreshDashboardData()
            }
        }
    }
}

// MARK: - Dashboard Models

/// Dashboard data
struct DashboardData: Codable {
    let errorAnalytics: ErrorAnalyticsDashboard?
    let userBehaviorAnalytics: UserBehaviorAnalytics?
    let performanceAnalytics: PerformanceAnalytics?
    let errorTrends: [ErrorTrend]
    let behaviorTrends: [BehaviorTrend]
    let performanceTrends: [PerformanceTrend]
    let timestamp: Date
}

/// Dashboard data request
struct DashboardDataRequest: Codable {
    let includeErrorAnalytics: Bool
    let includeUserBehaviorAnalytics: Bool
    let includePerformanceAnalytics: Bool
    let includeTrends: Bool
    let timeRange: TimeRange
}

/// Time range for analytics
enum TimeRange: String, Codable {
    case last24Hours = "last_24_hours"
    case last7Days = "last_7_days"
    case last30Days = "last_30_days"
    case last90Days = "last_90_days"
    case lastYear = "last_year"
}

/// Analytics insight
struct AnalyticsInsight: Codable, Identifiable {
    let id: String
    let type: InsightType
    let title: String
    let description: String
    let priority: InsightPriority
    let timestamp: Date
    let actionable: Bool
    let actionTitle: String?
    let actionData: [String: Any]
    
    enum CodingKeys: String, CodingKey {
        case id, type, title, description, priority, timestamp, actionable, actionTitle, actionData
    }
    
    init(id: String, type: InsightType, title: String, description: String, priority: InsightPriority, timestamp: Date, actionable: Bool, actionTitle: String?, actionData: [String: Any]) {
        self.id = id
        self.type = type
        self.title = title
        self.description = description
        self.priority = priority
        self.timestamp = timestamp
        self.actionable = actionable
        self.actionTitle = actionTitle
        self.actionData = actionData
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        type = try container.decode(InsightType.self, forKey: .type)
        title = try container.decode(String.self, forKey: .title)
        description = try container.decode(String.self, forKey: .description)
        priority = try container.decode(InsightPriority.self, forKey: .priority)
        timestamp = try container.decode(Date.self, forKey: .timestamp)
        actionable = try container.decode(Bool.self, forKey: .actionable)
        actionTitle = try container.decodeIfPresent(String.self, forKey: .actionTitle)
        // Decode actionData from JSON string
        if let jsonString = try? container.decodeIfPresent(String.self, forKey: .actionData),
           let jsonData = jsonString.data(using: .utf8),
           let decodedActionData = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {
            actionData = decodedActionData
        } else {
            actionData = [:]
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(type, forKey: .type)
        try container.encode(title, forKey: .title)
        try container.encode(description, forKey: .description)
        try container.encode(priority, forKey: .priority)
        try container.encode(timestamp, forKey: .timestamp)
        try container.encode(actionable, forKey: .actionable)
        try container.encodeIfPresent(actionTitle, forKey: .actionTitle)
        // Encode actionData as JSON string for simplicity
        if !actionData.isEmpty {
            if let jsonData = try? JSONSerialization.data(withJSONObject: actionData),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                try container.encode(jsonString, forKey: .actionData)
            }
        }
    }
}

/// Insight types
enum InsightType: String, Codable {
    case error = "error"
    case behavior = "behavior"
    case performance = "performance"
    case system = "system"
}

/// Insight priorities
enum InsightPriority: String, Codable, CaseIterable {
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

/// Analytics trends
struct AnalyticsTrends: Codable {
    let errorTrends: [ErrorTrend]
    let behaviorTrends: [BehaviorTrend]
    let performanceTrends: [PerformanceTrend]
    let timestamp: Date
}

/// Behavior trend
struct BehaviorTrend: Codable {
    let date: Date
    let behaviorCount: Int
    let topBehavior: String
}

/// Cached dashboard data for persistence
struct CachedDashboardData: Codable {
    let dashboardData: DashboardData
    let lastUpdated: Date
}
