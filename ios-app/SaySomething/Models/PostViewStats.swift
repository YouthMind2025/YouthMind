// Models/PostViewStats.swift
// Defines the data model for post view statistics and analytics
// Provides insights into post engagement and reach

import Foundation

struct PostViewStats: Identifiable, Codable {
    let id: UUID
    let totalViews: Int
    let recentViews: Int
    let uniqueViewers: Int
    
    // Computed properties for analytics
    var viewGrowthRate: Double {
        guard totalViews > 0 else { return 0.0 }
        return (Double(recentViews) / Double(totalViews)) * 100.0
    }
    
    var hasHighEngagement: Bool {
        return viewGrowthRate > 10.0
    }
    
    var engagementLevel: String {
        let rate = viewGrowthRate
        if rate > 20.0 { return "Very High" }
        if rate > 10.0 { return "High" }
        if rate > 5.0 { return "Moderate" }
        if rate > 1.0 { return "Low" }
        return "Very Low"
    }
    
    // MARK: - Codable Keys
    
    enum CodingKeys: String, CodingKey {
        case id = "postId"
        case totalViews
        case recentViews
        case uniqueViewers
    }
    
    // MARK: - Initialization
    
    init(id: UUID, totalViews: Int, recentViews: Int, uniqueViewers: Int) {
        self.id = id
        self.totalViews = totalViews
        self.recentViews = recentViews
        self.uniqueViewers = uniqueViewers
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        totalViews = try container.decode(Int.self, forKey: .totalViews)
        recentViews = try container.decode(Int.self, forKey: .recentViews)
        uniqueViewers = try container.decode(Int.self, forKey: .uniqueViewers)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(totalViews, forKey: .totalViews)
        try container.encode(recentViews, forKey: .recentViews)
        try container.encode(uniqueViewers, forKey: .uniqueViewers)
    }
}

// MARK: - Extensions

extension PostViewStats {
    /// Format view count with appropriate suffix (K, M, etc.)
    var formattedTotalViews: String {
        return formatCount(totalViews)
    }
    
    /// Format recent view count with appropriate suffix
    var formattedRecentViews: String {
        return formatCount(recentViews)
    }
    
    /// Format unique viewers count with appropriate suffix
    var formattedUniqueViewers: String {
        return formatCount(uniqueViewers)
    }
    
    /// Format growth rate as percentage with one decimal place
    var formattedGrowthRate: String {
        return String(format: "%.1f%%", viewGrowthRate)
    }
    
    /// Check if post is trending (high recent activity)
    var isTrending: Bool {
        return viewGrowthRate > 15.0
    }
    
    /// Get engagement color based on level
    var engagementColor: String {
        switch engagementLevel {
        case "Very High": return "#4CAF50" // Green
        case "High": return "#8BC34A"      // Light Green
        case "Moderate": return "#FFC107"  // Amber
        case "Low": return "#FF9800"       // Orange
        case "Very Low": return "#F44336"  // Red
        default: return "#9E9E9E"          // Grey
        }
    }
    
    // MARK: - Private Helper Methods
    
    private func formatCount(_ count: Int) -> String {
        switch count {
        case 0..<1000:
            return "\(count)"
        case 1000..<1000000:
            let thousands = Double(count) / 1000.0
            return String(format: "%.1fK", thousands)
        default:
            let millions = Double(count) / 1000000.0
            return String(format: "%.1fM", millions)
        }
    }
} 