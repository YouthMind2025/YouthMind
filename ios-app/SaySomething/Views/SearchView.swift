import SwiftUI

final class SearchViewModel: ObservableObject {
    enum SortOption: String, CaseIterable, Identifiable {
        case relevance
        case newest
        case views
        var id: String { rawValue }
        var title: String {
            switch self {
            case .relevance: return "Relevance"
            case .newest: return "Newest"
            case .views: return "Most Viewed"
            }
        }
    }

    @Published var query: String = ""
    @Published var historyKeywords: [String] = [
        "Break up", "Gossip", "The exam week", "Work pressure", "Bored lonely"
    ]
    

    // Search state
    @Published var results: [Post] = []
    @Published var selectedSort: SortOption = .relevance
    @Published var selectedEmotion: Emotion? = nil

    var displayedResults: [Post] {
        var filtered = results
        if let emotion = selectedEmotion {
            filtered = filtered.filter { $0.emotion == emotion }
        }
        switch selectedSort {
        case .relevance:
            return filtered
        case .newest:
            return filtered.sorted(by: { $0.date > $1.date })
        case .views:
            // Sorting by views uses the static count on Post. The view layer will display live engagement values.
            return filtered.sorted(by: { $0.view > $1.view })
        }
    }

    func runSearch(in feed: FeedViewModel) {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            results = []
            return
        }
        let lower = trimmed.lowercased()
        results = feed.posts.filter { post in
            post.title.lowercased().contains(lower) || post.content.lowercased().contains(lower)
        }
        // Maintain simple history (front insert, unique, cap 12)
        if !historyKeywords.contains(where: { $0.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            historyKeywords.insert(trimmed, at: 0)
            if historyKeywords.count > 12 { historyKeywords.removeLast(historyKeywords.count - 12) }
        }
    }

    func runTagSearch(_ tag: String, in feed: FeedViewModel) {
        query = tag
        runSearch(in: feed)
    }
}

struct SearchView: View {
    @StateObject private var vm = SearchViewModel()
    @EnvironmentObject private var feedVM: FeedViewModel
    @EnvironmentObject private var tabBarState: TabBarState
    @FocusState private var isFocused: Bool
    @Environment(\.dismiss) private var dismiss

    // Leaderboard controls
    @State private var leaderboardMetric: LeaderboardMetric = .resonance
    @State private var leaderboardTimeframe: LeaderboardTimeframe = .all

    // History controls
    @State private var historyShownCount: Int = 6
    private let historyBatchSize: Int = 8

    enum LeaderboardMetric: String, CaseIterable, Identifiable {
        case resonance
        case views
        case saves
        var id: String { rawValue }
        var title: String {
            switch self {
            case .resonance: return "Resonance"
            case .views: return "Views"
            case .saves: return "Saves"
            }
        }
    }

