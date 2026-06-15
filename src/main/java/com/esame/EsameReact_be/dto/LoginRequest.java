package com.esame.EsameReact_be.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email non valida")
        @NotBlank(message = "Email obbligatoria")
        String email,

        @NotBlank(message = "Password obbligatoria")
        String password
) {}
