//
//  Message.swift
//  Saysomething
//
//  Created by user278797 on 7/11/25.
//

import Foundation

// Message Model
struct Message: Identifiable, Equatable, Codable {
    // message id - UUID
    let id = UUID()
    // message text
    let text: String
    // message sender (user, ai, system)
    let sender: MessageSender
    // message timestamp
    let timestamp: Date
    
    // define message sender
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
    
    // initialize message
    init(text: String, sender: MessageSender, timestamp: Date = Date()) {
        self.text = text
        self.sender = sender
        self.timestamp = timestamp
    }
}

// Message Extensions
extension Message {
    // create user message
    static func userMessage(_ text: String) -> Message {
        return Message(text: text, sender: .user)
    }
    
    // create ai message
    static func aiMessage(_ text: String) -> Message {
        return Message(text: text, sender: .ai)
    }
    
    // create system message
    static func systemMessage(_ text: String) -> Message {
        return Message(text: text, sender: .system)
    }
} 