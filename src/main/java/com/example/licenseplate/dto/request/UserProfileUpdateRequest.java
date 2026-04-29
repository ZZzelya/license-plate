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
public class UserProfileUpdateRequest {
    @NotBlank(message = "Имя обязательно")
    private String fullName;

    @Pattern(
        regexp = "^(\\+375|80)(25|29|33|44)\\d{7}$|^\\+375 \\((25|29|33|44)\\) \\d{3}-\\d{2}-\\d{2}$",
        message = "Телефон должен быть в формате +375291234567")
    private String phoneNumber;

    @Email(message = "Неверный формат email")
    private String email;

    private String address;
}
