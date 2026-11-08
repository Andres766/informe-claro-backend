package co.edu.ucc.informeclaro.infraestructura.service;

import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.infraestructura.cliente.UsuariosGrpcCliente;
import co.edu.ucc.informeclaro.infraestructura.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Resuelve el usuario de la petición a partir del encabezado Authorization: Bearer. */
@Service
public class AutenticacionService {

    private final UsuariosGrpcCliente usuarios;

    public AutenticacionService(UsuariosGrpcCliente usuarios) {
        this.usuarios = usuarios;
    }

    public UsuarioReply usuarioActual(String autorizacion) {
        String token = extraerToken(autorizacion);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "inicia_sesion");
        }
        return usuarios.validarToken(token); // UNAUTHENTICATED por gRPC si el token no es válido
    }

    static String extraerToken(String autorizacion) {
        if (autorizacion == null) {
            return null;
        }
        String valor = autorizacion.strip();
        if (valor.length() <= 7 || !valor.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = valor.substring(7).strip();
        return token.isEmpty() ? null : token;
    }
}
