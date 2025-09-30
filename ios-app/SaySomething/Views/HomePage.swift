//Views/HomePage.swift
//Implement basic structure of the HomePage
// Including topBar, feed and bottomtap UI.


import SwiftUI

struct HomePage: View {
    @EnvironmentObject var viewModel: FeedViewModel
    @StateObject private var router = NavigationRouter()
    @EnvironmentObject private var tabBarState: TabBarState
    
    var body: some View {
        NavigationStack(path: $router.path) {
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
                    FeedView()
                        .environmentObject(viewModel)
                }
            }
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
                    .environmentObject(viewModel)
                    .onAppear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true } }
                    .onDisappear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = false } }
            }
        }
        .environmentObject(router)
//        .toolbar {
//            ToolbarItem(placement: .topBarTrailing) {
//                NavigationLink {
//                    SearchView()
//                        .environmentObject(viewModel)
//                        .environmentObject(tabBarState)
//                } label: {
//                    HStack(spacing: 4) {
//                        Image(systemName: "magnifyingglass")
//                        Text("Search")
//                    }
//                }
//            }
//        }
        // FeedViewModel already loads initial data in its initializer
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
        .environmentObject(FeedViewModel())
}

#Preview("HomePage with Tabs") {
    BottomTap()
}






