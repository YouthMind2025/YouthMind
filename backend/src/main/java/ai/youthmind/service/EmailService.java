package ai.youthmind.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${email.from}")
    private String fromEmail;
    
    @Value("${email.from-name}")
    private String fromName;
    
    @Value("${email.templates.otp-subject}")
    private String otpSubject;
    
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(otpSubject);
            message.setText(buildOtpEmailContent(otpCode));
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {} - Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }
    
    private String buildOtpEmailContent(String otpCode) {
        return String.format("""
            Hello,
            
            Your YouthMind verification code is: %s
            
            This code will expire in 10 minutes. Please do not share this code with anyone.
            
            If you didn't request this code, please ignore this email.
            
            Best regards,
            The YouthMind Team
            """, otpCode);
    }
    
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to YouthMind!");
            message.setText(buildWelcomeEmailContent(username));
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {} - Error: {}", toEmail, e.getMessage(), e);
            // Don't throw exception for welcome email failure
        }
    }
    
    private String buildWelcomeEmailContent(String username) {
        return String.format("""
            Hello %s,
            
            Welcome to YouthMind! We're excited to have you join our community.
            
            YouthMind is a platform designed to support your mental health and well-being journey. 
            You can now:
            - Share your thoughts and feelings safely
            - Connect with others who understand
            - Access AI-powered insights about your emotional patterns
            - Find support when you need it most
            
            If you have any questions or need help, don't hesitate to reach out to our support team.
            
            Take care,
            The YouthMind Team
            """, username != null ? username : "");
    }
}