    enum LeaderboardTimeframe: String, CaseIterable, Identifiable {
        case day
        case week
        case month
        case all
        var id: String { rawValue }
        var title: String {
            switch self {
            case .day: return "Last 24h"
            case .week: return "Last 7d"
            case .month: return "Last 30d"
            case .all: return "All time"
            }
        }
        var shortTitle: String {
            switch self {
            case .day: return "24h"
            case .week: return "7d"
            case .month: return "30d"
            case .all: return "All"
            }
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                searchBar
                if !vm.results.isEmpty || !vm.query.isEmpty {
                    filtersSection
                }
                historySection
                leaderboardSection
                resultsSection
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
        }
        .scrollDismissesKeyboard(.immediately)
        .navigationTitle("Search")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            isFocused = true
            withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true }
        }
        .onDisappear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = false } }
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField("Search posts, topics...", text: $vm.query)
                    .submitLabel(.search)
                    .focused($isFocused)
                    .onSubmit { executeSearch() }
                if !vm.query.isEmpty {
                    Button {
                        withAnimation { vm.query = ""; vm.results = [] }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                    .contentShape(Rectangle())
                }
            }
            .padding(.vertical, 10)
            .padding(.horizontal, 12)
                     .background(DesignConstants.ColorPalette.brandPurple.opacity(0.06))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            if isFocused || !vm.query.isEmpty {
                Button("Cancel") {
                    isFocused = false
                    withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = false }
                    dismiss()
                }
                .foregroundColor(.secondary)
                .transition(.opacity.combined(with: .move(edge: .trailing)))
            } else {
                Button("Search") { executeSearch() }
                    .buttonStyle(.borderedProminent)
                    .tint(DesignConstants.ColorPalette.brandPurple)
                    .transition(.opacity)
            }
        }
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("History").font(.headline)
                Spacer()
                if !vm.historyKeywords.isEmpty {
                    Button("Clear") {
                        withAnimation { vm.historyKeywords.removeAll() }
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }
            }
            let shown = Array(vm.historyKeywords.prefix(historyShownCount))
            FlowTagWrap(tags: shown) { tag in
                HistoryChip(
                    label: tag,
                    onSearch: { executeTagSearch(tag) },
                    onDelete: {
                        withAnimation {
                            vm.historyKeywords.removeAll { $0.caseInsensitiveCompare(tag) == .orderedSame }
                        }
                    }
                )
            }
            if vm.historyKeywords.count > historyShownCount {
                Button {
                    withAnimation {
                        historyShownCount = min(vm.historyKeywords.count, historyShownCount + historyBatchSize)
                    }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.down.circle")
                        Text("Load more history")
                    }
                }
                .buttonStyle(.bordered)
                .tint(DesignConstants.ColorPalette.brandPurple)
            } else if vm.historyKeywords.count > 6 && historyShownCount > 6 {
                Button {
                    withAnimation { historyShownCount = 6 }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.up.circle")
                        Text("Show less")
                    }
                }
                .buttonStyle(.bordered)
                .tint(DesignConstants.ColorPalette.brandPurple)
            }
        }
    }

    private var filtersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Sort picker
            Picker("Sort", selection: $vm.selectedSort) {
                ForEach(SearchViewModel.SortOption.allCases) { option in
                    Text(option.title).tag(option)
                }
            }
            .pickerStyle(.segmented)

            // Emotion filter chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    emotionChip(nil, label: "All")
                    ForEach(Emotion.allCases, id: \.rawValue) { emotion in
                        emotionChip(emotion, label: emotionDisplayName(emotion))
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }

    

    private var leaderboardSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with gradient accent pill
            HStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.black)
                    .frame(width: 6, height: 22)
                Text("Top \(leaderboardMetric.title) Leaderboard")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.black)
                Spacer()
            }
            .padding(.bottom, 2)

            // Metric selector
            Picker("Metric", selection: $leaderboardMetric) {
                ForEach(LeaderboardMetric.allCases) { metric in
                    Text(metric.title).tag(metric)
                }
            }
            .pickerStyle(.segmented)
            .tint(DesignConstants.ColorPalette.brandPurple)

            // Board
            VStack(spacing: 10) {
                ForEach(Array(leaderboardPosts.prefix(10).enumerated()), id: \.offset) { index, post in
                    NavigationLink {
                        PostDetailView(post: post)
                            .environmentObject(feedVM)
                            .onAppear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true } }
                            .onDisappear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = false } }
                    } label: {
                        HStack(spacing: 12) {
                            // Rank badge
                            rankBadge(index: index)

                            // Emotion color indicator
                            Circle()
                                .fill(post.emotion.color)
                                .frame(width: 8, height: 8)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(post.title)
                                    .font(.system(size: 14, weight: .semibold))
                                    .lineLimit(1)
                                    .foregroundColor(.black)
                            }
                            Spacer()
                            // Show only the selected metric value on the right
                            switch leaderboardMetric {
                            case .views:
                                Label("\(formattedViews(feedVM.engagement[post.id]?.views ?? post.view))", systemImage: "eye")
                                    .font(.caption)
                                    .foregroundColor(.black)
                            case .resonance:
                                Label("\(feedVM.engagement[post.id]?.resonance ?? post.resonance)", systemImage: "heart.fill")
                                    .font(.caption)
                                    .foregroundColor(.black)
                            case .saves:
                                Label("\(feedVM.engagement[post.id]?.saves ?? post.collect)", systemImage: "bookmark")
                                    .font(.caption)
                                    .foregroundColor(.black)
                            }
                        }
                        .padding(.vertical, 12)
                        .padding(.horizontal, 12)
                        .background(rowBackground(for: index))
                        .overlay(alignment: .bottom) {
                            if index < max(0, min(leaderboardPosts.count, 10) - 1) {
                                Rectangle()
                                    .fill(Color.gray.opacity(0.12))
                                    .frame(height: 0.5)
                                    .padding(.leading, 44)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            // Removed card styling so the board blends with page background
        }
    }

    private var resultsSection: some View {
        Group {
            if !vm.query.isEmpty && vm.displayedResults.isEmpty {
                VStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 28))
                        .foregroundColor(.secondary)
                    Text("No results")
                        .font(.headline)
                    Text("Try different keywords or filters")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    HStack(spacing: 8) {
                        Button("Clear filters") { withAnimation { vm.selectedEmotion = nil; vm.selectedSort = .relevance } }
                            .buttonStyle(.bordered)
                        Button("Clear query") { withAnimation { vm.query = ""; vm.results = [] } }
                            .buttonStyle(.bordered)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            } else if !vm.displayedResults.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Results")
                            .font(.headline)
                        Spacer()
                        Text("\(vm.displayedResults.count) found")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    ForEach(vm.displayedResults) { post in
                        NavigationLink {
                            PostDetailView(post: post)
                                .environmentObject(feedVM)
                                .onAppear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true } }
                                .onDisappear { withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = false } }
                        } label: {
                            HStack(alignment: .top, spacing: 12) {
                                ZStack {
                                    Circle()
                                        .fill(post.emotion.color.opacity(0.18))
                                        .frame(width: 42, height: 42)
                                    Text(String(post.title.prefix(1)))
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(.primary)
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    highlightedText(post.title, query: vm.query)
                                        .font(.system(size: 15, weight: .semibold))
                                        .lineLimit(1)
                                    highlightedText(post.content, query: vm.query)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                        .lineLimit(2)
                                    HStack(spacing: 10) {
                                        Label(formattedViews(feedVM.engagement[post.id]?.views ?? post.view), systemImage: "eye")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        Label("\(feedVM.engagement[post.id]?.resonance ?? post.resonance)", systemImage: "heart.fill")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        Text(emotionDisplayName(post.emotion))
                                            .font(.caption2)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(post.emotion.color.opacity(0.15))
                                            .clipShape(Capsule())
                                    }
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(12)
                            .background(Color(.systemBackground))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.gray.opacity(0.12), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func formattedViews(_ value: Int) -> String {
        // 219w style: treat 10k as 'w'
        if value >= 10000 {
            let w = Double(value) / 10000.0
            let text = (w >= 10 ? String(Int(w)) : String(format: "%.1f", w))
            return "\(text)w"
        }
        return String(value)
    }
}

private extension SearchView {
    // Compute leaderboard posts based on selected filters
    var leaderboardPosts: [Post] {
        let emotionFiltered: [Post]
        if let emotion = vm.selectedEmotion {
            emotionFiltered = feedVM.posts.filter { $0.emotion == emotion }
        } else {
            emotionFiltered = feedVM.posts
        }

        let timeFiltered: [Post]
        switch leaderboardTimeframe {
        case .all:
            timeFiltered = emotionFiltered
        case .day:
            timeFiltered = emotionFiltered.filter { $0.date >= Date().addingTimeInterval(-24 * 60 * 60) }
        case .week:
            timeFiltered = emotionFiltered.filter { $0.date >= Date().addingTimeInterval(-7 * 24 * 60 * 60) }
        case .month:
            timeFiltered = emotionFiltered.filter { $0.date >= Date().addingTimeInterval(-30 * 24 * 60 * 60) }
        }

        let sorted: [Post]
        switch leaderboardMetric {
        case .resonance:
            sorted = timeFiltered.sorted { (feedVM.engagement[$0.id]?.resonance ?? $0.resonance) > (feedVM.engagement[$1.id]?.resonance ?? $1.resonance) }
        case .views:
            sorted = timeFiltered.sorted { (feedVM.engagement[$0.id]?.views ?? $0.view) > (feedVM.engagement[$1.id]?.views ?? $1.view) }
        case .saves:
            sorted = timeFiltered.sorted { (feedVM.engagement[$0.id]?.saves ?? $0.collect) > (feedVM.engagement[$1.id]?.saves ?? $1.collect) }
        }

        return sorted
    }
    func executeSearch() {
        withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true }
        vm.runSearch(in: feedVM)
        isFocused = false
    }

    func executeTagSearch(_ tag: String) {
        withAnimation(.easeInOut(duration: 0.2)) { tabBarState.isHidden = true }
        vm.runTagSearch(tag, in: feedVM)
        isFocused = false
    }

    func highlightedText(_ text: String, query: String) -> Text {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return Text(text) }

        var attributed = AttributedString(text)

        let lowerText = text.lowercased()
        let lowerQuery = trimmed.lowercased()

        var searchRange: Range<String.Index>? = lowerText.startIndex..<lowerText.endIndex
        while let foundRange = lowerText.range(of: lowerQuery, options: [], range: searchRange, locale: nil) {
            if let lower = AttributedString.Index(foundRange.lowerBound, within: attributed),
               let upper = AttributedString.Index(foundRange.upperBound, within: attributed) {
                attributed[lower..<upper].foregroundColor = DesignConstants.ColorPalette.primary
                attributed[lower..<upper].font = .system(size: 13, weight: .semibold)
            }
            searchRange = foundRange.upperBound..<lowerText.endIndex
        }
        return Text(attributed)
    }

    func emotionDisplayName(_ emotion: Emotion) -> String {
        switch emotion {
        case .angry: return "Angry"
        case .sad: return "Sad"
        case .happy: return "Happy"
        case .conflicted: return "Conflicted"
        case .surprised: return "Surprised"
        case .fear: return "Fear"
        }
    }

    @ViewBuilder
    func rowBackground(for index: Int) -> some View {
        // Solid white background for all rows
        Color.white
    }

    @ViewBuilder
    func rankBadge(index: Int) -> some View {
        let rank = index + 1
        let size: CGFloat = rank >= 10 ? 28 : (rank == 1 ? 26 : 24)
        let font: Font = rank >= 10 ? .system(size: 12, weight: .semibold) : (rank == 1 ? .system(size: 14, weight: .bold) : .system(size: 13, weight: .semibold))
        // Depth factor: higher rank -> deeper colors (more opacity + stronger glow)
        let depth = max(0.55, 1.0 - 0.05 * Double(rank - 1))
        let purple = DesignConstants.ColorPalette.brandPurple.opacity(depth)
        let purpleLight = DesignConstants.ColorPalette.brandPurple.opacity(max(0.35, depth * 0.8))
        let fillGradient = LinearGradient(
            gradient: Gradient(stops: [
                .init(color: purple, location: 0.0),
                .init(color: purple, location: 0.7),
                .init(color: purpleLight, location: 1.0)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        let ringGradient = LinearGradient(
            gradient: Gradient(colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.6 * depth), DesignConstants.ColorPalette.brandPurple.opacity(0.35 * depth)]),
            startPoint: .bottomLeading,
            endPoint: .topTrailing
        )
        Text("\(rank)")
            .font(font)
            .foregroundColor(.white)
            .frame(width: size, height: size)
            .background(
                Circle()
                    .fill(fillGradient)
            )
            .overlay(
                Circle()
                    .stroke(ringGradient, lineWidth: 1)
            )
            .shadow(color: DesignConstants.ColorPalette.brandPurple.opacity(0.2 * depth), radius: rank == 1 ? 6 : 3, x: 0, y: 2)
    }

    @ViewBuilder
    func emotionChip(_ emotion: Emotion?, label: String) -> some View {
        let isSelected = vm.selectedEmotion == emotion
        Button {
            withAnimation { vm.selectedEmotion = emotion }
        } label: {
            HStack(spacing: 6) {
                if let emotionUnwrapped = emotion {
                    Circle()
                        .fill(emotionUnwrapped.color.opacity(0.8))
                        .frame(width: 8, height: 8)
                } else {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                Text(label)
                    .font(.caption)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isSelected ? DesignConstants.ColorPalette.primary.opacity(0.12) : Color(.secondarySystemBackground))
            .foregroundColor(isSelected ? DesignConstants.ColorPalette.primary : .primary)
            .overlay(Capsule().stroke(isSelected ? DesignConstants.ColorPalette.primary.opacity(0.4) : Color.gray.opacity(0.2)))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // Inline chip with a small trailing clear button
    struct HistoryChip: View {
        let label: String
        let onSearch: () -> Void
        let onDelete: () -> Void

        @State private var isPressed: Bool = false

        var body: some View {
            HStack(spacing: 8) {
                // Left icon + label area
                Button(action: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        isPressed = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.14) {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.9)) { isPressed = false }
                    }
                    onSearch()
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.secondary)
                        Text(label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.primary)
                    }
                    .padding(.leading, 12)
                    .padding(.vertical, 8)
                }
                .buttonStyle(.plain)

                // Delete chip button
                Button(action: onDelete) {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 18, height: 18)
                        .background(
                            Circle().fill(
                                LinearGradient(
                                    colors: [DesignConstants.ColorPalette.brandPurple, DesignConstants.ColorPalette.brandPurple.opacity(0.85)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        )
                        .shadow(color: DesignConstants.ColorPalette.brandPurple.opacity(0.15), radius: 2, x: 0, y: 1)
                        .padding(.trailing, 8)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Delete \(label)")
            }
            .scaleEffect(isPressed ? 0.98 : 1.0)
            .background(
                LinearGradient(
                    colors: [DesignConstants.ColorPalette.brandPurple.opacity(0.06), DesignConstants.ColorPalette.brandPurple.opacity(0.03)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay(
                Capsule().stroke(DesignConstants.ColorPalette.brandPurple.opacity(0.18), lineWidth: 0.8)
            )
            .clipShape(Capsule())
            .contentShape(Rectangle())
        }
    }
}

// Simple flow layout for tags using adaptive grid
private struct FlowTagWrap<TagContent: View>: View {
    let tags: [String]
    let content: (String) -> TagContent

    private let columns = [GridItem(.adaptive(minimum: 110), spacing: 8)]

    var body: some View {
        LazyVGrid(columns: columns, alignment: .leading, spacing: 8) {
            ForEach(tags, id: \.self) { tag in
                content(tag)
            }
        }
    }
}


