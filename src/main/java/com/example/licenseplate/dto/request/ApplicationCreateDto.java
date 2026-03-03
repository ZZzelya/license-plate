package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    private String vehicleVin;

    private String vehicleModel;

    private Integer vehicleYear;

    private List<Long> serviceIds;

    private String notes;
}