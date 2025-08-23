package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Draft;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.DraftRepository;
import ai.youthmind.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DraftService {

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private PostRepository postRepository;

    /**
     * Get drafts by user ID with pagination
     */
    public DraftListResponse getDraftsByUserId(UUID userId, Pageable pageable) {
        Page<Draft> draftsPage = draftRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);

        List<DraftDto> draftDtos = draftsPage.getContent().stream()
            .map(this::convertToDraftDto)
            .collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(pageable.getPageNumber() + 1);
        pagination.setLimit(pageable.getPageSize());
        pagination.setTotal((int) draftsPage.getTotalElements());
        pagination.setTotalItems((int) draftsPage.getTotalElements());
        pagination.setTotalPages(draftsPage.getTotalPages());
        pagination.setHasNext(draftsPage.hasNext());
        pagination.setHasPrevious(draftsPage.hasPrevious());

        DraftListResponse response = new DraftListResponse();
        response.setDrafts(draftDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Save a draft
     */
    public Draft saveDraft(Draft draft) {
        return draftRepository.save(draft);
    }

    /**
     * Get draft by ID
     */
    public Optional<Draft> getDraftById(UUID draftId) {
        return draftRepository.findById(draftId);
    }

    /**
     * Get user's drafts with pagination
     */
    public DraftListResponse getDrafts(UUID userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Draft> draftPage = draftRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);

        List<DraftDto> draftDtos = draftPage.getContent().stream()
            .map(this::convertToDraftDto)
            .collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalItems((int) draftPage.getTotalElements());
        pagination.setTotalPages(draftPage.getTotalPages());
        pagination.setHasNext(draftPage.hasNext());
        pagination.setHasPrevious(draftPage.hasPrevious());

        DraftListResponse response = new DraftListResponse();
        response.setDrafts(draftDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Create a new draft
     */
    public DraftDto createDraft(DraftCreateRequest request, UUID userId) {
        Draft draft = new Draft();
        draft.setUserId(userId);
        draft.setTitle(request.getTitle());
        draft.setContent(request.getContent());
        draft.setEmotion(request.getEmotion());
        
        // Convert List<String> to comma-separated String
        if (request.getTopics() != null && !request.getTopics().isEmpty()) {
            draft.setTopics(String.join(",", request.getTopics()));
        } else {
            draft.setTopics("");
        }

        Draft savedDraft = draftRepository.save(draft);
        return convertToDraftDto(savedDraft);
    }

    /**
     * Update an existing draft
     */
    public DraftDto updateDraft(UUID draftId, DraftUpdateRequest request, UUID userId) {
        Optional<Draft> draftOpt = draftRepository.findById(draftId);
        if (draftOpt.isEmpty()) {
            throw new RuntimeException("Draft not found");
        }

        Draft draft = draftOpt.get();
        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this draft");
        }

        draft.setTitle(request.getTitle());
        draft.setContent(request.getContent());
        draft.setEmotion(request.getEmotion());
        
        // Convert List<String> to comma-separated String
        if (request.getTopics() != null && !request.getTopics().isEmpty()) {
            draft.setTopics(String.join(",", request.getTopics()));
        } else {
            draft.setTopics("");
        }
        
        draft.setUpdatedAt(LocalDateTime.now());

        Draft savedDraft = draftRepository.save(draft);
        return convertToDraftDto(savedDraft);
    }

    /**
     * Delete a draft
     */
    public void deleteDraft(UUID draftId, UUID userId) {
        Optional<Draft> draftOpt = draftRepository.findById(draftId);
        if (draftOpt.isEmpty()) {
            throw new RuntimeException("Draft not found");
        }

        Draft draft = draftOpt.get();
        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this draft");
        }

        draftRepository.delete(draft);
    }

    /**
     * Publish a draft as a post
     */
    public DraftPublishResponse publishDraft(UUID draftId, UUID userId) {
        Optional<Draft> draftOpt = draftRepository.findById(draftId);
        if (draftOpt.isEmpty()) {
            throw new RuntimeException("Draft not found");
        }

        Draft draft = draftOpt.get();
        if (!draft.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to publish this draft");
        }

        // Create post from draft
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(draft.getTitle());
        post.setContent(draft.getContent());
        post.setEmotionTag(draft.getEmotion());
        post.setTopics(draft.getTopics());
        post.setPreImg(null); // Drafts don't have preview images
        post.setLikes(0);
        post.setBookmarks(0);
        post.setViews(0);

        Post savedPost = postRepository.save(post);

        // Delete the draft
        draftRepository.delete(draft);

        // Convert to response
        DraftPublishResponse response = new DraftPublishResponse();
        response.setId(savedPost.getId());
        response.setAuthorId(savedPost.getUserId());
        response.setTitle(savedPost.getTitle());
        response.setContent(savedPost.getContent());
        response.setEmotion(savedPost.getEmotionTag());
        response.setPreImg(savedPost.getPreImg());
        response.setTopics(savedPost.getTopics());
        response.setResonance(savedPost.getLikes());
        response.setViews(savedPost.getViews());
        response.setCollects(savedPost.getBookmarks());
        response.setResonanced(false);
        response.setCollected(false);
        response.setCreatedAt(savedPost.getCreatedAt());
        response.setUpdatedAt(savedPost.getCreatedAt()); // Use createdAt as updatedAt since Post doesn't have updatedAt

        return response;
    }

    // Helper methods
    private DraftDto convertToDraftDto(Draft draft) {
        DraftDto dto = new DraftDto();
        dto.setId(draft.getId());
        dto.setTitle(draft.getTitle());
        dto.setContent(draft.getContent());
        dto.setEmotion(draft.getEmotion());
        
        // Convert comma-separated String to List<String>
        if (draft.getTopics() != null && !draft.getTopics().isEmpty()) {
            dto.setTopics(Arrays.asList(draft.getTopics().split(",")));
        } else {
            dto.setTopics(List.of());
        }
        
        dto.setCreatedAt(draft.getCreatedAt());
        dto.setUpdatedAt(draft.getUpdatedAt());
        return dto;
    }
}
