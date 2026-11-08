package co.edu.ucc.informeclaro.usuarios;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.comun.grpc.ServidorGrpc;
import co.edu.ucc.informeclaro.grpc.usuarios.LoginRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.RegistroRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.SesionReply;
import co.edu.ucc.informeclaro.grpc.usuarios.TokenRequest;
import co.edu.ucc.informeclaro.grpc.usuarios.UsuariosServicioGrpc;
import co.edu.ucc.informeclaro.usuarios.model.Usuario;
import co.edu.ucc.informeclaro.usuarios.repository.UsuarioRepository;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Prueba de punta a punta del microservicio a través de su API gRPC real (HTTP/2). */
@SpringBootTest
@ActiveProfiles("test")
class UsuariosGrpcApiTest {

    // Generados con la versión anterior en Python (bcrypt 5 / PyJWT) para comprobar compatibilidad.
    static final String HASH_PYTHON = "$2b$12$iJ1p1ii7UD1YmWUSwgRl.uTOVnUhwJz9FxRbquUqPij6W9QY.iMZG";
    static final String TOKEN_PYTHON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI3IiwiaWF0IjoxNzkzNzUwMDAwLCJleHAiOjQxMDI0NDQ4MDB9.zPqPVw3L2ASjjE6m4UqqZB_zHkCYboOJ0ICwTgFaeqI";

    static ManagedChannel canal;
    static UsuariosServicioGrpc.UsuariosServicioBlockingStub cliente;

    @Autowired
    ServidorGrpc servidor;
    @Autowired
    UsuarioRepository repositorio;

    @BeforeAll
    static void sinCanal() {
        canal = null;
    }

    private UsuariosServicioGrpc.UsuariosServicioBlockingStub cliente() {
        if (canal == null) {
            canal = CanalesGrpc.crear("127.0.0.1:" + servidor.puerto());
            cliente = UsuariosServicioGrpc.newBlockingStub(canal);
        }
        return cliente;
    }

    @AfterAll
    static void cerrar() {
        if (canal != null) {
            canal.shutdownNow();
        }
    }

    @Test
    void registraIniciaSesionYValidaElToken() {
        SesionReply sesion = cliente().registrar(RegistroRequest.newBuilder()
                .setNombre("  Paciente Prueba ").setEmail("Prueba@InformeClaro.co").setPassword("clave-segura-1").build());
        assertThat(sesion.getUsuario().getEmail()).isEqualTo("prueba@informeclaro.co");
        assertThat(sesion.getUsuario().getNombre()).isEqualTo("Paciente Prueba");

        SesionReply login = cliente().iniciarSesion(LoginRequest.newBuilder()
                .setEmail("prueba@informeclaro.co").setPassword("clave-segura-1").build());
        assertThat(cliente().validarToken(TokenRequest.newBuilder().setToken(login.getAccessToken()).build()).getId())
                .isEqualTo(sesion.getUsuario().getId());
    }

    @Test
    void rechazaCorreoRepetidoYCredencialesMalasConElMismoMensaje() {
        cliente().registrar(RegistroRequest.newBuilder().setNombre("Ana").setEmail("ana@prueba.co").setPassword("clave-segura-1").build());

        StatusRuntimeException repetido = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().registrar(
                RegistroRequest.newBuilder().setNombre("Ana").setEmail("ANA@prueba.co").setPassword("otra-clave-1").build()));
        assertThat(repetido.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
        assertThat(repetido.getStatus().getDescription()).isEqualTo("correo_repetido");

        StatusRuntimeException mala = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().iniciarSesion(
                LoginRequest.newBuilder().setEmail("ana@prueba.co").setPassword("equivocada").build()));
        StatusRuntimeException noExiste = catchThrowableOfType(StatusRuntimeException.class, () -> cliente().iniciarSesion(
                LoginRequest.newBuilder().setEmail("nadie@prueba.co").setPassword("equivocada").build()));
        assertThat(mala.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(mala.getStatus().getDescription()).isEqualTo(noExiste.getStatus().getDescription()).isEqualTo("credenciales");
    }

    @Test
    void tokenInvalidoEsNoAutenticado() {
        StatusRuntimeException e = catchThrowableOfType(StatusRuntimeException.class,
                () -> cliente().validarToken(TokenRequest.newBuilder().setToken("token-falso").build()));
        assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(e.getStatus().getDescription()).isEqualTo("inicia_sesion");
    }

    @Test
    void lasCuentasYTokensDeLaVersionPythonSiguenFuncionando() {
        Usuario existente = repositorio.save(new Usuario("Cuenta Antigua", "antigua@prueba.co", HASH_PYTHON));
        SesionReply login = cliente().iniciarSesion(LoginRequest.newBuilder()
                .setEmail("antigua@prueba.co").setPassword("clave-segura-1").build());
        assertThat(login.getUsuario().getId()).isEqualTo(existente.getId());

        // El token de Python apunta al usuario 7 con el mismo JWT_SECRET: se acepta si ese usuario existe.
        while (repositorio.count() < 7) {
            repositorio.save(new Usuario("Relleno " + repositorio.count(), "relleno" + repositorio.count() + "@prueba.co", HASH_PYTHON));
        }
        assertThat(repositorio.findById(7L)).isPresent();
        assertThat(cliente().validarToken(TokenRequest.newBuilder().setToken(TOKEN_PYTHON).build()).getId()).isEqualTo(7L);
    }
}
