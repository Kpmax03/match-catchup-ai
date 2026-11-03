package com.matchcatchup.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    private String username;
    private String email;
    private String password;
    private String fullName;
}
