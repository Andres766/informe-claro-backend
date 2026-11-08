package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de registro e inicio de sesión. */
public record SesionOut(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        UsuarioOut usuario) {
}
