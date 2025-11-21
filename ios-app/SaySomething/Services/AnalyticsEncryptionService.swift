import Foundation
import CryptoKit
import os.log

// MARK: - Analytics Encryption Service
/// Provides encryption and decryption services for sensitive analytics data
final class AnalyticsEncryptionService {
    
    // MARK: - Properties
    private let logger = Logger(subsystem: "com.youthmind.analytics", category: "AnalyticsEncryptionService")
    private let queue = DispatchQueue(label: "com.youthmind.encryption.service", qos: .background)
    
    // Encryption key management
    private var encryptionKey: SymmetricKey?
    private let keychain = KeychainManager()
    
    // MARK: - Initialization
    init() {
        setupEncryptionKey()
    }
    
    // MARK: - Public Methods
    
    /// Encrypt data
    func encrypt(_ data: Data) -> Data? {
        guard let key = encryptionKey else {
            logger.error("Encryption key not available")
            return nil
        }
        
        do {
            let sealedBox = try AES.GCM.seal(data, using: key)
            return sealedBox.combined
        } catch {
            logger.error("Failed to encrypt data: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Decrypt data
    func decrypt(_ encryptedData: Data) -> Data? {
        guard let key = encryptionKey else {
            logger.error("Encryption key not available")
            return nil
        }
        
        do {
            let sealedBox = try AES.GCM.SealedBox(combined: encryptedData)
            return try AES.GCM.open(sealedBox, using: key)
        } catch {
            logger.error("Failed to decrypt data: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Encrypt string
    func encryptString(_ string: String) -> String? {
        guard let data = string.data(using: .utf8),
              let encryptedData = encrypt(data) else {
            return nil
        }
        
        return encryptedData.base64EncodedString()
    }
    
    /// Decrypt string
    func decryptString(_ encryptedString: String) -> String? {
        guard let encryptedData = Data(base64Encoded: encryptedString),
              let decryptedData = decrypt(encryptedData),
              let string = String(data: decryptedData, encoding: .utf8) else {
            return nil
        }
        
        return string
    }
    
    /// Encrypt analytics data
    func encryptAnalyticsData<T: Codable>(_ data: T) -> Data? {
        do {
            let jsonData = try JSONEncoder().encode(data)
            return encrypt(jsonData)
        } catch {
            logger.error("Failed to encode analytics data: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Decrypt analytics data
    func decryptAnalyticsData<T: Codable>(_ encryptedData: Data, as type: T.Type) -> T? {
        guard let decryptedData = decrypt(encryptedData) else {
            return nil
        }
        
        do {
            return try JSONDecoder().decode(type, from: decryptedData)
        } catch {
            logger.error("Failed to decode analytics data: \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Generate new encryption key
    func generateNewKey() -> Bool {
        // Store current key as old key for rotation
        if let currentKey = encryptionKey {
            do {
                try keychain.store(key: currentKey, forKey: "AnalyticsEncryptionKey_Old")
            } catch {
                logger.warning("Failed to store old key for rotation: \(error.localizedDescription)")
            }
        }
        
        let newKey = SymmetricKey(size: .bits256)
        
        do {
            try keychain.store(key: newKey, forKey: "AnalyticsEncryptionKey")
            encryptionKey = newKey
            setLastKeyRotationDate(Date())
            logger.info("New encryption key generated and stored")
            return true
        } catch {
            logger.error("Failed to store new encryption key: \(error.localizedDescription)")
            return false
        }
    }
    
    /// Rotate encryption key
    func rotateKey() -> Bool {
        // Generate new key
        guard generateNewKey() else {
            return false
        }
        
        // Implement key rotation logic for existing encrypted data
        await rotateExistingEncryptedData()
        logger.info("Encryption key rotated successfully")
        return true
    }
    
    /// Get encryption status
    func getEncryptionStatus() -> EncryptionStatus {
        return EncryptionStatus(
            isEncryptionEnabled: encryptionKey != nil,
            keyExists: encryptionKey != nil,
            lastKeyRotation: getLastKeyRotationDate(),
            encryptionAlgorithm: "AES-GCM-256"
        )
    }
    
    // MARK: - Private Methods
    
    private func setupEncryptionKey() {
        do {
            if let existingKey = try keychain.retrieveKey(forKey: "AnalyticsEncryptionKey") {
                encryptionKey = existingKey
                logger.info("Loaded existing encryption key")
            } else {
                // Generate new key if none exists
                generateNewKey()
            }
        } catch {
            logger.error("Failed to setup encryption key: \(error.localizedDescription)")
        }
    }
    
    private func getLastKeyRotationDate() -> Date? {
        return UserDefaults.standard.object(forKey: "AnalyticsLastKeyRotation") as? Date
    }
    
    private func setLastKeyRotationDate(_ date: Date) {
        UserDefaults.standard.set(date, forKey: "AnalyticsLastKeyRotation")
    }
    
    private func rotateExistingEncryptedData() async {
        // Get the old key for decryption
        guard let oldKey = try? keychain.retrieveKey(forKey: "AnalyticsEncryptionKey_Old") else {
            logger.info("No old key found for rotation")
            return
        }
        
        // Rotate encrypted analytics data files
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let analyticsFiles = [
            "analytics_error_data.json",
            "analytics_behavior_data.json", 
            "analytics_frequency_data.json",
            "analytics_crash_data.json",
            "analytics_performance_data.json",
            "analytics_dashboard_data.json"
        ]
        
        for fileName in analyticsFiles {
            let fileURL = documentsDirectory.appendingPathComponent(fileName)
            
            guard FileManager.default.fileExists(atPath: fileURL.path) else { continue }
            
            do {
                // Read encrypted data
                let encryptedData = try Data(contentsOf: fileURL)
                
                // Decrypt with old key
                let sealedBox = try AES.GCM.SealedBox(combined: encryptedData)
                let decryptedData = try AES.GCM.open(sealedBox, using: oldKey)
                
                // Encrypt with new key
                if let newEncryptedData = encrypt(decryptedData) {
                    try newEncryptedData.write(to: fileURL, options: [.atomic])
                    logger.debug("Rotated encryption for \(fileName)")
                }
                
            } catch {
                logger.error("Failed to rotate encryption for \(fileName): \(error.localizedDescription)")
            }
        }
        
        // Clean up old key
        try? keychain.deleteKey(forKey: "AnalyticsEncryptionKey_Old")
    }
}

// MARK: - Keychain Manager
class KeychainManager {
    
    private let service = "com.youthmind.analytics"
    
    func store(key: SymmetricKey, forKey keyName: String) throws {
        let data = key.withUnsafeBytes { Data($0) }
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: keyName,
            kSecValueData as String: data
        ]
        
        // Delete existing item
        SecItemDelete(query as CFDictionary)
        
        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.storeFailed(status)
        }
    }
    
    func retrieveKey(forKey keyName: String) throws -> SymmetricKey? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: keyName,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data else {
            if status == errSecItemNotFound {
                return nil
            }
            throw KeychainError.retrieveFailed(status)
        }
        
        return SymmetricKey(data: data)
    }
    
    func deleteKey(forKey keyName: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: keyName
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deleteFailed(status)
        }
    }
}

// MARK: - Keychain Errors
enum KeychainError: Error {
    case storeFailed(OSStatus)
    case retrieveFailed(OSStatus)
    case deleteFailed(OSStatus)
    
    var localizedDescription: String {
        switch self {
        case .storeFailed(let status):
            return "Failed to store key in keychain: \(status)"
        case .retrieveFailed(let status):
            return "Failed to retrieve key from keychain: \(status)"
        case .deleteFailed(let status):
            return "Failed to delete key from keychain: \(status)"
        }
    }
}

// MARK: - Encryption Models

/// Encryption status
struct EncryptionStatus: Codable {
    let isEncryptionEnabled: Bool
    let keyExists: Bool
    let lastKeyRotation: Date?
    let encryptionAlgorithm: String
}
