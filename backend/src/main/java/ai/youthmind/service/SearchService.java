package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.UserEngagement;
import ai.youthmind.repository.*;
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
    private UserEngagementRepository userEngagementRepository;

    @Autowired
    private CommentRepository commentRepository;

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
                postPage = postRepository.findByEmotion(emotion, pageable);
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
     * Advanced search with relevance scoring
     * Ranks results by relevance: title matches > content matches > topic matches
     */
    public SearchResponse advancedSearchPosts(String query, String emotion, int page, int limit, UUID currentUserId) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        // Get all posts that match the query (without pagination first)
        List<Post> allMatchingPosts;
        if (emotion != null && !emotion.trim().isEmpty()) {
            allMatchingPosts = postRepository.searchPostsByEmotion(query.trim(), emotion.trim());
        } else {
            allMatchingPosts = postRepository.searchPosts(query.trim());
        }

        // Calculate relevance scores and sort by relevance
        List<PostWithRelevance> postsWithRelevance = allMatchingPosts.stream()
            .map(post -> new PostWithRelevance(post, calculateRelevanceScore(post, query.trim())))
            .sorted((a, b) -> Double.compare(b.relevanceScore, a.relevanceScore))
            .collect(Collectors.toList());

        // Apply pagination
        int startIndex = page * limit;
        int endIndex = Math.min(startIndex + limit, postsWithRelevance.size());
        
        List<Post> paginatedPosts = postsWithRelevance.subList(startIndex, endIndex)
            .stream()
            .map(pwr -> pwr.post)
            .collect(Collectors.toList());

        // Convert to DTOs
        List<PostDto> postDtos = paginatedPosts.stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());

        // Create pagination info
        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page + 1); // Convert to 1-based
        pagination.setLimit(limit);
        pagination.setTotalItems(allMatchingPosts.size());
        pagination.setTotalPages((int) Math.ceil((double) allMatchingPosts.size() / limit));
        pagination.setHasNext(endIndex < allMatchingPosts.size());
        pagination.setHasPrevious(page > 0);

        SearchResponse response = new SearchResponse();
        response.setPosts(postDtos);
        response.setPagination(pagination);
        response.setTotalResults(allMatchingPosts.size());

        return response;
    }

    /**
     * Calculate relevance score for a post based on query
     * Higher scores indicate better matches
     */
    private double calculateRelevanceScore(Post post, String query) {
        String queryLower = query.toLowerCase();
        double score = 0.0;

        // Title matches (highest weight)
        if (post.getTitle() != null) {
            String titleLower = post.getTitle().toLowerCase();
            if (titleLower.contains(queryLower)) {
                score += 10.0; // Base score for title match
                
                // Bonus for exact title match
                if (titleLower.equals(queryLower)) {
                    score += 5.0;
                }
                
                // Bonus for title starting with query
                if (titleLower.startsWith(queryLower)) {
                    score += 3.0;
                }
                
                // Count word matches in title
                String[] titleWords = titleLower.split("\\s+");
                String[] queryWords = queryLower.split("\\s+");
                for (String queryWord : queryWords) {
                    for (String titleWord : titleWords) {
                        if (titleWord.contains(queryWord)) {
                            score += 1.0;
                        }
                    }
                }
            }
        }

        // Content matches (medium weight)
        if (post.getContent() != null) {
            String contentLower = post.getContent().toLowerCase();
            if (contentLower.contains(queryLower)) {
                score += 5.0; // Base score for content match
                
                // Count occurrences in content
                int occurrences = (contentLower.split(queryLower, -1).length - 1);
                score += occurrences * 0.5;
                
                // Count word matches in content
                String[] contentWords = contentLower.split("\\s+");
                String[] queryWords = queryLower.split("\\s+");
                for (String queryWord : queryWords) {
                    for (String contentWord : contentWords) {
                        if (contentWord.contains(queryWord)) {
                            score += 0.3;
                        }
                    }
                }
            }
        }

        // Topic matches (lower weight)
        if (post.getTopics() != null) {
            String topicsLower = post.getTopics().toLowerCase();
            if (topicsLower.contains(queryLower)) {
                score += 2.0; // Base score for topic match
                
                // Count word matches in topics
                String[] topicWords = topicsLower.split("[,\\s]+");
                String[] queryWords = queryLower.split("\\s+");
                for (String queryWord : queryWords) {
                    for (String topicWord : topicWords) {
                        if (topicWord.contains(queryWord)) {
                            score += 0.5;
                        }
                    }
                }
            }
        }

        // Emotion match bonus
        if (post.getEmotion() != null && post.getEmotion().toLowerCase().contains(queryLower)) {
            score += 1.0;
        }

        // Engagement boost (posts with more engagement get slight boost)
        double engagementBoost = Math.log(1 + post.getResonance() + post.getView() + post.getCollect()) * 0.1;
        score += engagementBoost;

        // Recency boost (newer posts get slight boost)
        long daysSinceCreation = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toDays();
        double recencyBoost = Math.max(0, 1.0 - (daysSinceCreation / 30.0)) * 0.5; // Boost decreases over 30 days
        score += recencyBoost;

        return score;
    }

    /**
     * Helper class to hold post with its relevance score
     */
    private static class PostWithRelevance {
        final Post post;
        final double relevanceScore;

        PostWithRelevance(Post post, double relevanceScore) {
            this.post = post;
            this.relevanceScore = relevanceScore;
        }
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
        dto.setEmotion(post.getEmotion());
        dto.setTopics(post.getTopics());
        dto.setPreImg(post.getPreImg());
        dto.setResonance(post.getResonance());
        dto.setCollect(post.getCollect());
        dto.setView(post.getView());
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

        // Set engagement flags
        Optional<UserEngagement> resonanceEngagement = userEngagementRepository.findByUserIdAndPostIdAndType(currentUserId, post.getId(), UserEngagement.EngagementType.RESONANCE);
        Optional<UserEngagement> collectEngagement = userEngagementRepository.findByUserIdAndPostIdAndType(currentUserId, post.getId(), UserEngagement.EngagementType.COLLECT);
        dto.setLiked(resonanceEngagement.isPresent());
        dto.setBookmarked(collectEngagement.isPresent());

        // Set comments count
        dto.setCommentsCount((int) commentRepository.countByPostId(post.getId()));

        return dto;
    }

    private TrendingPostDto convertToTrendingPostDto(Post post) {
        TrendingPostDto dto = new TrendingPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotion(post.getEmotion());
        dto.setResonance(post.getResonance());
        dto.setViews(post.getView());
        dto.setCollects(post.getCollect());
        dto.setTrendingScore(calculateTrendingScore(post));
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }

    private LeaderboardPostDto convertToLeaderboardPostDto(Post post, int rank) {
        LeaderboardPostDto dto = new LeaderboardPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotion(post.getEmotion());
        dto.setResonance(post.getResonance());
        dto.setViews(post.getView());
        dto.setCollects(post.getCollect());
        dto.setRank(rank);
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }

    private double calculateTrendingScore(Post post) {
        // Simple trending score calculation
        // In a real implementation, this would consider time decay, engagement velocity, etc.
        long hoursSinceCreation = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
        if (hoursSinceCreation == 0) hoursSinceCreation = 1;
        
        double engagementScore = post.getResonance() * 2 + post.getView() + post.getCollect() * 3;
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
