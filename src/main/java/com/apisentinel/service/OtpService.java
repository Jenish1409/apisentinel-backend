package com.apisentinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store. Each entry holds the hashed password (from the first step),
 * the plaintext OTP (only used in memory for comparison), and an expiry time.
 * A scheduled cleanup could be added, but with 10-min expiry the map stays tiny.
 */
@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;

    private record OtpEntry(String encodedPassword, String otp, LocalDateTime expiresAt) {}

    private final Map<String, OtpEntry> pending = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Generate and store a 6-digit OTP for the given email. Returns the OTP string. */
    public String createOtp(String email, String encodedPassword) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        pending.put(email.toLowerCase(), new OtpEntry(encodedPassword, otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));
        logger.info("OTP created for {}", email);
        return otp;
    }

    /** Validate OTP. Returns the encodedPassword if valid, null otherwise. */
    public String validateAndConsume(String email, String otp) {
        OtpEntry entry = pending.get(email.toLowerCase());
        if (entry == null) {
            logger.warn("No pending OTP for {}", email);
            return null;
        }
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            pending.remove(email.toLowerCase());
            logger.warn("OTP expired for {}", email);
            return null;
        }
        if (!entry.otp().equals(otp)) {
            logger.warn("Invalid OTP for {}", email);
            return null;
        }
        pending.remove(email.toLowerCase());
        logger.info("OTP validated for {}", email);
        return entry.encodedPassword();
    }

    public boolean hasPending(String email) {
        OtpEntry entry = pending.get(email.toLowerCase());
        return entry != null && LocalDateTime.now().isBefore(entry.expiresAt());
    }
}
