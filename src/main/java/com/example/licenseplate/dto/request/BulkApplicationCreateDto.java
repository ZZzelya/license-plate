package com.example.licenseplate.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApplicationCreateDto {

    @NotNull(message = "Passport number is required")
    private String passportNumber;

    @NotEmpty(message = "At least one application is required")
    @Valid
    private List<ApplicationCreateDto> applications;
}