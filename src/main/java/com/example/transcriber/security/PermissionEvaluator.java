package com.example.audiototext.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);
    private static final String PRODUCT = "FDTranscriber";
    private static final String FEATURE = "FD_TRANSCRIPTION";

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String permissionString = permission.toString();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // Check if user has the required permission
        boolean hasPermission = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(permissionString));
        
        logger.debug("Permission check: {} for user {} = {}", permissionString, 
                authentication.getName(), hasPermission);
        
        return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}
