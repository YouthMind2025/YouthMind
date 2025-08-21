//
//  SaySomethingApp.swift
//  SaySomething
//
//  Created by Dawei Bao on 01/08/2025.
//

import SwiftUI
import CoreData
import UIKit

@main
struct SaySomethingApp: App {
    let persistenceController = PersistenceController.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
                .tint(DesignConstants.ColorPalette.brandPurple)
                .onAppear {
                    // Set global appearance for UIKit components used under the hood
                    UINavigationBar.appearance().tintColor = UIColor(DesignConstants.ColorPalette.brandPurple)
                    UITabBar.appearance().tintColor = UIColor(DesignConstants.ColorPalette.brandPurple)
                    UISegmentedControl.appearance().selectedSegmentTintColor = UIColor(DesignConstants.ColorPalette.brandPurple)
                    UIButton.appearance(whenContainedInInstancesOf: [UINavigationBar.self]).tintColor = UIColor(DesignConstants.ColorPalette.brandPurple)
                }
        }
    }
}
