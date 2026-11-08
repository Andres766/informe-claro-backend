package co.edu.ucc.informeclaro.interpretacion.config;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Clientes salientes del microservicio, separados por tipo de comunicación:
 * gRPC (HTTP/2) hacia otros microservicios y REST hacia APIs de terceros.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionClientes {

    /** gRPC: canal hacia el microservicio de documentos. */
    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel canalDocumentos(@Value("${informeclaro.servicios.documentos}") String destino) {
        return CanalesGrpc.crear(destino);
    }

    @Bean
    public DocumentosServicioGrpc.DocumentosServicioBlockingStub stubDocumentos(ManagedChannel canalDocumentos) {
        return DocumentosServicioGrpc.newBlockingStub(canalDocumentos);
    }

    /** REST: cliente HTTP para la API de Anthropic (Claude), un servicio de terceros. */
    @Bean
    public RestClient restClientAnthropic(RestClient.Builder constructor,
                                          @Value("${informeclaro.anthropic.url}") String url) {
        var fabrica = ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(15))
                .withReadTimeout(Duration.ofMinutes(5))); // un informe largo puede tardar en explicarse
        return constructor.baseUrl(url).requestFactory(fabrica).build();
    }
}
