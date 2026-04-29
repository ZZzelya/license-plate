package com.example.licenseplate.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PassportChangeRequestDto {
    private Long id;
    private Long applicantId;
    private String applicantName;
    private String currentPassportNumber;
    private String requestedPassportNumber;
    private String contactPhone;
    private String status;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
