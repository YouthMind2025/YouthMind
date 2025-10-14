import SwiftUI

struct ProfileView: View {
    var body: some View {
        NavigationView {
            List {
                // Profile Header Section
                Section {
                    VStack(spacing: 16) {
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 100, height: 100)
                            .foregroundColor(DesignConstants.ColorPalette.brandPurple)
                        Text("Me")
                            .font(.title2)
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                }
                
                // Settings Section
                Section(header: Text("Settings")) {
                    NavigationLink(destination: Text("Account Settings")) {
                        Label("Account", systemImage: "person.circle")
                    }
                    
                    NavigationLink(destination: Text("Privacy Settings")) {
                        Label("Privacy", systemImage: "lock.circle")
                    }
                    
                    NavigationLink(destination: Text("Notifications")) {
                        Label("Notifications", systemImage: "bell.circle")
                    }
                }
                
                // Debug Section (Debug builds only)
                #if DEBUG
                Section(header: Text("Debug")) {
                    NavigationLink(destination: RecommendationDebugView()) {
                        Label("Recommendation Debug", systemImage: "gearshape.2.fill")
                    }
                }
                #endif
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}


