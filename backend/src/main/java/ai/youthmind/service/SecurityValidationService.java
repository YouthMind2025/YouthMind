package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.Comment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecurityValidationService {
    
    // Content validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,30}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    
    // Dangerous content patterns
    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL),
        Pattern.compile("(?i)javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)onload\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)onclick\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<embed[^>]*>", Pattern.CASE_INSENSITIVE)
    };
    
    // Profanity and inappropriate content (basic list - extend as needed)
    private static final String[] INAPPROPRIATE_WORDS = {
        "spam", "scam", "hack", "exploit", "virus", "malware"
    };
    
    // User Validation
    public ValidationResult validateUser(User user) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Email validation
        if (!StringUtils.hasText(user.getEmail())) {
            errors.add("Email is required");
        } else if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            errors.add("Invalid email format");
        } else if (user.getEmail().length() > 255) {
            errors.add("Email is too long (maximum 255 characters)");
        }
        
        // Username validation
        if (StringUtils.hasText(user.getUsername())) {
            if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
                errors.add("Username must be 3-30 characters and contain only letters, numbers, hyphens, and underscores");
            }
            if (containsInappropriateContent(user.getUsername())) {
                errors.add("Username contains inappropriate content");
            }
        }
        
        // Name validation
        if (!StringUtils.hasText(user.getFirstName())) {
            errors.add("First name is required");
        } else if (user.getFirstName().length() > 100) {
            errors.add("First name is too long (maximum 100 characters)");
        } else if (containsDangerousContent(user.getFirstName())) {
            errors.add("First name contains dangerous content");
        }
        
        if (!StringUtils.hasText(user.getLastName())) {
            errors.add("Last name is required");
        } else if (user.getLastName().length() > 100) {
            errors.add("Last name is too long (maximum 100 characters)");
        } else if (containsDangerousContent(user.getLastName())) {
            errors.add("Last name contains dangerous content");
        }
        
        // Bio validation
        if (StringUtils.hasText(user.getBio())) {
            if (user.getBio().length() > 1000) {
                errors.add("Bio is too long (maximum 1000 characters)");
            }
            if (containsDangerousContent(user.getBio())) {
                errors.add("Bio contains dangerous content");
            }
            if (containsInappropriateContent(user.getBio())) {
                warnings.add("Bio may contain inappropriate content");
            }
        }
        
        // Avatar URL validation
        if (StringUtils.hasText(user.getAvatar())) {
            if (user.getAvatar().length() > 500) {
                errors.add("Avatar URL is too long (maximum 500 characters)");
            }
            if (!isValidUrl(user.getAvatar())) {
                errors.add("Invalid avatar URL format");
            }
        }
        
        // Count validation
        if (user.getPostsCount() != null && user.getPostsCount() < 0) {
            errors.add("Posts count cannot be negative");
        }
        if (user.getLikesCount() != null && user.getLikesCount() < 0) {
            errors.add("Likes count cannot be negative");
        }
        if (user.getBookmarksCount() != null && user.getBookmarksCount() < 0) {
            errors.add("Bookmarks count cannot be negative");
        }
        if (user.getLoginCount() != null && user.getLoginCount() < 0) {
            errors.add("Login count cannot be negative");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    // Post Validation
    public ValidationResult validatePost(Post post) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Title validation
        if (!StringUtils.hasText(post.getTitle())) {
            errors.add("Title is required");
        } else if (post.getTitle().length() > 255) {
            errors.add("Title is too long (maximum 255 characters)");
        } else if (containsDangerousContent(post.getTitle())) {
            errors.add("Title contains dangerous content");
        } else if (containsInappropriateContent(post.getTitle())) {
            warnings.add("Title may contain inappropriate content");
        }
        
        // Content validation
        if (!StringUtils.hasText(post.getContent())) {
            errors.add("Content is required");
        } else if (post.getContent().length() > 10000) {
            errors.add("Content is too long (maximum 10000 characters)");
        } else if (containsDangerousContent(post.getContent())) {
            errors.add("Content contains dangerous content");
        } else if (containsInappropriateContent(post.getContent())) {
            warnings.add("Content may contain inappropriate content");
        }
        
        // Topics validation
        if (StringUtils.hasText(post.getTopics())) {
            if (post.getTopics().length() > 1000) {
                errors.add("Topics are too long (maximum 1000 characters)");
            }
            if (containsDangerousContent(post.getTopics())) {
                errors.add("Topics contain dangerous content");
            }
        }
        
        // Image URL validation
        if (StringUtils.hasText(post.getPreImg())) {
            if (post.getPreImg().length() > 500) {
                errors.add("Image URL is too long (maximum 500 characters)");
            }
            if (!isValidUrl(post.getPreImg())) {
                errors.add("Invalid image URL format");
            }
        }
        
        // Count validation
        if (post.getLikes() != null && post.getLikes() < 0) {
            errors.add("Likes count cannot be negative");
        }
        if (post.getBookmarks() != null && post.getBookmarks() < 0) {
            errors.add("Bookmarks count cannot be negative");
        }
        if (post.getViews() != null && post.getViews() < 0) {
            errors.add("Views count cannot be negative");
        }
        if (post.getResonanceCount() != null && post.getResonanceCount() < 0) {
            errors.add("Resonance count cannot be negative");
        }
        if (post.getCollectCount() != null && post.getCollectCount() < 0) {
            errors.add("Collect count cannot be negative");
        }
        
        // Flag reason validation
        if (StringUtils.hasText(post.getFlagReason()) && post.getFlagReason().length() > 500) {
            errors.add("Flag reason is too long (maximum 500 characters)");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    // Comment Validation
    public ValidationResult validateComment(Comment comment) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Content validation
        if (!StringUtils.hasText(comment.getContent())) {
            errors.add("Comment content is required");
        } else if (comment.getContent().length() > 2000) {
            errors.add("Comment is too long (maximum 2000 characters)");
        } else if (containsDangerousContent(comment.getContent())) {
            errors.add("Comment contains dangerous content");
        } else if (containsInappropriateContent(comment.getContent())) {
            warnings.add("Comment may contain inappropriate content");
        }
        
        // Likes validation
        if (comment.getLikes() != null && comment.getLikes() < 0) {
            errors.add("Likes count cannot be negative");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    // Content Security Methods
    public boolean containsDangerousContent(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean containsInappropriateContent(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        for (String word : INAPPROPRIATE_WORDS) {
            if (lowerContent.contains(word.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    public String sanitizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        
        String sanitized = content;
        
        // Remove dangerous patterns
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }
    
    // URL Validation
    private boolean isValidUrl(String url) {
        try {
            java.net.URL validUrl = new java.net.URL(url);
            return "http".equals(validUrl.getProtocol()) || "https".equals(validUrl.getProtocol());
        } catch (Exception e) {
            return false;
        }
    }
    
    // Rate Limiting Validation
    public boolean isRateLimitExceeded(String identifier, int maxRequests, int timeWindowMinutes) {
        // This would typically use Redis or another cache
        // For now, return false (no rate limiting)
        // In production, implement proper rate limiting logic
        return false;
    }
    
    // Password Validation
    public ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (!StringUtils.hasText(password)) {
            errors.add("Password is required");
            return new ValidationResult(false, errors, warnings);
        }
        
        if (password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }
        
        if (password.length() > 128) {
            errors.add("Password is too long (maximum 128 characters)");
        }
        
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        if (!hasUppercase) {
            warnings.add("Password should contain at least one uppercase letter");
        }
        if (!hasLowercase) {
            warnings.add("Password should contain at least one lowercase letter");
        }
        if (!hasDigit) {
            warnings.add("Password should contain at least one digit");
        }
        if (!hasSpecial) {
            warnings.add("Password should contain at least one special character");
        }
        
        // Check for common weak passwords
        String[] commonPasswords = {"password", "123456", "password123", "admin", "qwerty"};
        for (String common : commonPasswords) {
            if (password.toLowerCase().contains(common)) {
                errors.add("Password contains common weak patterns");
                break;
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    // Validation Result Class
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getErrorsAsString() {
            return String.join(", ", errors);
        }
        
        public String getWarningsAsString() {
            return String.join(", ", warnings);
        }
    }
}
