//
//  PsychologicalInsightsView.swift
//  SaySomething
//
//  Created by AI Assistant on 1/15/25.
//

import SwiftUI

// MARK: - Psychological Insights View (Youseef's Integration)

struct PsychologicalInsightsView: View {
    @ObservedObject var chatViewModel: ChatViewModel
    @State private var showingDetailedView = false
    
    var body: some View {
        VStack(spacing: 16) {
            // Header
            HStack {
                Image(systemName: "brain.head.profile")
                    .foregroundColor(.blue)
                    .font(.title2)
                
                Text("Psychological Insights")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                Button(action: {
                    showingDetailedView.toggle()
                }) {
                    Image(systemName: showingDetailedView ? "chevron.up" : "chevron.down")
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal)
            
            if showingDetailedView {
                detailedInsightsView
            } else {
                compactInsightsView
            }
        }
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }
    
    // MARK: - Compact View
    private var compactInsightsView: some View {
        VStack(spacing: 12) {
            // Current State Summary
            HStack(spacing: 20) {
                // Emotional State
                VStack {
                    Circle()
                        .fill(Color(chatViewModel.psychologicalState.emotionalState.color))
                        .frame(width: 40, height: 40)
                        .overlay(
                            Image(systemName: emotionIcon)
                                .foregroundColor(.white)
                                .font(.system(size: 16))
                        )
                    
                    Text(chatViewModel.psychologicalState.emotionalState.displayName)
                        .font(.caption)
                        .fontWeight(.medium)
                }
                
                // Response Tone
                VStack {
                    Circle()
                        .fill(Color(chatViewModel.responseTone.displayName.lowercased() == "calming" ? "orange" : 
                                   chatViewModel.responseTone.displayName.lowercased() == "reflective" ? "purple" :
                                   chatViewModel.responseTone.displayName.lowercased() == "empathetic" ? "pink" :
                                   chatViewModel.responseTone.displayName.lowercased() == "mindful" ? "blue" : "green"))
                        .frame(width: 40, height: 40)
                        .overlay(
                            Image(systemName: toneIcon)
                                .foregroundColor(.white)
                                .font(.system(size: 16))
                        )
                    
                    Text(chatViewModel.responseTone.displayName)
                        .font(.caption)
                        .fontWeight(.medium)
                }
                
                // Risk Level
                VStack {
                    Circle()
                        .fill(Color(chatViewModel.psychologicalInsights.riskAssessment.level.color))
                        .frame(width: 40, height: 40)
                        .overlay(
                            Image(systemName: riskIcon)
                                .foregroundColor(.white)
                                .font(.system(size: 16))
                        )
                    
                    Text(chatViewModel.psychologicalInsights.riskAssessment.level.displayName)
                        .font(.caption)
                        .fontWeight(.medium)
                }
            }
            
            // Quick Metrics
            HStack(spacing: 16) {
                MetricBar(title: "Stress", value: chatViewModel.psychologicalState.stress, color: .orange)
                MetricBar(title: "Mindfulness", value: chatViewModel.psychologicalState.mindfulness, color: .blue)
                MetricBar(title: "Compassion", value: chatViewModel.psychologicalState.compassion, color: .pink)
            }
        }
        .padding(.horizontal)
        .padding(.bottom)
    }
    
    // MARK: - Detailed View
    private var detailedInsightsView: some View {
        VStack(spacing: 16) {
            // Psychological State Details
            VStack(alignment: .leading, spacing: 12) {
                Text("Current Psychological State")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                HStack {
                    VStack(alignment: .leading) {
                        Text("Emotional State: \(chatViewModel.psychologicalState.emotionalState.displayName)")
                        Text("Communication Style: \(chatViewModel.psychologicalState.communicationStyle.displayName)")
                        Text("Personality Type: \(chatViewModel.psychologicalState.personalityType.displayName)")
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                    
                    Spacer()
                }
            }
            
            // Buddhist Factors
            VStack(alignment: .leading, spacing: 8) {
                Text("Buddhist Mental Factors")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 8) {
                    BuddhistFactorView(title: "Mindfulness", value: chatViewModel.buddhistFactors.mindfulness)
                    BuddhistFactorView(title: "Compassion", value: chatViewModel.buddhistFactors.compassion)
                    BuddhistFactorView(title: "Equanimity", value: chatViewModel.buddhistFactors.equanimity)
                    BuddhistFactorView(title: "Wisdom", value: chatViewModel.buddhistFactors.wisdom)
                }
            }
            
            // Recommendations
            if !chatViewModel.psychologicalInsights.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Recommendations")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    
                    ForEach(chatViewModel.psychologicalInsights.recommendations, id: \.self) { recommendation in
                        HStack(alignment: .top) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                                .font(.caption)
                            
                            Text(recommendation)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            Spacer()
                        }
                    }
                }
            }
            
            // Risk Assessment
            if chatViewModel.psychologicalInsights.riskAssessment.needsHumanReview {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        
                        Text("Professional Review Recommended")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.orange)
                    }
                    
                    Text(chatViewModel.psychologicalInsights.riskAssessment.reviewReason)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(8)
            }
        }
        .padding(.horizontal)
        .padding(.bottom)
    }
    
    // MARK: - Helper Views
    
    private var emotionIcon: String {
        switch chatViewModel.psychologicalState.emotionalState {
        case .anxious: return "exclamationmark.triangle"
        case .peaceful: return "leaf"
        case .empathetic: return "heart"
        case .mindful: return "brain.head.profile"
        case .neutral: return "circle"
        case .happy: return "face.smiling"
        case .sad: return "face.dashed"
        case .angry: return "face.dashed.fill"
        case .confused: return "questionmark.circle"
        case .calm: return "wind"
        }
    }
    
    private var toneIcon: String {
        switch chatViewModel.responseTone {
        case .calming: return "waveform"
        case .reflective: return "brain.head.profile"
        case .empathetic: return "heart.fill"
        case .mindful: return "leaf.fill"
        case .supportive: return "hand.raised.fill"
        }
    }
    
    private var riskIcon: String {
        switch chatViewModel.psychologicalInsights.riskAssessment.level {
        case .low: return "checkmark.circle"
        case .medium: return "exclamationmark.circle"
        case .high: return "exclamationmark.triangle"
        case .critical: return "xmark.circle"
        }
    }
}

// MARK: - Supporting Views

struct MetricBar: View {
    let title: String
    let value: Double
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
            
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Color(.systemGray5))
                    .frame(height: 6)
                    .cornerRadius(3)
                
                Rectangle()
                    .fill(color)
                    .frame(width: CGFloat(value) * 60, height: 6)
                    .cornerRadius(3)
            }
            .frame(width: 60)
            
            Text("\(Int(value * 100))%")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

struct BuddhistFactorView: View {
    let title: String
    let value: Double
    
    var body: some View {
        HStack {
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text("\(Int(value * 100))%")
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(value > 0.6 ? .green : value > 0.3 ? .orange : .red)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color(.systemGray6))
        .cornerRadius(6)
    }
}

// MARK: - Preview

struct PsychologicalInsightsView_Previews: PreviewProvider {
    static var previews: some View {
        PsychologicalInsightsView(chatViewModel: ChatViewModel())
            .previewLayout(.sizeThatFits)
            .padding()
    }
}
