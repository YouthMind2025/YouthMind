import Foundation

final class ConversationAPI {
    static let shared = ConversationAPI()
    
    private let baseURL = "http://localhost:8080/v1" // Update with your backend URL
    private var authToken: String?
    
    private init() {}
    
    func setAuthToken(_ token: String) {
        self.authToken = token
    }
    
    // MARK: - Conversations
    
    func createConversation(title: String) async throws -> Conversation {
        guard let url = URL(string: "\(baseURL)/conversations") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let requestBody = ConversationCreateRequest(title: title)
        request.httpBody = try JSONEncoder().encode(requestBody)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 201 else {
            throw APIError.serverError(httpResponse.statusCode)
        }
        
        let apiResponse = try JSONDecoder().decode(ApiResponse<ConversationDto>.self, from: data)
        return convertToConversation(apiResponse.data)
    }
    
    func getConversations(page: Int = 1, limit: Int = 20) async throws -> [Conversation] {
        guard let url = URL(string: "\(baseURL)/conversations?page=\(page)&limit=\(limit)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw APIError.serverError(httpResponse.statusCode)
        }
        
        let apiResponse = try JSONDecoder().decode(ApiResponse<ConversationListResponse>.self, from: data)
        return apiResponse.data.conversations.map { convertToConversation($0) }
    }
    
    // MARK: - Messages
    
    func addMessage(conversationId: UUID, text: String, sender: String) async throws -> Message {
        guard let url = URL(string: "\(baseURL)/conversations/\(conversationId)/messages") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let requestBody = MessageCreateRequest(text: text, sender: sender)
        request.httpBody = try JSONEncoder().encode(requestBody)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 201 else {
            throw APIError.serverError(httpResponse.statusCode)
        }
        
        let apiResponse = try JSONDecoder().decode(ApiResponse<MessageDto>.self, from: data)
        return convertToMessage(apiResponse.data)
    }
    
    func getMessages(conversationId: UUID, page: Int = 1, limit: Int = 50) async throws -> [Message] {
        guard let url = URL(string: "\(baseURL)/conversations/\(conversationId)/messages?page=\(page)&limit=\(limit)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard httpResponse.statusCode == 200 else {
            throw APIError.serverError(httpResponse.statusCode)
        }
        
        let apiResponse = try JSONDecoder().decode(ApiResponse<MessageListResponse>.self, from: data)
        return apiResponse.data.messages.map { convertToMessage($0) }
    }
    
    // MARK: - Helper Methods
    
    private func convertToConversation(_ dto: ConversationDto) -> Conversation {
        return Conversation(
            id: dto.id,
            title: dto.title,
            createdAt: dto.createdAt,
            updatedAt: dto.updatedAt,
            messages: []
        )
    }
    
    private func convertToMessage(_ dto: MessageDto) -> Message {
        let sender: Message.Sender
        switch dto.sender {
        case "user":
            sender = .user
        case "ai":
            sender = .ai
        case "system":
            sender = .system
        default:
            sender = .user
        }
        
        return Message(
            id: dto.id,
            text: dto.text,
            sender: sender,
            timestamp: dto.timestamp
        )
    }
}

// MARK: - Request/Response Models

struct ConversationCreateRequest: Codable {
    let title: String
}

struct ApiResponse<T: Codable>: Codable {
    let success: Bool
    let data: T
    let message: String?
    let statusCode: Int
}

struct ConversationDto: Codable {
    let id: UUID
    let title: String
    let createdAt: Date
    let updatedAt: Date
}

struct MessageDto: Codable {
    let id: UUID
    let text: String
    let sender: String
    let timestamp: Date
}

struct ConversationListResponse: Codable {
    let conversations: [ConversationDto]
    let pagination: PaginationDto
}

struct MessageListResponse: Codable {
    let messages: [MessageDto]
    let pagination: PaginationDto
}

struct PaginationDto: Codable {
    let page: Int
    let limit: Int
    let total: Int
    let totalPages: Int
}

// MARK: - Error Types

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case serverError(Int)
    case decodingError
    case networkError
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .serverError(let code):
            return "Server error: \(code)"
        case .decodingError:
            return "Failed to decode response"
        case .networkError:
            return "Network error"
        }
    }
} 