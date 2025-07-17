// Models/Post.swift
// Defines the data model for social media posts with emotion-based styling
// Serves as the fundamental data structure for the feed and post details

import Foundation
import SwiftUI

enum Emotion: String, CaseIterable {
    case angry = "angry"
    case sad = "sad"
    case happy = "happy"
    case conflicted = "conflicted"
    case surprised = "surprised"
    case fear = "fear"
    
    var color: Color {
        switch self {
        case .angry: return .red
        case .sad: return .blue
        case .happy: return .yellow
        case .conflicted: return .black
        case .surprised: return .green
        case .fear: return .purple
        }
    }
}

struct Post: Identifiable, Hashable {
    let id = UUID()
    let resonance: Int
    let view: Int
    let collect: Int  // Number of times this post has been collected
    let title: String
    let content: String
    let preImg: String
    let isresonanced: Bool
    var isCollected: Bool  // Whether current user has collected this post (var because it can change)
    let emotion: Emotion
    let date: Date
    
    static func == (lhs: Post, rhs: Post) -> Bool {
        lhs.id == rhs.id
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}


