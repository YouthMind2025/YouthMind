// Models/Post.swift
// Defines the data model for social media posts with emotion-based styling
// Serves as the fundamental data structure for the feed and post details

import Foundation
import SwiftUI

enum Emotion: String, CaseIterable, Codable {
    case angry = "angry"
    case sad = "sad"
    case happy = "happy"
    case conflicted = "conflicted"
    case surprised = "surprised"
    case fear = "fear"
    
    var color: Color {
        switch self {
        case .angry: return Color(hex: "#fe7877")
        case .sad: return Color(hex: "#7fbafe")
        case .happy: return Color(hex: "#ffd700")
        case .conflicted: return Color(hex: "#929584")
        case .surprised: return Color(hex: "#7ed7c3")
        case .fear: return DesignConstants.ColorPalette.brandPurple
        }
    }
}

struct Post: Identifiable, Hashable, Codable {
    let id: UUID
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
    init(id: UUID = UUID(), resonance: Int, view: Int, collect: Int, title: String, content: String, preImg: String, isresonanced: Bool, isCollected: Bool, emotion: Emotion, date: Date) {
        self.id = id
        self.resonance = resonance
        self.view = view
        self.collect = collect
        self.title = title
        self.content = content
        self.preImg = preImg
        self.isresonanced = isresonanced
        self.isCollected = isCollected
        self.emotion = emotion
        self.date = date
    }
}


