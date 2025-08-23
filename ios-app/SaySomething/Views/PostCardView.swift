import SwiftUI

struct PostCardView: View {
    let content: String
    let emotionTag: String
    let timestamp: Date
    let authorName: String?
    let post: Post?
    
    init(content: String, emotionTag: String, timestamp: Date = Date(), authorName: String? = nil, post: Post? = nil) {
        self.content = content
        self.emotionTag = emotionTag
        self.timestamp = timestamp
        self.authorName = authorName
        self.post = post
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: DesignConstants.Layout.mediumSpacing) {
            // Header with author and timestamp
            HStack {
                if let author = authorName {
                    Text(author)
                        .font(DesignConstants.FontStyles.subtitle)
                        .foregroundColor(DesignConstants.ColorPalette.textPrimary)
                }
                Spacer()
                Text(timestamp, style: .relative)
                    .font(DesignConstants.FontStyles.caption)
                    .foregroundColor(DesignConstants.ColorPalette.textSecondary)
            }
            
            // Content
            Text(content)
                .font(DesignConstants.FontStyles.body)
                .foregroundColor(DesignConstants.ColorPalette.textPrimary)
                .multilineTextAlignment(.leading)
            
            // Emotion tag
            HStack {
                Image(systemName: emotionIcon)
                    .foregroundColor(emotionColor)
                    .font(.system(size: 16))
                
                Text(emotionTag.capitalized)
                    .font(DesignConstants.FontStyles.caption)
                    .foregroundColor(emotionColor)
                    .padding(.horizontal, DesignConstants.Layout.smallSpacing)
                    .padding(.vertical, 4)
                    .background(emotionColor.opacity(0.1))
                    .cornerRadius(DesignConstants.Layout.cornerRadius / 2)
                
                Spacer()
            }
        }
        .padding(DesignConstants.Layout.mediumSpacing)
        .background(
            RoundedRectangle(cornerRadius: DesignConstants.Layout.cornerRadius)
                .fill(Color.white)
                .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        )
        .onTapGesture {
            if let post = post {
                // Navigation will be handled by parent view
            }
        }
    }
    
    // MARK: - Helper Properties
    
    private var emotionColor: Color {
        switch emotionTag.lowercased() {
        case "joy", "happy":
            return DesignConstants.ColorPalette.happy
        case "sadness", "sad":
            return DesignConstants.ColorPalette.sad
        case "anger", "angry":
            return DesignConstants.ColorPalette.angry
        case "fear", "scared":
            return DesignConstants.ColorPalette.fear
        case "surprise", "surprised":
            return DesignConstants.ColorPalette.surprised
        case "conflicted":
            return DesignConstants.ColorPalette.conflicted
        default:
            return DesignConstants.ColorPalette.textSecondary
        }
    }
    
    private var emotionIcon: String {
        switch emotionTag.lowercased() {
        case "joy", "happy":
            return "face.smiling"
        case "sadness", "sad":
            return "face.dashed"
        case "anger", "angry":
            return "face.dashed.fill"
        case "fear", "scared":
            return "face.dashed"
        case "surprise", "surprised":
            return "face.dashed"
        case "conflicted":
            return "face.neutral"
        default:
            return "face.neutral"
        }
    }
}

// MARK: - Preview
struct PostCardView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: DesignConstants.Layout.largeSpacing) {
            PostCardView(
                content: "Today was an amazing day! I felt so grateful for all the wonderful people in my life.",
                emotionTag: "happy",
                authorName: "Sarah"
            )
            
            PostCardView(
                content: "Feeling a bit overwhelmed with work lately. Need to take a step back and breathe.",
                emotionTag: "sad",
                authorName: "Mike"
            )
            
            PostCardView(
                content: "Just had the most incredible surprise party! I'm still in shock!",
                emotionTag: "surprised",
                authorName: "Alex"
            )
        }
        .padding()
        .background(DesignConstants.ColorPalette.background)
    }
} 