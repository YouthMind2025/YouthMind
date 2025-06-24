import Foundation
import Combine

struct AuthAPI {
    static func login(email: String, password: String) -> AnyPublisher<ApiResponse<LoginResponse>, APIError> {
        let loginRequest = [
            "email": email,
            "password": password
        ]
        guard let body = try? JSONSerialization.data(withJSONObject: loginRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        var headers: [String: String] = ["Content-Type": "application/json"]
        if let token = TokenManager.shared.getToken() {
            headers["Authorization"] = "Bearer \(token)"
        }
        let endpoint = Endpoint(
            path: "/auth/login",
            method: "POST",
            headers: headers,
            body: body
        )
        return NetworkService().request(endpoint)
    }
}

struct LoginResponse: Codable {
    let token: String
    let userId: String
    let expiresAt: String?
}

// Example: Use JWTInjectable for conditional JWT injection
struct LoginRequest: JWTInjectable {
    let email: String
    let password: String
    var shouldInjectJWT: Bool { false } // No JWT needed for login
}

// Usage (for reference):
// let loginReq = LoginRequest(email: "test@example.com", password: "1234")
// if loginReq.shouldInjectJWT, let token = TokenManager.shared.getToken() {
//     headers["Authorization"] = "Bearer \(token)"
// } 