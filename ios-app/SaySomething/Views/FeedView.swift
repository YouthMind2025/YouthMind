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
                    }
                }
                // Right column
                LazyVStack(spacing: 12) {
                    ForEach(rightColumn) { post in
                        NavigationLink(value: post) {
                            PostPreview(post: post, viewModel: viewModel)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 12)
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
            await viewModel.fetchPosts()
        }
    }
}

