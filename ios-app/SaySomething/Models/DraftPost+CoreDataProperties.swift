//
//  DraftPost+CoreDataProperties.swift
//  SaySomething
//
//  Created by Dawei Bao on 02/08/2025.
//
//

import Foundation
import CoreData


extension DraftPost {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<DraftPost> {
        return NSFetchRequest<DraftPost>(entityName: "DraftPost")
    }

    @NSManaged public var id: UUID?
    @NSManaged public var content: String?
    @NSManaged public var emotionTag: String?
    @NSManaged public var timestamp: Date?
    @NSManaged public var topics: String?

}

extension DraftPost : Identifiable {

}
