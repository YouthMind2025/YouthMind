//
//  FeedBlockResponse.swift
//  SaySomething

import Foundation

/// Response model for feed block requests
/// Matches backend FeedBlockResponse DTO structure
struct FeedBlockResponse: Codable {
    let posts: [Post]
    let blockNumber: Int?
    let blockMetadata: BlockMetadata?
    let nextBlockAvailable: Bool?
    let nextBlockNumber: Int?
    
    // Pagination fields from base FeedResponse
    let currentPage: Int?
    let totalPages: Int?
    let totalElements: Int64?
    let pageSize: Int?
    let hasNext: Bool?
    let isFirst: Bool?
    let isLast: Bool?
    
    enum CodingKeys: String, CodingKey {
        case posts
        case blockNumber
        case blockMetadata
        case nextBlockAvailable
        case nextBlockNumber
        case currentPage
        case totalPages
        case totalElements
        case pageSize
        case hasNext
        case isFirst
        case isLast
    }
    
    init(posts: [Post], blockNumber: Int? = nil, blockMetadata: BlockMetadata? = nil, 
         nextBlockAvailable: Bool? = nil, nextBlockNumber: Int? = nil,
         currentPage: Int? = nil, totalPages: Int? = nil, totalElements: Int64? = nil,
         pageSize: Int? = nil, hasNext: Bool? = nil, isFirst: Bool? = nil, isLast: Bool? = nil) {
        self.posts = posts
        self.blockNumber = blockNumber
        self.blockMetadata = blockMetadata
        self.nextBlockAvailable = nextBlockAvailable
        self.nextBlockNumber = nextBlockNumber
        self.currentPage = currentPage
        self.totalPages = totalPages
        self.totalElements = totalElements
        self.pageSize = pageSize
        self.hasNext = hasNext
        self.isFirst = isFirst
        self.isLast = isLast
    }
}

/// Metadata for feed blocks containing emotion distribution and generation information
/// Matches backend BlockMetadata DTO structure
struct BlockMetadata: Codable {
    let emotionDistribution: [String: Int]
    let rVectorVersion: Int?
    let generatedAt: Date
    let expiresAt: Date
    let isPersonalized: Bool
    
    enum CodingKeys: String, CodingKey {
        case emotionDistribution
        case rVectorVersion
        case generatedAt
        case expiresAt
        case isPersonalized
    }
    
    init(emotionDistribution: [String: Int], rVectorVersion: Int? = nil, 
         generatedAt: Date, expiresAt: Date, isPersonalized: Bool) {
        self.emotionDistribution = emotionDistribution
        self.rVectorVersion = rVectorVersion
        self.generatedAt = generatedAt
        self.expiresAt = expiresAt
        self.isPersonalized = isPersonalized
    }
    
    /// Check if this block has expired
    var isExpired: Bool {
        return Date() > expiresAt
    }
    
    /// Get the age of this block in hours
    var ageInHours: Int {
        let timeInterval = Date().timeIntervalSince(generatedAt)
        return Int(timeInterval / 3600)
    }
    
    /// Get time until expiration in hours
    var timeUntilExpirationInHours: Int {
        let timeInterval = expiresAt.timeIntervalSince(Date())
        return max(0, Int(timeInterval / 3600))
    }
    
    /// Get the total number of posts in this block
    var totalPostCount: Int {
        return emotionDistribution.values.reduce(0, +)
    }
    
    /// Get the dominant emotion in this block
    var dominantEmotion: String? {
        return emotionDistribution.max(by: { $0.value < $1.value })?.key
    }
    
    /// Get emotion distribution as percentages
    var emotionDistributionPercentages: [String: Double] {
        let total = totalPostCount
        guard total > 0 else { return [:] }
        
        return emotionDistribution.mapValues { Double($0) / Double(total) }
    }
}

/// Response model for feed scroll notifications
/// Matches backend FeedScrollResponse DTO structure
struct FeedScrollResponse: Codable {
    let nextBlockReady: Bool
    let estimatedWaitTime: Int
    let currentBlock: Int
    let isGenerating: Bool
    
    enum CodingKeys: String, CodingKey {
        case nextBlockReady
        case estimatedWaitTime
        case currentBlock
        case isGenerating
    }
    
    init(nextBlockReady: Bool, estimatedWaitTime: Int, currentBlock: Int, isGenerating: Bool) {
        self.nextBlockReady = nextBlockReady
        self.estimatedWaitTime = estimatedWaitTime
        self.currentBlock = currentBlock
        self.isGenerating = isGenerating
    }
}

/// Response model for feed status requests
/// Matches backend FeedStatusResponse DTO structure
struct FeedStatusResponse: Codable {
    let currentBlock: Int
    let rVectorSummary: RVectorSummary
    let nextBlockAvailability: NextBlockAvailability
    let userActivityStatus: UserActivityStatus
    
    enum CodingKeys: String, CodingKey {
        case currentBlock
        case rVectorSummary
        case nextBlockAvailability
        case userActivityStatus
    }
    
    init(currentBlock: Int, rVectorSummary: RVectorSummary, 
         nextBlockAvailability: NextBlockAvailability, userActivityStatus: UserActivityStatus) {
        self.currentBlock = currentBlock
        self.rVectorSummary = rVectorSummary
        self.nextBlockAvailability = nextBlockAvailability
        self.userActivityStatus = userActivityStatus
    }
}

/// R-vector summary information
struct RVectorSummary: Codable {
    let rVector: [Double]
    let topEmotions: [String]
    let lastUpdated: Date
    let interactionCount: Int
    
    enum CodingKeys: String, CodingKey {
        case rVector
        case topEmotions
        case lastUpdated
        case interactionCount
    }
    
    init(rVector: [Double], topEmotions: [String], lastUpdated: Date, interactionCount: Int) {
        self.rVector = rVector
        self.topEmotions = topEmotions
        self.lastUpdated = lastUpdated
        self.interactionCount = interactionCount
    }
}

/// Next block availability information
struct NextBlockAvailability: Codable {
    let isAvailable: Bool
    let blockNumber: Int
    let availableAt: Date
    let waitTime: Int
    
    enum CodingKeys: String, CodingKey {
        case isAvailable
        case blockNumber
        case availableAt
        case waitTime
    }
    
    init(isAvailable: Bool, blockNumber: Int, availableAt: Date, waitTime: Int) {
        self.isAvailable = isAvailable
        self.blockNumber = blockNumber
        self.availableAt = availableAt
        self.waitTime = waitTime
    }
}

/// User activity status information
struct UserActivityStatus: Codable {
    let isActive: Bool
    let lastScrollTime: Date
    let totalBlocksConsumed: Int
    let engagementScore: Double
    
    enum CodingKeys: String, CodingKey {
        case isActive
        case lastScrollTime
        case totalBlocksConsumed
        case engagementScore
    }
    
    init(isActive: Bool, lastScrollTime: Date, totalBlocksConsumed: Int, engagementScore: Double) {
        self.isActive = isActive
        self.lastScrollTime = lastScrollTime
        self.totalBlocksConsumed = totalBlocksConsumed
        self.engagementScore = engagementScore
    }
}
