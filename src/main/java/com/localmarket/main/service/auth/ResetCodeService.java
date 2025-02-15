package com.localmarket.main.service.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResetCodeService {
    // Store reset codes with expiration time: email -> (code, expirationTime)
    private final Map<String, ResetCodeEntry> resetCodes = new ConcurrentHashMap<>();
    
    private static class ResetCodeEntry {
        String code;
        LocalDateTime expiresAt;
        
        ResetCodeEntry(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
    
    public String generateCode(String email) {
        String code = String.format("%06d", new Random().nextInt(999999));
        resetCodes.put(email, new ResetCodeEntry(code, LocalDateTime.now().plusMinutes(15)));
        return code;
    }
    
    public boolean verifyCode(String email, String code) {
        ResetCodeEntry entry = resetCodes.get(email);
        if (entry == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(entry.expiresAt)) {
            resetCodes.remove(email);
            return false;
        }
        
        if (entry.code.equals(code)) {
            resetCodes.remove(email); // Remove code after successful verification
            return true;
        }
        
        return false;
    }
    
    // Cleanup expired codes (call this periodically)
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        resetCodes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void scheduledCleanup() {
        cleanupExpiredCodes();
    }
} 