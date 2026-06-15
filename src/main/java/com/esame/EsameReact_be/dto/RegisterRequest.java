package com.esame.EsameReact_be.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email non valida")
        @NotBlank(message = "Email obbligatoria")
        String email,

        @NotBlank(message = "Password obbligatoria")
        @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
        String password,

        String name
) {}
