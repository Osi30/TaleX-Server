package com.talex.server.configs;

import com.talex.server.entities.Account;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long verificationTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.verification-token-expiration:1800000}") long verificationTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.verificationTokenExpiration = verificationTokenExpiration;
    }

    public String generateAccessToken(Account account) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(account.getAccountId().toString())
                .claim("role", account.getRole().getCode())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public UUID extractAccountId(String token) {
        Claims claims = extractClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String generateVerificationToken(UUID accountId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + verificationTokenExpiration);

        return Jwts.builder()
                .subject(accountId.toString())
                .claim("type", "verification")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public UUID extractVerificationAccountId(String token) {
        try {
            Claims claims = extractClaims(token);
            String type = claims.get("type", String.class);
            if (!"verification".equals(type)) {
                throw new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN);
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN,
                    "Token xác minh không hợp lệ hoặc đã hết hạn", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
