//
//  CrashTestView.swift
//  Saysomething
//
//  Created by Firebase Integration
//

import SwiftUI
import FirebaseCrashlytics

struct CrashTestView: View {
    @State private var showingAlert = false
    @State private var alertMessage = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Firebase Crashlytics Test")
                    .font(.title)
                    .fontWeight(.bold)
                
                VStack(spacing: 15) {
                    // Test Logging
                    Button("Log Test Event") {
                        FirebaseService.shared.logEvent("User tapped 'Log Test Event' button")
                        showAlert("Event logged successfully!")
                    }
                    .buttonStyle(.borderedProminent)
                    
                    // Test Error Logging
                    Button("Log Test Error") {
                        let testError = NSError(domain: "TestDomain", code: 1001, userInfo: [
                            NSLocalizedDescriptionKey: "This is a test error for Crashlytics"
                        ])
                        FirebaseService.shared.logError(testError, additionalUserInfo: [
                            "test_key": "test_value",
                            "timestamp": Date().timeIntervalSince1970
                        ])
                        showAlert("Test error logged successfully!")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.orange)
                    
                    // Set User Properties
                    Button("Set User Properties") {
                        FirebaseService.shared.setUserProperty("test_user", forKey: "user_type")
                        FirebaseService.shared.setUserProperty("1.0.0", forKey: "app_version")
                        showAlert("User properties set successfully!")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)
                    
                    // Set User ID
                    Button("Set User ID") {
                        FirebaseService.shared.setUserID("test_user_123")
                        showAlert("User ID set successfully!")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.green)
                    
                    // Trigger Test Crash
                    Button("Trigger Test Crash") {
                        FirebaseService.shared.triggerTestCrash()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
                    
                    // Manual Crash
                    Button("Manual Crash (Force Unwrap)") {
                        let optionalString: String? = nil
                        let _ = optionalString! // This will crash the app
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.purple)
                }
                .padding()
                
                Spacer()
                
                Text("Instructions:")
                    .font(.headline)
                    .padding(.top)
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("• Tap 'Log Test Event' to log custom events")
                    Text("• Tap 'Log Test Error' to log non-fatal errors")
                    Text("• Tap 'Set User Properties' to add custom keys")
                    Text("• Tap 'Set User ID' to identify users in crashes")
                    Text("• Tap 'Trigger Test Crash' for controlled crash")
                    Text("• Tap 'Manual Crash' for unexpected crash")
                }
                .font(.caption)
                .foregroundColor(.secondary)
                .padding()
            }
            .padding()
            .navigationTitle("Crashlytics Test")
            .alert("Crashlytics", isPresented: $showingAlert) {
                Button("OK") { }
            } message: {
                Text(alertMessage)
            }
        }
    }
    
    private func showAlert(_ message: String) {
        alertMessage = message
        showingAlert = true
    }
}

#Preview {
    CrashTestView()
} 