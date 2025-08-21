// Models/Comment.swift
// Threaded comments for posts

import Foundation

struct Comment: Identifiable, Codable, Hashable {
    let id: UUID
    var author: String
    var content: String
    var createdAt: Date
    var likes: Int
    var replies: [Comment]
    var isLiked: Bool

    init(id: UUID = UUID(), author: String, content: String, createdAt: Date = Date(), likes: Int = 0, replies: [Comment] = [], isLiked: Bool = false) {
        self.id = id
        self.author = author
        self.content = content
        self.createdAt = createdAt
        self.likes = likes
        self.replies = replies
        self.isLiked = isLiked
    }
}


