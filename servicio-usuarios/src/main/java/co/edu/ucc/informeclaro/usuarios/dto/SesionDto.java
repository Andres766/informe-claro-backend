package co.edu.ucc.informeclaro.usuarios.dto;

/** Sesión iniciada: token JWT y usuario. */
public record SesionDto(String token, UsuarioDto usuario) {
}
