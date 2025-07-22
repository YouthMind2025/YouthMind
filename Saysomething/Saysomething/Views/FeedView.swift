// Views/FeedView
// Primary view for displaying a scrollable feed of posts in a two-column layout
// Handles navigation to post details and UI interactions

import SwiftUI


struct FeedView: View {
    @StateObject var viewModel = FeedViewModel()
    @State private var selectedPost: Post? = nil
    
    var body: some View {
        NavigationStack {
            ScrollView {
                HStack(alignment: .top) {
                    LazyVStack(spacing: 15) {
                        ForEach(viewModel.leftColumnPosts) { post in
                            NavigationLink(value: post) {
                                PostPreview(post: post, viewModel: viewModel)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    
                    LazyVStack(spacing: 15) {
                        ForEach(viewModel.rightColumnPosts) { post in
                            NavigationLink(value: post) {
                                PostPreview(post: post, viewModel: viewModel)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    Spacer()
                }
            }
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
            }
            .navigationTitle("Discovery")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {}) {
                        Image(systemName: "gearshape.fill")
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {}) {
                        Image(systemName: "magnifyingglass")
                    }
                }
            }
            .refreshable {
                await viewModel.fetchPosts()
            }
        }
    }
}

