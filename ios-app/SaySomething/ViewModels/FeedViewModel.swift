// ViewModels/FeedViewModel.swift
// Real API will be call in fetchPosts(), by replacing the mockdata
// These codes does:
// Manages the feed's data state and business logic
// Bridges between the view (UI) and data layer (API/mock data)


import SwiftUI

class FeedViewModel: ObservableObject {
    @Published var posts: [Post] = []  // Updates View when changed
    @Published var isLoading = false  //Tracks loading state for progress indicators
    @Published var error: Error?  //Stores error information when requests fail
    @Published var comments: [UUID: [Comment]] = [:] // Post ID -> comments
    @Published var engagement: [UUID: PostEngagement] = [:] // Post ID -> engagement
    
    
    // split the posts array into two halves to implment two-column grid in home page.
    var leftColumnPosts: [Post] {
        Array(posts.prefix(posts.count / 2))
    }
    var rightColumnPosts: [Post] {
        Array(posts.suffix(posts.count / 2))
    }
    
    // Initial setup with automatic data loading
    // Remark: we cannot call async fetchPosts in init()
    init(){
        // 延迟加载，避免在init中调用异步方法
        DispatchQueue.main.async {
            Task {
                await self.fetchPosts()
            }
        }
    }
    
    
    // Data methods:
    // Fetches posts from API (currently using mock data)
    // Handles loading states, errors, and updates UI thread
    func fetchPosts() async {
            isLoading = true
            error = nil // Reset error state
            
            do {
                //If need a real API call, it will loooks like:
                //let newPosts = try await APIService.fetchPosts()
                
                // Mock data - replace with real API call(maybe by above format)
                let mockPosts = [
                    // Happy posts
                    Post(resonance: 45, view: 200, collect: 32,
                         title: "Summer outfit ideas",
                         content: "Just discovered the perfect summer dress that's both stylish and comfortable for hot days. The lightweight fabric and vibrant colors make me feel so happy every time I wear it!",
                         preImg: "happy_outfit", isresonanced: false,
                         isCollected: false, emotion: .happy,
                         date: Date().addingTimeInterval(-86400)),
                    
                    Post(resonance: 120, view: 366, collect: 89,
                         title: "Puppy adoption day!",
                         content: "After years of waiting, finally brought home this golden retriever puppy. The way he tilts his head when confused fills my heart with joy every single time.",
                         preImg: "happy_puppy", isresonanced: true,
                         isCollected: true, emotion: .happy,
                         date: Date().addingTimeInterval(-172800)),
                    
                    // Sad posts
                    Post(resonance: 78, view: 450, collect: 43,
                         title: "Lost my favorite book",
                         content: "The signed first edition of my favorite novel that my grandmother gave me was lost in the move. It held so many memories and I can't believe it's gone forever.",
                         preImg: "sad_book", isresonanced: false,
                         isCollected: false, emotion: .sad,
                         date: Date().addingTimeInterval(-259200)),
                    
                    // Angry posts
                    Post(resonance: 210, view: 890, collect: 156,
                         title: "Parking ticket injustice",
                         content: "Got a $75 parking ticket despite having paid via the app. The receipt clearly shows payment but the officer refused to even look at my evidence. This system is broken!",
                         preImg: "angry_ticket", isresonanced: true,
                         isCollected: false, emotion: .angry,
                         date: Date().addingTimeInterval(-43200)),
                    
                    // Conflicted posts
                    Post(resonance: 65, view: 320, collect: 28,
                         title: "Job offer dilemma",
                         content: "Got offered my dream job in another country, but it would mean leaving my aging parents behind. Torn between career aspirations and family responsibilities.",
                         preImg: "conflicted_job", isresonanced: false,
                         isCollected: true, emotion: .conflicted,
                         date: Date().addingTimeInterval(-345600)),
                    
                    // Surprised posts
                    Post(resonance: 134, view: 670, collect: 92,
                         title: "Unexpected reunion",
                         content: "Ran into my childhood best friend at the airport after 15 years - neither of us knew the other would be there! We both screamed and hugged right at the baggage claim.",
                         preImg: "surprised_reunion", isresonanced: true,
                         isCollected: false, emotion: .surprised,
                         date: Date().addingTimeInterval(-86400)),
                    
                    // Fear posts
                    Post(resonance: 98, view: 540, collect: 37,
                         title: "Near miss on highway",
                         content: "A truck swerved into my lane last night - I had to brake so hard my coffee flew everywhere. Shaking just thinking about what could have happened.",
                         preImg: "fear_driving", isresonanced: false,
                         isCollected: false, emotion: .fear,
                         date: Date().addingTimeInterval(-28800)),
                    
                    // Additional varied posts
                    Post(resonance: 87, view: 410, collect: 64,
                         title: "Angry at broken promises",
                         content: "My roommate promised to clean the apartment before my parents visited but left everything in chaos. The disrespect makes my blood boil every time I think about it.",
                         preImg: "angry_roommate", isresonanced: true,
                         isCollected: true, emotion: .angry,
                         date: Date().addingTimeInterval(-172800)),
                    
                    Post(resonance: 52, view: 380, collect: 19,
                         title: "Conflicted about moving",
                         content: "Part of me wants to sell everything and travel the world, while another part is terrified of leaving my stable life behind. This back-and-forth is exhausting.",
                         preImg: "conflicted_travel", isresonanced: false,
                         isCollected: false, emotion: .conflicted,
                         date: Date().addingTimeInterval(-604800)),
                    
                    Post(resonance: 143, view: 720, collect: 105,
                         title: "Surprise birthday party",
                         content: "Thought everyone forgot my birthday until I walked into what I believed was a casual dinner to find all my friends yelling 'Surprise!' Complete shock but so touched.",
                         preImg: "surprised_birthday", isresonanced: true,
                         isCollected: true, emotion: .surprised,
                         date: Date().addingTimeInterval(-432000)),
                    
                    Post(resonance: 76, view: 490, collect: 42,
                         title: "Fear of public speaking",
                         content: "Have to give a big presentation tomorrow and my hands won't stop shaking during practice. The thought of everyone staring at me makes me want to hide forever.",
                         preImg: "fear_speaking", isresonanced: false,
                         isCollected: false, emotion: .fear,
                         date: Date().addingTimeInterval(-86400)),
                    
                    
                    
                    
                    
                    
                    
                    // more copies of posts:
                    Post(resonance: 87, view: 410, collect: 64,
                         title: "Angry at broken promises",
                         content: "My roommate promised to clean the apartment before my parents visited but left everything in chaos. The disrespect makes my blood boil every time I think about it.",
                         preImg: "angry_roommate", isresonanced: true,
                         isCollected: true, emotion: .angry,
                         date: Date().addingTimeInterval(-172800)),
                    
                    Post(resonance: 52, view: 380, collect: 19,
                         title: "Conflicted about moving",
                         content: "Part of me wants to sell everything and travel the world, while another part is terrified of leaving my stable life behind. This back-and-forth is exhausting.",
                         preImg: "conflicted_travel", isresonanced: false,
                         isCollected: false, emotion: .conflicted,
                         date: Date().addingTimeInterval(-604800)),
                    
                    Post(resonance: 143, view: 720, collect: 105,
                         title: "Surprise birthday party",
                         content: "Thought everyone forgot my birthday until I walked into what I believed was a casual dinner to find all my friends yelling 'Surprise!' Complete shock but so touched.",
                         preImg: "surprised_birthday", isresonanced: true,
                         isCollected: true, emotion: .surprised,
                         date: Date().addingTimeInterval(-432000)),
                    
                    Post(resonance: 76, view: 490, collect: 42,
                         title: "Fear of public speaking",
                         content: "Have to give a big presentation tomorrow and my hands won't stop shaking during practice. The thought of everyone staring at me makes me want to hide forever.",
                         preImg: "fear_speaking", isresonanced: false,
                         isCollected: false, emotion: .fear,
                         date: Date().addingTimeInterval(-86400)),
                ]
                
                
                //Update UI on main thread
                await MainActor.run {
                    posts = mockPosts
                    // seed comments for demo
                    for post in mockPosts {
                        comments[post.id] = FeedViewModel.sampleComments()
                        engagement[post.id] = PostEngagement(
                            postId: post.id,
                            resonance: post.resonance,
                            views: post.view,
                            saves: post.collect,
                            isResonanced: post.isresonanced,
                            isCollected: post.isCollected
                        )
                    }
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

extension FeedViewModel {
    static func sampleComments() -> [Comment] {
        let reply1 = Comment(author: "Ava", content: "Totally agree – the fabric matters a lot!", likes: 2)
        let reply2 = Comment(author: "Leo", content: "Where did you buy it?", likes: 1)
        let c1 = Comment(author: "Mia", content: "Love this! Got a similar one last week.", likes: 12, replies: [reply1])
        let c2 = Comment(author: "Noah", content: "Would this work for humid weather?", likes: 3, replies: [reply2])
        let c3 = Comment(author: "Zoe", content: "Link please!", likes: 6)
        return [c1, c3, c2]
    }

    // MARK: - Mutations
    func incrementView(for postId: UUID) {
        if var e = engagement[postId] { e.views += 1; engagement[postId] = e }
    }

    func updateResonance(for postId: UUID, liked: Bool) {
        guard var e = engagement[postId] else { return }
        let delta: Int
        if liked && !e.isResonanced { delta = 1 }
        else if !liked && e.isResonanced { delta = -1 }
        else { delta = 0 }
        e.resonance += delta
        e.isResonanced = liked
        engagement[postId] = e
    }

    func updateCollection(for postId: UUID, collected: Bool) {
        guard var e = engagement[postId] else { return }
        let delta: Int
        if collected && !e.isCollected { delta = 1 }
        else if !collected && e.isCollected { delta = -1 }
        else { delta = 0 }
        e.saves += delta
        e.isCollected = collected
        engagement[postId] = e
    }

    // Refresh comments for a specific post (simulates a network fetch)
    func refreshComments(for postId: UUID) async {
        do {
            try await Task.sleep(nanoseconds: 600_000_000) // 0.6s
        } catch {}
        await MainActor.run {
            comments[postId] = FeedViewModel.sampleComments()
        }
    }

    @MainActor
    func addNewPost(title: String, content: String, emotion: Emotion = .happy) {
        let newPost = Post(
            resonance: 0,
            view: 0,
            collect: 0,
            title: title,
            content: content,
            preImg: "", // 暂时为空，后续可以添加图片功能
            isresonanced: false,
            isCollected: false,
            emotion: emotion,
            date: Date()
        )
        
        // 将新Post添加到posts数组的开头
        self.posts.insert(newPost, at: 0)
        
        // 为新Post创建默认的engagement数据
        self.engagement[newPost.id] = PostEngagement(
            postId: newPost.id,
            resonance: 0,
            views: 0,
            saves: 0,
            isResonanced: false,
            isCollected: false
        )
        
        // 为新Post创建空的comments数组
        self.comments[newPost.id] = []
        
        print("New post added: \(title) - Total posts: \(self.posts.count)")
    }
}
