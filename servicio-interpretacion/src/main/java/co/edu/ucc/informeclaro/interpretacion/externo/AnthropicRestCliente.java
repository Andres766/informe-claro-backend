package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoLlmDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Cliente REST de la API de Anthropic (Claude): una API de TERCEROS, por eso se usa REST y no gRPC.
 * Pide salida estructurada (JSON Schema) para recibir siempre el mismo formato.
 */
@Component
public class AnthropicRestCliente implements ClienteLlm {

    private static final String VERSION_API = "2023-06-01";
    // Si el modelo declina por política, la API reintenta con otro modelo adecuado en la misma llamada.
    private static final String BETA_RESPALDO = "server-side-fallback-2026-07-01";

    private final RestClient http;
    private final JsonMapper json;
    private final String apiKey;
    private final String modelo;

    public AnthropicRestCliente(RestClient restClientAnthropic, JsonMapper json,
                                @Value("${informeclaro.anthropic.api-key:}") String apiKey,
                                @Value("${informeclaro.anthropic.modelo}") String modelo) {
        this.http = restClientAnthropic;
        this.json = json;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String modelo() {
        return modelo;
    }

    /** POST /v1/messages con la instrucción de sistema y el informe; devuelve la salida estructurada. */
    @Override
    @SuppressWarnings("unchecked")
    public ResultadoLlmDto interpretar(String instruccionSistema, String mensajeUsuario) {
        Map<String, Object> cuerpo = Map.of(
                "model", modelo,
                "max_tokens", 16000,
                "system", instruccionSistema,
                "messages", List.of(Map.of("role", "user", "content", mensajeUsuario)),
                "output_config", Map.of("format", Map.of("type", "json_schema", "schema", EsquemaSalida.json(true))),
                "fallbacks", "default");
        Map<String, Object> respuesta;
        try {
            respuesta = http.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", VERSION_API)
                    .header("anthropic-beta", BETA_RESPALDO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new LlmNoDisponibleException("Fallo al llamar a la API de Anthropic: " + e.getMessage(), e);
        }
        if (respuesta == null) {
            throw new LlmNoDisponibleException("La API de Anthropic respondió vacío", null);
        }
        Object razon = respuesta.get("stop_reason");
        if ("refusal".equals(razon)) {
            throw new LlmNoDisponibleException("El modelo declinó la solicitud", null);
        }
        // Con salida estructurada el bloque de texto trae el JSON; los bloques de razonamiento se ignoran.
        String texto = ((List<Map<String, Object>>) respuesta.getOrDefault("content", List.of())).stream()
                .filter(bloque -> "text".equals(bloque.get("type")))
                .map(bloque -> (String) bloque.get("text"))
                .findFirst()
                .orElseThrow(() -> new LlmNoDisponibleException("Respuesta sin texto (stop_reason=" + razon + ")", null));
        try {
            return json.readValue(texto, ResultadoLlmDto.class);
        } catch (RuntimeException e) {
            throw new LlmNoDisponibleException("La salida del modelo no tiene el formato esperado", e);
        }
    }
}
