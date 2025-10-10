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

// Network Service Implementation
class NetworkService: NetworkServiceProtocol {
    static let shared = NetworkService()
    private let session: URLSession
    
    // default value - shared, tuned for responsiveness
    init(session: URLSession? = nil) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = NetworkConfiguration.defaultRequestTimeout
        configuration.timeoutIntervalForResource = NetworkConfiguration.defaultResourceTimeout
        configuration.requestCachePolicy = .returnCacheDataElseLoad
        configuration.urlCache = URLCache(
            memoryCapacity: 32 * 1024 * 1024,
            diskCapacity: 128 * 1024 * 1024,
            diskPath: "SaySomethingURLCache"
        )
        self.session = session ?? URLSession(configuration: configuration)
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
                case NetworkConfiguration.StatusCode.successRange:
                    // Success status codes, continue processing
                    break
                case NetworkConfiguration.StatusCode.unauthorized:
                    // Automatically handle token errors
                    TokenManager.shared.handleTokenError()
                    throw APIError.unauthorized
                case NetworkConfiguration.StatusCode.forbidden:
                    throw APIError.forbidden
                case NetworkConfiguration.StatusCode.notFound:
                    throw APIError.notFound
                case NetworkConfiguration.StatusCode.serverErrorRange:
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
            .retry(NetworkConfiguration.maxRetries)
            .catch { error -> AnyPublisher<ApiResponse<T>, APIError> in
                // Log the error
                os_log("API Error: %{public}@ for endpoint: %{public}@", log: .default, type: .error, error.localizedDescription, endpoint.path)
                
                // Handle final failure with user-friendly error
                return Fail(error: error).eraseToAnyPublisher()
            }
            // 移除强制主线程处理，让调用者决定在哪个线程处理
            .eraseToAnyPublisher()
    }
}

// data structure
// login response
struct LoginResponse: Codable {
    let token: String
    let userId: String
    let expiresAt: String?
}

// register response
struct RegisterResponse: Codable {
    let userId: String
    let token: String
    let expiresAt: String
    let message: String?
}

// register request
struct RegisterRequest: Codable {
    let email: String
    let otp: String
}


// Registration request - no JWT needed
extension RegisterRequest: JWTInjectable {
    var shouldInjectJWT: Bool { false }
}

// Login request - no JWT needed
struct LoginRequest: JWTInjectable {
    let email: String
    let password: String
    var shouldInjectJWT: Bool { false }
}
