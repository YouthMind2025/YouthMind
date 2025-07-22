import Foundation
import Network
import Combine

// NetworkMonitor is a singleton class that observes the device's network connectivity status.
// It uses Apple's Network framework to monitor changes and publishes updates for SwiftUI or Combine subscribers.
class NetworkMonitor: ObservableObject {
    static let shared = NetworkMonitor()
    
    @Published var isConnected = true
    @Published var connectionType: ConnectionType = .unknown
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    
    private init() {
        startMonitoring()
    }
    
    // different type of network connection
    enum ConnectionType {
        case wifi
        case cellular
        case ethernet
        case unknown
        
        // description of network connection
        var description: String {
            switch self {
            case .wifi:
                return "WiFi"
            case .cellular:
                return "Cellular"
            case .ethernet:
                return "Ethernet"
            case .unknown:
                return "Unknown"
            }
        }
    }
    
    // deinit to stop monitoring
    deinit {
        stopMonitoring()
    }
    
    // start monitoring
    func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            // update UI on main thread
            DispatchQueue.main.async {
                self?.isConnected = path.status == .satisfied
                self?.updateConnectionType(path)
            }
        }
        monitor.start(queue: queue)
    }
    
    // stop monitoring
    func stopMonitoring() {
        monitor.cancel()
    }
    
    // update connection type
    private func updateConnectionType(_ path: NWPath) {
        if path.usesInterfaceType(.wifi) {
            connectionType = .wifi
        } else if path.usesInterfaceType(.cellular) {
            connectionType = .cellular
        } else if path.usesInterfaceType(.wiredEthernet) {
            connectionType = .ethernet
        } else {
            connectionType = .unknown
        }
    }
    
    // simulate no internet
    func simulateNoInternet() {
        DispatchQueue.main.async {
            self.isConnected = false
        }
    }
    
    // simulate internet restored
    func simulateInternetRestored() {
        DispatchQueue.main.async {
            self.isConnected = true
        }
    }
} 