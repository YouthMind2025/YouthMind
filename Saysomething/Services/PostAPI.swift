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

    static func createPost(content: String, emotionTag: String) -> AnyPublisher<Bool, APIError> {
        guard let token = TokenManager.shared.getToken() else {
            return Fail(error: APIError.unauthorized).eraseToAnyPublisher()
        }
        let payload: [String: Any] = [
            "content": content,
            "emotionTag": emotionTag
        ]
        let endpoint = Endpoint(
            path: "/v1/posts/create",
            method: "POST",
            headers: [
                "Content-Type": "application/json",
                "Authorization": "Bearer \(token)"
            ],
            body: try? JSONSerialization.data(withJSONObject: payload)
        )
        return NetworkService().requestRaw(endpoint)
            .tryMap { response in
                guard let httpResponse = response as? HTTPURLResponse else { throw APIError.unknown }
                guard (200...299).contains(httpResponse.statusCode) else {
                    if httpResponse.statusCode == 401 { throw APIError.unauthorized }
                    throw APIError.serverError(status: httpResponse.statusCode)
                }
                return true
            }
            .mapError { $0 as? APIError ?? .unknown }
            .eraseToAnyPublisher()
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