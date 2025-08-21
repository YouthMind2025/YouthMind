//
//  NetworkConfiguration.swift
//  SaySomething
//
//  Created by Dawei Bao on 05/08/25.
//

import Foundation
import Combine

// MARK: - Network Configuration Constants
struct NetworkConfiguration {
    // Base URLs
    static let defaultBaseURL = "http://localhost:8080"
    
    // Timeout configurations
    static let defaultRequestTimeout: TimeInterval = 30.0
    static let defaultResourceTimeout: TimeInterval = 60.0
    
    // Retry configurations
    static let maxRetries = 3
    static let retryDelay: TimeInterval = 2.0
    
    // HTTP Methods
    struct HTTPMethod {
        static let GET = "GET"
        static let POST = "POST"
        static let PUT = "PUT"
        static let DELETE = "DELETE"
        static let PATCH = "PATCH"
    }
    
    // Common Headers
    struct Headers {
        static let contentType = "Content-Type"
        static let authorization = "Authorization"
        static let accept = "Accept"
        static let userAgent = "User-Agent"
        
        static let applicationJSON = "application/json"
        static let applicationFormURLEncoded = "application/x-www-form-urlencoded"
    }
    
    // Status Code Ranges
    struct StatusCode {
        static let successRange = 200...299
        static let clientErrorRange = 400...499
        static let serverErrorRange = 500...599
        
        static let unauthorized = 401
        static let forbidden = 403
        static let notFound = 404
    }
}

// MARK: - Endpoint Configuration
struct Endpoint {
    let baseURL: String
    let path: String
    let method: String
    let headers: [String: String]?
    let body: Data?
    let timeout: TimeInterval
    
    init(baseURL: String = NetworkConfiguration.defaultBaseURL,
         path: String,
         method: String = NetworkConfiguration.HTTPMethod.GET,
         headers: [String: String]? = nil,
         body: Data? = nil,
         timeout: TimeInterval = NetworkConfiguration.defaultRequestTimeout) {
        self.baseURL = baseURL
        self.path = path
        self.method = method
        self.headers = headers
        self.body = body
        self.timeout = timeout
    }
}

// MARK: - Network Service Protocol
protocol NetworkServiceProtocol {
    func request<T: Codable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError>
}

// MARK: - JWT Injection Protocol
protocol JWTInjectable {
    var shouldInjectJWT: Bool { get }
}

// MARK: - Session Management
enum SessionExpiredError: Error {
    case sessionExpired
}

// MARK: - Helper Extensions
extension APIError {
    static func mapStatusCode(_ statusCode: Int) -> Error {
        if statusCode == NetworkConfiguration.StatusCode.unauthorized {
            return SessionExpiredError.sessionExpired
        }
        return APIError.invalidStatusCode(statusCode)
    }
} 
