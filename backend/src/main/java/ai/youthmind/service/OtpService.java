package ai.youthmind.service;

import ai.youthmind.entity.OtpCode;
import ai.youthmind.repository.OtpCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class OtpService {
    
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${otp.expiration-minutes:10}")
    private int expirationMinutes;
    
    @Value("${otp.length:6}")
    private int otpLength;
    
    @Value("${otp.max-attempts:3}")
    private int maxAttempts;
    
    private static final SecureRandom random = new SecureRandom();
    private static final int MAX_CODES_PER_HOUR = 5; // Rate limiting
    
    public void sendOtp(String email) {
        // Rate limiting - check if user has requested too many codes recently
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int recentCodes = otpCodeRepository.countRecentCodesByEmail(email, oneHourAgo);
        
        if (recentCodes >= MAX_CODES_PER_HOUR) {
            throw new RuntimeException("Too many OTP requests. Please wait before requesting a new code.");
        }
        
        // Expire any existing unused codes for this email
        expireExistingCodes(email);
        
        // Generate new OTP code
        String otpCode = generateOtpCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
        
        // Save OTP to database
        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setCode(otpCode);
        otp.setCreatedAt(now);
        otp.setExpiresAt(expiresAt);
        otp.setUsed(false);
        otp.setAttempts(0);
        otp.setExpired(false);
        
        otpCodeRepository.save(otp);
        
        // Send email
        try {
            emailService.sendOtpEmail(email, otpCode);
            log.info("OTP sent successfully to email: {} (DEV: OTP code is {})", email, otpCode);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {} - Error: {}", email, e.getMessage());
            // Mark the OTP as expired since email failed
            otp.setExpired(true);
            otpCodeRepository.save(otp);
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }
    
    public boolean validateOtp(String email, String code) {
        Optional<OtpCode> otpOptional = otpCodeRepository.findByEmailAndCodeAndUsedFalse(email, code);
        
        if (otpOptional.isEmpty()) {
            log.warn("Invalid OTP attempt for email: {} with code: {}", email, code);
            return false;
        }
        
        OtpCode otp = otpOptional.get();
        
        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);
        
        // Check if OTP is still valid
        if (!otp.isValid()) {
            if (otp.getAttempts() >= maxAttempts) {
                otp.setExpired(true);
                log.warn("OTP expired due to max attempts reached for email: {}", email);
            }
            otpCodeRepository.save(otp);
            return false;
        }
        
        // Mark as used
        otp.setUsed(true);
        otpCodeRepository.save(otp);
        
        log.info("OTP validated successfully for email: {}", email);
        return true;
    }
    
    private void expireExistingCodes(String email) {
        List<OtpCode> existingCodes = otpCodeRepository.findByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        for (OtpCode code : existingCodes) {
            code.setExpired(true);
        }
        if (!existingCodes.isEmpty()) {
            otpCodeRepository.saveAll(existingCodes);
            log.debug("Expired {} existing OTP codes for email: {}", existingCodes.size(), email);
        }
    }
    
    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    // Scheduled cleanup - runs every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredCodes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Expire old codes
            otpCodeRepository.expireOldCodes(now);
            
            // Delete codes older than 24 hours
            LocalDateTime cutoff = now.minusHours(24);
            otpCodeRepository.deleteOldCodes(cutoff);
            
            log.debug("OTP cleanup completed at: {}", now);
        } catch (Exception e) {
            log.error("Error during OTP cleanup: {}", e.getMessage(), e);
        }
    }
    
    public boolean hasValidOtp(String email) {
        List<OtpCode> codes = otpCodeRepository.findByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        return codes.stream().anyMatch(OtpCode::isValid);
    }
    
    public LocalDateTime getOtpExpiration(String email) {
        List<OtpCode> codes = otpCodeRepository.findByEmailAndUsedFalseOrderByCreatedAtDesc(email);
        return codes.stream()
                .filter(OtpCode::isValid)
                .map(OtpCode::getExpiresAt)
                .findFirst()
                .orElse(null);
    }
}
