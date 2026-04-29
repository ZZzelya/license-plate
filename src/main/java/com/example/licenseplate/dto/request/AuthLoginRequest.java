package com.example.licenseplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginRequest {
    @NotBlank(message = "Логин обязателен")
    private String identifier;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
