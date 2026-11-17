package co.edu.ucc.informeclaro.infraestructura.dto;

/** Usuario en las respuestas REST. {@code rol}: "usuario" o "admin". */
public record UsuarioOut(long id, String nombre, String email, String rol) {
}
