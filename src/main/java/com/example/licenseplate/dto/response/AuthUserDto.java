package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDto {
    private Long id;
    private String username;
    private String role;
    private Long applicantId;
    private String fullName;
    private String email;
    private String passportNumber;
    private String phoneNumber;
    private String address;
}
