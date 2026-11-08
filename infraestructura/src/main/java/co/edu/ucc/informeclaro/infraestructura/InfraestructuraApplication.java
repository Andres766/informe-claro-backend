package co.edu.ucc.informeclaro.infraestructura;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Módulo de infraestructura: el ORQUESTADOR. Es lo único que ve el navegador (API REST) y
 * coordina a los microservicios por gRPC sobre HTTP/2. No tiene base de datos propia.
 */
@SpringBootApplication
public class InfraestructuraApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(InfraestructuraApplication.class).properties("spring.config.name=infraestructura").run(args);
    }
}
