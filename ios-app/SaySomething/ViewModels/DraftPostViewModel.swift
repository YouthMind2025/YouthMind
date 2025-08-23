import Foundation
import CoreData
import Combine

class DraftPostViewModel: ObservableObject {
    @Published var drafts: [DraftPost] = []
    private var context: NSManagedObjectContext
    private var cancellables = Set<AnyCancellable>()
    
    init(context: NSManagedObjectContext) {
        self.context = context
        fetchDrafts()
    }
    
    func fetchDrafts() {
        let request: NSFetchRequest<DraftPost> = DraftPost.fetchRequest()
        do {
            drafts = try context.fetch(request)
        } catch {
            print("Failed to fetch drafts: \(error)")
        }
    }
    
    func addDraft(content: String, emotionTag: String) {
        let draft = DraftPost(context: context)
        draft.id = UUID()
        draft.content = content
        draft.timestamp = Date()
        draft.emotionTag = emotionTag
        saveContext()
    }
    
    func updateDraft(_ draft: DraftPost, content: String, emotionTag: String) {
        draft.content = content
        draft.emotionTag = emotionTag
        draft.timestamp = Date()
        saveContext()
    }
    
    func deleteDraft(_ draft: DraftPost) {
        context.delete(draft)
        saveContext()
    }
    
    private func saveContext() {
        do {
            try context.save()
            fetchDrafts()
        } catch {
            print("Failed to save context: \(error)")
        }
    }
} 
