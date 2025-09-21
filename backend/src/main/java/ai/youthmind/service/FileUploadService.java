package ai.youthmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.avatar.max-size:2097152}") // 2MB default
    private long maxFileSize;
    
    @Value("${app.avatar.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final int AVATAR_SIZE = 200; // 200x200 pixels
    
    /**
     * Upload and process avatar image
     * @param file The uploaded file
     * @param userId The user ID for unique naming
     * @return The file path/URL of the uploaded avatar
     * @throws IOException if file processing fails
     * @throws IllegalArgumentException if file validation fails
     */
    public String uploadAvatar(MultipartFile file, UUID userId) throws IOException {
        // Validate file
        validateAvatarFile(file);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "avatars");
        Files.createDirectories(uploadPath);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + extension;
        
        // Process and save the image
        Path filePath = uploadPath.resolve(filename);
        
        // Resize and optimize the image
        BufferedImage resizedImage = resizeImage(file);
        
        // Save the resized image
        ImageIO.write(resizedImage, extension, filePath.toFile());
        
        // Return the relative path for storage in database
        return "avatars/" + filename;
    }
    
    /**
     * Validate avatar file
     */
    private void validateAvatarFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file cannot be empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Avatar file size cannot exceed " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new IllegalArgumentException("Only image files (JPEG, PNG, GIF, WebP) are allowed for avatars");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid file extension. Allowed: " + ALLOWED_EXTENSIONS);
        }
    }
    
    /**
     * Check if the content type is allowed
     */
    private boolean isAllowedImageType(String contentType) {
        List<String> allowedTypesList = Arrays.asList(allowedTypes.split(","));
        return allowedTypesList.contains(contentType.toLowerCase());
    }
    
    /**
     * Check if the file has a valid extension
     */
    private boolean hasValidExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
    
    /**
     * Resize image to avatar size while maintaining aspect ratio
     */
    private BufferedImage resizeImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid image file");
        }
        
        // Calculate new dimensions maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            newWidth = AVATAR_SIZE;
            newHeight = (int) ((double) originalHeight / originalWidth * AVATAR_SIZE);
        } else {
            newHeight = AVATAR_SIZE;
            newWidth = (int) ((double) originalWidth / originalHeight * AVATAR_SIZE);
        }
        
        // Create new image with proper type
        BufferedImage resizedImage = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Enable anti-aliasing for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, AVATAR_SIZE, AVATAR_SIZE);
        
        // Center the image
        int x = (AVATAR_SIZE - newWidth) / 2;
        int y = (AVATAR_SIZE - newHeight) / 2;
        
        // Draw the resized image
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    /**
     * Delete avatar file
     */
    public void deleteAvatar(String avatarPath) {
        if (avatarPath != null && !avatarPath.isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir, avatarPath);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but don't throw exception
                System.err.println("Failed to delete avatar file: " + avatarPath + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Get avatar URL for serving
     */
    public String getAvatarUrl(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) {
            return null;
        }
        return "/uploads/" + avatarPath;
    }
}
