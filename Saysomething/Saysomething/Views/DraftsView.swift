import SwiftUI
import CoreData

struct DraftsView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        entity: DraftPost.entity(),
        sortDescriptors: [NSSortDescriptor(keyPath: \DraftPost.timestamp, ascending: false)]
    ) private var drafts: FetchedResults<DraftPost>
    
    @State private var mockContent: String = ""
    @State private var mockEmotion: String = ""
    
    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(gradient: Gradient(colors: [Color.purple.opacity(0.08), Color.blue.opacity(0.10)]), startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                VStack(spacing: 24) {
                    HStack(spacing: 8) {
                        Image(systemName: "doc.text")
                            .foregroundColor(.indigo)
                        Text("Drafts (")
                            .font(.title2)
                            .fontWeight(.semibold)
                        + Text("\(drafts.count)").font(.title2).bold().foregroundColor(.purple)
                        + Text(")")
                    }
                    .padding(.top, 16)
                    
                    List {
                        ForEach(drafts, id: \.id) { draft in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(draft.content ?? "(No Content)")
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                HStack {
                                    Image(systemName: "face.smiling")
                                        .foregroundColor(.orange)
                                    Text("Emotion: \(draft.emotionTag ?? "-")")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                HStack {
                                    Image(systemName: "clock")
                                        .foregroundColor(.blue)
                                    Text("Saved: \(draft.timestamp ?? Date(), formatter: dateFormatter)")
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding()
                            .background(Color.white.opacity(0.9))
                            .cornerRadius(14)
                            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
                        }
                        .onDelete { indexSet in
                            for index in indexSet {
                                context.delete(drafts[index])
                            }
                            try? context.save()
                        }
                    }
                    .listStyle(.plain)
                    .background(Color.clear)
                    
                    // Mock entry UI
                    VStack(spacing: 10) {
                        Text("Add New Draft")
                            .font(.headline)
                            .foregroundColor(.indigo)
                        TextField("Draft content...", text: $mockContent)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                        TextField("Emotion tag...", text: $mockEmotion)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                        Button(action: {
                            let newDraft = DraftPost(context: context)
                            newDraft.id = UUID()
                            newDraft.content = mockContent
                            newDraft.timestamp = Date()
                            newDraft.emotionTag = mockEmotion
                            try? context.save()
                            mockContent = ""
                            mockEmotion = ""
                        }) {
                            Label("Add Draft", systemImage: "plus.circle.fill")
                                .font(.headline)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.purple)
                        .disabled(mockContent.isEmpty || mockEmotion.isEmpty)
                    }
                    .padding()
                    .background(Color.white.opacity(0.95))
                    .cornerRadius(16)
                    .shadow(color: Color.purple.opacity(0.08), radius: 8, x: 0, y: 4)
                    .padding(.horizontal)
                    Spacer()
                }
            }
            .navigationTitle("Drafts")
        }
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .short
    formatter.timeStyle = .short
    return formatter
}() 