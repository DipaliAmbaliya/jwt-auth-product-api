package com.onedao.auth.controller;

import com.onedao.auth.dto.LoginRequest;
import com.onedao.auth.dto.OtpVerifyRequest;
import com.onedao.auth.dto.SignupRequest;
import com.onedao.auth.security.JwtAuthFilter;
import com.onedao.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final boolean exposeOtp;

    public AuthController(AuthService authService, @Value("${app.otp.expose-in-response}") boolean exposeOtp) {
        this.authService = authService;
        this.exposeOtp = exposeOtp;
    }

    @Operation(summary = "Sign up (country-restricted). Returns an OTP that must be verified.")
    @PostMapping("/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request,
                                                      HttpServletRequest http) {
        String otp = authService.signup(request, clientIp(http));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sign-up successful. Verify the OTP sent to " + request.email());
        if (exposeOtp) body.put("otp", otp);   // DEMO ONLY
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Verify the OTP to activate the account")
    @PostMapping("/auth/verify-otp")
    public Map<String, String> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyOtp(request);
        return Map.of("message", "Account verified. You can now log in.");
    }

    @Operation(summary = "Login. Returns a JWT bearer token; failure => 'Login Failed'")
    @PostMapping("/auth/login")
    public Map<String, String> login(@RequestBody(required = false) LoginRequest request) {
        String token = authService.login(request);
        return Map.of("message", "Login successful. Use this token on /api/dashboard",
                "tokenType", "Bearer", "token", token);
    }

    @Operation(summary = "Logout (revokes the JWT)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/auth/logout")
    public Map<String, String> logout(Authentication auth, HttpServletRequest http) {
        authService.logout(JwtAuthFilter.extractToken(http));
        return Map.of("message", auth.getName() + " Logged Out");
    }

    @Operation(summary = "Error endpoint for failed logins")
    @RequestMapping("/error")
    public ResponseEntity<Map<String, String>> error() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login Failed"));
    }

    @Operation(summary = "Dashboard - any authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/dashboard")
    public Map<String, String> dashboard(Authentication auth) {
        return Map.of("message", "Welcome " + auth.getName());
    }

    @Operation(summary = "Settings - Administrator only", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/settings")
    public Map<String, String> settings(Authentication auth) {
        return Map.of("message", "Settings - welcome Administrator " + auth.getName());
    }

    /**
     * Client IP. X-Forwarded-For is only trustworthy behind your own reverse proxy that overwrites it;
     * otherwise clients could spoof their location.
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
