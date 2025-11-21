import SwiftUI

struct ConversationSidebarView: View {
    @ObservedObject var viewModel: ChatViewModel
    var onClose: () -> Void

    @State private var dragOffsetX: CGFloat = 0

    var body: some View {
        ZStack(alignment: .leading) {
            sidebarContent
                .frame(maxWidth: 340)
                .background(.ultraThinMaterial)
                .overlay(
                    Rectangle()
                        .fill(Color.black.opacity(0.04))
                        .allowsHitTesting(false)
                )
                .shadow(color: Color.black.opacity(0.12), radius: 16, x: 0, y: 10)
                .offset(x: dragOffsetX)
                .gesture(closeDragGesture)
                .accessibilityIdentifier("conversationSidebar")
        }
        .frame(maxHeight: .infinity)
    }

    private var sidebarContent: some View {
        VStack(spacing: 0) {
            header
            Divider().opacity(0.6)
            ScrollView(showsIndicators: true) {
                LazyVStack(spacing: 6, pinnedViews: []) {
                    ForEach(viewModel.conversations) { conversation in
                        ConversationRow(
                            conversation: conversation,
                            isActive: viewModel.currentConversationId == conversation.id
                        ) {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                                viewModel.selectConversation(conversation)
                                onClose()
                            }
                        }
                        .contextMenu {
                            Button {
                                viewModel.togglePin(conversation)
                            } label: {
                                Label(conversation.isPinned ? "Unpin" : "Pin to top", systemImage: conversation.isPinned ? "pin.slash" : "pin")
                            }
                            Button(role: .destructive) {
                                viewModel.deleteConversation(conversation)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
            }
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Text("Conversations")
                .font(.system(.title3, design: .rounded).weight(.semibold))
                .foregroundColor(DesignConstants.ColorPalette.textPrimary)

            Spacer()

            Button(action: {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                    viewModel.newConversation()
                    onClose()
                }
            }) {
                Image(systemName: "plus")
                    .font(.headline)
                    .foregroundColor(DesignConstants.ColorPalette.primary)
                    .frame(width: 32, height: 32)
                    .background(DesignConstants.ColorPalette.primary.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
    }

    private var closeDragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                // Drag left to close (negative values). Limit to not drag right.
                dragOffsetX = min(0, value.translation.width)
            }
            .onEnded { value in
                let predicted = value.predictedEndTranslation.width
                let shouldClose = value.translation.width < -90 || predicted < -150
                withAnimation(.spring(response: 0.35, dampingFraction: 0.88)) {
                    dragOffsetX = 0
                    if shouldClose { onClose() }
                }
            }
    }
}

    private struct ConversationRow: View {
    let conversation: Conversation
    let isActive: Bool
    var onTap: () -> Void
    @State private var isHovering: Bool = false

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(isActive ? DesignConstants.ColorPalette.primary : Color.clear)
                    .frame(width: 3)

                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .firstTextBaseline) {
                            HStack(spacing: 6) {
                                if conversation.isPinned {
                                    Image(systemName: "pin.fill")
                                        .font(.caption)
                                        .foregroundColor(DesignConstants.ColorPalette.primary)
                                }
                                Text(titleText)
                            }
                            .font(.system(.subheadline, design: .rounded).weight(.semibold))
                            .foregroundColor(DesignConstants.ColorPalette.textPrimary)
                            .lineLimit(1)
                            .truncationMode(.tail)
                        Spacer()
                        Text(formattedDate(conversation.updatedAt))
                            .font(.caption2)
                            .foregroundColor(DesignConstants.ColorPalette.textSecondary)
                    }

                    Text(previewText)
                        .font(.caption)
                        .foregroundColor(DesignConstants.ColorPalette.textSecondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(isActive ? DesignConstants.ColorPalette.primary.opacity(0.10) : (isHovering ? Color.black.opacity(0.04) : Color.clear))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(isActive ? DesignConstants.ColorPalette.primary.opacity(0.20) : Color.clear, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovering in
            #if os(iOS)
            // onHover is a no-op on iOS; keep state unchanged
            #else
            isHovering = hovering
            #endif
        }
        .animation(.easeInOut(duration: 0.15), value: isHovering)
        .animation(.easeInOut(duration: 0.2), value: isActive)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(titleText))
        .accessibilityHint(Text("Open conversation"))
    }

    private var titleText: String {
        let title = conversation.title.isEmpty ? "New Chat" : conversation.title
        return title
    }

    private var previewText: String {
        if let first = conversation.messages.first?.text, !first.isEmpty {
            return first
        }
        return "No messages yet"
    }

    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}


