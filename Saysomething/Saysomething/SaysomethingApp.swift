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