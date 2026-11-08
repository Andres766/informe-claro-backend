package co.edu.ucc.informeclaro.usuarios.controller;

import co.edu.ucc.informeclaro.usuarios.dto.CredencialesDto;
import co.edu.ucc.informeclaro.usuarios.dto.RegistroDto;
import co.edu.ucc.informeclaro.usuarios.dto.SesionDto;
import co.edu.ucc.informeclaro.usuarios.dto.UsuarioDto;
import co.edu.ucc.informeclaro.usuarios.model.Usuario;
import co.edu.ucc.informeclaro.usuarios.service.UsuarioService;
import org.springframework.stereotype.Component;

/**
 * Controlador interno del microservicio: recibe DTOs, invoca los servicios y devuelve DTOs.
 * No sabe nada de gRPC; eso lo resuelve {@code api/UsuariosGrpcApi}.
 */
@Component
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    public SesionDto registrar(RegistroDto datos) {
        Usuario usuario = servicio.registrar(datos.nombre(), datos.email(), datos.password());
        return new SesionDto(servicio.emitirToken(usuario), aDto(usuario));
    }

    public SesionDto iniciarSesion(CredencialesDto credenciales) {
        Usuario usuario = servicio.autenticar(credenciales.email(), credenciales.password());
        return new SesionDto(servicio.emitirToken(usuario), aDto(usuario));
    }

    public UsuarioDto usuarioDelToken(String token) {
        return aDto(servicio.desdeToken(token));
    }

    private static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail());
    }
}
