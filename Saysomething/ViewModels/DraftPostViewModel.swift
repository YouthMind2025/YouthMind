import Foundation
import CoreData
import Combine

class DraftPostViewModel: ObservableObject {
    @Published var drafts: [DraftPost] = []
    @Published var syncMessage: String? = nil
    private var context: NSManagedObjectContext
    private var cancellables = Set<AnyCancellable>()
    
    init(context: NSManagedObjectContext) {
        self.context = context
        fetchDrafts()
        // Observe network changes
        NetworkMonitor.shared.$isConnected
            .sink { [weak self] isConnected in
                if isConnected {
                    self?.syncPendingDrafts()
                }
            }
            .store(in: &cancellables)
        // Optionally observe app resume (ScenePhase) in the View and call syncPendingDrafts()
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
        if NetworkMonitor.shared.isConnected {
            draft.syncStatus = "synced"
            // Try to send to backend immediately
            sendDraftToBackend(draft)
        } else {
            draft.syncStatus = "pending"
            syncMessage = "No internet. Post saved locally and will sync when online."
        }
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
    
    func syncPendingDrafts() {
        let request: NSFetchRequest<DraftPost> = DraftPost.fetchRequest()
        request.predicate = NSPredicate(format: "syncStatus == %@", "pending")
        do {
            let pendingDrafts = try context.fetch(request)
            for draft in pendingDrafts {
                sendDraftToBackend(draft)
            }
        } catch {
            print("Failed to fetch pending drafts: \(error)")
        }
    }

    private func sendDraftToBackend(_ draft: DraftPost) {
        guard let content = draft.content, let emotionTag = draft.value(forKey: "emotionTag") as? String else {
            syncMessage = "Draft missing content or emotion tag."
            return
        }
        PostAPI.createPost(content: content, emotionTag: emotionTag)
            .sink(receiveCompletion: { [weak self] completion in
                switch completion {
                case .finished:
                    break
                case .failure(let error):
                    DispatchQueue.main.async {
                        self?.syncMessage = "Failed to sync draft: \(error.localizedDescription)"
                    }
                }
            }, receiveValue: { [weak self] success in
                if success {
                    DispatchQueue.main.async {
                        self?.context.delete(draft)
                        self?.syncMessage = "Draft synced successfully."
                        self?.saveContext()
                    }
                } else {
                    DispatchQueue.main.async {
                        self?.syncMessage = "Failed to sync draft."
                    }
                }
            })
            .store(in: &cancellables)
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