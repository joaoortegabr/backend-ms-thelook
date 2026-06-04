package com.thelook.ms_auth.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.InvalidAccessTokenException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.exceptions.UnprocessableRequestException;
import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.models.dtos.*;
import com.thelook.ms_auth.models.enums.UserRole;
import com.thelook.ms_auth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "EXPIRATION_TIME", 900_000L);
        ReflectionTestUtils.setField(authService, "REFRESH_EXPIRATION_TIME", 7_200_000L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private User savedUser(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setPassword("hashed");
        u.setRole(UserRole.CREATOR);
        return u;
    }

    // =========================================================
    // register
    // =========================================================

    @Test
    void register_newUsername_savesUserAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest("alice", "pass123");
        User saved = savedUser("alice");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterResponse response = authService.register(request);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.id()).isEqualTo(saved.getId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CREATOR);
    }

    @Test
    void register_duplicateUsername_throwsBusinessRuleException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "pass")))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
    }

    // =========================================================
    // login
    // =========================================================

    @Test
    void login_validCredentials_withCreatorIdInRedis_returnsLoginResponse() {
        User user = savedUser("alice");
        String creatorId = UUID.randomUUID().toString();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(valueOps.get("user:profile:" + user.getId())).thenReturn(creatorId);
        when(jwtService.generateToken(user, creatorId)).thenReturn("access.token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh.token");

        LoginResponse response = authService.login(new LoginRequest("alice", "pass"));

        assertThat(response.token()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh.token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900_000L);
        assertThat(response.refreshExpiresIn()).isEqualTo(7_200_000L);
    }

    @Test
    void login_validCredentials_withoutCreatorIdInRedis_returnsLoginResponseWithNullCreatorId() {
        User user = savedUser("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(valueOps.get("user:profile:" + user.getId())).thenReturn(null);
        when(jwtService.generateToken(user, null)).thenReturn("access.token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh.token");

        LoginResponse response = authService.login(new LoginRequest("alice", "pass"));

        assertThat(response.token()).isEqualTo("access.token");
        verify(jwtService).generateToken(user, null);
    }

    @Test
    void login_unknownUsername_throwsUnprocessableRequestException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pass")))
                .isInstanceOf(UnprocessableRequestException.class);
    }

    @Test
    void login_wrongPassword_throwsUnprocessableRequestException() {
        User user = savedUser("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(UnprocessableRequestException.class);

        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_wrongPassword_doesNotRevealWhetherUsernameExists() {
        User user = savedUser("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // =========================================================
    // logout
    // =========================================================

    @Test
    void logout_validBearerToken_addsTokenToRedisBlocklist() {
        String token = "valid.access.token";
        Date expiration = new Date(System.currentTimeMillis() + 60_000);

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractExpiration(token)).thenReturn(expiration);

        authService.logout("Bearer " + token);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOps).set(eq("auth:blocklist:" + token), eq("1"), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void logout_tokenWithExpiredTtl_doesNotAddToBlocklist() {
        String token = "expiring.token";
        Date expiration = new Date(System.currentTimeMillis() - 1);

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractExpiration(token)).thenReturn(expiration);

        authService.logout("Bearer " + token);

        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void logout_nullHeader_throwsUnprocessableRequestException() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(UnprocessableRequestException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void logout_nonBearerHeader_throwsUnprocessableRequestException() {
        assertThatThrownBy(() -> authService.logout("Basic dXNlcjpwYXNz"))
                .isInstanceOf(UnprocessableRequestException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void logout_invalidToken_throwsInvalidAccessTokenException() {
        when(jwtService.isTokenValid("bad.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.logout("Bearer bad.token"))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
    }

    // =========================================================
    // refresh
    // =========================================================

    @Test
    void refresh_validRefreshToken_withCreatorIdInRedis_returnsNewAccessToken() {
        String refreshToken = "valid.refresh.token";
        User user = savedUser("alice");
        String creatorId = UUID.randomUUID().toString();

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractExpiration(refreshToken)).thenReturn(new Date(System.currentTimeMillis() + 7_200_000));
        when(jwtService.extractUsername(refreshToken)).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(valueOps.get("user:profile:" + user.getId())).thenReturn(creatorId);
        when(jwtService.generateToken(user, creatorId)).thenReturn("new.access.token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new.refresh.token");

        RefreshResponse response = authService.refresh(refreshToken);

        assertThat(response.token()).isEqualTo("new.access.token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900_000L);
    }

    @Test
    void refresh_validRefreshToken_withoutCreatorId_generatesTokenWithNullCreatorId() {
        String refreshToken = "valid.refresh.token";
        User user = savedUser("alice");

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractExpiration(refreshToken)).thenReturn(new Date(System.currentTimeMillis() + 7_200_000));
        when(jwtService.extractUsername(refreshToken)).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(valueOps.get("user:profile:" + user.getId())).thenReturn(null);
        when(jwtService.generateToken(user, null)).thenReturn("new.access.token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new.refresh.token");

        authService.refresh(refreshToken);

        verify(jwtService).generateToken(user, null);
    }

    @Test
    void refresh_invalidToken_throwsInvalidAccessTokenException() {
        when(jwtService.isTokenValid("bad.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad.token"))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void refresh_accessTokenPassedAsRefresh_throwsInvalidAccessTokenException() {
        String accessToken = "valid.access.token";
        when(jwtService.isTokenValid(accessToken)).thenReturn(true);
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(accessToken))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void refresh_userNotFound_throwsResourceNotFoundException() {
        String refreshToken = "valid.refresh.token";

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractExpiration(refreshToken)).thenReturn(new Date(System.currentTimeMillis() + 7_200_000));
        when(jwtService.extractUsername(refreshToken)).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jwtService, never()).generateToken(any(), any());
    }
}