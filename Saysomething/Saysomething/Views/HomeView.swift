//
//  HomeView.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Background gradient
                LinearGradient(gradient: Gradient(colors: [Color.blue.opacity(0.2), Color.purple.opacity(0.2)]), startPoint: .topLeading, endPoint: .bottomTrailing)
                    .ignoresSafeArea()
                VStack(spacing: 28) {
                    // App Title
                    Text(viewModel.title)
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                        .padding(.top, 16)
                        .shadow(radius: 2)
                    
                    // Navigation Buttons as Cards
                    ForEach(["Resonance", "Saved", "Shared", "Interactions"], id: ".") { label in
                        NavigationLink {
                            DetailView(content: label)
                        } label: {
                            HStack {
                                Image(systemName: iconName(for: label))
                                    .font(.title2)
                                    .foregroundColor(.white)
                                Text(label)
                                    .font(.headline)
                                    .foregroundColor(.white)
                                    .padding(.leading, 4)
                                Spacer()
                            }
                            .padding()
                            .background(LinearGradient(gradient: Gradient(colors: [Color.blue, Color.purple]), startPoint: .leading, endPoint: .trailing))
                            .cornerRadius(16)
                            .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 4)
                        }
                        .padding(.horizontal, 8)
                    }
                    // Drafts navigation card
                    NavigationLink {
                        DraftsView()
                    } label: {
                        HStack {
                            Image(systemName: "doc.text")
                                .font(.title2)
                                .foregroundColor(.white)
                            Text("Drafts")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.leading, 4)
                            Spacer()
                        }
                        .padding()
                        .background(LinearGradient(gradient: Gradient(colors: [Color.indigo, Color.purple]), startPoint: .leading, endPoint: .trailing))
                        .cornerRadius(16)
                        .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 4)
                    }
                    .padding(.horizontal, 8)
                    
                    // AI Chat navigation card
                    NavigationLink {
                        EnhancedChatView()
                    } label: {
                        HStack {
                            Image(systemName: "brain.head.profile")
                                .font(.title2)
                                .foregroundColor(.white)
                            Text("AI 心理健康助手")
                                .font(.headline)
                                .foregroundColor(.white)
                                .padding(.leading, 4)
                            Spacer()
                        }
                        .padding()
                        .background(LinearGradient(gradient: Gradient(colors: [Color.green, Color.teal]), startPoint: .leading, endPoint: .trailing))
                        .cornerRadius(16)
                        .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 4)
                    }
                    .padding(.horizontal, 8)
                    
                    if viewModel.isLoading {
                        ProgressView("Loading...")
                            .padding()
                    }
                    
                    // UUID label for testing
                    HStack {
                        Image(systemName: "person.crop.circle")
                            .foregroundColor(.gray)
                        Text("User UUID: \(UserIdentityManager.shared.userID)")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .padding(.top, 8)
                    .padding(.bottom, 12)
                }
                .padding(.vertical, 16)
            }
            .navigationTitle("Home Page")
        }
    }
    
    // Helper for icons
    private func iconName(for label: String) -> String {
        switch label {
        case "Resonance": return "waveform.path.ecg"
        case "Saved": return "bookmark.fill"
        case "Shared": return "square.and.arrow.up"
        case "Interactions": return "bubble.left.and.bubble.right.fill"
        default: return "circle"
        }
    }
}

struct DetailView: View {
    let content: String
    
    var body: some View {
        ZStack {
            LinearGradient(gradient: Gradient(colors: [Color.purple.opacity(0.1), Color.blue.opacity(0.1)]), startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
            VStack(spacing: 18) {
                // Content
                Text("detail：\(content)")
                    .font(.title)
                    .fontWeight(.semibold)
                    .padding(.top, 24)
                
                // DESCRIPTION
                Text("This is \(content) page")
                    .foregroundColor(.secondary)
                    .font(.body)
                Spacer()
            }
            .padding()
        }
        .navigationTitle("detail")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
