// Views/PostDetailView.swift
// Displays the full details of a post when selected from the feed
// Shows complete content, engagement options, and post metadata

import SwiftUI
import UIKit

// Shared helper for stable relative timestamps (scoped to this file)
fileprivate func relativeTimeString(from date: Date, to now: Date) -> String {
    let seconds = Int(now.timeIntervalSince(date))
    if seconds < 60 { return "Just now" }
    let minutes = seconds / 60
    if minutes < 60 { return "\(minutes)m" }
    let hours = minutes / 60
    if hours < 24 { return "\(hours)h" }
    let days = hours / 24
    if days < 7 { return "\(days)d" }
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    formatter.timeStyle = .short
    return formatter.string(from: date)
}

// MARK: - Floating hearts overlay (inline for easy integration)
fileprivate struct FloatingHeartsOverlay: View {
    let trigger: Int

    @State private var hearts: [Heart] = []

    var body: some View {
        ZStack {
            ForEach(hearts) { heart in
                SingleHeartView(heart: heart) { finishedId in
                    hearts.removeAll { $0.id == finishedId }
                }
                .transition(.opacity)
            }
        }
        .allowsHitTesting(false)
        .onChange(of: trigger) { _ in
            emitBurst()
        }
    }

    private func emitBurst(count: Int = 8) {
        let newHearts: [Heart] = (0..<count).map { index in
            let size: CGFloat = .random(in: 16...24)
            let travel: CGFloat = .random(in: 140...220)
            let sideways: CGFloat = [
                .random(in: (-40)...(-10)),
                .random(in: 10...40)
            ].randomElement() ?? 20
            let duration: Double = .random(in: 1.0...1.8)
            let delay: Double = Double(index) * .random(in: 0.02...0.06)
            let rotation: Double = .random(in: (-18)...18)
            return Heart(size: size,
                         travelDistance: travel,
                         sidewaysOffset: sideways,
                         duration: duration,
                         delay: delay,
                         rotation: rotation)
        }
        hearts.append(contentsOf: newHearts)
    }

    private struct Heart: Identifiable, Equatable {
        let id = UUID()
        let size: CGFloat
        let travelDistance: CGFloat
        let sidewaysOffset: CGFloat
        let duration: Double
        let delay: Double
        let rotation: Double
    }

    private struct SingleHeartView: View {
        let heart: Heart
        var onComplete: (UUID) -> Void

        @State private var animate: Bool = false

        private var gradient: LinearGradient {
            LinearGradient(
                gradient: Gradient(colors: [
                    DesignConstants.ColorPalette.brandPurple,
                    .pink,
                    .red
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
        }

        var body: some View {
            Image(systemName: "heart.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: heart.size, height: heart.size)
                .foregroundStyle(gradient)
                .shadow(color: .black.opacity(0.08), radius: 2, x: 0, y: 1)
                .scaleEffect(animate ? 1.0 : 0.6)
                .rotationEffect(.degrees(animate ? heart.rotation : 0))
                .opacity(animate ? 0.0 : 1.0)
                .offset(x: animate ? heart.sidewaysOffset : 0,
                        y: animate ? -heart.travelDistance : 0)
                .onAppear {
                    withAnimation(.easeOut(duration: heart.duration).delay(heart.delay)) {
                        animate = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + heart.duration + heart.delay) {
                        onComplete(heart.id)
                    }
                }
        }
    }
}

// MARK: - Floating balloon overlay (for post like)
fileprivate struct FloatingBalloonOverlay: View {
    let trigger: Int

    @State private var balloons: [Balloon] = []

    var body: some View {
        ZStack {
            ForEach(balloons) { balloon in
                SingleBalloonView(balloon: balloon) { id in
                    balloons.removeAll { $0.id == id }
                }
            }
        }
        .allowsHitTesting(false)
        .onChange(of: trigger) { _ in
            emitBalloon()
        }
    }

    private func emitBalloon() {
        let travel: CGFloat = .random(in: 140...220)
        let sway: CGFloat = .random(in: (-24)...24)
        let duration: Double = .random(in: 1.2...2.0)
        let delay: Double = 0
        let size: CGFloat = .random(in: 18...26)
        balloons.append(Balloon(size: size, travel: travel, sway: sway, duration: duration, delay: delay))
    }

    private struct Balloon: Identifiable, Equatable {
        let id = UUID()
        let size: CGFloat
        let travel: CGFloat
        let sway: CGFloat
        let duration: Double
        let delay: Double
    }

    private struct SingleBalloonView: View {
        let balloon: Balloon
        var onComplete: (UUID) -> Void

        @State private var animate: Bool = false
        @State private var phase: CGFloat = 0

        private var strokeColor: Color { DesignConstants.ColorPalette.brandPurple }

        var body: some View {
            ZStack(alignment: .top) {
                // Balloon outline (line art)
                Circle()
                    .stroke(strokeColor, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                    .frame(width: balloon.size * 1.6, height: balloon.size * 1.6)
                    .overlay(
                        // simple highlight arc like reference
                        Circle()
                            .trim(from: 0.78, to: 0.92)
                            .stroke(strokeColor.opacity(0.8), style: StrokeStyle(lineWidth: 2, lineCap: .round))
                            .rotationEffect(.degrees(28))
                            .offset(x: balloon.size * 0.08, y: balloon.size * 0.02)
                    )

                // Knot (stroked)
                Triangle()
                    .stroke(strokeColor, lineWidth: 2)
                    .frame(width: balloon.size * 0.42, height: balloon.size * 0.34)
                    .offset(y: balloon.size * 1.45)

                // String
                WavyString(phase: phase)
                    .stroke(strokeColor, lineWidth: 2)
                    .frame(width: balloon.size * 0.7, height: balloon.size * 3.2)
                    .offset(y: balloon.size * 2.0)
            }
            .shadow(color: strokeColor.opacity(0.18), radius: 2, x: 0, y: 1)
            .opacity(animate ? 0.0 : 1.0)
            .offset(x: animate ? balloon.sway : 0, y: animate ? -balloon.travel : 0)
            .scaleEffect(animate ? 1.0 : 0.85)
            .onAppear {
                withAnimation(.easeOut(duration: balloon.duration).delay(balloon.delay)) {
                    animate = true
                }
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: true)) {
                    phase = 1
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + balloon.duration + balloon.delay) {
                    onComplete(balloon.id)
                }
            }
        }
    }
}

// Small triangle shape used as balloon knot
fileprivate struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        p.closeSubpath()
        return p
    }
}

// Wavy string path with animated phase
fileprivate struct WavyString: Shape {
    var phase: CGFloat
    var amplitude: CGFloat = 4
    var frequency: CGFloat = 2

    var animatableData: CGFloat {
        get { phase }
        set { phase = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let steps = 40
        let stepX = rect.width / CGFloat(steps)
        var x: CGFloat = rect.minX
        var y: CGFloat = rect.minY
        path.move(to: CGPoint(x: x, y: y))
        for i in 0...steps {
            x = rect.minX + CGFloat(i) * stepX
            let t = CGFloat(i) / CGFloat(steps)
            y = rect.minY + t * rect.height + sin((t + phase) * .pi * frequency) * amplitude
            path.addLine(to: CGPoint(x: x, y: y))
        }
        return path
    }
}

// MARK: - Simple float up modifier for image overlay
fileprivate struct FloatUp: ViewModifier {
    @State private var y: CGFloat = 30
    @State private var opacity: Double = 0.0
    @State private var scale: CGFloat = 0.6

    func body(content: Content) -> some View {
        content
            .offset(y: y)
            .opacity(opacity)
            .scaleEffect(scale)
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                    scale = 1.0
                }
                withAnimation(.easeOut(duration: 1.0)) {
                    y = -160
                    opacity = 1.0
                }
                withAnimation(.easeIn(duration: 0.25).delay(0.9)) {
                    opacity = 0.0
                }
            }
    }
}

