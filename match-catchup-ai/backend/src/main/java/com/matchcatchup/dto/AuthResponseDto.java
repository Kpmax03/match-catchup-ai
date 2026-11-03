package com.matchcatchup.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponseDto { private String token; private String tokenType; private String email; private String displayName; private String lastActiveAt; }
