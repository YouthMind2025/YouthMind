//
//  Message.swift
//  Saysomething
//
//  Created by user278797 on 7/11/25.
//

import Foundation

// MARK: - Message Model
struct Message: Identifiable, Equatable, Codable {
    let id = UUID()
    let text: String
    let sender: MessageSender
    let timestamp: Date
    
    enum MessageSender: String, Codable, CaseIterable {
        case user = "user"
        case ai = "ai"
        case system = "system"
        
        var displayName: String {
            switch self {
            case .user:
                return "User"
            case .ai:
                return "AI Assistant"
            case .system:
                return "System"
            }
        }
        
        var isFromUser: Bool {
            return self == .user
        }
    }
    
    init(text: String, sender: MessageSender, timestamp: Date = Date()) {
        self.text = text
        self.sender = sender
        self.timestamp = timestamp
    }
}

// MARK: - Message Extensions
extension Message {
    static func userMessage(_ text: String) -> Message {
        return Message(text: text, sender: .user)
    }
    
    static func aiMessage(_ text: String) -> Message {
        return Message(text: text, sender: .ai)
    }
    
    static func systemMessage(_ text: String) -> Message {
        return Message(text: text, sender: .system)
    }
} 