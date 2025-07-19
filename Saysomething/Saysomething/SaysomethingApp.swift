//
//  SaysomethingApp.swift
//  Saysomething
//
//  Created by user278797 on 6/6/25.
//

import SwiftUI

@main
struct SaysomethingApp: App {
    @StateObject private var loginViewModel = LoginViewModel()
    
    init() {
        // Initialize Firebase and Crashlytics
        FirebaseService.shared.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            if loginViewModel.isLoggedIn {
                HomeView()
            } else {
                LoginView()
                    .environmentObject(loginViewModel)
            }
        }
    }
} 