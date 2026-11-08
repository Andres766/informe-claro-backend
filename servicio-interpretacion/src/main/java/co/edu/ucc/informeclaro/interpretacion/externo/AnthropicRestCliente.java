package co.edu.ucc.informeclaro.interpretacion.externo;

import co.edu.ucc.informeclaro.interpretacion.dto.ResultadoClaudeDto;
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
public class AnthropicRestCliente {

    private static final String VERSION_API = "2023-06-01";
    // Si el modelo declina por política, la API reintenta con otro modelo adecuado en la misma llamada.
    private static final String BETA_RESPALDO = "server-side-fallback-2026-07-01";

    private static final Map<String, Object> ESQUEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "resumen", Map.of("type", "string", "description", "2 a 3 frases que resumen el informe sin diagnosticar"),
                    "valores", Map.of("type", "array", "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "nombre_valor", Map.of("type", "string", "description", "Nombre del examen tal como aparece en el informe"),
                                    "valor", Map.of("type", "string", "description", "Resultado numérico o textual, sin la unidad"),
                                    "unidad", Map.of("type", "string", "description", "Unidad de medida; vacío si no aplica"),
                                    "rango_referencia", Map.of("type", "string", "description", "Rango impreso, p. ej. '70 - 100' o '< 200'; vacío si no aparece"),
                                    "explicacion", Map.of("type", "string", "description", "Explicación en lenguaje sencillo, sin diagnosticar")),
                            "required", List.of("nombre_valor", "valor", "unidad", "rango_referencia", "explicacion"),
                            "additionalProperties", false)),
                    "preguntas", Map.of("type", "array", "items", Map.of("type", "string"),
                            "description", "Entre 3 y 6 preguntas para el médico")),
            "required", List.of("resumen", "valores", "preguntas"),
            "additionalProperties", false);

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

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelo() {
        return modelo;
    }

    /** POST /v1/messages con la instrucción de sistema y el informe; devuelve la salida estructurada. */
    @SuppressWarnings("unchecked")
    public ResultadoClaudeDto interpretar(String instruccionSistema, String mensajeUsuario) {
        Map<String, Object> cuerpo = Map.of(
                "model", modelo,
                "max_tokens", 16000,
                "system", instruccionSistema,
                "messages", List.of(Map.of("role", "user", "content", mensajeUsuario)),
                "output_config", Map.of("format", Map.of("type", "json_schema", "schema", ESQUEMA)),
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
            throw new ClaudeNoDisponibleException("Fallo al llamar a la API de Anthropic: " + e.getMessage(), e);
        }
        if (respuesta == null) {
            throw new ClaudeNoDisponibleException("La API de Anthropic respondió vacío", null);
        }
        Object razon = respuesta.get("stop_reason");
        if ("refusal".equals(razon)) {
            throw new ClaudeNoDisponibleException("El modelo declinó la solicitud", null);
        }
        // Con salida estructurada el bloque de texto trae el JSON; los bloques de razonamiento se ignoran.
        String texto = ((List<Map<String, Object>>) respuesta.getOrDefault("content", List.of())).stream()
                .filter(bloque -> "text".equals(bloque.get("type")))
                .map(bloque -> (String) bloque.get("text"))
                .findFirst()
                .orElseThrow(() -> new ClaudeNoDisponibleException("Respuesta sin texto (stop_reason=" + razon + ")", null));
        try {
            return json.readValue(texto, ResultadoClaudeDto.class);
        } catch (RuntimeException e) {
            throw new ClaudeNoDisponibleException("La salida del modelo no tiene el formato esperado", e);
        }
    }

    public static class ClaudeNoDisponibleException extends RuntimeException {
        public ClaudeNoDisponibleException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
