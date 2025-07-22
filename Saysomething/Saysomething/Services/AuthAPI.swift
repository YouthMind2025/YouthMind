import Foundation
import Combine

struct AuthAPI {
    // Registration endpoint
    static func register(email: String, otp: String) -> AnyPublisher<ApiResponse<RegisterResponse>, APIError> {
        let registerRequest = RegisterRequest(email: email, otp: otp)
        guard let body = try? JSONEncoder().encode(registerRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        
        var headers: [String: String] = ["Content-Type": "application/json"]
        // Registration doesn't need JWT
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/auth/register",
            method: "POST",
            headers: headers,
            body: body
        )
        // send request to server by NetworkService
        return NetworkService().request(endpoint)
    }
    
    // Login endpoint
    static func login(email: String, password: String) -> AnyPublisher<ApiResponse<LoginResponse>, APIError> {
        let loginRequest = [
            "email": email,
            "password": password
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: loginRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        var headers: [String: String] = ["Content-Type": "application/json"]
        // Login doesn't need JWT
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/auth/login",
            method: "POST",
            headers: headers,
            body: body
        )
        return NetworkService().request(endpoint)
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

// JWT injection protocol
protocol JWTInjectable {
    var shouldInjectJWT: Bool { get }
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