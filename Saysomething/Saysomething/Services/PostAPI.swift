import Foundation
import Combine

struct PostAPI {
    static func fetchPosts() -> AnyPublisher<ApiResponse<[Post]>, APIError> {
        var headers: [String: String] = ["Content-Type": "application/json"]
        if let token = TokenManager.shared.getToken() {
            headers["Authorization"] = "Bearer \(token)"
        }
        let endpoint = Endpoint(
            path: "/posts",
            method: "GET",
            headers: headers
        )
        return NetworkService().request(endpoint)
    }
}

struct Post: Codable {
    let id: String
    let title: String
    let content: String
    let authorId: String
}

// Example: Use JWTInjectable for conditional JWT injection
struct FetchPostsRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true } // JWT needed for fetching posts
}

// Usage (for reference):
// let fetchReq = FetchPostsRequest()
// if fetchReq.shouldInjectJWT, let token = TokenManager.shared.getToken() {
//     headers["Authorization"] = "Bearer \(token)"
// } 