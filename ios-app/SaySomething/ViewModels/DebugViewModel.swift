//
//  DebugViewModel.swift
//  SaySomething


import Foundation
import Combine

@MainActor
class DebugViewModel: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var feedStatus: FeedStatusResponse?
    @Published var rVectorDetails: RVectorResponse?
    @Published var isLoading = false
    @Published var error: Error?
    @Published var lastRefresh: Date?
    @Published var resetSuccess = false
    
    // MARK: - Private Properties
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Computed Properties
    
    var currentBlock: Int {
        feedStatus?.currentBlock ?? 0
    }
    
    var topEmotions: [String] {
        feedStatus?.rVectorSummary.topEmotions ?? []
    }
    
    var rVector: [Double] {
        feedStatus?.rVectorSummary.rVector ?? []
    }
    
    var interactionCount: Int {
        feedStatus?.rVectorSummary.interactionCount ?? 0
    }
    
    var isNextBlockAvailable: Bool {
        feedStatus?.nextBlockAvailability.isAvailable ?? false
    }
    
    var engagementScore: Double {
        feedStatus?.userActivityStatus.engagementScore ?? 0.0
    }
    
    var totalBlocksConsumed: Int {
        feedStatus?.userActivityStatus.totalBlocksConsumed ?? 0
    }
    
    // MARK: - Public Methods
    
    /// Refresh all debug data
    func refresh() async {
        isLoading = true
        error = nil
        
        // Fetch feed status (includes R-vector summary)
        await fetchFeedStatus()
        
        // Optionally fetch detailed R-vector info
        await fetchRVectorDetails()
        
        lastRefresh = Date()
        isLoading = false
    }
    
    /// Reset R-vector
    func resetRVector(reason: String = "Debug reset") async {
        isLoading = true
        error = nil
        resetSuccess = false
        
        do {
            let response = try await withCheckedThrowingContinuation { continuation in
                RVectorAPI.resetRVector(reason: reason)
                    .sink(
                        receiveCompletion: { completion in
                            if case .failure(let error) = completion {
                                continuation.resume(throwing: error)
                            }
                        },
                        receiveValue: { response in
                            continuation.resume(returning: response)
                        }
                    )
                    .store(in: &cancellables)
            }
            
            if response.data.success {
                resetSuccess = true
                // Refresh data after reset
                await refresh()
            }
            
        } catch {
            self.error = error
        }
        
        isLoading = false
    }
    
    // MARK: - Private Methods
    
    private func fetchFeedStatus() async {
        do {
            let response = try await withCheckedThrowingContinuation { continuation in
                FeedAPI.getFeedStatus()
                    .sink(
                        receiveCompletion: { completion in
                            if case .failure(let error) = completion {
                                continuation.resume(throwing: error)
                            }
                        },
                        receiveValue: { response in
                            continuation.resume(returning: response)
                        }
                    )
                    .store(in: &cancellables)
            }
            
            feedStatus = response.data
            
        } catch {
            self.error = error
        }
    }
    
    private func fetchRVectorDetails() async {
        do {
            let response = try await withCheckedThrowingContinuation { continuation in
                RVectorAPI.getRVector()
                    .sink(
                        receiveCompletion: { completion in
                            if case .failure(let error) = completion {
                                continuation.resume(throwing: error)
                            }
                        },
                        receiveValue: { response in
                            continuation.resume(returning: response)
                        }
                    )
                    .store(in: &cancellables)
            }
            
            rVectorDetails = response.data
            
        } catch {
            // Optional data, don't set error
            print("Could not fetch R-vector details: \(error)")
        }
    }
}
