package co.edu.ucc.informeclaro.interpretacion;

import co.edu.ucc.informeclaro.comun.datos.ConfiguracionBaseDatos;
import co.edu.ucc.informeclaro.comun.grpc.ConfiguracionServidorGrpc;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/**
 * Microservicio de interpretación. Expone gRPC ({@code api/InterpretacionGrpcApi}), consulta al
 * microservicio de documentos por gRPC y a la API de Anthropic (tercero) por REST.
 */
@SpringBootApplication
@Import({ConfiguracionServidorGrpc.class, ConfiguracionBaseDatos.class})
public class InterpretacionApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(InterpretacionApplication.class).properties("spring.config.name=servicio-interpretacion").run(args);
    }
}
