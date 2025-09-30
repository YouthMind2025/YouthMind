import SwiftUI
import UIKit

enum TabIdentifier: Hashable {
    case home
    case youAI
    case messages
    case profile
}

struct CustomTabBar: View {
    @Binding var selectedTab: TabIdentifier
    var onPlusTapped: () -> Void

    private let barHeight: CGFloat = 64
    private let barCornerRadius: CGFloat = 22
    private let plusDiameter: CGFloat = 58

    @Namespace private var highlightNamespace
    @State private var isPlusPressed: Bool = false
    @State private var rippleScalePrimary: CGFloat = 0.001
    @State private var rippleOpacityPrimary: Double = 0.0
    @State private var rippleScaleSecondary: CGFloat = 0.001
    @State private var rippleOpacitySecondary: Double = 0.0

    var body: some View {
        ZStack {
            ConcaveTabBarShape(
                cornerRadius: barCornerRadius,
                notchRadius: plusDiameter / 2 + 12,
                notchDepth: 16
            )
            .fill(Color.white)
            .shadow(color: Color.black.opacity(0.08), radius: 12, x: 0, y: 4)
            .frame(height: barHeight)
            .padding(.horizontal, 16)
            // Soft inner rim around the notch for a deeper, concave look
            .overlay(
                ConcaveTabBarShape(
                    cornerRadius: barCornerRadius,
                    notchRadius: plusDiameter / 2 + 12,
                    notchDepth: 16
                )
                .stroke(LinearGradient(
                    colors: [Color.black.opacity(0.06), Color.clear],
                    startPoint: .top,
                    endPoint: .bottom
                ), lineWidth: 0.8)
                .padding(.horizontal, 16)
            )

            // Tabs row + flowing highlight background (animated)
            HStack(alignment: .center) {
                tabButton(systemImage: "house", title: "Home", tab: .home)
                Spacer(minLength: 0)
                tabButton(systemImage: "heart", title: "You AI", tab: .youAI)
                Spacer(minLength: 0)
                Spacer(minLength: plusDiameter) // space for the center button
                Spacer(minLength: 0)
                tabButton(systemImage: "message", title: "Message", tab: .messages)
                Spacer(minLength: 0)
                tabButton(systemImage: "person", title: "Me", tab: .profile)
            }
            .padding(.horizontal, 32)
            // Animated, flowing pill behind the selected tab
            .backgroundPreferenceValue(TabItemBoundsPreferenceKey.self) { preferences in
                GeometryReader { geo in
                    if let anchor = preferences[selectedTab] {
                        let rect = geo[anchor]
                        // Use TimelineView to create a subtle flowing gradient inside the pill
                        TimelineView(.animation) { context in
                            let t = context.date.timeIntervalSinceReferenceDate
                            let phase = (sin(t * 0.8) + 1) / 2 // 0...1
                            let start = UnitPoint(x: 0.2 + 0.6 * phase, y: 0.0)
                            let end = UnitPoint(x: 1 - (0.2 + 0.6 * phase), y: 1.0)
                            ZStack {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(
                                        LinearGradient(
                                            colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.22), DesignConstants.ColorPalette.brandPurple.opacity(0.18)],
                                            startPoint: start,
                                            endPoint: end
                                        )
                                    )
                                    .frame(width: rect.width + 22, height: 44)
                                    .position(x: rect.midX, y: rect.midY)
                                    .animation(.spring(response: 0.45, dampingFraction: 0.85), value: selectedTab)
                                    .matchedGeometryEffect(id: "tabHighlight", in: highlightNamespace)

                                // Soft glow for a liquid feel
                                RoundedRectangle(cornerRadius: 18)
                                    .fill(
                                        LinearGradient(
                                            colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.14), DesignConstants.ColorPalette.brandPurple.opacity(0.08)],
                                            startPoint: end,
                                            endPoint: start
                                        )
                                    )
                                    .blur(radius: 6)
                                    .frame(width: rect.width + 28, height: 52)
                                    .position(x: rect.midX, y: rect.midY)
                                    .animation(.spring(response: 0.45, dampingFraction: 0.85), value: selectedTab)
                            }
                        }
                    }
                }
            }
            // Ripple micro-interactions around the selected tab icon
            .overlayPreferenceValue(TabItemBoundsPreferenceKey.self) { preferences in
                GeometryReader { geo in
                    if let anchor = preferences[selectedTab] {
                        let rect = geo[anchor]
                        ZStack {
                            Circle()
                                .stroke(
                                    LinearGradient(
                                        colors: [
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.85),
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.55)
                                        ],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ), lineWidth: 1.6
                                )
                                .frame(width: 8 + (rippleScalePrimary * 48), height: 8 + (rippleScalePrimary * 48))
                                .position(x: rect.midX, y: rect.midY - 8)
                                .opacity(rippleOpacityPrimary)
                                .blendMode(.plusLighter)

                            Circle()
                                .stroke(
                                    LinearGradient(
                                        colors: [
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.5),
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.35)
                                        ],
                                        startPoint: .bottomTrailing,
                                        endPoint: .topLeading
                                    ), lineWidth: 1.0
                                )
                                .frame(width: 8 + (rippleScaleSecondary * 64), height: 8 + (rippleScaleSecondary * 64))
                                .position(x: rect.midX, y: rect.midY - 8)
                                .opacity(rippleOpacitySecondary * 0.9)
                                .blur(radius: 0.8)
                                .blendMode(.plusLighter)
                        }
                        .allowsHitTesting(false)
                    }
                }
            }

            VStack {
                TimelineView(.animation) { context in
                    let t = context.date.timeIntervalSinceReferenceDate
                    let bob: CGFloat = sin(t * 1.2) * 3 // -3...+3 subtle vertical float
                    let pulse: CGFloat = (cos(t * 1.2) + 1) / 2 // 0...1

                    Button(action: {
                        let generator = UIImpactFeedbackGenerator(style: .soft)
                        generator.impactOccurred()
                        withAnimation(.spring(response: 0.28, dampingFraction: 0.65)) { isPlusPressed = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.7)) { isPlusPressed = false }
                        }
                        onPlusTapped()
                    }) {
                        ZStack {
                            // Ambient aura bloom (purple-forward, fairy-like)
                            Circle()
                                .fill(
                                    RadialGradient(
                                        gradient: Gradient(colors: [
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.28),
                                            Color.clear
                                        ]),
                                        center: .center,
                                        startRadius: 0,
                                        endRadius: plusDiameter
                                    )
                                )
                                .scaleEffect(1.34)
                                .blur(radius: 10)
                                .blendMode(.plusLighter)

                            // Core button (purple-leaning gradient)
                            Circle()
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(stops: [
                                            .init(color: DesignConstants.ColorPalette.brandPurple, location: 0.0),
                                             .init(color: DesignConstants.ColorPalette.brandPurple.opacity(0.92), location: 0.65),
                                             .init(color: DesignConstants.ColorPalette.brandPurple.opacity(0.80), location: 1.0)
                                        ]),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )

                            // Shimmer ring (slow rotation)
                            Circle()
                                .stroke(
                                    AngularGradient(
                                        gradient: Gradient(colors: [
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.95),
                                            DesignConstants.ColorPalette.brandPurple.opacity(0.6),
                        DesignConstants.ColorPalette.brandPurple.opacity(0.95)
                                        ]),
                                        center: .center
                                    ), lineWidth: 1.2
                                )
                                .rotationEffect(.degrees(t * 40))
                                .opacity(0.9)
                                .blendMode(.plusLighter)

                            // Inner glass ring
                            Circle()
                                .stroke(Color.white.opacity(0.28), lineWidth: 0.7)
                                .padding(1.5)

                            // Specular highlight for glassy floating feel
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [Color.white.opacity(0.28), Color.white.opacity(0.0)],
                                        startPoint: .top,
                                        endPoint: .center
                                    )
                                )
                                .padding(6)

                            Image(systemName: "plus")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.white)
                                .shadow(color: Color.black.opacity(0.2), radius: 2.5, x: 0, y: 1)
                                .shadow(color: Color.white.opacity(0.35), radius: 4, x: 0, y: 0)
                        }
                    }
                    .frame(width: plusDiameter, height: plusDiameter)
                    .scaleEffect(isPlusPressed ? 0.9 : 1.0)
                    // Lift shadows
                    .shadow(color: Color.white.opacity(0.55 + 0.15 * Double(pulse)), radius: 14 + 4 * pulse, x: 0, y: 6)
                    .shadow(color: DesignConstants.ColorPalette.brandPurple.opacity(0.35), radius: 10, x: 0, y: 8)
                    // Ground shadow to imply floating above the notch
                    .overlay(alignment: .bottom) {
                        Ellipse()
                            .fill(
                                RadialGradient(
                                    gradient: Gradient(colors: [Color.black.opacity(0.20), Color.black.opacity(0.04), Color.clear]),
                                    center: .center,
                                    startRadius: 0,
                                    endRadius: plusDiameter
                                )
                            )
                            .frame(width: plusDiameter * (1.58 + 0.08 * pulse), height: plusDiameter * (0.58 + 0.05 * pulse))
                            .blur(radius: 8 + 2 * pulse)
                            .offset(y: plusDiameter * 0.42)
                            .allowsHitTesting(false)
                    }
                    .offset(y: bob)
                }
            }
            .offset(y: -barHeight / 2)
        }
            .padding(.bottom, 8)
        // Trigger ripples when the selected tab changes
        .onChange(of: selectedTab) { _ in
            rippleScalePrimary = 0.001
            rippleOpacityPrimary = 0.85
            rippleScaleSecondary = 0.001
            rippleOpacitySecondary = 0.65
            withAnimation(.easeOut(duration: 0.72)) {
                rippleScalePrimary = 1.6
                rippleOpacityPrimary = 0.0
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.10) {
                withAnimation(.easeOut(duration: 0.9)) {
                    rippleScaleSecondary = 1.9
                    rippleOpacitySecondary = 0.0
                }
            }
        }
    }

    private func tabButton(systemImage: String, title: String, tab: TabIdentifier) -> some View {
        Button {
            guard selectedTab != tab else { return }
            let generator = UIImpactFeedbackGenerator(style: .soft)
            generator.impactOccurred()
            withAnimation(.spring(response: 0.6, dampingFraction: 0.78)) {
                selectedTab = tab
            }
        } label: {
            VStack(spacing: 4) {
                ZStack {
                    if selectedTab == tab {
                        Circle()
                            .fill(
                                RadialGradient(
                                    gradient: Gradient(colors: [
                                        DesignConstants.ColorPalette.brandPurple.opacity(0.22),
                                        Color.clear
                                    ]),
                                    center: .center,
                                    startRadius: 0,
                                    endRadius: 22
                                )
                            )
                            .frame(width: 28, height: 28)
                            .offset(y: -2)
                            .blur(radius: 4)
                    }
                    Image(systemName: systemImage)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(selectedTab == tab ? DesignConstants.ColorPalette.brandPurple : Color.gray.opacity(0.7))
                        .scaleEffect(selectedTab == tab ? 1.12 : 0.96)
                        .offset(y: selectedTab == tab ? -2 : 0)
                        .rotation3DEffect(
                            .degrees(selectedTab == tab ? 8 : 0),
                            axis: (x: 1, y: 0, z: 0),
                            anchor: .bottom,
                            perspective: 0.6
                        )
                        .shadow(color: selectedTab == tab ? DesignConstants.ColorPalette.brandPurple.opacity(0.3) : .clear, radius: 4, x: 0, y: 2)
                        .animation(.spring(response: 0.6, dampingFraction: 0.78), value: selectedTab)
                }
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(selectedTab == tab ? DesignConstants.ColorPalette.brandPurple : Color.gray.opacity(0.7))
                    .scaleEffect(selectedTab == tab ? 1.06 : 1.0)
                    .animation(.spring(response: 0.6, dampingFraction: 0.78), value: selectedTab)
            }
            .padding(.horizontal, 6)
            .padding(.vertical, 6)
        }
        .buttonStyle(.plain)
        // Expose bounds for the animated background pill using anchor preference
        .anchorPreference(key: TabItemBoundsPreferenceKey.self, value: .bounds) { [tab: $0] }
    }

    // Intentionally avoiding direct UIKit safe-area access to keep this
    // view portable; outer containers should handle additional padding.
}

