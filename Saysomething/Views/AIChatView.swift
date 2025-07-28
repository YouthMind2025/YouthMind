import SwiftUI

struct ChatMessageBubble: View {
    let msg: ChatMessage
    var body: some View {
        VStack(alignment: msg.isUser ? .trailing : .leading, spacing: 2) {
            Text(msg.text)
                .padding(12)
                .background(msg.isUser ? Color.blue.gradient : Color(.systemGray5))
                .foregroundColor(msg.isUser ? .white : .primary)
                .cornerRadius(18)
                .shadow(color: msg.isUser ? Color.blue.opacity(0.10) : Color.gray.opacity(0.05), radius: 2, y: 2)
                .transition(.move(edge: msg.isUser ? .trailing : .leading).combined(with: .opacity))
            Text(msg.timestamp, style: .time)
                .font(.caption2)
                .foregroundColor(.gray)
                .padding(msg.isUser ? .trailing : .leading, 6)
        }
        .frame(maxWidth: 260, alignment: msg.isUser ? .trailing : .leading)
    }
}

struct AIChatView: View {
    @StateObject private var viewModel = AIChatViewModel()
    @State private var inputText: String = ""
    @FocusState private var isInputFocused: Bool
    
    var body: some View {
        ZStack {
            // Soft gradient background
            LinearGradient(gradient: Gradient(colors: [Color.purple.opacity(0.08), Color.blue.opacity(0.10)]), startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
            VStack(spacing: 0) {
                // Header
                HStack(spacing: 10) {
                    Image(systemName: "sparkles")
                        .font(.title2)
                        .foregroundColor(.purple)
                    Text("AI Chat")
                        .font(.title2.bold())
                        .foregroundColor(.primary)
                    Spacer()
                    // Debug button for performance testing
                    #if DEBUG
                    Button(action: populateLongChat) {
                        Image(systemName: "bolt.fill")
                            .foregroundColor(.yellow)
                    }
                    .help("Populate with many messages for performance testing")
                    #endif
                }
                .padding(.horizontal)
                .padding(.top, 12)
                .padding(.bottom, 4)
                // Message List
                ChatMessagesView(messages: viewModel.messages)
                    .background(Color.clear)
                    .onAppear {
                        // Profile this section with Instruments (Time Profiler, Memory, etc.)
                    }
                Divider()
                // Input Bar
                HStack(spacing: 10) {
                    TextField("Type a message...", text: $inputText, axis: .vertical)
                        .textFieldStyle(.plain)
                        .padding(10)
                        .background(Color(.systemGray6))
                        .cornerRadius(16)
                        .focused($isInputFocused)
                        .onSubmit {
                            send()
                        }
                    Button(action: send) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(.white)
                            .padding(10)
                            .background(inputText.trimmingCharacters(in: .whitespaces).isEmpty ? Color.gray : Color.blue)
                            .clipShape(Circle())
                            .shadow(radius: 2, y: 2)
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                .padding(.horizontal)
                .padding(.vertical, 10)
                .background(.ultraThinMaterial)
            }
        }
        .onTapGesture { isInputFocused = false }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func send() {
        guard !inputText.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        viewModel.sendMessage(inputText)
        inputText = ""
    }
    #if DEBUG
    private func populateLongChat() {
        for i in 1...100 {
            let userMsg = ChatMessage(text: "User message \(i)", isUser: true)
            let aiMsg = ChatMessage(text: "AI reply to message \(i)", isUser: false)
            viewModel.messages.append(userMsg)
            viewModel.messages.append(aiMsg)
        }
    }
    #endif
}

struct ChatMessagesView: View {
    let messages: [ChatMessage]
    
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 14) {
                    ForEach(messages) { msg in
                        HStack(alignment: .bottom, spacing: 8) {
                            if msg.isUser { Spacer() }
                            ChatMessageBubble(msg: msg)
                            if !msg.isUser { Spacer() }
                        }
                        .id(msg.id)
                    }
                }
                .padding(.vertical, 16)
                .padding(.horizontal, 8)
            }
            .onChange(of: messages.count) { _ in
                // Scroll to bottom on new message
                if let last = messages.last {
                    withAnimation(.easeOut(duration: 0.4)) { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }
        }
    }
}

struct AIChatView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            AIChatView()
        }
    }
} 