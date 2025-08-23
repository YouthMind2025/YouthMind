import SwiftUI

struct ConversationHistoryView: View {
    @ObservedObject var viewModel: ChatViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(viewModel.conversations) { conversation in
                        Button(action: {
                            viewModel.selectConversation(conversation)
                            dismiss()
                        }) {
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: "message.fill")
                                    .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                                    .frame(width: 24, height: 24)
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack(spacing: 6) {
                                        if conversation.isPinned {
                                            Image(systemName: "pin.fill")
                                                .font(.caption)
                                                .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                                        }
                                        Text(conversation.title.isEmpty ? "New Chat" : conversation.title)
                                    }
                                        .font(.headline)
                                        .lineLimit(1)
                                    if let preview = conversation.messages.last?.text {
                                        Text(preview)
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                            .lineLimit(1)
                                    }
                                    Text(formattedDate(conversation.updatedAt))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                            }
                            .contentShape(Rectangle())
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button {
                                viewModel.togglePin(conversation)
                            } label: {
                                Label(conversation.isPinned ? "Unpin" : "Pin", systemImage: conversation.isPinned ? "pin.slash" : "pin")
                            }
                            Button(role: .destructive) {
                                viewModel.deleteConversation(conversation)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("History")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { viewModel.newConversation(); dismiss() }) {
                        Label("New Chat", systemImage: "plus")
                    }
                }
            }
        }
    }

    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}


