// Views/PostPreview.swift
//Compact preview card for posts in the feed
//Visually distinguishes posts by emotion type
//Displays key engagement metrics


import SwiftUI

struct PostPreview: View {
    let post: Post
    @ObservedObject var viewModel: FeedViewModel
    @State private var likeBounce: Bool = false
    
    // Engagement is now separate from the post content
    private var currentEngagement: PostEngagement? {
        viewModel.engagement[post.id]
    }
    private var isCurrentlyLiked: Bool {
        viewModel.engagement[post.id]?.isResonanced ?? post.isresonanced
    }

    // Helper property to get emotion color from DesignConstants
    private var emotionColor: Color {
        switch post.emotion {
        case .angry: return DesignConstants.ColorPalette.angry
        case .sad: return DesignConstants.ColorPalette.sad
        case .happy: return DesignConstants.ColorPalette.happy
        case .conflicted: return DesignConstants.ColorPalette.conflicted
        case .surprised: return DesignConstants.ColorPalette.surprised
        case .fear: return DesignConstants.ColorPalette.fear
        }
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Emotion-tinted gradient cover background
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            emotionColor.opacity(0.26),
                            emotionColor.opacity(0.10)
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.black.opacity(0.06), lineWidth: 1)
                )
            VStack(spacing: 8) {
                if !post.preImg.isEmpty {
                    Image(post.preImg)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(8)
                }

                Text(post.title)
                    .font(.system(.title3, weight: .semibold))
                    .foregroundColor(DesignConstants.ColorPalette.textPrimary)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 10)
                    .padding(.bottom, 32)
                    .lineLimit(2)

                Spacer(minLength: 0)
            }
        }
        .contentShape(Rectangle())
        .highPriorityGesture(
            TapGesture(count: 2).onEnded {
                Task {
                    await viewModel.toggleResonance(for: post.id)
                }
                withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                    likeBounce = true
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) { likeBounce = false }
                }
            }
        )
        // Heart button at bottom-left overlay
        .overlay(alignment: .bottomLeading) {
            Button {
                Task {
                    await viewModel.toggleResonance(for: post.id)
                }
                withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                    likeBounce = true
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) { likeBounce = false }
                }
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: isCurrentlyLiked ? "heart.fill" : "heart")
                        .foregroundColor(isCurrentlyLiked ? .red : .primary)
                        .imageScale(.small)
                        .scaleEffect(likeBounce ? 1.25 : 1.0)
                    Text("\(currentEngagement?.resonance ?? post.resonance)")
                        .font(.system(size: 12))
                        .minimumScaleFactor(0.7)
                }
                .padding(.vertical, 4)
                .padding(.leading, 6)
                .padding(.bottom, 8)
            }
            .buttonStyle(.plain)
        }
        // Views badge at bottom-right overlay
        .overlay(alignment: .bottomTrailing) {
            HStack(spacing: 6) {
                Image(systemName: "eye")
                    .imageScale(.small)
                Text("\(currentEngagement?.views ?? post.view)")
                    .font(.system(size: 12))
            }
            .padding(.vertical, 4)
            .padding(.trailing, 6)
            .padding(.bottom, 8)
        }
        // Let intrinsic height vary for masonry layout
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.06), radius: 6, x: 0, y: 3)
        .shadow(color: emotionColor.opacity(0.10), radius: 10, x: 0, y: 6)
    }
}

