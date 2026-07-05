package com.example.aiproject.lovable_clone.security;

import com.example.aiproject.lovable_clone.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

@Component
public class AuthUtil {
    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 100))
                .signWith(getSecretKey())
                .compact();
    }

    public JwtUserPrincipal verifyAccessToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey()) // This tells JJWT what key to verify against
                .build()                   // Builds the JwtParser
                .parseClaimsJws(token)     // This actually parses AND verifies the signature
                .getBody();
        //if claims are present it means token is verified and we will get the below response object
        Long userId = Long.parseLong(claims.get("userId", String.class));
        String username = claims.getSubject();
        return new JwtUserPrincipal(userId, username, new ArrayList<>());
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !((authentication.getPrincipal()) instanceof JwtUserPrincipal userPrincipal)) {
            throw new AuthenticationCredentialsNotFoundException("No jwt authentication found");
        }
        return userPrincipal.userId();

    }
}
