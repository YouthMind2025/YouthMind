package ai.youthmind;

import ai.youthmind.dto.*;
import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ComprehensiveApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserEngagementRepository userEngagementRepository;

    @Autowired
    private CommentEngagementRepository commentEngagementRepository;

    private User testUser;
    private Post testPost;
    private Comment testComment;
    private Draft testDraft;
    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        commentEngagementRepository.deleteAll();
        userEngagementRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        draftRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setBio("Test bio");
        testUser.setAvatar("avatar.jpg");
        testUser.setPostsCount(0);
        testUser.setLikesCount(0);
        testUser.setBookmarksCount(0);
        testUser = userRepository.save(testUser);

        // Create test post
        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setTitle("Test Post");
        testPost.setContent("This is a test post content");
        testPost.setEmotion("happy");
        testPost.setTopics("test,example");
        testPost.setPreImg("preview.jpg");
        testPost.setResonance(0);
        testPost.setCollect(0);
        testPost.setView(0);
        testPost = postRepository.save(testPost);

        // Create test comment
        testComment = new Comment();
        testComment.setPostId(testPost.getId());
        testComment.setAuthorId(testUser.getId());
        testComment.setContent("This is a test comment");
        testComment.setLikes(0);
        testComment = commentRepository.save(testComment);

        // Create test draft
        testDraft = new Draft();
        testDraft.setUserId(testUser.getId());
        testDraft.setTitle("Test Draft");
        testDraft.setContent("This is a test draft content");
        testDraft.setEmotion("sad");
        testDraft.setTopics("draft,test");
        testDraft = draftRepository.save(testDraft);

        // Create test conversation
        testConversation = new Conversation();
        testConversation.setUserId(testUser.getId());
        testConversation.setTitle("Test Conversation");
        testConversation.setIsPinned(false);
        testConversation = conversationRepository.save(testConversation);

        // Create test message
        testMessage = new Message();
        testMessage.setConversationId(testConversation.getId());
        testMessage.setText("This is a test message");
        testMessage.setSender("user");
        testMessage = messageRepository.save(testMessage);
    }

    @Test
    void testUserEntityAndRepository() {
        // Test user creation and retrieval
        assertNotNull(testUser.getId());
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("testuser", testUser.getUsername());
        assertEquals("Test", testUser.getFirstName());
        assertEquals("User", testUser.getLastName());
        assertEquals("Test bio", testUser.getBio());
        assertEquals("avatar.jpg", testUser.getAvatar());
        assertEquals(0, testUser.getPostsCount());
        assertEquals(0, testUser.getLikesCount());
        assertEquals(0, testUser.getBookmarksCount());

        // Test repository methods
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getId(), foundUser.get().getId());

        Optional<User> foundUserByUsername = userRepository.findByUsername("testuser");
        assertTrue(foundUserByUsername.isPresent());
        assertEquals(testUser.getId(), foundUserByUsername.get().getId());

        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertTrue(userRepository.existsByUsername("testuser"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
        assertFalse(userRepository.existsByUsername("nonexistentuser"));

        // Test count methods
        assertEquals(0, userRepository.getPostsCountByUserId(testUser.getId()));
        assertEquals(0, userRepository.getLikesCountByUserId(testUser.getId()));
        assertEquals(0, userRepository.getBookmarksCountByUserId(testUser.getId()));
    }

    @Test
    void testPostEntityAndRepository() {
        // Test post creation and retrieval
        assertNotNull(testPost.getId());
        assertEquals(testUser.getId(), testPost.getUserId());
        assertEquals("Test Post", testPost.getTitle());
        assertEquals("This is a test post content", testPost.getContent());
        assertEquals("happy", testPost.getEmotion());
        assertEquals("test,example", testPost.getTopics());
        assertEquals("preview.jpg", testPost.getPreImg());
        assertEquals(0, testPost.getResonance());
        assertEquals(0, testPost.getCollect());
        assertEquals(0, testPost.getView());

        // Test repository methods
        List<Post> userPosts = postRepository.findByUserId(testUser.getId());
        assertEquals(1, userPosts.size());
        assertEquals(testPost.getId(), userPosts.get(0).getId());

        List<Post> postsByEmotion = postRepository.findByEmotion("happy");
        assertEquals(1, postsByEmotion.size());
        assertEquals(testPost.getId(), postsByEmotion.get(0).getId());

        List<Post> userPostsByEmotion = postRepository.findByUserIdAndEmotion(testUser.getId(), "happy");
        assertEquals(1, userPostsByEmotion.size());
        assertEquals(testPost.getId(), userPostsByEmotion.get(0).getId());
    }

    @Test
    void testCommentEntityAndRepository() {
        // Test comment creation and retrieval
        assertNotNull(testComment.getId());
        assertEquals(testPost.getId(), testComment.getPostId());
        assertEquals(testUser.getId(), testComment.getAuthorId());
        assertEquals("This is a test comment", testComment.getContent());
        assertEquals(0, testComment.getLikes());

        // Test repository methods
        List<Comment> postComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(testPost.getId());
        assertEquals(1, postComments.size());
        assertEquals(testComment.getId(), postComments.get(0).getId());

        assertEquals(1, commentRepository.countByPostId(testPost.getId()));
        assertEquals(1, commentRepository.countByAuthorId(testUser.getId()));

        // Test hierarchical comments
        Comment replyComment = new Comment();
        replyComment.setPostId(testPost.getId());
        replyComment.setAuthorId(testUser.getId());
        replyComment.setContent("This is a reply");
        replyComment.setParentId(testComment.getId());
        replyComment.setLikes(0);
        replyComment = commentRepository.save(replyComment);

        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(testComment.getId());
        assertEquals(1, replies.size());
        assertEquals(replyComment.getId(), replies.get(0).getId());
    }

    @Test
    void testDraftEntityAndRepository() {
        // Test draft creation and retrieval
        assertNotNull(testDraft.getId());
        assertEquals(testUser.getId(), testDraft.getUserId());
        assertEquals("Test Draft", testDraft.getTitle());
        assertEquals("This is a test draft content", testDraft.getContent());
        assertEquals("sad", testDraft.getEmotion());
        assertEquals("draft,test", testDraft.getTopics());

        // Test repository methods
        var draftPage = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId(), 
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, draftPage.getTotalElements());
        assertEquals(testDraft.getId(), draftPage.getContent().get(0).getId());
    }

    @Test
    void testConversationEntityAndRepository() {
        // Test conversation creation and retrieval
        assertNotNull(testConversation.getId());
        assertEquals(testUser.getId(), testConversation.getUserId());
        assertEquals("Test Conversation", testConversation.getTitle());
        assertFalse(testConversation.getIsPinned());

        // Test repository methods
        var conversationPage = conversationRepository.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(testUser.getId(), 
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, conversationPage.getTotalElements());
        assertEquals(testConversation.getId(), conversationPage.getContent().get(0).getId());
    }

    @Test
    void testMessageEntityAndRepository() {
        // Test message creation and retrieval
        assertNotNull(testMessage.getId());
        assertEquals(testConversation.getId(), testMessage.getConversationId());
        assertEquals("This is a test message", testMessage.getText());
        assertEquals("user", testMessage.getSender());

        // Test repository methods
        List<Message> conversationMessages = messageRepository.findByConversationIdOrderByTimestampAsc(testConversation.getId());
        assertEquals(1, conversationMessages.size());
        assertEquals(testMessage.getId(), conversationMessages.get(0).getId());
    }

    @Test
    void testUserEngagementEntityAndRepository() {
        // Test user engagement creation
        UserEngagement engagement = new UserEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setPostId(testPost.getId());
        engagement.setType(UserEngagement.EngagementType.RESONANCE);
        engagement = userEngagementRepository.save(engagement);

        assertNotNull(engagement.getId());
        assertEquals(testUser.getId(), engagement.getUserId());
        assertEquals(testPost.getId(), engagement.getPostId());
        assertEquals(UserEngagement.EngagementType.RESONANCE, engagement.getType());

        // Test repository methods
        Optional<UserEngagement> foundEngagement = userEngagementRepository.findByUserIdAndPostIdAndType(
            testUser.getId(), testPost.getId(), UserEngagement.EngagementType.RESONANCE);
        assertTrue(foundEngagement.isPresent());
        assertEquals(engagement.getId(), foundEngagement.get().getId());

        List<UserEngagement> userEngagements = userEngagementRepository.findByUserIdAndType(
            testUser.getId(), UserEngagement.EngagementType.RESONANCE);
        assertEquals(1, userEngagements.size());

        List<UserEngagement> postEngagements = userEngagementRepository.findByPostIdAndType(
            testPost.getId(), UserEngagement.EngagementType.RESONANCE);
        assertEquals(1, postEngagements.size());

        assertEquals(1, userEngagementRepository.countByPostIdAndType(
            testPost.getId(), UserEngagement.EngagementType.RESONANCE));
        assertEquals(1, userEngagementRepository.countByUserIdAndType(
            testUser.getId(), UserEngagement.EngagementType.RESONANCE));
    }

    @Test
    void testCommentEngagementEntityAndRepository() {
        // Test comment engagement creation
        CommentEngagement engagement = new CommentEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setCommentId(testComment.getId());
        engagement.setType(CommentEngagement.EngagementType.LIKE);
        engagement = commentEngagementRepository.save(engagement);

        assertNotNull(engagement.getId());
        assertEquals(testUser.getId(), engagement.getUserId());
        assertEquals(testComment.getId(), engagement.getCommentId());
        assertEquals(CommentEngagement.EngagementType.LIKE, engagement.getType());

        // Test repository methods
        Optional<CommentEngagement> foundEngagement = commentEngagementRepository.findByUserIdAndCommentIdAndType(
            testUser.getId(), testComment.getId(), CommentEngagement.EngagementType.LIKE);
        assertTrue(foundEngagement.isPresent());
        assertEquals(engagement.getId(), foundEngagement.get().getId());

        List<CommentEngagement> userEngagements = commentEngagementRepository.findByUserIdAndType(
            testUser.getId(), CommentEngagement.EngagementType.LIKE);
        assertEquals(1, userEngagements.size());

        List<CommentEngagement> commentEngagements = commentEngagementRepository.findByCommentIdAndType(
            testComment.getId(), CommentEngagement.EngagementType.LIKE);
        assertEquals(1, commentEngagements.size());

        assertEquals(1, commentEngagementRepository.countByCommentIdAndType(
            testComment.getId(), CommentEngagement.EngagementType.LIKE));
        assertEquals(1, commentEngagementRepository.countByUserIdAndType(
            testUser.getId(), CommentEngagement.EngagementType.LIKE));
    }

    @Test
    void testDtoCreationAndValidation() {
        // Test SendOtpRequest DTO
        SendOtpRequest otpRequest = new SendOtpRequest("test@example.com");
        assertEquals("test@example.com", otpRequest.getEmail());

        // Test RegisterRequest DTO
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setOtp("123456");
        assertEquals("test@example.com", registerRequest.getEmail());
        assertEquals("123456", registerRequest.getOtp());

        // Test LoginRequest DTO
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        assertEquals("test@example.com", loginRequest.getEmail());
        assertEquals("password123", loginRequest.getPassword());

        // Test PostCreateRequest DTO
        PostCreateRequest postRequest = new PostCreateRequest("Test Title", "Test Content", "happy", "preview.jpg", "test,topics");
        assertEquals("Test Title", postRequest.getTitle());
        assertEquals("Test Content", postRequest.getContent());
        assertEquals("happy", postRequest.getEmotion());
        assertEquals("preview.jpg", postRequest.getPreImg());
        assertEquals("test,topics", postRequest.getTopics());

        // Test CommentCreateRequest DTO
        CommentCreateRequest commentRequest = new CommentCreateRequest();
        commentRequest.setContent("Test Comment");
        commentRequest.setParentId(null);
        assertEquals("Test Comment", commentRequest.getContent());
        assertNull(commentRequest.getParentId());

        // Test DraftCreateRequest DTO
        DraftCreateRequest draftRequest = new DraftCreateRequest();
        draftRequest.setTitle("Test Draft");
        draftRequest.setContent("Test Content");
        draftRequest.setEmotion("sad");
        draftRequest.setTopics(List.of("draft", "test"));
        assertEquals("Test Draft", draftRequest.getTitle());
        assertEquals("Test Content", draftRequest.getContent());
        assertEquals("sad", draftRequest.getEmotion());
        assertEquals(List.of("draft", "test"), draftRequest.getTopics());

        // Test ConversationCreateRequest DTO
        ConversationCreateRequest conversationRequest = new ConversationCreateRequest();
        conversationRequest.setTitle("Test Conversation");
        assertEquals("Test Conversation", conversationRequest.getTitle());

        // Test MessageCreateRequest DTO
        MessageCreateRequest messageRequest = new MessageCreateRequest();
        messageRequest.setText("Test Message");
        messageRequest.setSender("user");
        assertEquals("Test Message", messageRequest.getText());
        assertEquals("user", messageRequest.getSender());
    }

    @Test
    void testResponseDtoCreation() {
        // Test SendOtpResponse DTO
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        SendOtpResponse otpResponse = new SendOtpResponse();
        otpResponse.setSuccess(true);
        otpResponse.setMessage("OTP sent successfully");
        otpResponse.setExpiresAt(expiresAt);
        assertTrue(otpResponse.isSuccess());
        assertEquals("OTP sent successfully", otpResponse.getMessage());
        assertEquals(expiresAt, otpResponse.getExpiresAt());

        // Test RegisterResponse DTO
        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.setUserId(testUser.getId());
        registerResponse.setToken("jwt-token");
        registerResponse.setExpiresAt(expiresAt);
        registerResponse.setMessage("Registration successful");
        assertEquals(testUser.getId(), registerResponse.getUserId());
        assertEquals("jwt-token", registerResponse.getToken());
        assertEquals(expiresAt, registerResponse.getExpiresAt());
        assertEquals("Registration successful", registerResponse.getMessage());

        // Test LoginResponse DTO
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("jwt-token");
        loginResponse.setUserId(testUser.getId());
        loginResponse.setExpiresAt(expiresAt);
        assertEquals("jwt-token", loginResponse.getToken());
        assertEquals(testUser.getId(), loginResponse.getUserId());
        assertEquals(expiresAt, loginResponse.getExpiresAt());

        // Test PostDto DTO
        UserDto authorDto = new UserDto();
        authorDto.setId(testUser.getId());
        authorDto.setEmail(testUser.getEmail());
        authorDto.setUsername(testUser.getUsername());
        authorDto.setFirstName(testUser.getFirstName());
        authorDto.setLastName(testUser.getLastName());
        authorDto.setBio(testUser.getBio());
        authorDto.setAvatar(testUser.getAvatar());
        authorDto.setPostsCount(testUser.getPostsCount());
        authorDto.setLikesCount(testUser.getLikesCount());
        authorDto.setBookmarksCount(testUser.getBookmarksCount());
        authorDto.setCreatedAt(testUser.getCreatedAt());
        authorDto.setUpdatedAt(testUser.getUpdatedAt());

        PostDto postDto = new PostDto();
        postDto.setId(testPost.getId());
        postDto.setUserId(testPost.getUserId());
        postDto.setTitle(testPost.getTitle());
        postDto.setContent(testPost.getContent());
        postDto.setEmotion(testPost.getEmotion());
        postDto.setTopics(testPost.getTopics());
        postDto.setPreImg(testPost.getPreImg());
        postDto.setResonance(testPost.getResonance());
        postDto.setCollect(testPost.getCollect());
        postDto.setView(testPost.getView());
        postDto.setAiMetadata(testPost.getAiMetadata());
        postDto.setFlagged(testPost.isFlagged());
        postDto.setFlagReason(testPost.getFlagReason());
        postDto.setFlaggedAt(testPost.getFlaggedAt());
        postDto.setCreatedAt(testPost.getCreatedAt());
        postDto.setAuthor(authorDto);
        postDto.setLiked(false);
        postDto.setBookmarked(false);
        postDto.setCommentsCount(0);
        
        assertEquals(testPost.getId(), postDto.getId());
        assertEquals(testPost.getTitle(), postDto.getTitle());
        assertEquals(testPost.getContent(), postDto.getContent());
        assertEquals(authorDto, postDto.getAuthor());

        // Test PaginationDto DTO
        PaginationDto paginationDto = new PaginationDto();
        paginationDto.setPage(1);
        paginationDto.setLimit(10);
        paginationDto.setTotalItems(100);
        paginationDto.setTotalPages(10);
        paginationDto.setHasNext(true);
        paginationDto.setHasPrevious(false);
        assertEquals(1, paginationDto.getPage());
        assertEquals(10, paginationDto.getLimit());
        assertEquals(100, paginationDto.getTotalItems());
        assertEquals(10, paginationDto.getTotalPages());
        assertTrue(paginationDto.isHasNext());
        assertFalse(paginationDto.isHasPrevious());
    }

    @Test
    void testCascadeOperations() {
        // Test that deleting a user cascades to related entities
        UUID userId = testUser.getId();
        UUID postId = testPost.getId();
        UUID commentId = testComment.getId();
        UUID draftId = testDraft.getId();
        UUID conversationId = testConversation.getId();
        UUID messageId = testMessage.getId();

        // Verify entities exist before deletion
        assertTrue(userRepository.existsById(userId));
        assertTrue(postRepository.existsById(postId));
        assertTrue(commentRepository.existsById(commentId));
        assertTrue(draftRepository.existsById(draftId));
        assertTrue(conversationRepository.existsById(conversationId));
        assertTrue(messageRepository.existsById(messageId));

        // Delete user
        userRepository.deleteById(userId);

        // Verify all related entities are deleted
        assertFalse(userRepository.existsById(userId));
        assertFalse(postRepository.existsById(postId));
        assertFalse(commentRepository.existsById(commentId));
        assertFalse(draftRepository.existsById(draftId));
        assertFalse(conversationRepository.existsById(conversationId));
        assertFalse(messageRepository.existsById(messageId));
    }

    @Test
    void testUniqueConstraints() {
        // Test unique constraint on user email
        User duplicateUser = new User();
        duplicateUser.setEmail("test@example.com"); // Same email as testUser
        duplicateUser.setPassword("password123");
        duplicateUser.setUsername("differentuser");
        duplicateUser.setFirstName("Different");
        duplicateUser.setLastName("User");
        duplicateUser.setBio("Different bio");
        duplicateUser.setAvatar("different-avatar.jpg");
        duplicateUser.setPostsCount(0);
        duplicateUser.setLikesCount(0);
        duplicateUser.setBookmarksCount(0);

        // This should throw an exception due to unique constraint
        assertThrows(Exception.class, () -> userRepository.save(duplicateUser));

        // Test unique constraint on user engagement (user, post, type combination)
        UserEngagement engagement1 = new UserEngagement();
        engagement1.setUserId(testUser.getId());
        engagement1.setPostId(testPost.getId());
        engagement1.setType(UserEngagement.EngagementType.RESONANCE);
        userEngagementRepository.save(engagement1);

        UserEngagement engagement2 = new UserEngagement();
        engagement2.setUserId(testUser.getId());
        engagement2.setPostId(testPost.getId());
        engagement2.setType(UserEngagement.EngagementType.RESONANCE); // Same combination

        // This should throw an exception due to unique constraint
        assertThrows(Exception.class, () -> userEngagementRepository.save(engagement2));
    }

    @Test
    void testDataIntegrity() {
        // Test that foreign key relationships are maintained
        assertTrue(userRepository.existsById(testUser.getId()));
        assertTrue(postRepository.existsById(testPost.getId()));

        // Try to create a post with non-existent user
        Post invalidPost = new Post();
        invalidPost.setUserId(UUID.randomUUID()); // Non-existent user
        invalidPost.setTitle("Invalid Post");
        invalidPost.setContent("This should fail");
        invalidPost.setEmotion("sad");
        invalidPost.setTopics("invalid");
        invalidPost.setPreImg("invalid.jpg");
        invalidPost.setResonance(0);
        invalidPost.setCollect(0);
        invalidPost.setView(0);

        // This should throw an exception due to foreign key constraint
        assertThrows(Exception.class, () -> postRepository.save(invalidPost));

        // Try to create a comment with non-existent post
        Comment invalidComment = new Comment();
        invalidComment.setPostId(UUID.randomUUID()); // Non-existent post
        invalidComment.setAuthorId(testUser.getId());
        invalidComment.setContent("This should fail");
        invalidComment.setLikes(0);

        // This should throw an exception due to foreign key constraint
        assertThrows(Exception.class, () -> commentRepository.save(invalidComment));
    }

    @Test
    void testEnumValues() {
        // Test UserEngagement enum values
        UserEngagement resonanceEngagement = new UserEngagement();
        resonanceEngagement.setUserId(testUser.getId());
        resonanceEngagement.setPostId(testPost.getId());
        resonanceEngagement.setType(UserEngagement.EngagementType.RESONANCE);
        resonanceEngagement = userEngagementRepository.save(resonanceEngagement);
        assertEquals(UserEngagement.EngagementType.RESONANCE, resonanceEngagement.getType());

        UserEngagement collectEngagement = new UserEngagement();
        collectEngagement.setUserId(testUser.getId());
        collectEngagement.setPostId(testPost.getId());
        collectEngagement.setType(UserEngagement.EngagementType.COLLECT);
        collectEngagement = userEngagementRepository.save(collectEngagement);
        assertEquals(UserEngagement.EngagementType.COLLECT, collectEngagement.getType());

        UserEngagement viewEngagement = new UserEngagement();
        viewEngagement.setUserId(testUser.getId());
        viewEngagement.setPostId(testPost.getId());
        viewEngagement.setType(UserEngagement.EngagementType.VIEW);
        viewEngagement = userEngagementRepository.save(viewEngagement);
        assertEquals(UserEngagement.EngagementType.VIEW, viewEngagement.getType());

        // Test CommentEngagement enum values
        CommentEngagement likeEngagement = new CommentEngagement();
        likeEngagement.setUserId(testUser.getId());
        likeEngagement.setCommentId(testComment.getId());
        likeEngagement.setType(CommentEngagement.EngagementType.LIKE);
        likeEngagement = commentEngagementRepository.save(likeEngagement);
        assertEquals(CommentEngagement.EngagementType.LIKE, likeEngagement.getType());
    }

    @Test
    void testAuditFields() {
        // Test that audit fields are automatically populated
        assertNotNull(testUser.getCreatedAt());
        assertNotNull(testUser.getUpdatedAt());
        assertNotNull(testPost.getCreatedAt());
        assertNotNull(testComment.getCreatedAt());
        assertNotNull(testDraft.getCreatedAt());
        assertNotNull(testDraft.getUpdatedAt());
        assertNotNull(testConversation.getCreatedAt());
        assertNotNull(testConversation.getUpdatedAt());
        assertNotNull(testMessage.getTimestamp());

        // Test that timestamps are recent
        LocalDateTime now = LocalDateTime.now();
        assertTrue(testUser.getCreatedAt().isBefore(now.plusSeconds(1)));
        assertTrue(testUser.getCreatedAt().isAfter(now.minusSeconds(5)));
    }

    @Test
    void testDefaultValues() {
        // Test that default values are properly set
        assertEquals(0, testUser.getPostsCount());
        assertEquals(0, testUser.getLikesCount());
        assertEquals(0, testUser.getBookmarksCount());
        assertEquals(0, testPost.getResonance());
        assertEquals(0, testPost.getCollect());
        assertEquals(0, testPost.getView());
        assertEquals(0, testComment.getLikes());
        assertFalse(testConversation.getIsPinned());
    }

    @Test
    void testSearchAndFiltering() {
        // Create additional posts for testing search
        Post post2 = new Post();
        post2.setUserId(testUser.getId());
        post2.setTitle("Another Test Post");
        post2.setContent("This is another test post about anxiety");
        post2.setEmotion("anxiety");
        post2.setTopics("anxiety,mental-health");
        post2.setPreImg("anxiety-preview.jpg");
        post2.setResonance(5);
        post2.setCollect(2);
        post2.setView(10);
        post2 = postRepository.save(post2);

        Post post3 = new Post();
        post3.setUserId(testUser.getId());
        post3.setTitle("Depression Post");
        post3.setContent("This is a post about depression");
        post3.setEmotion("depression");
        post3.setTopics("depression,mental-health");
        post3.setPreImg("depression-preview.jpg");
        post3.setResonance(10);
        post3.setCollect(5);
        post3.setView(25);
        post3 = postRepository.save(post3);

        // Test emotion filtering
        List<Post> happyPosts = postRepository.findByEmotion("happy");
        assertEquals(1, happyPosts.size());
        assertEquals("happy", happyPosts.get(0).getEmotion());

        List<Post> anxietyPosts = postRepository.findByEmotion("anxiety");
        assertEquals(1, anxietyPosts.size());
        assertEquals("anxiety", anxietyPosts.get(0).getEmotion());

        // Test sorting by resonance
        var postsByResonance = postRepository.findAllByOrderByResonanceDesc(
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(3, postsByResonance.getTotalElements());
        // First post should be the one with most resonance (post3 with 10 resonance)
        assertEquals(post3.getId(), postsByResonance.getContent().get(0).getId());

        // Test sorting by views
        var postsByViews = postRepository.findAllByOrderByViewDesc(
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(3, postsByViews.getTotalElements());
        // First post should be the one with most views (post3 with 25 views)
        assertEquals(post3.getId(), postsByViews.getContent().get(0).getId());
    }

    @Test
    void testPagination() {
        // Create multiple drafts for pagination testing
        for (int i = 1; i <= 15; i++) {
            Draft draft = new Draft();
            draft.setUserId(testUser.getId());
            draft.setTitle("Draft " + i);
            draft.setContent("Content for draft " + i);
            draft.setEmotion("emotion" + i);
            draft.setTopics("topic" + i);
            draftRepository.save(draft);
        }

        // Test first page
        var firstPage = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId(),
            org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(16, firstPage.getTotalElements()); // 15 new + 1 from setup
        assertEquals(10, firstPage.getContent().size());
        assertTrue(firstPage.hasNext());
        assertFalse(firstPage.hasPrevious());

        // Test second page
        var secondPage = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId(),
            org.springframework.data.domain.PageRequest.of(1, 10));
        assertEquals(16, secondPage.getTotalElements());
        assertEquals(6, secondPage.getContent().size()); // Remaining 6 items
        assertFalse(secondPage.hasNext());
        assertTrue(secondPage.hasPrevious());
    }

    @Test
    void testComplexQueries() {
        // Test user engagement counting
        UserEngagement engagement1 = new UserEngagement();
        engagement1.setUserId(testUser.getId());
        engagement1.setPostId(testPost.getId());
        engagement1.setType(UserEngagement.EngagementType.RESONANCE);
        userEngagementRepository.save(engagement1);

        UserEngagement engagement2 = new UserEngagement();
        engagement2.setUserId(testUser.getId());
        engagement2.setPostId(testPost.getId());
        engagement2.setType(UserEngagement.EngagementType.COLLECT);
        userEngagementRepository.save(engagement2);

        assertEquals(1, userEngagementRepository.countByPostIdAndType(
            testPost.getId(), UserEngagement.EngagementType.RESONANCE));
        assertEquals(1, userEngagementRepository.countByPostIdAndType(
            testPost.getId(), UserEngagement.EngagementType.COLLECT));
        assertEquals(2, userEngagementRepository.countByUserIdAndType(
            testUser.getId(), UserEngagement.EngagementType.RESONANCE) + 
            userEngagementRepository.countByUserIdAndType(testUser.getId(), UserEngagement.EngagementType.COLLECT));

        // Test comment counting
        Comment comment2 = new Comment();
        comment2.setPostId(testPost.getId());
        comment2.setAuthorId(testUser.getId());
        comment2.setContent("Second comment");
        comment2.setLikes(0);
        commentRepository.save(comment2);

        assertEquals(2, commentRepository.countByPostId(testPost.getId()));
        assertEquals(2, commentRepository.countByAuthorId(testUser.getId()));
    }

    @Test
    void testCompleteWorkflow() {
        // Test a complete workflow: User creates post, gets engagement, adds comments
        
        // 1. User creates a post
        Post workflowPost = new Post();
        workflowPost.setUserId(testUser.getId());
        workflowPost.setTitle("Workflow Post");
        workflowPost.setContent("This is a workflow test post");
        workflowPost.setEmotion("excited");
        workflowPost.setTopics("workflow,test");
        workflowPost.setPreImg("workflow.jpg");
        workflowPost.setResonance(0);
        workflowPost.setCollect(0);
        workflowPost.setView(0);
        workflowPost = postRepository.save(workflowPost);

        // 2. Another user likes the post
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password123");
        otherUser.setUsername("otheruser");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setBio("Other user bio");
        otherUser.setAvatar("other-avatar.jpg");
        otherUser.setPostsCount(0);
        otherUser.setLikesCount(0);
        otherUser.setBookmarksCount(0);
        otherUser = userRepository.save(otherUser);

        UserEngagement likeEngagement = new UserEngagement();
        likeEngagement.setUserId(otherUser.getId());
        likeEngagement.setPostId(workflowPost.getId());
        likeEngagement.setType(UserEngagement.EngagementType.RESONANCE);
        userEngagementRepository.save(likeEngagement);

        // 3. User adds a comment
        Comment workflowComment = new Comment();
        workflowComment.setPostId(workflowPost.getId());
        workflowComment.setAuthorId(testUser.getId());
        workflowComment.setContent("Great post!");
        workflowComment.setLikes(0);
        workflowComment = commentRepository.save(workflowComment);

        // 4. Other user likes the comment
        CommentEngagement commentLike = new CommentEngagement();
        commentLike.setUserId(otherUser.getId());
        commentLike.setCommentId(workflowComment.getId());
        commentLike.setType(CommentEngagement.EngagementType.LIKE);
        commentEngagementRepository.save(commentLike);

        // 5. Verify the workflow
        assertEquals(1, userEngagementRepository.countByPostIdAndType(
            workflowPost.getId(), UserEngagement.EngagementType.RESONANCE));
        assertEquals(1, commentEngagementRepository.countByCommentIdAndType(
            workflowComment.getId(), CommentEngagement.EngagementType.LIKE));
        assertEquals(1, commentRepository.countByPostId(workflowPost.getId()));

        // 6. Test retrieval of complete workflow data
        List<Comment> postComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(workflowPost.getId());
        assertEquals(1, postComments.size());
        assertEquals(workflowComment.getId(), postComments.get(0).getId());

        List<UserEngagement> postEngagements = userEngagementRepository.findByPostIdAndType(
            workflowPost.getId(), UserEngagement.EngagementType.RESONANCE);
        assertEquals(1, postEngagements.size());
        assertEquals(otherUser.getId(), postEngagements.get(0).getUserId());
    }
}
