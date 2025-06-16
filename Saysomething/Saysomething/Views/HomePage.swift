//Views/HomePage.swift
//Implement basic structure of the HomePage
// Including topBar, feed and bottomtap UI.


import SwiftUI

struct HomePage: View {
    @StateObject var viewModel = FeedViewModel()
    
    var body: some View {
        ZStack {
            // Loading State
            if viewModel.isLoading {
                ProgressView()
                    .scaleEffect(2)
            }
            // Error State
            else if let error = viewModel.error {
                ErrorView(error: error, retryAction: {
                    Task { await viewModel.fetchPosts() }
                })
            }
            // Empty State
            else if viewModel.posts.isEmpty {
                EmptyStateView()
            }
            // Content State
            else {
                NavigationStack{
                    ScrollView {
                        // Your existing post grid
                        HStack(alignment: .top) {
                            LazyVStack(spacing: 15){
                                ForEach(viewModel.leftColumnPosts) { post in
                                    PostPreview(post: post, viewModel: viewModel)
                                }
                            }//end of LazyVStack1 in HStack
                            
                            LazyVStack(spacing: 15){
                                ForEach(viewModel.rightColumnPosts) { post in
                                    PostPreview(post: post, viewModel: viewModel)
                                }
                            }//end of LazyVStack2 in HStack
                            Spacer()
                        }
                    }
                    .navigationTitle("Discovery")  // 2. Directly on ScrollView
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        // 3. Toolbar items
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
        .task {
            await viewModel.fetchPosts()
        }
    }
}


// Displays error message with retry button
struct ErrorView: View {
    let error: Error
    let retryAction: () -> Void
    
    var body: some View {
        VStack {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
            Text("Failed to load posts")
            Button("Retry", action: retryAction)
        }
    }
}

// Shows empty state placeholder
struct EmptyStateView: View {
    var body: some View {
        VStack {
            Image(systemName: "photo.on.rectangle")
                .font(.largeTitle)
            Text("No posts yet")
        }
        .foregroundColor(.secondary)
    }
}

#Preview("HomePage only") {
    HomePage()
}

#Preview("HomePage with Tabs") {
    BottomTap()
}




