//
//  ChatView.swift
//  Saysomething
//
//  Created by Dawei Bao on 05/08/25.
//

import SwiftUI
import UIKit

// MARK: - Main Chat View
struct ChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @FocusState private var isInputFocused: Bool
    @State private var showHistory: Bool = false
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    var body: some View {
        NavigationStack {
            ZStack(alignment: .leading) {
                // Background gradient
                LinearGradient(
                    gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.08), DesignConstants.ColorPalette.brandPurple.opacity(0.14)]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                // Main chat content
                VStack(spacing: 0) {
                    // Connection Status
                    HStack {
                        Circle()
                            .fill(viewModel.connectionStatus.color)
                            .frame(width: 8, height: 8)
                        Text(viewModel.connectionStatus.displayText)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    
                    // Messages ScrollView
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 8) {
                                ForEach(viewModel.messages) { message in
                                    ChatBubble(message: message)
                                        .id(message.id)
                                        .transition(.asymmetric(
                                            insertion: .move(edge: .bottom).combined(with: .opacity),
                                            removal: .opacity
                                        ))
                                }
                                
                                // Loading indicator for AI response
                                if viewModel.isLoading {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            HStack(alignment: .top, spacing: 8) {
                                                Image(systemName: "brain.head.profile")
                                                    .font(.title2)
                                                    .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                                                    .frame(width: 32, height: 32)
                                                    .background(DesignConstants.ColorPalette.brandPurple.opacity(0.1))
                                                    .clipShape(Circle())
                                                
                                                HStack(spacing: 4) {
                                                    ForEach(0..<3) { index in
                                                        Circle()
                                                            .fill(DesignConstants.ColorPalette.brandPurple.opacity(0.8))
                                                            .frame(width: 8, height: 8)
                                                            .scaleEffect(1.0)
                                                            .animation(
                                                                Animation.easeInOut(duration: 0.6)
                                                                    .repeatForever()
                                                                    .delay(Double(index) * 0.2),
                                                                value: viewModel.isLoading
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
                            }
                            .padding(.vertical, 8)
                        }
                        // scroll to bottom when new message is added
                        .onChange(of: viewModel.messages.count) { _ in
                            if let lastMessage = viewModel.messages.last {
                                withAnimation(.easeOut(duration: 0.3)) {
                                    proxy.scrollTo(lastMessage.id, anchor: .bottom)
                                }
                            }
                        }
                    }
                    
                    // Suggestions
                    if viewModel.showSuggestions && !viewModel.suggestions.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(viewModel.suggestions, id: \.self) { suggestion in
                                    Button(action: {
                                        viewModel.sendSuggestion(suggestion)
                                    }) {
                                        Text(suggestion)
                                            .font(.caption)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(DesignConstants.ColorPalette.brandPurple.opacity(0.12))
                                            .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                                            .clipShape(RoundedRectangle(cornerRadius: 12))
                                    }
                                }
                            }
                            .padding(.horizontal, 16)
                        }
                        .padding(.vertical, 8)
                    }
                    
                    // Input area
                    VStack(spacing: 0) {
                        Divider()
                            .background(Color(.systemGray4))
                        
                        HStack(spacing: 12) {
                            // Text input
                            TextField("Type a message...", text: $viewModel.inputText, axis: .vertical)
                                .textFieldStyle(PlainTextFieldStyle())
                                .padding(.horizontal, 16)
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
                                    .frame(width: 44, height: 44)
                                    .background(
                                        LinearGradient(
                                            gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple, DesignConstants.ColorPalette.brandPurple.opacity(0.9)]),
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
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(.systemBackground))
                    }
                }
                .padding(.leading, isSidebarPinned ? 340 : 0)
                .contentShape(Rectangle())
                .gesture(edgeOpenGesture)

                // Dimming backdrop when sidebar open
                if showHistory && !isSidebarPinned {
                    Color.black.opacity(0.28)
                        .ignoresSafeArea()
                        .transition(.opacity)
                        .onTapGesture {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                                showHistory = false
                            }
                        }
                }

                // Sidebar Overlay
                if showHistory || isSidebarPinned {
                    ConversationSidebarView(viewModel: viewModel) {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                            showHistory = false
                        }
                    }
                    .frame(maxHeight: .infinity)
                    .transition(.move(edge: .leading).combined(with: .opacity))
                }
            }
            .navigationTitle(viewModel.currentConversationId != nil ? "AI Assistant" : "AI Assistant")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Group {
                        if !isSidebarPinned {
                            Button(action: {
                                withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                                    showHistory = true
                                }
                            }) {
                                Image(systemName: "line.3.horizontal")
                            }
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: { viewModel.newConversation() }) {
                            Label("New Chat", systemImage: "plus")
                        }
                        Button(action: { viewModel.clearChat() }) {
                            Label("Clear Current", systemImage: "trash")
                        }
                        Button(action: {
                            let text = viewModel.exportChat()
                            UIPasteboard.general.string = text
                        }) {
                            Label("Copy Transcript", systemImage: "doc.on.doc")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            // Sheet replaced by inline sidebar
        }
    }
}

private extension ChatView {
    var isSidebarPinned: Bool {
        horizontalSizeClass == .regular
    }

    var edgeOpenGesture: some Gesture {
        DragGesture(minimumDistance: 15, coordinateSpace: .local)
            .onEnded { value in
                guard !isSidebarPinned, !showHistory else { return }
                let isFromLeftEdge = value.startLocation.x < 24
                let draggedRight = value.translation.width > 60
                if isFromLeftEdge && draggedRight {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                        showHistory = true
                    }
                }
            }
    }
}

//
//  Preview
//
struct ChatView_Previews: PreviewProvider {
    static var previews: some View {
        ChatView()
    }
} 
