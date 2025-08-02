//
//  NetworkService.swift
//  Saysomething
//
//  Created by user278797 on 6/16/25.
//


// Core function
import Foundation
import Combine
import os.log

// API Response and Error Handling
enum APIError: Error, LocalizedError {
    case invalidURL
    case networkError(Error)
    case invalidResponse
    case invalidStatusCode(Int)         // wrong status code
    case decodingError(Error)           // json decode error
    case timeout
    case unauthorized
    case forbidden
    case notFound
    case serverError
    case noInternetConnection
    case connectionFailed
    case connectionLost
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .invalidResponse:
            return "Invalid response"
        case .invalidStatusCode(let code):
            switch code {
            case 401:
                return "Unauthorized, please login again"
            case 403:
                return "Forbidden"
            case 404:
                return "Resource not found"
            case 500...599:
                return "Server error"
            default:
                return "Request failed (status code: \(code))"
            }
        case .decodingError(let error):
            return "Data parsing error: \(error.localizedDescription)"
        case .timeout:
            return "Request timeout - please check your connection"
        case .unauthorized:
            return "Unauthorized, please login again"
        case .forbidden:
            return "Forbidden"
        case .notFound:
            return "Resource not found"
        case .serverError:
            return "Server error"
        case .noInternetConnection:
            return "No internet connection - please check your network"
        case .connectionFailed:
            return "Connection failed - please try again"
        case .connectionLost:
            return "Connection lost - please check your network"
        }
    }
    
    var isRetryable: Bool {
        switch self {
        case .timeout, .networkError, .noInternetConnection, .connectionFailed, .connectionLost:
            return true
        case .invalidURL, .invalidResponse, .invalidStatusCode, .decodingError, .unauthorized, .forbidden, .notFound, .serverError:
            return false
        }
    }
}


struct ApiResponse<T: Codable>: Codable {
    let data: T
    let statusCode: Int
    let headers: [String: String]
}


// Network Service Protocol
protocol NetworkServiceProtocol {
    func request<T: Codable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError>
}

// Endpoint Configuration
struct Endpoint {
    let baseURL: String
    let path: String
    let method: String
    let headers: [String: String]?
    let body: Data?
    let timeout: TimeInterval
    
    init(baseURL: String = "https://api.example.com",
            path: String,
            method: String = "GET",
            headers: [String: String]? = nil,
            body: Data? = nil,
            timeout: TimeInterval = 30.0) {
        self.baseURL = baseURL
        self.path = path
        self.method = method
        self.headers = headers
        self.body = body
        self.timeout = timeout
    }
}

// Network Service Implementation
class NetworkService: NetworkServiceProtocol {
    private let session: URLSession
    private let maxRetries = 3
    private let retryDelay: TimeInterval = 2.0
    
    // default value
    init(session: URLSession = .shared) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30.0
        configuration.timeoutIntervalForResource = 60.0
        self.session = URLSession(configuration: configuration)
    }
    
    // handle API request with retry mechanism
    func request<T: Decodable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError> {
        // Log the API request
        os_log("API Request: %{public}@ %{public}@", log: .default, type: .info, endpoint.method, endpoint.path)
        
        // url checking, if URL is not valid then return Fail
        guard let url = URL(string: endpoint.baseURL + endpoint.path) else {
            os_log("Invalid URL: %{public}@", log: .default, type: .error, endpoint.baseURL + endpoint.path)
            return Fail(error: APIError.invalidURL)
                .eraseToAnyPublisher()
        }
        
        // create request, and intial it with endpoint
        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method
        request.httpBody = endpoint.body
        endpoint.headers?.forEach { request.addValue($1, forHTTPHeaderField: $0) }      // also handle no header situation
        request.timeoutInterval = endpoint.timeout
        
        return session.dataTaskPublisher(for: request)
            .tryMap { data, response in
                // convert response to HTTPURLResponse
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw APIError.invalidResponse
                }
                
                // Handle different status codes
                switch httpResponse.statusCode {
                case 200...299:
                    // Success status codes, continue processing
                    break
                case 401:
                    // Automatically handle token errors
                    TokenManager.shared.handleTokenError()
                    throw APIError.unauthorized
                case 403:
                    throw APIError.forbidden
                case 404:
                    throw APIError.notFound
                case 500...599:
                    throw APIError.serverError
                default:
                    throw APIError.invalidStatusCode(httpResponse.statusCode)
                }
                
                // check decode error with detailed error handling
                do {
                    let decodedResponse = try JSONDecoder().decode(ApiResponse<T>.self, from: data)
                    return decodedResponse
                } catch let decodingError as DecodingError {
                    // Handle specific decoding errors
                    switch decodingError {
                    case .keyNotFound(let key, let context):
                        throw APIError.decodingError(DecodingError.keyNotFound(key, context))
                    case .typeMismatch(let type, let context):
                        throw APIError.decodingError(DecodingError.typeMismatch(type, context))
                    case .valueNotFound(let type, let context):
                        throw APIError.decodingError(DecodingError.valueNotFound(type, context))
                    case .dataCorrupted(let context):
                        throw APIError.decodingError(DecodingError.dataCorrupted(context))
                    @unknown default:
                        throw APIError.decodingError(decodingError)
                    }
                } catch {
                    throw APIError.decodingError(error)
                }
            } // covert error into APIError
            .mapError { error in
                if let urlError = error as? URLError {
                    switch urlError.code {
                    case .timedOut:
                        return APIError.timeout
                    case .notConnectedToInternet:
                        return APIError.noInternetConnection
                    case .cannotConnectToHost:
                        return APIError.connectionFailed
                    case .networkConnectionLost:
                        return APIError.connectionLost
                    default:
                        return APIError.networkError(error)
                    }
                }
                return APIError.networkError(error)
            }
            .retry(maxRetries)
            .catch { error -> AnyPublisher<ApiResponse<T>, APIError> in
                // Log the error
                os_log("API Error: %{public}@ for endpoint: %{public}@", log: .default, type: .error, error.localizedDescription, endpoint.path)
                
                // Handle final failure with user-friendly error
                return Fail(error: error).eraseToAnyPublisher()
            }
            .receive(on: DispatchQueue.main)    // on main
            .eraseToAnyPublisher()
    }
}

struct RegisterResponse: Codable {
    let userId: String
    let token: String
    let expiresAt: String
}

struct RegisterRequest: Codable {
    let email: String
    let otp: String
}

// Centralized error for session expiration
enum SessionExpiredError: Error {
    case sessionExpired
}

// Protocol for conditional JWT injection
protocol JWTInjectable {
    var shouldInjectJWT: Bool { get }
}

// Helper for error mapping
extension APIError {
    static func mapStatusCode(_ statusCode: Int) -> Error {
        if statusCode == 401 {
            return SessionExpiredError.sessionExpired
        }
        return APIError.invalidStatusCode(statusCode)
    }
}

// Example usage for error mapping (non-breaking, for reference):
// .mapError { error in
//     if case APIError.invalidStatusCode(let code) = error, code == 401 {
//         return SessionExpiredError.sessionExpired
//     }
//     return error
// }  frontend-together
