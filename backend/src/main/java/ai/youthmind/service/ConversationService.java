package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Conversation;
import ai.youthmind.entity.Message;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Get conversations by user ID with pagination
     */
    public ConversationListResponse getConversationsByUserId(UUID userId, Pageable pageable) {
        Page<Conversation> conversationPage = conversationRepository.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId, pageable);

        List<ConversationDto> conversationDtos = conversationPage.getContent().stream()
            .map(this::convertToConversationDto)
            .collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(pageable.getPageNumber() + 1);
        pagination.setLimit(pageable.getPageSize());
        pagination.setTotal((int) conversationPage.getTotalElements());
        pagination.setTotalItems((int) conversationPage.getTotalElements());
        pagination.setTotalPages(conversationPage.getTotalPages());
        pagination.setHasNext(conversationPage.hasNext());
        pagination.setHasPrevious(conversationPage.hasPrevious());

        ConversationListResponse response = new ConversationListResponse();
        response.setConversations(conversationDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Save a conversation
     */
    public Conversation saveConversation(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    /**
     * Get conversation by ID
     */
    public Optional<Conversation> getConversationById(UUID conversationId) {
        return conversationRepository.findById(conversationId);
    }

    /**
     * Save a message
     */
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    /**
     * Get messages by conversation ID
     */
    public List<MessageDto> getMessagesByConversationId(UUID conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        return messages.stream()
            .map(this::convertToMessageDto)
            .collect(Collectors.toList());
    }

    /**
     * Get user's conversations with pagination
     */
    public ConversationListResponse getConversations(UUID userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Conversation> conversationPage = conversationRepository.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId, pageable);

        List<ConversationDto> conversationDtos = conversationPage.getContent().stream()
            .map(this::convertToConversationDto)
            .collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalItems((int) conversationPage.getTotalElements());
        pagination.setTotalPages(conversationPage.getTotalPages());
        pagination.setHasNext(conversationPage.hasNext());
        pagination.setHasPrevious(conversationPage.hasPrevious());

        ConversationListResponse response = new ConversationListResponse();
        response.setConversations(conversationDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Create a new conversation
     */
    public ConversationDto createConversation(ConversationCreateRequest request, UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle());
        conversation.setIsPinned(false);

        Conversation savedConversation = conversationRepository.save(conversation);
        return convertToConversationDto(savedConversation);
    }

    /**
     * Get a single conversation with messages
     */
    public ConversationDto getConversation(UUID conversationId, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        Conversation conversation = conversationOpt.get();
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this conversation");
        }

        return convertToConversationDto(conversation);
    }

    /**
     * Add a message to a conversation
     */
    public MessageDto addMessage(UUID conversationId, MessageCreateRequest request, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        Conversation conversation = conversationOpt.get();
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to add messages to this conversation");
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setText(request.getText());
        message.setSender(request.getSender());

        Message savedMessage = messageRepository.save(message);

        // Update conversation's updatedAt timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return convertToMessageDto(savedMessage);
    }

    /**
     * Update a conversation
     */
    public ConversationDto updateConversation(UUID conversationId, ConversationUpdateRequest request, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        Conversation conversation = conversationOpt.get();
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this conversation");
        }

        if (request.getTitle() != null) {
            conversation.setTitle(request.getTitle());
        }
        if (request.isPinned() != false) { // Check for change from default
            conversation.setIsPinned(request.isPinned());
        }
        conversation.setUpdatedAt(LocalDateTime.now());

        Conversation savedConversation = conversationRepository.save(conversation);
        return convertToConversationDto(savedConversation);
    }

    /**
     * Delete a conversation
     */
    public void deleteConversation(UUID conversationId, UUID userId) {
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new RuntimeException("Conversation not found");
        }

        Conversation conversation = conversationOpt.get();
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this conversation");
        }

        // Delete all messages first
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        messageRepository.deleteAll(messages);

        // Delete the conversation
        conversationRepository.delete(conversation);
    }

    // Helper methods
    private ConversationDto convertToConversationDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setTitle(conversation.getTitle());
        dto.setPinned(conversation.getIsPinned());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        // Get messages for this conversation
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());
        List<MessageDto> messageDtos = messages.stream()
            .map(this::convertToMessageDto)
            .collect(Collectors.toList());
        dto.setMessages(messageDtos);

        return dto;
    }

    private MessageDto convertToMessageDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setSender(message.getSender());
        dto.setTimestamp(message.getTimestamp());
        dto.setConversationId(message.getConversationId());
        return dto;
    }
}
