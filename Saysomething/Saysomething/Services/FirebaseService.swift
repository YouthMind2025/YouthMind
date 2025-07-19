//
//  FirebaseService.swift
//  Saysomething
//
//  Created by Firebase Integration
//

import Foundation
import Firebase
import FirebaseCrashlytics

class FirebaseService {
    static let shared = FirebaseService()
    
    private init() {}
    
    func configure() {
        FirebaseApp.configure()
        
        // Enable Crashlytics debug mode for development
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif
        
        // Set user identifier for crash reports
        if let userId = UserDefaults.standard.string(forKey: "user_id") {
            Crashlytics.crashlytics().setUserID(userId)
        }
        
        // Log app launch
        Crashlytics.crashlytics().log("App launched successfully")
    }
    
    // MARK: - Crashlytics Helper Methods
    
    func logEvent(_ message: String) {
        Crashlytics.crashlytics().log(message)
    }
    
    func logError(_ error: Error, additionalUserInfo: [String: Any]? = nil) {
        if let additionalInfo = additionalUserInfo {
            Crashlytics.crashlytics().record(error: error, userInfo: additionalInfo)
        } else {
            Crashlytics.crashlytics().record(error: error)
        }
    }
    
    func setUserProperty(_ value: String, forKey key: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }
    
    func setUserID(_ userID: String) {
        Crashlytics.crashlytics().setUserID(userID)
        UserDefaults.standard.set(userID, forKey: "user_id")
    }
    
    // MARK: - Test Crash Method
    
    func triggerTestCrash() {
        Crashlytics.crashlytics().log("Triggering test crash")
        Crashlytics.crashlytics().crash()
    }
} 