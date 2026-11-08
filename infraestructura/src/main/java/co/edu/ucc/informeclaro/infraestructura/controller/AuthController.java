package co.edu.ucc.informeclaro.infraestructura.controller;

import co.edu.ucc.informeclaro.grpc.usuarios.SesionReply;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.infraestructura.cliente.UsuariosGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.dto.LoginRequest;
import co.edu.ucc.informeclaro.infraestructura.dto.RegistroRequest;
import co.edu.ucc.informeclaro.infraestructura.dto.SesionOut;
import co.edu.ucc.informeclaro.infraestructura.dto.UsuarioOut;
import co.edu.ucc.informeclaro.infraestructura.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST para el navegador: registro, inicio de sesión y usuario actual. Delega por gRPC en usuarios. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuariosGrpcCliente usuarios;
    private final AutenticacionService autenticacion;

    public AuthController(UsuariosGrpcCliente usuarios, AutenticacionService autenticacion) {
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionOut registro(@Valid @RequestBody RegistroRequest datos) {
        return aSesion(usuarios.registrar(datos.nombre(), datos.email(), datos.password()));
    }

    @PostMapping("/login")
    public SesionOut login(@Valid @RequestBody LoginRequest datos) {
        return aSesion(usuarios.iniciarSesion(datos.email(), datos.password()));
    }

    @GetMapping("/yo")
    public UsuarioOut yo(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return aUsuario(autenticacion.usuarioActual(autorizacion));
    }

    private static SesionOut aSesion(SesionReply s) {
        return new SesionOut(s.getAccessToken(), "bearer", aUsuario(s.getUsuario()));
    }

    private static UsuarioOut aUsuario(UsuarioReply u) {
        return new UsuarioOut(u.getId(), u.getNombre(), u.getEmail());
    }
}
