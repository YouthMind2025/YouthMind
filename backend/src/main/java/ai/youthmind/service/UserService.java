package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.DraftRepository;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.UserEngagementRepository;
import ai.youthmind.repository.CommentEngagementRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private CommentEngagementRepository commentEngagementRepository;
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostCollectionRepository postCollectionRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Comprehensive account deletion that removes all user-related data
     * This method handles deletion in the correct order to avoid foreign key constraint violations
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        // 1. Delete user engagements (resonances, collections, views)
        userEngagementRepository.deleteByUserId(userId);
        
        // 2. Delete post resonances
        postResonanceRepository.deleteByUserId(userId);
        
        // 3. Delete post collections
        postCollectionRepository.deleteByUserId(userId);
        
        // 4. Delete comment engagements (likes)
        commentEngagementRepository.deleteByUserId(userId);
        
        // 5. Delete user's comments
        commentRepository.deleteByAuthorId(userId);
        
        // 6. Delete user's drafts
        draftRepository.deleteByUserId(userId);
        
        // 7. Delete user's conversations and messages
        conversationRepository.deleteByUserId(userId);
        
        // 8. Delete user's posts
        postRepository.deleteByUserId(userId);
        
        // 9. Finally, delete the user account
        userRepository.deleteById(userId);
    }
} 