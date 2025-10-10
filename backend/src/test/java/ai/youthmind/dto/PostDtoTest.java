package ai.youthmind.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

@DisplayName("PostDto Tests")
public class PostDtoTest {
    
    private final UUID testId = UUID.randomUUID();
    private final UUID testUserId = UUID.randomUUID();
    private final LocalDateTime testDate = LocalDateTime.now();
    
    @Test
    @DisplayName("Should create PostDto with all required fields")
    void shouldCreatePostDtoWithRequiredFields() {
        PostDto postDto = new PostDto(
            testId, 5, 10, 2, "Test Title", "Test Content", "test.jpg",
            false, false, "happy", testDate, testUserId, "test topics",
            "ai metadata", false, "", null, null, 0
        );
        
        assertNotNull(postDto);
        assertEquals(testId, postDto.getId());
        assertEquals("Test Title", postDto.getTitle());
        assertEquals("Test Content", postDto.getContent());
        assertEquals("happy", postDto.getEmotion());
        assertEquals(testDate, postDto.getDate());
    }
    
    @Test
    @DisplayName("Should validate required fields correctly")
    void shouldValidateRequiredFields() {
        PostDto validPostDto = new PostDto(
            testId, 0, 0, 0, "Title", "Content", "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertTrue(validPostDto.hasRequiredFields());
        
        PostDto invalidPostDto = new PostDto(
            null, 0, 0, 0, "Title", "Content", "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertFalse(invalidPostDto.hasRequiredFields());
    }
    
    @Test
    @DisplayName("Should generate correct summary for long content")
    void shouldGenerateCorrectSummaryForLongContent() {
        String longContent = "This is a very long content that should be truncated to 100 characters " +
                           "when generating a summary for display purposes in the UI.";
        
        PostDto postDto = new PostDto(
            testId, 0, 0, 0, "Title", longContent, "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        String summary = postDto.getSummary();
        assertEquals(103, summary.length());
        assertTrue(summary.endsWith("..."));
    }
    
    @Test
    @DisplayName("Should generate correct summary for short content")
    void shouldGenerateCorrectSummaryForShortContent() {
        String shortContent = "Short content";
        
        PostDto postDto = new PostDto(
            testId, 0, 0, 0, "Title", shortContent, "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        String summary = postDto.getSummary();
        assertEquals(shortContent, summary);
    }
    
    @Test
    @DisplayName("Should handle null content gracefully")
    void shouldHandleNullContentGracefully() {
        PostDto postDto = new PostDto(
            testId, 0, 0, 0, "Title", null, "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertEquals("", postDto.getSummary());
    }
    
    @Test
    @DisplayName("Should check image presence correctly")
    void shouldCheckImagePresenceCorrectly() {
        PostDto postWithImage = new PostDto(
            testId, 0, 0, 0, "Title", "Content", "image.jpg", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        PostDto postWithoutImage = new PostDto(
            testId, 0, 0, 0, "Title", "Content", "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        PostDto postWithNullImage = new PostDto(
            testId, 0, 0, 0, "Title", "Content", null, false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertTrue(postWithImage.hasImage());
        assertFalse(postWithoutImage.hasImage());
        assertFalse(postWithNullImage.hasImage());
    }
    
    @Test
    @DisplayName("Should normalize emotion correctly")
    void shouldNormalizeEmotionCorrectly() {
        PostDto postDto = new PostDto(
            testId, 0, 0, 0, "Title", "Content", "", false, false, "HAPPY", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertEquals("happy", postDto.getNormalizedEmotion());
    }
    
    @Test
    @DisplayName("Should handle null emotion gracefully")
    void shouldHandleNullEmotionGracefully() {
        PostDto postDto = new PostDto(
            testId, 0, 0, 0, "Title", "Content", "", false, false, null, testDate,
            null, "", "", false, "", null, null, 0
        );
        
        assertEquals("", postDto.getNormalizedEmotion());
    }
    
    @Test
    @DisplayName("Should provide compatibility aliases correctly")
    void shouldProvideCompatibilityAliasesCorrectly() {
        PostDto postDto = new PostDto(
            testId, 5, 10, 2, "Title", "Content", "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        // Test likes alias
        assertEquals(5, postDto.getLikes());
        postDto.setLikes(15);
        assertEquals(15, postDto.getResonance());
        
        // Test views alias
        assertEquals(10, postDto.getViews());
        postDto.setViews(20);
        assertEquals(20, postDto.getView());
        
        // Test bookmarks alias
        assertEquals(2, postDto.getBookmarks());
        postDto.setBookmarks(8);
        assertEquals(8, postDto.getCollect());
        
        // Test liked alias
        assertFalse(postDto.isLiked());
        postDto.setLiked(true);
        assertTrue(postDto.isIsresonanced());
        
        // Test bookmarked alias
        assertFalse(postDto.isBookmarked());
        postDto.setBookmarked(true);
        assertTrue(postDto.isCollected());
        
        // Test createdAt alias
        assertEquals(testDate, postDto.getCreatedAt());
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        postDto.setCreatedAt(newDate);
        assertEquals(newDate, postDto.getDate());
    }
    
    @Test
    @DisplayName("Should handle null values in alias setters")
    void shouldHandleNullValuesInAliasSetters() {
        PostDto postDto = new PostDto(
            testId, 5, 10, 2, "Title", "Content", "", false, false, "happy", testDate,
            null, "", "", false, "", null, null, 0
        );
        
        postDto.setLikes(null);
        assertEquals(0, postDto.getResonance());
        
        postDto.setViews(null);
        assertEquals(0, postDto.getView());
        
        postDto.setBookmarks(null);
        assertEquals(0, postDto.getCollect());
    }
} 