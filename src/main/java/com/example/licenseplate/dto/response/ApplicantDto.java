package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantDto {
    private Long id;
    private String fullName;
    private String passportNumber;
    private String phoneNumber;
    private String email;
    private String address;
    private Integer applicationsCount;
    private String role;
    private Boolean hasUserAccount;
}