struct PostDetailView: View {
    let post: Post
    @State private var showBalloon: Bool = false
    @State private var commentText: String = ""
    @FocusState private var isCommentFocused: Bool
    @EnvironmentObject private var feedVM: FeedViewModel
    @State private var viewOpenedAt: Date = Date()
    @Environment(\.dismiss) private var dismiss
    
    private var currentViews: Int {
        feedVM.engagement[post.id]?.views ?? post.view
    }
    private var currentResonance: Int {
        feedVM.engagement[post.id]?.resonance ?? post.resonance
    }
    private var isResonanced: Bool {
        feedVM.engagement[post.id]?.isResonanced ?? post.isresonanced
    }
    private var isCollected: Bool {
        feedVM.engagement[post.id]?.isCollected ?? post.isCollected
    }
    private var currentSaves: Int {
        feedVM.engagement[post.id]?.saves ?? post.collect
    }
    
    init(post: Post) {
        self.post = post
    }
    
    var body: some View {
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Post Cover Frame (uses deeper emotion color); shows image if available
                CoverPlaceholder(emotion: post.emotion, imageName: post.preImg)
                    .contentShape(Rectangle())
                
                VStack(alignment: .leading, spacing: 16) {
                    // Title
                    Text(post.title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    // Content
                    Text(post.content)
                        .font(.body)
                        .foregroundColor(.primary)
                        .lineSpacing(4)
                    
                    // Engagement Section
                    VStack(spacing: 12) {
                        // Action Buttons
                        HStack(spacing: 20) {
                            // Resonance Button
                            Button(action: {
                                Task {
                                    await feedVM.toggleResonance(for: post.id)
                                }
                                if !isResonanced { fireBalloon() }
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: isResonanced ? "heart.fill" : "heart")
                                        .foregroundColor(isResonanced ? .red : .gray)
                                        .overlay(alignment: .center) {
                                            if showBalloon {
                                                Image("balloon")
                                                    .resizable()
                                                    .scaledToFit()
                                                    .frame(width: 90)
                                                    .transition(.scale.combined(with: .opacity))
                                                    .offset(y: -100)
                                                    .modifier(FloatUp())
                                                    .allowsHitTesting(false)
                                            }
                                        }
                                    Text("\(currentResonance)")
                                        .foregroundColor(.secondary)
                                }
                                .font(.body)
                            }
                            
                            // Collect Button
                            Button(action: {
                                Task {
                                    await feedVM.toggleCollection(for: post.id)
                                }
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: isCollected ? "bookmark.fill" : "bookmark")
                                        .foregroundColor(isCollected ? DesignConstants.ColorPalette.brandPurple : .gray)
                                    Text("\(currentSaves)")
                                        .foregroundColor(.secondary)
                                }
                                .font(.body)
                            }
                            
                            Spacer()
                        }
                        
                        // Stats Row
                        HStack(spacing: 16) {
                            HStack(spacing: 4) {
                                Image(systemName: "eye")
                                    .foregroundColor(.gray)
                                Text("\(currentViews) views")
                                    .foregroundColor(.secondary)
                            }
                            .font(.caption)
                            
                            // Emotion indicator
                            HStack(spacing: 4) {
                                Circle()
                                    .fill(post.emotion.color)
                                    .frame(width: 12, height: 12)
                                Text(post.emotion.rawValue.capitalized)
                                    .foregroundColor(.secondary)
                            }
                            .font(.caption)
                            
                            Spacer()
                            
                            // Date
                            Text(relativeTimeString(from: post.date, to: viewOpenedAt))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.top, 8)
                }
                .padding(.horizontal, 16)

                // Comments Section
                CommentsSection(
                    post: post,
                    proxy: proxy,
                    commentText: $commentText,
                    isCommentFocused: $isCommentFocused,
                    viewOpenedAt: viewOpenedAt
                )
            }
            .contentShape(Rectangle())
            .highPriorityGesture(
                TapGesture(count: 2).onEnded {
                    Task {
                        await feedVM.toggleResonance(for: post.id)
                    }
                    if !isResonanced { fireBalloon() }
                }
            )
        }
        .scrollDismissesKeyboard(.interactively)
        .refreshable {
            await feedVM.refreshComments(for: post.id)
            viewOpenedAt = Date()
        }
        .safeAreaInset(edge: .bottom) {
            CommentInputBar(text: $commentText, onSend: {
                addComment()
            })
            .background(.ultraThinMaterial)
        }
        }
        .onAppear { feedVM.incrementView(for: post.id) }
        .background(post.emotion.color.opacity(0.12).ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .tint(DesignConstants.ColorPalette.brandPurple)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    // Action for top-right button
                }) {
                    Image(systemName: "arrowshape.turn.up.right")
                        .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                }
            }
        }
    }
}

