package com.thelook.api_gateway.security;

import com.thelook.api_gateway.dtos.UserContext;
import com.thelook.api_gateway.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoggedUserTest {

    @AfterEach
    void clearContext() {
        LoggedUser.set(null);
    }

    @Test
    void set_and_get_returnsStoredContext() {
        UUID userId = UUID.randomUUID();
        UserContext context = new UserContext(userId, "user1", UserRole.CREATOR);

        LoggedUser.set(context);

        assertThat(LoggedUser.get()).isEqualTo(context);
    }

    @Test
    void getUserId_returnsUserIdFromContext() {
        UUID userId = UUID.randomUUID();
        LoggedUser.set(new UserContext(userId, "user1", UserRole.CREATOR));

        assertThat(LoggedUser.getUserId()).isEqualTo(userId);
    }

    @Test
    void getUsername_returnsUsernameFromContext() {
        LoggedUser.set(new UserContext(UUID.randomUUID(), "testuser", UserRole.CREATOR));

        assertThat(LoggedUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserRole_adminRole_returnsAdmin() {
        LoggedUser.set(new UserContext(UUID.randomUUID(), "admin", UserRole.ADMIN));

        assertThat(LoggedUser.getUserRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void getUserRole_creatorRole_returnsCreator() {
        LoggedUser.set(new UserContext(UUID.randomUUID(), "creator1", UserRole.CREATOR));

        assertThat(LoggedUser.getUserRole()).isEqualTo(UserRole.CREATOR);
    }

    @Test
    void set_overwritesPreviousContext() {
        LoggedUser.set(new UserContext(UUID.randomUUID(), "first", UserRole.CREATOR));

        UUID secondId = UUID.randomUUID();
        LoggedUser.set(new UserContext(secondId, "second", UserRole.ADMIN));

        assertThat(LoggedUser.getUsername()).isEqualTo("second");
        assertThat(LoggedUser.getUserId()).isEqualTo(secondId);
    }
}