package com.example.transcriber.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtTokenValidator.validateToken(token)) {
            Claims claims = jwtTokenValidator.extractClaims(token);
            if (claims != null) {
                // Extract user ID
                Object userIdObj = claims.get("user_id");
                if (userIdObj == null) {
                    userIdObj = claims.getSubject();
                }
                
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj != null) {
                    try {
                        userId = Long.parseLong(userIdObj.toString());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid user_id format in token");
                    }
                }
                
                if (userId != null) {
                    UserContext.setUserId(userId);
                    
                    // Extract permissions
                    Object permsObj = claims.get("permissions");
                    List<String> permissions = new ArrayList<>();
                    if (permsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> permsList = (List<Object>) permsObj;
                        for (Object perm : permsList) {
                            if (perm != null) {
                                permissions.add(perm.toString());
                            }
                        }
                    }
                    UserContext.setPermissions(permissions);
                    
                    // Set authentication
                    List<SimpleGrantedAuthority> authorities = permissions.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Alternative header
        String authToken = request.getHeader("auth-token");
        if (authToken != null) {
            return authToken;
        }

        return null;
    }
}
