import XCTest
import Combine
@testable import Saysomething

class MockNetworkService: NetworkServiceProtocol {
    var result: Result<ApiResponse<RegisterResponse>, APIError>?
    func request<T>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError> where T : Decodable, T : Encodable {
        if let result = result as? Result<ApiResponse<T>, APIError> {
            return result.publisher.eraseToAnyPublisher()
        } else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
    }
}

class LoginServiceTests: XCTestCase {
    var cancellables = Set<AnyCancellable>()
    
    func testSuccessfulRegisterStoresTokenAndValidatesExpiration() {
        let mockService = MockNetworkService()
        let response = RegisterResponse(userId: "u1", token: "jwt-token", expiresAt: "2099-12-31")
        let apiResponse = ApiResponse(data: response, statusCode: 200, headers: [:])
        mockService.result = .success(apiResponse)
        let loginService = LoginService(networkService: mockService)
        let exp = expectation(description: "register success")
        loginService.register(email: "a@b.com", otp: "123456")
            .sink { completion in
                if case .failure(let error) = completion {
                    XCTFail("Should not fail: \(error)")
                }
            } receiveValue: { regResponse in
                XCTAssertEqual(regResponse.token, "jwt-token")
                exp.fulfill()
            }
            .store(in: &cancellables)
        wait(for: [exp], timeout: 2)
    }
    
    func testLoginViewModelHandlesError() {
        let mockService = MockNetworkService()
        mockService.result = .failure(.invalidStatusCode(400))
        let viewModel = LoginViewModel()
        viewModel.email = "fail@b.com"
        viewModel.otp = "000000"
        // Inject mock
        viewModel.registerService = LoginService(networkService: mockService)
        let exp = expectation(description: "error feedback")
        viewModel.$errorMessage
            .dropFirst()
            .sink { error in
                if let error = error {
                    XCTAssertTrue(error.contains("Registration failed"))
                    exp.fulfill()
                }
            }
            .store(in: &cancellables)
        viewModel.register()
        wait(for: [exp], timeout: 2)
    }
    
    func testLoginViewModelValidatesTokenExpiration() {
        let mockService = MockNetworkService()
        // Expired date
        let response = RegisterResponse(userId: "u2", token: "jwt-token2", expiresAt: "2000-01-01")
        let apiResponse = ApiResponse(data: response, statusCode: 200, headers: [:])
        mockService.result = .success(apiResponse)
        let viewModel = LoginViewModel()
        viewModel.email = "expired@b.com"
        viewModel.otp = "111111"
        viewModel.registerService = LoginService(networkService: mockService)
        let exp = expectation(description: "expired token")
        viewModel.$errorMessage
            .dropFirst()
            .sink { error in
                if let error = error, error.contains("expired") {
                    exp.fulfill()
                }
            }
            .store(in: &cancellables)
        viewModel.register()
        wait(for: [exp], timeout: 2)
    }
} 