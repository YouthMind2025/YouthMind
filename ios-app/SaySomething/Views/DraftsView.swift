import SwiftUI
import CoreData
import UIKit

struct DraftsView: View {
    private enum Field: Hashable { case title, content }
    @Environment(\.managedObjectContext) private var context
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var feedViewModel: FeedViewModel
    @FetchRequest(
        entity: DraftPost.entity(),
        sortDescriptors: [NSSortDescriptor(keyPath: \DraftPost.timestamp, ascending: false)]
    ) private var drafts: FetchedResults<DraftPost>
    
    @State private var title: String = ""
    @State private var content: String = ""
    @State private var selectedEmotion: Emotion = .happy
    @State private var topics: [String] = []
    @State private var showingTagInput = false
    @State private var newTag = ""
    @State private var showingEmotionSelection = false  // 添加这一行
    @FocusState private var focusedField: Field?
    @State private var showTagSuggestions: Bool = false
    @State private var tagQuery: String = ""
    
    private let maxTags = 5
    private let maxTagLength = 20
    private let trendingTags: [String] = [
        "BreakUp", "Work", "ExamWeek", "Anxiety", "Friendship", "Family", "SelfCare",
        "Study", "Fitness", "Love", "Career", "Sleep", "Stress", "Lonely",
        "Motivation", "Travel", "Hobby", "Music", "Movie", "Book"
    ]
    
    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.10), DesignConstants.ColorPalette.brandPurple.opacity(0.05)]), startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                    .onTapGesture {
                        focusedField = nil
                    }
                
                VStack(spacing: 0) {
                    // Header with back button and Link a Post button
                    HStack {
                        Button(action: {
                            dismiss()
                        }) {
                            Image(systemName: "chevron.left")
                                .font(.title2)
                                .foregroundColor(.primary)
                        }
                        
                        Spacer()
                        
                        Button(action: {
                            // Link a Post action
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "paperclip")
                                    .font(.caption)
                                Text("Link a Post")
                                    .font(.caption)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.black)
                            .cornerRadius(20)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    
                    // Main content area
                    VStack(spacing: 20) {
                        // Combined input field with divider
                        VStack(spacing: 0) {
                            // Title input field
                            ZStack(alignment: .bottomTrailing) {
                                TextField("Add Title", text: $title)
                                    .font(.title3)
                                    .fontWeight(.bold)
                                    .focused($focusedField, equals: .title)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .padding(.bottom, 6)
                                    .background(Color.white)
                                    .onChange(of: title) { value in
                                        if value.count > 60 {
                                            title = String(value.prefix(60))
                                        }
                                    }
                                
                                // Character count indicator
                                Text("\(title.count)/60")
                                    .font(.caption2)
                                    .foregroundColor(.gray)
                                    .padding(.horizontal, 16)
                                    .padding(.bottom, 8)
                            }
                            
                            // Divider line
                            Divider()
                                .background(Color.gray.opacity(0.3))
                                .padding(.horizontal, 16)
                            
                            // Content input field + tag suggestions overlay
                            GeometryReader { contentArea in
                                ZStack(alignment: .topLeading) {
                                    ScrollView(showsIndicators: false) {
                                        VStack(spacing: 0) {
                                            TextField("Add Content", text: $content, axis: .vertical)
                                                .font(.body)
                                                .lineLimit(1...)
                                                .focused($focusedField, equals: .content)
                                                .padding(.horizontal, 16)
                                                .padding(.top, 12)
                                                .padding(.bottom, 12)
                                                .background(Color.white)
                                            Spacer(minLength: 0)
                                        }
                                        .frame(minHeight: contentArea.size.height, alignment: .top)
                                    }
                                    .scrollDismissesKeyboard(.interactively)

                                    if showTagSuggestions {
                                        VStack(alignment: .leading, spacing: 8) {
                                            ForEach(suggestedTags, id: \.self) { tag in
                                                Button(action: {
                                                    insertSuggestedTag(tag)
                                                }) {
                                                    HStack(spacing: 8) {
                                                        Text("#\(tag)")
                                                            .font(.caption)
                                                            .fontWeight(.medium)
                                                            .foregroundColor(.primary)
                                                        Spacer()
                                                    }
                                                    .padding(.horizontal, 10)
                                                    .padding(.vertical, 8)
                                                    .background(Color.white)
                                                    .overlay(
                                                        RoundedRectangle(cornerRadius: 8)
                                                            .stroke(DesignConstants.ColorPalette.brandPurple.opacity(0.2), lineWidth: 1)
                                                    )
                                                }
                                                .buttonStyle(.plain)
                                            }
                                        }
                                        .padding(.horizontal, 16)
                                        .padding(.top, 8)
                                        .padding(.bottom, 8)
                                        .background(.ultraThinMaterial)
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .shadow(color: Color.black.opacity(0.05), radius: 6, x: 0, y: 3)
                                        .padding(.leading, 8)
                                        .padding(.top, 44)
                                        .frame(maxWidth: min(contentArea.size.width - 24, 320), alignment: .leading)
                                        .zIndex(1)
                                    }
                                }
                            }
                            .frame(maxHeight: .infinity)
                            .background(Color.white)
                        }
                        .background(Color.white)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(DesignConstants.ColorPalette.brandPurple.opacity(0.3), lineWidth: 1)
                        )
                        .frame(height: min(max(UIScreen.main.bounds.height * 0.5, 320), UIScreen.main.bounds.height - 160))
                        
                        // Bottom section with tags and buttons
                        VStack(spacing: 16) {
                            // Tags section
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text("Tags")
                                        .font(.headline)
                                    Spacer()
                                    Text("(\(topics.count)/\(maxTags))")
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                                HStack {
                                    Button(action: {
                                        if topics.count < maxTags {
                                            showingTagInput = true
                                        }
                                    }) {
                                        HStack(spacing: 6) {
                                            Image(systemName: "number")
                                                .font(.caption)
                                            Text("Tag+")
                                                .font(.caption)
                                                .fontWeight(.medium)
                                        }
                                        .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(DesignConstants.ColorPalette.brandPurple.opacity(0.1))
                                        .cornerRadius(16)
                                    }
                                    .disabled(topics.count >= maxTags)
                                    Spacer()
                                }
                                
                                // Display existing tags
                                if !topics.isEmpty {
                                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 8) {
                                        ForEach(topics.indices, id: \.self) { index in
                                            HStack(spacing: 6) {
                                                Text("#\(topics[index])")
                                                    .font(.caption)
                                                    .fontWeight(.medium)
                                                    .foregroundColor(.primary)
                                                    .lineLimit(1)
                                                Button(action: { topics.remove(at: index) }) {
                                                    Image(systemName: "xmark")
                                                        .font(.system(size: 10, weight: .bold))
                                                        .foregroundColor(.white)
                                                        .frame(width: 16, height: 16)
                                                        .background(Circle().fill(DesignConstants.ColorPalette.brandPurple))
                                                }
                                                .buttonStyle(.plain)
                                            }
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 6)
                                            .background(DesignConstants.ColorPalette.brandPurple.opacity(0.08))
                                            .overlay(Capsule().stroke(DesignConstants.ColorPalette.brandPurple.opacity(0.22), lineWidth: 0.8))
                                            .clipShape(Capsule())
                                        }
                                    }
                                }
                            }
                            
                            // Action buttons
                            HStack(spacing: 16) {
                                Button(action: {
                                    saveDraft()
                                }) {
                                    HStack(spacing: 5.0) {
                                        Image(systemName: "arrow.down.circle")
                                            .font(.body)
                                        Text("Save Draft")
                                            .font(.body)
                                            .fontWeight(.medium)
                                    }
                                    .foregroundColor(.gray)
                                }
                                .disabled(title.isEmpty && content.isEmpty)
                                
                                Spacer()
                                
                                // switch page
                                NavigationLink(destination: EmotionSelectionView(
                                    selectedEmotion: $selectedEmotion,
                                    title: $title,
                                    content: $content,
                                    topics: $topics
                                ))
//                                Button(action: {
//                                    generateCov()
//                                })
                                {
                                    Text("Generate Cover")
                                        .font(.headline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                        .frame(width: 200)
                                        .padding(.vertical, 16.0)
                                        .background(buttonBackground)
                                        .cornerRadius(12)
                                }
                                .disabled(title.isEmpty || content.isEmpty)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 24)
                    .onChange(of: content) { newValue in
                        updateTagSuggestionState(with: newValue)
                    }
                    
                    Spacer()
                }
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle("")
            .navigationBarHidden(true)
            // emotion select shown
