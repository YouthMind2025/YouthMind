// Views/BottomTap.swift
// This would be the major view of app

import SwiftUI

struct BottomTap: View {
    var body: some View {
        TabView {
            HomePage()
                .tabItem {
                    Image(systemName: "house")
                    Text("Home")
                }
            
            Text("YouAI View")
                .tabItem {
                    Image(systemName: "heart")
                    Text("YouAI")
                }
                    
            Text("Post View")
                .tabItem {
                    Image(systemName: "plus.square")
                    Text("Post")
                }
            
            Text("Messages View")
                .tabItem {
                    Image(systemName: "message")
                    Text("Messages")
                }
                    
            Text("Profile View")
                .tabItem {
                    Image(systemName: "person")
                    Text("Me")
                }
        }
        .accentColor(.purple)
    }
}