// MARK: - Cover Placeholder
private enum CoverAspect: String, Codable {
    case threeByFour
    case fourByThree
}

private struct CoverPlaceholder: View {
    let emotion: Emotion
    let imageName: String
    let coverAspect: CoverAspect = .threeByFour

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            emotion.color.opacity(0.35),
                            emotion.color.opacity(0.22)
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            if let uiImage = UIImage(named: imageName), !imageName.isEmpty {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 240)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(emotion.color.opacity(0.45), lineWidth: 1)
        )
        .clipped()
    }
}

// MARK: - Comments Section
private struct CommentsSection: View {
    let post: Post
    let proxy: ScrollViewProxy
    @EnvironmentObject var feedVM: FeedViewModel
    @Binding var commentText: String
    var isCommentFocused: FocusState<Bool>.Binding
    let viewOpenedAt: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Comments")
                .font(.headline)
                .padding(.horizontal, 16)

            if let comments = sortedComments, !comments.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(comments) { comment in
                        CommentRow(postId: post.id, comment: comment, emotion: post.emotion, viewOpenedAt: viewOpenedAt)
                            .padding(.horizontal, 16)
                    }
                }
                .id("comments_top")
            } else {
                Text("Be the first to comment")
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
            }
        }
        .padding(.top, 8)
    }

    private var sortedComments: [Comment]? {
        guard let arr = feedVM.comments[post.id] else { return nil }
        return arr.sorted { lhs, rhs in
            if lhs.likes == rhs.likes { return lhs.createdAt < rhs.createdAt }
            return lhs.likes > rhs.likes
        }
    }
}

