package co.edu.ucc.informeclaro.infraestructura.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/auth/login. */
public record LoginRequest(
        @NotNull @Pattern(regexp = Formatos.CORREO, message = "value is not a valid email address") String email,
        @NotNull @Size(max = 72) String password) {
}
