package com.matchcatchup.service;

import com.matchcatchup.config.JwtService;
import com.matchcatchup.dto.AuthResponseDto;
import com.matchcatchup.dto.LoginRequestDto;
import com.matchcatchup.dto.SignupRequestDto;
import com.matchcatchup.entity.AppUser;
import com.matchcatchup.repository.AppUserRepository;
import com.matchcatchup.repository.UserMatchActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository users;
    private final UserMatchActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDto signup(SignupRequestDto request) {
        if (request == null || blank(request.getDisplayName()) || request.getDisplayName().trim().length() > 100
                || blank(request.getEmail()) || !request.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || request.getPassword() == null || request.getPassword().length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, valid email, and a password of at least 8 characters are required");
        String email = request.getEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        AppUser user = AppUser.builder().email(email).displayName(request.getDisplayName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword())).lastActiveAt(LocalDateTime.now()).build();
        return toResponse(users.save(user));
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        if (request == null || blank(request.getEmail()) || request.getPassword() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        AppUser user = users.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));
        user.setLastActiveAt(LocalDateTime.now());
        // When user logs in, ensure match viewing status is marked INACTIVE so catchup is guaranteed to trigger on opening live match
        activityRepository.markAllInactiveForUser(user.getEmail(), LocalDateTime.now());
        return toResponse(users.save(user));
    }

    @Transactional
    public AuthResponseDto profile(String email) {
        AppUser user = get(email);
        user.setLastActiveAt(LocalDateTime.now());
        return toResponse(users.save(user));
    }

    @Transactional
    public void touch(String email) { AppUser user = get(email); user.setLastActiveAt(LocalDateTime.now()); users.save(user); }

    private AppUser get(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists")); }
    private AuthResponseDto toResponse(AppUser user) { return AuthResponseDto.builder().token(jwtService.issue(user.getEmail())).tokenType("Bearer").email(user.getEmail()).displayName(user.getDisplayName()).lastActiveAt(user.getLastActiveAt() == null ? null : user.getLastActiveAt().toString()).build(); }
    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
