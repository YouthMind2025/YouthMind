// Views/PostPreview.swift
//Compact preview card for posts in the feed
//Visually distinguishes posts by emotion type
//Displays key engagement metrics


import SwiftUI

struct PostPreview: View {
    let post: Post
    @ObservedObject var viewModel: FeedViewModel
    
    // Helper property to get emotion color from DesignConstants
    private var emotionColor: Color {
        switch post.emotion {
        case .angry: return DesignConstants.ColorPalette.angry
        case .sad: return DesignConstants.ColorPalette.sad
        case .happy: return DesignConstants.ColorPalette.happy
        case .conflicted: return DesignConstants.ColorPalette.conflicted
        case .surprised: return DesignConstants.ColorPalette.suprised
        case .fear: return DesignConstants.ColorPalette.fear
        }
    }

    var body: some View {
        VStack {
            HStack(alignment: .center) {
                // Heart + Resonance in capsule
                HStack(spacing: 4) {
                    Image(systemName: "heart")
                        .imageScale(.small)
                    
                    Text("\(post.resonance) Resonance")
                        .font(.system(size: 12)) // Smaller font size
                        .minimumScaleFactor(0.7) // Allows text to shrink if needed
                }
                .padding(.horizontal, 6) // Reduced padding
                .padding(.vertical, 4)
                .background(
                    Capsule()
                        .stroke(Color.gray, lineWidth: 1)
                )
                .lineLimit(1) // Force single line
                
                
                VStack(alignment: .leading) {
                    Image(systemName: "eye")
                    Text("\(post.view)")
                }
            }
            
            Spacer()
            
            Text(post.title)
                .font(.system(.title2, weight: .bold))
                .padding(.horizontal)
            
            Image(post.preImg)
                .resizable()
                .scaledToFit()
                .frame(height: 150)
                .padding()
            
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(emotionColor, lineWidth: 6)
        )
        .cornerRadius(12)
        .padding(.horizontal, 8)
    }
}

