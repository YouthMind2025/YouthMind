//
//  EmotionEntry.swift
//  SaySomething
//
//  Created by Assistant on 01/08/2025.
//

import Foundation
import SwiftUI

struct EmotionEntry: Identifiable, Codable {
    let id = UUID()
    let emotion: Emotion
    let title: String
    let content: String
    let date: Date
    let intensity: Int // 1-10 scale
    
    init(emotion: Emotion, title: String, content: String, date: Date, intensity: Int = 5) {
        self.emotion = emotion
        self.title = title
        self.content = content
        self.date = date
        self.intensity = max(1, min(10, intensity)) // Clamp between 1-10
    }
}

// MARK: - Mock Data
extension EmotionEntry {
    static let mockEntries: [EmotionEntry] = [
        EmotionEntry(
            emotion: .happy,
            title: "Morning Coffee",
            content: "Had the most amazing coffee this morning. The barista remembered my order!",
            date: Date().addingTimeInterval(-3600), // 1 hour ago
            intensity: 8
        ),
        EmotionEntry(
            emotion: .sad,
            title: "Missing Home",
            content: "Feeling homesick today. Wish I could be with family right now.",
            date: Date().addingTimeInterval(-7200), // 2 hours ago
            intensity: 6
        ),
        EmotionEntry(
            emotion: .surprised,
            title: "Unexpected Gift",
            content: "My friend surprised me with tickets to the concert I've been wanting to see!",
            date: Date().addingTimeInterval(-14400), // 4 hours ago
            intensity: 9
        ),
        EmotionEntry(
            emotion: .angry,
            title: "Traffic Jam",
            content: "Stuck in traffic for 45 minutes. Completely ruined my morning routine.",
            date: Date().addingTimeInterval(-28800), // 8 hours ago
            intensity: 7
        ),
        EmotionEntry(
            emotion: .fear,
            title: "Job Interview",
            content: "Big job interview tomorrow. Nervous about making a good impression.",
            date: Date().addingTimeInterval(-86400), // 1 day ago
            intensity: 8
        ),
        EmotionEntry(
            emotion: .conflicted,
            title: "Career Decision",
            content: "Torn between staying at my current job or taking a risk with a new opportunity.",
            date: Date().addingTimeInterval(-172800), // 2 days ago
            intensity: 6
        ),
        EmotionEntry(
            emotion: .happy,
            title: "Weekend Plans",
            content: "Excited about the weekend trip with friends. Can't wait to explore the new city!",
            date: Date().addingTimeInterval(-259200), // 3 days ago
            intensity: 9
        ),
        EmotionEntry(
            emotion: .sad,
            title: "Lost Connection",
            content: "Had to say goodbye to a close friend who's moving away. Will miss them dearly.",
            date: Date().addingTimeInterval(-345600), // 4 days ago
            intensity: 7
        ),
        EmotionEntry(
            emotion: .surprised,
            title: "Random Act of Kindness",
            content: "A stranger paid for my coffee today. Restored my faith in humanity!",
            date: Date().addingTimeInterval(-432000), // 5 days ago
            intensity: 8
        ),
        EmotionEntry(
            emotion: .happy,
            title: "Personal Achievement",
            content: "Finally completed that project I've been working on for months. So proud!",
            date: Date().addingTimeInterval(-518400), // 6 days ago
            intensity: 10
        )
    ]
} 