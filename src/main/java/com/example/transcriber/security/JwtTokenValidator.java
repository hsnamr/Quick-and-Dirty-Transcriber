package com.example.audiototext.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

@Component
public class JwtTokenValidator {

    @Value("${jwt.public-key}")
    private String publicKeyString;

    @Value("${jwt.issuer}")
    private String issuer;

    public boolean validateToken(String token) {
        try {
            // TODO: Implement JWT token validation using public key
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKeyString)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Validate issuer
            if (!claims.getIssuer().equals(issuer)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        // TODO: Implement claims extraction
        return null;
    }
}
