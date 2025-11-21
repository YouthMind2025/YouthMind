//
//  ApiResponse.swift
//  SaySomething
//
//  Created by Dawei Bao on 05/08/25.
//

import Foundation

/// Generic API response wrapper for all network requests
/// Provides a consistent structure for handling API responses across the app
struct ApiResponse<T: Codable>: Codable {
    /// The actual data payload from the API
    let data: T
    
    /// HTTP status code of the response
    let statusCode: Int
    
    /// Response headers as a dictionary
    let headers: [String: String]
    
    /// Initialize with required fields
    init(data: T, statusCode: Int, headers: [String: String] = [:]) {
        self.data = data
        self.statusCode = statusCode
        self.headers = headers
    }
}

/// Extension to provide convenience methods for ApiResponse
extension ApiResponse {
    /// Check if the response indicates success (status code 200-299)
    var isSuccess: Bool {
        return statusCode >= 200 && statusCode < 300
    }
    
    /// Get a specific header value
    /// - Parameter key: The header key to retrieve
    /// - Returns: The header value if it exists, nil otherwise
    func headerValue(for key: String) -> String? {
        return headers[key]
    }
    
    /// Check if the response contains a specific header
    /// - Parameter key: The header key to check
    /// - Returns: True if the header exists, false otherwise
    func hasHeader(_ key: String) -> Bool {
        return headers[key] != nil
    }
} 
