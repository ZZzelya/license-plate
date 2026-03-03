package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private String status;
    private LocalDateTime applicationDate;
    private LocalDateTime reservedUntil;
    private LocalDateTime paymentDate;
    private BigDecimal paymentAmount;
    private String notes;
    private String vehicleVin;
    private String vehicleModel;
    private Integer vehicleYear;

    private Long applicantId;
    private String applicantName;
    private String applicantPassport;

    private Long licensePlateId;
    private String licensePlateNumber;

    private Long departmentId;
    private String departmentName;

    private List<String> additionalServices;
}