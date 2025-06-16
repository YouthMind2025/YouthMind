import XCTest
import Combine
@testable import Saysomething

class NetworkServiceTests: XCTestCase {
    private var cancellables = Set<AnyCancellable>()
    private var mockSession: MockURLSession!
    private var networkService: NetworkService!
    
    override func setUp() {
        super.setUp()
        mockSession = MockURLSession()
        networkService = NetworkService(session: mockSession)
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
        
        let responseData = try! JSONEncoder().encode(expectedResponse)
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
        networkService.request(endpoint)
            .sink { completion in
                switch completion {
                case .finished:
                    expectation.fulfill()
                case .failure(let error):
                    XCTFail("request failed: \(error.localizedDescription)")
                }
            } receiveValue: { (response: ApiResponse<RegisterResponse>) in
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
        
        networkService.request(endpoint)
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
        
        networkService.request(endpoint)
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
class MockURLSession: URLSession {
    var mockData: Data?
    var mockResponse: URLResponse?
    var mockError: Error?
    
    override func dataTaskPublisher(for request: URLRequest) -> URLSession.DataTaskPublisher {
        return MockDataTaskPublisher(
            request: request,
            mockData: mockData,
            mockResponse: mockResponse,
            mockError: mockError
        )
    }
}

// Mock DataTaskPublisher
struct MockDataTaskPublisher: Publisher {
    typealias Output = (data: Data, response: URLResponse)
    typealias Failure = Error
    
    let request: URLRequest
    let mockData: Data?
    let mockResponse: URLResponse?
    let mockError: Error?
    
    func receive<S>(subscriber: S) where S : Subscriber, Failure == S.Failure, Output == S.Input {
        if let error = mockError {
            subscriber.receive(completion: .failure(error))
            return
        }
        
        if let data = mockData, let response = mockResponse {
            _ = subscriber.receive((data: data, response: response))
            subscriber.receive(completion: .finished)
        } else {
            subscriber.receive(completion: .failure(APIError.invalidResponse))
        }
    }
} 