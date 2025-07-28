import Foundation
import Combine

class LoginService {
    static let shared = LoginService()
    private let networkService: NetworkServiceProtocol
    
    init(networkService: NetworkServiceProtocol = NetworkService()) {
        self.networkService = networkService
    }
    
    func register(email: String, otp: String) -> AnyPublisher<RegisterResponse, APIError> {
        let registerRequest = RegisterRequest(email: email, otp: otp)
        guard let body = try? JSONEncoder().encode(registerRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        let endpoint = Endpoint(
            path: "/v1/auth/register",
            method: "POST",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return networkService.request(endpoint)
            .map { $0.data }
            .eraseToAnyPublisher()
    }
} 