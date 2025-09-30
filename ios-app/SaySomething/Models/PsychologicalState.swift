//
//  PsychologicalState.swift
//  SaySomething
//
//  Created by AI Assistant on 1/15/25.
//

import Foundation

// MARK: - Psychological State Models (Youseef's Integration)

/// Represents a user's psychological state with 43-dimensional Buddhist mental factors
struct PsychologicalState: Codable, Identifiable {
    let id = UUID()
    let stress: Double
    let mindfulness: Double
    let compassion: Double
    let equanimity: Double
    let intensityFactor: Double
    let timestamp: Date
    let vectorDimensions: Int
    
    init(stress: Double = 0.3, mindfulness: Double = 0.5, compassion: Double = 0.6, 
         equanimity: Double = 0.4, intensityFactor: Double = 0.5, 
         timestamp: Date = Date(), vectorDimensions: Int = 43) {
        self.stress = stress
        self.mindfulness = mindfulness
        self.compassion = compassion
        self.equanimity = equanimity
        self.intensityFactor = intensityFactor
        self.timestamp = timestamp
        self.vectorDimensions = vectorDimensions
    }
    
    /// Determine emotional state from psychological metrics
    var emotionalState: EmotionalState {
        if stress > 0.7 {
            return .anxious
        } else if mindfulness > 0.6 && compassion > 0.5 {
            return .peaceful
        } else if compassion > 0.7 {
            return .empathetic
        } else if mindfulness > 0.5 {
            return .mindful
        } else {
            return .neutral
        }
    }
    
    /// Determine communication style from Buddhist factors
    var communicationStyle: CommunicationStyle {
        if mindfulness > 0.6 && compassion > 0.5 {
            return .gentle
        } else if equanimity > 0.6 {
            return .balanced
        } else if compassion > 0.7 {
            return .warm
        } else {
            return .supportive
        }
    }
    
    /// Determine personality type from psychological state
    var personalityType: PersonalityType {
        if stress > 0.7 {
            return .empathetic
        } else if mindfulness > 0.6 && compassion > 0.5 {
            return .reflective
        } else if compassion > 0.7 {
            return .empathetic
        } else if mindfulness > 0.5 {
            return .mindful
        } else {
            return .supportive
        }
    }
}

/// Buddhist mental factors analysis
struct BuddhistFactors: Codable, Identifiable {
    let id = UUID()
    let mindfulness: Double
    let compassion: Double
    let equanimity: Double
    let nonAttachment: Double
    let wisdom: Double
    let vigor: Double
    let faith: Double
    let innerShame: Double
    let shame: Double
    let absenceOfCraving: Double
    let absenceOfHatred: Double
    let absenceOfIgnorance: Double
    let lightEase: Double
    let nonLaxness: Double
    let renunciation: Double
    let nonHarming: Double
    let timestamp: Date
    
    init(mindfulness: Double = 0.4, compassion: Double = 0.3, equanimity: Double = 0.2,
         nonAttachment: Double = 0.1, wisdom: Double = 0.3, vigor: Double = 0.2,
         faith: Double = 0.4, innerShame: Double = 0.1, shame: Double = 0.1,
         absenceOfCraving: Double = 0.2, absenceOfHatred: Double = 0.2,
         absenceOfIgnorance: Double = 0.3, lightEase: Double = 0.2,
         nonLaxness: Double = 0.3, renunciation: Double = 0.2,
         nonHarming: Double = 0.4, timestamp: Date = Date()) {
        self.mindfulness = mindfulness
        self.compassion = compassion
        self.equanimity = equanimity
        self.nonAttachment = nonAttachment
        self.wisdom = wisdom
        self.vigor = vigor
        self.faith = faith
        self.innerShame = innerShame
        self.shame = shame
        self.absenceOfCraving = absenceOfCraving
        self.absenceOfHatred = absenceOfHatred
        self.absenceOfIgnorance = absenceOfIgnorance
        self.lightEase = lightEase
        self.nonLaxness = nonLaxness
        self.renunciation = renunciation
        self.nonHarming = nonHarming
        self.timestamp = timestamp
    }
}

/// Psychological insights derived from analysis
struct PsychologicalInsights: Codable, Identifiable {
    let id = UUID()
    let primaryEmotion: EmotionalState
    let secondaryEmotions: [EmotionalState]
    let emotionalIntensity: Double
    let emotionalStability: Double
    let overallAssessment: String
    let recommendations: [String]
    let insights: [String]
    let riskAssessment: RiskAssessment
    let timestamp: Date
    
