//
//  PostViewTracker.swift
//  SaySomething
//
//  Post view tracking service for analytics
//  Tracks time spent on posts and handles edge cases
//

import Foundation
import os.log
import Combine
import SystemConfiguration

// MARK: - Post View Tracking Service
final class PostViewTracker: ObservableObject {
    
    // MARK: - Properties
    static let shared = PostViewTracker()
    private let logger = Logger(subsystem: "com.youthmind.tracking", category: "PostViewTracker")
    private let networkService = NetworkService.shared
    private let retryHandler = RetryButtonHandler()
    
    // Active tracking sessions
    @Published private var activeSessions: [UUID: ViewSession] = [:]
    
    // Request queue for failed network calls
    private var failedRequests: [PostViewEndRequest] = []
    private let queue = DispatchQueue(label: "com.youthmind.postview.queue", qos: .utility)
    
    // UserDefaults keys
    private enum UserDefaultsKeys {
        static let activeSessions = "PostViewTracker.activeSessions"
        static let failedRequests = "PostViewTracker.failedRequests"
    }
    
    // MARK: - Initialization
    private init() {
        loadPersistedData()
        startNetworkMonitoring()
    }
    
    // MARK: - Public Methods
    
    /// Start tracking a post view
    /// Calls POST /v1/posts/{postId}/view
    func startTracking(postId: UUID) {
        logger.info("Starting view tracking for post: \(postId)")
        
        // Check if already tracking this post
        guard activeSessions[postId] == nil else {
            logger.warning("Already tracking post: \(postId)")
            return
        }
        
        // Create new session
        let session = ViewSession(
            postId: postId,
            startTime: Date(),
            isActive: true
        )
        
        activeSessions[postId] = session
        
        // Persist session for crash recovery
        persistActiveSessions()
        
        // Call view start endpoint
        callViewStartEndpoint(postId: postId)
    }
    
    /// End tracking a post view
    /// Calls POST /v1/posts/{postId}/view/end
    func endTracking(postId: UUID, reason: ViewEndReason) {
        logger.info("Ending view tracking for post: \(postId), reason: \(reason)")
        
        guard let session = activeSessions[postId] else {
            logger.warning("No active session found for post: \(postId)")
            return
        }
        
        // Calculate duration
        let duration = Int64(Date().timeIntervalSince(session.startTime) * 1000) // Convert to milliseconds
        
        // Only track if viewed for more than 1 second
        guard duration >= 1000 else {
            logger.info("View duration too short (\(duration)ms), not tracking")
            activeSessions.removeValue(forKey: postId)
            persistActiveSessions()
            return
        }
        
        // Remove from active sessions
        activeSessions.removeValue(forKey: postId)
        persistActiveSessions()
        
        // Call view end endpoint
        callViewEndEndpoint(postId: postId, duration: duration, reason: reason)
    }
    
    /// Handle app backgrounding - end all active sessions
    func handleAppBackgrounding() {
        logger.info("App backgrounding - ending all active sessions")
        
        for (postId, _) in activeSessions {
            endTracking(postId: postId, reason: .background)
        }
    }
    
    /// Handle app crash recovery - send incomplete sessions
    func handleAppCrashRecovery() {
        logger.info("Handling app crash recovery")
        
        let incompleteSessions = loadIncompleteSessions()
        
        for session in incompleteSessions {
            // Estimate duration as 30 seconds (default for crash recovery)
            let estimatedDuration: Int64 = 30000
            
            logger.info("Sending incomplete session for post: \(session.postId)")
            callViewEndEndpoint(
                postId: session.postId,
                duration: estimatedDuration,
                reason: .crash
            )
        }
        
        // Clear incomplete sessions
        clearIncompleteSessions()
    }
    
    // MARK: - Private Methods
    
