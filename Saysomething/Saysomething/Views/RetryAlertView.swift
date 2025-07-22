import SwiftUI

struct RetryAlertView: View {
    let message: String
    let retryAction: () -> Void
    let dismissAction: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            // Icon
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 40))
                .foregroundColor(.orange)
            
            // Message
            Text("Connection Issue")
                .font(.headline)
                .foregroundColor(.primary)
            
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            // Buttons
            HStack(spacing: 12) {
                Button("Retry") {
                    retryAction()
                }
                .buttonStyle(.borderedProminent)
                
                Button("Dismiss") {
                    dismissAction()
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(24)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 10)
        .padding(.horizontal, 20)
    }
}

struct NetworkStatusView: View {
    @ObservedObject var networkMonitor: NetworkMonitor
    
    var body: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(networkMonitor.isConnected ? Color.green : Color.red)
                .frame(width: 8, height: 8)
            
            Text(networkMonitor.isConnected ? "Connected" : "No Connection")
                .font(.caption)
                .foregroundColor(networkMonitor.isConnected ? .green : .red)
            
            if networkMonitor.isConnected {
                Text("(\(networkMonitor.connectionType.description))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct RetryAlertView_Previews: PreviewProvider {
    static var previews: some View {
        RetryAlertView(
            message: "Network connection lost. Please try again.",
            retryAction: {},
            dismissAction: {}
        )
        .previewLayout(.sizeThatFits)
        .padding()
        
        NetworkStatusView(networkMonitor: NetworkMonitor.shared)
            .previewLayout(.sizeThatFits)
            .padding()
    }
} 