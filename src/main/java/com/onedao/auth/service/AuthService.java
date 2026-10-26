package com.onedao.auth.service;

import com.onedao.auth.dto.LoginRequest;
import com.onedao.auth.dto.OtpVerifyRequest;
import com.onedao.auth.dto.SignupRequest;
import com.onedao.auth.exception.BadRequestException;
import com.onedao.auth.exception.ConflictException;
import com.onedao.auth.exception.LoginFailedException;
import com.onedao.auth.model.UserType;
import com.onedao.auth.security.JwtService;
import com.onedao.auth.security.TokenBlacklist;
import com.onedao.auth.service.PendingSignupStore.Pending;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InMemoryUserDetailsManager userManager;
    private final PendingSignupStore pendingStore;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final GeoLocationService geoService;
    private final long otpExpiryMinutes;

    public AuthService(InMemoryUserDetailsManager userManager, PendingSignupStore pendingStore,
                       AuthenticationManager authenticationManager, PasswordEncoder encoder,
                       JwtService jwtService, TokenBlacklist blacklist, GeoLocationService geoService,
                       @Value("${app.otp.expiry-minutes}") long otpExpiryMinutes) {
        this.userManager = userManager;
        this.pendingStore = pendingStore;
        this.authenticationManager = authenticationManager;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.geoService = geoService;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    /** Registers a pending user and returns the generated OTP (caller decides whether to expose it). */
    public synchronized String signup(SignupRequest req, String clientIp) {
        geoService.assertCountryAllowed(clientIp);                      // country restriction

        if (userManager.userExists(req.username())) throw new ConflictException("Username already exists");

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        pendingStore.add(new Pending(req.username(), req.email(), encoder.encode(req.password()),
                UserType.fromLabel(req.userType()), encoder.encode(otp),
                Instant.now().plusSeconds(otpExpiryMinutes * 60)));

        // Demo only: a real system would e-mail/SMS the OTP.
        log.info("OTP for {} (valid {} min): {}", req.email(), otpExpiryMinutes, otp);
        return otp;
    }

    /** On a correct OTP the user is created in Spring's InMemoryUserDetailsManager. */
    public void verifyOtp(OtpVerifyRequest req) {
        Pending p = pendingStore.findByEmail(req.email())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));
        if (Instant.now().isAfter(p.otpExpiresAt()) || !encoder.matches(req.otp(), p.otpHash())) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        UserDetails user = User.builder()
                .username(p.username())
                .password(p.passwordHash())          // already BCrypt-encoded
                .roles(p.userType().getRole())
                .build();
        userManager.createUser(user);
        pendingStore.complete(p);
    }

    /** Authenticates via AuthenticationManager. Any failure => "Login Failed". Returns a JWT. */
    public String login(LoginRequest req) {
        if (req == null || req.username() == null || req.password() == null) throw new LoginFailedException();
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            return jwtService.generateToken((UserDetails) auth.getPrincipal());
        } catch (AuthenticationException e) {
            throw new LoginFailedException();
        }
    }

    /** Revokes the token so it can no longer be used. */
    public void logout(String token) {
        if (token == null) return;
        jwtService.parse(token).ifPresent(c -> blacklist.revoke(c.getId(), c.getExpiration()));
    }
}
