package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentCreateDto {
    @NotBlank(message = "Department name is required")
    private String name;

    private String address;

    @Pattern(
        regexp = "^$|^(\\+375\\d{9}|\\+375 \\((25|29|33|44)\\) \\d{3}-\\d{2}-\\d{2})$",
        message = "Phone number must be in format: +375 (29) 498-20-91"
    )
    private String phoneNumber;

    @NotBlank(message = "Region is required")
    private String region;
}
