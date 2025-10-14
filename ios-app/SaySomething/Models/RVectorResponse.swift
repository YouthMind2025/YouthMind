//
//  RVectorResponse.swift
//  SaySomething


import Foundation

/// Response from GET /v1/recommendation/rvector
struct RVectorResponse: Codable {
    let version: Int
    let weights: [String: Double]
    let topEmotions: [String]
    let updatedAt: Date
    let totalWeight: Double
    let userId: UUID
    
    enum CodingKeys: String, CodingKey {
        case version
        case weights
        case topEmotions
        case updatedAt
        case totalWeight
        case userId
    }
}

/// Response from POST /v1/recommendation/rvector/reset
struct RVectorResetResponse: Codable {
    let userId: UUID
    let success: Bool
    let resetAt: Date
    let reason: String
    let newRVector: [Double]
    
    enum CodingKeys: String, CodingKey {
        case userId
        case success
        case resetAt
        case reason
        case newRVector
    }
}
