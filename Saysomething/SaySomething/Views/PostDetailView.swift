// Views/PostDetailView.swift
// Displays the full details of a post when selected from the feed
// Shows complete content, engagement options, and post metadata

import SwiftUI

struct PostDetailView: View {
    let post: Post
    @State private var isResonanced: Bool
    @State private var isCollected: Bool
    
    init(post: Post) {
        self.post = post
        self._isResonanced = State(initialValue: post.isresonanced)
        self._isCollected = State(initialValue: post.isCollected)
    }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Post Image
                Image(post.preImg)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(maxWidth: .infinity)
                    .frame(height: 300)
                    .clipped()
                
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
                                isResonanced.toggle()
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: isResonanced ? "heart.fill" : "heart")
                                        .foregroundColor(isResonanced ? .red : .gray)
                                    Text("\(post.resonance + (isResonanced ? 1 : 0))")
                                        .foregroundColor(.secondary)
                                }
                                .font(.body)
                            }
                            
                            // Collect Button
                            Button(action: {
                                isCollected.toggle()
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: isCollected ? "bookmark.fill" : "bookmark")
                                        .foregroundColor(isCollected ? .blue : .gray)
                                    Text("\(post.collect + (isCollected ? 1 : 0))")
                                        .foregroundColor(.secondary)
                                }
                                .font(.body)
                            }
                            
                            // Share Button
                            Button(action: {
                                // Share functionality
                            }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "square.and.arrow.up")
                                        .foregroundColor(.gray)
                                    Text("Share")
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
                                Text("\(post.view) views")
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
                            Text(post.date, style: .relative)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.top, 8)
                }
                .padding(.horizontal, 16)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    // More options
                }) {
                    Image(systemName: "ellipsis")
                }
            }
        }
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
    }
}

