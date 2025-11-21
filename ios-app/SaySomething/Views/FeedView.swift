// Views/FeedView
// Primary view for displaying a scrollable feed of posts in a two-column layout
// Handles navigation to post details and UI interactions

import SwiftUI


struct FeedView: View {
    @EnvironmentObject var viewModel: FeedViewModel
    @EnvironmentObject private var tabBarState: TabBarState
    @State private var selectedPost: Post? = nil
    
    // Simple even/odd distribution for waterfall effect
    private var leftColumn: [Post] {
        viewModel.posts.enumerated().compactMap { index, post in index % 2 == 0 ? post : nil }
    }
    private var rightColumn: [Post] {
        viewModel.posts.enumerated().compactMap { index, post in index % 2 != 0 ? post : nil }
    }
    
    var body: some View {
        ScrollView {
            HStack(alignment: .top, spacing: 12) {
                // Left column
                LazyVStack(spacing: 12) {
                    ForEach(leftColumn) { post in
                        NavigationLink(value: post) {
                            PostPreview(post: post, viewModel: viewModel)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .onAppear {
                            // Load more when reaching near-end of left column
                            if post == leftColumn.last && viewModel.hasMoreBlocks {
                                Task {
                                    await viewModel.loadNextBlock()
                                }
                            }
                        }
                    }
                }
                // Right column
                LazyVStack(spacing: 12) {
                    ForEach(rightColumn) { post in
                        NavigationLink(value: post) {
                            PostPreview(post: post, viewModel: viewModel)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .onAppear {
                            // Load more when reaching near-end of right column
                            if post == rightColumn.last && viewModel.hasMoreBlocks {
                                Task {
                                    await viewModel.loadNextBlock()
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 12)
            
            // Loading indicator at bottom
            if viewModel.isLoadingMore {
                VStack(spacing: 8) {
                    ProgressView()
                        .scaleEffect(0.8)
                    Text("Loading more posts...")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            }
            
            // Error retry button
            if let error = viewModel.error, !viewModel.isLoadingMore {
                RetryButton(
                    error: error,
                    action: {
                        Task {
                            await viewModel.loadNextBlock()
                        }
                    },
                    title: "Failed to load more posts",
                    message: "Please check your connection and try again."
                )
            }
        }
        .navigationTitle("Discovery")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: {}) {
                    Image(systemName: "gearshape.fill")
                        .foregroundStyle(DesignConstants.ColorPalette.brandPurple)
                }
            }

            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    SearchView()
                        .environmentObject(viewModel)
                        .environmentObject(tabBarState)
                } label: {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(DesignConstants.ColorPalette.brandPurple)
                }
            }
        }
        .refreshable {
            await viewModel.refreshFeed()
        }
        .onAppear {
            // Trigger initial scroll notification when view appears
            Task {
                await viewModel.notifyScroll()
            }
        }
    }
}

