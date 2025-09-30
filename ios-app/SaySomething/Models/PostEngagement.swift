// Models/PostEngagement.swift
// Holds mutable engagement metrics separate from post content

import Foundation

struct PostEngagement: Identifiable, Codable, Hashable {
    var id: UUID { postId }
    let postId: UUID
    var resonance: Int
    var views: Int
    var saves: Int
    var isResonanced: Bool
    var isCollected: Bool
}