private struct CommentRow: View {
    let postId: UUID
    @EnvironmentObject var feedVM: FeedViewModel
    @State private var isExpanded: Bool = false
    @State private var localComment: Comment
    @State private var heartsTrigger: Int = 0
    @State private var bigHeartsTrigger: Int = 0
    let emotion: Emotion
    let viewOpenedAt: Date

    init(postId: UUID, comment: Comment, emotion: Emotion, viewOpenedAt: Date) {
        self.postId = postId
        _localComment = State(initialValue: comment)
        self.emotion = emotion
        self.viewOpenedAt = viewOpenedAt
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(localComment.content)
                .font(.body)
                .foregroundColor(.primary)

            

            // Replies
            if !displayedReplies.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(displayedReplies) { reply in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(reply.content)
                                .font(.callout)
                                .foregroundColor(.primary)

                            HStack(spacing: 12) {
                                Button(action: { toggleReplyLike(reply.id) }) {
                                    HStack(spacing: 6) {
                                        Image(systemName: reply.isLiked ? "heart.fill" : "heart")
                                            .foregroundColor(reply.isLiked ? .red : .secondary)
                                        Text("\(reply.likes)")
                                    }
                                }
                                .buttonStyle(.plain)

                                Spacer()

                                Text(relativeTimeString(from: reply.createdAt, to: viewOpenedAt))
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        }
                        .padding(8)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    emotion.color.opacity(0.14),
                                    emotion.color.opacity(0.08)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(emotion.color.opacity(0.22), lineWidth: 0.5)
                        )
                        .padding(.leading, 8)
                        .contentShape(Rectangle())
                        .onTapGesture { likeReplyIfNeeded(reply.id) }
                    }
                }
                .padding(.top, 6)
            }

            HStack(spacing: 16) {
                Button(action: toggleLike) {
                    HStack(spacing: 6) {
                        Image(systemName: localComment.isLiked ? "heart.fill" : "heart")
                            .foregroundColor(localComment.isLiked ? .red : .secondary)
                            .overlay(FloatingHeartsOverlay(trigger: heartsTrigger))
                        Text("\(localComment.likes)")
                    }
                }
                .buttonStyle(.plain)

                if !localComment.replies.isEmpty {
                    Button(action: { withAnimation(.easeInOut) { isExpanded.toggle() } }) {
                        HStack(spacing: 6) {
                            Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            Text(isExpanded ? "Hide replies" : "View \(localComment.replies.count) repl\(localComment.replies.count == 1 ? "y" : "ies")")
                        }
                    }
                    .buttonStyle(.plain)
                }

                Spacer()

                Text(relativeTimeString(from: localComment.createdAt, to: viewOpenedAt))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .font(.caption)
            .foregroundColor(.secondary)
        }
        .padding(8)
        .background(
            LinearGradient(
                gradient: Gradient(colors: [
                    emotion.color.opacity(0.16),
                    emotion.color.opacity(0.10)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(emotion.color.opacity(0.24), lineWidth: 0.5)
        )
        .overlay(FloatingHeartsOverlay(trigger: bigHeartsTrigger))
        .contentShape(Rectangle())
        .onTapGesture {
            if likeCommentIfNeeded() { bigHeartsTrigger += 1 }
        }
    }

    private var sortedReplies: [Comment] {
        localComment.replies.sorted { lhs, rhs in
            if lhs.likes == rhs.likes { return lhs.createdAt < rhs.createdAt }
            return lhs.likes > rhs.likes
        }
    }

    private var displayedReplies: [Comment] {
        guard isExpanded else { return [] }
        let sorted = sortedReplies
        if localComment.isLiked {
            return Array(sorted.prefix(9))
        }
        return sorted
    }

    private func toggleLike() {
        localComment.isLiked.toggle()
        localComment.likes += localComment.isLiked ? 1 : -1
        if localComment.isLiked {
            isExpanded = true
            heartsTrigger += 1
        }
        // update in store
        if var list = feedVM.comments[postId], let idx = list.firstIndex(where: { $0.id == localComment.id }) {
            list[idx] = localComment
            feedVM.comments[postId] = list
        }
    }

    private func toggleReplyLike(_ replyId: UUID) {
        guard var list = feedVM.comments[postId],
              let commentIndex = list.firstIndex(where: { $0.id == localComment.id }) else { return }

        var comment = list[commentIndex]
        if let replyIndex = comment.replies.firstIndex(where: { $0.id == replyId }) {
            comment.replies[replyIndex].isLiked.toggle()
            comment.replies[replyIndex].likes += comment.replies[replyIndex].isLiked ? 1 : -1
            list[commentIndex] = comment
            feedVM.comments[postId] = list
            localComment = comment
        }
    }

    private func likeCommentIfNeeded() -> Bool {
        guard !localComment.isLiked else { return false }
        localComment.isLiked = true
        localComment.likes += 1
        isExpanded = true
        if var list = feedVM.comments[postId], let idx = list.firstIndex(where: { $0.id == localComment.id }) {
            list[idx] = localComment
            feedVM.comments[postId] = list
        }
        return true
    }

    private func likeReplyIfNeeded(_ replyId: UUID) {
        guard var list = feedVM.comments[postId],
              let commentIndex = list.firstIndex(where: { $0.id == localComment.id }) else { return }
        var comment = list[commentIndex]
        if let replyIndex = comment.replies.firstIndex(where: { $0.id == replyId }) {
            if !comment.replies[replyIndex].isLiked {
                comment.replies[replyIndex].isLiked = true
                comment.replies[replyIndex].likes += 1
                list[commentIndex] = comment
                feedVM.comments[postId] = list
                localComment = comment
            }
        }
    }
}

// MARK: - Input Bar
private struct CommentInputBar: View {
    @Binding var text: String
    var onSend: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            TextField("Add a comment...", text: $text, axis: .vertical)
                .textFieldStyle(PlainTextFieldStyle())
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            Button(action: onSend) {
                Image(systemName: "paperplane.fill")
                    .foregroundColor(.white)
                    .padding(10)
                    .background(DesignConstants.ColorPalette.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .opacity(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.6 : 1.0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

// MARK: - Helpers
private extension PostDetailView {
    func addComment() {
        guard !commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let new = Comment(author: "You", content: commentText)
        if var arr = feedVM.comments[post.id] {
            arr.insert(new, at: 0)
            feedVM.comments[post.id] = arr
        } else {
            feedVM.comments[post.id] = [new]
        }
        commentText = ""
    }
    func fireBalloon() {
        UIImpactFeedbackGenerator(style: .soft).impactOccurred()
        showBalloon = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            withAnimation(.easeInOut(duration: 0.25)) {
                showBalloon = false
            }
        }
    }
    func relativeTimeString(from date: Date, to now: Date) -> String {
        let seconds = Int(now.timeIntervalSince(date))
        if seconds < 60 { return "Just now" }
        let minutes = seconds / 60
        if minutes < 60 { return "\(minutes)m" }
        let hours = minutes / 60
        if hours < 24 { return "\(hours)h" }
        let days = hours / 24
        if days < 7 { return "\(days)d" }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

#Preview {
    NavigationStack {
        PostDetailView(
            post: Post(
                resonance: 45,
                view: 200,
                collect: 32,
                title: "Summer outfit ideas",
                content: "Just discovered the perfect summer dress that's both stylish and comfortable for hot days. The lightweight fabric and vibrant colors make me feel so happy every time I wear it! The way it flows and moves with me makes every day feel like a special occasion.",
                preImg: "happy_outfit",
                isresonanced: false,
                isCollected: false,
                emotion: .happy,
                date: Date().addingTimeInterval(-86400)
            )
        )
        .environmentObject(FeedViewModel())
    }
}

