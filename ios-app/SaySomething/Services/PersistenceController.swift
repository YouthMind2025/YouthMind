import CoreData

struct PersistenceController {
    static let shared = PersistenceController()
    
    let container: NSPersistentContainer
    
    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "SaySomething")
        
        if inMemory {
            container.persistentStoreDescriptions.first!.url = URL(fileURLWithPath: "/dev/null")
        }
        
        // 创建局部变量来避免在闭包中捕获self
        let persistentContainer = container
        
        container.loadPersistentStores { description, error in
            if let error = error {
                // 记录错误但不崩溃应用
                print("❌ Core Data load error: \(error.localizedDescription)")
                
                // 尝试恢复：删除损坏的存储文件并重新创建
                if let storeURL = description.url {
                    do {
                        try FileManager.default.removeItem(at: storeURL)
                        try persistentContainer.persistentStoreCoordinator.addPersistentStore(
                            ofType: description.type,
                            configurationName: description.configuration,
                            at: storeURL,
                            options: description.options
                        )
                        print("✅ Core Data store recreated successfully")
                    } catch {
                        print("❌ Failed to recreate store: \(error.localizedDescription)")
                        // 即使恢复失败，也不应该崩溃应用
                        // 可以显示错误提示给用户，让用户重启应用
                    }
                }
            }
        }
        
        container.viewContext.automaticallyMergesChangesFromParent = true
    }
} 