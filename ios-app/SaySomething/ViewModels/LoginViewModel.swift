import SwiftUI
import Combine

class LoginViewModel: ObservableObject {
    enum AuthMode: String, CaseIterable, Identifiable {
        case login = "Login"
        case register = "Register"
        var id: String { rawValue }
    }

    @Published var email: String = ""
    @Published var otp: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var successMessage: String? = nil
    @Published var isLoggedIn: Bool = false
    @Published var showRetryAlert = false
    @Published var retryAction: (() -> Void)?
    @Published var mode: AuthMode = .login
    @Published var isCodeSent: Bool = false
    @Published var resendSecondsRemaining: Int = 0
    
    private var cancellables = Set<AnyCancellable>()
    private var resendTimer: AnyCancellable?
    
    func register() {
        // Reset messages
        errorMessage = nil
        successMessage = nil
        isLoading = true
        
        // Simulate API call with delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
            self?.isLoading = false
            
            // Simple validation
            guard let email = self?.email, !email.isEmpty else {
                self?.errorMessage = "Email cannot be empty"
                return
            }
            
            guard let otp = self?.otp, otp.count == 6 else {
                self?.errorMessage = "OTP must be 6 digits"
                return
            }
            
            // Simulate successful registration
            self?.isLoggedIn = true
            self?.successMessage = self?.mode == .login ? "Login successful!" : "Registration successful!"
            
            // Clear success message after 3 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                self?.successMessage = nil
            }
        }
    }
    
    func sendCode() {
        guard !email.isEmpty else {
            errorMessage = "Please enter your email first"
            return
        }
        errorMessage = nil
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            guard let self = self else { return }
            self.isLoading = false
            self.isCodeSent = true
            self.startResendCountdown(seconds: 60)
            self.successMessage = "Verification code sent"
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
                self?.successMessage = nil
            }
        }
    }

    func resendCode() {
        guard resendSecondsRemaining == 0 else { return }
        sendCode()
    }

    private func startResendCountdown(seconds: Int) {
        resendSecondsRemaining = seconds
        resendTimer?.cancel()
        resendTimer = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                if self.resendSecondsRemaining > 0 {
                    self.resendSecondsRemaining -= 1
                } else {
                    self.resendTimer?.cancel()
                }
            }
    }

    func retryLastAction() {
        retryAction?()
        showRetryAlert = false
        retryAction = nil
    }
    
    private func handleNoInternetConnection() {
        errorMessage = "No internet connection - please check your network"
    }
} 