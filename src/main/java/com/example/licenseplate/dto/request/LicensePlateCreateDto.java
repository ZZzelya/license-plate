package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePlateCreateDto {

    @NotBlank(message = "Номерной знак обязателен")
    @Pattern(regexp = "^\\d{4} [A-Z]{2}-\\d$",
        message = "Номерной знак должен быть в формате: 1234 AB-7")
    private String plateNumber;

    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    private BigDecimal price;

    private String series;

    @NotNull(message = "ID отдела ГАИ обязателен")
    private Long departmentId;
}