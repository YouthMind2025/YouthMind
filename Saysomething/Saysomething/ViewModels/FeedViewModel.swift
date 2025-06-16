// ViewModels/FeedViewModel.swift
// Real API will be call in fetchPosts(), by replacing the mockdata


import SwiftUI

class FeedViewModel: ObservableObject {
    @Published var posts: [Post] = []  // Updates View when changed
    @Published var isLoading = false  //Tracks loading state for progress indicators
    @Published var error: Error?  //Stores error information when requests fail
    
    
    // split the posts array into two halves to implment two-column grid in home page.
    var leftColumnPosts: [Post] {
        Array(posts.prefix(posts.count / 2))
    }
    var rightColumnPosts: [Post] {
        Array(posts.suffix(posts.count / 2))
    }
    
    // Initial setup with automatic data loading
    //Remark: we cannot call async fetchPosts in init()
    init(){
            loadInitialData()
        }
    private func loadInitialData() {
        Task {
            await fetchPosts()
        }
    }
    
    
    // Data methods:
    // Fetches posts from API (currently using mock data)
    // Handles loading states, errors, and updates UI thread
    func fetchPosts() async {
            isLoading = true
            error = nil // Reset error state
            
            do {
                // Simulate network delay (2 seconds)
                try await Task.sleep(nanoseconds: 2 * 1_000_000_000)
                
                //If need a real API call, it will loooks like:
                //let newPosts = try await APIService.fetchPosts()
                
                // Mock data - replace with real API call(maybe by above format)
                let mockPosts = [
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 120, view: 366, title: "KDB felt cold", preImg: "character1", isresonanced: true),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 120, view: 366, title: "KDB felt cold", preImg: "character1", isresonanced: true),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 120, view: 366, title: "KDB felt cold", preImg: "character1", isresonanced: true),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 120, view: 366, title: "KDB felt cold", preImg: "character1", isresonanced: true),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false),
                    Post(resonance: 120, view: 366, title: "KDB felt cold", preImg: "character1", isresonanced: true),
                    Post(resonance: 45, view: 200, title: "Summer outfit ideas", preImg: "character2", isresonanced: false)
                    
                ]
                
                
                //Update UI on main thread
                await MainActor.run {
                    posts = mockPosts
                    isLoading = false
//                    If Real API fetch:
//                    posts = newPosts
//                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.error = error
                    isLoading = false
//                    If real API fetch:
//                    self.error = error
//                    isLoading = false
                }
            }
        }
    
//    Example method for handling post engagement:
//    func likePost(postId: UUID) {
//        if let index = posts.firstIndex(where: { $0.id == postId }) {
//            posts[index].isresonanced.toggle()
//            posts[index].resonance += posts[index].isresonanced ? 1 : -1
//        }
//    }
    
}
