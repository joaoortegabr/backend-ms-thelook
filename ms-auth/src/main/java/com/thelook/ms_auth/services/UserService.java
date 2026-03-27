package com.thelook.ms_auth.services;

import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.models.dtos.UserRequest;
import com.thelook.ms_auth.models.dtos.UserResponse;
import com.thelook.ms_auth.models.enums.UserRole;
import com.thelook.ms_auth.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CREATOR);

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getPassword(),
                savedUser.getRole());
    }
}
