//
//  FeedCacheService.swift
//  SaySomething

import Foundation
import os.log

/// Service for caching feed blocks locally for offline viewing
/// Provides persistent storage and retrieval of feed blocks with expiration handling
final class FeedCacheService {
    
    // MARK: - Properties
    static let shared = FeedCacheService()
    
    private let fileManager = FileManager.default
    private let cacheDirectory: URL
    private let logger = Logger(subsystem: "com.youthmind.feed", category: "FeedCacheService")
    
    // Cache configuration
    private let maxCacheSize: Int = 50 // Maximum number of blocks to cache
    private let cacheExpirationHours: Int = 24 // Cache expires after 24 hours
    
    // MARK: - Initialization
    private init() {
        // Create cache directory in Documents
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        self.cacheDirectory = documentsDirectory.appendingPathComponent("FeedCache")
        
        // Create cache directory if it doesn't exist
        createCacheDirectoryIfNeeded()
        
        // Clean up expired cache on initialization
        Task {
            await clearExpiredCache()
        }
    }
    
    // MARK: - Public Methods
    
    /// Cache a feed block with posts
    /// - Parameters:
    ///   - blockNumber: The block number to cache
    ///   - posts: Array of posts in the block
    ///   - metadata: Optional block metadata
    func cacheBlock(_ blockNumber: Int, posts: [Post], metadata: BlockMetadata? = nil) async throws {
        let cacheItem = FeedBlockCacheItem(
            blockNumber: blockNumber,
            posts: posts,
            metadata: metadata,
            cachedAt: Date()
        )
        
        let fileName = "block_\(blockNumber).json"
        let fileURL = cacheDirectory.appendingPathComponent(fileName)
        
        do {
            let data = try JSONEncoder().encode(cacheItem)
            try data.write(to: fileURL, options: [.atomic])
            
            logger.debug("Cached block \(blockNumber) with \(posts.count) posts")
            
            // Clean up old cache if we're over the limit
            await cleanupOldCache()
            
        } catch {
            logger.error("Failed to cache block \(blockNumber): \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Retrieve a cached feed block
    /// - Parameter blockNumber: The block number to retrieve
    /// - Returns: Array of posts if cached and not expired, nil otherwise
    func getCachedBlock(_ blockNumber: Int) -> [Post]? {
        let fileName = "block_\(blockNumber).json"
        let fileURL = cacheDirectory.appendingPathComponent(fileName)
        
        guard fileManager.fileExists(atPath: fileURL.path) else {
            return nil
        }
        
        do {
            let data = try Data(contentsOf: fileURL)
            let cacheItem = try JSONDecoder().decode(FeedBlockCacheItem.self, from: data)
            
            // Check if cache is expired
            if cacheItem.isExpired(expirationHours: cacheExpirationHours) {
                logger.debug("Block \(blockNumber) cache expired, removing")
                try? fileManager.removeItem(at: fileURL)
                return nil
            }
            
            logger.debug("Retrieved cached block \(blockNumber) with \(cacheItem.posts.count) posts")
            return cacheItem.posts
            
        } catch {
            logger.error("Failed to retrieve cached block \(blockNumber): \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Get cached block metadata
    /// - Parameter blockNumber: The block number to retrieve metadata for
    /// - Returns: Block metadata if available, nil otherwise
    func getCachedBlockMetadata(_ blockNumber: Int) -> BlockMetadata? {
        let fileName = "block_\(blockNumber).json"
        let fileURL = cacheDirectory.appendingPathComponent(fileName)
        
        guard fileManager.fileExists(atPath: fileURL.path) else {
            return nil
        }
        
        do {
            let data = try Data(contentsOf: fileURL)
            let cacheItem = try JSONDecoder().decode(FeedBlockCacheItem.self, from: data)
            return cacheItem.metadata
            
        } catch {
            logger.error("Failed to retrieve cached block metadata \(blockNumber): \(error.localizedDescription)")
            return nil
        }
    }
    
    /// Clear all expired cache entries
    func clearExpiredCache() async {
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: [.creationDateKey])
            
            for fileURL in fileURLs {
                guard fileURL.pathExtension == "json" else { continue }
                
                do {
                    let data = try Data(contentsOf: fileURL)
                    let cacheItem = try JSONDecoder().decode(FeedBlockCacheItem.self, from: data)
                    
                    if cacheItem.isExpired(expirationHours: cacheExpirationHours) {
                        try fileManager.removeItem(at: fileURL)
                        logger.debug("Removed expired cache file: \(fileURL.lastPathComponent)")
                    }
                } catch {
                    // If we can't decode the file, remove it as it's likely corrupted
                    try? fileManager.removeItem(at: fileURL)
                    logger.debug("Removed corrupted cache file: \(fileURL.lastPathComponent)")
                }
            }
        } catch {
            logger.error("Failed to clear expired cache: \(error.localizedDescription)")
        }
    }
    
    /// Clear all cached blocks
    func clearAllCache() async throws {
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: nil)
            
            for fileURL in fileURLs {
                try fileManager.removeItem(at: fileURL)
            }
            
            logger.info("Cleared all feed cache")
        } catch {
            logger.error("Failed to clear all cache: \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Get cache statistics
    /// - Returns: Cache statistics including size and expiration info
    func getCacheStatistics() -> CacheStatistics {
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: [.fileSizeKey, .creationDateKey])
            
            var totalSize: Int64 = 0
            var blockCount = 0
            var expiredCount = 0
            
            for fileURL in fileURLs {
                guard fileURL.pathExtension == "json" else { continue }
                
                let resourceValues = try fileURL.resourceValues(forKeys: [.fileSizeKey, .creationDateKey])
                totalSize += Int64(resourceValues.fileSize ?? 0)
                blockCount += 1
                
                // Check if this block is expired
                do {
                    let data = try Data(contentsOf: fileURL)
                    let cacheItem = try JSONDecoder().decode(FeedBlockCacheItem.self, from: data)
                    if cacheItem.isExpired(expirationHours: cacheExpirationHours) {
                        expiredCount += 1
                    }
                } catch {
                    // Count as expired if we can't decode
                    expiredCount += 1
                }
            }
            
            return CacheStatistics(
                totalBlocks: blockCount,
                expiredBlocks: expiredCount,
                totalSizeBytes: totalSize,
                cacheDirectory: cacheDirectory.path
            )
            
        } catch {
            logger.error("Failed to get cache statistics: \(error.localizedDescription)")
            return CacheStatistics(
                totalBlocks: 0,
                expiredBlocks: 0,
                totalSizeBytes: 0,
                cacheDirectory: cacheDirectory.path
            )
        }
    }
    
    // MARK: - Private Methods
    
    private func createCacheDirectoryIfNeeded() {
        if !fileManager.fileExists(atPath: cacheDirectory.path) {
            do {
                try fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
                logger.info("Created feed cache directory: \(cacheDirectory.path)")
            } catch {
                logger.error("Failed to create cache directory: \(error.localizedDescription)")
            }
        }
    }
    
    private func cleanupOldCache() async {
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: [.creationDateKey])
            
            // Sort by creation date (oldest first)
            let sortedFiles = fileURLs.sorted { file1, file2 in
                let date1 = (try? file1.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                let date2 = (try? file2.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                return date1 < date2
            }
            
            // Remove oldest files if we're over the limit
            if sortedFiles.count > maxCacheSize {
                let filesToRemove = sortedFiles.prefix(sortedFiles.count - maxCacheSize)
                for fileURL in filesToRemove {
                    try fileManager.removeItem(at: fileURL)
                    logger.debug("Removed old cache file: \(fileURL.lastPathComponent)")
                }
            }
        } catch {
            logger.error("Failed to cleanup old cache: \(error.localizedDescription)")
        }
    }
}

// MARK: - Supporting Types

/// Internal cache item structure
private struct FeedBlockCacheItem: Codable {
    let blockNumber: Int
    let posts: [Post]
    let metadata: BlockMetadata?
    let cachedAt: Date
    
    func isExpired(expirationHours: Int) -> Bool {
        let expirationTime = cachedAt.addingTimeInterval(TimeInterval(expirationHours * 3600))
        return Date() > expirationTime
    }
}

/// Cache statistics information
struct CacheStatistics {
    let totalBlocks: Int
    let expiredBlocks: Int
    let totalSizeBytes: Int64
    let cacheDirectory: String
    
    var totalSizeMB: Double {
        return Double(totalSizeBytes) / (1024 * 1024)
    }
    
    var activeBlocks: Int {
        return totalBlocks - expiredBlocks
    }
}
