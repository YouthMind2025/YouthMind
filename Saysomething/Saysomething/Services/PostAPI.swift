import Foundation
import Combine

struct PostAPI {
    // Fetch posts list
    static func fetchPosts() -> AnyPublisher<ApiResponse<[Post]>, APIError> {
        var headers: [String: String] = ["Content-Type": "application/json"]
        if let token = TokenManager.shared.getToken() {
            headers["Authorization"] = "Bearer \(token)"
        }
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts",
            method: "GET",
            headers: headers
        )
        return NetworkService().request(endpoint)
    }
    
    // Create new post
    static func createPost(title: String, content: String) -> AnyPublisher<ApiResponse<Post>, APIError> {
        let createPostRequest = CreatePostRequest(title: title, content: content)
        guard let body = try? JSONEncoder().encode(createPostRequest) else {
            return Fail(error: APIError.invalidResponse).eraseToAnyPublisher()
        }
        
        var headers: [String: String] = ["Content-Type": "application/json"]
        // Creating posts requires JWT
        if let token = TokenManager.shared.getToken() {
            headers["Authorization"] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(401)).eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts/create",
            method: "POST",
            headers: headers,
            body: body
        )
        return NetworkService().request(endpoint)
    }
}

struct Post: Codable {
    let id: String
    let title: String
    let content: String
    let authorId: String
    let createdAt: String?
    let updatedAt: String?
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