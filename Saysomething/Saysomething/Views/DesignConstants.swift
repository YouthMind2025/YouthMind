// Views/DesignConstants
//Centralized design system for consistent UI/UX across the app
//Provides reusable design tokens for colors, typography, and layout
                                
import SwiftUI

// MARK: - Design Constants
struct DesignConstants {
    // Color palette
    struct ColorPalette {
        static let primary = Color(hex: "#1A73E8")
        static let secondary = Color(hex: "#34A853")
        static let background = Color(hex: "#F8F9FA")
        static let textPrimary = Color(hex: "#202124")
        static let textSecondary = Color(hex: "#5F6368")
        
        // Emotion tag colors
        static let angry = Color(hex: "#fe7877")
        static let sad = Color(hex: "#7fbafe")
        static let happy = Color(hex: "#ffeb68")
        static let conflicted = Color(hex: "#929584")
        static let suprised = Color(hex: "#7ed7c3")
        static let fear = Color(hex: "#b68ef1")
       
        
    }
    
    // Font styles
    struct FontStyles {
        static let title = Font.system(.title).weight(.bold)
        static let subtitle = Font.system(.title2).weight(.semibold)
        static let body = Font.system(.body)
        static let caption = Font.system(.caption)
    }
    
    // Spacing and layout
    struct Layout {
        static let smallSpacing: CGFloat = 8
        static let mediumSpacing: CGFloat = 16
        static let largeSpacing: CGFloat = 24
        static let cornerRadius: CGFloat = 8
        static let padding: CGFloat = 16
    }
}

// MARK: - Color Extensions
extension Color {
    // Convenience initializer for hex colors
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - Font Extensions
extension Font {
    static func customTitle() -> Font {
        return DesignConstants.FontStyles.title
    }
    
    static func customBody() -> Font {
        return DesignConstants.FontStyles.body
    }
    
    static func customCaption() -> Font {
        return DesignConstants.FontStyles.caption
    }
}

