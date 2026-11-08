package co.edu.ucc.informeclaro.infraestructura.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/auth/registro. */
public record RegistroRequest(
        @NotNull @Size(min = 2, max = 120) String nombre,
        @NotNull @Pattern(regexp = Formatos.CORREO, message = "value is not a valid email address") String email,
        @NotNull @Size(min = 8, max = 72) String password) {
}
