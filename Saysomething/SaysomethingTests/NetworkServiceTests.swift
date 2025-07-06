import XCTest
import Combine
@testable import Saysomething

struct RegisterResponse: Codable {
    let userId: String
    let token: String
    let expiresAt: String
}

struct RegisterRequest: Codable {
    let email: String
    let otp: String
}

protocol NetworkSessionProtocol {
    func makeDataTaskPublisher(for request: URLRequest) -> AnyPublisher<(data: Data, response: URLResponse), URLError>
}

extension URLSession: NetworkSessionProtocol {
    func makeDataTaskPublisher(for request: URLRequest) -> AnyPublisher<(data: Data, response: URLResponse), URLError> {
        self.dataTaskPublisher(for: request)
            .eraseToAnyPublisher()
    }
}

class NetworkServiceTests: XCTestCase {
    private var cancellables = Set<AnyCancellable>()
    private var mockSession: MockSession!
    private var networkService: TestableNetworkService!
    
    override func setUp() {
        super.setUp()
        mockSession = MockSession()
        networkService = TestableNetworkService(session: mockSession)
    }
    
    override func tearDown() {
        cancellables.removeAll()
        mockSession = nil
        networkService = nil
        super.tearDown()
    }
    
    func testSuccessfulRegisterRequest() {
        // test data
        let expectedResponse = RegisterResponse(
            userId: "test123",
            token: "test-token",
            expiresAt: "2024-12-31"
        )
        
        // Create the wrapped API response
        let apiResponse = ApiResponse(
            data: expectedResponse,
            statusCode: 200,
            headers: ["Content-Type": "application/json"]
        )
        
        let responseData = try! JSONEncoder().encode(apiResponse)
        let mockResponse = HTTPURLResponse(
            url: URL(string: "https://api.example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: ["Content-Type": "application/json"]
        )!
        
        // mock response
        mockSession.mockData = responseData
        mockSession.mockResponse = mockResponse
        mockSession.mockError = nil
        
        // create request
        let registerData = RegisterRequest(email: "test@example.com", otp: "123456")
        let body = try! JSONEncoder().encode(registerData)
        
        let endpoint = Endpoint(
            path: "/v1/auth/register",
            method: "POST",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        // create expectation
        let expectation = XCTestExpectation(description: "Register request should succeed")
        
        // send request
        networkService.request<RegisterResponse>(endpoint)
            .sink { completion in
                switch completion {
                case .finished:
                    expectation.fulfill()
                case .failure(let error):
                    XCTFail("request failed: \(error.localizedDescription)")
                }
            } receiveValue: { response in
                // verify response
                XCTAssertEqual(response.data.userId, expectedResponse.userId)
                XCTAssertEqual(response.data.token, expectedResponse.token)
                XCTAssertEqual(response.data.expiresAt, expectedResponse.expiresAt)
                XCTAssertEqual(response.statusCode, 200)
            }
            .store(in: &cancellables)
        
        // wait for test to complete
        wait(for: [expectation], timeout: 5.0)
    }
    
    func testNetworkError() {
        // mock error
        mockSession.mockData = nil
        mockSession.mockResponse = nil
        mockSession.mockError = URLError(.notConnectedToInternet)
        
        let endpoint = Endpoint(path: "/v1/auth/register")
        
        let expectation = XCTestExpectation(description: "Request should fail with network error")
        
        networkService.request<RegisterResponse>(endpoint)
            .sink { completion in
                switch completion {
                case .finished:
                    XCTFail("request should not succeed")
                case .failure(let error):
                    if case .networkError = error {
                        expectation.fulfill()
                    } else {
                        XCTFail("expected network error, but got: \(error)")
                    }
                }
            } receiveValue: { _ in
                XCTFail("should not receive response")
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 5.0)
    }
    
    func testInvalidStatusCode() {
        // mock response
        let mockResponse = HTTPURLResponse(
            url: URL(string: "https://api.example.com")!,
            statusCode: 400,
            httpVersion: nil,
            headerFields: nil
        )!
        
        mockSession.mockData = Data()
        mockSession.mockResponse = mockResponse
        mockSession.mockError = nil
        
        let endpoint = Endpoint(path: "/v1/auth/register")
        
        let expectation = XCTestExpectation(description: "Request should fail with invalid status code")
        
        networkService.request<RegisterResponse>(endpoint)
            .sink { completion in
                switch completion {
                case .finished:
                    XCTFail("request should not succeed")
                case .failure(let error):
                    if case .invalidStatusCode(400) = error {
                        expectation.fulfill()
                    } else {
                        XCTFail("expected invalid status code, but got: \(error)")
                    }
                }
            } receiveValue: { _ in
                XCTFail("should not receive response")
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 5.0)
    }
}

// Mock URLSession
class MockSession: NetworkSessionProtocol {
    var mockData: Data?
    var mockResponse: URLResponse?
    var mockError: URLError?
    
    func makeDataTaskPublisher(for request: URLRequest) -> AnyPublisher<(data: Data, response: URLResponse), URLError> {
        if let error = mockError {
            return Fail(error: error).eraseToAnyPublisher()
        }
        if let data = mockData, let response = mockResponse {
            return Just((data: data, response: response))
                .setFailureType(to: URLError.self)
                .eraseToAnyPublisher()
        }
        return Fail(error: URLError(.badServerResponse)).eraseToAnyPublisher()
    }
}

class TestableNetworkService: NetworkServiceProtocol {
    private let session: NetworkSessionProtocol

    init(session: NetworkSessionProtocol) {
        self.session = session
    }

    func request<T: Codable>(_ endpoint: Endpoint) -> AnyPublisher<ApiResponse<T>, APIError> {
        guard let url = URL(string: endpoint.baseURL + endpoint.path) else {
            return Fail(error: APIError.invalidURL)
                .eraseToAnyPublisher()
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method
        request.httpBody = endpoint.body
        endpoint.headers?.forEach { request.addValue($1, forHTTPHeaderField: $0) }
        request.timeoutInterval = endpoint.timeout

        return session.makeDataTaskPublisher(for: request)
            .tryMap { data, response in
                guard let httpResponse = response as? HTTPURLResponse else {
                    throw APIError.invalidResponse
                }
                guard (200...299).contains(httpResponse.statusCode) else {
                    throw APIError.invalidStatusCode(httpResponse.statusCode)
                }
                do {
                    let decodedResponse = try JSONDecoder().decode(ApiResponse<T>.self, from: data)
                    return decodedResponse
                } catch {
                    throw APIError.decodingError(error)
                }
            }
            .mapError { error in
                if let urlError = error as? URLError, urlError.code == .timedOut {
                    return APIError.timeout
                }
                return APIError.networkError(error)
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()
    }
} 
} 
 frontend-together
