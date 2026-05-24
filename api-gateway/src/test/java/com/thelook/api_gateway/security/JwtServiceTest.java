package com.thelook.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Jwts.SIG.HS256.key().build();
        String secret = Encoders.BASE64URL.encode(secretKey.getEncoded());
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", secret);
    }

    private String buildToken(String username, String userId, String role, String creatorId, long expirationMs) {
        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey);
        if (creatorId != null) {
            builder.claim("creatorId", creatorId);
        }
        return builder.compact();
    }

    @Test
    void extractUsername_validToken_returnsSubject() {
        String token = buildToken("user1", UUID.randomUUID().toString(), "CREATOR", null, 60_000);
        assertThat(jwtService.extractUsername(token)).isEqualTo("user1");
    }

    @Test
    void extractUserId_validToken_returnsUserId() {
        String userId = UUID.randomUUID().toString();
        String token = buildToken("user1", userId, "CREATOR", null, 60_000);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractRole_validToken_returnsRole() {
        String token = buildToken("user1", UUID.randomUUID().toString(), "ADMIN", null, 60_000);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractCreatorId_tokenWithCreatorId_returnsCreatorId() {
        String creatorId = UUID.randomUUID().toString();
        String token = buildToken("user1", UUID.randomUUID().toString(), "CREATOR", creatorId, 60_000);
        assertThat(jwtService.extractCreatorId(token)).isEqualTo(creatorId);
    }

    @Test
    void extractCreatorId_tokenWithoutCreatorId_returnsNull() {
        String token = buildToken("user1", UUID.randomUUID().toString(), "CREATOR", null, 60_000);
        assertThat(jwtService.extractCreatorId(token)).isNull();
    }

    @Test
    void extractClaims_expiredToken_throwsException() {
        String token = buildToken("user1", UUID.randomUUID().toString(), "CREATOR", null, -1_000);
        assertThatThrownBy(() -> jwtService.extractClaims(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractClaims_invalidSignature_throwsException() {
        SecretKey otherKey = Jwts.SIG.HS256.key().build();
        String token = Jwts.builder()
                .subject("user1")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "CREATOR")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();
        assertThatThrownBy(() -> jwtService.extractClaims(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractClaims_malformedToken_throwsException() {
        assertThatThrownBy(() -> jwtService.extractClaims("not.a.jwt"))
                .isInstanceOf(Exception.class);
    }
}