package co.edu.ucc.informeclaro.documentos;

import co.edu.ucc.informeclaro.comun.datos.ConfiguracionBaseDatos;
import co.edu.ucc.informeclaro.comun.grpc.ConfiguracionServidorGrpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/** Microservicio de documentos. Solo expone su API gRPC ({@code api/DocumentosGrpcApi}). */
@SpringBootApplication
@Import({ConfiguracionServidorGrpc.class, ConfiguracionBaseDatos.class})
public class DocumentosApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DocumentosApplication.class).properties("spring.config.name=servicio-documentos").run(args);
    }
}
