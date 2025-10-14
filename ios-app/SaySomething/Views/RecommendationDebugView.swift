//
//  RecommendationDebugView.swift
//  SaySomething
//
//  Created by Assistant on 01/15/2025.
//

import SwiftUI

#if DEBUG
struct RecommendationDebugView: View {
    @StateObject private var viewModel = DebugViewModel()
    @State private var showResetConfirmation = false
    
    var body: some View {
        NavigationView {
            List {
                // R-Vector Section
                Section(header: Text("R-Vector (Recommendation Vector)")) {
                    if viewModel.isLoading {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    } else {
                        // Top 3 Emotions
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Top 3 Emotions")
                                .font(.headline)
                            
                            ForEach(Array(viewModel.topEmotions.enumerated()), id: \.offset) { index, emotion in
                                HStack {
                                    Text("\(index + 1).")
                                        .foregroundColor(.secondary)
                                    Text(emotion.capitalized)
                                        .fontWeight(.medium)
                                    Spacer()
                                    EmotionBadge(emotion: emotion)
                                }
                            }
                            
                            if viewModel.topEmotions.isEmpty {
                                Text("No data available")
                                    .foregroundColor(.secondary)
                                    .italic()
                            }
                        }
                        .padding(.vertical, 4)
                        
                        // R-Vector Values
                        VStack(alignment: .leading, spacing: 8) {
                            Text("R-Vector Values")
                                .font(.headline)
                            
                            if !viewModel.rVector.isEmpty {
                                ForEach(Array(viewModel.rVector.enumerated()), id: \.offset) { index, value in
                                    HStack {
                                        Text("Dimension \(index + 1)")
                                            .foregroundColor(.secondary)
                                        Spacer()
                                        Text(String(format: "%.4f", value))
                                            .font(.system(.body, design: .monospaced))
                                    }
                                }
                            } else {
                                Text("No data available")
                                    .foregroundColor(.secondary)
                                    .italic()
                            }
                        }
                        .padding(.vertical, 4)
                        
                        // Interaction Count
                        HStack {
                            Text("Interaction Count")
                            Spacer()
                            Text("\(viewModel.interactionCount)")
                                .font(.system(.body, design: .monospaced))
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // Feed State Section
                Section(header: Text("Feed State")) {
                    HStack {
                        Text("Current Block")
                        Spacer()
                        Text("\(viewModel.currentBlock)")
                            .font(.system(.body, design: .monospaced))
                            .foregroundColor(.blue)
                    }
                    
                    HStack {
                        Text("Next Block Available")
                        Spacer()
                        Image(systemName: viewModel.isNextBlockAvailable ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(viewModel.isNextBlockAvailable ? .green : .orange)
                    }
                    
                    HStack {
                        Text("Total Blocks Consumed")
                        Spacer()
                        Text("\(viewModel.totalBlocksConsumed)")
                            .font(.system(.body, design: .monospaced))
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Text("Engagement Score")
                        Spacer()
                        Text(String(format: "%.2f", viewModel.engagementScore))
                            .font(.system(.body, design: .monospaced))
                            .foregroundColor(.purple)
                    }
                }
                
                // Actions Section
                Section(header: Text("Actions")) {
                    Button(action: {
                        Task {
                            await viewModel.refresh()
                        }
                    }) {
                        Label("Refresh Data", systemImage: "arrow.clockwise")
                    }
                    .disabled(viewModel.isLoading)
                    
                    Button(role: .destructive, action: {
                        showResetConfirmation = true
                    }) {
                        Label("Reset R-Vector", systemImage: "arrow.counterclockwise")
                    }
                    .disabled(viewModel.isLoading)
                }
                
                // Info Section
                if let lastRefresh = viewModel.lastRefresh {
                    Section(header: Text("Info")) {
                        HStack {
                            Text("Last Refresh")
                            Spacer()
                            Text(lastRefresh, style: .time)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // Error Display
                if let error = viewModel.error {
                    Section {
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Error", systemImage: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(error.localizedDescription)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // Success Message
                if viewModel.resetSuccess {
                    Section {
                        Label("R-Vector reset successfully!", systemImage: "checkmark.circle.fill")
                            .foregroundColor(.green)
                    }
                }
            }
            .navigationTitle("Debug: Recommendations")
            .navigationBarTitleDisplayMode(.inline)
            .refreshable {
                await viewModel.refresh()
            }
            .task {
                await viewModel.refresh()
            }
            .alert("Reset R-Vector?", isPresented: $showResetConfirmation) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    Task {
                        await viewModel.resetRVector(reason: "Manual debug reset")
                    }
                }
            } message: {
                Text("This will reset your recommendation vector to default values. Your personalized feed will rebuild based on new interactions.")
            }
        }
    }
}

// MARK: - Helper Views

struct EmotionBadge: View {
    let emotion: String
    
    private var emotionColor: Color {
        switch emotion.lowercased() {
        case "happy": return .yellow
        case "sad": return .blue
        case "angry": return .red
        case "fear": return .purple
        case "surprised": return .cyan
        case "conflicted": return .gray
        default: return .secondary
        }
    }
    
    var body: some View {
        Text(emotion.capitalized)
            .font(.caption)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(emotionColor.opacity(0.2))
            .foregroundColor(emotionColor)
            .cornerRadius(8)
    }
}

#Preview {
    RecommendationDebugView()
}
#endif
