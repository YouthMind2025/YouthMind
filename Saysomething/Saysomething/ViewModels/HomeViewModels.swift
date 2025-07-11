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
    @Published var successMessage: String? = nil
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Check token status on launch
        if let token = TokenManager.shared.getToken() {
            print("[Keychain] Token on launch: \(token)")
        } else {
            print("[Keychain] No token found on launch.")
        }
    }

    func login(email: String, password: String) {
        isLoading = true
        errorMessage = nil
        successMessage = nil
        
        AuthAPI.login(email: email, password: password)
            .sink(receiveCompletion: { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    if let apiError = error as? APIError {
                        self?.errorMessage = apiError.errorDescription
                    } else {
                        self?.errorMessage = "Login failed: \(error.localizedDescription)"
                    }
                case .finished:
                    break
                }
            }, receiveValue: { [weak self] response in
                TokenManager.shared.saveToken(response.data.token)
                self?.loginStatus = "Login successful!"
                self?.successMessage = "Login successful!"
                // Clear success message
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    self?.successMessage = nil
                }
            })
            .store(in: &cancellables)
    }

    func fetchPosts() {
        isLoading = true
        errorMessage = nil
        
        PostAPI.fetchPosts()
            .sink(receiveCompletion: { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    if let apiError = error as? APIError {
                        self?.errorMessage = apiError.errorDescription
                    } else {
                        self?.errorMessage = "Failed to fetch posts: \(error.localizedDescription)"
                    }
                case .finished:
                    break
                }
            }, receiveValue: { [weak self] response in
                self?.posts = response.data
            })
            .store(in: &cancellables)
    }
    
    func createPost(title: String, content: String) {
        isLoading = true
        errorMessage = nil
        successMessage = nil
        
        PostAPI.createPost(title: title, content: content)
            .sink(receiveCompletion: { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    if let apiError = error as? APIError {
                        self?.errorMessage = apiError.errorDescription
                    } else {
                        self?.errorMessage = "Failed to create post: \(error.localizedDescription)"
                    }
                case .finished:
                    break
                }
            }, receiveValue: { [weak self] response in
                self?.successMessage = "Post created successfully!"
                // Refresh posts list
                self?.fetchPosts()
                // Clear success message
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    self?.successMessage = nil
                }
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
    @Published var successMessage: String? = nil
    @Published var isLoggedIn: Bool = false
    
    private var cancellables = Set<AnyCancellable>()
    
    func register() {
        isLoading = true
        errorMessage = nil
        successMessage = nil
        
        AuthAPI.register(email: email, otp: otp)
            .sink { [weak self] completion in
                self?.isLoading = false
                switch completion {
                case .failure(let error):
                    if let apiError = error as? APIError {
                        self?.errorMessage = apiError.errorDescription
                    } else {
                        self?.errorMessage = "Registration failed: \(error.localizedDescription)"
                    }
                case .finished:
                    break
                }
            } receiveValue: { [weak self] response in
                // Save JWT token
                let saved = TokenManager.shared.saveToken(response.data.token)
                // Validate expiration time
                if let expiresAt = self?.parseDate(response.data.expiresAt), expiresAt > Date() {
                    self?.isLoggedIn = saved
                    self?.successMessage = "Registration successful!"
                    self?.errorMessage = nil
                    // Delay clearing success message
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                        self?.successMessage = nil
                        self?.isLoggedIn = false
                    }
                } else {
                    self?.errorMessage = "Token expired or invalid"
                }
            }
            .store(in: &cancellables)
    }
    
    private func parseDate(_ dateString: String) -> Date? {
        // Try ISO8601 format first, fallback to yyyy-MM-dd
        let isoFormatter = ISO8601DateFormatter()
        if let date = isoFormatter.date(from: dateString) {
            return date
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: dateString)
    }
} 