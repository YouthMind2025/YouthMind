//
//  HomeViewModels.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI

class HomeViewModel: ObservableObject {
    @Published var title: String = "Welcome to Home"
    @Published var isLoading: Bool = false
    
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
}
