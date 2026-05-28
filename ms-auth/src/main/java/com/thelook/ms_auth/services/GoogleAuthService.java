package com.thelook.ms_auth.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.thelook.exceptions.InvalidAccessTokenException;
import com.thelook.exceptions.UnprocessableRequestException;
import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.models.dtos.LoginResponse;
import com.thelook.ms_auth.models.enums.UserRole;
import com.thelook.ms_auth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
public class GoogleAuthService {

    @Value("${jwt-token.expiration}")
    private long expirationTime;

    @Value("${jwt-token.refresh-expiration}")
    private long refreshExpirationTime;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleAuthService(UserRepository userRepository, JwtService jwtService,
                             StringRedisTemplate redisTemplate, GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public LoginResponse loginWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        String googleId = payload.getSubject();
        String email = payload.getEmail();

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> resolveByUsername(googleId, email));

        String creatorId = redisTemplate.opsForValue().get("user:profile:" + user.getId());
        String token = jwtService.generateToken(user, creatorId);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(token, refreshToken, "Bearer", expirationTime, refreshExpirationTime);
    }

    private User resolveByUsername(String googleId, String email) {
        return userRepository.findByUsername(email)
                .map(existing -> {
                    // link Google account to existing local user with same email as username
                    existing.setGoogleId(googleId);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> createGoogleUser(googleId, email));
    }

    private User createGoogleUser(String googleId, String email) {
        if (email == null || email.isBlank()) {
            throw new UnprocessableRequestException("Google account has no email associated");
        }
        User user = new User();
        user.setUsername(email);
        user.setGoogleId(googleId);
        user.setRole(UserRole.CREATOR);
        return userRepository.save(user);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidAccessTokenException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (InvalidAccessTokenException e) {
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidAccessTokenException("Failed to verify Google token");
        }
    }
}