//            .sheet(isPresented: $showingEmotionSelection) {
//                EmotionSelectionView(
//                    selectedEmotion: $selectedEmotion,
//                    title: $title,
//                    content: $content,
//                    topics: $topics)
//            }
            // tag input
            .sheet(isPresented: $showingTagInput) {
                TagInputView(
                    newTag: $newTag,
                    topics: $topics,
                    maxTags: maxTags,
                    maxTagLength: maxTagLength,
                    isPresented: $showingTagInput
                )
            }
            
        }
    }
    
    // MARK: - generateCov func
    private func generateCov() {
        showingEmotionSelection = true
    }
    
    // MARK: - saveDraft Button func
    private func saveDraft() {
        
    }
    
    // MARK: - Computed Properties
    private var buttonBackground: some View {
        if title.isEmpty || content.isEmpty {
            AnyView(Color.gray.opacity(0.6))
        } else {
            AnyView(LinearGradient(
                gradient: Gradient(colors: [
                    DesignConstants.ColorPalette.brandPurple.opacity(0.55),
                    DesignConstants.ColorPalette.brandPurple.opacity(0.35)
                ]),
                startPoint: .top,
                endPoint: .bottom
            ))
        }
    }
    
    private var buttonTitle: String {
        return "Generate Cover"
    }

    // MARK: - Text Height Estimation
    private func estimatedContentHeight(for text: String, inWidth width: CGFloat) -> CGFloat {
        let font = UIFont.preferredFont(forTextStyle: .body)
        let horizontalPadding: CGFloat = 32 // 16 leading + 16 trailing
        let verticalPadding: CGFloat = 24   // 12 top + 12 bottom
        let constrainedWidth = max(width - horizontalPadding, 0)
        let boundingSize = CGSize(width: constrainedWidth, height: .greatestFiniteMagnitude)
        let rect = (text as NSString).boundingRect(
            with: boundingSize,
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: [.font: font],
            context: nil
        )
        let oneLine = font.lineHeight
        let contentHeight = max(rect.height, oneLine)
        return contentHeight + verticalPadding
    }

    private func estimatedSingleLineHeight(inWidth width: CGFloat) -> CGFloat {
        return estimatedContentHeight(for: "A", inWidth: width)
    }

    // MARK: - Tag Suggestions Logic
    private var suggestedTags: [String] {
        let lower = tagQuery.lowercased()
        if lower.isEmpty { return trendingTags }
        return trendingTags.filter { $0.lowercased().contains(lower) }
    }
    private func updateTagSuggestionState(with text: String) {
        // Find the last word segment to detect '#'
        let components = text.split(whereSeparator: { $0.isWhitespace || $0.isNewline })
        guard let last = components.last else {
            showTagSuggestions = false
            tagQuery = ""
            return
        }
        let segment = String(last)
        if segment.hasPrefix("#") {
            let query = String(segment.dropFirst())
            tagQuery = query
            showTagSuggestions = true
        } else {
            showTagSuggestions = false
            tagQuery = ""
        }
    }
    private func insertSuggestedTag(_ tag: String) {
        // Replace the last '#query' segment with '#tag '
        var tokens = content.split(whereSeparator: { $0.isWhitespace || $0.isNewline }).map(String.init)
        guard !tokens.isEmpty else {
            content = "#\(tag) "
            showTagSuggestions = false
            return
        }
        if let last = tokens.last, last.hasPrefix("#") {
            tokens.removeLast()
            tokens.append("#\(tag)")
        } else {
            tokens.append("#\(tag)")
        }
        // Rebuild preserving simple spaces; this may collapse multiple spaces
        content = tokens.joined(separator: " ") + " "
        showTagSuggestions = false
    }
}

