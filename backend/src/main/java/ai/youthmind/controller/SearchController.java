package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.LeaderboardPostDto;
import ai.youthmind.dto.LeaderboardResponse;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.SearchResponse;
import ai.youthmind.dto.SuggestionsResponse;
import ai.youthmind.dto.TrendingPostDto;
import ai.youthmind.dto.TrendingResponse;
import ai.youthmind.dto.TrendingTopicDto;
import ai.youthmind.dto.TrendingTopicsResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.service.PostService;
import ai.youthmind.service.SearchService;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
@Tag(name = "Search & Discovery", description = "Search and discovery operations")
@SecurityRequirement(name = "bearer-jwt")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    @Operation(
        summary = "Search posts",
        description = "Search posts by query with filters and pagination"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    public ResponseEntity<ApiResponse<SearchResponse>> searchPosts(
            @RequestParam String q,
            @RequestParam(required = false) String emotion,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            // Get current user for engagement status
            UUID currentUserId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UUID) {
                    currentUserId = (UUID) authentication.getPrincipal();
                }
            } catch (Exception e) {
                // User not authenticated, continue without engagement status
            }

            // Perform search
            SearchResponse response = searchService.searchPosts(q, emotion, sort, page, limit, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/trending")
    @Operation(
        summary = "Get trending posts",
        description = "Get trending posts based on timeframe"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trending posts retrieved successfully")
    })
    public ResponseEntity<ApiResponse<TrendingResponse>> getTrendingPosts(
            @RequestParam(defaultValue = "week") String timeframe,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // Get trending posts
            TrendingResponse response = searchService.getTrendingPosts(timeframe, limit);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/leaderboard")
    @Operation(
        summary = "Get leaderboard",
        description = "Get leaderboard posts based on metric and timeframe"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    })
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @RequestParam(defaultValue = "resonance") String metric,
            @RequestParam(defaultValue = "week") String timeframe,
            @RequestParam(required = false) String emotion,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // Get leaderboard posts
            LeaderboardResponse response = searchService.getLeaderboard(metric, timeframe, emotion, limit);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/suggestions")
    @Operation(
        summary = "Get suggestions",
        description = "Get AI-powered suggestions based on context"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully")
    })
    public ResponseEntity<ApiResponse<SuggestionsResponse>> getSuggestions(
            @RequestParam String context) {
        
        try {
            // Get suggestions
            SuggestionsResponse response = searchService.getSuggestions(context);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/trending-topics")
    @Operation(
        summary = "Get trending topics",
        description = "Get trending topics with counts and growth"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trending topics retrieved successfully")
    })
    public ResponseEntity<ApiResponse<TrendingTopicsResponse>> getTrendingTopics(
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            // Get trending topics
            TrendingTopicsResponse response = searchService.getTrendingTopics(limit);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
}
