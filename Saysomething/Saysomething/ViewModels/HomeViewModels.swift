//
//  HomeViewModels.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI
import Combine

class HomeViewModel: ObservableObject {
    @Published var title: String = "Welcome to Home"
    @Published var isLoading: Bool = false
    @Published var posts: [Post] = []
    @Published var loginStatus: String = ""
    @Published var errorMessage: String? = nil
    private var cancellables = Set<AnyCancellable>()
    
    // test token data
    init() {
        // mock login and save token
        let _ = TokenManager.shared.saveToken("mock_jwt_token_123456")
        // get token on launch
        if let token = TokenManager.shared.getToken() {
            print("[Keychain] Token on launch: \(token)")
        } else {
            print("[Keychain] No token found on launch.")
        }
    }

    func login(email: String, password: String) {
        AuthAPI.login(email: email, password: password)
            .sink(receiveCompletion: { completion in
                switch completion {
                case .failure(let error):
                    if let sessionError = error as? SessionExpiredError, sessionError == .sessionExpired {
                        self.errorMessage = "Session expired. Please log in again."
                    } else {
                        self.errorMessage = "Login failed: \(error)"
                    }
                case .finished:
                    break
                }
            }, receiveValue: { response in
                TokenManager.shared.saveToken(response.data.token)
                self.loginStatus = "Login successful!"
            })
            .store(in: &cancellables)
    }

    func fetchPosts() {
        PostAPI.fetchPosts()
            .sink(receiveCompletion: { completion in
                switch completion {
                case .failure(let error):
                    if let sessionError = error as? SessionExpiredError, sessionError == .sessionExpired {
                        self.errorMessage = "Session expired. Please log in again."
                    } else {
                        self.errorMessage = "Failed to fetch posts: \(error)"
                    }
                case .finished:
                    break
                }
            }, receiveValue: { response in
                self.posts = response.data
            })
            .store(in: &cancellables)
    }
}

// MARK: - LoginViewModel

class LoginViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var otp: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var isLoggedIn: Bool = false
    
    var registerService: LoginService = LoginService.shared
    private var cancellables = Set<AnyCancellable>()
    
    func register() {
        isLoading = true
        errorMessage = nil
        registerService.register(email: email, otp: otp)
            .sink { [weak self] completion in
                self?.isLoading = false
                if case let .failure(error) = completion {
                    self?.errorMessage = "Registration failed: \(error)"
                }
            } receiveValue: { [weak self] response in
                // Save JWT token
                let saved = TokenManager.shared.saveToken(response.token)
                // Validate expiration
                if let expiresAt = self?.parseDate(response.expiresAt), expiresAt > Date() {
                    self?.isLoggedIn = saved
                    self?.errorMessage = nil
                    // Optionally clear success after a delay
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        self?.isLoggedIn = false
                    }
                } else {
                    self?.errorMessage = "Token expired or invalid."
                }
            }
            .store(in: &cancellables)
    }
    
    private func parseDate(_ dateString: String) -> Date? {
        // Try ISO8601 first, fallback to yyyy-MM-dd
        let isoFormatter = ISO8601DateFormatter()
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: dateString)
    }
} 