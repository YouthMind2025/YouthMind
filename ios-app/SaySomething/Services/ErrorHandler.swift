import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case networkError(Error)
    case invalidResponse
    case invalidStatusCode(Int)
    case decodingError(Error)
    case timeout
    case unauthorized
    case forbidden
    case notFound
    case serverError
    case noInternetConnection
    case connectionFailed
    case connectionLost

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .invalidResponse:
            return "Invalid response"
        case .invalidStatusCode(let code):
            switch code {
            case 401:
                return "Unauthorized, please login again"
            case 403:
                return "Forbidden"
            case 404:
                return "Resource not found"
            case 500...599:
                return "Server error"
            default:
                return "Request failed (status code: \(code))"
            }
        case .decodingError(let error):
            return "Data parsing error: \(error.localizedDescription)"
        case .timeout:
            return "Request timeout - please check your connection"
        case .unauthorized:
            return "Unauthorized, please login again"
        case .forbidden:
            return "Forbidden"
        case .notFound:
            return "Resource not found"
        case .serverError:
            return "Server error"
        case .noInternetConnection:
            return "No internet connection - please check your network"
        case .connectionFailed:
            return "Connection failed - please try again"
        case .connectionLost:
            return "Connection lost - please check your network"
        }
    }

    var isRetryable: Bool {
        switch self {
        case .timeout, .networkError, .noInternetConnection, .connectionFailed, .connectionLost:
            return true
        case .invalidURL, .invalidResponse, .invalidStatusCode, .decodingError, .unauthorized, .forbidden, .notFound, .serverError:
            return false
        }
    }
} 