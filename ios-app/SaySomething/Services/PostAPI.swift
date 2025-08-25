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
    
    // Toggle post resonance (like/unlike)
    static func toggleResonance(postId: UUID) -> AnyPublisher<ApiResponse<Post>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Resonance toggle requires JWT
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized)).eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts/\(postId)/resonance",
            method: NetworkConfiguration.HTTPMethod.PUT,
            headers: headers
        )
        return NetworkService.shared.request(endpoint)
    }
    
    // Toggle post collection (save/unsave)
    static func toggleCollection(postId: UUID) -> AnyPublisher<ApiResponse<Post>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Collection toggle requires JWT
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized)).eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/posts/\(postId)/collect",
            method: NetworkConfiguration.HTTPMethod.PUT,
            headers: headers
        )
        return NetworkService.shared.request(endpoint)
    }
    
    // Search posts with text query and optional emotion filter
    static func searchPosts(query: String, emotion: String? = nil, page: Int = 0, size: Int = 20, sortBy: String = "relevance") -> AnyPublisher<ApiResponse<FeedResponse>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Search requires JWT
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized)).eraseToAnyPublisher()
        }
        
        var path = "/v1/posts/search?query=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query)&page=\(page)&size=\(size)&sortBy=\(sortBy)"
        if let emotion = emotion {
            path += "&emotion=\(emotion.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? emotion)"
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: path,
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
        )
        return NetworkService.shared.request(endpoint)
    }
    
    // Advanced search with relevance scoring
    static func advancedSearch(query: String, emotion: String? = nil, page: Int = 0, size: Int = 20) -> AnyPublisher<ApiResponse<FeedResponse>, APIError> {
        var headers: [String: String] = [NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON]
        // Advanced search requires JWT
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized)).eraseToAnyPublisher()
        }
        
        var path = "/v1/posts/search/advanced?query=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query)&page=\(page)&size=\(size)"
        if let emotion = emotion {
            path += "&emotion=\(emotion.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? emotion)"
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: path,
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
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

// Search request - JWT required
struct SearchRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
} 
