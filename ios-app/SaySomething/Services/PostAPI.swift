import Foundation
import Combine

struct PostAPI {
    // Fetch posts list
    static func fetchPosts() -> AnyPublisher<ApiResponse<[Post]>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        }
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts",
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
        )
        return NetworkService.shared.request(endpoint)
    }
    
    // Create new post
    static func createPost(title: String, content: String) -> AnyPublisher<ApiResponse<Post>, APIError> {
        let createPostRequest = CreatePostRequest(title: title, content: content)
        guard let body = try? JSONEncoder().encode(createPostRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Creating posts requires JWT
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized)).eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts/create",
            method: NetworkConfiguration.HTTPMethod.POST,
            headers: headers,
            body: body
        )
        return NetworkService.shared.request(endpoint)
    }
}

struct CreatePostRequest: Codable {
    let title: String
    let content: String
}

// Create post request - JWT required
extension CreatePostRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
}

// Fetch posts request - JWT required
struct FetchPostsRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
} 
