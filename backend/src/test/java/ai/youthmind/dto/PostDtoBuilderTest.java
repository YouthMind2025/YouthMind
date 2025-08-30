package ai.youthmind.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

@DisplayName("PostDtoBuilder Tests")
public class PostDtoBuilderTest {
    
    private UUID testId;
    private UUID testUserId;
    private LocalDateTime testDate;
    
    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testDate = LocalDateTime.now();
    }
    
    @Test
    @DisplayName("Should create builder with create() method")
    void shouldCreateBuilderWithCreateMethod() {
        PostDtoBuilder builder = PostDtoBuilder.create();
        assertNotNull(builder);
    }
    
    @Test
    @DisplayName("Should build PostDto with all required fields")
    void shouldBuildPostDtoWithAllRequiredFields() {
        PostDto postDto = PostDtoBuilder.create()
            .id(testId)
            .title("Test Title")
            .content("Test Content")
            .emotion("happy")
            .date(testDate)
            .build();
        
        assertNotNull(postDto);
        assertEquals(testId, postDto.getId());
        assertEquals("Test Title", postDto.getTitle());
        assertEquals("Test Content", postDto.getContent());
        assertEquals("happy", postDto.getEmotion());
        assertEquals(testDate, postDto.getDate());
    }
    
    @Test
    @DisplayName("Should set default values correctly")
    void shouldSetDefaultValuesCorrectly() {
        PostDto postDto = PostDtoBuilder.create()
            .id(testId)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate)
            .build();
        
        assertEquals(0, postDto.getResonance());
        assertEquals(0, postDto.getView());
        assertEquals(0, postDto.getCollect());
        assertEquals("", postDto.getPreImg());
        assertFalse(postDto.isIsresonanced());
        assertFalse(postDto.isCollected());
        assertEquals("", postDto.getTopics());
        assertEquals("", postDto.getAiMetadata());
        assertFalse(postDto.isFlagged());
        assertEquals("", postDto.getFlagReason());
        assertEquals(0, postDto.getCommentsCount());
    }
    
    @Test
    @DisplayName("Should handle string ID conversion")
    void shouldHandleStringIdConversion() {
        String idString = testId.toString();
        
        PostDto postDto = PostDtoBuilder.create()
            .id(idString)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate)
            .build();
        
        assertEquals(testId, postDto.getId());
    }
    
    @Test
    @DisplayName("Should handle invalid string ID gracefully")
    void shouldHandleInvalidStringIdGracefully() {
        PostDtoBuilder builder = PostDtoBuilder.create()
            .id("invalid-uuid")
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate);
        
        // Should not throw exception, but ID will be null
        PostDto postDto = builder.buildUnsafe();
        assertNull(postDto.getId());
    }
    
    @Test
    @DisplayName("Should enforce non-negative counts")
    void shouldEnforceNonNegativeCounts() {
        PostDto postDto = PostDtoBuilder.create()
            .id(testId)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate)
            .resonance(-5)
            .view(-10)
            .collect(-2)
            .build();
        
        assertEquals(0, postDto.getResonance());
        assertEquals(0, postDto.getView());
        assertEquals(0, postDto.getCollect());
    }
    
    @Test
    @DisplayName("Should handle null string fields gracefully")
    void shouldHandleNullStringFieldsGracefully() {
        PostDto postDto = PostDtoBuilder.create()
            .id(testId)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate)
            .preImg(null)
            .topics(null)
            .aiMetadata(null)
            .flagReason(null)
            .build();
        
        assertEquals("", postDto.getPreImg());
        assertEquals("", postDto.getTopics());
        assertEquals("", postDto.getAiMetadata());
        assertEquals("", postDto.getFlagReason());
    }
    
    @Test
    @DisplayName("Should set date to now when using dateNow()")
    void shouldSetDateToNowWhenUsingDateNow() {
        PostDto postDto = PostDtoBuilder.create()
            .id(testId)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .dateNow()
            .build();
        
        assertNotNull(postDto.getDate());
        assertTrue(postDto.getDate().isAfter(testDate.minusSeconds(1)));
        assertTrue(postDto.getDate().isBefore(testDate.plusSeconds(2)));
    }
    
    @Test
    @DisplayName("Should build from existing PostDto")
    void shouldBuildFromExistingPostDto() {
        PostDto original = PostDtoBuilder.create()
            .id(testId)
            .title("Original Title")
            .content("Original Content")
            .emotion("happy")
            .date(testDate)
            .resonance(10)
            .view(20)
            .collect(5)
            .build();
        
        PostDto copy = PostDtoBuilder.from(original).build();
        
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(original.getContent(), copy.getContent());
        assertEquals(original.getEmotion(), copy.getEmotion());
        assertEquals(original.getDate(), copy.getDate());
        assertEquals(original.getResonance(), copy.getResonance());
        assertEquals(original.getView(), copy.getView());
        assertEquals(original.getCollect(), copy.getCollect());
    }
    
    @Test
    @DisplayName("Should handle null PostDto in from() method")
    void shouldHandleNullPostDtoInFromMethod() {
        PostDtoBuilder builder = PostDtoBuilder.from(null);
        assertNotNull(builder);
        
        // Should build with default values
        PostDto postDto = builder.buildUnsafe();
        assertNull(postDto.getId());
        assertNull(postDto.getTitle());
        assertNull(postDto.getContent());
        assertNull(postDto.getEmotion());
        assertNull(postDto.getDate());
    }
    
    @Test
    @DisplayName("Should throw exception when building without required fields")
    void shouldThrowExceptionWhenBuildingWithoutRequiredFields() {
        PostDtoBuilder builder = PostDtoBuilder.create();
        
        // Missing required fields
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
    
    @Test
    @DisplayName("Should build unsafe without validation")
    void shouldBuildUnsafeWithoutValidation() {
        PostDto postDto = PostDtoBuilder.create().buildUnsafe();
        
        assertNotNull(postDto);
        // All fields will be null/default values
        assertNull(postDto.getId());
        assertNull(postDto.getTitle());
        assertNull(postDto.getContent());
        assertNull(postDto.getEmotion());
        assertNull(postDto.getDate());
    }
    
    @Test
    @DisplayName("Should provide fluent API")
    void shouldProvideFluentApi() {
        PostDtoBuilder builder = PostDtoBuilder.create()
            .id(testId)
            .title("Title")
            .content("Content")
            .emotion("happy")
            .date(testDate)
            .resonance(5)
            .view(10)
            .collect(2)
            .preImg("image.jpg")
            .isresonanced(true)
            .isCollected(false)
            .userId(testUserId)
            .topics("mental health")
            .aiMetadata("sentiment: positive")
            .flagged(false)
            .flagReason("")
            .author(null)
            .commentsCount(3);
        
        assertNotNull(builder);
        
        PostDto postDto = builder.build();
        assertEquals(testId, postDto.getId());
        assertEquals("Title", postDto.getTitle());
        assertEquals("Content", postDto.getContent());
        assertEquals("happy", postDto.getEmotion());
        assertEquals(testDate, postDto.getDate());
        assertEquals(5, postDto.getResonance());
        assertEquals(10, postDto.getView());
        assertEquals(2, postDto.getCollect());
        assertEquals("image.jpg", postDto.getPreImg());
        assertTrue(postDto.isIsresonanced());
        assertFalse(postDto.isCollected());
        assertEquals(testUserId, postDto.getUserId());
        assertEquals("mental health", postDto.getTopics());
        assertEquals("sentiment: positive", postDto.getAiMetadata());
        assertFalse(postDto.isFlagged());
        assertEquals("", postDto.getFlagReason());
        assertEquals(3, postDto.getCommentsCount());
    }
} 