// PreviewProvider.swift
// This is for viewing the major views
//(At this stage BottomTap() is the HomePage with bottomtap, and this page can navigate to PostPreview and other page, like YouAI, Me, Message...)
// The device can be change on the bottom right in Xcode
// Recommended test device: iphone 16, iphone16 promax, Ipad11-inch, iphone SE


import SwiftUI

struct BottomTap_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Default appearance
            BottomTap()
                .previewDisplayName("Default Light")
            
            // Dark mode variant
            BottomTap()
                .preferredColorScheme(.dark)
                .previewDisplayName("Dark Mode")
            
            // Small device test
            BottomTap()
                .previewDevice("iphone SE")
                .previewDisplayName("iPhone SE")
            
            // Large device test
            BottomTap()
                .previewDevice("iPhone 15 Pro Max")
                .previewDisplayName("Pro Max")
            
            BottomTap()
                .previewDevice("iPad Pro 11-inch (5th generation)")
                .previewDisplayName("IPad")
            
        }
        
    }
}

