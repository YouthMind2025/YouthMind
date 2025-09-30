//
//  PersonalityProfileView.swift
//  SaySomething
//
//  Created by AI Assistant on 1/15/25.
//

import SwiftUI

// MARK: - Personality Profile View (Youseef's Integration)

struct PersonalityProfileView: View {
    @ObservedObject var chatViewModel: ChatViewModel
    @State private var selectedTab = 0
    
    var body: some View {
        VStack(spacing: 16) {
            // Header
            HStack {
                Image(systemName: "person.crop.circle.fill")
                    .foregroundColor(.purple)
                    .font(.title2)
                
                Text("Personality Profile")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                Button(action: {
                    chatViewModel.togglePsychologicalInsights()
                }) {
                    Image(systemName: chatViewModel.showPsychologicalInsights ? "eye.fill" : "eye")
                        .foregroundColor(.purple)
                }
            }
            .padding(.horizontal)
            
            // Tab Selector
            Picker("Profile Sections", selection: $selectedTab) {
                Text("Overview").tag(0)
                Text("Dual Personality").tag(1)
                Text("Buddhist Factors").tag(2)
                Text("Insights").tag(3)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal)
            
            // Content based on selected tab
            TabView(selection: $selectedTab) {
                overviewTab
                    .tag(0)
                
                dualPersonalityTab
                    .tag(1)
                
                buddhistFactorsTab
                    .tag(2)
                
                insightsTab
                    .tag(3)
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            .frame(height: 300)
        }
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }
    
    // MARK: - Overview Tab
    private var overviewTab: some View {
        VStack(spacing: 16) {
            // Primary Personality Type
            VStack(spacing: 8) {
                Circle()
                    .fill(Color.purple)
                    .frame(width: 60, height: 60)
                    .overlay(
                        Image(systemName: personalityIcon)
                            .foregroundColor(.white)
                            .font(.system(size: 24))
                    )
                
                Text(chatViewModel.psychologicalState.personalityType.displayName)
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text(chatViewModel.psychologicalState.communicationStyle.displayName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Current Emotional State
            HStack {
                VStack {
                    Circle()
                        .fill(Color(chatViewModel.psychologicalState.emotionalState.color))
                        .frame(width: 40, height: 40)
                        .overlay(
                            Image(systemName: emotionIcon)
                                .foregroundColor(.white)
                                .font(.system(size: 16))
                        )
                    
                    Text("Current State")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text(chatViewModel.psychologicalState.emotionalState.displayName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Text("Last updated: \(formatTime(chatViewModel.psychologicalState.timestamp))")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            // Quick Stats
            HStack(spacing: 20) {
                StatView(title: "Stress", value: chatViewModel.psychologicalState.stress, color: .orange)
                StatView(title: "Mindfulness", value: chatViewModel.psychologicalState.mindfulness, color: .blue)
                StatView(title: "Compassion", value: chatViewModel.psychologicalState.compassion, color: .pink)
            }
        }
        .padding()
    }
    
    // MARK: - Dual Personality Tab
    private var dualPersonalityTab: some View {
        VStack(spacing: 16) {
            Text("Dual Personality Analysis")
                .font(.headline)
                .fontWeight(.semibold)
            
            // Active Personality (μ_post)
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "person.fill")
                        .foregroundColor(.blue)
                    
                    Text("Active Personality")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                Text("Weighted mean based on recent interactions")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                PersonalityTypeCard(
                    type: chatViewModel.psychologicalState.personalityType,
                    style: chatViewModel.psychologicalState.communicationStyle,
                    confidence: 0.85
                )
            }
            
            // Subconscious Personality (V_shadow)
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "brain.head.profile")
                        .foregroundColor(.purple)
                    
                    Text("Subconscious Personality")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                Text("Top-K resonance patterns from psychological analysis")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                PersonalityTypeCard(
                    type: determineSubconsciousType(),
                    style: determineSubconsciousStyle(),
                    confidence: 0.72
                )
            }
        }
        .padding()
    }
    
    // MARK: - Buddhist Factors Tab
    private var buddhistFactorsTab: some View {
        VStack(spacing: 16) {
            Text("Buddhist Mental Factors")
                .font(.headline)
                .fontWeight(.semibold)
            
            ScrollView {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                    BuddhistFactorCard(title: "Mindfulness", value: chatViewModel.buddhistFactors.mindfulness, description: "Present-moment awareness")
                    BuddhistFactorCard(title: "Compassion", value: chatViewModel.buddhistFactors.compassion, description: "Loving-kindness")
                    BuddhistFactorCard(title: "Equanimity", value: chatViewModel.buddhistFactors.equanimity, description: "Mental balance")
                    BuddhistFactorCard(title: "Wisdom", value: chatViewModel.buddhistFactors.wisdom, description: "Insight and understanding")
                    BuddhistFactorCard(title: "Vigor", value: chatViewModel.buddhistFactors.vigor, description: "Energy and enthusiasm")
                    BuddhistFactorCard(title: "Faith", value: chatViewModel.buddhistFactors.faith, description: "Confidence and trust")
                }
            }
        }
        .padding()
    }
    
    // MARK: - Insights Tab
    private var insightsTab: some View {
        VStack(spacing: 16) {
            Text("Psychological Insights")
                .font(.headline)
                .fontWeight(.semibold)
            
            ScrollView {
                VStack(spacing: 12) {
                    // Overall Assessment
                    InsightCard(
                        title: "Overall Assessment",
                        content: chatViewModel.psychologicalInsights.overallAssessment,
                        icon: "doc.text",
                        color: .blue
                    )
                    
                    // Recommendations
                    if !chatViewModel.psychologicalInsights.recommendations.isEmpty {
                        InsightCard(
                            title: "Recommendations",
                            content: chatViewModel.psychologicalInsights.recommendations.joined(separator: "\n• "),
                            icon: "lightbulb",
                            color: .yellow
                        )
                    }
                    
                    // Risk Assessment
                    if chatViewModel.psychologicalInsights.riskAssessment.needsHumanReview {
                        InsightCard(
                            title: "Risk Assessment",
                            content: chatViewModel.psychologicalInsights.riskAssessment.reviewReason,
                            icon: "exclamationmark.triangle",
                            color: .orange
                        )
                    }
                }
            }
        }
        .padding()
    }
    
    // MARK: - Helper Methods
    
    private func determineSubconsciousType() -> PersonalityType {
        // This would be calculated from Top-K resonance patterns
        // For now, return a variation of the active type
        switch chatViewModel.psychologicalState.personalityType {
        case .empathetic: return .reflective
        case .reflective: return .mindful
        case .mindful: return .supportive
        case .supportive: return .empathetic
        case .professional: return .casual
        case .casual: return .professional
        }
    }
    
    private func determineSubconsciousStyle() -> CommunicationStyle {
        // This would be calculated from Top-K resonance patterns
        switch chatViewModel.psychologicalState.communicationStyle {
        case .gentle: return .warm
        case .balanced: return .supportive
        case .warm: return .gentle
        case .supportive: return .balanced
        case .direct: return .casual
        case .casual: return .direct
        case .formal: return .professional
        }
    }
    
    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    private var personalityIcon: String {
        switch chatViewModel.psychologicalState.personalityType {
        case .empathetic: return "heart.fill"
        case .reflective: return "brain.head.profile"
        case .mindful: return "leaf.fill"
        case .supportive: return "hand.raised.fill"
        case .professional: return "briefcase.fill"
        case .casual: return "person.fill"
        }
    }
    
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
}

// MARK: - Supporting Views

struct StatView: View {
    let title: String
    let value: Double
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
            