// MARK: - Tag Input View
struct TagInputView: View {
    @Binding var newTag: String
    @Binding var topics: [String]
    let maxTags: Int
    let maxTagLength: Int
    @Binding var isPresented: Bool
    @FocusState private var isTextFieldFocused: Bool
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Add Tag")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Tags help others find your post. Add keywords that describe your content.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    TextField("Enter tag (max \(maxTagLength) characters)", text: $newTag)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .focused($isTextFieldFocused)
                        .onChange(of: newTag) { value in
                            if value.count > maxTagLength {
                                newTag = String(value.prefix(maxTagLength))
                            }
                        }
                    
                    HStack {
                        Text("\(newTag.count)/\(maxTagLength) characters")
                            .font(.caption2)
                            .foregroundColor(.gray)
                        
                        Spacer()
                        
                        Text("\(topics.count)/\(maxTags) tags")
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                
                Spacer()
                
                HStack(spacing: 16) {
                    Button("Cancel") {
                        newTag = ""
                        isPresented = false
                    }
                    .buttonStyle(.bordered)
                    
                    Button("Add Tag") {
                        addTag()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.purple)
                    .disabled(newTag.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || topics.count >= maxTags)
                }
                .padding()
            }
            .navigationTitle("Add Tag")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                }
            }
        }
        .onAppear {
            isTextFieldFocused = true
        }
    }
    
    private func addTag() {
        let trimmed = newTag.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty && topics.count < maxTags {
            if !topics.contains(trimmed) {
                topics.append(trimmed)
                newTag = ""
            }
        }
    }
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .short
    formatter.timeStyle = .short
    return formatter
}()

// MARK: - Preview
#Preview {
    DraftsView()
        .environment(\.managedObjectContext, PersistenceController.shared.container.viewContext)
        .environmentObject(FeedViewModel())
}

