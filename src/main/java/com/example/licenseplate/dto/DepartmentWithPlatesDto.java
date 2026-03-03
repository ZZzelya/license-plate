package com.example.licenseplate.dto;

import com.example.licenseplate.dto.response.LicensePlateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentWithPlatesDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private String region;
    private Boolean isActive;
    private List<LicensePlateDto> licensePlates;
}