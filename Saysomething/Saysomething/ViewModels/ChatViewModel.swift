//
//  ChatViewModel.swift
//  Saysomething
//
//  Created by user278797 on 7/11/25.
//

import SwiftUI
import Combine

// MARK: - Dynamic Chat View Model
// This class manages the chat functionality, including message handling, AI response generation, and UI updates.
class ChatViewModel: ObservableObject {
    // messages: the list of messages in the chat
    @Published var messages: [Message] = []
    // inputText: the text input by the user
    @Published var inputText: String = ""
    // isLoading: true if the chat is loading, false otherwise
    @Published var isLoading: Bool = false
    // suggestions: the list of suggestions for the user
    @Published var suggestions: [String] = []
    // showSuggestions: true if the suggestions are shown, false otherwise
    @Published var showSuggestions: Bool = false
    // connectionStatus: the status of the connection
    @Published var connectionStatus: ConnectionStatus = .connected
    
    private var cancellables = Set<AnyCancellable>()
    private var messagePublisher = PassthroughSubject<Message, Never>()
    private var responseTimer: Timer?
    
    // different type of connection status
    enum ConnectionStatus {
        case connected
        case connecting
        case disconnected
        
        var displayText: String {
            switch self {
            case .connected:
                return "Connected"
            case .connecting:
                return "Connecting..."
            case .disconnected:
                return "Disconnected"
            }
        }
        
        var color: Color {
            switch self {
            case .connected:
                return .green
            case .connecting:
                return .orange
            case .disconnected:
                return .red
            }
        }
    }
    
    init() {
        setupCombineBindings()
        loadMockData()
        startMessageSimulation()
    }
    
    deinit {
        responseTimer?.invalidate()
    }
    
    // MARK: - Combine Setup
    private func setupCombineBindings() {
        // Monitor input text changes for suggestions
        $inputText
            .debounce(for: .milliseconds(300), scheduler: RunLoop.main)
            .sink { [weak self] text in
                self?.updateSuggestions()
            }
            .store(in: &cancellables)
        
        // Monitor messages for auto-scroll and analytics
        $messages
            .sink { [weak self] messages in
                self?.handleMessagesUpdate(messages)
            }
            .store(in: &cancellables)
        
        // Setup message publisher for real-time updates
        messagePublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] message in
                self?.messages.append(message)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Message Handling
    func sendMessage() {
        guard !inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        
        let userMessage = Message.userMessage(inputText)
        let userInput = inputText
        inputText = ""
        showSuggestions = false
        
        // Add user message immediately
        messagePublisher.send(userMessage)
        
        // Simulate AI response with delay
        simulateAIResponse(to: userInput)
    }
    
    // send suggestion as message
    func sendSuggestion(_ suggestion: String) {
        inputText = suggestion
        sendMessage()
    }
    
