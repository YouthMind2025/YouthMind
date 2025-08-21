// Views/BottomTap.swift
// This would be the major view of app

import SwiftUI

struct BottomTap: View {
    @State private var selectedTab: TabIdentifier = .home
    @State private var isPresentingPostSheet: Bool = false
    @StateObject private var tabBarState = TabBarState()
    @StateObject private var feedViewModel = FeedViewModel()

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                Group {
                    switch selectedTab {
                    case .home:
                        HomePage()
                            .environmentObject(tabBarState)
                            .environmentObject(feedViewModel)
                    case .youAI:
                        ChatView()
                    case .messages:
                        Text("Messages View")
                    case .profile:
                        ProfileView()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea(edges: .bottom)

                if !tabBarState.isHidden {
                    CustomTabBar(selectedTab: $selectedTab) {
                        isPresentingPostSheet = true
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .navigationDestination(isPresented: $isPresentingPostSheet) {
                DraftsView()
                    .environmentObject(feedViewModel)
            }
        }
        
    }
}

