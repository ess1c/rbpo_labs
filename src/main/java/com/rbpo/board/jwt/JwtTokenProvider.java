package com.rbpo.board.jwt;

import com.rbpo.board.model.Role;
import com.rbpo.board.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong}")
    private String secret;

    @Value("${jwt.access-token-expiration:900}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private Long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("type", "access");
        
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.SECONDS)))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("sessionId", sessionId);
        claims.put("type", "refresh");
        
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.SECONDS)))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String expectedType) {
        try {
            String tokenType = extractTokenType(token);
            if (!expectedType.equals(tokenType)) {
                return false;
            }
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateAccessToken(String token) {
        return validateToken(token, "access");
    }

    public Boolean validateRefreshToken(String token) {
        return validateToken(token, "refresh");
    }
}

