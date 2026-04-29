package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PassportChangeRequestCreateRequest {
    @NotBlank(message = "Укажите новый номер паспорта")
    @Pattern(regexp = "^[A-Z]{2}\\d{7}$", message = "Паспорт должен быть в формате MP1234567")
    private String newPassportNumber;

    @NotBlank(message = "Укажите телефон для связи")
    private String contactPhone;
}
