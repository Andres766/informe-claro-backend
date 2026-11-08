package co.edu.ucc.informeclaro.usuarios.api;

import co.edu.ucc.informeclaro.comun.grpc.RespuestaGrpc;
import co.edu.ucc.informeclaro.grpc.usuarios.LoginRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.RegistroRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.SesionReply;
import co.edu.ucc.informeclaro.grpc.usuarios.TokenRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuarioReply;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuariosServicioGrpc;
import co.edu.ucc.informeclaro.usuarios.controller.UsuarioController;
import co.edu.ucc.informeclaro.usuarios.dto.CredencialesDto;
import co.edu.ucc.informeclaro.usuarios.dto.RegistroDto;
import co.edu.ucc.informeclaro.usuarios.dto.SesionDto;
import co.edu.ucc.informeclaro.usuarios.dto.UsuarioDto;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

/**
 * API gRPC (HTTP/2) del microservicio de usuarios: la única puerta de entrada desde los demás
 * módulos. Traduce mensajes protobuf a DTOs y delega en el controlador interno.
 */
@Component
public class UsuariosGrpcApi extends UsuariosServicioGrpc.UsuariosServicioImplBase {

    private final UsuarioController controlador;

    public UsuariosGrpcApi(UsuarioController controlador) {
        this.controlador = controlador;
    }

    @Override
    public void registrar(RegistroRequest peticion, StreamObserver<SesionReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aSesion(controlador.registrar(
                new RegistroDto(peticion.getNombre(), peticion.getEmail(), peticion.getPassword()))));
    }

    @Override
    public void iniciarSesion(LoginRequest peticion, StreamObserver<SesionReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aSesion(controlador.iniciarSesion(
                new CredencialesDto(peticion.getEmail(), peticion.getPassword()))));
    }

    @Override
    public void validarToken(TokenRequest peticion, StreamObserver<UsuarioReply> respuesta) {
        RespuestaGrpc.responder(respuesta, () -> aUsuario(controlador.usuarioDelToken(peticion.getToken())));
    }

    private static SesionReply aSesion(SesionDto sesion) {
        return SesionReply.newBuilder().setAccessToken(sesion.token()).setUsuario(aUsuario(sesion.usuario())).build();
    }

    private static UsuarioReply aUsuario(UsuarioDto u) {
        return UsuarioReply.newBuilder().setId(u.id()).setNombre(u.nombre()).setEmail(u.email()).build();
    }
}
