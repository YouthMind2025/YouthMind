import SwiftUI

struct EmotionSelectionView: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selectedEmotion: Emotion
    @Binding var title: String
    @Binding var content: String
    @Binding var topics: [String] // tags
    
    @State private var localSelectedEmotion: Emotion?
    
    // Emotion with image names
    private let emotions: [(emotion: Emotion, imageName: String, label: String)] = [
        (.sad, "Sad", "Sad"),
        (.fear, "Fear", "Fear"),
        (.surprised, "Surprised", "Surprised"),
        (.happy, "Happy", "Happy"),
        (.conflicted, "Conflicted", "Conflicted"),
        (.angry, "Angry", "Angry")
        
    ]
    
    init(selectedEmotion: Binding<Emotion>, title: Binding<String>, content: Binding<String>, topics: Binding<[String]>) {
        self._selectedEmotion = selectedEmotion
        self._title = title
        self._content = content
        self._topics = topics
        self._localSelectedEmotion = State(initialValue: nil)
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(
                    gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.10), DesignConstants.ColorPalette.brandPurple.opacity(0.05)]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // head
                    VStack(spacing: 16) {
                        // back button and title
                        HStack {
                            Button(action: {
                                dismiss()
                            }) {
                                Image(systemName: "chevron.left")
                                    .font(.title2)
                                    .foregroundColor(.primary)
                            }
                            
                            Spacer()
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        
                        // title
                        Text("What's your mood like?")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.black)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .frame(maxWidth: .infinity)
                            .background(Color.white)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(DesignConstants.ColorPalette.brandPurple.opacity(0.3), lineWidth: 1)
                            )
                            .padding(.horizontal, 20)
                    }
                    
                    // emotion select
                    VStack(spacing: 24) {
                        ForEach(0..<3) { row in
                            HStack(spacing: 20) {
                                ForEach(0..<2) { col in
                                    let index = row * 2 + col
                                    if index < emotions.count {
                                        let emotionData = emotions[index]
                                        EmotionCard(
                                            emotion: emotionData.emotion,
                                            imageName: emotionData.imageName,
                                            label: emotionData.label,
                                            isSelected: localSelectedEmotion == emotionData.emotion
                                        ) {
                                            localSelectedEmotion = emotionData.emotion
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(.top, 40)
                    
                    Spacer()
                    
                    // Next button
                    Button(action: {
                        if let selected = localSelectedEmotion {
                            selectedEmotion = selected
                            dismiss()
                        }
                    }) {
                        Text("Next")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 200)
                            .padding(.all, 16)
                            .background(
                                localSelectedEmotion == nil ?
                                LinearGradient(
                                    gradient: Gradient(colors: [Color.gray.opacity(0.6), Color.gray.opacity(0.4)]),
                                    startPoint: .top,
                                    endPoint: .bottom
                                ) :
                                LinearGradient(
                                    gradient: Gradient(colors: [
                                        DesignConstants.ColorPalette.brandPurple.opacity(0.55),
                                        DesignConstants.ColorPalette.brandPurple.opacity(0.35)
                                    ]),
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 32)
                    .disabled(localSelectedEmotion == nil)
                }
            }
            .navigationTitle("")
            .navigationBarHidden(true)
        }
    }
}

// MARK: - Emotion Card
struct EmotionCard: View {
    let emotion: Emotion
    let imageName: String
    let label: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                VStack(spacing: 3) {
                    Image(imageName)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 80, height: 80)
                    
                    Text(label)
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.black)
                }
                .frame(width: 120, height: 120)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color.yellow.opacity(0.3))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(isSelected ? emotion.color : Color.clear, lineWidth: 3)
                        )
                )
                .overlay(
                    // highlight
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(
                            isSelected ? 
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    emotion.color.opacity(0.8),
                                    emotion.color.opacity(0.4)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ) : 
                            LinearGradient(
                                gradient: Gradient(colors: [Color.clear, Color.clear]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: isSelected ? 2 : 0
                        )
                )
                .shadow(
                    color: isSelected ? emotion.color.opacity(0.3) : Color.clear,
                    radius: isSelected ? 8 : 0,
                    x: 0,
                    y: isSelected ? 4 : 0
                )
                
                // add correct emoji on top-right
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title2)
                        .foregroundColor(emotion.color)
                        .background(Color.white)
                        .clipShape(Circle())
                        .offset(x: 8, y: -8)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isSelected ? 1.05 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}

// MARK: - Preview
#Preview {
    EmotionSelectionView(
        selectedEmotion: .constant(.happy),
        title: .constant(""),
        content: .constant(""),
        topics: .constant([])
    )
} 