    private func callViewStartEndpoint(postId: UUID) {
        let endpoint = Endpoint.trackPostViewStart(postId: postId.uuidString)
        
        networkService.request(endpoint)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    switch completion {
                    case .finished:
                        self?.logger.debug("View start tracked successfully for post: \(postId)")
                    case .failure(let error):
                        self?.logger.error("Failed to track view start for post: \(postId) - \(error)")
                        // View start failures are not critical, don't queue for retry
                    }
                },
                receiveValue: { _ in
                    // Success - no action needed
                }
            )
            .store(in: &cancellables)
    }
    
    private func callViewEndEndpoint(postId: UUID, duration: Int64, reason: ViewEndReason) {
        let request = PostViewEndRequest(
            postId: postId.uuidString,
            durationMs: duration,
            viewEndReason: reason.rawValue
        )
        
        let endpoint = Endpoint.trackPostViewEnd(postId: postId.uuidString, request: request)
        
        networkService.request(endpoint)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    switch completion {
                    case .finished:
                        self?.logger.debug("View end tracked successfully for post: \(postId)")
                    case .failure(let error):
                        self?.logger.error("Failed to track view end for post: \(postId) - \(error)")
                        // Queue for retry
                        self?.queueFailedRequest(request)
                    }
                },
                receiveValue: { _ in
                    // Success - no action needed
                }
            )
            .store(in: &cancellables)
    }
    
    private func queueFailedRequest(_ request: PostViewEndRequest) {
        queue.async { [weak self] in
            self?.failedRequests.append(request)
            self?.persistFailedRequests()
            
            // Use existing retry handler
            self?.retryHandler.handleRetry(
                for: .network(.serverError),
                context: ErrorContext(
                    originalRequest: nil,
                    userAction: "PostViewTracking",
                    additionalInfo: ["postId": request.postId]
                )
            )
        }
    }
    
    private func retryFailedRequests() {
        logger.info("Retrying \(failedRequests.count) failed view tracking requests")
        
        let requestsToRetry = failedRequests
        failedRequests.removeAll()
        persistFailedRequests()
        
        for request in requestsToRetry {
            guard let postId = UUID(uuidString: request.postId) else { continue }
            
            callViewEndEndpoint(
                postId: postId,
                duration: request.durationMs,
                reason: ViewEndReason(rawValue: request.viewEndReason) ?? .navigation
            )
        }
    }
    
    // MARK: - Persistence
    
    private func persistActiveSessions() {
        let sessionsData = activeSessions.compactMapValues { session in
            try? JSONEncoder().encode(session)
        }
        
        UserDefaults.standard.set(sessionsData, forKey: UserDefaultsKeys.activeSessions)
    }
    
    private func persistFailedRequests() {
        if let data = try? JSONEncoder().encode(failedRequests) {
            UserDefaults.standard.set(data, forKey: UserDefaultsKeys.failedRequests)
        }
    }
    
    private func loadPersistedData() {
        // Load active sessions
        if let sessionsData = UserDefaults.standard.dictionary(forKey: UserDefaultsKeys.activeSessions) as? [String: Data] {
            for (key, data) in sessionsData {
                if let postId = UUID(uuidString: key),
                   let session = try? JSONDecoder().decode(ViewSession.self, from: data) {
                    activeSessions[postId] = session
                }
            }
        }
        
        // Load failed requests
        if let data = UserDefaults.standard.data(forKey: UserDefaultsKeys.failedRequests),
           let requests = try? JSONDecoder().decode([PostViewEndRequest].self, from: data) {
            failedRequests = requests
        }
    }
    
    private func loadIncompleteSessions() -> [ViewSession] {
        guard let sessionsData = UserDefaults.standard.dictionary(forKey: UserDefaultsKeys.activeSessions) as? [String: Data] else {
            return []
        }
        
        var incompleteSessions: [ViewSession] = []
        
        for (key, data) in sessionsData {
            if let postId = UUID(uuidString: key),
               let session = try? JSONDecoder().decode(ViewSession.self, from: data) {
                incompleteSessions.append(session)
            }
        }
        
        return incompleteSessions
    }
    
    private func clearIncompleteSessions() {
        UserDefaults.standard.removeObject(forKey: UserDefaultsKeys.activeSessions)
    }
    
    // MARK: - Network Monitoring
    
    private var cancellables = Set<AnyCancellable>()
    
    private func startNetworkMonitoring() {
        // Monitor network reachability changes
        NotificationCenter.default.publisher(for: .networkReachabilityChanged)
            .sink { [weak self] _ in
                if self?.isNetworkAvailable() == true {
                    self?.retryFailedRequests()
                }
            }
            .store(in: &cancellables)
    }
    
    private func isNetworkAvailable() -> Bool {
        var zeroAddress = sockaddr_in()
        zeroAddress.sin_len = UInt8(MemoryLayout.size(ofValue: zeroAddress))
        zeroAddress.sin_family = sa_family_t(AF_INET)
        
        let defaultRouteReachability = withUnsafePointer(to: &zeroAddress) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { zeroSockAddress in
                SCNetworkReachabilityCreateWithAddress(nil, zeroSockAddress)
            }
        }
        
        var flags = SCNetworkReachabilityFlags()
        if !SCNetworkReachabilityGetFlags(defaultRouteReachability!, &flags) {
            return false
        }
        
        let isReachable = flags.contains(.reachable)
        let needsConnection = flags.contains(.connectionRequired)
        
        return isReachable && !needsConnection
    }
}

// MARK: - Supporting Types

struct ViewSession: Codable {
    let postId: UUID
    let startTime: Date
    let isActive: Bool
}


enum ViewEndReason: String, CaseIterable {
    case navigation = "navigation"
    case background = "background"
    case scroll = "scroll"
    case crash = "crash"
}


// MARK: - Notification Extensions

extension Notification.Name {
    static let networkReachabilityChanged = Notification.Name("NetworkReachabilityChanged")
}
