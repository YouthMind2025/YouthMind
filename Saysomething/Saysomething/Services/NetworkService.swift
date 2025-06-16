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


struct ApiResponse<T: Decodable>: Decodable {
    let data: T
    let statusCode: Int
    let headers: [String: String]
}


// Network Service Protocol
protocol NetworkServiceProtocol {
    func request<T: Decodable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError>
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
                    let decodedData = try JSONDecoder().decode(T.self, from: data)
                    return ApiResponse<T>(
                        data: decodedData,
                        statusCode: httpResponse.statusCode,
                        headers: httpResponse.allHeaderFields as? [String: String] ?? [:]
                    )
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


// Test data struct
struct RegisterRequest: Encodable {
    let email: String
    let otp: String
}

struct RegisterResponse: Decodable {
    let userId: String
    let token: String
    let expiresAt: String
    
    enum CodingKeys: String, CodingKey {
        case userId
        case token
        case expiresAt
    }
}

// Test Implementation
class NetworkServiceTests {
    private var cancellables = Set<AnyCancellable>()
    private let networkService: NetworkServiceProtocol
    
    init(networkService: NetworkServiceProtocol = NetworkService()) {
        self.networkService = networkService
    }
    
    func testRegisterEndpoint() {
        let registerData = RegisterRequest(email: "user@example.com", otp: "123456")
        
        guard let body = try? JSONEncoder().encode(registerData) else {
            print("[\(Date().formatted())] ERROR: Failed to encode register data")
            return
        }
        
        let endpoint = Endpoint(
            path: "/v1/auth/register",
            method: "POST",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        print("[\(Date().formatted())] INFO: Sending register request to \(endpoint.baseURL + endpoint.path)")
        print("[\(Date().formatted())] INFO: Request body: \(String(data: body, encoding: .utf8) ?? "N/A")")
        
        networkService.request(endpoint)
            .sink { completion in
                switch completion {
                case .finished:
                    print("[\(Date().formatted())] SUCCESS: Register request completed")
                case .failure(let error):
                    print("[\(Date().formatted())] ERROR: Register request failed - \(error.localizedDescription)")
                    switch error {
                    case .invalidURL:
                        print("[\(Date().formatted())] DETAIL: Invalid URL")
                    case .networkError(let underlyingError):
                        print("[\(Date().formatted())] DETAIL: Network error - \(underlyingError.localizedDescription)")
                    case .invalidResponse:
                        print("[\(Date().formatted())] DETAIL: Invalid response from server")
                    case .invalidStatusCode(let code):
                        print("[\(Date().formatted())] DETAIL: Invalid status code: \(code)")
                    case .decodingError(let decodingError):
                        print("[\(Date().formatted())] DETAIL: Decoding error - \(decodingError.localizedDescription)")
                    case .timeout:
                        print("[\(Date().formatted())] DETAIL: Request timed out")
                    }
                }
            } receiveValue: { (response: ApiResponse<RegisterResponse>) in
                print("[\(Date().formatted())] SUCCESS: Register request successful")
                print("[\(Date().formatted())] INFO: User ID: \(response.data.userId)")
                print("[\(Date().formatted())] INFO: Token: \(response.data.token)")
                print("[\(Date().formatted())] INFO: Expires At: \(response.data.expiresAt)")
                print("[\(Date().formatted())] INFO: Status Code: \(response.statusCode)")
                print("[\(Date().formatted())] INFO: Headers: \(response.headers)")
            }
            .store(in: &cancellables)
    }
}