package co.edu.ucc.informeclaro.usuarios;

import co.edu.ucc.informeclaro.comun.datos.ConfiguracionBaseDatos;
import co.edu.ucc.informeclaro.comun.grpc.ConfiguracionServidorGrpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/** Microservicio de usuarios. No expone HTTP: solo su API gRPC ({@code api/UsuariosGrpcApi}). */
@SpringBootApplication
@Import({ConfiguracionServidorGrpc.class, ConfiguracionBaseDatos.class})
public class UsuariosApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(UsuariosApplication.class).properties("spring.config.name=servicio-usuarios").run(args);
    }
}
