package co.edu.ucc.informeclaro.comun.grpc;

import io.grpc.BindableService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Publica por gRPC todas las APIs ({@link BindableService}) del microservicio que la importe.
 * El puerto se toma de {@code grpc.servidor.puerto}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionServidorGrpc {

    @Bean
    public ServidorGrpc servidorGrpc(@Value("${grpc.servidor.puerto}") int puerto, List<BindableService> apis) {
        return new ServidorGrpc(puerto, apis);
    }
}
