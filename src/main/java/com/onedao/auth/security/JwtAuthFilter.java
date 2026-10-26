package com.onedao.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Reads "Authorization: Bearer <jwt>", validates it, and populates the SecurityContext. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, TokenBlacklist blacklist, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.userDetailsService = userDetailsService;
    }

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7).trim() : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<Claims> claims = jwtService.parse(token);
            if (claims.isPresent() && !blacklist.isRevoked(claims.get().getId())) {
                try {
                    UserDetails user = userDetailsService.loadUserByUsername(claims.get().getSubject());
                    var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (UsernameNotFoundException ignored) {
                    // user no longer exists => stay unauthenticated
                }
            }
        }
        chain.doFilter(request, response);
    }
}
