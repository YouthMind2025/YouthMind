//
//  FeedAPI.swift
//  SaySomething
//

import Foundation
import Combine

/// API client for feed block endpoints
/// Handles communication with backend feed block services
struct FeedAPI {
    
    // MARK: - Feed Block Endpoints
    
    /// Fetch a specific feed block
    /// GET /v1/feed/blocks?blockNumber={N}&size={size}
    static func fetchFeedBlock(blockNumber: Int, size: Int = 20) -> AnyPublisher<ApiResponse<FeedBlockResponse>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        
        // Add authentication if available
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.unauthorized).eraseToAnyPublisher()
        }
        
        let path = "/v1/feed/blocks?blockNumber=\(blockNumber)&size=\(size)"
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: path,
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
        )
        
        return NetworkService.shared.request(endpoint)
    }
    
    /// Notify backend of user scroll event to trigger pre-calculation
    /// POST /v1/feed/scroll?currentBlock={N}
    static func notifyScroll(currentBlock: Int) -> AnyPublisher<ApiResponse<FeedScrollResponse>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        
        // Add authentication if available
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.unauthorized).eraseToAnyPublisher()
        }
        
        let path = "/v1/feed/scroll?currentBlock=\(currentBlock)"
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: path,
            method: NetworkConfiguration.HTTPMethod.POST,
            headers: headers
        )
        
        return NetworkService.shared.request(endpoint)
    }
    
    /// Get current feed status and R-vector information
    /// GET /v1/feed/status
    static func getFeedStatus() -> AnyPublisher<ApiResponse<FeedStatusResponse>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        
        // Add authentication if available
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.unauthorized).eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/feed/status",
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
        )
        
        return NetworkService.shared.request(endpoint)
    }
}

// MARK: - Request/Response Models

/// Request for feed block operations
struct FeedBlockRequest: Codable {
    let blockNumber: Int
    let size: Int
    
    init(blockNumber: Int, size: Int = 20) {
        self.blockNumber = blockNumber
        self.size = size
    }
}

/// Request for scroll notification
struct FeedScrollRequest: Codable {
    let currentBlock: Int
    
    init(currentBlock: Int) {
        self.currentBlock = currentBlock
    }
}

// MARK: - JWT Injection Protocol Conformance

extension FeedBlockRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
}

extension FeedScrollRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
}
