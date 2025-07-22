package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public User createUser(String email, String otp) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(otp); // Simplified, should encrypt in production
        return userRepository.save(user);
    }
    
    public User authenticateUser(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        throw new RuntimeException("Invalid credentials");
    }
    
    public String generateToken(User user) {
        // Simplified, should use proper JWT library
        return "jwt_token_" + user.getId() + "_" + System.currentTimeMillis();
    }
    
    public User getDefaultUser() {
        // Get or create default user
        Optional<User> defaultUser = userRepository.findByEmail("default@example.com");
        if (defaultUser.isPresent()) {
            return defaultUser.get();
        }
        
        User user = new User();
        user.setEmail("default@example.com");
        user.setPassword("default");
        return userRepository.save(user);
    }
} 