import Foundation
import CoreData

extension DraftPost {
    @nonobjc public class func fetchRequest() -> NSFetchRequest<DraftPost> {
        return NSFetchRequest<DraftPost>(entityName: "DraftPost")
    }

    @NSManaged public var id: UUID
    @NSManaged public var content: String?
    @NSManaged public var createdAt: Date
    @NSManaged public var updatedAt: Date?
    @NSManaged public var syncStatus: String?
} 