package com.thelook.ms_auth.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.InvalidAccessTokenException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.exceptions.UnprocessableRequestException;
import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.models.dtos.LoginRequest;
import com.thelook.ms_auth.models.dtos.LoginResponse;
import com.thelook.ms_auth.models.dtos.RegisterRequest;
import com.thelook.ms_auth.models.dtos.RegisterResponse;
import com.thelook.ms_auth.models.enums.UserRole;
import com.thelook.ms_auth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CREATOR);

        User savedUser = userRepository.save(user);

        return new RegisterResponse(savedUser.getId(), savedUser.getUsername());
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnprocessableRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnprocessableRequestException("Invalid username or password");
        }

        String redisKey = "user:profile:" + user.getId();
        String creatorId = redisTemplate.opsForValue().get(redisKey);

        String token = jwtService.generateToken(user, creatorId);

        return new LoginResponse(token, "Bearer", EXPIRATION_TIME);
    }

    public LoginResponse refresh(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidAccessTokenException("Refresh access token is invalid or expired");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String creatorId = redisTemplate.opsForValue().get("user:profile:" + user.getId());

        String newToken = jwtService.generateToken(user, creatorId);

        return new LoginResponse(newToken, "Bearer", EXPIRATION_TIME);
    }

}