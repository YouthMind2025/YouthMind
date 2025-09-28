import Foundation
import os.log

// MARK: - Error Frequency Analyzer
/// Analyzes error frequency patterns and detects anomalies
final class ErrorFrequencyAnalyzer {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "ErrorFrequencyAnalyzer")
    private let queue = DispatchQueue(label: "com.youthmind.frequency.analyzer", qos: .background)
    
    // Frequency tracking
    private var errorFrequencies: [String: ErrorFrequencyData] = [:]
    private var timeWindows: [TimeWindow] = []
    private let maxTimeWindows = 100
    
    // Configuration
    private let anomalyThreshold: Double = 2.0 // Standard deviations
    private let minDataPoints = 5
    
    // MARK: - Initialization
    init() {
        setupTimeWindows()
    }
    
    // MARK: - Public Methods
    
    /// Initialize the frequency analyzer
    func initialize() async {
        logger.info("Initializing Error Frequency Analyzer")
        
        // Load cached frequency data
        await loadCachedFrequencyData()
        
        // Start background analysis
        startBackgroundAnalysis()
    }
    
    /// Record an error for frequency analysis
    func recordError(_ error: AppError) async {
        let errorCode = error.errorCode
        let timestamp = Date()
        
        await queue.async { [weak self] in
            self?.processErrorFrequency(errorCode: errorCode, timestamp: timestamp, severity: error.severity)
        }
    }
    
    /// Get frequency analysis for a specific error code
    func getFrequencyAnalysis(for errorCode: String) async -> ErrorFrequencyAnalysis? {
        return await queue.sync {
            return errorFrequencies[errorCode]?.analysis
        }
    }
    
    /// Get all frequency analyses
    func getAllFrequencyAnalyses() async -> [String: ErrorFrequencyAnalysis] {
        return await queue.sync {
            return errorFrequencies.compactMapValues { $0.analysis }
        }
    }
    
    /// Get anomaly detection results
    func getAnomalyDetection() async -> [ErrorAnomaly] {
        return await queue.sync {
            return detectAnomalies()
        }
    }
    
    /// Get frequency trends
    func getFrequencyTrends() async -> [FrequencyTrend] {
        return await queue.sync {
            return calculateTrends()
        }
    }
    
    /// Get error burst detection
    func getErrorBursts() async -> [ErrorBurst] {
        return await queue.sync {
            return detectErrorBursts()
        }
    }
    
    // MARK: - Private Methods
    
    private func processErrorFrequency(errorCode: String, timestamp: Date, severity: AppError.ErrorSeverity) {
        // Get or create frequency data for this error code
        var frequencyData = errorFrequencies[errorCode] ?? ErrorFrequencyData(
            errorCode: errorCode,
            occurrences: [],
            severity: severity,
            firstOccurrence: timestamp,
            lastOccurrence: timestamp
        )
        
        // Add occurrence
        frequencyData.occurrences.append(ErrorOccurrence(timestamp: timestamp, severity: severity))
        frequencyData.lastOccurrence = timestamp
        
        // Update time windows
        updateTimeWindows(errorCode: errorCode, timestamp: timestamp)
        
        // Calculate frequency metrics
        frequencyData.analysis = calculateFrequencyAnalysis(frequencyData)
        
        // Store updated data
        errorFrequencies[errorCode] = frequencyData
        
        // Check for anomalies
        checkForAnomalies(errorCode: errorCode, frequencyData: frequencyData)
        
        logger.debug("Recorded error frequency for \(errorCode): \(frequencyData.occurrences.count) total occurrences")
    }
    
    private func updateTimeWindows(errorCode: String, timestamp: Date) {
        // Add to current time window
        if let currentWindow = timeWindows.last, currentWindow.isActive {
            currentWindow.addError(errorCode: errorCode, timestamp: timestamp)
        } else {
            // Create new time window
            let newWindow = TimeWindow(startTime: timestamp, duration: 3600) // 1 hour windows
            newWindow.addError(errorCode: errorCode, timestamp: timestamp)
            timeWindows.append(newWindow)
            
            // Maintain max time windows
            if timeWindows.count > maxTimeWindows {
                timeWindows.removeFirst()
            }
        }
    }
    
    private func calculateFrequencyAnalysis(_ data: ErrorFrequencyData) -> ErrorFrequencyAnalysis {
        let occurrences = data.occurrences
        let totalCount = occurrences.count
        
        guard totalCount > 0 else {
            return ErrorFrequencyAnalysis(
                errorCode: data.errorCode,
                totalOccurrences: 0,
                frequencyPerHour: 0.0,
                frequencyPerDay: 0.0,
                averageInterval: 0.0,
                trend: .stable,
                isAnomaly: false,
                severity: data.severity,
                timeRange: TimeRange(start: data.firstOccurrence, end: data.lastOccurrence)
            )
        }
        
        // Calculate time range
        let timeRange = TimeRange(start: data.firstOccurrence, end: data.lastOccurrence)
        let totalDuration = timeRange.duration
        
        // Calculate frequencies
        let frequencyPerHour = totalDuration > 0 ? Double(totalCount) / (totalDuration / 3600.0) : 0.0
        let frequencyPerDay = totalDuration > 0 ? Double(totalCount) / (totalDuration / 86400.0) : 0.0
        
        // Calculate average interval
        let intervals = calculateIntervals(occurrences)
        let averageInterval = intervals.isEmpty ? 0.0 : intervals.reduce(0, +) / Double(intervals.count)
        
        // Determine trend
        let trend = calculateTrend(occurrences)
        
        // Check for anomaly
        let isAnomaly = checkAnomaly(frequencyPerHour: frequencyPerHour, errorCode: data.errorCode)
        
        return ErrorFrequencyAnalysis(
            errorCode: data.errorCode,
            totalOccurrences: totalCount,
            frequencyPerHour: frequencyPerHour,
            frequencyPerDay: frequencyPerDay,
            averageInterval: averageInterval,
            trend: trend,
            isAnomaly: isAnomaly,
            severity: data.severity,
            timeRange: timeRange
        )
    }
    
    private func calculateIntervals(_ occurrences: [ErrorOccurrence]) -> [TimeInterval] {
        guard occurrences.count > 1 else { return [] }
        
        let sortedOccurrences = occurrences.sorted { $0.timestamp < $1.timestamp }
        var intervals: [TimeInterval] = []
        
        for i in 1..<sortedOccurrences.count {
            let interval = sortedOccurrences[i].timestamp.timeIntervalSince(sortedOccurrences[i-1].timestamp)
            intervals.append(interval)
        }
        
        return intervals
    }
    
    private func calculateTrend(_ occurrences: [ErrorOccurrence]) -> FrequencyTrend {
        guard occurrences.count >= minDataPoints else { return .stable }
        
        // Split occurrences into two halves
        let midPoint = occurrences.count / 2
        let firstHalf = Array(occurrences.prefix(midPoint))
        let secondHalf = Array(occurrences.suffix(occurrences.count - midPoint))
        
        let firstHalfRate = calculateRate(firstHalf)
        let secondHalfRate = calculateRate(secondHalf)
        
        let changePercent = firstHalfRate > 0 ? ((secondHalfRate - firstHalfRate) / firstHalfRate) * 100 : 0
        
        if changePercent > 20 {
            return .increasing
        } else if changePercent < -20 {
            return .decreasing
        } else {
            return .stable
        }
    }
    
    private func calculateRate(_ occurrences: [ErrorOccurrence]) -> Double {
        guard occurrences.count > 1 else { return 0.0 }
        
        let sortedOccurrences = occurrences.sorted { $0.timestamp < $1.timestamp }
        let duration = sortedOccurrences.last!.timestamp.timeIntervalSince(sortedOccurrences.first!.timestamp)
        
        return duration > 0 ? Double(occurrences.count) / (duration / 3600.0) : 0.0
    }
    
    private func checkAnomaly(frequencyPerHour: Double, errorCode: String) -> Bool {
        // Get historical frequency data for this error code
        let historicalFrequencies = timeWindows.compactMap { window in
            window.errorCounts[errorCode]
        }
        
        guard historicalFrequencies.count >= minDataPoints else { return false }
        
        // Calculate mean and standard deviation
        let mean = historicalFrequencies.reduce(0, +) / Double(historicalFrequencies.count)
        let variance = historicalFrequencies.map { pow($0 - mean, 2) }.reduce(0, +) / Double(historicalFrequencies.count)
        let standardDeviation = sqrt(variance)
        
        // Check if current frequency is an anomaly
        let zScore = standardDeviation > 0 ? abs(frequencyPerHour - mean) / standardDeviation : 0
        return zScore > anomalyThreshold
    }
    
    private func detectAnomalies() -> [ErrorAnomaly] {
        var anomalies: [ErrorAnomaly] = []
        
        for (errorCode, data) in errorFrequencies {
            if let analysis = data.analysis, analysis.isAnomaly {
                let anomaly = ErrorAnomaly(
                    errorCode: errorCode,
                    severity: analysis.severity,
                    frequencyPerHour: analysis.frequencyPerHour,
                    expectedFrequency: calculateExpectedFrequency(errorCode: errorCode),
                    deviation: analysis.frequencyPerHour - calculateExpectedFrequency(errorCode: errorCode),
                    timestamp: Date(),
                    description: "Unusual frequency detected for \(errorCode)"
                )
                anomalies.append(anomaly)
            }
        }
        
        return anomalies.sorted { $0.deviation > $1.deviation }
    }
    
    private func calculateExpectedFrequency(errorCode: String) -> Double {
        let historicalFrequencies = timeWindows.compactMap { window in
            window.errorCounts[errorCode]
        }
        
        guard !historicalFrequencies.isEmpty else { return 0.0 }
        
        return historicalFrequencies.reduce(0, +) / Double(historicalFrequencies.count)
    }
    
    private func calculateTrends() -> [FrequencyTrend] {
        return errorFrequencies.compactMap { (errorCode, data) in
            guard let analysis = data.analysis else { return nil }
            
            return FrequencyTrend(
                errorCode: errorCode,
                trend: analysis.trend,
                frequencyPerHour: analysis.frequencyPerHour,
                changePercent: calculateChangePercent(errorCode: errorCode),
                timestamp: Date()
            )
        }
    }
    
    private func calculateChangePercent(errorCode: String) -> Double {
        let historicalFrequencies = timeWindows.compactMap { window in
            window.errorCounts[errorCode]
        }
        
        guard historicalFrequencies.count >= 2 else { return 0.0 }
        
        let recent = Array(historicalFrequencies.suffix(historicalFrequencies.count / 2))
        let older = Array(historicalFrequencies.prefix(historicalFrequencies.count / 2))
        
        let recentAvg = recent.reduce(0, +) / Double(recent.count)
        let olderAvg = older.reduce(0, +) / Double(older.count)
        
        return olderAvg > 0 ? ((recentAvg - olderAvg) / olderAvg) * 100 : 0.0
    }
    
    private func detectErrorBursts() -> [ErrorBurst] {
        var bursts: [ErrorBurst] = []
        
        for window in timeWindows {
            for (errorCode, count) in window.errorCounts {
                if count > 5 { // Threshold for burst detection
                    let burst = ErrorBurst(
                        errorCode: errorCode,
                        count: count,
                        startTime: window.startTime,
                        endTime: window.endTime,
                        duration: window.duration,
                        severity: getSeverityForErrorCode(errorCode)
                    )
                    bursts.append(burst)
                }
            }
        }
        
        return bursts.sorted { $0.count > $1.count }
    }
    
    private func getSeverityForErrorCode(_ errorCode: String) -> AppError.ErrorSeverity {
        return errorFrequencies[errorCode]?.severity ?? .low
    }
    
    private func checkForAnomalies(errorCode: String, frequencyData: ErrorFrequencyData) {
        if let analysis = frequencyData.analysis, analysis.isAnomaly {
            logger.warning("Anomaly detected for error \(errorCode): \(analysis.frequencyPerHour) occurrences/hour")
        }
    }
    
    private func setupTimeWindows() {
        // Initialize with empty time windows
        timeWindows = []
    }
    
    private func loadCachedFrequencyData() async {
        do {
            let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let frequencyDataURL = documentsDirectory.appendingPathComponent("analytics_frequency_data.json")
            
            guard FileManager.default.fileExists(atPath: frequencyDataURL.path) else {
                logger.info("No cached frequency data found")
                return
            }
            
            let data = try Data(contentsOf: frequencyDataURL)
            let cachedData = try JSONDecoder().decode(CachedFrequencyData.self, from: data)
            
            await queue.async { [weak self] in
                self?.errorFrequencies = Dictionary(uniqueKeysWithValues: cachedData.errorFrequencies.map { ($0.errorCode, $0) })
                self?.timeWindows = cachedData.timeWindows
            }
            
            logger.info("Loaded \(cachedData.errorFrequencies.count) cached frequency records")
        } catch {
            logger.error("Failed to load cached frequency data: \(error.localizedDescription)")
        }
    }
    
    private func startBackgroundAnalysis() {
        // Run analysis every 5 minutes
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { [weak self] _ in
            Task {
                await self?.performBackgroundAnalysis()
            }
        }
    }
    
    private func performBackgroundAnalysis() async {
        // Clean up old data
        await cleanupOldData()
        
        // Cache current data
        await cacheFrequencyData()
        
        // Detect anomalies
        let anomalies = await getAnomalyDetection()
        if !anomalies.isEmpty {
            logger.info("Detected \(anomalies.count) error anomalies")
        }
    }
    
    private func cacheFrequencyData() async {
        await queue.async { [weak self] in
            guard let self = self else { return }
            
            do {
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let frequencyDataURL = documentsDirectory.appendingPathComponent("analytics_frequency_data.json")
                
                let cachedData = CachedFrequencyData(
                    errorFrequencies: Array(self.errorFrequencies.values),
                    timeWindows: self.timeWindows,
                    lastUpdated: Date()
                )
                
                let data = try JSONEncoder().encode(cachedData)
                try data.write(to: frequencyDataURL, options: [.atomic])
                
                self.logger.debug("Cached \(self.errorFrequencies.count) frequency records")
            } catch {
                self.logger.error("Failed to cache frequency data: \(error.localizedDescription)")
            }
        }
    }
    
    private func cleanupOldData() async {
        let cutoffTime = Date().addingTimeInterval(-7 * 24 * 3600) // 7 days ago
        
        await queue.async { [weak self] in
            guard let self = self else { return }
            
            // Clean up old time windows
            self.timeWindows.removeAll { $0.endTime < cutoffTime }
            
            // Clean up old occurrences
            for (errorCode, data) in self.errorFrequencies {
                let filteredOccurrences = data.occurrences.filter { $0.timestamp > cutoffTime }
                if filteredOccurrences.isEmpty {
                    self.errorFrequencies.removeValue(forKey: errorCode)
                } else {
                    var updatedData = data
                    updatedData.occurrences = filteredOccurrences
                    self.errorFrequencies[errorCode] = updatedData
                }
            }
        }
    }
}

