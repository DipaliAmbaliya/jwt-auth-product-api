package com.onedao.auth.service;

import com.onedao.auth.exception.ConflictException;
import com.onedao.auth.model.UserType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds sign-ups that are waiting for OTP verification (in memory).
 * Once verified, the user is moved into Spring's InMemoryUserDetailsManager.
 */
@Component
public class PendingSignupStore {

    public record Pending(String username, String email, String passwordHash, UserType userType,
                          String otpHash, Instant otpExpiresAt) {}

    private final Map<String, Pending> byEmail = new ConcurrentHashMap<>();
    private final Set<String> usernames = ConcurrentHashMap.newKeySet();
    private final Set<String> registeredEmails = ConcurrentHashMap.newKeySet();

    public synchronized void add(Pending p) {
        if (usernames.contains(p.username().toLowerCase())) throw new ConflictException("Username already exists");
        if (registeredEmails.contains(p.email().toLowerCase())) throw new ConflictException("Email already registered");
        usernames.add(p.username().toLowerCase());
        registeredEmails.add(p.email().toLowerCase());
        byEmail.put(p.email().toLowerCase(), p);
    }

    public Optional<Pending> findByEmail(String email) {
        return email == null ? Optional.empty() : Optional.ofNullable(byEmail.get(email.toLowerCase()));
    }

    /** Removes the pending entry after successful verification (email stays reserved). */
    public synchronized void complete(Pending p) {
        byEmail.remove(p.email().toLowerCase());
    }
}
