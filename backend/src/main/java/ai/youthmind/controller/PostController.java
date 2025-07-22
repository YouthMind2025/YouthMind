package ai.youthmind.controller;

import ai.youthmind.entity.Post;
import ai.youthmind.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/posts")
@CrossOrigin(origins = "*")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String content = request.get("content");
            
            if (title == null || title.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Title is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Content is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Post post = postService.createPost(title, content);
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("id", post.getId().toString());
            data.put("title", post.getTitle());
            data.put("content", post.getContent());
            data.put("authorId", post.getAuthorId().toString());
            data.put("createdAt", post.getCreatedAt().toString());
            data.put("updatedAt", post.getUpdatedAt().toString());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create post: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPosts() {
        try {
            List<Post> posts = postService.getAllPosts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", posts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch posts: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 