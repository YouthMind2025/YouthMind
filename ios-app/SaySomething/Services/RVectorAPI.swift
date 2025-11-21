//
//  RVectorAPI.swift
//  SaySomething


import Foundation
import Combine

struct RVectorAPI {
    
    /// Get current R-vector for authenticated user
    /// GET /v1/recommendation/rvector
    static func getRVector() -> AnyPublisher<ApiResponse<RVectorResponse>, APIError> {
        var headers: [String: String] = [
            NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON
        ]
        
        // JWT required
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized))
                .eraseToAnyPublisher()
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: "/v1/recommendation/rvector",
            method: NetworkConfiguration.HTTPMethod.GET,
            headers: headers
        )
        
        return NetworkService.shared.request(endpoint)
    }
    
    /// Reset R-vector for authenticated user
    /// POST /v1/recommendation/rvector/reset
    static func resetRVector(reason: String? = nil) -> AnyPublisher<ApiResponse<RVectorResetResponse>, APIError> {
        var headers: [String: String] = [
            NetworkConfiguration.Headers.contentType: NetworkConfiguration.Headers.applicationJSON
        ]
        
        // JWT required
        if let token = TokenManager.shared.getToken() {
            headers[NetworkConfiguration.Headers.authorization] = "Bearer \(token)"
        } else {
            return Fail(error: APIError.invalidStatusCode(NetworkConfiguration.StatusCode.unauthorized))
                .eraseToAnyPublisher()
        }
        
        var path = "/v1/recommendation/rvector/reset"
        if let reason = reason {
            let encodedReason = reason.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? reason
            path += "?reason=\(encodedReason)"
        }
        
        let endpoint = Endpoint(
            baseURL: "http://localhost:8080",
            path: path,
            method: NetworkConfiguration.HTTPMethod.POST,
            headers: headers
        )
        
        return NetworkService.shared.request(endpoint)
    }
}
