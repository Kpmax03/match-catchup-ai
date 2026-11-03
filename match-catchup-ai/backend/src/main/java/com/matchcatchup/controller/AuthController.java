package com.matchcatchup.controller;

import com.matchcatchup.dto.AuthResponseDto;
import com.matchcatchup.dto.LoginRequestDto;
import com.matchcatchup.dto.SignupRequestDto;
import com.matchcatchup.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor @CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;
    @PostMapping("/signup") public AuthResponseDto signup(@RequestBody SignupRequestDto request) { return authService.signup(request); }
    @PostMapping("/login") public AuthResponseDto login(@RequestBody LoginRequestDto request) { return authService.login(request); }
    @GetMapping("/me") public AuthResponseDto me(Principal principal) { return authService.profile(principal.getName()); }
}
