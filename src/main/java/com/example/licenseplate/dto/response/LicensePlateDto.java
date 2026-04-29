package com.example.licenseplate.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlateDto {
    private Long id;
    private String plateNumber;
    private String numberPart;
    private String series;
    private String regionCode;
    private String region;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime issueDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime expiryDate;
    private Long departmentId;
    private String departmentName;
}
