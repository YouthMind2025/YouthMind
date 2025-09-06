package ai.youthmind.service;

import ai.youthmind.entity.AuditLog;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPostCreation(Post post) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType("POST");
            auditLog.setEntityId(post.getId());
            auditLog.setAction(AuditLog.Action.CREATE);
            auditLog.setUserId(post.getUserId());
            auditLog.setNewValues(objectMapper.writeValueAsString(post));
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPostUpdate(Post oldPost, Post newPost) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType("POST");
            auditLog.setEntityId(newPost.getId());
            auditLog.setAction(AuditLog.Action.UPDATE);
            auditLog.setUserId(newPost.getLastModifiedBy());
            auditLog.setOldValues(objectMapper.writeValueAsString(oldPost));
            auditLog.setNewValues(objectMapper.writeValueAsString(newPost));
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPostDeletion(Post post) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType("POST");
            auditLog.setEntityId(post.getId());
            auditLog.setAction(AuditLog.Action.DELETE);
            auditLog.setUserId(post.getUserId());
            auditLog.setOldValues(objectMapper.writeValueAsString(post));
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPostFlagged(Post post, String reason, UUID moderatorId) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType("POST");
            auditLog.setEntityId(post.getId());
            auditLog.setAction(AuditLog.Action.UPDATE);
            auditLog.setUserId(moderatorId);
            auditLog.setNewValues("{\"flagged\": true, \"flagReason\": \"" + reason + "\"}");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPostUnflagged(Post post, UUID moderatorId) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType("POST");
            auditLog.setEntityId(post.getId());
            auditLog.setAction(AuditLog.Action.UPDATE);
            auditLog.setUserId(moderatorId);
            auditLog.setNewValues("{\"flagged\": false, \"flagReason\": null}");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("Failed to create audit log: " + e.getMessage());
        }
    }
    
    public boolean isHealthy() {
        try {
            auditLogRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}