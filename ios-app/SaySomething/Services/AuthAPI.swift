import Foundation
import Combine

struct AuthAPI {
    // Registration endpoint
    static func register(email: String, otp: String) -> AnyPublisher<ApiResponse<RegisterResponse>, APIError> {
        let registerRequest = RegisterRequest(email: email, otp: otp)
        guard let body = try? JSONEncoder().encode(registerRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        
        let headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Registration doesn't need JWT
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/auth/register",
            method: NetworkConfiguration.HTTPMethod.POST,
            headers: headers,
            body: body
        )
        // send request to server by NetworkService
        return NetworkService.shared.request(endpoint)
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
        let headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Login doesn't need JWT
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/auth/login",
            method: NetworkConfiguration.HTTPMethod.POST,
            headers: headers,
            body: body
        )
        return NetworkService.shared.request(endpoint)
    }
}
