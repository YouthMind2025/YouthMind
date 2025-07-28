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
    let persistenceController = PersistenceController.shared
    var body: some Scene {
        WindowGroup {
            if loginViewModel.isLoggedIn {
                HomeView()
                    .environment(\.managedObjectContext, persistenceController.container.viewContext)
            } else {
                LoginView()
                    .environmentObject(loginViewModel)
                    .environment(\.managedObjectContext, persistenceController.container.viewContext)
            }
        }
    }
} 