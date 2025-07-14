//
//  ResponsiveChatView.swift
//  Saysomething
//
//  Created by user278797 on 7/11/25.
//

import SwiftUI

// MARK: - Responsive Chat Layout
struct ResponsiveChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @FocusState private var isInputFocused: Bool
    // for horizontal device
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    // for vertical device
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    
    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                ZStack {
                    // Background gradient
                    LinearGradient(
                        gradient: Gradient(colors: [Color.blue.opacity(0.1), Color.purple.opacity(0.1)]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .ignoresSafeArea()
                    
                    VStack(spacing: 0) {
                        // Messages area
                        messagesArea
                            .frame(maxHeight: geometry.size.height * 0.8)
                        
                        // Input area
                        inputArea
                            .frame(height: max(80, geometry.size.height * 0.15))
                    }
                }
            }
            .navigationTitle("AI helper")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("clear") {
                        viewModel.messages.removeAll()
                        viewModel.loadMockData()
                    }
                    .foregroundColor(.blue)
                }
            }
        }
    }
    
    // MARK: - Messages Area
    private var messagesArea: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: adaptiveSpacing) {
                    ForEach(viewModel.messages) { message in
                        ResponsiveChatBubble(
                            message: message,
                            maxWidth: adaptiveMaxWidth
                        )
                        .id(message.id)
                    }
                    
                    // Loading indicator
                    if viewModel.isLoading {
                        ResponsiveLoadingIndicator()
                    }
                }
                .padding(.vertical, 8)
            }
            .onChange(of: viewModel.messages.count) { _ in
                if let lastMessage = viewModel.messages.last {
                    withAnimation(.easeOut(duration: 0.3)) {
                        proxy.scrollTo(lastMessage.id, anchor: .bottom)
                    }
                }
            }
        }
    }
    
    // MARK: - Input Area
    private var inputArea: some View {
        VStack(spacing: 0) {
            Divider()
                .background(Color(.systemGray4))
            
            HStack(spacing: adaptiveInputSpacing) {
                // Text input
                TextField("input message...", text: $viewModel.inputText, axis: .vertical)
                    .textFieldStyle(PlainTextFieldStyle())
                    .padding(.horizontal, adaptiveInputPadding)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .focused($isInputFocused)
                    .onSubmit {
                        viewModel.sendMessage()
                    }
                
                // Send button
                Button(action: {
                    viewModel.sendMessage()
                    isInputFocused = false
                }) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: adaptiveButtonSize, height: adaptiveButtonSize)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [Color.blue, Color.purple]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
                }
                .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading)
                .opacity(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isLoading ? 0.6 : 1.0)
            }
            .padding(.horizontal, adaptiveInputPadding)
            .padding(.vertical, 12)
            .background(Color(.systemBackground))
        }
    }
    
    // MARK: - Adaptive Properties
    private var adaptiveSpacing: CGFloat {
        switch (horizontalSizeClass, verticalSizeClass) {
        case (.compact, .compact): return 4
        case (.regular, .compact): return 6
        case (.compact, .regular): return 8
        case (.regular, .regular): return 12
        default: return 8
        }
    }
    
    private var adaptiveMaxWidth: CGFloat {
        switch horizontalSizeClass {
        case .compact: return 280
        case .regular: return 400
        default: return 320
        }
    }
    
    private var adaptiveInputSpacing: CGFloat {
        switch horizontalSizeClass {
        case .compact: return 8
        case .regular: return 12
        default: return 10
        }
    }
    
    private var adaptiveInputPadding: CGFloat {
        switch horizontalSizeClass {
        case .compact: return 12
        case .regular: return 16
        default: return 14
        }
    }
    
    private var adaptiveButtonSize: CGFloat {
        switch horizontalSizeClass {
        case .compact: return 40
        case .regular: return 44
        default: return 42
        }
    }
}

// MARK: - Responsive Chat Bubble
struct ResponsiveChatBubble: View {
    let message: ChatMessage
    let maxWidth: CGFloat
    
    var body: some View {
        HStack {
            if message.isFromUser {
                Spacer(minLength: 60)
                VStack(alignment: .trailing, spacing: 4) {
                    Text(message.content)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.8)]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
                        .frame(maxWidth: maxWidth, alignment: .trailing)
                    
                    Text(formatTime(message.timestamp))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .padding(.trailing, 4)
                }
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .top, spacing: 8) {
                        // AI Avatar
                        Image(systemName: "brain.head.profile")
                            .font(.title2)
                            .foregroundColor(.purple)
                            .frame(width: 32, height: 32)
                            .background(Color.purple.opacity(0.1))
                            .clipShape(Circle())
                        
                        Text(message.content)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(Color(.systemGray6))
                            .foregroundColor(.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                            .shadow(color: Color.black.opacity(0.05), radius: 1, x: 0, y: 1)
                            .frame(maxWidth: maxWidth, alignment: .leading)
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

// MARK: - Responsive Loading Indicator
struct ResponsiveLoadingIndicator: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "brain.head.profile")
                        .font(.title2)
                        .foregroundColor(.purple)
                        .frame(width: 32, height: 32)
                        .background(Color.purple.opacity(0.1))
                        .clipShape(Circle())
                    
                    HStack(spacing: 4) {
                        ForEach(0..<3) { index in
                            Circle()
                                .fill(Color.purple.opacity(0.6))
                                .frame(width: adaptiveDotSize, height: adaptiveDotSize)
                                .scaleEffect(1.0)
                                .animation(
                                    Animation.easeInOut(duration: 0.6)
                                        .repeatForever()
                                        .delay(Double(index) * 0.2),
                                    value: true
                                )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                }
            }
            Spacer(minLength: 60)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
    
    private var adaptiveDotSize: CGFloat {
        switch horizontalSizeClass {
        case .compact: return 6
        case .regular: return 8
        default: return 7
        }
    }
}

// MARK: - Preview
struct ResponsiveChatView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            ResponsiveChatView()
                .previewDevice("iPhone 15 Pro")
                .previewDisplayName("iPhone 15 Pro")
            
            ResponsiveChatView()
                .previewDevice("iPad Pro (12.9-inch)")
                .previewDisplayName("iPad Pro")
        }
    }
} 