// MARK: - Concave background shape with center notch
struct ConcaveTabBarShape: Shape {
    let cornerRadius: CGFloat
    let notchRadius: CGFloat
    let notchDepth: CGFloat

    func path(in rect: CGRect) -> Path {
        let width = rect.width
        let height = rect.height

        let left = rect.minX
        let right = rect.maxX
        let top = rect.minY
        let bottom = rect.maxY

        let centerX = rect.midX

        var path = Path()

        // Start at bottom-left corner
        path.move(to: CGPoint(x: left + cornerRadius, y: bottom))
        // Bottom edge to bottom-right with radius
        path.addLine(to: CGPoint(x: right - cornerRadius, y: bottom))
        path.addArc(center: CGPoint(x: right - cornerRadius, y: bottom - cornerRadius), radius: cornerRadius, startAngle: .degrees(90), endAngle: .degrees(0), clockwise: true)
        // Right edge up
        path.addLine(to: CGPoint(x: right, y: top + cornerRadius))
        path.addArc(center: CGPoint(x: right - cornerRadius, y: top + cornerRadius), radius: cornerRadius, startAngle: .degrees(0), endAngle: .degrees(-90), clockwise: true)

        // Top edge to notch start
        let notchStartX = centerX + notchRadius
        path.addLine(to: CGPoint(x: notchStartX, y: top))

        // Notch: concave arc downward
        let controlOffset: CGFloat = notchDepth
        let notchEndX = centerX - notchRadius
        path.addCurve(
            to: CGPoint(x: centerX, y: top + controlOffset),
            control1: CGPoint(x: notchStartX - controlOffset, y: top),
            control2: CGPoint(x: centerX + controlOffset, y: top + controlOffset)
        )
        path.addCurve(
            to: CGPoint(x: notchEndX, y: top),
            control1: CGPoint(x: centerX - controlOffset, y: top + controlOffset),
            control2: CGPoint(x: notchEndX + controlOffset, y: top)
        )

        // Continue to top-left
        path.addLine(to: CGPoint(x: left + cornerRadius, y: top))
        path.addArc(center: CGPoint(x: left + cornerRadius, y: top + cornerRadius), radius: cornerRadius, startAngle: .degrees(-90), endAngle: .degrees(-180), clockwise: true)
        // Left edge down
        path.addLine(to: CGPoint(x: left, y: bottom - cornerRadius))
        path.addArc(center: CGPoint(x: left + cornerRadius, y: bottom - cornerRadius), radius: cornerRadius, startAngle: .degrees(180), endAngle: .degrees(90), clockwise: true)

        path.closeSubpath()
        return path
    }
}

// MARK: - Preferences to expose tab item bounds for animated background
private struct TabItemBoundsPreferenceKey: PreferenceKey {
    static var defaultValue: [TabIdentifier: Anchor<CGRect>] { [:] }
    static func reduce(value: inout [TabIdentifier: Anchor<CGRect>], nextValue: () -> [TabIdentifier: Anchor<CGRect>]) {
        value.merge(nextValue(), uniquingKeysWith: { $1 })
    }
}


