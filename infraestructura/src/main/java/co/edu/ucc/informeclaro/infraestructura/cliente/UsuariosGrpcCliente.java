package co.edu.ucc.informeclaro.infraestructura.cliente;

import co.edu.ucc.informeclaro.grpc.usuarios.LoginRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.RegistroRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.SesionReply;
import co.edu.ucc.informeclaro.grpc.usuarios.TokenRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuariosServicioGrpc;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Cliente gRPC (HTTP/2) del microservicio de usuarios. */
@Component
public class UsuariosGrpcCliente {

    private final UsuariosServicioGrpc.UsuariosServicioBlockingStub stub;

    public UsuariosGrpcCliente(UsuariosServicioGrpc.UsuariosServicioBlockingStub stub) {
        this.stub = stub;
    }

    private UsuariosServicioGrpc.UsuariosServicioBlockingStub conPlazo() {
        return stub.withDeadlineAfter(15, TimeUnit.SECONDS);
    }

    public SesionReply registrar(String nombre, String email, String password) {
        return conPlazo().registrar(RegistroRequest.newBuilder().setNombre(nombre).setEmail(email).setPassword(password).build());
    }

    public SesionReply iniciarSesion(String email, String password) {
        return conPlazo().iniciarSesion(LoginRequest.newBuilder().setEmail(email).setPassword(password).build());
    }

    public UsuarioReply validarToken(String token) {
        return conPlazo().validarToken(TokenRequest.newBuilder().setToken(token).build());
    }
}
