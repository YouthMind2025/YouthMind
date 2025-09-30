import Foundation
import os.log

// MARK: - Analytics API Client
/// Handles communication with backend analytics APIs
final class AnalyticsAPIClient {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsAPIClient")
    private let baseURL = "http://localhost:8080/v1" // Update with your backend URL
    private let session = URLSession.shared
    
    // MARK: - Initialization
    init() {
        setupSession()
    }
    
    // MARK: - Public Methods
    
    /// Track error to backend
    func trackError(_ request: ErrorTrackingRequest) async throws {
        let url = URL(string: "\(baseURL)/analytics/errors")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let jsonData = try JSONEncoder().encode(request)
        urlRequest.httpBody = jsonData
        
        let (_, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        logger.debug("Successfully tracked error: \(request.errorCode)")
    }
    
    /// Track behavior to backend
    func trackBehavior(_ request: BehaviorTrackingRequest) async throws {
        let url = URL(string: "\(baseURL)/analytics/behaviors")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let jsonData = try JSONEncoder().encode(request)
        urlRequest.httpBody = jsonData
        
        let (_, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        logger.debug("Successfully tracked behavior: \(request.behavior)")
    }
    
    /// Record performance metric to backend
    func recordPerformanceMetric(_ request: PerformanceMetricRequest) async throws {
        let url = URL(string: "\(baseURL)/analytics/performance")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let jsonData = try JSONEncoder().encode(request)
        urlRequest.httpBody = jsonData
        
        let (_, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        logger.debug("Successfully recorded performance metric: \(request.type)")
    }
    
    /// Report crash to backend
    func reportCrash(_ request: CrashReportingRequest) async throws {
        let url = URL(string: "\(baseURL)/analytics/crashes")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let jsonData = try JSONEncoder().encode(request)
        urlRequest.httpBody = jsonData
        
        let (_, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        logger.debug("Successfully reported crash: \(request.error)")
    }
    
    /// Fetch dashboard data from backend
    func fetchDashboardData(_ request: DashboardDataRequest) async throws -> DashboardData {
        let url = URL(string: "\(baseURL)/analytics/dashboard")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "GET"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        // Add query parameters
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "includeErrorAnalytics", value: String(request.includeErrorAnalytics)),
            URLQueryItem(name: "includeUserBehaviorAnalytics", value: String(request.includeUserBehaviorAnalytics)),
            URLQueryItem(name: "includePerformanceAnalytics", value: String(request.includePerformanceAnalytics)),
            URLQueryItem(name: "includeTrends", value: String(request.includeTrends)),
            URLQueryItem(name: "timeRange", value: request.timeRange.rawValue)
        ]
        urlRequest.url = components.url
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        let dashboardData = try JSONDecoder().decode(DashboardData.self, from: data)
        logger.debug("Successfully fetched dashboard data")
        
        return dashboardData
    }
    
    /// Get error analytics from backend
    func getErrorAnalytics() async throws -> ErrorAnalyticsDashboard {
        let url = URL(string: "\(baseURL)/analytics/errors")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "GET"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        let analytics = try JSONDecoder().decode(ErrorAnalyticsDashboard.self, from: data)
        logger.debug("Successfully fetched error analytics")
        
        return analytics
    }
    
    /// Get user behavior analytics from backend
    func getUserBehaviorAnalytics() async throws -> UserBehaviorAnalytics {
        let url = URL(string: "\(baseURL)/analytics/behaviors")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "GET"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        let analytics = try JSONDecoder().decode(UserBehaviorAnalytics.self, from: data)
        logger.debug("Successfully fetched user behavior analytics")
        
        return analytics
    }
    
    /// Get performance analytics from backend
    func getPerformanceAnalytics() async throws -> PerformanceAnalytics {
        let url = URL(string: "\(baseURL)/analytics/performance")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "GET"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        let analytics = try JSONDecoder().decode(PerformanceAnalytics.self, from: data)
        logger.debug("Successfully fetched performance analytics")
        
        return analytics
    }
    
    /// Get crash analytics from backend
    func getCrashAnalytics() async throws -> CrashAnalytics {
        let url = URL(string: "\(baseURL)/analytics/crashes")!
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "GET"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        
        // Add authentication if needed
        addAuthentication(to: &urlRequest)
        
        let (data, response) = try await session.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AnalyticsAPIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw AnalyticsAPIError.serverError(httpResponse.statusCode)
        }
        
        let analytics = try JSONDecoder().decode(CrashAnalytics.self, from: data)
        logger.debug("Successfully fetched crash analytics")
        
        return analytics
    }
    
    // MARK: - Private Methods
    
    private func setupSession() {
        // Configure URLSession if needed
        // Add timeout, retry policies, etc.
    }
    
    private func addAuthentication(to request: inout URLRequest) {
        // Add JWT token or other authentication
        if let token = getAuthToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        // Add API key if available
        if let apiKey = getAPIKey() {
            request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        }
        
        // Add user agent
        request.setValue("YouthMind-iOS/\(getAppVersion())", forHTTPHeaderField: "User-Agent")
    }
    
    private func getAuthToken() -> String? {
        // Get JWT token from Keychain for security
        let keychain = KeychainManager()
        do {
            if let keyData = try keychain.retrieveKey(forKey: "AuthToken") {
                let data = keyData.withUnsafeBytes { Data($0) }
                return String(data: data, encoding: .utf8)
            }
            return nil
        } catch {
            // Fallback to UserDefaults for backward compatibility
            return UserDefaults.standard.string(forKey: "AuthToken")
        }
    }
    
    private func getAPIKey() -> String? {
        // Get API key from Keychain
        let keychain = KeychainManager()
        do {
            if let keyData = try keychain.retrieveKey(forKey: "APIKey") {
                let data = keyData.withUnsafeBytes { Data($0) }
                return String(data: data, encoding: .utf8)
            }
            return nil
        } catch {
            return nil
        }
    }
    
    private func getAppVersion() -> String {
        return Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }
}

// MARK: - Analytics API Errors
enum AnalyticsAPIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case serverError(Int)
    case networkError
    case decodingError
    case authenticationError
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .serverError(let code):
            return "Server error: \(code)"
        case .networkError:
            return "Network error"
        case .decodingError:
            return "Failed to decode response"
        case .authenticationError:
            return "Authentication error"
        }
    }
}
