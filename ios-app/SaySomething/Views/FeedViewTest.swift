//
//  FeedViewTest.swift
//  SaySomething
//
//  Created by AI Assistant on 1/15/25.
//

import SwiftUI

/// Test view to verify infinite scroll implementation
/// This view can be used for testing the feed functionality
struct FeedViewTest: View {
    @StateObject private var viewModel = FeedViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Test controls
                VStack(spacing: 12) {
                    Text("Feed Infinite Scroll Test")
                        .font(.headline)
                        .padding()
                    
                    HStack(spacing: 16) {
                        Button("Load Next Block") {
                            Task {
                                await viewModel.loadNextBlock()
                            }
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isLoadingMore || !viewModel.hasMoreBlocks)
                        
                        Button("Reset") {
                            viewModel.resetToInitialState()
                        }
                        .buttonStyle(.bordered)
                        
                        Button("Notify Scroll") {
                            Task {
                                await viewModel.notifyScroll()
                            }
                        }
                        .buttonStyle(.bordered)
                        
                        Button("Refresh Feed") {
                            Task {
                                await viewModel.refreshFeed()
                            }
                        }
                        .buttonStyle(.bordered)
                        .disabled(viewModel.isLoading)
                    }
                    .padding(.horizontal)
                    
                    // Status indicators
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Status:")
                            .font(.caption)
                            .fontWeight(.semibold)
                        
                        HStack {
                            Text("Loading: \(viewModel.isLoading ? "Yes" : "No")")
                            Spacer()
                            Text("Loading More: \(viewModel.isLoadingMore ? "Yes" : "No")")
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                        
                        HStack {
                            Text("Has More: \(viewModel.hasMoreBlocks ? "Yes" : "No")")
                            Spacer()
                            Text("Posts: \(viewModel.posts.count)")
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                        
                        if let error = viewModel.error {
                            Text("Error: \(error.localizedDescription)")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                .background(Color(.systemGray6))
                
                // Feed view
                FeedView()
                    .environmentObject(viewModel)
            }
        }
    }
}

#Preview {
    FeedViewTest()
}
