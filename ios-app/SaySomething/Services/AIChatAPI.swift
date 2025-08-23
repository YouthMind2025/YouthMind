import Foundation

final class AIChatAPI {
    static let shared = AIChatAPI()

    private var apiKey: String = ""
    private let endpointURL = URL(string: "https://api.openai.com/v1/chat/completions")!
    private let defaultModel = "gpt-4o-mini"

    private init() {}

    func updateAPIKey(_ key: String) {
        apiKey = key
    }

    func sendChatCompletion(messages: [Message], model: String? = nil, temperature: Double = 0.7, maxTokens: Int? = nil) async throws -> String {
        guard !apiKey.isEmpty else {
            throw NSError(domain: "AIChatAPI", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing OpenAI API key"])
        }

        let roleMapped: [OpenAIChatMessage] = messages.map { msg in
            let role: String
            switch msg.sender {
            case .user: role = "user"
            case .ai: role = "assistant"
            case .system: role = "system"
            }
            return OpenAIChatMessage(role: role, content: msg.text)
        }

        let requestBody = OpenAIChatRequest(
            model: model ?? defaultModel,
            messages: roleMapped,
            temperature: temperature,
            max_tokens: maxTokens,
            stream: false
        )

        let jsonData = try JSONEncoder().encode(requestBody)

        var request = URLRequest(url: endpointURL)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.httpBody = jsonData

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw NSError(domain: "AIChatAPI", code: (response as? HTTPURLResponse)?.statusCode ?? -1, userInfo: [NSLocalizedDescriptionKey: message])
        }

        let decoded = try JSONDecoder().decode(OpenAIChatResponse.self, from: data)
        if let content = decoded.choices.first?.message.content, !content.isEmpty {
            return content
        }
        throw NSError(domain: "AIChatAPI", code: -2, userInfo: [NSLocalizedDescriptionKey: "Empty response from model"])
    }
}

private struct OpenAIChatMessage: Codable {
    let role: String
    let content: String
}

private struct OpenAIChatRequest: Codable {
    let model: String
    let messages: [OpenAIChatMessage]
    let temperature: Double?
    let max_tokens: Int?
    let stream: Bool?
}

private struct OpenAIChatResponse: Codable {
    struct Choice: Codable {
        struct Message: Codable {
            let role: String
            let content: String
        }
        let index: Int
        let message: Message
    }
    let choices: [Choice]
}


