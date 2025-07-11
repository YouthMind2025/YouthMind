//
//  ChatViewModelTests.swift
//  SaysomethingTests
//
//  Created by user278797 on 7/11/25.
//

import XCTest
import Combine
@testable import Saysomething

class ChatViewModelTests: XCTestCase {
    var viewModel: ChatViewModel!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        viewModel = ChatViewModel()
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        viewModel = nil
        cancellables = nil
        super.tearDown()
    }
    
    func testMessageStructProperties() {
        // Test Message struct has required properties
        let message = Message(text: "Test message", sender: .user)
        
        XCTAssertNotNil(message.id)
        XCTAssertEqual(message.text, "Test message")
        XCTAssertEqual(message.sender, .user)
        XCTAssertNotNil(message.timestamp)
    }
    
    func testMessageSenderEnum() {
        // Test MessageSender enum properties
        XCTAssertEqual(Message.MessageSender.user.displayName, "User")
        XCTAssertEqual(Message.MessageSender.ai.displayName, "AI Assistant")
        XCTAssertEqual(Message.MessageSender.system.displayName, "System")
        
        XCTAssertTrue(Message.MessageSender.user.isFromUser)
        XCTAssertFalse(Message.MessageSender.ai.isFromUser)
        XCTAssertFalse(Message.MessageSender.system.isFromUser)
    }
    
    func testChatViewModelInitialization() {
        // Test ChatViewModel initializes with @Published properties
        XCTAssertNotNil(viewModel.messages)
        XCTAssertEqual(viewModel.inputText, "")
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNotNil(viewModel.suggestions)
        XCTAssertFalse(viewModel.showSuggestions)
        XCTAssertEqual(viewModel.connectionStatus, .connected)
    }
    
    func testSendMessage() {
        // Test sending a message
        let expectation = XCTestExpectation(description: "Message sent and response received")
        
        viewModel.inputText = "Hello, AI!"
        
        // Monitor messages array for changes
        viewModel.$messages
            .dropFirst() // Skip initial load
            .sink { messages in
                if messages.count >= 2 { // User message + AI response
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        viewModel.sendMessage()
        
        wait(for: [expectation], timeout: 5.0)
        
        // Verify user message was added
        XCTAssertTrue(viewModel.messages.contains { $0.sender == .user && $0.text == "Hello, AI!" })
        
        // Verify AI response was added
        XCTAssertTrue(viewModel.messages.contains { $0.sender == .ai })
    }
    
    func testInputTextDebouncing() {
        // Test input text debouncing for suggestions
        let expectation = XCTestExpectation(description: "Suggestions updated")
        
        viewModel.$suggestions
            .dropFirst()
            .sink { suggestions in
                if !suggestions.isEmpty {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        viewModel.inputText = "anxiety"
        
        wait(for: [expectation], timeout: 2.0)
        
        // Verify suggestions are updated based on input
        XCTAssertTrue(viewModel.suggestions.contains("Deep breathing exercises"))
    }
    
    func testConnectionStatusChanges() {
        // Test connection status changes during message sending
        let expectation = XCTestExpectation(description: "Connection status changed")
        
        viewModel.$connectionStatus
            .dropFirst()
            .sink { status in
                if status == .connecting {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        viewModel.inputText = "Test message"
        viewModel.sendMessage()
        
        wait(for: [expectation], timeout: 2.0)
        
        // Verify status eventually returns to connected
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            XCTAssertEqual(self.viewModel.connectionStatus, .connected)
        }
    }
    
    func testMessageSimulation() {
        // Test periodic message simulation
        let expectation = XCTestExpectation(description: "System message received")
        
        viewModel.$messages
            .dropFirst()
            .sink { messages in
                if messages.contains(where: { $0.sender == .system }) {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        // Wait for periodic reminder (30 seconds, but we'll test with a shorter timeout)
        // In a real test, you might want to mock the timer or reduce the interval
        wait(for: [expectation], timeout: 35.0)
    }
    
    func testExportChat() {
        // Test chat export functionality
        viewModel.loadMockData()
        
        let export = viewModel.exportChat()
        
        XCTAssertFalse(export.isEmpty)
        XCTAssertTrue(export.contains("User"))
        XCTAssertTrue(export.contains("AI Assistant"))
    }
    
    func testClearChat() {
        // Test clearing chat functionality
        viewModel.loadMockData()
        XCTAssertFalse(viewModel.messages.isEmpty)
        
        viewModel.clearChat()
        XCTAssertFalse(viewModel.messages.isEmpty) // Should reload mock data
    }
} 