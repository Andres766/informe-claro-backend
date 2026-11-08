package co.edu.ucc.informeclaro.informes;

import co.edu.ucc.informeclaro.comun.datos.ConfiguracionBaseDatos;
import co.edu.ucc.informeclaro.comun.grpc.ConfiguracionServidorGrpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/** Microservicio de informes. Solo expone su API gRPC ({@code api/InformesGrpcApi}). */
@SpringBootApplication
@Import({ConfiguracionServidorGrpc.class, ConfiguracionBaseDatos.class})
public class InformesApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(InformesApplication.class).properties("spring.config.name=servicio-informes").run(args);
    }
}
