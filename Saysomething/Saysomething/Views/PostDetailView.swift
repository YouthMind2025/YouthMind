// Views/PostDetailView.swift
// Displays full content of a single post with interactive engagement controls
// Handles post collection state and displays all post metadata

import SwiftUI

struct PostDetailView: View {
    @State var post: Post
    @EnvironmentObject var viewModel: FeedViewModel
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Post title and content
                Text(post.title)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.horizontal)
                
                Text(post.content)
                    .padding(.horizontal)
                
                // Emotion indicator
                HStack {
                    Text(post.emotion.rawValue.capitalized)
                    Circle()
                        .fill(post.emotion.color)
                        .frame(width: 20, height: 20)
                }
                .padding(.horizontal)
                
                // Stats and actions
                HStack(spacing: 20) {
                    // Resonance
                    Label("\(post.resonance)", systemImage: post.isresonanced ? "heart.fill" : "heart")
                        .foregroundColor(post.isresonanced ? .red : .primary)
                    
                    // Collect button with count
                    Button(action: {
                        // add collect function here
                        post.isCollected.toggle()
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: post.isCollected ? "bookmark.fill" : "bookmark")
                                .foregroundColor(post.isCollected ? .blue : .primary)
                            Text("\(post.collect)")
                        }
                    }
                }
                .padding(.horizontal)
                
                // Date
                Text(post.date, style: .date)
                    .font(.caption)
                    .foregroundColor(.gray)
                    .padding(.horizontal)
                
                Spacer()
            }
            .padding(.top)
        }
        .navigationTitle("Post Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    print("Share button tapped")
                }) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
    }
}