// MARK: - Frequency Analysis Models

/// Error frequency data
struct ErrorFrequencyData: Codable {
    let errorCode: String
    var occurrences: [ErrorOccurrence]
    let severity: AppError.ErrorSeverity
    let firstOccurrence: Date
    var lastOccurrence: Date
    var analysis: ErrorFrequencyAnalysis?
}

/// Error occurrence
struct ErrorOccurrence: Codable {
    let timestamp: Date
    let severity: AppError.ErrorSeverity
}

/// Error frequency analysis
struct ErrorFrequencyAnalysis: Codable {
    let errorCode: String
    let totalOccurrences: Int
    let frequencyPerHour: Double
    let frequencyPerDay: Double
    let averageInterval: TimeInterval
    let trend: FrequencyTrendType
    let isAnomaly: Bool
    let severity: AppError.ErrorSeverity
    let timeRange: TimeRange
}

/// Frequency trend types
enum FrequencyTrendType: String, Codable {
    case increasing = "increasing"
    case decreasing = "decreasing"
    case stable = "stable"
}

/// Time range
struct TimeRange: Codable {
    let start: Date
    let end: Date
    
    var duration: TimeInterval {
        return end.timeIntervalSince(start)
    }
}

/// Time window for frequency tracking
class TimeWindow: Codable {
    let startTime: Date
    let duration: TimeInterval
    var errorCounts: [String: Int] = [:]
    
