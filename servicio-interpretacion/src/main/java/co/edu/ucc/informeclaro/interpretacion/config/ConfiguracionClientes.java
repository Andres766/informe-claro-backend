package co.edu.ucc.informeclaro.interpretacion.config;

import co.edu.ucc.informeclaro.comun.grpc.CanalesGrpc;
import co.edu.ucc.informeclaro.interpretacion.externo.AnthropicRestCliente;
import co.edu.ucc.informeclaro.interpretacion.externo.ClienteLlm;
import co.edu.ucc.informeclaro.interpretacion.externo.GeminiRestCliente;
import co.edu.ucc.informeclaro.grpc.documentos.DocumentosServicioGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

    /** REST: cliente HTTP para la API de Gemini (Google), un servicio de terceros. */
    @Bean
    public RestClient restClientGemini(RestClient.Builder constructor,
                                       @Value("${informeclaro.gemini.url}") String url,
                                       @Value("${informeclaro.gemini.timeout-s:90}") long timeoutS) {
        return clienteRest(constructor, url, Duration.ofSeconds(timeoutS)); // si tarda más, se prueba el modelo de respaldo
    }

    /** REST: cliente HTTP para la API de Anthropic (Claude), un servicio de terceros. */
    @Bean
    public RestClient restClientAnthropic(RestClient.Builder constructor,
                                          @Value("${informeclaro.anthropic.url}") String url) {
        return clienteRest(constructor, url, Duration.ofMinutes(5)); // un informe largo puede tardar en explicarse
    }

    /** Proveedor de IA activo: Gemini si tiene API key; si no, Claude (sin ninguna, modo demo por reglas). */
    @Bean
    @Primary
    public ClienteLlm clienteLlm(GeminiRestCliente gemini, AnthropicRestCliente anthropic) {
        return gemini.configurado() ? gemini : anthropic;
    }

    private static RestClient clienteRest(RestClient.Builder constructor, String url, Duration lectura) {
        var fabrica = ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(15))
                .withReadTimeout(lectura));
        return constructor.baseUrl(url).requestFactory(fabrica).build();
    }
}
