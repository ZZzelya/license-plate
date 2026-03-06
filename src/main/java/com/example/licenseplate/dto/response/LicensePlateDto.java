package com.example.licenseplate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlateDto {
    private Long id;
    private String plateNumber;
    private BigDecimal price;
    private String series;
    private LocalDateTime issueDate;
    private LocalDateTime expiryDate;
    private Long departmentId;
    private String departmentName;
}