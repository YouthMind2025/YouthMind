import Foundation
import Combine
import Network

// NetworkTestService is a class that tests the network connection quality and publishes updates.
// It uses the NetworkMonitor to check if the device is connected to the internet and the quality of the connection.
// It also simulates network issues and restores the connection.
class NetworkTestService: ObservableObject {
    // isReachable: true if the device is connected to the internet, false otherwise
    @Published var isReachable = true
    // connectionQuality: the quality of the connection
    @Published var connectionQuality: ConnectionQuality = .excellent
    // lastTestTime: the time of the last test
    @Published var lastTestTime: Date?
    
    private var cancellables = Set<AnyCancellable>()
    private let networkMonitor = NetworkMonitor.shared
    
    // different type of connection quality
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
        
        // define color of connection quality
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

    // test connection quality
    // return the quality of the connection
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
            // receive on main thread
            .receive(on: DispatchQueue.main)
            // update connection quality and last test time
            .handleEvents(receiveOutput: { [weak self] quality in
                self?.connectionQuality = quality
                self?.lastTestTime = Date()
            })
            .eraseToAnyPublisher()
    }
    
    // simulate network issues
    func simulateNetworkIssues() {
        // Simulate poor connection
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.connectionQuality = .poor
        }
        
        // Simulate connection loss after 3 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.connectionQuality = .unavailable
            self.isReachable = false
        }
    }
    
    // restore connection
    func restoreConnection() {
        isReachable = true
        connectionQuality = .excellent
    }
} 