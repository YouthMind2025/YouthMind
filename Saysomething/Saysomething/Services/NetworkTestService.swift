import Foundation
import Combine
import Network

class NetworkTestService: ObservableObject {
    @Published var isReachable = true
    @Published var connectionQuality: ConnectionQuality = .excellent
    @Published var lastTestTime: Date?
    
    private var cancellables = Set<AnyCancellable>()
    private let networkMonitor = NetworkMonitor.shared
    
    enum ConnectionQuality {
        case excellent
        case good
        case poor
        case unavailable
        
        var description: String {
            switch self {
            case .excellent:
                return "Excellent"
            case .good:
                return "Good"
            case .poor:
                return "Poor"
            case .unavailable:
                return "Unavailable"
            }
        }
        
        var color: String {
            switch self {
            case .excellent:
                return "green"
            case .good:
                return "blue"
            case .poor:
                return "orange"
            case .unavailable:
                return "red"
            }
        }
    }
    
    init() {
        // Monitor network changes
        networkMonitor.$isConnected
            .sink { [weak self] isConnected in
                self?.isReachable = isConnected
                if !isConnected {
                    self?.connectionQuality = .unavailable
                }
            }
            .store(in: &cancellables)
    }
    
    func testConnection() -> AnyPublisher<ConnectionQuality, Never> {
        guard let url = URL(string: "https://www.google.com") else {
            return Just(.unavailable).eraseToAnyPublisher()
        }
        
        let startTime = Date()
        
        return URLSession.shared.dataTaskPublisher(for: url)
            .map { _ -> ConnectionQuality in
                let responseTime = Date().timeIntervalSince(startTime)
                
                switch responseTime {
                case 0..<0.5:
                    return .excellent
                case 0.5..<1.0:
                    return .good
                case 1.0..<3.0:
                    return .poor
                default:
                    return .unavailable
                }
            }
            .catch { _ -> AnyPublisher<ConnectionQuality, Never> in
                Just(.unavailable).eraseToAnyPublisher()
            }
            .receive(on: DispatchQueue.main)
            .handleEvents(receiveOutput: { [weak self] quality in
                self?.connectionQuality = quality
                self?.lastTestTime = Date()
            })
            .eraseToAnyPublisher()
    }
    
    func simulateNetworkIssues() {
        // Simulate poor connection
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.connectionQuality = .poor
        }
        
        // Simulate connection loss
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.connectionQuality = .unavailable
            self.isReachable = false
        }
    }
    
    func restoreConnection() {
        isReachable = true
        connectionQuality = .excellent
    }
} 