    // simulate AI response
    private func simulateAIResponse(to userInput: String) {
        isLoading = true
        connectionStatus = .connecting
        
        // Simulate network delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.connectionStatus = .connected
            self.isLoading = false
            
            let aiResponse = self.generateAIResponse(to: userInput)
            let aiMessage = Message.aiMessage(aiResponse)
            
            // Add AI response
            self.messagePublisher.send(aiMessage)
            
            // Continue conversation loop if needed
            self.continueConversationLoop(userInput: userInput)
        }
    }
    
    // based on user input, continue the conversation
    private func continueConversationLoop(userInput: String) {
        // Simulate follow-up questions or suggestions
        let lowercasedInput = userInput.lowercased()
        
        if lowercasedInput.contains("anxiety") || lowercasedInput.contains("stress") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                let followUp = Message.aiMessage("You mentioned anxiety symptoms earlier. Do they occur frequently? We can work together to create a relief plan.")
                self.messagePublisher.send(followUp)
            }
        } else if lowercasedInput.contains("sleep") || lowercasedInput.contains("insomnia") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                let followUp = Message.aiMessage("Regarding sleep issues, what time do you usually go to bed? We can start by adjusting your sleep schedule.")
                self.messagePublisher.send(followUp)
            }
        }
    }
    
    // MARK: - AI Response Generation
    private func generateAIResponse(to userInput: String) -> String {
        let lowercasedInput = userInput.lowercased()
        
        // Context-aware responses with more dynamic content
        if lowercasedInput.contains("anxiety") || lowercasedInput.contains("stress") {
            let responses = [
                "I understand the feeling of anxiety. Let's work together to manage it:\n\n• Try the 4-7-8 breathing technique: inhale for 4 seconds, hold for 7, exhale for 8\n• Practice progressive muscle relaxation\n• Try mindfulness meditation\n• Maintain a regular routine\n\nWhich method would you like to try first?",
                "Anxiety is a very common emotional response. Here are some effective relief methods:\n\n1. Deep breathing exercises\n2. Progressive muscle relaxation\n3. Mindfulness meditation\n4. Regular exercise\n5. Adequate sleep\n\nWhich method would you like to try?",
                "I notice you mentioned anxiety. Let's start with a simple breathing exercise:\n\nTake a deep breath, feel the air entering your lungs, then exhale slowly. Repeat a few times and you'll feel better.\n\nHow are you feeling now?"
            ]
            return responses.randomElement() ?? "I'm here to support you."
        } else if lowercasedInput.contains("depression") || lowercasedInput.contains("sad") {
            let responses = [
                "Depression is a common mental health issue. Here are some suggestions:\n\n• Maintain a regular sleep schedule\n• Engage in moderate exercise\n• Stay connected with friends and family\n• Seek professional help\n\nRemember, seeking help is a brave act.",
                "I understand how you're feeling now. Depression can make you feel isolated, but remember you're not alone.\n\nI suggest:\n• Do some light exercise daily\n• Maintain a regular routine\n• Share your feelings with someone you trust\n\nWould you like to talk about your specific situation?"
            ]
            return responses.randomElement() ?? "I'm here to support you."
        } else if lowercasedInput.contains("pressure") || lowercasedInput.contains("stress") {
            let responses = [
                "Stress management is important. Here are some effective methods:\n\n• Time management: Create reasonable plans\n• Exercise and relaxation: walking, yoga, etc.\n• Social support: communicate with friends\n• Hobbies: develop personal interests\n\nWhat specific stress are you facing now?",
                "Stress is a common challenge in modern life. Let's manage it together:\n\n1. Identify stress sources\n2. Develop coping strategies\n3. Practice relaxation techniques\n4. Seek support\n\nWould you like to share your specific stress sources?"
            ]
            return responses.randomElement() ?? "I'm here to support you."
        } else if lowercasedInput.contains("sleep") || lowercasedInput.contains("insomnia") {
            let responses = [
                "Good sleep is crucial for mental health:\n\n• Maintain a regular sleep schedule\n• Create a comfortable sleep environment\n• Avoid electronic devices before bed\n• Try relaxation techniques\n\nHow long have you had sleep problems?",
                "Sleep quality directly affects our mental health. Here are some improvement suggestions:\n\n1. Establish a fixed sleep time\n2. Create a dark, quiet sleep environment\n3. Avoid caffeine and alcohol\n4. Do relaxing activities before bed\n\nWhat time do you usually go to bed?"
            ]
            return responses.randomElement() ?? "I'm here to support you."
        } else {
            let responses = [
                "I understand your feelings. Let's explore this topic together.",
                "This is a great question. Based on my knowledge, I can provide some suggestions.",
                "Thank you for sharing. Mental health indeed needs our attention.",
                "I notice you mentioned some important points. Let's discuss this in depth.",
                "Your thoughts are valuable. We can look at this issue from multiple perspectives.",
                "I'm here to support you. If you need more specific advice, please tell me.",
                "This is a topic worth exploring in depth. What specific aspect would you like to know about?",
                "Thank you for your trust. Let's explore this topic together and find a solution that works for you."
            ]
            return responses.randomElement() ?? "I'm here to support you."
        }
    }
    
    // MARK: - Message Simulation
    private func startMessageSimulation() {
        // Simulate periodic system messages or reminders
        responseTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            self?.sendPeriodicReminder()
        }
    }
    
    // send system message
    private func sendPeriodicReminder() {
        guard !messages.isEmpty else { return }
        
        let reminders = [
            "Remember to maintain a regular sleep schedule!",
            "If you feel stressed, try deep breathing exercises.",
            "Keep a positive mindset, you're doing great!",
            "Remember to stay connected with friends and family."
        ]
        
        if let reminder = reminders.randomElement() {
            let systemMessage = Message.systemMessage(reminder)
            messagePublisher.send(systemMessage)
        }
    }
    
    // MARK: - Suggestions
    private func updateSuggestions() {
        let currentInput = inputText.lowercased()
        
        if currentInput.isEmpty {
            suggestions = [
                "How to relieve anxiety?",
                "Stress management techniques",
                "Improve sleep quality",
                "Emotional regulation methods",
                "Interpersonal relationship advice"
            ]
        } else if currentInput.contains("anxiety") {
            suggestions = [
                "Deep breathing exercises",
                "Progressive muscle relaxation",
                "Mindfulness meditation",
                "Exercise relief"
            ]
        } else if currentInput.contains("stress") {
            suggestions = [
                "Time management",
                "Exercise relaxation",
                "Social support",
                "Hobbies and interests"
            ]
        } else if currentInput.contains("sleep") {
            suggestions = [
                "Regular routine",
                "Sleep environment",
                "Relaxation techniques",
                "Avoid electronic devices"
            ]
        } else {
            suggestions = [
                "Mental health knowledge",
                "Emotional management",
                "Interpersonal relationships",
                "Self-care"
            ]
        }
        
        showSuggestions = !inputText.isEmpty && !suggestions.isEmpty
    }
    
    // MARK: - Message Update Handler
    private func handleMessagesUpdate(_ messages: [Message]) {
        // Analytics or logging could be added here
        print("Messages updated: \(messages.count) messages")
        
        // Auto-clear suggestions after certain number of messages
        if messages.count > 10 {
            showSuggestions = false
        }
    }
    
    // MARK: - Utility Methods
    func clearChat() {
        messages.removeAll()
        loadMockData()
    }
    
    // export chat as string
    func exportChat() -> String {
        return messages.map { message in
            let sender = message.sender.displayName
            let time = formatTime(message.timestamp)
            return "[\(time)] \(sender): \(message.text)"
        }.joined(separator: "\n\n")
    }
    
    // format time
    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
    
    // MARK: - Mock Data
    func loadMockData() {
        messages = [
            Message.aiMessage("Hello! I'm your AI mental health assistant. How can I help you?"),
            Message.userMessage("I'd like to learn about mental health knowledge"),
            Message.aiMessage("Of course! Mental health is a very important topic. We can discuss stress management, emotional regulation, interpersonal relationships, and more. What aspect are you most interested in?"),
            Message.userMessage("I want to learn how to relieve anxiety"),
            Message.aiMessage("Anxiety is a very common emotional response. Here are some effective relief methods:\n\n1. Deep breathing exercises\n2. Progressive muscle relaxation\n3. Mindfulness meditation\n4. Regular exercise\n5. Adequate sleep\n\nWhich method would you like to try?")
        ]
        
        updateSuggestions()
    }
} 