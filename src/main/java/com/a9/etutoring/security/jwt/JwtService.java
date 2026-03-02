package com.a9.etutoring.security.jwt;

import com.a9.etutoring.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private final String secret;
    private final long expirationSeconds;

    public JwtService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.jwt.secret must not be blank");
        }
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
            .subject(principal.getId().toString())
            .claim("username", principal.getUsername())
            .claim("email", principal.getEmail())
            .claim("role", principal.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = extractAllClaims(token);
        String username = claims.get("username", String.class);
        UUID userId = UUID.fromString(claims.getSubject());
        Date expiration = claims.getExpiration();

        return principal.getUsername().equals(username)
            && principal.getId().equals(userId)
            && expiration != null
            && expiration.toInstant().isAfter(Instant.now());
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes;
        if (secret.matches("^[A-Za-z0-9+/]+={0,2}$")) {
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (IllegalArgumentException ex) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
