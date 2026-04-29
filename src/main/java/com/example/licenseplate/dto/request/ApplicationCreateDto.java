package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDto {
    @NotBlank(message = "Passport number is required")
    private String passportNumber;

    private String plateNumber;

    private String region;

    private Long departmentId;

    @Pattern(
        regexp = "^$|^[A-Za-z0-9]{17}$",
        message = "VIN must contain exactly 17 latin letters and digits"
    )
    private String vehicleVin;

    private String vehicleModel;

    private Integer vehicleYear;

    @Pattern(
        regexp = "^$|^(STANDARD|ELECTRIC)$",
        message = "Vehicle type must be STANDARD or ELECTRIC"
    )
    private String vehicleType;

    private List<Long> serviceIds;

    private String notes;
}
