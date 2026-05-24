package com.thelook.ms_auth.services;

import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.models.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Jwts.SIG.HS256.key().build();
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", Encoders.BASE64URL.encode(secretKey.getEncoded()));
        ReflectionTestUtils.setField(jwtService, "EXPIRATION_TIME", 900_000L);
        ReflectionTestUtils.setField(jwtService, "REFRESH_EXPIRATION_TIME", 7_200_000L);
    }

    private User user(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setPassword("encoded");
        u.setRole(UserRole.CREATOR);
        return u;
    }

    // --- generateToken ---

    @Test
    void generateToken_withCreatorId_includesCreatorIdClaim() {
        User u = user("alice");
        String creatorId = UUID.randomUUID().toString();

        String token = jwtService.generateToken(u, creatorId);

        assertThat(jwtService.extractCreatorId(token)).isEqualTo(creatorId);
    }

    @Test
    void generateToken_withoutCreatorId_omitsCreatorIdClaim() {
        User u = user("alice");

        String token = jwtService.generateToken(u, null);

        assertThat(jwtService.extractCreatorId(token)).isNull();
    }

    @Test
    void generateToken_setsTokenTypeAccess() {
        User u = user("alice");

        String token = jwtService.generateToken(u, null);

        String tokenType = jwtService.extractClaim(token, c -> c.get("tokenType", String.class));
        assertThat(tokenType).isEqualTo("access");
    }

    @Test
    void generateToken_includesUserIdRoleAndSubject() {
        User u = user("alice");

        String token = jwtService.generateToken(u, null);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractUserId(token)).isEqualTo(u.getId().toString());
        assertThat(jwtService.extractRole(token)).isEqualTo("CREATOR");
    }

    // --- generateRefreshToken ---

    @Test
    void generateRefreshToken_setsTokenTypeRefresh() {
        User u = user("alice");

        String token = jwtService.generateRefreshToken(u);

        String tokenType = jwtService.extractClaim(token, c -> c.get("tokenType", String.class));
        assertThat(tokenType).isEqualTo("refresh");
    }

    @Test
    void generateRefreshToken_includesUserIdAndSubject() {
        User u = user("alice");

        String token = jwtService.generateRefreshToken(u);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractUserId(token)).isEqualTo(u.getId().toString());
    }

    @Test
    void generateRefreshToken_doesNotIncludeRoleClaim() {
        User u = user("alice");

        String token = jwtService.generateRefreshToken(u);

        String role = jwtService.extractClaim(token, c -> c.get("role", String.class));
        assertThat(role).isNull();
    }

    // --- isTokenValid ---

    @Test
    void isTokenValid_validAccessToken_returnsTrue() {
        String token = jwtService.generateToken(user("alice"), null);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_validRefreshToken_returnsTrue() {
        String token = jwtService.generateRefreshToken(user("alice"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "EXPIRATION_TIME", -1_000L);
        String token = jwtService.generateToken(user("alice"), null);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_invalidSignature_returnsFalse() {
        SecretKey otherKey = Jwts.SIG.HS256.key().build();
        String token = Jwts.builder()
                .subject("alice")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "CREATOR")
                .claim("tokenType", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    // --- isRefreshToken ---

    @Test
    void isRefreshToken_refreshToken_returnsTrue() {
        String token = jwtService.generateRefreshToken(user("alice"));
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    void isRefreshToken_accessToken_returnsFalse() {
        String token = jwtService.generateToken(user("alice"), null);
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    // --- extractExpiration ---

    @Test
    void extractExpiration_returnsFutureDate() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(user("alice"), null);

        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration.getTime()).isGreaterThan(before);
        assertThat(expiration.getTime()).isLessThanOrEqualTo(before + 900_000L + 100);
    }

    // --- extract methods ---

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken(user("bob"), null);
        assertThat(jwtService.extractUsername(token)).isEqualTo("bob");
    }

    @Test
    void extractUserId_returnsUserId() {
        User u = user("bob");
        String token = jwtService.generateToken(u, null);
        assertThat(jwtService.extractUserId(token)).isEqualTo(u.getId().toString());
    }

    @Test
    void extractRole_returnsRole() {
        User u = user("bob");
        u.setRole(UserRole.ADMIN);
        String token = jwtService.generateToken(u, null);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void extractCreatorId_presentClaim_returnsValue() {
        String creatorId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(user("bob"), creatorId);
        assertThat(jwtService.extractCreatorId(token)).isEqualTo(creatorId);
    }

    @Test
    void extractCreatorId_missingClaim_returnsNull() {
        String token = jwtService.generateToken(user("bob"), null);
        assertThat(jwtService.extractCreatorId(token)).isNull();
    }
}