    init(primaryEmotion: EmotionalState = .neutral, secondaryEmotions: [EmotionalState] = [],
         emotionalIntensity: Double = 0.5, emotionalStability: Double = 0.5,
         overallAssessment: String = "Stable emotional state",
         recommendations: [String] = [], insights: [String] = [],
         riskAssessment: RiskAssessment = RiskAssessment(), timestamp: Date = Date()) {
        self.primaryEmotion = primaryEmotion
        self.secondaryEmotions = secondaryEmotions
        self.emotionalIntensity = emotionalIntensity
        self.emotionalStability = emotionalStability
        self.overallAssessment = overallAssessment
        self.recommendations = recommendations
        self.insights = insights
        self.riskAssessment = riskAssessment
        self.timestamp = timestamp
    }
}

/// Risk assessment for psychological state
struct RiskAssessment: Codable {
    let level: RiskLevel
    let factors: [String]
    let needsHumanReview: Bool
    let reviewReason: String
    
    init(level: RiskLevel = .low, factors: [String] = [],
         needsHumanReview: Bool = false, reviewReason: String = "") {
        self.level = level
        self.factors = factors
        self.needsHumanReview = needsHumanReview
        self.reviewReason = reviewReason
    }
}

// MARK: - Enums

enum EmotionalState: String, CaseIterable, Codable {
    case anxious = "anxious"
    case peaceful = "peaceful"
    case empathetic = "empathetic"
    case mindful = "mindful"
    case neutral = "neutral"
    case happy = "happy"
    case sad = "sad"
    case angry = "angry"
    case confused = "confused"
    case calm = "calm"
    
    var displayName: String {
        switch self {
        case .anxious: return "Anxious"
        case .peaceful: return "Peaceful"
        case .empathetic: return "Empathetic"
        case .mindful: return "Mindful"
        case .neutral: return "Neutral"
        case .happy: return "Happy"
        case .sad: return "Sad"
        case .angry: return "Angry"
        case .confused: return "Confused"
        case .calm: return "Calm"
        }
    }
    
    var color: String {
        switch self {
        case .anxious: return "orange"
        case .peaceful: return "green"
        case .empathetic: return "purple"
        case .mindful: return "blue"
        case .neutral: return "gray"
        case .happy: return "yellow"
        case .sad: return "blue"
        case .angry: return "red"
        case .confused: return "purple"
        case .calm: return "teal"
        }
    }
}

enum CommunicationStyle: String, CaseIterable, Codable {
    case gentle = "gentle"
    case balanced = "balanced"
    case warm = "warm"
    case supportive = "supportive"
    case direct = "direct"
    case casual = "casual"
    case formal = "formal"
    
    var displayName: String {
        switch self {
        case .gentle: return "Gentle"
        case .balanced: return "Balanced"
        case .warm: return "Warm"
        case .supportive: return "Supportive"
        case .direct: return "Direct"
        case .casual: return "Casual"
        case .formal: return "Formal"
        }
    }
}

enum PersonalityType: String, CaseIterable, Codable {
    case empathetic = "empathetic"
    case reflective = "reflective"
    case mindful = "mindful"
    case supportive = "supportive"
    case professional = "professional"
    case casual = "casual"
    
    var displayName: String {
        switch self {
        case .empathetic: return "Empathetic"
        case .reflective: return "Reflective"
        case .mindful: return "Mindful"
        case .supportive: return "Supportive"
        case .professional: return "Professional"
        case .casual: return "Casual"
        }
    }
}

enum ResponseTone: String, CaseIterable, Codable {
    case calming = "calming"
    case reflective = "reflective"
    case empathetic = "empathetic"
    case mindful = "mindful"
    case supportive = "supportive"
    
    var displayName: String {
        switch self {
        case .calming: return "Calming"
        case .reflective: return "Reflective"
        case .empathetic: return "Empathetic"
        case .mindful: return "Mindful"
        case .supportive: return "Supportive"
        }
    }
    
    var description: String {
        switch self {
        case .calming: return "Gentle and soothing responses"
        case .reflective: return "Thoughtful and introspective responses"
        case .empathetic: return "Compassionate and understanding responses"
        case .mindful: return "Present-moment focused responses"
        case .supportive: return "Encouraging and helpful responses"
        }
    }
}

enum RiskLevel: String, CaseIterable, Codable {
    case low = "low"
    case medium = "medium"
    case high = "high"
    case critical = "critical"
    
    var displayName: String {
        switch self {
        case .low: return "Low Risk"
        case .medium: return "Medium Risk"
        case .high: return "High Risk"
        case .critical: return "Critical Risk"
        }
    }
    
    var color: String {
        switch self {
        case .low: return "green"
        case .medium: return "yellow"
        case .high: return "orange"
        case .critical: return "red"
        }
    }
}