    var endTime: Date {
        return startTime.addingTimeInterval(duration)
    }
    
    var isActive: Bool {
        return Date() < endTime
    }
    
    init(startTime: Date, duration: TimeInterval) {
        self.startTime = startTime
        self.duration = duration
    }
    
    func addError(errorCode: String, timestamp: Date) {
        errorCounts[errorCode, default: 0] += 1
    }
}

/// Error anomaly
struct ErrorAnomaly: Codable {
    let errorCode: String
    let severity: AppError.ErrorSeverity
    let frequencyPerHour: Double
    let expectedFrequency: Double
    let deviation: Double
    let timestamp: Date
    let description: String
}

/// Frequency trend
struct FrequencyTrend: Codable {
    let errorCode: String
    let trend: FrequencyTrendType
    let frequencyPerHour: Double
    let changePercent: Double
    let timestamp: Date
}

/// Error burst
struct ErrorBurst: Codable {
    let errorCode: String
    let count: Int
    let startTime: Date
    let endTime: Date
    let duration: TimeInterval
    let severity: AppError.ErrorSeverity
}

/// Cached frequency data for persistence
struct CachedFrequencyData: Codable {
    let errorFrequencies: [ErrorFrequencyData]
    let timeWindows: [TimeWindow]
    let lastUpdated: Date
}
