import Foundation

struct Conversation: Identifiable, Codable, Equatable {
    let id: UUID
    var title: String
    var createdAt: Date
    var updatedAt: Date
    var messages: [Message]
    var isPinned: Bool

    init(id: UUID = UUID(),
         title: String,
         createdAt: Date = Date(),
         updatedAt: Date = Date(),
         messages: [Message] = [],
         isPinned: Bool = false) {
        self.id = id
        self.title = title
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.messages = messages
        self.isPinned = isPinned
    }

    enum CodingKeys: String, CodingKey {
        case id, title, createdAt, updatedAt, messages, isPinned
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        title = try container.decode(String.self, forKey: .title)
        createdAt = try container.decode(Date.self, forKey: .createdAt)
        updatedAt = try container.decode(Date.self, forKey: .updatedAt)
        messages = try container.decode([Message].self, forKey: .messages)
        isPinned = (try? container.decode(Bool.self, forKey: .isPinned)) ?? false
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(title, forKey: .title)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encode(updatedAt, forKey: .updatedAt)
        try container.encode(messages, forKey: .messages)
        try container.encode(isPinned, forKey: .isPinned)
    }
}

extension Conversation {
    static func generateTitle(from messages: [Message]) -> String {
        if let firstUser = messages.first(where: { $0.sender == .user }) {
            return String(firstUser.text.prefix(40))
        }
        // If there's no user message yet, keep placeholder title
        return "New Chat"
    }
}


