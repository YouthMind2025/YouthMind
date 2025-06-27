package ai.youthmind.service;

import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    public Post createPost(UUID userId, PostCreateRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setEmotionTag(request.getEmotionTag());
        post.setAiMetadata(request.getAiMetadata());
        
        return postRepository.save(post);
    }
} 