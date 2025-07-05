import Foundation

class UserIdentityManager {
    static let shared = UserIdentityManager()
    private let userDefaultsKey = "com.saysomething.userUUID"
    private(set) var userID: String
    
    private init() {
        if let savedUUID = UserDefaults.standard.string(forKey: userDefaultsKey) {
            userID = savedUUID
        } else {
            let newUUID = UUID().uuidString
            userID = newUUID
            UserDefaults.standard.set(newUUID, forKey: userDefaultsKey)
        }
    }
} 