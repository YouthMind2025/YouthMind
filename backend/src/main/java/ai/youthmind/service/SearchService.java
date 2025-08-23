package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * Search posts by query, emotion, and sort criteria
     */
    public SearchResponse searchPosts(String query, String emotion, String sort, int page, int limit, UUID currentUserId) {
        Pageable pageable = PageRequest.of(page - 1, limit, getSortFromString(sort));
        
        Page<Post> postPage;
        if (query != null && !query.trim().isEmpty()) {
            if (emotion != null && !emotion.trim().isEmpty()) {
                postPage = postRepository.searchPostsByEmotion(query, emotion, pageable);
            } else {
                postPage = postRepository.searchPosts(query, pageable);
            }
        } else {
            if (emotion != null && !emotion.trim().isEmpty()) {
                postPage = postRepository.findByEmotionTag(emotion, pageable);
            } else {
                postPage = postRepository.findAll(pageable);
            }
        }

        List<PostDto> postDtos = postPage.getContent().stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalItems((int) postPage.getTotalElements());
        pagination.setTotalPages(postPage.getTotalPages());
        pagination.setHasNext(postPage.hasNext());
        pagination.setHasPrevious(postPage.hasPrevious());

        SearchResponse response = new SearchResponse();
        response.setPosts(postDtos);
        response.setPagination(pagination);
        response.setTotalResults((int) postPage.getTotalElements());

        return response;
    }

    /**
     * Get trending posts based on timeframe
     */
    public TrendingResponse getTrendingPosts(String timeframe, int limit) {
        LocalDateTime startDate = getStartDateFromTimeframe(timeframe);
        
        Page<Post> trendingPostsPage = postRepository.findTrendingPosts(startDate, PageRequest.of(0, limit));
        List<Post> trendingPosts = trendingPostsPage.getContent();
        
        List<TrendingPostDto> trendingPostDtos = trendingPosts.stream()
            .map(this::convertToTrendingPostDto)
            .collect(Collectors.toList());

        TrendingResponse response = new TrendingResponse();
        response.setPosts(trendingPostDtos);
        response.setTimeframe(timeframe);
        response.setLimit(limit);

        return response;
    }

    /**
     * Get leaderboard posts by metric and timeframe
     */
    public LeaderboardResponse getLeaderboard(String metric, String timeframe, String emotion, int limit) {
        LocalDateTime startDate = getStartDateFromTimeframe(timeframe);
        
        List<Post> leaderboardPosts;
        switch (metric.toLowerCase()) {
            case "resonance":
                leaderboardPosts = postRepository.findMostLikedPosts(startDate, emotion, PageRequest.of(0, limit));
                break;
            case "views":
                leaderboardPosts = postRepository.findMostViewedPosts(startDate, emotion, PageRequest.of(0, limit));
                break;
            case "collects":
                leaderboardPosts = postRepository.findMostBookmarkedPosts(startDate, emotion, PageRequest.of(0, limit));
                break;
            default:
                leaderboardPosts = postRepository.findMostLikedPosts(startDate, emotion, PageRequest.of(0, limit));
        }

        List<LeaderboardPostDto> leaderboardPostDtos = leaderboardPosts.stream()
            .map(post -> convertToLeaderboardPostDto(post, leaderboardPosts.indexOf(post) + 1))
            .collect(Collectors.toList());

        LeaderboardResponse response = new LeaderboardResponse();
        response.setPosts(leaderboardPostDtos);
        response.setMetric(metric);
        response.setTimeframe(timeframe);
        response.setEmotion(emotion);
        response.setLimit(limit);

        return response;
    }

    /**
     * Get AI-powered suggestions based on context
     */
    public SuggestionsResponse getSuggestions(String context) {
        // This would typically integrate with an AI service
        // For now, return mock suggestions based on context
        List<String> suggestions = generateMockSuggestions(context);

        SuggestionsResponse response = new SuggestionsResponse();
        response.setSuggestions(suggestions);
        response.setContext(context);

        return response;
    }

    /**
     * Get trending topics with counts and growth
     */
    public TrendingTopicsResponse getTrendingTopics(int limit) {
        // This would typically analyze post topics over time
        // For now, return mock trending topics
        List<TrendingTopicDto> trendingTopics = generateMockTrendingTopics(limit);

        TrendingTopicsResponse response = new TrendingTopicsResponse();
        response.setTopics(trendingTopics);
        response.setLimit(limit);

        return response;
    }

    // Helper methods
    private Sort getSortFromString(String sort) {
        switch (sort.toLowerCase()) {
            case "newest":
                return Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest":
                return Sort.by(Sort.Direction.ASC, "createdAt");
            case "resonance":
                return Sort.by(Sort.Direction.DESC, "likes");
            case "views":
                return Sort.by(Sort.Direction.DESC, "views");
            case "collects":
                return Sort.by(Sort.Direction.DESC, "bookmarks");
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    private LocalDateTime getStartDateFromTimeframe(String timeframe) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeframe.toLowerCase()) {
            case "day":
                return now.minusDays(1);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "year":
                return now.minusYears(1);
            default:
                return now.minusWeeks(1);
        }
    }

    private PostDto convertToPostDto(Post post, UUID currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotionTag(post.getEmotionTag());
        dto.setTopics(post.getTopics());
        dto.setPreImg(post.getPreImg());
        dto.setLikes(post.getLikes());
        dto.setBookmarks(post.getBookmarks());
        dto.setViews(post.getViews());
        dto.setAiMetadata(post.getAiMetadata());
        dto.setFlagged(post.isFlagged());
        dto.setFlagReason(post.getFlagReason());
        dto.setFlaggedAt(post.getFlaggedAt());
        dto.setCreatedAt(post.getCreatedAt());
        
        // Set author information
        userRepository.findById(post.getUserId()).ifPresent(user -> {
            UserDto authorDto = new UserDto();
            authorDto.setId(user.getId());
            authorDto.setEmail(user.getEmail());
            authorDto.setUsername(user.getUsername());
            authorDto.setFirstName(user.getFirstName());
            authorDto.setLastName(user.getLastName());
            authorDto.setBio(user.getBio());
            authorDto.setAvatar(user.getAvatar());
            authorDto.setPostsCount(user.getPostsCount());
            authorDto.setLikesCount(user.getLikesCount());
            authorDto.setBookmarksCount(user.getBookmarksCount());
            authorDto.setCreatedAt(user.getCreatedAt());
            authorDto.setUpdatedAt(user.getUpdatedAt());
            dto.setAuthor(authorDto);
        });

        // Set engagement flags (would need to check user engagement)
        dto.setLiked(false); // TODO: Check if current user liked this post
        dto.setBookmarked(false); // TODO: Check if current user bookmarked this post
        dto.setCommentsCount(0); // TODO: Get actual comment count

        return dto;
    }

    private TrendingPostDto convertToTrendingPostDto(Post post) {
        TrendingPostDto dto = new TrendingPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotion(post.getEmotionTag());
        dto.setResonance(post.getLikes());
        dto.setViews(post.getViews());
        dto.setCollects(post.getBookmarks());
        dto.setTrendingScore(calculateTrendingScore(post));
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }

    private LeaderboardPostDto convertToLeaderboardPostDto(Post post, int rank) {
        LeaderboardPostDto dto = new LeaderboardPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotion(post.getEmotionTag());
        dto.setResonance(post.getLikes());
        dto.setViews(post.getViews());
        dto.setCollects(post.getBookmarks());
        dto.setRank(rank);
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }

    private double calculateTrendingScore(Post post) {
        // Simple trending score calculation
        // In a real implementation, this would consider time decay, engagement velocity, etc.
        long hoursSinceCreation = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hoursSinceCreation == 0) hoursSinceCreation = 1;
        
        double engagementScore = post.getLikes() * 2 + post.getViews() + post.getBookmarks() * 3;
        double timeDecay = Math.pow(0.95, hoursSinceCreation);
        
        return engagementScore * timeDecay;
    }

    private List<String> generateMockSuggestions(String context) {
        List<String> suggestions = new ArrayList<>();
        
        if (context.toLowerCase().contains("sad")) {
            suggestions.add("How to deal with sadness?");
            suggestions.add("Tips for feeling better");
            suggestions.add("Coping with difficult emotions");
            suggestions.add("Self-care practices");
        } else if (context.toLowerCase().contains("anxiety")) {
            suggestions.add("Managing anxiety effectively");
            suggestions.add("Breathing exercises for anxiety");
            suggestions.add("Grounding techniques");
            suggestions.add("When to seek professional help");
        } else if (context.toLowerCase().contains("happy")) {
            suggestions.add("How to maintain happiness");
            suggestions.add("Gratitude practices");
            suggestions.add("Spreading joy to others");
            suggestions.add("Celebrating small wins");
        } else {
            suggestions.add("How are you feeling today?");
            suggestions.add("What's on your mind?");
            suggestions.add("Share your thoughts");
            suggestions.add("Connect with others");
        }
        
        return suggestions;
    }

    private List<TrendingTopicDto> generateMockTrendingTopics(int limit) {
        List<TrendingTopicDto> topics = new ArrayList<>();
        
        topics.add(new TrendingTopicDto("mental health", 456, 0.25));
        topics.add(new TrendingTopicDto("photography", 234, 0.15));
        topics.add(new TrendingTopicDto("nature", 189, 0.30));
        topics.add(new TrendingTopicDto("self-care", 156, 0.20));
        topics.add(new TrendingTopicDto("anxiety", 123, 0.10));
        topics.add(new TrendingTopicDto("happiness", 98, 0.35));
        topics.add(new TrendingTopicDto("depression", 87, -0.05));
        topics.add(new TrendingTopicDto("mindfulness", 76, 0.40));
        
        return topics.stream().limit(limit).collect(Collectors.toList());
    }
}
