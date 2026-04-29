package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlateCreateDto {

    @NotBlank(message = "Номер обязателен")
    @Pattern(regexp = "^(\\d{4}|E\\d{3})$", message = "Номер должен быть в формате 3256 или E000")
    private String plateNumber;

    @NotBlank(message = "Серия обязательна")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Серия должна содержать 2 латинские буквы")
    private String series;

    @Pattern(regexp = "^[0-8]$", message = "Код региона должен быть цифрой от 0 до 8")
    private String regionCode;

    @NotNull(message = "ID отделения обязателен")
    private Long departmentId;
}
