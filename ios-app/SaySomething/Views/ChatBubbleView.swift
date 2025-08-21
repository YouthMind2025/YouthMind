//
//  ChatBubbleView.swift
//  SaySomething
//
//  Created by user278797 on 7/11/25.
//

import SwiftUI

// MARK: - Chat Bubble View
struct ChatBubble: View {
    let message: Message
    
    var body: some View {
        HStack {
            if message.sender.isFromUser {
                // user message to right
                Spacer(minLength: 60)
                VStack(alignment: .trailing, spacing: 4) {
                    Text(message.text)
                        .font(.customBody())
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple, DesignConstants.ColorPalette.brandPurple.opacity(0.85)]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
                    
                    // time of message
                    Text(formatTime(message.timestamp))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .padding(.trailing, 4)
                }
            } else {
                // ai message to left
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .top, spacing: 8) {
                        // AI Avatar
                        Image(systemName: "brain.head.profile")
                            .font(.title2)
                            .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                            .frame(width: 32, height: 32)
                            .background(DesignConstants.ColorPalette.brandPurple.opacity(0.1))
                            .clipShape(Circle())
                        
                        Text(message.text)
                            .font(.customBody())
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(message.sender == .system ? Color.orange.opacity(0.1) : Color(.systemGray6))
                            .foregroundColor(.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                            .shadow(color: Color.black.opacity(0.05), radius: 1, x: 0, y: 1)
                    }
                    
                    Text(formatTime(message.timestamp))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .padding(.leading, 40)
                }
                Spacer(minLength: 60)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
    
    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - Preview
struct ChatBubble_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            ChatBubble(message: Message(
                text: "Hello! How can I help you today?",
                sender: .ai,
                timestamp: Date()
            ))
            
            ChatBubble(message: Message(
                text: "I have a question about the app",
                sender: .user,
                timestamp: Date()
            ))
        }
        .padding()
    }
} 
