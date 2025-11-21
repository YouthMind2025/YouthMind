//
//  RetryButton.swift
//  SaySomething


import SwiftUI

/// Retry button component for graceful error handling
/// Provides a user-friendly way to retry failed operations
struct RetryButton: View {
    let error: Error
    let action: () -> Void
    let title: String?
    let message: String?
    
    init(error: Error, action: @escaping () -> Void, title: String? = nil, message: String? = nil) {
        self.error = error
        self.action = action
        self.title = title
        self.message = message
    }
    
    var body: some View {
        VStack(spacing: 12) {
            // Error icon
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 24))
                .foregroundColor(.orange)
            
            // Error title
            Text(title ?? "Something went wrong")
                .font(.headline)
                .multilineTextAlignment(.center)
            
            // Error message
            Text(message ?? getErrorMessage())
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            // Retry button
            Button(action: action) {
                HStack(spacing: 8) {
                    Image(systemName: "arrow.clockwise")
                    Text("Try Again")
                }
                .font(.subheadline)
                .fontWeight(.medium)
            }
            .buttonStyle(.bordered)
            .controlSize(.regular)
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        )
        .padding(.horizontal, 20)
    }
    
    private func getErrorMessage() -> String {
        if let apiError = error as? APIError {
            return apiError.localizedDescription
        } else {
            return error.localizedDescription
        }
    }
}

/// Compact retry button for inline error states
struct CompactRetryButton: View {
    let error: Error
    let action: () -> Void
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .foregroundColor(.orange)
            
            Text("Failed to load")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Button("Retry", action: action)
                .font(.caption)
                .buttonStyle(.bordered)
                .controlSize(.mini)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.secondarySystemBackground))
        )
    }
}

/// Loading retry button that shows loading state
struct LoadingRetryButton: View {
    let isLoading: Bool
    let error: Error?
    let action: () -> Void
    
    var body: some View {
        if isLoading {
            HStack(spacing: 8) {
                ProgressView()
                    .scaleEffect(0.8)
                Text("Loading...")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        } else if let error = error {
            CompactRetryButton(error: error, action: action)
        } else {
            EmptyView()
        }
    }
}

#Preview {
    VStack(spacing: 20) {
        // Full retry button
        RetryButton(
            error: APIError.networkError(URLError(.notConnectedToInternet)),
            action: { print("Retry tapped") },
            title: "Failed to load more posts",
            message: "Please check your internet connection and try again."
        )
        
        // Compact retry button
        CompactRetryButton(
            error: APIError.timeout,
            action: { print("Compact retry tapped") }
        )
        
        // Loading retry button
        LoadingRetryButton(
            isLoading: true,
            error: nil,
            action: { print("Loading retry tapped") }
        )
    }
    .padding()
}
