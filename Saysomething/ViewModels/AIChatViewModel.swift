import Foundation
import Combine

struct ChatMessage: Identifiable, Equatable {
    let id: UUID = UUID()
    let text: String
    let isUser: Bool
    let timestamp: Date = Date()
}

class AIChatViewModel: ObservableObject {
    @Published private(set) var messages: [ChatMessage] = []
    private var cancellables = Set<AnyCancellable>()
    
    // Simulate AI response (replace with real API call)
    func sendMessage(_ text: String) {
        let userMsg = ChatMessage(text: text, isUser: true)
        messages.append(userMsg)
        // Simulate network/AI delay
        Just(ChatMessage(text: "AI: \(text.reversed())", isUser: false))
            .delay(for: .milliseconds(500), scheduler: DispatchQueue.main)
            .sink { [weak self] aiMsg in
                self?.messages.append(aiMsg)
            }
            .store(in: &cancellables)
    }
} 