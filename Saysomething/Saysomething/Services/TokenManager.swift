import Foundation
import Security

class TokenManager {
    static let shared = TokenManager()
    private let service = "com.yourapp.saysomething"    // service name
    private let account = "sessionToken"                // account name
    
    private init() {}       // singleton design pattern
    
    func saveToken(_ token: String) -> Bool {
        // convert token to Data as keychain only accepts Data
        guard let data = token.data(using: .utf8) else { return false }
        // create query to find existing token
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        // delete existing token
        SecItemDelete(query as CFDictionary)
        // create attributes to save token
        let attributes: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data
        ]
        // Keychain API
        let status = SecItemAdd(attributes as CFDictionary, nil)
        return status == errSecSuccess
    }
    
    func getToken() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        // Keychain API and save token to dataTypeRef
        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
        
        if status == errSecSuccess, let data = dataTypeRef as? Data, let token = String(data: data, encoding: .utf8) {
            return token
        }
        return nil
    }
    
    func deleteToken() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        // Keychain API
        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess
    }

    
    // Validate token expiration using expiresAt (ISO8601 string)
    func isTokenExpired(expiresAt: String) -> Bool {
        let formatter = ISO8601DateFormatter()
        guard let expiryDate = formatter.date(from: expiresAt) else { return true }
        return Date() >= expiryDate
    }
    
    // Check if token exists and is valid
    func isTokenValid() -> Bool {
        guard let token = getToken(), !token.isEmpty else {
            return false
        }
        return true
    }
    
    // Clear token (for logout or token errors)
    func clearToken() {
        _ = deleteToken()
    }
    
    // Handle token errors
    func handleTokenError() {
        clearToken()
        // Can add notifications or other handling logic here
        print("[TokenManager] Token error detected, cleared token")
    }
}
 frontend-together
