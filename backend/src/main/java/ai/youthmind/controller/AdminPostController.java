package ai.youthmind.controller;

import ai.youthmind.dto.FlaggedPostDto;
import ai.youthmind.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrative endpoints for post moderation.
 */
@RestController
@RequestMapping("/admin/v1/posts")
@Tag(name = "Admin Posts", description = "Administrative operations on posts")
@SecurityRequirement(name = "bearer-jwt")
public class AdminPostController {

    @Autowired
    private PostService postService;

    @GetMapping("/flagged")
    @Operation(summary = "Get all flagged posts", description = "Returns a list of posts that have been flagged for moderation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flagged posts retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getFlaggedPosts() {
        List<FlaggedPostDto> flaggedPosts = postService.getFlaggedPosts();
        Map<String, Object> response = new HashMap<>();
        response.put("flagged_posts", flaggedPosts);
        response.put("total_pending", flaggedPosts.size());
        return ResponseEntity.ok(response);
    }
}