            ZStack {
                Circle()
                    .stroke(Color(.systemGray5), lineWidth: 4)
                    .frame(width: 40, height: 40)
                
                Circle()
                    .trim(from: 0, to: CGFloat(value))
                    .stroke(color, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .frame(width: 40, height: 40)
                    .rotationEffect(.degrees(-90))
                
                Text("\(Int(value * 100))")
                    .font(.caption2)
                    .fontWeight(.bold)
            }
        }
    }
}

struct PersonalityTypeCard: View {
    let type: PersonalityType
    let style: CommunicationStyle
    let confidence: Double
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(type.displayName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                Text("\(Int(confidence * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Text(style.displayName)
                .font(.caption)
                .foregroundColor(.secondary)
            
            ProgressView(value: confidence)
                .progressViewStyle(LinearProgressViewStyle(tint: .purple))
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

struct BuddhistFactorCard: View {
    let title: String
    let value: Double
    let description: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                Text("\(Int(value * 100))%")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(value > 0.6 ? .green : value > 0.3 ? .orange : .red)
            }
            
            Text(description)
                .font(.caption2)
                .foregroundColor(.secondary)
            
            ProgressView(value: value)
                .progressViewStyle(LinearProgressViewStyle(tint: value > 0.6 ? .green : value > 0.3 ? .orange : .red))
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

struct InsightCard: View {
    let title: String
    let content: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            
            Text(content)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// MARK: - Preview

struct PersonalityProfileView_Previews: PreviewProvider {
    static var previews: some View {
        PersonalityProfileView(chatViewModel: ChatViewModel())
            .previewLayout(.sizeThatFits)
            .padding()
    }
}
