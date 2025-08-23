import Foundation

final class ConversationStore {
    static let shared = ConversationStore()

    private let fileURL: URL
    private let queue = DispatchQueue(label: "ConversationStore.queue", qos: .background)

    private init() {
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.fileURL = directory.appendingPathComponent("conversations.json")
    }

    func loadConversations() -> [Conversation] {
        guard let data = try? Data(contentsOf: fileURL) else { return [] }
        do {
            let conversations = try JSONDecoder().decode([Conversation].self, from: data)
            return conversations.sorted(by: { $0.updatedAt > $1.updatedAt })
        } catch {
            print("[ConversationStore] load error: \(error)")
            return []
        }
    }

    func saveConversations(_ conversations: [Conversation]) {
        queue.async {
            do {
                let data = try JSONEncoder().encode(conversations)
                try data.write(to: self.fileURL, options: [.atomic])
            } catch {
                print("[ConversationStore] save error: \(error)")
            }
        }
    }
}


