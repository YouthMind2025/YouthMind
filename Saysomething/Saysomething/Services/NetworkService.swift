//
//  NetworkService.swift
//  Saysomething
//
//  Created by user278797 on 6/16/25.
//


// Core function
import Foundation
import Combine

// API Response and Error Handling
enum APIError: Error {
    case invalidURL
    case networkError(Error)
    case invalidResponse
    case invalidStatusCode(Int)         // wrong status code
    case decodingError(Error)           // json decode error
    case timeout
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
    
    // default value
    init(session: URLSession = .shared) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30.0
        configuration.timeoutIntervalForResource = 60.0
        self.session = URLSession(configuration: configuration)
    }
    
    // handle API request
    func request<T: Decodable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError> {
        // url checking, if URL is not valid then return Fail
        guard let url = URL(string: endpoint.baseURL + endpoint.path) else {
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
                // check status code is 2xx
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw APIError.invalidStatusCode(httpResponse.statusCode)
                }
                // check decode error
                do {
                    let decodedResponse = try JSONDecoder().decode(ApiResponse<T>.self, from: data)
                    return decodedResponse
                } catch {
                    throw APIError.decodingError(error)
                }
            } // covert error into APIError
            .mapError { error in
                if let urlError = error as? URLError, urlError.code == .timedOut {
                    return APIError.timeout
                }
                return APIError.networkError(error)
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
