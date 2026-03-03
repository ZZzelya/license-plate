package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.Email;
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
public class ApplicantCreateDto {

    @NotBlank(message = "Имя обязательно для заполнения")
    private String fullName;

    @NotBlank(message = "Номер паспорта обязателен")
    @Pattern(regexp = "^[A-Z]{2}\\d{7}$",
        message = "Номер паспорта должен быть в формате: MP1234567")
    private String passportNumber;

    @Pattern(regexp = "^(\\+375|80)(25|29|33|44)\\d{7}$|^\\+375 \\((25|29|33|44)\\) \\d{3}-\\d{2}-\\d{2}$",
        message = "Телефон должен быть в формате: 80294982091 или +375 (33) 458-23-91")
    private String phoneNumber;

    @Email(message = "Неверный формат email")
    private String email;

    private String address;
}